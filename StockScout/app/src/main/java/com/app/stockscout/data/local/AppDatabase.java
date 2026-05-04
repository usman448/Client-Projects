package com.app.stockscout.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;
import com.app.stockscout.data.model.Item;
import com.app.stockscout.data.model.Pick;

/**
 * Room Database configuration
 * Main database for the app containing items and picks tables
 */
@Database(entities = {Item.class, Pick.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ItemDao itemDao();
    public abstract PickDao pickDao();

    // Singleton pattern for database instance
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "stockscout_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}