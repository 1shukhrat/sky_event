package com.example.sky_event.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.sky_event.database.converters.DateConverter;
import com.example.sky_event.database.converters.ListConverter;
import com.example.sky_event.database.converters.WeatherConditionConverter;
import com.example.sky_event.database.dao.EventDao;
import com.example.sky_event.database.dao.WeatherDao;
import com.example.sky_event.database.entity.EventEntity;
import com.example.sky_event.database.entity.WeatherEntity;

@Database(entities = {WeatherEntity.class, EventEntity.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class, ListConverter.class, WeatherConditionConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "sky_event_db";
    private static volatile AppDatabase instance;
    
    public abstract WeatherDao weatherDao();
    public abstract EventDao eventDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
    
    public static synchronized void clearDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
        instance = null;
    }
} 