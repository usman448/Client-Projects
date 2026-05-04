package com.app.stockscout.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.app.stockscout.data.model.Pick;
import java.util.List;

/**
 * Data Access Object for Pick operations
 * Manages pick records pending sync
 */
@Dao
public interface PickDao {

    @Insert
    void insertPick(Pick pick);

    @Update
    void updatePick(Pick pick);

    @Query("SELECT * FROM picks WHERE synced = 0 ORDER BY timestamp ASC")
    List<Pick> getUnsyncedPicks();

    @Query("SELECT * FROM picks WHERE synced = 1 AND timestamp > :cutoffTime")
    List<Pick> getSyncedPicks(long cutoffTime);

    @Query("DELETE FROM picks WHERE synced = 1 AND timestamp < :cutoffTime")
    void deleteOldSyncedPicks(long cutoffTime);

    @Query("SELECT COUNT(*) FROM picks WHERE synced = 0")
    int getPendingSyncCount();
}