package com.aareno.seen.data.Anime;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.aareno.seen.ui.Anime.Anime;

@Database(entities = {Anime.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AnimeDatabase extends RoomDatabase {
    private static volatile AnimeDatabase INSTANCE;

    public abstract AnimeDao animeDao();

    public static synchronized AnimeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AnimeDatabase.class,
                    "anime_database"
            ).fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }
}