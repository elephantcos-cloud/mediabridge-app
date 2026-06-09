package com.shohan.mediabridge.db;
import androidx.room.Dao;import androidx.room.Insert;import androidx.room.Query;
import java.util.List;
@Dao public interface ConversionDao {
    @Insert void insert(ConversionRecord r);
    @Query("SELECT * FROM conversions ORDER BY timestamp DESC") List<ConversionRecord> getAll();
    @Query("DELETE FROM conversions WHERE id=:id") void deleteById(int id);
    @Query("DELETE FROM conversions") void deleteAll();
}
