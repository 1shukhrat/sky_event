package com.example.sky_event.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.sky_event.services.WeatherMonitoringService;
import com.example.sky_event.utils.WorkManagerHelper;
import com.google.firebase.auth.FirebaseAuth;

public class BootCompletedReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            
            // Убедимся, что пользователь авторизован
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // Запускаем периодические задачи WorkManager
                WorkManagerHelper.scheduleWeatherCheckWorker(context);
                WorkManagerHelper.scheduleWeatherSyncWorker(context);
                
                // Запускаем сервис мониторинга погоды
                Intent serviceIntent = new Intent(context, WeatherMonitoringService.class);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
} 