package com.example.sky_event.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sky_event.models.weather.CurrentWeather;
import com.example.sky_event.models.weather.DailyForecast;
import com.example.sky_event.models.weather.HourlyForecast;
import com.example.sky_event.models.weather.MonthlyForecast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    private static final String PREF_NAME = "sky_event_data";
    private static final String KEY_CURRENT_WEATHER = "current_weather";
    private static final String KEY_HOURLY_FORECAST = "hourly_forecast";
    private static final String KEY_DAILY_FORECAST = "daily_forecast";
    private static final String KEY_MONTHLY_FORECAST = "monthly_forecast";
    
    private static DataRepository instance;
    private final SharedPreferences preferences;
    private final Gson gson;
    
    private CurrentWeather cachedCurrentWeather;
    private List<HourlyForecast> cachedHourlyForecast;
    private List<DailyForecast> cachedDailyForecast;
    private List<MonthlyForecast> cachedMonthlyForecast;
    
    private DataRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCachedData();
    }
    
    public static synchronized DataRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DataRepository(context);
        }
        return instance;
    }
    
    private void loadCachedData() {
        String currentWeatherJson = preferences.getString(KEY_CURRENT_WEATHER, null);
        if (currentWeatherJson != null) {
            cachedCurrentWeather = gson.fromJson(currentWeatherJson, CurrentWeather.class);
        }
        
        String hourlyForecastJson = preferences.getString(KEY_HOURLY_FORECAST, null);
        if (hourlyForecastJson != null) {
            Type type = new TypeToken<List<HourlyForecast>>(){}.getType();
            cachedHourlyForecast = gson.fromJson(hourlyForecastJson, type);
        } else {
            cachedHourlyForecast = new ArrayList<>();
        }
        
        String dailyForecastJson = preferences.getString(KEY_DAILY_FORECAST, null);
        if (dailyForecastJson != null) {
            Type type = new TypeToken<List<DailyForecast>>(){}.getType();
            cachedDailyForecast = gson.fromJson(dailyForecastJson, type);
        } else {
            cachedDailyForecast = new ArrayList<>();
        }
        
        String monthlyForecastJson = preferences.getString(KEY_MONTHLY_FORECAST, null);
        if (monthlyForecastJson != null) {
            Type type = new TypeToken<List<MonthlyForecast>>(){}.getType();
            cachedMonthlyForecast = gson.fromJson(monthlyForecastJson, type);
        } else {
            cachedMonthlyForecast = new ArrayList<>();
        }
    }
    
    public void saveCurrentWeather(CurrentWeather weather) {
        cachedCurrentWeather = weather;
        String json = gson.toJson(weather);
        preferences.edit().putString(KEY_CURRENT_WEATHER, json).apply();
    }
    
    public void saveHourlyForecast(List<HourlyForecast> forecast) {
        cachedHourlyForecast = forecast;
        String json = gson.toJson(forecast);
        preferences.edit().putString(KEY_HOURLY_FORECAST, json).apply();
    }
    
    public void saveDailyForecast(List<DailyForecast> forecast) {
        cachedDailyForecast = forecast;
        String json = gson.toJson(forecast);
        preferences.edit().putString(KEY_DAILY_FORECAST, json).apply();
    }
    
    public void saveMonthlyForecast(List<MonthlyForecast> forecast) {
        cachedMonthlyForecast = forecast;
        String json = gson.toJson(forecast);
        preferences.edit().putString(KEY_MONTHLY_FORECAST, json).apply();
    }
    
    public CurrentWeather getCurrentWeather() {
        return cachedCurrentWeather;
    }
    
    public List<HourlyForecast> getHourlyForecast() {
        if (cachedHourlyForecast == null) {
            return new ArrayList<>();
        }
        return cachedHourlyForecast;
    }
    
    public List<DailyForecast> getDailyForecast() {
        if (cachedDailyForecast == null) {
            return new ArrayList<>();
        }
        return cachedDailyForecast;
    }
    
    public List<MonthlyForecast> getMonthlyForecast() {
        if (cachedMonthlyForecast == null) {
            return new ArrayList<>();
        }
        return cachedMonthlyForecast;
    }
    
    public void clearData() {
        cachedCurrentWeather = null;
        cachedHourlyForecast = null;
        cachedDailyForecast = null;
        cachedMonthlyForecast = null;
        preferences.edit()
                .remove(KEY_CURRENT_WEATHER)
                .remove(KEY_HOURLY_FORECAST)
                .remove(KEY_DAILY_FORECAST)
                .remove(KEY_MONTHLY_FORECAST)
                .apply();
    }
} 