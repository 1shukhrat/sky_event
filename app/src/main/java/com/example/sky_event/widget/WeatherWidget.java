package com.example.sky_event.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.sky_event.activities.MainActivity;
import com.example.sky_event.R;
import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.utils.Constants;
import com.example.sky_event.utils.DateFormatter;

import java.util.Date;
import java.util.List;

public class WeatherWidget extends AppWidgetProvider {
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    @Override
    public void onEnabled(Context context) {
        Intent intent = new Intent(context, WeatherWidgetUpdateService.class);
        context.startService(intent);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        if (Constants.WIDGET_UPDATE_ACTION.equals(intent.getAction())) {
            WeatherRepository repository = WeatherRepository.getInstance(context);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, WeatherWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
    
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        String location = prefs.getString(Constants.PREF_WIDGET_LOCATION, "");
        double latitude = prefs.getFloat(Constants.PREF_WIDGET_LAT, 0);
        double longitude = prefs.getFloat(Constants.PREF_WIDGET_LON, 0);
        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        
        Intent updateIntent = new Intent(context, WeatherWidgetUpdateService.class);
        PendingIntent updatePendingIntent = PendingIntent.getService(
                context, 0, updateIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_button, updatePendingIntent);
        
        if (latitude != 0 && longitude != 0) {
            WeatherRepository repository = WeatherRepository.getInstance(context);
            loadWeatherData(context, views, repository, latitude, longitude, location);
        } else {
            views.setTextViewText(R.id.widget_location, "Выберите локацию в приложении");
            views.setTextViewText(R.id.widget_temperature, "---");
            views.setTextViewText(R.id.widget_description, "");
            views.setTextViewText(R.id.widget_date, DateFormatter.formatDateForDisplay(new Date()));
        }
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    private static void loadWeatherData(Context context, RemoteViews views, WeatherRepository repository,
                                        double latitude, double longitude, String location) {
        Date today = new Date();
        List<WeatherEntity> weatherEntities = repository.getWeatherForLocationBetweenDatesSync(
                latitude, longitude, today, today);
        
        if (weatherEntities != null && !weatherEntities.isEmpty()) {
            WeatherEntity weatherEntity = weatherEntities.get(0);
            
            views.setTextViewText(R.id.widget_location, location);
            views.setTextViewText(R.id.widget_temperature, 
                    Math.round(weatherEntity.getTemperature()) + "°C");
            views.setTextViewText(R.id.widget_description, weatherEntity.getDescription());
            views.setTextViewText(R.id.widget_date, DateFormatter.formatDateForDisplay(today));
            
            int weatherIconResId = getWeatherIcon(weatherEntity.getIcon());
            views.setImageViewResource(R.id.widget_weather_icon, weatherIconResId);
            
            boolean isGoodWeather = !weatherEntity.isHasPrecipitation() &&
                    weatherEntity.getTemperature() > 15 && 
                    weatherEntity.getTemperature() < 30 && 
                    weatherEntity.getWindSpeed() < 10;
            
            views.setTextViewText(R.id.widget_weather_recommendation, 
                    isGoodWeather ? "Хорошая погода для мероприятий" : "Не лучшая погода для мероприятий");
        } else {
            views.setTextViewText(R.id.widget_location, location);
            views.setTextViewText(R.id.widget_temperature, "---");
            views.setTextViewText(R.id.widget_description, "Нет данных");
            views.setTextViewText(R.id.widget_date, DateFormatter.formatDateForDisplay(today));
            views.setTextViewText(R.id.widget_weather_recommendation, "");
            views.setImageViewResource(R.id.widget_weather_icon, R.drawable.ic_weather_unknown);
        }
    }
    
    private static int getWeatherIcon(String weatherId) {
        if (weatherId == null) {
            return R.drawable.ic_weather_unknown;
        }
        
        int id = Integer.parseInt(weatherId);
        if (id >= 200 && id < 300) {
            return R.drawable.ic_weather_lightning;
        } else if (id >= 300 && id < 400) {
            return R.drawable.ic_weather_rainy;
        } else if (id >= 500 && id < 600) {
            if (id >= 502) {
                return R.drawable.ic_weather_pouring;
            } else {
                return R.drawable.ic_weather_rainy;
            }
        } else if (id >= 600 && id < 700) {
            return R.drawable.ic_weather_snowy;
        } else if (id >= 700 && id < 800) {
            return R.drawable.ic_weather_fog;
        } else if (id == 800) {
            return R.drawable.ic_weather_sunny;
        } else if (id == 801) {
            return R.drawable.ic_weather_partly_cloudy_day;
        } else if (id == 802) {
            return R.drawable.ic_weather_partly_cloudy_night;
        } else if (id >= 803) {
            return R.drawable.ic_weather_cloudy;
        }
        
        return R.drawable.ic_weather_unknown;
    }
} 