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
import com.example.sky_event.database.dao.EventDao;
import com.example.sky_event.database.dao.EventDao_Impl;
import com.example.sky_event.database.entity.EventEntity;
import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.models.weather.DailyForecast;
import com.example.sky_event.models.weather.DailyForecastResponse;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.models.weather.HourlyForecastResponse;
import com.example.sky_event.network.WeatherApi;
import com.example.sky_event.repositories.EventRepository;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        
        if (eventId != null && !eventId.isEmpty()) {
            try {
                EventEntity event = eventRepository.getEventByIdSync(eventId);
                if (event != null) {
                    //checkEventWeather(event);
                    return Result.success();
                }
            } catch (Exception e) {
                return Result.retry();
            }
        } else {
            try {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    return Result.success();
                }

                String userId = currentUser.getUid();
                List<EventEntity> events = eventRepository.getEventsByUserId(userId);
                for (EventEntity event: events) {
                    getWeatherForEvent(event, new Date());
                }
                return Result.success();
            } catch (Exception ex) {
                return Result.failure();
            }

//            Calendar tomorrow = Calendar.getInstance();
//            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
//            tomorrow.set(Calendar.HOUR_OF_DAY, 0);
//            tomorrow.set(Calendar.MINUTE, 0);
//            tomorrow.set(Calendar.SECOND, 0);
//            tomorrow.set(Calendar.MILLISECOND, 0);
//
//            Calendar endDate = Calendar.getInstance();
//            endDate.add(Calendar.DAY_OF_YEAR, 7);
//            endDate.set(Calendar.HOUR_OF_DAY, 23);
//            endDate.set(Calendar.MINUTE, 59);
//            endDate.set(Calendar.SECOND, 59);
//
//            try {
//                List<EventEntity> events = eventRepository.getEventsBetweenDatesSync(
//                        userId, tomorrow.getTime(), endDate.getTime());
//
//                for (EventEntity event : events) {
//                    checkEventWeather(event);
//                }
//
//                return Result.success();
//            } catch (Exception e) {
//                return Result.retry();
//            }
        }
        
        return Result.success();
    }


    public void getWeatherForEvent(EventEntity event, Date currentTime) {
        long daysDiff = getDaysDifference(currentTime, event.getDate());

        try {
            StringJoiner stringJoiner = new StringJoiner("\n");
            if (daysDiff <= 4) {
                HourlyForecastResponse hourlyData = weatherRepository.getHourlyForecastFor4Days(event.getLatitude(), event.getLongitude());
                HourlyForecastResponse.ForecastItem closestTime =  findClosestOneHourly(hourlyData.getList(), event.getDate());
                if (event.getWeatherCondition().getMinTemperature() > closestTime.getMain().getTemp()) {
                    stringJoiner.add("Пониженная температура " + closestTime.getMain().getTemp() + " °C");
                }
                if (event.getWeatherCondition().getMaxTemperature() < closestTime.getMain().getTemp()) {
                    stringJoiner.add("Повышенная температура " + closestTime.getMain().getTemp() + " °C");
                }
                if (event.getWeatherCondition().getMaxWindSpeed() < closestTime.getWind().getSpeed()) {
                    stringJoiner.add("Повышенная скорость ветра " + closestTime.getWind().getSpeed() + " м/c");
                }
                if (event.getWeatherCondition().isNoRain()) {
                    if (closestTime.getRain() != null) {
                        stringJoiner.add("Ожидается дождь");
                    }
                }
            } else if (daysDiff <= 5) {
                ForecastResponse threeHourlyData = weatherRepository.get3HourForecastFor5Days(event.getLatitude(), event.getLongitude());
                ForecastResponse.ForecastItem closestTime =  findClosestThreeHourly(threeHourlyData.getList(), event.getDate());
                if (event.getWeatherCondition().getMinTemperature() > closestTime.getMain().getTemp()) {
                    stringJoiner.add("Пониженная температура " + closestTime.getMain().getTemp() + " °C");
                }
                if (event.getWeatherCondition().getMaxTemperature() < closestTime.getMain().getTemp()) {
                    stringJoiner.add("Повышенная температура " + closestTime.getMain().getTemp() + " °C");
                }
                if (event.getWeatherCondition().getMaxWindSpeed() < closestTime.getWind().getSpeed()) {
                    stringJoiner.add("Повышенная скорость ветра " + closestTime.getWind().getSpeed() + " м/c");
                }
                if (event.getWeatherCondition().isNoRain()) {
                    if (closestTime.getRain() != null) {
                        stringJoiner.add("Дождь");
                    }
                }
            } else if (daysDiff <= 16) {
                DailyForecastResponse dailyData = weatherRepository.getDailyForecastFor16Days(event.getLatitude(), event.getLongitude());
                DailyForecastResponse.WeatherData closestDay = findClosestDaily(dailyData.getList(), event.getDate());
                float closestTimeTemp = getNearestTimeBlock(closestDay, event.getDate());
                if (event.getWeatherCondition().getMinTemperature() > closestTimeTemp) {
                    stringJoiner.add("Пониженная температура " + closestTimeTemp + " °C");
                }
                if (event.getWeatherCondition().getMaxTemperature() < closestTimeTemp) {
                    stringJoiner.add("Повышенная температура " + closestTimeTemp + " °C");
                }
                if (event.getWeatherCondition().getMaxWindSpeed() < closestDay.getSpeed()) {
                    stringJoiner.add("Повышенная скорость ветра " + closestDay.getSpeed() + " м/c");
                }
                if (event.getWeatherCondition().isNoRain()) {
                    if (closestDay.getRain() != null) {
                        stringJoiner.add("Дождь");
                    }
                } else {
                    return;
                }
            }
            if (stringJoiner.length() != 0) {
                sendWeatherAlert(event, stringJoiner.toString(), Collections.emptyList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Расчет разницы в днях между датами
    private long getDaysDifference(Date date1, Date date2) {
        long diffInMillis = Math.abs(date2.getTime() - date1.getTime());
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    // Поиск ближайшего замера для почасовых данных
    private ForecastResponse.ForecastItem findClosestThreeHourly(List<ForecastResponse.ForecastItem> forecasts, Date target) {
        ForecastResponse.ForecastItem closest = null;
        long minDifference = Long.MAX_VALUE;

        for (ForecastResponse.ForecastItem data : forecasts) {
            long diff = Math.abs((data.getDt() * 1000) - target.getTime());
            if (diff < minDifference) {
                minDifference = diff;
                closest = data;
            }
        }
        return closest;
    }

    private HourlyForecastResponse.ForecastItem findClosestOneHourly(List<HourlyForecastResponse.ForecastItem> forecasts, Date target) {
        HourlyForecastResponse.ForecastItem closest = null;
        long minDifference = Long.MAX_VALUE;

        for (HourlyForecastResponse.ForecastItem data : forecasts) {
            long diff = Math.abs((data.getDt() * 1000) - target.getTime());
            if (diff < minDifference) {
                minDifference = diff;
                closest = data;
            }
        }
        return closest;
    }

    // Поиск ближайшего временного блока для дневного прогноза
    private DailyForecastResponse.WeatherData findClosestDaily(List<DailyForecastResponse.WeatherData> forecasts, Date target) {
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(target);
        int targetYear = targetCal.get(Calendar.YEAR);
        int targetDay = targetCal.get(Calendar.DAY_OF_YEAR);

        for (DailyForecastResponse.WeatherData forecast : forecasts) {
            Calendar forecastCal = Calendar.getInstance();
            forecastCal.setTime(new Date(forecast.getDt() * 1000));

            if (forecastCal.get(Calendar.YEAR) == targetYear &&
                    forecastCal.get(Calendar.DAY_OF_YEAR) == targetDay) {
                return forecast;
            }
        }
        return null;
    }

    // Определение ближайшего временного блока в дневном прогнозе
    private float getNearestTimeBlock(DailyForecastResponse.WeatherData daily, Date target) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(daily.getDt() * 1000));

        // Создаем временные точки для блоков
        Date[] blockTimes = new Date[4];
        float[] blocks = {
                daily.getTemp().getMorn(),
                daily.getTemp().getDay(),
                daily.getTemp().getEve(),
                daily.getTemp().getNight()
        };

        // Утро - 6:00
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        blockTimes[0] = cal.getTime();

        // День - 12:00
        cal.set(Calendar.HOUR_OF_DAY, 12);
        blockTimes[1] = cal.getTime();

        // Вечер - 18:00
        cal.set(Calendar.HOUR_OF_DAY, 18);
        blockTimes[2] = cal.getTime();

        // Ночь - 00:00 следующего дня
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        blockTimes[3] = cal.getTime();

        // Находим ближайший блок
        int closestIndex = 0;
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < blockTimes.length; i++) {
            long diff = Math.abs(blockTimes[i].getTime() - target.getTime());
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }
        return blocks[closestIndex];
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
                        weatherForEvent.isHasPrecipitation());
                
                if (!isSuitable) {
//                    List<Date> alternativeDates = findAlternativeDates(event);
//                    sendWeatherAlert(event, weatherForEvent, alternativeDates);
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
                    forecast.isHasPrecipitation());
            
            if (isSuitable) {
                alternativeDates.add(forecast.getForecastDate());
                if (alternativeDates.size() >= 3) {
                    break;
                }
            }
        }
        
        return alternativeDates;
    }
    
    private void sendWeatherAlert(EventEntity event, String text, List<Date> alternativeDates) {
        createNotificationChannel();
        
        String title = "Неподходящая погода для \"" + event.getName() + "\"";
        
        StringBuilder message = new StringBuilder();
        message.append("На ").append(dateFormat.format(event.getDate()))
                .append(" ожидается: ")
                .append(text);
        
//        if (!alternativeDates.isEmpty()) {
//            message.append("\n\nРекомендуемые даты:");
//            for (Date date : alternativeDates) {
//                message.append("\n• ").append(dateFormat.format(date));
//            }
//        }
        
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
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message.toString()))
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