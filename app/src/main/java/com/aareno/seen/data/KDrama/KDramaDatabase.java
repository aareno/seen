package com.aareno.seen.data.KDrama;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.aareno.seen.data.Anime.DateConverter;
import com.aareno.seen.ui.KDrama.KDrama;

@Database(entities = {KDrama.class}, version = 6, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class KDramaDatabase extends RoomDatabase {
    private static volatile KDramaDatabase INSTANCE;
    public abstract KDramaDao kDramaDao();
    public static synchronized KDramaDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    KDramaDatabase.class,
                    "kdrama_database"
            ).fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }
}
