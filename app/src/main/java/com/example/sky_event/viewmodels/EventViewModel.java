package com.example.sky_event.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.repositories.EventRepository;
import com.example.sky_event.repositories.WeatherRepository;

import java.util.ArrayList;
import java.util.List;

public class EventViewModel extends AndroidViewModel {
    
    private final EventRepository eventRepository;
    private final WeatherRepository weatherRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasJoinedEvent = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> eventParticipants = new MutableLiveData<>(new ArrayList<>());
    
    public EventViewModel(@NonNull Application application) {
        super(application);
        eventRepository = EventRepository.getInstance(application);
        weatherRepository = WeatherRepository.getInstance(application);
    }
    
    public LiveData<List<Event>> getEvents() {
        return eventRepository.getEvents();
    }
    
    public LiveData<Event> getEvent() {
        return eventRepository.getEvent();
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    public LiveData<Boolean> hasJoinedEvent() {
        return hasJoinedEvent;
    }
    
    public LiveData<List<String>> getEventParticipants() {
        return eventParticipants;
    }
    
    public void fetchEvents() {
        isLoading.setValue(true);
        eventRepository.fetchUserEvents();
        
        // Добавляем задержку и устанавливаем isLoading в false после неё, на случай если обратные вызовы не сработают
        new android.os.Handler().postDelayed(() -> {
            isLoading.setValue(false);
        }, 5000); // 5 секунд максимального времени ожидания
    }
    
    public void fetchUserEvents() {
        fetchEvents();
    }
    
    public void fetchEvent(String eventId) {
        isLoading.setValue(true);
        eventRepository.fetchEvent(eventId);
        isLoading.setValue(false);
    }
    
    public void loadEventWeather(double latitude, double longitude) {
        weatherRepository.fetchForecast(latitude, longitude);
    }
    
    public LiveData<ForecastResponse> getForecast() {
        return weatherRepository.getForecast();
    }
    
    public WeatherRepository getWeatherRepository() {
        return weatherRepository;
    }
    
    public void findOptimalDate(Event event) {
        isLoading.setValue(true);
        ForecastResponse forecastResponse = weatherRepository.getForecast().getValue();
        if (forecastResponse != null) {
            eventRepository.findOptimalDate(forecastResponse, event);
            isLoading.setValue(false);
        } else {
            weatherRepository.fetchForecast(event.getLatitude(), event.getLongitude());
            MediatorLiveData<ForecastResponse> forecastMediator = new MediatorLiveData<>();
            forecastMediator.addSource(weatherRepository.getForecast(), forecast -> {
                if (forecast != null) {
                    eventRepository.findOptimalDate(forecast, event);
                    isLoading.setValue(false);
                    forecastMediator.removeSource(weatherRepository.getForecast());
                }
            });
        }
    }
    
    public void findOptimalDateForEvent(Event event, double latitude, double longitude) {
        isLoading.setValue(true);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        
        new android.os.Handler().postDelayed(() -> {
            if (isLoading.getValue() != null && isLoading.getValue()) {
                isLoading.setValue(false);
                error.setValue("Превышено время ожидания запроса прогноза погоды.");
            }
        }, 15000);
        
        weatherRepository.fetchForecast(latitude, longitude);
        
        MediatorLiveData<ForecastResponse> forecastMediator = new MediatorLiveData<>();
        
        boolean[] responseReceived = {false};
        
        forecastMediator.addSource(weatherRepository.getForecast(), forecast -> {
            responseReceived[0] = true;
            if (forecast != null) {
                try {
                    eventRepository.findOptimalDate(forecast, event);
                } catch (Exception e) {
                    error.setValue("Ошибка при поиске оптимальной даты: " + e.getMessage());
                } finally {
                    isLoading.setValue(false);
                    forecastMediator.removeSource(weatherRepository.getForecast());
                }
            } else {
                isLoading.setValue(false);
                error.setValue("Не удалось получить прогноз погоды");
            }
        });
        
        forecastMediator.addSource(weatherRepository.getError(), errorMsg -> {
            if (!responseReceived[0] && errorMsg != null && !errorMsg.isEmpty()) {
                error.setValue(errorMsg);
                isLoading.setValue(false);
                forecastMediator.removeSource(weatherRepository.getError());
                forecastMediator.removeSource(weatherRepository.getForecast());
            }
        });
        
        forecastMediator.observeForever(response -> {});
    }
    
    public void findOptimalDateByLocation(Event event, String location) {
        isLoading.setValue(true);
        if (location != null && !location.isEmpty()) {
            weatherRepository.fetchForecastByCity(location);
            
            MediatorLiveData<ForecastResponse> forecastMediator = new MediatorLiveData<>();
            forecastMediator.addSource(weatherRepository.getForecast(), forecast -> {
                if (forecast != null) {
                    eventRepository.findOptimalDate(forecast, event);
                    isLoading.setValue(false);
                    forecastMediator.removeSource(weatherRepository.getForecast());
                }
            });
        } else {
            isLoading.setValue(false);
        }
    }
    
    public void saveEvent(Event event) {
        isLoading.setValue(true);
        
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(java.util.UUID.randomUUID().toString());
        }
        
        eventRepository.saveEvent(event);
    }
    
    public void updateEvent(Event event) {
        isLoading.setValue(true);
        eventRepository.updateEvent(event);
        isLoading.setValue(false);
    }
    
    public void deleteEvent(String eventId) {
        isLoading.setValue(true);
        eventRepository.deleteEvent(eventId);
        isLoading.setValue(false);
    }
    
    public boolean hasUserJoinedEvent(String eventId) {
        return eventRepository.hasUserJoinedEvent(eventId);
    }
    
    public void joinEvent(String eventId) {
        isLoading.setValue(true);
        eventRepository.joinEvent(eventId, success -> {
            isLoading.setValue(false);
            if (success) {
                hasJoinedEvent.setValue(true);
                fetchEvent(eventId);
            } else {
                error.setValue("Не удалось присоединиться к событию");
            }
        });
    }
    
    public void leaveEvent(String eventId) {
        isLoading.setValue(true);
        eventRepository.leaveEvent(eventId, success -> {
            isLoading.setValue(false);
            if (success) {
                hasJoinedEvent.setValue(false);
                fetchEvent(eventId);
            } else {
                error.setValue("Не удалось отменить участие в событии");
            }
        });
    }
    
    public void fetchEventParticipants(String eventId) {
        isLoading.setValue(true);
        eventRepository.fetchEventParticipants(eventId, participants -> {
            isLoading.setValue(false);
            eventParticipants.setValue(participants);
        });
    }
} 