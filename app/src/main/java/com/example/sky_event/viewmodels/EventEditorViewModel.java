package com.example.sky_event.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.models.weather.CurrentWeatherResponse;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.repositories.EventRepository;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.utils.CalendarIntegrationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventEditorViewModel extends AndroidViewModel {
    
    private final MutableLiveData<Event> eventLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Date>> suggestedDatesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingDatesLiveData = new MutableLiveData<>(false);
    
    private final EventRepository eventRepository;
    private final DateSuggestionViewModel dateSuggestionViewModel;
    
    public EventEditorViewModel(@NonNull Application application) {
        super(application);
        this.eventRepository = new EventRepository(application);
        this.dateSuggestionViewModel = new DateSuggestionViewModel(application);
        
        dateSuggestionViewModel.getSuggestedDates().observeForever(dates -> {
            suggestedDatesLiveData.setValue(dates);
            isLoadingDatesLiveData.setValue(false);
        });
        
        dateSuggestionViewModel.getError().observeForever(error -> {
            if (error != null) {
                errorLiveData.setValue(error);
                isLoadingDatesLiveData.setValue(false);
            }
        });
        
        dateSuggestionViewModel.isLoading().observeForever(isLoading -> {
            isLoadingDatesLiveData.setValue(isLoading);
        });
    }
    
    public LiveData<Event> getEvent() {
        return eventLiveData;
    }
    
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccessLiveData;
    }
    
    public LiveData<String> getError() {
        return errorLiveData;
    }
    
    public LiveData<List<Date>> getSuggestedDates() {
        return suggestedDatesLiveData;
    }
    
    public LiveData<Boolean> isLoadingDates() {
        return isLoadingDatesLiveData;
    }
    
    public void loadEvent(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Event event = new Event();
                event.setId(eventId);
                event.setUserId(currentUser.getUid());
                eventLiveData.setValue(event);
            }
        } else {
            createNewEvent();
        }
    }
    
    public void createNewEvent() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setUserId(currentUser.getUid());
            event.setDate(new Date(System.currentTimeMillis() + 86400000)); // Завтра
            event.setWeatherCondition(new WeatherCondition());
            eventLiveData.setValue(event);
        } else {
            errorLiveData.setValue("Пользователь не авторизован");
        }
    }
    
    public void saveEvent(Event event) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            event.setUserId(currentUser.getUid());
            
            if (event.getDate() == null) {
                event.setDate(new Date());
            }
            
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId(UUID.randomUUID().toString());
            }
            
            if (event.getName() == null || event.getName().trim().isEmpty()) {
                errorLiveData.setValue("Введите название события");
                return;
            }
            
            eventRepository.saveEvent(event);
            saveSuccessLiveData.setValue(true);
        } else {
            errorLiveData.setValue("Пользователь не авторизован");
        }
    }
    
    public void deleteEvent(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            eventRepository.deleteEvent(eventId);
            saveSuccessLiveData.setValue(true);
        }
    }
    
    public void findOptimalDates(Event event) {
        if (event != null) {
            double latitude = event.getLatitude();
            double longitude = event.getLongitude();
            String location = event.getLocation();
            WeatherCondition weatherCondition = event.getWeatherCondition();
            
            if (latitude != 0 && longitude != 0) {
                isLoadingDatesLiveData.setValue(true);
                dateSuggestionViewModel.findOptimalDates(latitude, longitude, location, weatherCondition);
            } else if (location != null && !location.isEmpty()) {
                isLoadingDatesLiveData.setValue(true);
                dateSuggestionViewModel.findOptimalDates(0, 0, location, weatherCondition);
            } else {
                errorLiveData.setValue("Необходимо указать местоположение события");
            }
        }
    }
    
    public void cancelFindOptimalDates() {
        dateSuggestionViewModel.cancelFindOptimalDates();
    }
    
    public void addEventToCalendar(Event event) {
        if (event != null) {
            CalendarIntegrationHelper.addEventToCalendar(getApplication(), event);
        }
    }
    
    public void shareEvent(Event event) {
        if (event != null) {
            CalendarIntegrationHelper.shareEvent(getApplication(), event);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelFindOptimalDates();
    }
} 