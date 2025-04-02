package com.example.sky_event.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;

import com.example.sky_event.R;
import com.example.sky_event.activities.MainActivity;
import com.example.sky_event.database.entity.EventEntity;
import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.repositories.EventRepository;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.utils.LiveDataBus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherMonitoringService extends Service {
    
    private static final String TAG = "WeatherMonitoringService";
    private static final String CHANNEL_ID = "weather_monitoring_channel";
    private static final String CHANNEL_NAME = "Мониторинг погоды";
    private static final int NOTIFICATION_ID = 1001;
    private static final int CHECK_INTERVAL_MINUTES = 15;
    
    private static final String WEATHER_ALERT_CHANNEL_ID = "weather_alert_channel";
    private static final String WEATHER_ALERT_CHANNEL_NAME = "Погодные предупреждения";
    
    private static final String EVENT_REMINDER_CHANNEL_ID = "event_reminder_channel";
    private static final String EVENT_REMINDER_CHANNEL_NAME = "Напоминания о событиях";
    
    private static final String WEATHER_CHANGE_CHANNEL_ID = "weather_change_channel";
    private static final String WEATHER_CHANGE_CHANNEL_NAME = "Изменения погоды";
    
    private static final int NOTIFICATION_TYPE_WEATHER_ALERT = 1;
    private static final int NOTIFICATION_TYPE_EVENT_REMINDER = 2;
    private static final int NOTIFICATION_TYPE_WEATHER_CHANGE = 3;
    
    private static final String PREFS_NAME = "WeatherMonitoringPrefs";
    private static final String KEY_SERVICE_NOTIFICATION_SHOWN = "service_notification_shown";
    
    private WeatherRepository weatherRepository;
    private EventRepository eventRepository;
    private ScheduledExecutorService scheduler;
    private PowerManager.WakeLock wakeLock;
    private final Map<String, WeatherEntity> lastWeatherForEvents = new HashMap<>();
    private SimpleDateFormat dateFormat;
    
    @Override
    public void onCreate() {
        super.onCreate();
        weatherRepository = WeatherRepository.getInstance(this);
        eventRepository = EventRepository.getInstance(this);
        dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
                "SkyEvent:WeatherMonitoringWakeLock");
        
        createNotificationChannels();
        startForeground(NOTIFICATION_ID, createServiceNotification());
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("check_event_id")) {
            String eventId = intent.getStringExtra("check_event_id");
            if (eventId != null && !eventId.isEmpty()) {
                checkEventById(eventId);
            }
        }
        
        startMonitoring();
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        stopMonitoring();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
    
    private void startMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.scheduleWithFixedDelay(this::checkWeatherForEvents, 0,
                    CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
    }
    
    private void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void checkWeatherForEvents() {
        try {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L); // 10 минут максимум
            }
            
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return;
            }
            
            String userId = currentUser.getUid();
            
            // Получаем события на ближайшие 7 дней
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.DAY_OF_YEAR, 7);
            
            List<EventEntity> events = eventRepository.getEventsBetweenDatesSync(
                    userId, Calendar.getInstance().getTime(), endDate.getTime());
            
            for (EventEntity event : events) {
                if (event.getLatitude() == 0 && event.getLongitude() == 0) {
                    continue;
                }
                
                checkEventWeatherConditions(event);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Ошибка при проверке погоды: " + e.getMessage());
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
    
    private void checkEventWeatherConditions(EventEntity event) {
        String eventKey = event.getId();
        
        // Получаем актуальные данные о погоде для локации события
        WeatherEntity currentWeather = weatherRepository.syncWeatherForLocation(
                event.getLatitude(), event.getLongitude());
        
        if (currentWeather == null) {
            return;
        }
        
        WeatherCondition condition = event.getWeatherCondition();
        
        condition.setCurrentTemperature(currentWeather.getTemperature());
        condition.setCurrentWindSpeed(currentWeather.getWindSpeed());
        condition.setCurrentHumidity(currentWeather.getHumidity());
        
        boolean isSuitable = condition.isSuitableWeather(
                currentWeather.getTemperature(),
                currentWeather.getWindSpeed(),
                currentWeather.getMainCondition(),
                currentWeather.isHasRain(),
                currentWeather.getHumidity());
        
        boolean weatherChanged = false;
        WeatherEntity lastWeather = lastWeatherForEvents.get(eventKey);
        
        if (lastWeather != null) {
            weatherChanged = isWeatherChanged(lastWeather, currentWeather);
        }
        
        lastWeatherForEvents.put(eventKey, currentWeather); 
        boolean isNewEvent = false;
        if (event.getCreatedAt() != null) {
            long creationTimeMillis = event.getCreatedAt().getTime();
            long currentTimeMillis = System.currentTimeMillis();
            long timeDifferenceMinutes = (currentTimeMillis - creationTimeMillis) / (1000 * 60);
            isNewEvent = timeDifferenceMinutes < 30; // Считаем новым, если создано менее 30 минут назад
        }
        
        // Отправляем уведомление, если:
        // 1. Погода не соответствует и изменилась с последней проверки
        // 2. Это новое событие и погода не соответствует
        // 3. У нас нет записи о предыдущей погоде
        if (!isSuitable && (weatherChanged || lastWeather == null || isNewEvent)) {
            sendWeatherAlert(event, currentWeather);
            
            // Оповещаем фрагменты о необходимости обновить UI
            notifyUIUpdate(event, currentWeather);
        }
    }
    
    private boolean isWeatherChanged(WeatherEntity previous, WeatherEntity current) {
        boolean tempChanged = Math.abs(previous.getTemperature() - current.getTemperature()) > 2;
        boolean windChanged = Math.abs(previous.getWindSpeed() - current.getWindSpeed()) > 1.5;
        boolean rainChanged = previous.isHasRain() != current.isHasRain();
        boolean conditionChanged = !previous.getMainCondition().equals(current.getMainCondition());
        
        return tempChanged || windChanged || rainChanged || conditionChanged;
    }
    
    private void sendWeatherAlert(EventEntity event, WeatherEntity weatherEntity) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        String title = "Неподходящая погода для \"" + event.getName() + "\"";
        
        StringBuilder message = new StringBuilder();
        message.append("Текущие условия: ")
                .append(weatherEntity.getDescription())
                .append(", ")
                .append(Math.round(weatherEntity.getTemperature()))
                .append("°C, ветер ")
                .append(Math.round(weatherEntity.getWindSpeed()))
                .append(" м/с");
                
        if (weatherEntity.isHasRain()) {
            message.append(", осадки");
        }
        
        message.append("\nЭти условия не соответствуют заданным критериям события.");
        List<Date> alternativeDates = findAlternativeDates(event);
        if (!alternativeDates.isEmpty()) {
            message.append("\n\nРекомендуемые даты:");
            for (Date date : alternativeDates) {
                message.append("\n• ").append(dateFormat.format(date));
            }
        }
        
        sendNotification(title, message.toString(), event.getId(), NOTIFICATION_TYPE_WEATHER_ALERT);
        
        // Оповещаем фрагменты о необходимости обновить UI
        notifyUIUpdate(event, weatherEntity);
    }
    
    private void sendNotification(String title, String content, String eventId, int notificationType) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("notification", true);
        intent.setAction("OPEN_EVENT_DETAILS");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        
        PendingIntent pendingIntent;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        pendingIntent = PendingIntent.getActivity(this, requestCode, intent, flags);
        
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder;
        
        switch (notificationType) {
            case NOTIFICATION_TYPE_WEATHER_ALERT:
                notificationBuilder = new NotificationCompat.Builder(this, WEATHER_ALERT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setColor(getResources().getColor(R.color.weather_alert_color, getTheme()));
                break;
            case NOTIFICATION_TYPE_EVENT_REMINDER:
                notificationBuilder = new NotificationCompat.Builder(this, EVENT_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setColor(getResources().getColor(R.color.event_reminder_color, getTheme()));
                break;
            case NOTIFICATION_TYPE_WEATHER_CHANGE:
                notificationBuilder = new NotificationCompat.Builder(this, WEATHER_CHANGE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setColor(getResources().getColor(R.color.weather_change_color, getTheme()));
                break;
            default:
                notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                break;
        }
        
        int notificationId = eventId.hashCode() + 3000;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
    
    public void sendEventReminder(EventEntity event, long timeToEventMinutes) {
        String title = "Напоминание о событии: " + event.getName();
        
        StringBuilder message = new StringBuilder();
        if (timeToEventMinutes < 60) {
            message.append("Ваше событие начнется через ")
                  .append(timeToEventMinutes)
                  .append(" минут");
        } else {
            message.append("Ваше событие начнется через ")
                  .append(timeToEventMinutes / 60)
                  .append(" часов");
        }
        
        message.append("\nДата: ")
              .append(dateFormat.format(event.getDate()));
        
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            message.append("\nМесто: ")
                  .append(event.getLocation());
        }
        
        WeatherEntity weatherEntity = weatherRepository.syncWeatherForLocation(
                event.getLatitude(), event.getLongitude());
        
        if (weatherEntity != null) {
            message.append("\n\nПрогноз погоды: ")
                  .append(weatherEntity.getDescription())
                  .append(", ")
                  .append(Math.round(weatherEntity.getTemperature()))
                  .append("°C");
        }
        
        sendNotification(title, message.toString(), event.getId(), NOTIFICATION_TYPE_EVENT_REMINDER);
    }
    
    public void sendWeatherChangeNotification(EventEntity event, WeatherEntity newWeather, WeatherEntity oldWeather) {
        String title = "Изменение погоды для события: " + event.getName();
        
        StringBuilder message = new StringBuilder();
        message.append("Изменение погодных условий в месте проведения события:\n\n");
        
        message.append("Было: ")
              .append(oldWeather.getDescription())
              .append(", ")
              .append(Math.round(oldWeather.getTemperature()))
              .append("°C, ветер ")
              .append(Math.round(oldWeather.getWindSpeed()))
              .append(" м/с");
        
        if (oldWeather.isHasRain()) {
            message.append(", осадки");
        }
        
        message.append("\n\nСтало: ")
              .append(newWeather.getDescription())
              .append(", ")
              .append(Math.round(newWeather.getTemperature()))
              .append("°C, ветер ")
              .append(Math.round(newWeather.getWindSpeed()))
              .append(" м/с");
        
        if (newWeather.isHasRain()) {
            message.append(", осадки");
        }
        
        WeatherCondition condition = event.getWeatherCondition();
        boolean isSuitable = condition.isSuitableWeather(
                newWeather.getTemperature(),
                newWeather.getWindSpeed(),
                newWeather.getMainCondition(),
                newWeather.isHasRain(),
                newWeather.getHumidity());
        
        message.append("\n\nТекущие условия ");
        message.append(isSuitable ? "соответствуют" : "не соответствуют");
        message.append(" заданным критериям события.");
        
        sendNotification(title, message.toString(), event.getId(), NOTIFICATION_TYPE_WEATHER_CHANGE);
    }
    
    private List<Date> findAlternativeDates(EventEntity event) {
        List<Date> alternativeDates = new ArrayList<>();
        
        Calendar startSearch = Calendar.getInstance();
        startSearch.add(Calendar.DAY_OF_YEAR, 1); // с завтрашнего дня
        
        Calendar endSearch = Calendar.getInstance();
        endSearch.add(Calendar.DAY_OF_YEAR, 7); // до 7 дней вперед
        
        List<WeatherEntity> forecasts = weatherRepository.getWeatherForLocationBetweenDatesSync(
                event.getLatitude(), event.getLongitude(), startSearch.getTime(), endSearch.getTime());
        
        WeatherCondition condition = event.getWeatherCondition();
        
        for (WeatherEntity forecast : forecasts) {
            boolean isSuitable = condition.isSuitableWeather(
                    forecast.getTemperature(),
                    forecast.getWindSpeed(),
                    forecast.getMainCondition(),
                    forecast.isHasRain(),
                    forecast.getHumidity());
            
            if (isSuitable) {
                alternativeDates.add(forecast.getForecastDate());
                if (alternativeDates.size() >= 3) {
                    break;
                }
            }
        }
        
        return alternativeDates;
    }
    
    private void notifyUIUpdate(EventEntity event, WeatherEntity weather) {
        // Используем LiveDataBus для уведомления фрагментов о необходимости обновления UI
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getId());
        data.put("temperature", weather.getTemperature());
        data.put("windSpeed", weather.getWindSpeed());
        data.put("condition", weather.getMainCondition());
        data.put("hasRain", weather.isHasRain());
        data.put("humidity", weather.getHumidity());
        data.put("isSuitable", false);
        
        LiveDataBus.getInstance().with("weather_condition_changed").setValue(data);
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            // Канал для сервиса
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            
            serviceChannel.setDescription("Канал для уведомлений мониторинга погоды");
            serviceChannel.setSound(null, null);
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);
            
            // Канал для предупреждений о погоде
            NotificationChannel weatherAlertChannel = new NotificationChannel(
                    WEATHER_ALERT_CHANNEL_ID,
                    WEATHER_ALERT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            
            weatherAlertChannel.setDescription("Канал для уведомлений о неподходящих погодных условиях");
            weatherAlertChannel.setSound(defaultSoundUri, audioAttributes);
            weatherAlertChannel.enableLights(true);
            weatherAlertChannel.enableVibration(true);
            weatherAlertChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            // Канал для напоминаний о событиях
            NotificationChannel eventReminderChannel = new NotificationChannel(
                    EVENT_REMINDER_CHANNEL_ID,
                    EVENT_REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            
            eventReminderChannel.setDescription("Канал для напоминаний о предстоящих событиях");
            eventReminderChannel.setSound(defaultSoundUri, audioAttributes);
            eventReminderChannel.enableLights(true);
            eventReminderChannel.enableVibration(true);
            eventReminderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            // Канал для уведомлений об изменении погоды
            NotificationChannel weatherChangeChannel = new NotificationChannel(
                    WEATHER_CHANGE_CHANNEL_ID,
                    WEATHER_CHANGE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            
            weatherChangeChannel.setDescription("Канал для уведомлений об изменениях погоды");
            weatherChangeChannel.setSound(defaultSoundUri, audioAttributes);
            weatherChangeChannel.enableLights(true);
            weatherChangeChannel.enableVibration(true);
            
            notificationManager.createNotificationChannel(serviceChannel);
            notificationManager.createNotificationChannel(weatherAlertChannel);
            notificationManager.createNotificationChannel(eventReminderChannel);
            notificationManager.createNotificationChannel(weatherChangeChannel);
        }
    }
    
    private Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationShown = prefs.getBoolean(KEY_SERVICE_NOTIFICATION_SHOWN, false);
        
        if (!notificationShown) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_SERVICE_NOTIFICATION_SHOWN, true);
            editor.apply();
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Мониторинг погоды")
                .setContentText("Отслеживаем погодные условия для ваших событий")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);
                
        if (!notificationShown) {
            builder.setAutoCancel(true);
        } else {
            builder.setOngoing(true)
                  .setVisibility(NotificationCompat.VISIBILITY_SECRET);
        }
        
        return builder.build();
    }
    
    private void checkEventById(String eventId) {
        try {
            EventEntity event = eventRepository.getEventByIdSync(eventId);
            if (event != null && event.getLatitude() != 0 && event.getLongitude() != 0) {
                checkEventWeatherConditions(event);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Ошибка при проверке события: " + e.getMessage());
        }
    }
} 