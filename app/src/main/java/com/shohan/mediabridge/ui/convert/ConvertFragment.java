package com.shohan.mediabridge.ui.convert;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.shohan.mediabridge.converter.ConversionManager;
import com.shohan.mediabridge.databinding.FragmentConvertBinding;
import com.shohan.mediabridge.db.*;
import com.shohan.mediabridge.model.ConversionTask;
import com.shohan.mediabridge.service.ConversionService;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConvertFragment extends Fragment {
    private FragmentConvertBinding b;
    private QueueAdapter queueAdapter;
    private static final List<ConversionTask> tasks=new ArrayList<>();
    private static final AtomicInteger ids=new AtomicInteger(0);
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService pool=Executors.newCachedThreadPool();
    private final Map<Integer,ParcelFileDescriptor> pfdMap=new ConcurrentHashMap<>();
    private boolean pollerRunning=false;

    private final Runnable progressPoller=new Runnable(){
        @Override public void run(){
            boolean any=tasks.stream().anyMatch(t->"RUNNING".equals(t.status));
            if(any&&b!=null){queueAdapter.notifyDataSetChanged();ui.postDelayed(this,300);}
            else pollerRunning=false;}};

    private final ActivityResultLauncher<Intent> picker=
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{
            if(r.getResultCode()!=Activity.RESULT_OK||r.getData()==null)return;
            if(r.getData().getClipData()!=null){
                for(int i=0;i<r.getData().getClipData().getItemCount();i++)
                    enqueue(r.getData().getClipData().getItemAt(i).getUri());
            }else if(r.getData().getData()!=null)enqueue(r.getData().getData());
        });

    @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup p,Bundle s){
        b=FragmentConvertBinding.inflate(i,p,false);return b.getRoot();}

    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        super.onViewCreated(v,s);
        queueAdapter=new QueueAdapter(requireContext(),tasks);
        queueAdapter.setCancelListener(task->{
            ConversionManager.cancelTask(task.id);
            task.status="CANCELLED";
            closePfd(task.id);
            safeRefresh();stopServiceIfIdle();});
        b.queueRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.queueRecycler.setAdapter(queueAdapter);
        b.btnPickFile.setOnClickListener(x->pick());
        b.btnClearDone.setOnClickListener(x->clearDone());
        if(tasks.stream().anyMatch(t->"RUNNING".equals(t.status)))startPoller();
        refresh();}

    private void startPoller(){
        if(!pollerRunning){pollerRunning=true;ui.postDelayed(progressPoller,300);}}

    private void pick(){
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","audio/*","image/*"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);
        picker.launch(Intent.createChooser(i,"Select files"));}

    private void enqueue(Uri uri){
        final Context ctx=requireContext().getApplicationContext();
        pool.execute(()->{
            String fileName=null,mime=null,realPath=null;
            try(Cursor c=ctx.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATA},null,null,null)){
                if(c!=null&&c.moveToFirst()){
                    fileName=c.getString(0);mime=c.getString(1);
                    String data=c.getString(2);
                    if(data!=null&&new File(data).exists())realPath=data;
                }
            }catch(Exception ignored){}
            if(mime==null&&fileName!=null){
                String e=FileUtils.ext(fileName);
                if(e.matches("mp4|avi|mkv|mov|3gp|wmv|flv|webm"))mime="video/mp4";
                else if(e.matches("mp3|aac|wav|ogg|flac|amr|m4a"))mime="audio/mpeg";
                else if(e.matches("jpg|jpeg|png|bmp|gif|webp"))mime="image/jpeg";
                else mime="";}
            String type=FileUtils.typeFrom(mime!=null?mime:"");
            if("UNKNOWN".equals(type))return;
            final String finalName=(fileName!=null&&!fileName.isEmpty())?fileName:"file";
            final String finalType=type;
            final String uriStr=uri.toString();
            final String outDir=FileUtils.getOutputDir(ctx).getAbsolutePath();

            if("VIDEO".equals(type)){
                // VIDEO: try real path, else PFD fd path
                if(realPath!=null){
                    ui.post(()->showFormatPicker(ctx,realPath,null,finalType,outDir,finalName,uriStr));
                }else{
                    try{
                        ParcelFileDescriptor pfd=ctx.getContentResolver().openFileDescriptor(uri,"r");
                        if(pfd==null)return;
                        // Try to resolve symlink to real path
                        String resolved=new File("/proc/self/fd/"+pfd.getFd()).getCanonicalPath();
                        if(!resolved.startsWith("/proc")&&new File(resolved).exists()){
                            pfd.close();
                            ui.post(()->showFormatPicker(ctx,resolved,null,finalType,outDir,finalName,uriStr));
                        }else{
                            String fdPath="/proc/self/fd/"+pfd.getFd();
                            ui.post(()->showFormatPicker(ctx,fdPath,pfd,finalType,outDir,finalName,uriStr));
                        }
                    }catch(Exception e){return;}
                }
            }else{
                // AUDIO & IMAGE: use content URI directly — no file path needed
                ui.post(()->showFormatPicker(ctx,uriStr,null,finalType,outDir,finalName,uriStr));
            }
        });
    }

    private void showFormatPicker(Context ctx,String inputPath,ParcelFileDescriptor pfd,
                                   String type,String outDir,String fileName,String uriStr){
        if(!isAdded()){if(pfd!=null)try{pfd.close();}catch(Exception ignored){}return;}
        String[] items;
        if("VIDEO".equals(type)){
            ConversionManager.VideoFormat[] fmts=ConversionManager.VideoFormat.values();
            items=new String[fmts.length];for(int i=0;i<fmts.length;i++)items[i]=fmts[i].label;
        }else if("AUDIO".equals(type)){
            ConversionManager.AudioFormat[] fmts=ConversionManager.AudioFormat.values();
            items=new String[fmts.length];for(int i=0;i<fmts.length;i++)items[i]=fmts[i].label;
        }else{
            ConversionManager.ImageFormat[] fmts=ConversionManager.ImageFormat.values();
            items=new String[fmts.length];for(int i=0;i<fmts.length;i++)items[i]=fmts[i].label;}
        final int[] sel={0};
        new AlertDialog.Builder(requireContext())
            .setTitle("Format: "+fileName)
            .setSingleChoiceItems(items,0,(d,which)->sel[0]=which)
            .setPositiveButton("Convert",(d,w)->{
                ConversionTask task=new ConversionTask(
                    ids.incrementAndGet(),inputPath,outDir,type,items[sel[0]],sel[0]);
                task.displayName=fileName;
                task.inputUri=uriStr;
                if(pfd!=null)pfdMap.put(task.id,pfd);
                tasks.add(task);
                if(b!=null){queueAdapter.notifyItemInserted(tasks.size()-1);refresh();}
                startTask(task,ctx);})
            .setNegativeButton("Cancel",(d,w)->{if(pfd!=null)try{pfd.close();}catch(Exception ignored){}})
            .setOnCancelListener(d->{if(pfd!=null)try{pfd.close();}catch(Exception ignored){}})
            .show();}

    private void startTask(ConversionTask task,Context ctx){
        task.status="RUNNING";safeRefresh();startPoller();updateService(ctx,task,0);
        ConversionManager.Callback cb=new ConversionManager.Callback(){
            @Override public void onProgress(int pct,String l){task.progress=pct;updateService(ctx,task,pct);}
            @Override public void onSuccess(String op,long sz){
                task.status="DONE";task.outputPath=op;task.outputSize=sz;task.progress=100;
                closePfd(task.id);saveDb(ctx,task);
                android.media.MediaScannerConnection.scanFile(ctx,new String[]{op},null,null);
                ui.post(()->{safeRefresh();stopServiceIfIdle();});}
            @Override public void onFailure(String e){
                task.status="CANCELLED".equals(e)?"CANCELLED":"FAILED";
                closePfd(task.id);
                ui.post(()->{safeRefresh();stopServiceIfIdle();});}};
        switch(task.type){
            case "VIDEO":
                ConversionManager.convertVideo(ctx,task.id,task.inputPath,task.outputDir,
                    ConversionManager.VideoFormat.values()[task.formatIndex],cb);break;
            case "AUDIO":
                ConversionManager.convertAudio(ctx,task.id,task.getFileName(),task.inputUri,task.outputDir,
                    ConversionManager.AudioFormat.values()[task.formatIndex],cb);break;
            case "IMAGE":
                ConversionManager.convertImage(ctx,task.id,task.getFileName(),task.inputUri,task.outputDir,
                    ConversionManager.ImageFormat.values()[task.formatIndex],cb);break;
        }}

    private void closePfd(int id){
        ParcelFileDescriptor p=pfdMap.remove(id);
        if(p!=null)try{p.close();}catch(Exception ignored){}}

    private void updateService(Context ctx,ConversionTask task,int pct){
        long total=tasks.size();long done=tasks.stream().filter(t->"DONE".equals(t.status)).count();
        Intent si=new Intent(ctx,ConversionService.class);
        si.setAction(ConversionService.ACTION_UPDATE);
        si.putExtra(ConversionService.EXTRA_FILENAME,task.getFileName());
        si.putExtra(ConversionService.EXTRA_PROGRESS,pct);
        si.putExtra(ConversionService.EXTRA_TOTAL,(int)total);
        si.putExtra("done",(int)done);
        ContextCompat.startForegroundService(ctx,si);}

    private void stopServiceIfIdle(){
        boolean any=tasks.stream().anyMatch(t->"RUNNING".equals(t.status));
        if(!any&&isAdded()){Intent si=new Intent(requireContext(),ConversionService.class);
            si.setAction(ConversionService.ACTION_STOP);requireContext().startService(si);}}

    private void safeRefresh(){if(b==null)return;queueAdapter.notifyDataSetChanged();refresh();}

    private void refresh(){
        if(b==null)return;
        long r=tasks.stream().filter(t->"RUNNING".equals(t.status)).count();
        long w=tasks.stream().filter(t->"WAITING".equals(t.status)).count();
        long d=tasks.stream().filter(t->"DONE".equals(t.status)).count();
        b.tvStatus.setText(tasks.isEmpty()?"Pick files to convert.":
            r+" converting  "+w+" waiting  "+d+" done");}

    private void clearDone(){
        tasks.removeIf(t->"DONE".equals(t.status)||"FAILED".equals(t.status)||"CANCELLED".equals(t.status));
        if(b!=null){queueAdapter.notifyDataSetChanged();refresh();}stopServiceIfIdle();}

    private void saveDb(Context ctx,ConversionTask t){
        pool.execute(()->AppDatabase.get(ctx).conversionDao().insert(new ConversionRecord(
            t.getFileName(),t.inputPath,t.outputPath,t.format,t.type,
            System.currentTimeMillis(),"SUCCESS",t.outputSize)));}

    @Override public void onDestroyView(){
        super.onDestroyView();ui.removeCallbacks(progressPoller);b=null;}
}
