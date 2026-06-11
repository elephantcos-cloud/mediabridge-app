package com.shohan.mediabridge.converter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.TransformationRequest;
import androidx.media3.transformer.Transformer;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.google.common.collect.ImmutableList;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;
import java.io.FileOutputStream;

public class ConversionManager {

    public enum VideoFormat {
        FMT_176x144("MP4 176x144 - All Modern Phones", "mp4", 176, 144, false),
        FMT_320x240("MP4 320x240 - Better Quality",    "mp4", 320, 240, false),
        FMT_480x360("MP4 480x360 - High Quality",      "mp4", 480, 360, false),
        FMT_3GP_176("3GP 176x144 - Button Phone",      "3gp", 176, 144, true),
        FMT_3GP_320("3GP 320x240 - Feature Phone",     "3gp", 320, 240, true);
        public final String label, ext; public final int w, h; public final boolean useFFmpeg;
        VideoFormat(String l,String e,int w,int h,boolean ff){label=l;ext=e;this.w=w;this.h=h;useFFmpeg=ff;}
    }
    public enum AudioFormat {
        FMT_AAC_32K ("AAC 32kbps - Compact",       "m4a"),
        FMT_AAC_64K ("AAC 64kbps - Good Quality",  "m4a"),
        FMT_AAC_128K("AAC 128kbps - High Quality", "m4a"),
        FMT_AUDIO_ONLY("Extract Audio (AAC)",       "m4a");
        public final String label, ext;
        AudioFormat(String l,String e){label=l;ext=e;}
    }
    public enum ImageFormat {
        FMT_128x160("JPEG 128x160 - Basic Phones",  128, 160),
        FMT_176x220("JPEG 176x220 - Standard",       176, 220),
        FMT_240x320("JPEG 240x320 - QVGA Portrait",  240, 320),
        FMT_320x240("JPEG 320x240 - QVGA Landscape", 320, 240),
        FMT_BMP    ("BMP RGB565 - Max Compatibility",  0,   0);
        public final String label; public final int w, h;
        ImageFormat(String l,int w,int h){label=l;this.w=w;this.h=h;}
    }

    public interface Callback {
        void onProgress(int pct, String label);
        void onSuccess(String outPath, long outSize);
        void onFailure(String error);
    }

    private static Transformer activeTransformer;

    public static void convertVideo(Context ctx,String in,String outDir,VideoFormat fmt,Callback cb){
        String out=outDir+File.separator+FileUtils.base(new File(in).getName())+"_conv."+fmt.ext;
        if(fmt.useFFmpeg) convertVideo3GP(in,out,fmt,cb);
        else convertVideoMedia3(ctx,in,out,fmt,cb);
    }

    private static void convertVideoMedia3(Context ctx,String in,String out,VideoFormat fmt,Callback cb){
        Transformer t=new Transformer.Builder(ctx)
            .setTransformationRequest(new TransformationRequest.Builder()
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC).build())
            .addListener(new Transformer.Listener(){
                @Override public void onCompleted(Composition c,ExportResult r){cb.onSuccess(out,new File(out).length());}
                @Override public void onError(Composition c,ExportResult r,ExportException e){cb.onFailure(e.getMessage()!=null?e.getMessage():"Video failed");}
                @Override public void onFallbackApplied(Composition c,TransformationRequest o,TransformationRequest f){}
            }).build();
        android.os.Handler h=new android.os.Handler(android.os.Looper.getMainLooper());
        androidx.media3.transformer.ProgressHolder ph=new androidx.media3.transformer.ProgressHolder();
        Runnable poll=new Runnable(){@Override public void run(){
            if(activeTransformer!=null){int state=t.getProgress(ph);
                if(ph.progress>=0)cb.onProgress(ph.progress,ph.progress+"%");
                if(state!=Transformer.PROGRESS_STATE_NOT_STARTED)h.postDelayed(this,100);}}};
        h.postDelayed(poll,100); activeTransformer=t;
        EditedMediaItem.Builder ib=new EditedMediaItem.Builder(
            androidx.media3.common.MediaItem.fromUri(Uri.fromFile(new File(in))));
        if(fmt.w>0&&fmt.h>0){ib.setEffects(new Effects(ImmutableList.of(),
            ImmutableList.of(Presentation.createForWidthAndHeight(fmt.w,fmt.h,Presentation.LAYOUT_SCALE_TO_FIT))));}
        t.start(ib.build(),out);
    }

    private static void convertVideo3GP(String in,String out,VideoFormat fmt,Callback cb){
        long totalMs=0;
        try{MediaMetadataRetriever mmr=new MediaMetadataRetriever();mmr.setDataSource(in);
            String dur=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(dur!=null)totalMs=Long.parseLong(dur);mmr.release();}catch(Exception ignored){}
        final long total=totalMs;
        String cmd="-y -i \""+in+"\" -vcodec h263 -s "+fmt.w+"x"+fmt.h+
            " -r 15 -b:v 128k -acodec aac -ar 22050 -ac 1 -b:a 32k \""+out+"\"";
        FFmpegKit.executeAsync(cmd,
            session->{ReturnCode rc=session.getReturnCode();
                if(ReturnCode.isSuccess(rc))cb.onSuccess(out,new File(out).length());
                else cb.onFailure("3GP conversion failed");},
            log->{},
            stats->{if(stats!=null&&stats.getTime()>0){
                int pct=(total>0)?(int)Math.min(95,(stats.getTime()*100L)/total):50;
                cb.onProgress(pct,(stats.getTime()/1000)+"s");}});
    }

    public static void convertAudio(Context ctx,String in,String outDir,AudioFormat fmt,Callback cb){
        String out=outDir+File.separator+FileUtils.base(new File(in).getName())+"_conv."+fmt.ext;
        Transformer t=new Transformer.Builder(ctx)
            .setTransformationRequest(new TransformationRequest.Builder()
                .setAudioMimeType(MimeTypes.AUDIO_AAC).build())
            .addListener(new Transformer.Listener(){
                @Override public void onCompleted(Composition c,ExportResult r){cb.onSuccess(out,new File(out).length());}
                @Override public void onError(Composition c,ExportResult r,ExportException e){cb.onFailure(e.getMessage()!=null?e.getMessage():"Audio failed");}
            }).build();
        activeTransformer=t;
        t.start(new EditedMediaItem.Builder(
            androidx.media3.common.MediaItem.fromUri(Uri.fromFile(new File(in))))
            .setRemoveVideo(true).build(),out);
    }

    public static void convertImage(String in,String outDir,ImageFormat fmt,Callback cb){
        new Thread(()->{try{
            String ext=(fmt==ImageFormat.FMT_BMP)?"bmp":"jpg";
            String out=outDir+File.separator+FileUtils.base(new File(in).getName())+"_conv."+ext;
            BitmapFactory.Options o=new BitmapFactory.Options();
            o.inJustDecodeBounds=true;BitmapFactory.decodeFile(in,o);
            int tw=(fmt.w>0)?fmt.w:o.outWidth,th=(fmt.h>0)?fmt.h:o.outHeight;
            o.inSampleSize=ss(o.outWidth,o.outHeight,tw,th);o.inJustDecodeBounds=false;
            Bitmap bmp=BitmapFactory.decodeFile(in,o);
            if(bmp==null){cb.onFailure("Cannot decode image");return;}
            Bitmap sc=Bitmap.createScaledBitmap(bmp,tw,th,true);bmp.recycle();
            try(FileOutputStream fos=new FileOutputStream(out)){
                if(fmt==ImageFormat.FMT_BMP)sc.copy(Bitmap.Config.RGB_565,false).compress(Bitmap.CompressFormat.JPEG,95,fos);
                else sc.compress(Bitmap.CompressFormat.JPEG,85,fos);}
            sc.recycle();cb.onSuccess(out,new File(out).length());
        }catch(Exception e){cb.onFailure("Image error: "+e.getMessage());}}).start();
    }

    private static int ss(int iw,int ih,int tw,int th){
        int s=1;if(iw>tw||ih>th){int hw=iw/2,hh=ih/2;while(hw/s>=tw&&hh/s>=th)s*=2;}return s;}

    public static void cancelAll(){
        if(activeTransformer!=null){activeTransformer.cancel();activeTransformer=null;}
        FFmpegKit.cancel();
    }
}
