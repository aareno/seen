package com.aareno.seen.data.TvMovies;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.aareno.seen.ui.TvMovies.Show;

import java.util.List;

@Dao
public interface ShowDao {
    @Query("SELECT * FROM show WHERE id = :id")
    Show getShowById(int id);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Show show);

    @Query("SELECT * FROM show WHERE isWatching = 1")
    List<Show> getWatchingShow();

    @Query("SELECT * FROM show WHERE isWatching = 0")
    List<Show> getWatchedShow();

    @Update
    void update(Show show);

    @Delete
    void delete(Show show);

    @Query("DELETE FROM show")
    void deleteAllShows();
}