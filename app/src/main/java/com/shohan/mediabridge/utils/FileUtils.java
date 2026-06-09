package com.shohan.mediabridge.utils;
import android.content.ContentResolver;import android.content.Context;
import android.database.Cursor;import android.net.Uri;import android.os.Environment;
import android.provider.OpenableColumns;import android.webkit.MimeTypeMap;
import java.io.*;import java.text.SimpleDateFormat;import java.util.*;
public class FileUtils {
    public static File getOutputDir(Context ctx){
        File dir;
        if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.Q)
            dir=new File(ctx.getExternalFilesDir(null),"Converted");
        else dir=new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS),"MediaBridge");
        if(!dir.exists())dir.mkdirs(); return dir;}

    public static String getMime(Context ctx,Uri uri){
        if(ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()))
            return ctx.getContentResolver().getType(uri);
        String e=MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(e.toLowerCase(Locale.US));}

    public static String typeFrom(String mime){
        if(mime==null)return "UNKNOWN";
        if(mime.startsWith("video/"))return "VIDEO";
        if(mime.startsWith("audio/"))return "AUDIO";
        if(mime.startsWith("image/"))return "IMAGE";
        return "UNKNOWN";}

    public static String nameFromUri(Context ctx,Uri uri){
        if(ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())){
            try(Cursor c=ctx.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception e){}}
        String l=uri.getLastPathSegment(); return l!=null?l:"file";}

    public static String copyToCache(Context ctx,Uri uri){
        try{
            String name=nameFromUri(ctx,uri),ext=ext(name);
            if(ext.isEmpty()){String m=getMime(ctx,uri);
                ext=m!=null?MimeTypeMap.getSingleton().getExtensionFromMimeType(m):"tmp";
                if(ext==null)ext="tmp";}
            File tmp=new File(ctx.getCacheDir(),"mb_"+System.currentTimeMillis()+"."+ext);
            try(InputStream in=ctx.getContentResolver().openInputStream(uri);
                OutputStream out=new FileOutputStream(tmp)){
                if(in==null)return null;
                byte[]buf=new byte[16384];int r;
                while((r=in.read(buf))!=-1)out.write(buf,0,r);}
            return tmp.getAbsolutePath();}catch(Exception e){return null;}}

    public static String ext(String n){if(n==null)return "";int i=n.lastIndexOf('.');
        return i>=0?n.substring(i+1).toLowerCase(Locale.US):"";}
    public static String base(String n){if(n==null)return "";int i=n.lastIndexOf('.');
        return i>=0?n.substring(0,i):n;}
    public static String fmtSize(long b){
        if(b<1024)return b+" B";
        if(b<1048576)return String.format(Locale.US,"%.1f KB",b/1024f);
        if(b<1073741824L)return String.format(Locale.US,"%.1f MB",b/1048576f);
        return String.format(Locale.US,"%.2f GB",b/1073741824f);}
    public static String fmtDuration(long ms){
        if(ms<=0)return "0:00";long s=ms/1000,m=s/60,h=m/60;
        return h>0?String.format(Locale.US,"%d:%02d:%02d",h,m%60,s%60)
                  :String.format(Locale.US,"%d:%02d",m,s%60);}
    public static String fmtDate(long ts){
        return new SimpleDateFormat("MMM dd, yyyy  HH:mm",Locale.US).format(new Date(ts));}
    public static void cleanCache(Context ctx){File[]fs=ctx.getCacheDir().listFiles();
        if(fs!=null)for(File f:fs)if(f.getName().startsWith("mb_"))f.delete();}
}
