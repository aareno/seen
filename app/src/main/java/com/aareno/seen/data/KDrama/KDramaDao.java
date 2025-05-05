package com.aareno.seen.data.KDrama;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.aareno.seen.ui.KDrama.KDrama;

import java.util.List;

@Dao
public interface KDramaDao {
    @Query("SELECT * FROM kdrama WHERE id = :id")
    KDrama getKdramaById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(KDrama kdrama);

    @Query("SELECT * FROM kdrama WHERE isWatching = 1")
    List<KDrama> getWatchingKdrama();

    @Query("SELECT * FROM kdrama WHERE isWatching = 0")
    List<KDrama> getWatchedKdrama();

    @Update
    void update(KDrama kdrama);

    @Delete
    void delete(KDrama kdrama);

}
