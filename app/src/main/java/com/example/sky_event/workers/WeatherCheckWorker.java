package com.example.sky_event.workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.sky_event.R;
import com.example.sky_event.activities.MainActivity;
import com.example.sky_event.database.entity.EventEntity;
import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.repositories.EventRepository;
import com.example.sky_event.repositories.WeatherRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WeatherCheckWorker extends Worker {
    
    private static final String CHANNEL_ID = "weather_alerts";
    private static final String CHANNEL_NAME = "Weather Alerts";
    private static final int NOTIFICATION_BASE_ID = 2000;
    
    private final Context context;
    private final WeatherRepository weatherRepository;
    private final EventRepository eventRepository;
    private final SimpleDateFormat dateFormat;
    private final NotificationManager notificationManager;
    
    public WeatherCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.weatherRepository = WeatherRepository.getInstance(context);
        this.eventRepository = EventRepository.getInstance(context);
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        String eventId = getInputData().getString("event_id");
        double latitude = getInputData().getDouble("latitude", 0);
        double longitude = getInputData().getDouble("longitude", 0);
        
        if (eventId != null && !eventId.isEmpty()) {
            try {
                EventEntity event = eventRepository.getEventByIdSync(eventId);
                if (event != null) {
                    checkEventWeather(event);
                    return Result.success();
                }
            } catch (Exception e) {
                return Result.retry();
            }
        } else {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return Result.success();
            }
            
            String userId = currentUser.getUid();
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            tomorrow.set(Calendar.HOUR_OF_DAY, 0);
            tomorrow.set(Calendar.MINUTE, 0);
            tomorrow.set(Calendar.SECOND, 0);
            tomorrow.set(Calendar.MILLISECOND, 0);
            
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.DAY_OF_YEAR, 7);
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);
            
            try {
                List<EventEntity> events = eventRepository.getEventsBetweenDatesSync(
                        userId, tomorrow.getTime(), endDate.getTime());
                
                for (EventEntity event : events) {
                    checkEventWeather(event);
                }
                
                return Result.success();
            } catch (Exception e) {
                return Result.retry();
            }
        }
        
        return Result.success();
    }
    
    private void checkEventWeather(EventEntity event) throws ExecutionException, InterruptedException {
        Calendar eventDay = Calendar.getInstance();
        eventDay.setTime(event.getDate());
        eventDay.set(Calendar.HOUR_OF_DAY, 12);
        eventDay.set(Calendar.MINUTE, 0);
        eventDay.set(Calendar.SECOND, 0);
        
        Calendar today = Calendar.getInstance();
        
        if (today.get(Calendar.YEAR) == eventDay.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == eventDay.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        
        long daysDiff = TimeUnit.MILLISECONDS.toDays(
                eventDay.getTimeInMillis() - today.getTimeInMillis());
        
        if (daysDiff <= 1) {
            WeatherEntity weatherForEvent = weatherRepository.getWeatherForLocationAndDate(
                    event.getLatitude(), event.getLongitude(), eventDay.getTime());
            
            if (weatherForEvent != null) {
                WeatherCondition condition = event.getWeatherCondition();
                boolean isSuitable = condition.isSuitableWeather(
                        weatherForEvent.getTemperature(),
                        weatherForEvent.getWindSpeed(),
                        weatherForEvent.getMainCondition(),
                        weatherForEvent.isHasRain());
                
                if (!isSuitable) {
                    List<Date> alternativeDates = findAlternativeDates(event);
                    sendWeatherAlert(event, weatherForEvent, alternativeDates);
                }
            }
        }
    }
    
    private List<Date> findAlternativeDates(EventEntity event) {
        List<Date> alternativeDates = new ArrayList<>();
        
        Calendar startSearch = Calendar.getInstance();
        startSearch.add(Calendar.DAY_OF_YEAR, 2);
        
        Calendar endSearch = Calendar.getInstance();
        endSearch.add(Calendar.DAY_OF_YEAR, 7);
        
        List<WeatherEntity> forecasts = weatherRepository.getWeatherForLocationBetweenDatesSync(
                event.getLatitude(), event.getLongitude(), startSearch.getTime(), endSearch.getTime());
        
        WeatherCondition condition = event.getWeatherCondition();
        
        for (WeatherEntity forecast : forecasts) {
            boolean isSuitable = condition.isSuitableWeather(
                    forecast.getTemperature(),
                    forecast.getWindSpeed(),
                    forecast.getMainCondition(),
                    forecast.isHasRain());
            
            if (isSuitable) {
                alternativeDates.add(forecast.getForecastDate());
                if (alternativeDates.size() >= 3) {
                    break;
                }
            }
        }
        
        return alternativeDates;
    }
    
    private void sendWeatherAlert(EventEntity event, WeatherEntity weatherEntity, List<Date> alternativeDates) {
        createNotificationChannel();
        
        String title = "Неподходящая погода для мероприятия \"" + event.getName() + "\"";
        
        StringBuilder message = new StringBuilder();
        message.append("На ").append(dateFormat.format(event.getDate()))
                .append(" ожидается: ")
                .append(weatherEntity.getDescription())
                .append(", ")
                .append(Math.round(weatherEntity.getTemperature()))
                .append("°C, ветер ")
                .append(Math.round(weatherEntity.getWindSpeed()))
                .append(" м/с");
        
        if (!alternativeDates.isEmpty()) {
            message.append("\n\nРекомендуемые даты:");
            for (Date date : alternativeDates) {
                message.append("\n• ").append(dateFormat.format(date));
            }
        }
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("eventId", event.getId());
        intent.putExtra("notification", true);
        intent.setAction("OPEN_EVENT_DETAILS");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        
        PendingIntent pendingIntent;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        pendingIntent = PendingIntent.getActivity(context, requestCode, intent, flags);
        
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        int notificationId = NOTIFICATION_BASE_ID + event.getId().hashCode();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            
            channel.setDescription("Уведомления о неподходящей погоде для запланированных мероприятий");
            
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            channel.setSound(defaultSoundUri, audioAttributes);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
} 