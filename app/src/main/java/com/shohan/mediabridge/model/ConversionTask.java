package com.shohan.mediabridge.model;
public class ConversionTask {
    public static final String TYPE_VIDEO="VIDEO",TYPE_AUDIO="AUDIO",TYPE_IMAGE="IMAGE";
    public final int id; public final String inputPath,outputDir,type,format; public final int formatIndex;
    public volatile int progress=0; public volatile String status="WAITING",outputPath=""; public volatile long outputSize=0;
    public ConversionTask(int id,String ip,String od,String t,String f,int fi){this.id=id;inputPath=ip;outputDir=od;type=t;format=f;formatIndex=fi;}
    public String getFileName(){return new java.io.File(inputPath).getName();}
}
