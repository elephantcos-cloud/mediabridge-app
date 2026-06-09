package com.shohan.mediabridge.db;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName="conversions")
public class ConversionRecord {
    @PrimaryKey(autoGenerate=true) public int id;
    public String sourceName,sourcePath,outputPath,outputFormat,mediaType,status;
    public long timestamp,outputSize;
    public ConversionRecord(String sourceName,String sourcePath,String outputPath,
            String outputFormat,String mediaType,long timestamp,String status,long outputSize){
        this.sourceName=sourceName;this.sourcePath=sourcePath;this.outputPath=outputPath;
        this.outputFormat=outputFormat;this.mediaType=mediaType;this.timestamp=timestamp;
        this.status=status;this.outputSize=outputSize;}
}
