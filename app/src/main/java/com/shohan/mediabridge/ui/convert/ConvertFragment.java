package com.shohan.mediabridge.ui.convert;
import android.app.Activity;import android.app.NotificationChannel;
import android.app.NotificationManager;import android.content.Context;
import android.content.Intent;import android.net.Uri;import android.os.Build;
import android.os.Bundle;import android.os.Handler;import android.os.Looper;
import android.view.*;import android.widget.*;
import androidx.activity.result.*;import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.shohan.mediabridge.R;import com.shohan.mediabridge.converter.ConversionManager;
import com.shohan.mediabridge.databinding.FragmentConvertBinding;
import com.shohan.mediabridge.db.*;import com.shohan.mediabridge.model.ConversionTask;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;import java.util.*;import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class ConvertFragment extends Fragment {
    private FragmentConvertBinding b;
    private QueueAdapter queueAdapter;
    private static final List<ConversionTask> tasks=new ArrayList<>();
    private static final AtomicInteger ids=new AtomicInteger(0);
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService pool=Executors.newCachedThreadPool();
    private static final String CH="mb_convert";
    private static final int BNID=3000;
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
        super.onViewCreated(v,s);createCh();
        queueAdapter=new QueueAdapter(requireContext(),tasks);
        b.queueRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.queueRecycler.setAdapter(queueAdapter);
        b.btnPickFile.setOnClickListener(x->pick());
        b.btnClearDone.setOnClickListener(x->clearDone());
        refresh();
    }
    private void pick(){
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","audio/*","image/*"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);
        picker.launch(Intent.createChooser(i,"Select files"));
    }
    private void enqueue(Uri uri){
        final Context ctx=requireContext().getApplicationContext();
        pool.execute(()->{
            String path=FileUtils.copyToCache(ctx,uri);if(path==null)return;
            String mime=FileUtils.getMime(ctx,uri);
            if(mime==null){String e=FileUtils.ext(new File(path).getName());
                if(e.matches("mp4|avi|mkv|mov|3gp|wmv|flv|webm"))mime="video/mp4";
                else if(e.matches("mp3|aac|wav|ogg|flac|amr|m4a"))mime="audio/mpeg";
                else if(e.matches("jpg|jpeg|png|bmp|gif|webp"))mime="image/jpeg";
                else mime="";}
            String type=FileUtils.typeFrom(mime);if("UNKNOWN".equals(type))return;
            String outDir=FileUtils.getOutputDir(ctx).getAbsolutePath();
            int fi;String fl;
            switch(type){
                case "VIDEO":fl=ConversionManager.VideoFormat.FMT_320x240.label;fi=1;break;
                case "AUDIO":fl=ConversionManager.AudioFormat.FMT_AAC_64K.label;fi=1;break;
                default:fl=ConversionManager.ImageFormat.FMT_240x320.label;fi=2;break;}
            ConversionTask task=new ConversionTask(ids.incrementAndGet(),path,outDir,type,fl,fi);
            ui.post(()->{tasks.add(task);
                if(b!=null){queueAdapter.notifyItemInserted(tasks.size()-1);refresh();}
                startTask(task,ctx);});
        });
    }
    private void startTask(ConversionTask task,Context ctx){
        task.status="RUNNING";int nid=BNID+task.id;safeRefresh();
        ConversionManager.Callback cb=new ConversionManager.Callback(){
            @Override public void onProgress(int pct,String l){task.progress=pct;
                ui.post(()->{safeRefresh();notif(ctx,nid,task.getFileName(),pct,false);});}
            @Override public void onSuccess(String op,long sz){
                task.status="DONE";task.outputPath=op;task.outputSize=sz;task.progress=100;
                saveDb(ctx,task);
                android.media.MediaScannerConnection.scanFile(ctx,new String[]{op},null,null);
                ui.post(()->{safeRefresh();notif(ctx,nid,"\u2705 "+task.getFileName(),100,true);});}
            @Override public void onFailure(String e){task.status="FAILED";
                ui.post(()->{safeRefresh();NotificationManagerCompat.from(ctx).cancel(nid);});}};
        switch(task.type){
            case "VIDEO":ConversionManager.convertVideo(ctx,task.inputPath,task.outputDir,
                ConversionManager.VideoFormat.values()[task.formatIndex],cb);break;
            case "AUDIO":ConversionManager.convertAudio(ctx,task.inputPath,task.outputDir,
                ConversionManager.AudioFormat.values()[task.formatIndex],cb);break;
            case "IMAGE":ConversionManager.convertImage(task.inputPath,task.outputDir,
                ConversionManager.ImageFormat.values()[task.formatIndex],cb);break;}
    }
    private void safeRefresh(){if(b==null)return;queueAdapter.notifyDataSetChanged();refresh();}
    private void refresh(){if(b==null)return;
        long r=tasks.stream().filter(t->"RUNNING".equals(t.status)).count();
        long w=tasks.stream().filter(t->"WAITING".equals(t.status)).count();
        long d=tasks.stream().filter(t->"DONE".equals(t.status)).count();
        b.tvStatus.setText(tasks.isEmpty()?"Pick files to convert.":
            r+" converting \u2022 "+w+" waiting \u2022 "+d+" done");}
    private void clearDone(){tasks.removeIf(t->"DONE".equals(t.status)||"FAILED".equals(t.status));
        if(b!=null){queueAdapter.notifyDataSetChanged();refresh();}}
    private void notif(Context ctx,int id,String txt,int pct,boolean done){
        try{NotificationCompat.Builder n=new NotificationCompat.Builder(ctx,CH)
            .setSmallIcon(R.drawable.ic_convert).setContentTitle("MediaBridge")
            .setContentText(txt).setSilent(true).setOnlyAlertOnce(true)
            .setProgress(100,pct,pct==0&&!done);
            if(done)n.setAutoCancel(true).setOngoing(false);else n.setOngoing(true);
            NotificationManagerCompat.from(ctx).notify(id,n.build());}catch(Exception ignored){}}
    private void createCh(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
        NotificationChannel c=new NotificationChannel(CH,"Conversion",NotificationManager.IMPORTANCE_LOW);
        c.setSound(null,null);((NotificationManager)requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private void saveDb(Context ctx,ConversionTask t){pool.execute(()->
        AppDatabase.get(ctx).conversionDao().insert(new ConversionRecord(
            t.getFileName(),t.inputPath,t.outputPath,t.format,t.type,
            System.currentTimeMillis(),"SUCCESS",t.outputSize)));}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
