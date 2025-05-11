package com.aareno.seen.data.Anime;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.aareno.seen.ui.Anime.Anime;

import java.util.List;

@Dao
public interface AnimeDao {
    @Query("SELECT * FROM anime WHERE titleRomaji = :title")
    Anime getAnimeByTitle(String title);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Anime anime);

    @Query("SELECT * FROM anime WHERE isWatching = 1")
    List<Anime> getWatchingAnime();

    @Query("SELECT * FROM anime WHERE isWatching = 0")
    List<Anime> getWatchedAnime();

    @Update
    void update(Anime anime);

    @Delete
    void delete(Anime anime);

    @Query("DELETE FROM anime")
    void deleteAllAnime();
}