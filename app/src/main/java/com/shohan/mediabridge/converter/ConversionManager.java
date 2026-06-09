package com.shohan.mediabridge.converter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;
import java.io.FileOutputStream;

public class ConversionManager {

    public enum VideoFormat {
        FMT_3GP_176x144("3GP 176\u00d7144 \u2014 All Button Phones","3gp"),
        FMT_MP4_320x240("MP4 320\u00d7240 \u2014 Better Quality","mp4"),
        FMT_AVI_320x240("AVI 320\u00d7240 \u2014 Wide Compatibility","avi");
        public final String label,ext;
        VideoFormat(String l,String e){label=l;ext=e;}
    }
    public enum AudioFormat {
        FMT_AMR_NB ("AMR-NB 8kHz \u2014 Tiny, All Phones","amr"),
        FMT_MP3_64K("MP3 64kbps \u2014 Compact","mp3"),
        FMT_MP3_128K("MP3 128kbps \u2014 High Quality","mp3"),
        FMT_AAC_64K("AAC 64kbps \u2014 Modern Phones","aac"),
        FMT_WAV    ("WAV PCM \u2014 Lossless","wav");
        public final String label,ext;
        AudioFormat(String l,String e){label=l;ext=e;}
    }
    public enum ImageFormat {
        FMT_128x160("JPEG 128\u00d7160 \u2014 Basic Phones",128,160),
        FMT_176x220("JPEG 176\u00d7220 \u2014 Standard",176,220),
        FMT_240x320("JPEG 240\u00d7320 \u2014 QVGA Portrait",240,320),
        FMT_320x240("JPEG 320\u00d7240 \u2014 QVGA Landscape",320,240),
        FMT_BMP    ("BMP RGB565 \u2014 Max Compatibility",0,0);
        public final String label; public final int w,h;
        ImageFormat(String l,int w,int h){label=l;this.w=w;this.h=h;}
    }

    public interface Callback {
        void onProgress(int elapsedSec, String elapsed);
        void onSuccess(String outPath, long outSize);
        void onFailure(String error);
    }

    public static void convertVideo(String in, String outDir, VideoFormat fmt, Callback cb){
        String out=outDir+File.separator+FileUtils.base(new File(in).getName())+"_conv."+fmt.ext;
        runFfmpeg(videoArgs(in,out,fmt),out,cb);
    }
    private static String[] videoArgs(String in,String out,VideoFormat fmt){
        String p176="scale=176:144:force_original_aspect_ratio=decrease,pad=176:144:(ow-iw)/2:(oh-ih)/2";
        String p320="scale=320:240:force_original_aspect_ratio=decrease,pad=320:240:(ow-iw)/2:(oh-ih)/2";
        switch(fmt){
            case FMT_3GP_176x144: return new String[]{"-i",in,"-vcodec","h263","-vf",p176,
                "-r","15","-b:v","128k","-acodec","amr_nb","-ar","8000","-ab","12200","-y",out};
            case FMT_MP4_320x240: return new String[]{"-i",in,"-vcodec","libx264","-vf",p320,
                "-r","25","-b:v","256k","-acodec","aac","-ar","22050","-ab","64k",
                "-profile:v","baseline","-level","3.0","-movflags","+faststart","-y",out};
            default: return new String[]{"-i",in,"-vcodec","mpeg4","-vf",p320,
                "-r","25","-b:v","384k","-acodec","libmp3lame","-ar","22050","-ab","64k","-y",out};
        }
    }

    public static void convertAudio(String in, String outDir, AudioFormat fmt, Callback cb){
        String out=outDir+File.separator+FileUtils.base(new File(in).getName())+"_conv."+fmt.ext;
        runFfmpeg(audioArgs(in,out,fmt),out,cb);
    }
    private static String[] audioArgs(String in,String out,AudioFormat fmt){
        switch(fmt){
            case FMT_AMR_NB:  return new String[]{"-i",in,"-acodec","amr_nb","-ar","8000","-ab","12200","-y",out};
            case FMT_MP3_64K: return new String[]{"-i",in,"-acodec","libmp3lame","-ar","22050","-ab","64k","-y",out};
            case FMT_MP3_128K:return new String[]{"-i",in,"-acodec","libmp3lame","-ar","44100","-ab","128k","-y",out};
            case FMT_AAC_64K: return new String[]{"-i",in,"-acodec","aac","-ar","22050","-ab","64k","-y",out};
            default:          return new String[]{"-i",in,"-acodec","pcm_s16le","-ar","22050","-y",out};
        }
    }

    public static void convertImage(String in, String outDir, ImageFormat fmt, Callback cb){
        new Thread(()->{
            try{
                String ext=(fmt==ImageFormat.FMT_BMP)?"bmp":"jpg";
                String out=outDir+File.separator+FileUtils.base(new File(in).getName())+"_conv."+ext;
                BitmapFactory.Options o=new BitmapFactory.Options();
                o.inJustDecodeBounds=true; BitmapFactory.decodeFile(in,o);
                int tw=(fmt.w>0)?fmt.w:o.outWidth, th=(fmt.h>0)?fmt.h:o.outHeight;
                o.inSampleSize=sampleSize(o.outWidth,o.outHeight,tw,th);
                o.inJustDecodeBounds=false;
                Bitmap bmp=BitmapFactory.decodeFile(in,o);
                if(bmp==null){cb.onFailure("Cannot decode image"); return;}
                Bitmap sc=Bitmap.createScaledBitmap(bmp,tw,th,true); bmp.recycle();
                try(FileOutputStream fos=new FileOutputStream(out)){
                    if(fmt==ImageFormat.FMT_BMP){
                        Bitmap rgb=sc.copy(Bitmap.Config.RGB_565,false);
                        rgb.compress(Bitmap.CompressFormat.JPEG,95,fos); rgb.recycle();
                    } else sc.compress(Bitmap.CompressFormat.JPEG,85,fos);
                }
                sc.recycle();
                cb.onSuccess(out,new File(out).length());
            }catch(Exception e){cb.onFailure("Image error: "+e.getMessage());}
        }).start();
    }

    private static int sampleSize(int iw,int ih,int tw,int th){
        int s=1; if(iw>tw||ih>th){int hw=iw/2,hh=ih/2;while(hw/s>=tw&&hh/s>=th)s*=2;} return s;}

    private static void runFfmpeg(String[] args,String out,Callback cb){
        FFmpegKit.executeWithArgumentsAsync(args,session->{
            ReturnCode rc=session.getReturnCode();
            if(ReturnCode.isSuccess(rc)){cb.onSuccess(out,new File(out).exists()?new File(out).length():0L);}
            else{String logs=session.getLogsAsString();
                int st=(logs!=null&&logs.length()>500)?logs.length()-500:0;
                cb.onFailure(logs!=null?logs.substring(st):"FFmpeg error");}
        },log->{},stats->cb.onProgress((int)(stats.getTime()/1000),FileUtils.fmtDuration(stats.getTime())));
    }
    public static void cancelAll(){FFmpegKit.cancel();}
}
