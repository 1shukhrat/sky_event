package com.example.sky_event.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.repositories.WeatherRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DateSuggestionWorker extends Worker {
    
    private final Context context;
    private final WeatherRepository weatherRepository;
    
    public DateSuggestionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.weatherRepository = WeatherRepository.getInstance(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            double latitude = getInputData().getDouble("latitude", 0);
            double longitude = getInputData().getDouble("longitude", 0);
            String location = getInputData().getString("location");
            double minTemp = getInputData().getDouble("min_temp", 15);
            double maxTemp = getInputData().getDouble("max_temp", 30);
            double maxWindSpeed = getInputData().getDouble("max_wind_speed", 10);
            boolean noRain = getInputData().getBoolean("no_rain", true);
            String[] allowedConditionsArray = getInputData().getStringArray("allowed_conditions");
            
            List<String> allowedConditions = new ArrayList<>();
            if (allowedConditionsArray != null) {
                for (String condition : allowedConditionsArray) {
                    allowedConditions.add(condition);
                }
            }
            
            WeatherCondition weatherCondition = new WeatherCondition();
            weatherCondition.setMinTemperature(minTemp);
            weatherCondition.setMaxTemperature(maxTemp);
            weatherCondition.setMaxWindSpeed(maxWindSpeed);
            weatherCondition.setNoRain(noRain);
            weatherCondition.setAllowedConditions(allowedConditions);
            
            List<Date> suggestedDates = findOptimalDates(latitude, longitude, location, weatherCondition);
            
            if (!suggestedDates.isEmpty()) {
                long[] timestamps = new long[suggestedDates.size()];
                for (int i = 0; i < suggestedDates.size(); i++) {
                    timestamps[i] = suggestedDates.get(i).getTime();
                }
                
                Data outputData = new Data.Builder()
                        .putLongArray("suggested_dates", timestamps)
                        .build();
                
                return Result.success(outputData);
            } else {
                return Result.failure();
            }
        } catch (Exception e) {
            return Result.failure();
        }
    }
    
    private List<Date> findOptimalDates(double latitude, double longitude, String location, WeatherCondition condition) {
        List<Date> suggestedDates = new ArrayList<>();
        
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_YEAR, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_YEAR, 14);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        
        List<WeatherEntity> forecasts;
        
        if (latitude != 0 && longitude != 0) {
            forecasts = weatherRepository.getWeatherForLocationBetweenDatesSync(
                    latitude, longitude, startDate.getTime(), endDate.getTime());
        } else if (location != null && !location.isEmpty()) {
            forecasts = weatherRepository.getWeatherForLocationNameBetweenDatesSync(
                    location, startDate.getTime(), endDate.getTime());
        } else {
            return suggestedDates;
        }
        
        for (WeatherEntity forecast : forecasts) {
            boolean isSuitable = condition.isSuitableWeather(
                    forecast.getTemperature(),
                    forecast.getWindSpeed(),
                    forecast.getMainCondition(),
                    forecast.isHasPrecipitation());
            
            if (isSuitable) {
                Calendar forecastDate = Calendar.getInstance();
                forecastDate.setTime(forecast.getForecastDate());
                forecastDate.set(Calendar.HOUR_OF_DAY, 12);
                forecastDate.set(Calendar.MINUTE, 0);
                forecastDate.set(Calendar.SECOND, 0);
                
                suggestedDates.add(forecastDate.getTime());
                
                if (suggestedDates.size() >= 5) {
                    break;
                }
            }
        }
        
        return suggestedDates;
    }
    
    private List<Date> prioritizeDates(List<Date> dates) {
        dates.sort((date1, date2) -> {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(date1);
            
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(date2);
            
            Calendar now = Calendar.getInstance();
            
            boolean isWeekend1 = cal1.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                    cal1.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            
            boolean isWeekend2 = cal2.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                    cal2.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            
            if (isWeekend1 && !isWeekend2) {
                return -1;
            } else if (!isWeekend1 && isWeekend2) {
                return 1;
            }
            
            long diff1 = Math.abs(cal1.getTimeInMillis() - now.getTimeInMillis());
            long diff2 = Math.abs(cal2.getTimeInMillis() - now.getTimeInMillis());
            
            long daysDiff1 = TimeUnit.MILLISECONDS.toDays(diff1);
            long daysDiff2 = TimeUnit.MILLISECONDS.toDays(diff2);
            
            return Long.compare(daysDiff1, daysDiff2);
        });
        
        return dates;
    }
} 