package com.example.sky_event;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.example.sky_event.database.AppDatabase;
import com.example.sky_event.services.FirebaseMessagingService;
import com.example.sky_event.utils.AuthManager;
import com.example.sky_event.utils.Constants;
import com.example.sky_event.utils.WorkManagerHelper;
import com.google.firebase.FirebaseApp;
import com.yandex.mapkit.MapKitFactory;

public class SkyEventApplication extends Application implements Configuration.Provider {
    
    private static SkyEventApplication instance;
    private boolean preloadCompleted = false;
    private boolean isFirstLaunch = true;
    
    public static SkyEventApplication getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        FirebaseApp.initializeApp(this);
        MapKitFactory.setApiKey(Constants.YANDEX_MAPS_API_KEY);
        
        AuthManager.getInstance();
        AppDatabase.clearDatabase(this);
        
        createNotificationChannels();
        
        WorkManagerHelper.scheduleWeatherCheckWorker(this);
        WorkManagerHelper.scheduleWeatherSyncWorker(this);
        
        startWeatherMonitoringService();
    }
    
    private void startWeatherMonitoringService() {
        Intent serviceIntent = new Intent(this, com.example.sky_event.services.WeatherMonitoringService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    public boolean isPreloadCompleted() {
        return preloadCompleted;
    }
    
    public void setPreloadCompleted(boolean completed) {
        this.preloadCompleted = completed;
    }
    
    public boolean isFirstLaunch() {
        return isFirstLaunch;
    }
    
    public void setFirstLaunch(boolean firstLaunch) {
        isFirstLaunch = firstLaunch;
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            NotificationChannel weatherAlertsChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            weatherAlertsChannel.setDescription(Constants.NOTIFICATION_CHANNEL_DESC);
            weatherAlertsChannel.setSound(defaultSoundUri, audioAttributes);
            weatherAlertsChannel.enableLights(true);
            weatherAlertsChannel.enableVibration(true);
            weatherAlertsChannel.setShowBadge(true);
            
            NotificationChannel firebaseChannel = new NotificationChannel(
                    "weather_notifications",
                    "Weather Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            firebaseChannel.setDescription("Уведомления о событиях и погоде");
            firebaseChannel.setSound(defaultSoundUri, audioAttributes);
            firebaseChannel.enableLights(true);
            firebaseChannel.enableVibration(true);
            firebaseChannel.setShowBadge(true);
            
            notificationManager.createNotificationChannel(weatherAlertsChannel);
            notificationManager.createNotificationChannel(firebaseChannel);
        }
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
    
    public void terminateApplication() {
        SharedPreferences prefs = getSharedPreferences("create_event_data", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
} 