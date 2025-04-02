package com.example.sky_event.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.sky_event.api.RetrofitClient;
import com.example.sky_event.api.WeatherService;
import com.example.sky_event.models.weather.WeatherResponse;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.utils.Constants;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherWidgetUpdateService extends IntentService {
    
    public WeatherWidgetUpdateService() {
        super("WeatherWidgetUpdateService");
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        String location = prefs.getString(Constants.PREF_WIDGET_LOCATION, "");
        float latitude = prefs.getFloat(Constants.PREF_WIDGET_LAT, 0);
        float longitude = prefs.getFloat(Constants.PREF_WIDGET_LON, 0);
        
        if (latitude != 0 && longitude != 0) {
            updateWeatherData(latitude, longitude, location);
        } else if (!location.isEmpty()) {
            getCoordinatesFromLocation(location);
        }
    }
    
    private void updateWeatherData(float latitude, float longitude, String location) {
        WeatherRepository repository = WeatherRepository.getInstance(this);
        repository.getForecast(latitude, longitude);
        
        Intent updateIntent = new Intent(this, WeatherWidget.class);
        updateIntent.setAction(Constants.WIDGET_UPDATE_ACTION);
        updateIntent.putExtra(Constants.EXTRA_WIDGET_LAT, latitude);
        updateIntent.putExtra(Constants.EXTRA_WIDGET_LON, longitude);
        sendBroadcast(updateIntent);
    }
    
    private void getCoordinatesFromLocation(String location) {
        WeatherService weatherService = RetrofitClient.getClient().create(WeatherService.class);
        weatherService.getCurrentWeatherByCity(location, Constants.OPEN_WEATHER_API_KEY, "metric", "ru")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherResponse = response.body();
                            if (weatherResponse.getCoord() != null) {
                                float lat = (float) weatherResponse.getCoord().getLat();
                                float lon = (float) weatherResponse.getCoord().getLon();
                                
                                SharedPreferences.Editor editor = getSharedPreferences(
                                        Constants.PREF_NAME, Context.MODE_PRIVATE).edit();
                                editor.putFloat(Constants.PREF_WIDGET_LAT, lat);
                                editor.putFloat(Constants.PREF_WIDGET_LON, lon);
                                editor.apply();
                                
                                updateWeatherData(lat, lon, location);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        broadcastWidgetUpdate();
                    }
                });
    }
    
    private void broadcastWidgetUpdate() {
        Intent updateIntent = new Intent(this, WeatherWidget.class);
        updateIntent.setAction(Constants.WIDGET_UPDATE_ACTION);
        sendBroadcast(updateIntent);
    }
} 