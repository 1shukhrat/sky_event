package com.example.sky_event.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import com.example.sky_event.database.entity.EventEntity;
import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.repositories.EventRepository;
import com.example.sky_event.repositories.WeatherRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class WeatherSyncWorker extends Worker {
    
    private final Context context;
    private final EventRepository eventRepository;
    private final WeatherRepository weatherRepository;
    private final Map<String, WeatherEntity> previousWeatherData = new HashMap<>();
    
    public WeatherSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.eventRepository = EventRepository.getInstance(context);
        this.weatherRepository = WeatherRepository.getInstance(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return Result.success();
        }
        
        String userId = currentUser.getUid();
        
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_YEAR, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_YEAR, 14);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        
        try {
            List<EventEntity> events = eventRepository.getEventsBetweenDatesSync(
                    userId, startDate.getTime(), endDate.getTime());
            
            for (EventEntity event : events) {
                syncEventWeather(event);
            }
            
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
    
    private void syncEventWeather(EventEntity event) throws ExecutionException, InterruptedException {
        if (event.getLatitude() == 0 && event.getLongitude() == 0) {
            return;
        }
        
        WeatherEntity currentWeather = weatherRepository.syncWeatherForLocation(
                event.getLatitude(), event.getLongitude());
        
        if (currentWeather != null) {
            String eventKey = event.getId();
            WeatherEntity previousWeather = previousWeatherData.get(eventKey);
            
            if (previousWeather != null) {
                boolean weatherChanged = isWeatherChanged(previousWeather, currentWeather);
                
                if (weatherChanged) {
                    WeatherCondition condition = event.getWeatherCondition();
                    
                    boolean wasSuitable = condition.isSuitableWeather(
                            previousWeather.getTemperature(),
                            previousWeather.getWindSpeed(),
                            previousWeather.getMainCondition(),
                            previousWeather.isHasPrecipitation());
                    
                    boolean isSuitable = condition.isSuitableWeather(
                            currentWeather.getTemperature(),
                            currentWeather.getWindSpeed(),
                            currentWeather.getMainCondition(),
                            currentWeather.isHasPrecipitation());
                    
                    if (wasSuitable != isSuitable) {
                        scheduleImmediateWeatherCheck(event);
                    }
                }
            }
            
            previousWeatherData.put(eventKey, currentWeather);
        }
    }
    
    private boolean isWeatherChanged(WeatherEntity previous, WeatherEntity current) {
        if (previous == null || current == null) {
            return true;
        }
        
        boolean tempChanged = Math.abs(previous.getTemperature() - current.getTemperature()) > 3;
        boolean windChanged = Math.abs(previous.getWindSpeed() - current.getWindSpeed()) > 2;
        boolean rainChanged = previous.isHasPrecipitation() != current.isHasPrecipitation();
        
        return tempChanged || windChanged || rainChanged;
    }
    
    private void scheduleImmediateWeatherCheck(EventEntity event) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        Data inputData = new Data.Builder()
                .putString("event_id", event.getId())
                .putDouble("latitude", event.getLatitude())
                .putDouble("longitude", event.getLongitude())
                .build();
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherCheckWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();
        
        WorkManager.getInstance(context).enqueueUniqueWork(
                "immediate_weather_check_" + event.getId(),
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }
} 