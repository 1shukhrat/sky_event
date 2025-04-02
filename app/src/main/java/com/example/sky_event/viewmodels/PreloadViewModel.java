package com.example.sky_event.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sky_event.models.DataLoadingStatus;
import com.example.sky_event.models.weather.CurrentWeather;
import com.example.sky_event.models.weather.CurrentWeatherResponse;
import com.example.sky_event.models.weather.DailyForecast;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.models.weather.HourlyForecast;
import com.example.sky_event.network.RetrofitClient;
import com.example.sky_event.network.WeatherApi;
import com.example.sky_event.repositories.DataRepository;
import com.example.sky_event.repositories.EventRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreloadViewModel extends AndroidViewModel {
    private static final String TAG = "PreloadViewModel";
    private static final String API_KEY = "339fbf23040d10ed9d2f8275180603b7";
    private static final String UNITS = "metric";
    private static final String LANG = "ru";
    
    private final MutableLiveData<DataLoadingStatus> loadingStatus = new MutableLiveData<>();
    private final WeatherApi weatherApi;
    private final DataRepository dataRepository;
    private final EventRepository eventRepository;
    
    private int totalTasks = 3;
    private AtomicInteger completedTasks = new AtomicInteger(0);
    
    public PreloadViewModel(@NonNull Application application) {
        super(application);
        weatherApi = RetrofitClient.getInstance().getWeatherApi();
        dataRepository = DataRepository.getInstance(application);
        eventRepository = EventRepository.getInstance(application);
    }
    
    public LiveData<DataLoadingStatus> getLoadingStatus() {
        return loadingStatus;
    }
    
    public void preloadAppData() {
        Log.d(TAG, "Начинаем предварительную загрузку данных");
        loadingStatus.setValue(DataLoadingStatus.loading("Подготовка...", 0));
        
        preloadWeatherData();
        preloadEventData();
        preloadUserData();
    }
    
    private void preloadWeatherData() {
        Log.d(TAG, "Загружаем данные о погоде");
        loadingStatus.setValue(DataLoadingStatus.loading("Загружаем прогноз погоды...", 10));
        
        // Используем фиксированные координаты Москвы для начальной загрузки
        double lat = 55.7558;
        double lon = 37.6173;
        
        weatherApi.getCurrentWeather(lat, lon, UNITS, LANG, API_KEY)
                .enqueue(new Callback<CurrentWeatherResponse>() {
                    @Override
                    public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Успешно загружены текущие данные о погоде");
                            
                            CurrentWeatherResponse data = response.body();
                            processAndSaveCurrentWeather(data);
                            
                            loadingStatus.setValue(DataLoadingStatus.loading("Текущая погода загружена", 30));
                        } else {
                            Log.e(TAG, "Ошибка при загрузке текущей погоды: " + response.code());
                            loadFallbackData();
                        }
                        
                        taskCompleted();
                    }
                    
                    @Override
                    public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                        Log.e(TAG, "Ошибка загрузки текущей погоды", t);
                        loadFallbackData();
                        taskCompleted();
                    }
                });
        
        weatherApi.getForecast(lat, lon, UNITS, LANG, API_KEY)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Успешно загружен прогноз погоды");
                            
                            ForecastResponse data = response.body();
                            if (data.getList() != null && !data.getList().isEmpty()) {
                                processAndSaveHourlyForecast(data.getList());
                                processAndSaveDailyForecast(data.getList());
                            } else {
                                loadFallbackData();
                            }
                            
                            loadingStatus.setValue(DataLoadingStatus.loading("Прогноз погоды загружен", 50));
                        } else {
                            Log.e(TAG, "Ошибка при загрузке прогноза погоды: " + response.code());
                            loadFallbackData();
                        }
                        
                        taskCompleted();
                    }
                    
                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        Log.e(TAG, "Ошибка загрузки прогноза погоды", t);
                        loadFallbackData();
                        taskCompleted();
                    }
                });
    }
    
    private void processAndSaveCurrentWeather(CurrentWeatherResponse data) {
        String iconCode = "";
        String description = "";
        if (data.getWeather() != null && !data.getWeather().isEmpty()) {
            iconCode = data.getWeather().get(0).getIcon();
            description = data.getWeather().get(0).getDescription();
            description = capitalize(description);
        }
        
        int pressure = data.getMain().getPressure();
        int pressureMmHg = (int) (pressure * 0.750062);
        
        CurrentWeather weather = new CurrentWeather(
                data.getName(),
                (int) Math.round(data.getMain().getTemp()),
                description,
                (int) Math.round(data.getMain().getFeels_like()),
                data.getMain().getHumidity(),
                pressureMmHg,
                (int) Math.round(data.getWind().getSpeed()),
                iconCode);
        
        dataRepository.saveCurrentWeather(weather);
    }
    
    private void processAndSaveHourlyForecast(List<ForecastResponse.ForecastItem> forecastItems) {
        List<HourlyForecast> hourlyList = new ArrayList<>();
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        Date endTime = calendar.getTime();
        
        for (ForecastResponse.ForecastItem item : forecastItems) {
            Date itemDate = new Date(item.getDt() * 1000L);
            
            if (itemDate.after(new Date()) && itemDate.before(endTime)) {
                String iconCode = "";
                String description = "";
                if (item.getWeather() != null && !item.getWeather().isEmpty()) {
                    iconCode = item.getWeather().get(0).getIcon();
                    description = item.getWeather().get(0).getDescription();
                    description = capitalize(description);
                }
                
                HourlyForecast forecast = new HourlyForecast(
                        itemDate,
                        (int) Math.round(item.getMain().getTemp()),
                        iconCode,
                        description,
                        (int) Math.round(item.getWind().getSpeed()));
                
                hourlyList.add(forecast);
                
                if (hourlyList.size() >= 8) {
                    break;
                }
            }
        }
        
        dataRepository.saveHourlyForecast(hourlyList);
    }
    
    private void processAndSaveDailyForecast(List<ForecastResponse.ForecastItem> forecastItems) {
        Map<String, List<ForecastResponse.ForecastItem>> dailyMap = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (ForecastResponse.ForecastItem item : forecastItems) {
            Date itemDate = new Date(item.getDt() * 1000L);
            String day = dayFormat.format(itemDate);
            
            if (!dailyMap.containsKey(day)) {
                dailyMap.put(day, new ArrayList<>());
            }
            
            dailyMap.get(day).add(item);
        }
        
        List<DailyForecast> dailyList = new ArrayList<>();
        
        for (Map.Entry<String, List<ForecastResponse.ForecastItem>> entry : dailyMap.entrySet()) {
            List<ForecastResponse.ForecastItem> dayItems = entry.getValue();
            
            if (!dayItems.isEmpty()) {
                int minTemp = Integer.MAX_VALUE;
                int maxTemp = Integer.MIN_VALUE;
                int maxWind = 0;
                boolean hasRain = false;
                ForecastResponse.ForecastItem middayItem = null;
                
                for (ForecastResponse.ForecastItem item : dayItems) {
                    int temp = (int) Math.round(item.getMain().getTemp());
                    minTemp = Math.min(minTemp, temp);
                    maxTemp = Math.max(maxTemp, temp);
                    
                    int wind = (int) Math.round(item.getWind().getSpeed());
                    maxWind = Math.max(maxWind, wind);
                    
                    if (item.getRain() != null && item.getRain().getThreeHour() > 0) {
                        hasRain = true;
                    }
                    
                    Date itemDate = new Date(item.getDt() * 1000L);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(itemDate);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    
                    if (hour == 12 || middayItem == null) {
                        middayItem = item;
                    }
                }
                
                if (middayItem != null) {
                    String iconCode = "";
                    String description = "";
                    if (middayItem.getWeather() != null && !middayItem.getWeather().isEmpty()) {
                        iconCode = middayItem.getWeather().get(0).getIcon();
                        description = middayItem.getWeather().get(0).getDescription();
                        description = capitalize(description);
                    }
                    
                    Date date = new Date(middayItem.getDt() * 1000L);
                    DailyForecast forecast = new DailyForecast(
                            date,
                            minTemp,
                            maxTemp,
                            iconCode,
                            description,
                            maxWind,
                            hasRain);
                    
                    dailyList.add(forecast);
                }
            }
        }
        
        dataRepository.saveDailyForecast(dailyList);
    }
    
    private void loadFallbackData() {
        Log.d(TAG, "Загружаем резервные данные");
        
        dataRepository.saveCurrentWeather(createFallbackCurrentWeather());
        dataRepository.saveHourlyForecast(createFallbackHourlyForecast());
        dataRepository.saveDailyForecast(createFallbackDailyForecast());
        
        loadingStatus.postValue(DataLoadingStatus.loading("Загружены базовые данные", 50));
    }
    
    private CurrentWeather createFallbackCurrentWeather() {
        return new CurrentWeather(
                "Москва",
                15,
                "Переменная облачность",
                13,
                65,
                750,
                5,
                "03d");
    }
    
    private List<HourlyForecast> createFallbackHourlyForecast() {
        List<HourlyForecast> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        String[] icons = {"01d", "02d", "03d", "04d", "10d", "01d", "02d", "03d"};
        String[] desc = {"Ясно", "Малооблачно", "Облачно", "Пасмурно", "Небольшой дождь", "Ясно", "Малооблачно", "Облачно"};
        int[] temps = {15, 16, 17, 16, 15, 14, 13, 12};
        
        for (int i = 0; i < 8; i++) {
            cal.add(Calendar.HOUR_OF_DAY, 1);
            result.add(new HourlyForecast(
                    cal.getTime(),
                    temps[i],
                    icons[i],
                    desc[i],
                    4));
        }
        
        return result;
    }
    
    private List<DailyForecast> createFallbackDailyForecast() {
        List<DailyForecast> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        String[] icons = {"01d", "02d", "10d", "03d", "01d"};
        String[] desc = {"Ясно", "Малооблачно", "Небольшой дождь", "Облачно", "Ясно"};
        int[] minTemps = {10, 12, 11, 9, 13};
        int[] maxTemps = {18, 19, 17, 16, 20};
        boolean[] hasRain = {false, false, true, false, false};
        
        for (int i = 0; i < 5; i++) {
            result.add(new DailyForecast(
                    cal.getTime(),
                    minTemps[i],
                    maxTemps[i],
                    icons[i],
                    desc[i],
                    5,
                    hasRain[i]));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        return result;
    }
    
    private void preloadEventData() {
        Log.d(TAG, "Загружаем данные о событиях");
        loadingStatus.setValue(DataLoadingStatus.loading("Загружаем события...", 60));
        
        // Здесь загрузка событий из API или базы данных
        eventRepository.preloadEvents(() -> {
            Log.d(TAG, "События загружены");
            loadingStatus.setValue(DataLoadingStatus.loading("События загружены", 80));
            taskCompleted();
        });
    }
    
    private void preloadUserData() {
        Log.d(TAG, "Загружаем данные пользователя");
        loadingStatus.setValue(DataLoadingStatus.loading("Проверяем учетную запись...", 90));
        
        // Здесь загрузка данных пользователя или проверка сессии
        // Имитация работы
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                taskCompleted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void taskCompleted() {
        int completed = completedTasks.incrementAndGet();
        int progressPercentage = (completed * 100) / totalTasks;
        
        Log.d(TAG, "Задача выполнена. Прогресс: " + completed + "/" + totalTasks + " (" + progressPercentage + "%)");
        
        if (completed >= totalTasks) {
            loadingStatus.postValue(DataLoadingStatus.complete("Загрузка завершена", 100));
            Log.d(TAG, "Все задачи выполнены, загрузка завершена");
        } else {
            String message = "Загрузка данных: " + progressPercentage + "%";
            loadingStatus.postValue(DataLoadingStatus.loading(message, progressPercentage));
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
} 