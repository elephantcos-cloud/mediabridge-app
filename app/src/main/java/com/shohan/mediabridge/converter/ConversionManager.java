package com.shohan.mediabridge.converter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import androidx.media3.common.MimeTypes;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.TransformationRequest;
import androidx.media3.transformer.Transformer;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConversionManager {

    public enum VideoFormat {
        FMT_3GP_176("3GP 176x144 - Button Phone", "3gp", 176, 144, "h263"),
        FMT_3GP_320("3GP 320x240 - Feature Phone", "3gp", 320, 240, "mpeg4");
        public final String label, ext, codec;
        public final int w, h;
        VideoFormat(String l,String e,int w,int h,String c){label=l;ext=e;this.w=w;this.h=h;codec=c;}
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
        FMT_320x240("JPEG 320x240 - QVGA Landscape", 320, 240);
        public final String label; public final int w, h;
        ImageFormat(String l,int w,int h){label=l;this.w=w;this.h=h;}
    }

    public interface Callback {
        void onProgress(int pct, String label);
        void onSuccess(String outPath, long outSize);
        void onFailure(String error);
    }

    private static final Map<Integer, Long> sessionMap = new ConcurrentHashMap<>();
    private static Transformer activeTransformer;

    public static void convertVideo(Context ctx, int taskId, String in, String outDir, VideoFormat fmt, Callback cb) {
        String out = outDir + File.separator + FileUtils.base(new File(in).getName()) + "_conv." + fmt.ext;
        convertVideo3GP(taskId, in, out, fmt, cb);
    }

    private static void convertVideo3GP(int taskId, String in, String out, VideoFormat fmt, Callback cb) {
        long totalMs = 0;
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(in);
            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) totalMs = Long.parseLong(dur);
            mmr.release();
        } catch (Exception ignored) {}
        final long total = totalMs;
        String cmd = "-y -i \"" + in + "\" -vcodec " + fmt.codec +
            " -s " + fmt.w + "x" + fmt.h +
            " -r 15 -b:v 128k -acodec aac -ar 22050 -ac 1 -b:a 32k \"" + out + "\"";
        FFmpegSession session = FFmpegKit.executeAsync(cmd,
            s -> {
                sessionMap.remove(taskId);
                ReturnCode rc = s.getReturnCode();
                if (ReturnCode.isSuccess(rc)) cb.onSuccess(out, new File(out).length());
                else if (ReturnCode.isCancel(rc)) cb.onFailure("CANCELLED");
                else cb.onFailure("3GP conversion failed");
            },
            log -> {},
            stats -> {
                if (stats != null && stats.getTime() > 0) {
                    int pct = (total > 0) ? (int) Math.min(95, (stats.getTime() * 100L) / total) : 50;
                    cb.onProgress(pct, (stats.getTime() / 1000) + "s");
                }
            });
        sessionMap.put(taskId, session.getSessionId());
    }

    public static void cancelTask(int taskId) {
        Long sid = sessionMap.get(taskId);
        if (sid != null) { FFmpegKit.cancel(sid); sessionMap.remove(taskId); }
        if (activeTransformer != null) { activeTransformer.cancel(); activeTransformer = null; }
    }

    public static void convertAudio(Context ctx, int taskId, String in, String outDir, AudioFormat fmt, Callback cb) {
        String out = outDir + File.separator + FileUtils.base(new File(in).getName()) + "_conv." + fmt.ext;
        Transformer t = new Transformer.Builder(ctx)
            .setTransformationRequest(new TransformationRequest.Builder()
                .setAudioMimeType(MimeTypes.AUDIO_AAC).build())
            .addListener(new Transformer.Listener() {
                @Override public void onCompleted(Composition c, ExportResult r) { cb.onSuccess(out, new File(out).length()); }
                @Override public void onError(Composition c, ExportResult r, ExportException e) { cb.onFailure(e.getMessage() != null ? e.getMessage() : "Audio failed"); }
            }).build();
        activeTransformer = t;
        t.start(new EditedMediaItem.Builder(
            androidx.media3.common.MediaItem.fromUri(Uri.fromFile(new File(in))))
            .setRemoveVideo(true).build(), out);
    }

    public static void convertImage(int taskId, String in, String outDir, ImageFormat fmt, Callback cb) {
        new Thread(() -> {
            try {
                String out = outDir + File.separator + FileUtils.base(new File(in).getName()) + "_conv.jpg";
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true; BitmapFactory.decodeFile(in, o);
                o.inSampleSize = ss(o.outWidth, o.outHeight, fmt.w, fmt.h);
                o.inJustDecodeBounds = false;
                Bitmap bmp = BitmapFactory.decodeFile(in, o);
                if (bmp == null) { cb.onFailure("Cannot decode image"); return; }
                Bitmap sc = Bitmap.createScaledBitmap(bmp, fmt.w, fmt.h, true); bmp.recycle();
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    sc.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                }
                sc.recycle(); cb.onSuccess(out, new File(out).length());
            } catch (Exception e) { cb.onFailure("Image error: " + e.getMessage()); }
        }).start();
    }

    private static int ss(int iw, int ih, int tw, int th) {
        int s = 1;
        if (iw > tw || ih > th) { int hw = iw/2, hh = ih/2; while (hw/s >= tw && hh/s >= th) s *= 2; }
        return s;
    }

    public static void cancelAll() {
        for (long sid : sessionMap.values()) FFmpegKit.cancel(sid);
        sessionMap.clear();
        if (activeTransformer != null) { activeTransformer.cancel(); activeTransformer = null; }
    }
}
