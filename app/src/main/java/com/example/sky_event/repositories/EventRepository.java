package com.example.sky_event.repositories;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sky_event.database.AppDatabase;
import com.example.sky_event.database.dao.EventDao;
import com.example.sky_event.database.entity.EventEntity;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.models.weather.CurrentWeatherResponse;
import com.example.sky_event.utils.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import android.content.Intent;

public class EventRepository {
    
    private static final String EVENTS_COLLECTION = "events";
    private static final String PARTICIPANTS_COLLECTION = "event_participants";
    
    private final FirebaseFirestore firestore;
    private final CollectionReference eventsCollection;
    private final EventDao eventDao;
    private final AppExecutors executors;
    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    private final MutableLiveData<Event> event = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private static EventRepository instance;
    
    private final Context context;
    private final Executor executor;
    private final Handler mainHandler;
    
    public static EventRepository getInstance(Context context) {
        if (instance == null) {
            instance = new EventRepository(context);
        }
        return instance;
    }

    
    public EventRepository(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.eventsCollection = firestore.collection(EVENTS_COLLECTION);
        this.eventDao = AppDatabase.getInstance(context).eventDao();
        this.executors = AppExecutors.getInstance();
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<List<Event>> getEvents() {
        return events;
    }

    public LiveData<Event> getEvent() {
        return event;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void fetchUserEvents() {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.postValue("Пользователь не авторизован");
            return;
        }
        
        fetchEventsFromFirestore(userId);
    }

    public List<EventEntity> getEventsByUserId(String userId) {
        return eventDao.getAllEventsSync(userId);
    }
    
    public void fetchEvent(String eventId) {
        eventsCollection.document(eventId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Event eventObj = documentSnapshot.toObject(Event.class);
                    eventObj.setId(documentSnapshot.getId());
                    event.postValue(eventObj);
                } else {
                    error.postValue("Событие не найдено");
                }
            })
            .addOnFailureListener(e -> error.postValue("Ошибка при загрузке события: " + e.getMessage()));
    }
    
    public void findOptimalDate(ForecastResponse forecastResponse, Event event) {
        if (forecastResponse == null || event == null || event.getWeatherCondition() == null) {
            error.postValue("Недостаточно данных для подбора оптимальной даты");
            return;
        }

        executors.diskIO().execute(() -> {
            try {
                Date optimalDate = findOptimalDateTime(forecastResponse, event.getWeatherCondition());
                if (optimalDate != null) {
                    event.setDate(optimalDate);
                    this.event.postValue(event);
                } else {
                    error.postValue("Не удалось найти подходящую дату в ближайшие дни");
                }
            } catch (Exception e) {
                error.postValue("Ошибка при поиске оптимальной даты: " + e.getMessage());
            }
        });
    }
    
    private Date findOptimalDateTime(ForecastResponse forecastResponse, WeatherCondition conditions) {
        if (forecastResponse == null || forecastResponse.getList() == null || 
                forecastResponse.getList().isEmpty() || conditions == null) {
            return null;
        }

        List<TimeSlot> timeSlots = new ArrayList<>();
        
        for (ForecastResponse.ForecastItem item : forecastResponse.getList()) {
            try {
                Date timestamp = parseDateTime(item.getDt_txt());
                if (timestamp == null) continue;
                
                double score = scoreTimeSlot(item, timestamp, conditions);
                timeSlots.add(new TimeSlot(timestamp, score));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обработке прогноза: " + e.getMessage());
            }
        }
        
        if (timeSlots.isEmpty()) {
            return null;
        }
        
        sortTimeSlotsByPreference(timeSlots);
        
        TimeSlot bestSlot = timeSlots.get(0);
        if (bestSlot.score < 50) {
            return null;
        }
        
        return bestSlot.timestamp;
    }

    private static class TimeSlot {
        final Date timestamp;
        final double score;
        
        TimeSlot(Date timestamp, double score) {
            this.timestamp = timestamp;
            this.score = score;
        }
    }

    private Date parseDateTime(String datetimeString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return format.parse(datetimeString);
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка парсинга даты: " + e.getMessage());
            return null;
        }
    }

    private double scoreTimeSlot(ForecastResponse.ForecastItem item, Date timestamp, WeatherCondition conditions) {
        if (item == null || item.getMain() == null || timestamp == null) {
            return 0;
        }
        
        double baseScore = 100.0;
        
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        calendar.setTime(timestamp);
        
        if (calendar.before(now)) {
            return 0;
        }
        
        int daysInFuture = (int)((calendar.getTimeInMillis() - now.getTimeInMillis()) / (24 * 60 * 60 * 1000));
        
        if (daysInFuture > 5) {
            baseScore -= (daysInFuture - 5) * 3;
        }
        
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int month = calendar.get(Calendar.MONTH);
        
        boolean isSummer = month >= Calendar.JUNE && month <= Calendar.AUGUST;
        boolean isWinter = month == Calendar.DECEMBER || month == Calendar.JANUARY || month == Calendar.FEBRUARY;
        boolean isSpring = month >= Calendar.MARCH && month <= Calendar.MAY;
        boolean isFall = month >= Calendar.SEPTEMBER && month <= Calendar.NOVEMBER;
        
        boolean isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
        boolean isEvening = hourOfDay >= 17 && hourOfDay <= 20;
        boolean isMorning = hourOfDay >= 6 && hourOfDay <= 10;
        boolean isMidday = hourOfDay >= 11 && hourOfDay <= 16;
        boolean isNight = hourOfDay > 20 || hourOfDay < 6;
        
        double temperature = item.getMain().getTemp() - 273.15;
        double windSpeed = item.getWind() != null ? item.getWind().getSpeed() : 0;
        double humidity = safeGetHumidity(item);
        double feelsLike = safeGetFeelsLike(item);
        int cloudiness = safeGetCloudiness(item);
        boolean hasRain = item.getRain() != null;
        boolean hasSnow = hasSnowPrecipitation(item);
        
        double weatherQualityScore = evaluateWeatherQuality(temperature, windSpeed, humidity, cloudiness, hasRain, hasSnow, feelsLike);
        double seasonalFactor = calculateSeasonalFactor(temperature, isSummer, isWinter, isSpring, isFall);
        double timeOfDayScore = calculateTimeOfDayScore(hourOfDay, isSummer, isWinter);
        
        if (isWeekend) {
            baseScore += 15;
        } else if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.MONDAY) {
            baseScore += 8;
        }
        
        if (isNight) {
            baseScore -= 25;
        } else if (isMorning) {
            if (isWeekend) {
                baseScore += 2;
            } else {
                baseScore += 8;
            }
        } else if (isMidday) {
            baseScore += 12;
        } else if (isEvening) {
            if (isWeekend) {
                baseScore += 10;
            } else {
                baseScore += 15;
            }
        }
        
        baseScore *= seasonalFactor;
        baseScore += weatherQualityScore;
        baseScore += timeOfDayScore;
        
        baseScore = applyUserConditions(baseScore, temperature, windSpeed, hasRain, conditions);
        
        return Math.max(0, Math.min(100, baseScore));
    }

    private boolean hasSnowPrecipitation(ForecastResponse.ForecastItem item) {
        if (item.getWeather() == null || item.getWeather().isEmpty()) {
            return false;
        }
        
        for (CurrentWeatherResponse.Weather weather : item.getWeather()) {
            String main = weather.getMain();
            String description = weather.getDescription();
            
            if (main != null && main.equalsIgnoreCase("Snow")) {
                return true;
            }
            
            if (description != null && (
                description.toLowerCase().contains("snow") || 
                description.toLowerCase().contains("снег"))) {
                return true;
            }
        }
        
        return false;
    }

    private double safeGetHumidity(ForecastResponse.ForecastItem item) {
        try {
            return item.getMain().getHumidity();
        } catch (Exception e) {
            return 50;
        }
    }

    private double safeGetFeelsLike(ForecastResponse.ForecastItem item) {
        try {
            return item.getMain().getFeels_like() - 273.15;
        } catch (Exception e) {
            return item.getMain().getTemp() - 273.15;
        }
    }

    private int safeGetCloudiness(ForecastResponse.ForecastItem item) {
        try {
            if (item.getClouds() != null) {
                return item.getClouds().getAll();
            }
            return 50;
        } catch (Exception e) {
            return 50;
        }
    }

    private double evaluateWeatherQuality(double temperature, double windSpeed, double humidity, int cloudiness, boolean hasRain, boolean hasSnow, double feelsLike) {
        double score = 0;
        
        if (temperature >= 18 && temperature <= 26) {
            score += 15;
        } else if (temperature >= 15 && temperature < 18) {
            score += 10;
        } else if (temperature > 26 && temperature <= 30) {
            score += 5;
        } else if ((temperature >= 10 && temperature < 15) || (temperature > 30 && temperature <= 33)) {
            score += 0;
        } else if ((temperature >= 5 && temperature < 10) || (temperature > 33 && temperature <= 36)) {
            score -= 10;
        } else {
            score -= 20;
        }
        
        double tempDiff = Math.abs(temperature - feelsLike);
        if (tempDiff > 5) {
            score -= (tempDiff - 5) * 2;
        }
        
        if (windSpeed <= 2) {
            score += 10;
        } else if (windSpeed > 2 && windSpeed <= 5) {
            score += 5;
        } else if (windSpeed > 5 && windSpeed <= 8) {
            score += 0;
        } else if (windSpeed > 8 && windSpeed <= 12) {
            score -= 15;
        } else {
            score -= 30;
        }
        
        if (humidity < 40) {
            score += 8;
        } else if (humidity >= 40 && humidity <= 60) {
            score += 12;
        } else if (humidity > 60 && humidity <= 75) {
            score += 5;
        } else if (humidity > 75 && humidity <= 85) {
            score -= 5;
        } else {
            score -= 10;
        }
        
        if (cloudiness <= 20) {
            score += 15;
        } else if (cloudiness > 20 && cloudiness <= 50) {
            score += 10;
        } else if (cloudiness > 50 && cloudiness <= 70) {
            score += 0;
        } else if (cloudiness > 70 && cloudiness <= 90) {
            score -= 5;
        } else {
            score -= 10;
        }
        
        if (hasRain) {
            score -= 30;
        }
        
        if (hasSnow) {
            score -= 25;
        }
        
        return score;
    }

    private double calculateSeasonalFactor(double temperature, boolean isSummer, boolean isWinter, boolean isSpring, boolean isFall) {
        if (isWinter) {
            if (temperature < 0) {
                return 0.7;
            } else if (temperature >= 0 && temperature <= 5) {
                return 0.75;
            } else {
                return 0.85;
            }
        } else if (isSummer) {
            if (temperature > 30) {
                return 0.8;
            } else if (temperature >= 25 && temperature <= 30) {
                return 0.95;
            } else {
                return 1.0;
            }
        } else if (isSpring) {
            if (temperature < 10) {
                return 0.8;
            } else if (temperature >= 10 && temperature <= 20) {
                return 0.9;
            } else {
                return 1.0;
            }
        } else if (isFall) {
            if (temperature < 8) {
                return 0.75;
            } else if (temperature >= 8 && temperature <= 15) {
                return 0.85;
            } else {
                return 0.95;
            }
        }
        
        return 1.0;
    }

    private double calculateTimeOfDayScore(int hourOfDay, boolean isSummer, boolean isWinter) {
        if (isSummer) {
            if (hourOfDay >= 5 && hourOfDay <= 10) {
                return 15;
            } else if (hourOfDay >= 19 && hourOfDay <= 22) {
                return 12;
            } else if (hourOfDay > 10 && hourOfDay < 16) {
                return 5;
            } else if (hourOfDay >= 16 && hourOfDay < 19) {
                return 8;
            }
        } else if (isWinter) {
            if (hourOfDay >= 10 && hourOfDay <= 15) {
                return 15;
            } else if (hourOfDay > 15 && hourOfDay <= 18) {
                return 8;
            } else if (hourOfDay >= 7 && hourOfDay < 10) {
                return 5;
            }
        } else {
            if (hourOfDay >= 9 && hourOfDay <= 17) {
                return 12;
            } else if (hourOfDay > 17 && hourOfDay <= 20) {
                return 8;
            } else if (hourOfDay >= 6 && hourOfDay < 9) {
                return 5;
            }
        }
        
        return 0;
    }

    private double applyUserConditions(double baseScore, double temperature, double windSpeed, boolean hasRain, WeatherCondition conditions) {
        double score = baseScore;
        
        if (conditions.getMinTemperature() > 0 && temperature < conditions.getMinTemperature()) {
            double tempDiff = conditions.getMinTemperature() - temperature;
            double penalty = Math.min(60, tempDiff * 6);
            score -= penalty;
        }
        
        if (conditions.getMaxTemperature() > 0 && temperature > conditions.getMaxTemperature()) {
            double tempDiff = temperature - conditions.getMaxTemperature();
            double penalty = Math.min(60, tempDiff * 6);
            score -= penalty;
        }
        
        if (conditions.getMaxWindSpeed() > 0 && windSpeed > conditions.getMaxWindSpeed()) {
            double windDiff = windSpeed - conditions.getMaxWindSpeed();
            double penalty = Math.min(50, windDiff * 7);
            score -= penalty;
        }
        
        if (conditions.isNoRain() && hasRain) {
            score -= 70;
        }
        
        return score;
    }

    private void sortTimeSlotsByPreference(List<TimeSlot> timeSlots) {
        Collections.sort(timeSlots, (a, b) -> {
            double scoreDiff = b.score - a.score;
            
            if (Math.abs(scoreDiff) >= 20) {
                return scoreDiff > 0 ? 1 : -1;
            }
            
            Calendar calA = Calendar.getInstance();
            Calendar calB = Calendar.getInstance();
            calA.setTime(a.timestamp);
            calB.setTime(b.timestamp);
            
            int dayOfWeekA = calA.get(Calendar.DAY_OF_WEEK);
            int dayOfWeekB = calB.get(Calendar.DAY_OF_WEEK);
            
            boolean isWeekendA = (dayOfWeekA == Calendar.SATURDAY || dayOfWeekA == Calendar.SUNDAY);
            boolean isWeekendB = (dayOfWeekB == Calendar.SATURDAY || dayOfWeekB == Calendar.SUNDAY);
            
            if (isWeekendA && !isWeekendB) {
                return -1;
            }
            if (!isWeekendA && isWeekendB) {
                return 1;
            }
            
            int hourA = calA.get(Calendar.HOUR_OF_DAY);
            int hourB = calB.get(Calendar.HOUR_OF_DAY);
            
            boolean isDaytimeA = hourA >= 9 && hourA <= 19;
            boolean isDaytimeB = hourB >= 9 && hourB <= 19;
            
            if (isDaytimeA && !isDaytimeB) {
                return -1;
            }
            if (!isDaytimeA && isDaytimeB) {
                return 1;
            }
            
            if (Math.abs(scoreDiff) >= 8) {
                return scoreDiff > 0 ? 1 : -1;
            }
            
            long daysToA = (calA.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / (24 * 60 * 60 * 1000);
            long daysToB = (calB.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / (24 * 60 * 60 * 1000);
            
            if (daysToA != daysToB) {
                return Long.compare(daysToA, daysToB);
            }
            
            if (dayOfWeekA == Calendar.SATURDAY || dayOfWeekA == Calendar.SUNDAY) {
                boolean isMorningToEveningA = hourA >= 10 && hourA <= 18;
                boolean isMorningToEveningB = hourB >= 10 && hourB <= 18;
                
                if (isMorningToEveningA && !isMorningToEveningB) {
                    return -1;
                }
                if (!isMorningToEveningA && isMorningToEveningB) {
                    return 1;
                }
            } else {
                boolean isEveningA = hourA >= 17 && hourA <= 21;
                boolean isEveningB = hourB >= 17 && hourB <= 21;
                
                if (isEveningA && !isEveningB) {
                    return -1;
                }
                if (!isEveningA && isEveningB) {
                    return 1;
                }
            }
            
            return Double.compare(b.score, a.score);
        });
    }
    
    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ? 
               FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }
    
    public List<EventEntity> getEventsBetweenDatesSync(String userId, Date startDate, Date endDate) 
            throws ExecutionException, InterruptedException {
        Future<LiveData<List<EventEntity>>> future = executors.diskIO().submit(() -> 
                eventDao.getEventsBetweenDates(userId, startDate, endDate));
        
        LiveData<List<EventEntity>> liveData = future.get();
        MutableLiveData<List<EventEntity>> result = new MutableLiveData<>();
        
        liveData.observeForever(eventEntities -> {
            result.postValue(eventEntities);
        });
        
        if (result.getValue() != null) {
            return result.getValue();
        }
        
        return new ArrayList<>();
    }

    public List<EventEntity> getEvents(String userId)
            throws ExecutionException, InterruptedException {
        return eventDao.getAllEventsSync(userId);

//        LiveData<List<EventEntity>> liveData = future.get();
//        MutableLiveData<List<EventEntity>> result = new MutableLiveData<>();
//
//        liveData.observeForever(eventEntities -> {
//            result.postValue(eventEntities);
//        });
//
//        if (result.getValue() != null) {
//            return result.getValue();
//        }
//
//        return new ArrayList<>();
    }
    
    public void saveEvent(Event event) {
        String eventId = event.getId();
        if (eventId == null || eventId.isEmpty()) {
            eventId = UUID.randomUUID().toString();
            event.setId(eventId);
        }
        
        if (event.getUserId() == null || event.getUserId().isEmpty()) {
            String userId = getCurrentUserId();
            if (userId != null) {
                event.setUserId(userId);
            } else {
                error.postValue("Пользователь не авторизован");
                return;
            }
        }
        
        if (event.getName() == null) event.setName("");
        if (event.getDescription() == null) event.setDescription("");
        if (event.getLocation() == null) event.setLocation("");
        if (event.getWeatherCondition() == null) event.setWeatherCondition(new WeatherCondition());
        
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(Timestamp.now());
        }
        
        Event finalEvent = event;
        final String finalEventId = eventId;
        
        DocumentReference document = eventsCollection.document(finalEventId);
        
        document.set(finalEvent)
                .addOnSuccessListener(aVoid -> {
                    List<Event> currentEvents = events.getValue();
                    if (currentEvents != null) {
                        List<Event> updatedEvents = new ArrayList<>(currentEvents);
                        boolean found = false;
                        for (int i = 0; i < updatedEvents.size(); i++) {
                            if (updatedEvents.get(i).getId().equals(finalEvent.getId())) {
                                updatedEvents.set(i, finalEvent);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            updatedEvents.add(finalEvent);
                        }
                        events.postValue(updatedEvents);
                    } else {
                        List<Event> newEvents = new ArrayList<>();
                        newEvents.add(finalEvent);
                        events.postValue(newEvents);
                    }
                    
                    EventEntity eventEntity = convertToEventEntity(finalEvent);
                    eventEntity.setSynced(true);
                    executors.diskIO().execute(() -> {
                        eventDao.insert(eventEntity);
                    });
                    
                    runImmediateWeatherCheck(finalEvent);
                })
                .addOnFailureListener(e -> {
                    error.postValue("Ошибка при сохранении события: " + e.getMessage());
                });
    }
    
    private void runImmediateWeatherCheck(Event event) {
        if (context != null && event.getLatitude() != 0 && event.getLongitude() != 0) {
            Intent serviceIntent = new Intent(context, com.example.sky_event.services.WeatherMonitoringService.class);
            serviceIntent.putExtra("check_event_id", event.getId());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
    
    public void updateEvent(Event event) {
        eventsCollection.document(event.getId()).set(event);
        
        EventEntity eventEntity = convertToEventEntity(event);
        executors.diskIO().execute(() -> {
            eventDao.update(eventEntity);
        });
    }
    
    public void deleteEvent(String eventId) {
        eventsCollection.document(eventId).delete();
        
        executors.diskIO().execute(() -> {
            EventEntity eventEntity = eventDao.getEventById(eventId);
            if (eventEntity != null) {
                eventDao.delete(eventEntity);
            }
        });
    }
    
    private void fetchEventsFromFirestore(String userId) {
        eventsCollection.whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> eventList = new ArrayList<>();
                    List<EventEntity> eventEntities = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                        Event event = document.toObject(Event.class);
                            event.setId(document.getId());
                            if (event.getName() == null) event.setName("");
                            if (event.getDescription() == null) event.setDescription("");
                            if (event.getLocation() == null) event.setLocation("");
                            if (event.getWeatherCondition() == null) event.setWeatherCondition(new WeatherCondition());
                            
                            eventList.add(event);
                            
                            EventEntity eventEntity = convertToEventEntity(event);
                            eventEntity.setSynced(true);
                            eventEntities.add(eventEntity);
                        } catch (Exception e) {
                            error.postValue("Ошибка при обработке события: " + e.getMessage());
                        }
                    }
                    
                    events.postValue(eventList);
                    
                    executors.diskIO().execute(() -> {
                        try {
                            for (EventEntity entity : eventEntities) {
                                eventDao.insert(entity);
                            }
                        } catch (Exception e) {
                            error.postValue("Ошибка при сохранении в локальную базу: " + e.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    error.postValue("Ошибка при загрузке событий: " + e.getMessage());
                    loadEventsFromLocalDb(userId);
                });
    }
    
    private void loadEventsFromLocalDb(String userId) {
        executors.diskIO().execute(() -> {
            List<EventEntity> localEvents = eventDao.getAllEventsSync(userId);
            List<Event> eventList = new ArrayList<>();
            
            for (EventEntity entity : localEvents) {
                eventList.add(convertToEvent(entity));
            }
            
            events.postValue(eventList);
        });
    }
    

    
    private EventEntity convertToEventEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setId(event.getId());
        entity.setName(event.getName());
        entity.setDescription(event.getDescription());
        entity.setDate(event.getDate());
        entity.setLocation(event.getLocation());
        entity.setLatitude(event.getLatitude());
        entity.setLongitude(event.getLongitude());
        entity.setWeatherCondition(event.getWeatherCondition());
        entity.setUserId(event.getUserId());
        
        if (event.getCreatedAt() != null) {
            entity.setCreatedAt(event.getCreatedAt());
        }
        
        if (event.getUpdatedAt() != null) {
            entity.setUpdatedAt(event.getUpdatedAt());
        }
        
        return entity;
    }
    
    private Event convertToEvent(EventEntity entity) {
        Event event = new Event();
        event.setId(entity.getId());
        event.setName(entity.getName());
        event.setDescription(entity.getDescription());
        event.setDate(entity.getDate());
        event.setLocation(entity.getLocation());
        event.setLatitude(entity.getLatitude());
        event.setLongitude(entity.getLongitude());
        event.setWeatherCondition(entity.getWeatherCondition());
        event.setUserId(entity.getUserId());
        
        if (entity.getCreatedAt() != null) {
            event.setCreatedAt(entity.getCreatedAt());
        }
        
        if (entity.getUpdatedAt() != null) {
            event.setUpdatedAt(entity.getUpdatedAt());
        }
        
        return event;
    }
    

            
    public EventEntity getEventByIdSync(String eventId) throws ExecutionException, InterruptedException {
        return eventDao.getEventById(eventId);
    }

    public void joinEvent(String eventId, CallbackListener<Boolean> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onCallback(false);
            return;
        }
        
        DocumentReference eventRef = eventsCollection.document(eventId);
        DocumentReference participantRef = firestore.collection(PARTICIPANTS_COLLECTION)
                .document(eventId + "_" + userId);
        
        firestore.runTransaction(transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot participantSnapshot = transaction.get(participantRef);
            
            if (!eventSnapshot.exists()) {
                throw new RuntimeException("Событие не существует");
            }
            
            if (participantSnapshot.exists()) {
                throw new RuntimeException("Пользователь уже присоединился к событию");
            }
            
            Event event = eventSnapshot.toObject(Event.class);
            if (event == null) {
                throw new RuntimeException("Ошибка при получении данных события");
            }
            
            int currentCount = event.getParticipantsCount();
            
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("userId", userId);
            participantData.put("eventId", eventId);
            participantData.put("joinDate", new Date());
            participantData.put("name", getCurrentUserName());
            
            transaction.update(eventRef, "participantsCount", currentCount + 1);
            transaction.set(participantRef, participantData);
            
            return true;
        }).addOnSuccessListener(result -> {
            if (callback != null) callback.onCallback(true);
        }).addOnFailureListener(e -> {
            error.postValue("Ошибка при присоединении к событию: " + e.getMessage());
            if (callback != null) callback.onCallback(false);
        });
    }
    
    public void leaveEvent(String eventId, CallbackListener<Boolean> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onCallback(false);
            return;
        }
        
        DocumentReference eventRef = eventsCollection.document(eventId);
        DocumentReference participantRef = firestore.collection(PARTICIPANTS_COLLECTION)
                .document(eventId + "_" + userId);
        
        firestore.runTransaction(transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot participantSnapshot = transaction.get(participantRef);
            
            if (!eventSnapshot.exists()) {
                throw new RuntimeException("Событие не существует");
            }
            
            if (!participantSnapshot.exists()) {
                throw new RuntimeException("Пользователь не участвует в этом событии");
            }
            
            Event event = eventSnapshot.toObject(Event.class);
            if (event == null) {
                throw new RuntimeException("Ошибка при получении данных события");
            }
            
            int currentCount = event.getParticipantsCount();
            if (currentCount > 0) {
                transaction.update(eventRef, "participantsCount", currentCount - 1);
            }
            
            transaction.delete(participantRef);
            
            return true;
        }).addOnSuccessListener(result -> {
            if (callback != null) callback.onCallback(true);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onCallback(false);
        });
    }
    
    public boolean hasUserJoinedEvent(String eventId) {
        String userId = getCurrentUserId();
        if (userId == null) return false;
        
        try {
            DocumentSnapshot doc = firestore.collection(PARTICIPANTS_COLLECTION)
                    .document(eventId + "_" + userId)
                    .get()
                    .continueWith(task -> {
                        if (task.isSuccessful()) {
                            return task.getResult();
                        } else {
                            throw task.getException();
                        }
                    }).getResult();
            
            return doc != null && doc.exists();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void fetchEventParticipants(String eventId, CallbackListener<List<String>> callback) {
        firestore.collection(PARTICIPANTS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> participants = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            participants.add(name);
                        } else {
                            participants.add("Пользователь");
                        }
                    }
                    if (callback != null) callback.onCallback(participants);
                })
                .addOnFailureListener(e -> {
                    error.postValue("Ошибка при загрузке участников: " + e.getMessage());
                    if (callback != null) callback.onCallback(new ArrayList<>());
                });
    }
    
    private String getCurrentUserName() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String displayName = auth.getCurrentUser().getDisplayName();
            return displayName != null && !displayName.isEmpty() ? displayName : "Пользователь";
        }
        return "Пользователь";
    }
    
    public interface CallbackListener<T> {
        void onCallback(T result);
    }

    public void preloadEvents(Runnable callback) {
        executor.execute(() -> {
            try {
                // Здесь загрузка событий из API или базы данных
                // Имитация задержки загрузки данных
                Thread.sleep(1500);
                
                Log.d(TAG, "События успешно загружены");
                
                if (callback != null) {
                    mainHandler.post(callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки событий", e);
                
                if (callback != null) {
                    mainHandler.post(callback);
                }
            }
        });
    }
} 