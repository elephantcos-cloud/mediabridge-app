package com.shohan.mediabridge.db;
import android.content.Context;
import androidx.room.Database;import androidx.room.Room;import androidx.room.RoomDatabase;
@Database(entities={ConversionRecord.class},version=1,exportSchema=false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract ConversionDao conversionDao();
    public static AppDatabase get(Context ctx){
        if(INSTANCE==null){synchronized(AppDatabase.class){
            if(INSTANCE==null)INSTANCE=Room.databaseBuilder(ctx.getApplicationContext(),
                AppDatabase.class,"mediabridge.db").build();}}
        return INSTANCE;}
}
