package com.example.sky_event.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.sky_event.activities.MainActivity;
import com.example.sky_event.R;
import com.example.sky_event.models.notification.Notification;
import com.example.sky_event.repositories.NotificationRepository;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.Map;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    
    private static final String TAG = "FirebaseMsgService";
    private static final String CHANNEL_ID = "weather_notifications";
    private static final String CHANNEL_NAME = "Weather Notifications";
    private static final int NOTIFICATION_ID = 1000;
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String message = data.get("message");
        String userId = data.get("userId");
        String eventId = data.get("eventId");
        
        if (title != null && message != null && userId != null) {
            sendNotification(title, message, eventId);
            saveNotificationToDatabase(userId, title, message, eventId);
        }
    }
    
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }
    
    private void sendNotification(String title, String messageBody, String eventId) {
        Intent intent = createNotificationIntent(eventId);
        PendingIntent pendingIntent = createPendingIntent(intent);
        
        createNotificationChannel();
        
        NotificationCompat.Builder notificationBuilder = buildNotification(title, messageBody, pendingIntent);
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        int notificationId = eventId != null ? NOTIFICATION_ID + eventId.hashCode() : NOTIFICATION_ID;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
    
    private Intent createNotificationIntent(String eventId) {
        Intent intent = new Intent(this, MainActivity.class);
        if (eventId != null && !eventId.isEmpty()) {
            intent.putExtra("eventId", eventId);
            intent.putExtra("notification", true);
            intent.setAction("OPEN_EVENT_DETAILS");
        } else {
            intent.setAction("OPEN_MAIN");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
    
    private PendingIntent createPendingIntent(Intent intent) {
        int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getActivity(this, requestCode, intent, flags);
    }
    
    private NotificationCompat.Builder buildNotification(String title, String messageBody, PendingIntent pendingIntent) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            
            channel.setDescription("Уведомления о событиях и погоде");
            
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            channel.setSound(defaultSoundUri, audioAttributes);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void saveNotificationToDatabase(String userId, String title, String message, String eventId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventId(eventId);
        notification.setTimestamp(new Date());
        notification.setRead(false);
        
        NotificationRepository notificationRepository = new NotificationRepository();
        notificationRepository.addNotification(notification, new NotificationRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Уведомление сохранено в базу данных");
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка при сохранении уведомления: " + e.getMessage());
            }
        });
    }
} 