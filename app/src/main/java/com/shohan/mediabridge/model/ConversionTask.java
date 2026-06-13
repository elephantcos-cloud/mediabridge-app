package com.shohan.mediabridge.model;
public class ConversionTask {
    public static final String TYPE_VIDEO="VIDEO",TYPE_AUDIO="AUDIO",TYPE_IMAGE="IMAGE";
    public final int id, formatIndex;
    public final String inputPath, outputDir, type, format;
    public volatile int progress = 0;
    public volatile String status = "WAITING", outputPath = "", displayName = null, inputUri = null;
    public volatile long outputSize = 0;
    public ConversionTask(int id,String ip,String od,String t,String f,int fi){
        this.id=id; inputPath=ip; outputDir=od; type=t; format=f; formatIndex=fi;}
    public String getFileName(){
        if(displayName!=null&&!displayName.isEmpty()) return displayName;
        return new java.io.File(inputPath).getName();
    }
}
