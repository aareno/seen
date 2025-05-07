package com.aareno.seen.data.TvMovies;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.aareno.seen.data.Anime.DateConverter;
import com.aareno.seen.ui.TvMovies.Show;

@Database(entities = {Show.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class ShowDatabase extends RoomDatabase {
    private static volatile ShowDatabase INSTANCE;
    public abstract ShowDao ShowDao();
    public static synchronized ShowDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    ShowDatabase.class,
                    "show_database"
            ).fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }
}