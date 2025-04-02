package com.example.sky_event.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.WorkInfo;

import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.utils.WorkManagerHelper;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class DateSuggestionViewModel extends AndroidViewModel {
    
    private final MutableLiveData<List<Date>> suggestedDatesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    
    private UUID workerId;
    
    public DateSuggestionViewModel(@NonNull Application application) {
        super(application);
    }
    
    public LiveData<List<Date>> getSuggestedDates() {
        return suggestedDatesLiveData;
    }
    
    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }
    
    public LiveData<String> getError() {
        return errorLiveData;
    }
    
    public void findOptimalDates(double latitude, double longitude, String location, WeatherCondition weatherCondition) {
        isLoadingLiveData.setValue(true);
        errorLiveData.setValue(null);
        
        workerId = WorkManagerHelper.runDateSuggestionWorker(
                getApplication(), latitude, longitude, location, weatherCondition);
        
        new Thread(() -> {
            try {
                boolean isFinished = false;
                while (!isFinished) {
                    Thread.sleep(500);
                    WorkInfo workInfo = WorkManagerHelper.getWorkInfo(getApplication(), workerId);
                    
                    if (workInfo != null) {
                        WorkInfo.State state = workInfo.getState();
                        
                        if (state == WorkInfo.State.SUCCEEDED) {
                            long[] timestamps = workInfo.getOutputData().getLongArray("suggested_dates");
                            if (timestamps != null && timestamps.length > 0) {
                                List<Date> suggestedDates = convertTimestampsToDates(timestamps);
                                suggestedDatesLiveData.postValue(suggestedDates);
                            } else {
                                errorLiveData.postValue("Не удалось найти подходящие даты");
                            }
                            isFinished = true;
                            isLoadingLiveData.postValue(false);
                        } else if (state == WorkInfo.State.FAILED || state == WorkInfo.State.CANCELLED) {
                            errorLiveData.postValue("Не удалось выполнить поиск оптимальной даты");
                            isFinished = true;
                            isLoadingLiveData.postValue(false);
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                errorLiveData.postValue("Ошибка: " + e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        }).start();
    }
    
    private List<Date> convertTimestampsToDates(long[] timestamps) {
        List<Date> dates = new java.util.ArrayList<>();
        for (long timestamp : timestamps) {
            dates.add(new Date(timestamp));
        }
        return dates;
    }
    
    public void cancelFindOptimalDates() {
        if (workerId != null) {
            WorkManagerHelper.cancelWorkById(getApplication(), workerId);
            isLoadingLiveData.setValue(false);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelFindOptimalDates();
    }
} 