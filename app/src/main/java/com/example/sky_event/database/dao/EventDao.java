package com.example.sky_event.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sky_event.database.entity.EventEntity;

import java.util.Date;
import java.util.List;

@Dao
public interface EventDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventEntity event);
    
    @Update
    void update(EventEntity event);
    
    @Delete
    void delete(EventEntity event);
    
    @Query("SELECT * FROM events WHERE id = :eventId")
    EventEntity getEventById(String eventId);
    
    @Query("SELECT * FROM events WHERE userId = :userId ORDER BY date ASC")
    LiveData<List<EventEntity>> getAllEvents(String userId);
    
    @Query("SELECT * FROM events WHERE userId = :userId ORDER BY date ASC")
    List<EventEntity> getAllEventsSync(String userId);
    
    @Query("SELECT * FROM events WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    LiveData<List<EventEntity>> getEventsBetweenDates(String userId, Date startDate, Date endDate);
    
    @Query("SELECT * FROM events WHERE userId = :userId AND isSynced = 0")
    List<EventEntity> getUnsyncedEvents(String userId);
    
    @Query("UPDATE events SET isSynced = 1 WHERE id = :id")
    void markEventAsSynced(String id);
    
    @Query("DELETE FROM events WHERE date < :date")
    void deleteOldEvents(Date date);
    
    @Query("DELETE FROM events WHERE id = :eventId")
    void deleteEventById(String eventId);
} 