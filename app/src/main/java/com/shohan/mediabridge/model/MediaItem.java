package com.shohan.mediabridge.model;
import java.util.Locale;
public class MediaItem {
    public static final int TYPE_VIDEO=0,TYPE_AUDIO=1,TYPE_IMAGE=2;
    private final long id; private final String name,path,mimeType;
    private final long size,duration,dateAdded; private final int type;
    public MediaItem(long id,String name,String path,long size,long duration,int type,String mime,long date){
        this.id=id;this.name=name;this.path=path;this.size=size;this.duration=duration;
        this.type=type;this.mimeType=mime;this.dateAdded=date;}
    public long getId(){return id;} public String getName(){return name;}
    public String getPath(){return path;} public long getSize(){return size;}
    public long getDuration(){return duration;} public int getType(){return type;}
    public String getMimeType(){return mimeType;} public long getDateAdded(){return dateAdded;}
    public String getFormattedSize(){
        if(size<1024)return size+" B";
        if(size<1048576)return String.format(Locale.US,"%.1f KB",size/1024f);
        return String.format(Locale.US,"%.1f MB",size/1048576f);}
    public String getFormattedDuration(){
        if(duration<=0)return "";
        long s=duration/1000,m=s/60,h=m/60;
        return h>0?String.format(Locale.US,"%d:%02d:%02d",h,m%60,s%60)
                  :String.format(Locale.US,"%d:%02d",m,s%60);}
}
