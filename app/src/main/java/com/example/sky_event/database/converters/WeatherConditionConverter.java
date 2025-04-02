package com.example.sky_event.database.converters;

import androidx.room.TypeConverter;

import com.example.sky_event.models.event.WeatherCondition;
import com.google.gson.Gson;

public class WeatherConditionConverter {
    
    @TypeConverter
    public static WeatherCondition fromString(String value) {
        if (value == null) {
            return new WeatherCondition();
        }
        return new Gson().fromJson(value, WeatherCondition.class);
    }
    
    @TypeConverter
    public static String fromWeatherCondition(WeatherCondition condition) {
        if (condition == null) {
            return new Gson().toJson(new WeatherCondition());
        }
        return new Gson().toJson(condition);
    }
} 