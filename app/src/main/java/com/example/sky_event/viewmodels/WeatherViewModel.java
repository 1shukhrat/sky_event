package com.example.sky_event.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sky_event.network.RetrofitClient;
import com.example.sky_event.network.WeatherApi;
import com.example.sky_event.models.weather.CurrentWeather;
import com.example.sky_event.models.weather.CurrentWeatherResponse;
import com.example.sky_event.models.weather.DailyForecast;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.models.weather.HourlyForecast;
import com.example.sky_event.models.weather.MonthlyForecast;
import com.example.sky_event.repositories.DataRepository;
import com.example.sky_event.repositories.WeatherRepository;
import com.example.sky_event.SkyEventApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherViewModel extends ViewModel {
    private static final String API_KEY = "339fbf23040d10ed9d2f8275180603b7";
    private static final String UNITS = "metric";
    private static final String LANG = "ru";
    
    private final MutableLiveData<CurrentWeather> currentWeather = new MutableLiveData<>();
    private final MutableLiveData<List<HourlyForecast>> hourlyForecast = new MutableLiveData<>();
    private final MutableLiveData<List<DailyForecast>> dailyForecast = new MutableLiveData<>();
    private final MutableLiveData<List<MonthlyForecast>> monthlyForecast = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private final WeatherApi apiService;
    private final DataRepository dataRepository;
    private final WeatherRepository weatherRepository;
    private AtomicInteger pendingRequests = new AtomicInteger(0);
    
    private final Map<String, Call<?>> activeRequests = new ConcurrentHashMap<>();
    private double lastLatitude = 0;
    private double lastLongitude = 0;
    
    public WeatherViewModel() {
        apiService = RetrofitClient.getInstance().getWeatherApi();
        dataRepository = DataRepository.getInstance(SkyEventApplication.getInstance());
        weatherRepository = WeatherRepository.getInstance(SkyEventApplication.getInstance());
        if (SkyEventApplication.getInstance().isPreloadCompleted()) {
            Log.d("WeatherViewModel", "Используем предзагруженные данные");
            loadCachedData();
        }
    }
    
    public LiveData<CurrentWeather> getCurrentWeather() {
        return currentWeather;
    }
    
    public LiveData<List<HourlyForecast>> getHourlyForecast() {
        return hourlyForecast;
    }
    
    public LiveData<List<DailyForecast>> getDailyForecast() {
        return dailyForecast;
    }
    
    public LiveData<List<MonthlyForecast>> getMonthlyForecast() {
        return monthlyForecast;
    }
    
    public LiveData<Boolean> isLoading() {
        return loading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public void fetchWeather(double lat, double lon) {
        if (Math.abs(lat - lastLatitude) < 0.01 && Math.abs(lon - lastLongitude) < 0.01 
                && currentWeather.getValue() != null 
                && hourlyForecast.getValue() != null 
                && !hourlyForecast.getValue().isEmpty()
                && dailyForecast.getValue() != null
                && !dailyForecast.getValue().isEmpty()) {
            Log.d("WeatherViewModel", "Координаты не изменились существенно, используем кэшированные данные");
            loading.postValue(false);
            return;
        }
        
        cancelActiveRequests();
        lastLatitude = lat;
        lastLongitude = lon;
        
        loading.setValue(true);
        error.setValue(null);
        Log.d("WeatherViewModel", "Загрузка погоды для: " + lat + ", " + lon);
        
        pendingRequests.set(3);
        fetchCurrentWeather(lat, lon);
        fetchForecast(lat, lon);
        fetchMonthlyForecast(lat, lon);
    }
    
    private void cancelActiveRequests() {
        for (Call<?> call : activeRequests.values()) {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
        activeRequests.clear();
    }
    
    private void fetchCurrentWeather(double lat, double lon) {
        Log.d("WeatherViewModel", "Вызов API текущей погоды");
        Call<CurrentWeatherResponse> call = apiService.getCurrentWeather(lat, lon, UNITS, LANG, API_KEY);
        activeRequests.put("current", call);
        
        call.enqueue(new Callback<CurrentWeatherResponse>() {
            @Override
            public void onResponse(Call<CurrentWeatherResponse> call, 
                                   Response<CurrentWeatherResponse> response) {
                activeRequests.remove("current");
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("WeatherViewModel", "API текущей погоды: успех");
                    CurrentWeatherResponse data = response.body();
                    processCurrentWeatherResponse(data);
                    saveCurrentWeatherToCache(data);
                } else {
                    Log.e("WeatherViewModel", "API текущей погоды: ошибка " + response.code());
                    if (currentWeather.getValue() == null) {
                        error.setValue("Ошибка получения данных о текущей погоде");
                        loadCachedData();
                    }
                }
                
                checkRequestsCompletion();
            }
            
            @Override
            public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                activeRequests.remove("current");
                
                if (call.isCanceled()) {
                    Log.d("WeatherViewModel", "Запрос текущей погоды отменен");
                } else {
                    Log.e("WeatherViewModel", "Сбой запроса текущей погоды", t);
                    if (currentWeather.getValue() == null) {
                        error.setValue("Сетевая ошибка: " + t.getMessage());
                        loadCachedData();
                    }
                }
                
                checkRequestsCompletion();
            }
        });
    }
    
    private void saveCurrentWeatherToCache(CurrentWeatherResponse data) {
        if (dataRepository != null) {
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
    }
    
    private void processCurrentWeatherResponse(CurrentWeatherResponse data) {
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
        
        currentWeather.setValue(weather);
    }
    
    private void loadCachedData() {
        Log.d("WeatherViewModel", "Загрузка данных из кэша");
        
        CurrentWeather cached = dataRepository.getCurrentWeather();
        if (cached != null) {
            currentWeather.setValue(cached);
        } else {
            loadFallbackWeather();
        }
        
        List<HourlyForecast> hourly = dataRepository.getHourlyForecast();
        if (hourly != null && !hourly.isEmpty()) {
            hourlyForecast.setValue(hourly);
        } else {
            List<HourlyForecast> fallbackHourly = createFallbackHourlyForecast();
            hourlyForecast.setValue(fallbackHourly);
        }
        
        List<DailyForecast> daily = dataRepository.getDailyForecast();
        if (daily != null && !daily.isEmpty()) {
            dailyForecast.setValue(daily);
        } else {
            List<DailyForecast> fallbackDaily = createFallbackDailyForecast();
            dailyForecast.setValue(fallbackDaily);
        }
        
        List<MonthlyForecast> monthly = dataRepository.getMonthlyForecast();
        if (monthly != null && !monthly.isEmpty()) {
            monthlyForecast.setValue(monthly);
        } else {
            List<MonthlyForecast> fallbackMonthly = createFallbackMonthlyForecast();
            monthlyForecast.setValue(fallbackMonthly);
        }
    }
    
    private void loadFallbackWeather() {
        Log.d("WeatherViewModel", "Загрузка резервных данных о погоде");
        
        CurrentWeather fallbackWeather = new CurrentWeather(
                "Москва",
                15,
                "Переменная облачность",
                13,
                65,
                750,
                5,
                "03d");
        currentWeather.setValue(fallbackWeather);
    }
    
    private List<HourlyForecast> createFallbackHourlyForecast() {
        List<HourlyForecast> forecasts = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        for (int i = 0; i < 24; i++) {
            if (i > 0) {
                calendar.add(Calendar.HOUR_OF_DAY, 1);
            }
            
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int temp = 10 + (int) (Math.random() * 15);
            String icon = hour >= 6 && hour <= 18 ? "01d" : "01n";
            
            HourlyForecast forecast = new HourlyForecast(
                    (Date) calendar.getTime().clone(),
                    temp,
                    icon,
                    "Ясно",
                    (int) (Math.random() * 10),
                    false);
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
    
    private List<DailyForecast> createFallbackDailyForecast() {
        List<DailyForecast> forecasts = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        for (int i = 0; i < 5; i++) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            int minTemp = 8 + (int) (Math.random() * 10);
            int maxTemp = minTemp + 5 + (int) (Math.random() * 5);
            
            DailyForecast forecast = new DailyForecast(
                    (Date) calendar.getTime().clone(),
                    minTemp,
                    maxTemp,
                    "01d",
                    "Ясно",
                    (int) (Math.random() * 10),
                    false);
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
    
    private void fetchForecast(double lat, double lon) {
        Log.d("WeatherViewModel", "Вызов API прогноза");
        Call<ForecastResponse> call = apiService.get3HourForecastFor5Days(lat, lon, UNITS, LANG, API_KEY);
        activeRequests.put("forecast", call);
        
        call.enqueue(new Callback<ForecastResponse>() {
            @Override
            public void onResponse(Call<ForecastResponse> call, 
                                   Response<ForecastResponse> response) {
                activeRequests.remove("forecast");
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("WeatherViewModel", "API прогноза: успех");
                    ForecastResponse data = response.body();
                    
                    if (data.getList() != null && !data.getList().isEmpty()) {
                        processHourlyForecast(data.getList());
                        processDailyForecast(data.getList());
                        saveForecastToCache(data.getList());
                    } else {
                        Log.e("WeatherViewModel", "API прогноза вернул пустой список");
                        loadCachedData();
                    }
                } else {
                    Log.e("WeatherViewModel", "API прогноза: ошибка " + response.code());
                    if (hourlyForecast.getValue() == null || hourlyForecast.getValue().isEmpty()) {
                        error.setValue("Ошибка получения данных о прогнозе");
                        loadCachedData();
                    }
                }
                
                checkRequestsCompletion();
            }
            
            @Override
            public void onFailure(Call<ForecastResponse> call, Throwable t) {
                activeRequests.remove("forecast");
                
                if (call.isCanceled()) {
                    Log.d("WeatherViewModel", "Запрос прогноза отменен");
                } else {
                    Log.e("WeatherViewModel", "Сбой запроса прогноза", t);
                    if (hourlyForecast.getValue() == null || hourlyForecast.getValue().isEmpty()) {
                        error.setValue("Сетевая ошибка при получении прогноза: " + t.getMessage());
                        loadCachedData();
                    }
                }
                
                checkRequestsCompletion();
            }
        });
    }
    
    private void saveForecastToCache(List<ForecastResponse.ForecastItem> items) {
        if (dataRepository != null) {
            List<HourlyForecast> hourlyList = processHourlyForecastList(items);
            List<DailyForecast> dailyList = processDailyForecastList(items);
            
            dataRepository.saveHourlyForecast(hourlyList);
            dataRepository.saveDailyForecast(dailyList);
        }
    }
    
    private void processHourlyForecast(List<ForecastResponse.ForecastItem> items) {
        List<HourlyForecast> hourlyList = processHourlyForecastList(items);
        hourlyForecast.setValue(hourlyList);
    }
    
    private List<HourlyForecast> processHourlyForecastList(List<ForecastResponse.ForecastItem> items) {
        List<HourlyForecast> hourlyList = new ArrayList<>();
        
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        
        Map<String, HourlyForecast> hourlyMap = new HashMap<>();
        SimpleDateFormat hourKey = new SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault());
        
        for (ForecastResponse.ForecastItem item : items) {
            Date date = new Date(item.getDt() * 1000L);
            Calendar itemDate = Calendar.getInstance();
            itemDate.setTime(date);
            itemDate.set(Calendar.MINUTE, 0);
            itemDate.set(Calendar.SECOND, 0);
            itemDate.set(Calendar.MILLISECOND, 0);
            
            if (itemDate.after(now) || itemDate.equals(now)) {
                String key = hourKey.format(itemDate.getTime());
                
                String icon = "";
                String description = "";
                
                if (item.getWeather() != null && !item.getWeather().isEmpty()) {
                    icon = item.getWeather().get(0).getIcon();
                    description = capitalize(item.getWeather().get(0).getDescription());
                }
                
                boolean hasRain = item.getRain() != null && item.getRain().getThreeHour() > 0;
                
                HourlyForecast hourlyItem = new HourlyForecast(
                        itemDate.getTime(),
                        (int) Math.round(item.getMain().getTemp()),
                        icon,
                        description,
                        (int) Math.round(item.getWind().getSpeed()),
                        hasRain);
                
                hourlyMap.put(key, hourlyItem);
            }
        }
        
        Calendar startHour = Calendar.getInstance();
        startHour.set(Calendar.MINUTE, 0);
        startHour.set(Calendar.SECOND, 0);
        startHour.set(Calendar.MILLISECOND, 0);
        
        for (int i = 0; i < 24; i++) {
            String key = hourKey.format(startHour.getTime());
            
            if (hourlyMap.containsKey(key)) {
                hourlyList.add(hourlyMap.get(key));
            } else {
                String prevKey = null;
                String nextKey = null;
                Date prevDate = null;
                Date nextDate = null;
                
                for (String mapKey : hourlyMap.keySet()) {
                    Date mapDate = hourlyMap.get(mapKey).getTime();
                    
                    if (mapDate.before(startHour.getTime())) {
                        if (prevDate == null || mapDate.after(prevDate)) {
                            prevKey = mapKey;
                            prevDate = mapDate;
                        }
                    } else if (mapDate.after(startHour.getTime())) {
                        if (nextDate == null || mapDate.before(nextDate)) {
                            nextKey = mapKey;
                            nextDate = mapDate;
                        }
                    }
                }
                
                if (prevKey != null && nextKey != null) {
                    HourlyForecast prev = hourlyMap.get(prevKey);
                    HourlyForecast next = hourlyMap.get(nextKey);
                    
                    long prevTime = prev.getTime().getTime();
                    long nextTime = next.getTime().getTime();
                    long currentTime = startHour.getTimeInMillis();
                    
                    double ratio = (double)(currentTime - prevTime) / (nextTime - prevTime);
                    int temp = (int)(prev.getTemperature() + ratio * (next.getTemperature() - prev.getTemperature()));
                    int wind = (int)(prev.getWindSpeed() + ratio * (next.getWindSpeed() - prev.getWindSpeed()));
                    
                    HourlyForecast interpolated = new HourlyForecast(
                        startHour.getTime(),
                        temp,
                        prev.getIcon(),
                        prev.getDescription(),
                        wind,
                        prev.isHasRain() || next.isHasRain());
                    
                    hourlyList.add(interpolated);
                } else if (prevKey != null) {
                    HourlyForecast prev = hourlyMap.get(prevKey);
                    HourlyForecast copy = new HourlyForecast(
                        startHour.getTime(),
                        prev.getTemperature(),
                        prev.getIcon(),
                        prev.getDescription(),
                        prev.getWindSpeed(),
                        prev.isHasRain());
                    
                    hourlyList.add(copy);
                } else if (nextKey != null) {
                    HourlyForecast next = hourlyMap.get(nextKey);
                    HourlyForecast copy = new HourlyForecast(
                        startHour.getTime(),
                        next.getTemperature(),
                        next.getIcon(),
                        next.getDescription(),
                        next.getWindSpeed(),
                        next.isHasRain());
                    
                    hourlyList.add(copy);
                }
            }
            
            startHour.add(Calendar.HOUR_OF_DAY, 1);
        }
        
        return hourlyList;
    }
    
    private void processDailyForecast(List<ForecastResponse.ForecastItem> items) {
        List<DailyForecast> dailyList = processDailyForecastList(items);
        dailyForecast.setValue(dailyList);
    }
    
    private List<DailyForecast> processDailyForecastList(List<ForecastResponse.ForecastItem> items) {
        Map<String, List<ForecastResponse.ForecastItem>> dailyMap = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (ForecastResponse.ForecastItem item : items) {
            Date date = new Date(item.getDt() * 1000L);
            String dateKey = dateFormat.format(date);
            
            if (!dailyMap.containsKey(dateKey)) {
                dailyMap.put(dateKey, new ArrayList<>());
            }
            
            dailyMap.get(dateKey).add(item);
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
        
        return dailyList;
    }
    
    private void checkRequestsCompletion() {
        int remaining = pendingRequests.decrementAndGet();
        Log.d("WeatherViewModel", "Осталось запросов: " + remaining);
        if (remaining <= 0) {
            loading.postValue(false);
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private void fetchMonthlyForecast(double lat, double lon) {
        weatherRepository.getClimate30DayForecast(lat, lon).observeForever(forecasts -> {
            if (forecasts != null && !forecasts.isEmpty()) {
                monthlyForecast.setValue(forecasts);
                dataRepository.saveMonthlyForecast(forecasts);
            } else {
                List<MonthlyForecast> cachedForecasts = dataRepository.getMonthlyForecast();
                if (cachedForecasts != null && !cachedForecasts.isEmpty()) {
                    monthlyForecast.setValue(cachedForecasts);
                } else {
                    monthlyForecast.setValue(createFallbackMonthlyForecast());
                }
            }
            Log.d("WeatherViewModel", "Завершен запрос климатических данных");
            checkRequestsCompletion();
        });
    }
    
    private List<MonthlyForecast> createFallbackMonthlyForecast() {
        List<MonthlyForecast> forecasts = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        for (int i = 0; i < 30; i++) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            
            MonthlyForecast forecast = new MonthlyForecast();
            forecast.setDate((Date) calendar.getTime().clone());
            
            // Генерируем случайные данные для демонстрации
            int baseTemp = 10 + (int) (Math.random() * 20);
            forecast.setTempDay(baseTemp + (int) (Math.random() * 5));
            forecast.setTempMin(baseTemp - (int) (Math.random() * 5));
            forecast.setTempMax(baseTemp + (int) (Math.random() * 8));
            forecast.setTempNight(baseTemp - (int) (Math.random() * 7));
            forecast.setTempEve(baseTemp);
            forecast.setTempMorn(baseTemp - 3);
            
            forecast.setFeelsLikeDay(forecast.getTempDay() - 2);
            forecast.setFeelsLikeNight(forecast.getTempNight() - 2);
            forecast.setFeelsLikeEve(forecast.getTempEve() - 1);
            forecast.setFeelsLikeMorn(forecast.getTempMorn() - 3);
            
            forecast.setPressure(1000 + (int) (Math.random() * 30));
            forecast.setHumidity(40 + (int) (Math.random() * 40));
            forecast.setWindSpeed(3 + (Math.random() * 10));
            forecast.setWindDeg((int) (Math.random() * 360));
            forecast.setClouds((int) (Math.random() * 100));
            
            // 30% шанс дождя
            if (Math.random() < 0.3) {
                forecast.setRainAmount(Math.random() * 10);
                forecast.setIconId("500");
                forecast.setDescription("Небольшой дождь");
                forecast.setMain("Rain");
            } else {
                forecast.setRainAmount(0);
                forecast.setIconId("800");
                forecast.setDescription("Ясно");
                forecast.setMain("Clear");
            }
            
            calendar.add(Calendar.HOUR_OF_DAY, -6);
            forecast.setSunrise(calendar.getTimeInMillis() / 1000);
            calendar.add(Calendar.HOUR_OF_DAY, 12);
            forecast.setSunset(calendar.getTimeInMillis() / 1000);
            
            forecasts.add(forecast);
        }
        
        return forecasts;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelActiveRequests();
    }
} 
