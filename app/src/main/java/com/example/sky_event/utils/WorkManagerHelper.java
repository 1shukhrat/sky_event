package com.example.sky_event.utils;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.workers.DateSuggestionWorker;
import com.example.sky_event.workers.WeatherCheckWorker;
import com.example.sky_event.workers.WeatherSyncWorker;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WorkManagerHelper {
    
    private static final String WEATHER_CHECK_WORK = "weather_check_work";
    private static final String DATE_SUGGESTION_WORK = "date_suggestion_work";
    
    public static void scheduleWeatherCheckWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WeatherCheckWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WEATHER_CHECK_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }
    
    public static UUID runDateSuggestionWorker(Context context, double latitude, double longitude, 
                                           String location, WeatherCondition weatherCondition) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        Data inputData = new Data.Builder()
                .putDouble("latitude", latitude)
                .putDouble("longitude", longitude)
                .putString("location", location)
                .putDouble("min_temp", weatherCondition.getMinTemperature())
                .putDouble("max_temp", weatherCondition.getMaxTemperature())
                .putDouble("max_wind_speed", weatherCondition.getMaxWindSpeed())
                .putBoolean("no_rain", weatherCondition.isNoRain())
                .putStringArray("allowed_conditions", 
                        weatherCondition.getAllowedConditions().toArray(new String[0]))
                .build();
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DateSuggestionWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();
        
        WorkManager.getInstance(context).enqueueUniqueWork(
                DATE_SUGGESTION_WORK,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
        
        return workRequest.getId();
    }
    
    public static WorkInfo getWorkInfo(Context context, UUID workId) throws ExecutionException, InterruptedException {
        return WorkManager.getInstance(context).getWorkInfoById(workId).get();
    }
    
    public static List<WorkInfo> getWorkInfosByTag(Context context, String tag) throws ExecutionException, InterruptedException {
        return WorkManager.getInstance(context).getWorkInfosByTag(tag).get();
    }
    
    public static void cancelAllWork(Context context) {
        WorkManager.getInstance(context).cancelAllWork();
    }
    
    public static void cancelWorkById(Context context, UUID workId) {
        WorkManager.getInstance(context).cancelWorkById(workId);
    }
    
    public static void cancelWeatherCheckWork(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEATHER_CHECK_WORK);
    }
    
    public static void scheduleWeatherSyncWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WeatherSyncWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "weather_sync_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }
    
    public static void scheduleImmediateWeatherCheck(Context context, String eventId, double latitude, double longitude) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        Data inputData = new Data.Builder()
                .putString("event_id", eventId)
                .putDouble("latitude", latitude)
                .putDouble("longitude", longitude)
                .build();
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherCheckWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();
        
        WorkManager.getInstance(context).enqueueUniqueWork(
                "immediate_weather_check_" + eventId,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }
    
    public static void updateWeatherCheckFrequency(Context context, int hours) {
        cancelWeatherCheckWork(context);
        
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WeatherCheckWorker.class, hours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WEATHER_CHECK_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }
} 