package com.shohan.mediabridge.ui.convert;
import android.app.Activity;import android.content.Intent;import android.net.Uri;
import android.os.Bundle;import android.os.Handler;import android.os.Looper;
import android.view.*;import android.widget.*;
import androidx.activity.result.*;import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;import androidx.fragment.app.Fragment;
import com.shohan.mediabridge.converter.ConversionManager;
import com.shohan.mediabridge.databinding.FragmentConvertBinding;
import com.shohan.mediabridge.db.*;import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;import java.util.concurrent.*;

public class ConvertFragment extends Fragment {
    private FragmentConvertBinding b;
    private String cachedPath; private String currentMime="";
    private boolean converting=false;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService pool=Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> picker=
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{
            if(result.getResultCode()==Activity.RESULT_OK&&result.getData()!=null)
                handleUri(result.getData().getData());
        });

    @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup p,Bundle s){
        b=FragmentConvertBinding.inflate(i,p,false);return b.getRoot();}

    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        super.onViewCreated(v,s);
        b.btnPickFile.setOnClickListener(x->pickFile());
        b.btnConvert.setOnClickListener(x->convert());
        b.btnCancel.setOnClickListener(x->cancel());
        b.btnConvert.setEnabled(false);
        b.btnCancel.setVisibility(View.GONE);
        b.progressGroup.setVisibility(View.GONE);
        b.cardFileInfo.setVisibility(View.GONE);
        updateSpinner("UNKNOWN");
    }

    private void pickFile(){
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","audio/*","image/*"});
        i.addCategory(Intent.CATEGORY_OPENABLE);
        picker.launch(Intent.createChooser(i,"Select media file"));
    }

    private void handleUri(Uri uri){
        b.btnConvert.setEnabled(false); b.cardFileInfo.setVisibility(View.GONE);
        b.tvStatus.setText("Loading file\u2026");
        pool.execute(()->{
            String path=FileUtils.copyToCache(requireContext(),uri);
            String mime=FileUtils.getMime(requireContext(),uri);
            if(mime==null&&path!=null){
                String e=FileUtils.ext(new File(path).getName());
                if(e.matches("mp4|avi|mkv|mov|3gp|wmv|flv"))mime="video/mp4";
                else if(e.matches("mp3|aac|wav|ogg|flac|amr|m4a"))mime="audio/mpeg";
                else if(e.matches("jpg|jpeg|png|bmp|gif|webp"))mime="image/jpeg";
            }
            String fp=path, fm=(mime!=null?mime:"");
            ui.post(()->{
                if(fp==null){b.tvStatus.setText("Failed to load file.");return;}
                cachedPath=fp; currentMime=fm;
                String name=FileUtils.nameFromUri(requireContext(),uri);
                b.tvFileName.setText(name);
                b.tvFileInfo.setText(FileUtils.fmtSize(new File(fp).length())+"  \u2022  "+FileUtils.typeFrom(fm));
                b.cardFileInfo.setVisibility(View.VISIBLE);
                updateSpinner(FileUtils.typeFrom(fm));
                b.btnConvert.setEnabled(true);
                b.tvStatus.setText("File ready. Select format and convert.");
            });
        });
    }

    private void updateSpinner(String type){
        String[] opts;
        switch(type){
            case "VIDEO": opts=new String[]{
                ConversionManager.VideoFormat.FMT_3GP_176x144.label,
                ConversionManager.VideoFormat.FMT_MP4_320x240.label,
                ConversionManager.VideoFormat.FMT_AVI_320x240.label}; break;
            case "AUDIO": opts=new String[]{
                ConversionManager.AudioFormat.FMT_AMR_NB.label,
                ConversionManager.AudioFormat.FMT_MP3_64K.label,
                ConversionManager.AudioFormat.FMT_MP3_128K.label,
                ConversionManager.AudioFormat.FMT_AAC_64K.label,
                ConversionManager.AudioFormat.FMT_WAV.label}; break;
            case "IMAGE": opts=new String[]{
                ConversionManager.ImageFormat.FMT_128x160.label,
                ConversionManager.ImageFormat.FMT_176x220.label,
                ConversionManager.ImageFormat.FMT_240x320.label,
                ConversionManager.ImageFormat.FMT_320x240.label,
                ConversionManager.ImageFormat.FMT_BMP.label}; break;
            default: opts=new String[]{"Pick a file first"}; break;
        }
        ArrayAdapter<String> adp=new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item,opts);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerFormat.setAdapter(adp);
    }

    private void convert(){
        if(cachedPath==null||converting)return;
        converting=true;
        b.btnConvert.setEnabled(false);b.btnCancel.setVisibility(View.VISIBLE);
        b.progressGroup.setVisibility(View.VISIBLE);
        b.progressBar.setIndeterminate(true);
        b.tvStatus.setText("Converting\u2026 Please wait.");
        b.tvElapsed.setText("Elapsed: 0:00");
        String outDir=FileUtils.getOutputDir(requireContext()).getAbsolutePath();
        String type=FileUtils.typeFrom(currentMime);
        int sel=b.spinnerFormat.getSelectedItemPosition();
        ConversionManager.Callback cb=new ConversionManager.Callback(){
            @Override public void onProgress(int s,String e){ui.post(()->b.tvElapsed.setText("Elapsed: "+e));}
            @Override public void onSuccess(String p,long sz){ui.post(()->done(p,sz));}
            @Override public void onFailure(String e){ui.post(()->fail(e));}
        };
        switch(type){
            case "VIDEO":ConversionManager.convertVideo(cachedPath,outDir,ConversionManager.VideoFormat.values()[sel],cb);break;
            case "AUDIO":ConversionManager.convertAudio(cachedPath,outDir,ConversionManager.AudioFormat.values()[sel],cb);break;
            case "IMAGE":ConversionManager.convertImage(cachedPath,outDir,ConversionManager.ImageFormat.values()[sel],cb);break;
            default:fail("Unknown media type");
        }
    }

    private void done(String path,long size){
        converting=false;
        b.btnConvert.setEnabled(true);b.btnCancel.setVisibility(View.GONE);
        b.progressBar.setIndeterminate(false);b.progressBar.setProgress(100);
        b.tvStatus.setText("\u2705 Done!\n"+path);
        b.tvElapsed.setText("Output size: "+FileUtils.fmtSize(size));
        String name=(cachedPath!=null)?new File(cachedPath).getName():"file";
        String fmt=(String)b.spinnerFormat.getSelectedItem();
        String type=FileUtils.typeFrom(currentMime);
        pool.execute(()->AppDatabase.get(requireContext()).conversionDao().insert(
            new ConversionRecord(name,cachedPath,path,fmt!=null?fmt:"",type,
                System.currentTimeMillis(),"SUCCESS",size)));
    }

    private void fail(String err){
        converting=false;b.btnConvert.setEnabled(true);b.btnCancel.setVisibility(View.GONE);
        b.progressBar.setIndeterminate(false);
        b.tvStatus.setText("\u274c Failed: "+err);}

    private void cancel(){ConversionManager.cancelAll();converting=false;
        b.btnConvert.setEnabled(true);b.btnCancel.setVisibility(View.GONE);
        b.progressBar.setIndeterminate(false);b.tvStatus.setText("Cancelled.");}

    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
