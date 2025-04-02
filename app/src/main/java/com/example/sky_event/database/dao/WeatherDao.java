package com.example.sky_event.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sky_event.database.entity.WeatherEntity;

import java.util.Date;
import java.util.List;

@Dao
public interface WeatherDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(WeatherEntity weatherEntity);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<WeatherEntity> weatherEntities);
    
    @Query("SELECT * FROM weather WHERE latitude = :latitude AND longitude = :longitude AND forecastDate = :date LIMIT 1")
    WeatherEntity getWeatherForLocationAndDate(double latitude, double longitude, Date date);
    
    @Query("SELECT * FROM weather WHERE latitude = :latitude AND longitude = :longitude AND forecastDate BETWEEN :startDate AND :endDate ORDER BY forecastDate ASC")
    List<WeatherEntity> getWeatherForLocationBetweenDates(double latitude, double longitude, Date startDate, Date endDate);
    
    @Query("SELECT * FROM weather WHERE location = :location AND forecastDate BETWEEN :startDate AND :endDate ORDER BY forecastDate ASC")
    List<WeatherEntity> getWeatherForLocationNameBetweenDates(String location, Date startDate, Date endDate);
    
    @Query("SELECT * FROM weather WHERE expiryTime < :currentTime")
    List<WeatherEntity> getExpiredWeather(long currentTime);
    
    @Query("DELETE FROM weather WHERE expiryTime < :currentTime")
    void deleteExpiredWeather(long currentTime);
    
    @Query("DELETE FROM weather")
    void deleteAllWeather();
} 