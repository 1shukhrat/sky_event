package com.example.sky_event.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sky_event.api.RetrofitClient;
import com.example.sky_event.api.WeatherService;
import com.example.sky_event.api.models.OpenWeatherClimateResponse;
import com.example.sky_event.database.AppDatabase;
import com.example.sky_event.database.dao.WeatherDao;
import com.example.sky_event.database.entity.WeatherEntity;
import com.example.sky_event.models.weather.CurrentWeatherResponse;
import com.example.sky_event.models.weather.DailyForecastResponse;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.models.weather.HourlyForecastResponse;
import com.example.sky_event.models.weather.MonthlyForecast;
import com.example.sky_event.network.WeatherApi;
import com.example.sky_event.utils.AppExecutors;
import com.example.sky_event.utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {
    private static final String TAG = "WeatherRepository";
    private static final String API_KEY = Constants.OPEN_WEATHER_API_KEY;
    private static final String UNITS = "metric";
    private static final String LANG = "ru";
    
    private static WeatherRepository instance;
    private final WeatherApi weatherApi;
    private final WeatherDao weatherDao;
    private final WeatherService weatherService;
    private final AppExecutors executors;
    private final MutableLiveData<CurrentWeatherResponse> currentWeatherLiveData = new MutableLiveData<>();
    private final MutableLiveData<ForecastResponse> forecastLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private WeatherRepository(Context context) {
        weatherApi = com.example.sky_event.network.RetrofitClient.getInstance().getWeatherApi();
        AppDatabase database = AppDatabase.getInstance(context);
        this.weatherDao = database.weatherDao();
        this.weatherService = com.example.sky_event.api.RetrofitClient.getClient().create(WeatherService.class);
        this.executors = AppExecutors.getInstance();
    }

    public static synchronized WeatherRepository getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherRepository(context);
        }
        return instance;
    }

    public LiveData<CurrentWeatherResponse> getCurrentWeather(double lat, double lon) {
        MutableLiveData<CurrentWeatherResponse> data = new MutableLiveData<>();
        
        weatherApi.getCurrentWeather(lat, lon, UNITS, LANG, API_KEY)
                .enqueue(new Callback<CurrentWeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CurrentWeatherResponse> call, 
                                           @NonNull Response<CurrentWeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            data.setValue(response.body());
                            currentWeatherLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Ошибка получения данных о погоде");
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<CurrentWeatherResponse> call, @NonNull Throwable t) {
                        errorLiveData.setValue("Сетевая ошибка: " + t.getMessage());
                    }
                });
        
        return data;
    }
    
    public LiveData<ForecastResponse> getForecast(double lat, double lon) {
        MutableLiveData<ForecastResponse> data = new MutableLiveData<>();
        
        weatherApi.get3HourForecastFor5Days(lat, lon, UNITS, LANG, API_KEY)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ForecastResponse> call, 
                                           @NonNull Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            data.setValue(response.body());
                            forecastLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Ошибка получения данных о прогнозе");
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<ForecastResponse> call, @NonNull Throwable t) {
                        errorLiveData.setValue("Сетевая ошибка: " + t.getMessage());
                    }
                });
        
        return data;
    }

    public LiveData<CurrentWeatherResponse> getCurrentWeather() {
        return currentWeatherLiveData;
    }
    
    public LiveData<ForecastResponse> getForecast() {
        return forecastLiveData;
    }
    
    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void fetchWeatherByCity(String cityName) {
        weatherApi.getCurrentWeatherByCity(cityName, "metric", Constants.OPEN_WEATHER_API_KEY)
                .enqueue(new Callback<CurrentWeatherResponse>() {
                    @Override
                    public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentWeatherLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Ошибка получения данных о погоде для города");
                        }
                    }

                    @Override
                    public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                        errorLiveData.setValue("Сетевая ошибка: " + t.getMessage());
                    }
                });
    }

    public void fetchForecast(double latitude, double longitude) {
        weatherApi.get3HourForecastFor5Days(latitude, longitude, UNITS, LANG, API_KEY)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ForecastResponse> call, 
                                          @NonNull Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            forecastLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Ошибка получения данных о прогнозе");
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<ForecastResponse> call, @NonNull Throwable t) {
                        errorLiveData.setValue("Сетевая ошибка: " + t.getMessage());
                    }
                });
    }

    public void fetchForecastByCity(String cityName) {
        weatherApi.getForecastByCity(cityName, UNITS, API_KEY)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            forecastLiveData.setValue(response.body());
                        } else {
                            errorLiveData.setValue("Ошибка получения данных о прогнозе для города");
                        }
                    }

                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        errorLiveData.setValue("Сетевая ошибка: " + t.getMessage());
                    }
                });
    }

    public LiveData<List<WeatherEntity>> getWeatherForecast(double latitude, double longitude) {
        MutableLiveData<List<WeatherEntity>> result = new MutableLiveData<>();
        
        executors.diskIO().execute(() -> {
            Calendar startDate = Calendar.getInstance();
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);
            
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.DAY_OF_YEAR, 7);
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);
            
            List<WeatherEntity> cachedForecast = weatherDao.getWeatherForLocationBetweenDates(
                    latitude, longitude, startDate.getTime(), endDate.getTime());
            
            if (cachedForecast != null && !cachedForecast.isEmpty() && !isDataExpired(cachedForecast)) {
                result.postValue(cachedForecast);
            } else {
                fetchForecastFromApi(latitude, longitude, result);
            }
        });
        
        return result;
    }
    
    public WeatherEntity getWeatherForLocationAndDate(double latitude, double longitude, Date date) {
        Future<WeatherEntity> future = executors.diskIO().submit(() -> 
                weatherDao.getWeatherForLocationAndDate(latitude, longitude, date));
        
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }
    
    public List<WeatherEntity> getWeatherForLocationBetweenDatesSync(double latitude, double longitude, 
                                                                  Date startDate, Date endDate) {
        Future<List<WeatherEntity>> future = executors.diskIO().submit(() -> 
                weatherDao.getWeatherForLocationBetweenDates(latitude, longitude, startDate, endDate));
        
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return new ArrayList<>();
        }
    }
    
    public List<WeatherEntity> getWeatherForLocationNameBetweenDatesSync(String location, 
                                                                      Date startDate, Date endDate) {
        Future<List<WeatherEntity>> future = executors.diskIO().submit(() -> 
                weatherDao.getWeatherForLocationNameBetweenDates(location, startDate, endDate));
        
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return new ArrayList<>();
        }
    }
    
    private boolean isDataExpired(List<WeatherEntity> weatherEntities) {
        if (weatherEntities.isEmpty()) {
            return true;
        }
        
        for (WeatherEntity entity : weatherEntities) {
            if (entity.isExpired()) {
                return true;
            }
        }
        
        return false;
    }
    
    private void fetchForecastFromApi(double latitude, double longitude, MutableLiveData<List<WeatherEntity>> result) {
        weatherService.getForecast(latitude, longitude, API_KEY, "metric", "ru")
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ForecastResponse forecastResponse = response.body();
                            
                            executors.diskIO().execute(() -> {
                                List<WeatherEntity> weatherEntities = convertToWeatherEntities(forecastResponse, latitude, longitude);
                                weatherDao.insertAll(weatherEntities);
                                result.postValue(weatherEntities);
                            });
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        result.postValue(new ArrayList<>());
                    }
                });
    }
    
    private List<WeatherEntity> convertToWeatherEntities(ForecastResponse forecastResponse, double latitude, double longitude) {
        List<WeatherEntity> entities = new ArrayList<>();
        
        if (forecastResponse.getList() != null) {
            for (ForecastResponse.ForecastItem item : forecastResponse.getList()) {
                WeatherEntity entity = new WeatherEntity();
                
                entity.setLatitude(latitude);
                entity.setLongitude(longitude);
                if (forecastResponse.getCity() != null) {
                    entity.setLocation(forecastResponse.getCity().getName());
                }
                
                if (item.getMain() != null) {
                    entity.setTemperature(item.getMain().getTemp());
                    entity.setTempMin(item.getMain().getTemp_min());
                    entity.setTempMax(item.getMain().getTemp_max());
                    entity.setFeelsLike(item.getMain().getFeels_like());
                    entity.setHumidity(item.getMain().getHumidity());
                    entity.setPressure(item.getMain().getPressure());
                }
                
                if (item.getWind() != null) {
                    entity.setWindSpeed(item.getWind().getSpeed());
                }
                
                if (item.getWeather() != null && !item.getWeather().isEmpty()) {
                    CurrentWeatherResponse.Weather weather = item.getWeather().get(0);
                    entity.setDescription(weather.getDescription());
                    entity.setIcon(String.valueOf(weather.getId()));
                    entity.setMainCondition(weather.getMain());
                }
                
                entity.setHasPrecipitation(item.getRain() != null && item.getRain().getThreeHour() > 0);
                
                entity.setForecastDate(new Date(item.getDt() * 1000L));
                
                entities.add(entity);
            }
        }
        
        return entities;
    }
    
    public void clearExpiredData() {
        executors.diskIO().execute(() -> {
            weatherDao.deleteExpiredWeather(System.currentTimeMillis());
        });
    }

    public WeatherEntity syncWeatherForLocation(double latitude, double longitude) {
        try {
            Call<CurrentWeatherResponse> call = weatherApi.getCurrentWeather(
                    latitude, longitude, "metric", "ru", API_KEY);
            
            Response<CurrentWeatherResponse> response = call.execute();
            
            if (response.isSuccessful() && response.body() != null) {
                CurrentWeatherResponse weatherResponse = response.body();
                
                WeatherEntity entity = new WeatherEntity();
                entity.setLatitude(latitude);
                entity.setLongitude(longitude);
                entity.setTemperature(weatherResponse.getMain().getTemp());
                entity.setFeelsLike(weatherResponse.getMain().getFeels_like());
                entity.setTempMin(weatherResponse.getMain().getTemp_min());
                entity.setTempMax(weatherResponse.getMain().getTemp_max());
                entity.setPressure(weatherResponse.getMain().getPressure());
                entity.setHumidity(weatherResponse.getMain().getHumidity());
                entity.setWindSpeed(weatherResponse.getWind().getSpeed());
                
                if (weatherResponse.getWeather() != null && !weatherResponse.getWeather().isEmpty()) {
                    entity.setDescription(weatherResponse.getWeather().get(0).getDescription());
                    entity.setMainCondition(weatherResponse.getWeather().get(0).getMain());
                    entity.setIcon(weatherResponse.getWeather().get(0).getIcon());
                }

                entity.setHasPrecipitation(weatherResponse.getRain() != null || weatherResponse.getSnow() != null);
                entity.setForecastDate(new Date(weatherResponse.getDt() * 1000));
                entity.setTimestamp(new Date());
                
                executors.diskIO().execute(() -> {
                    weatherDao.insert(entity);
                });
                
                return entity;
            }
        } catch (Exception e) {
            errorLiveData.setValue("Ошибка при синхронизации данных о погоде: " + e.getMessage());
        }
        
        return null;
    }

    public LiveData<List<MonthlyForecast>> getClimate30DayForecast(double latitude, double longitude) {
        MutableLiveData<List<MonthlyForecast>> result = new MutableLiveData<>();
        
        WeatherService climateService = RetrofitClient.getProClient().create(WeatherService.class);
        
        climateService.getClimate30DayForecast(latitude, longitude, API_KEY, UNITS, LANG)
                .enqueue(new Callback<OpenWeatherClimateResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OpenWeatherClimateResponse> call, 
                                           @NonNull Response<OpenWeatherClimateResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            OpenWeatherClimateResponse climateForecast = response.body();
                            List<MonthlyForecast> monthlyForecasts = convertToMonthlyForecasts(climateForecast);
                            result.postValue(monthlyForecasts);
                        } else {
                            result.postValue(new ArrayList<>());
                            errorLiveData.postValue("Ошибка при получении прогноза на 30 дней: " + 
                                    (response.errorBody() != null ? response.code() : "Неизвестная ошибка"));
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<OpenWeatherClimateResponse> call, @NonNull Throwable t) {
                        result.postValue(new ArrayList<>());
                        errorLiveData.postValue("Ошибка сети при загрузке прогноза на 30 дней: " + t.getMessage());
                    }
                });
        
        return result;
    }
    
    private List<MonthlyForecast> convertToMonthlyForecasts(OpenWeatherClimateResponse climateResponse) {
        List<MonthlyForecast> forecasts = new ArrayList<>();
        
        if (climateResponse.getList() != null) {
            for (OpenWeatherClimateResponse.DailyForecast item : climateResponse.getList()) {
                MonthlyForecast forecast = new MonthlyForecast();
                
                forecast.setDate(new Date(item.getDt() * 1000L));
                
                if (item.getTemp() != null) {
                    forecast.setTempDay(item.getTemp().getDay());
                    forecast.setTempMin(item.getTemp().getMin());
                    forecast.setTempMax(item.getTemp().getMax());
                    forecast.setTempNight(item.getTemp().getNight());
                    forecast.setTempEve(item.getTemp().getEve());
                    forecast.setTempMorn(item.getTemp().getMorn());
                }
                
                if (item.getFeelsLike() != null) {
                    forecast.setFeelsLikeDay(item.getFeelsLike().getDay());
                    forecast.setFeelsLikeNight(item.getFeelsLike().getNight());
                    forecast.setFeelsLikeEve(item.getFeelsLike().getEve());
                    forecast.setFeelsLikeMorn(item.getFeelsLike().getMorn());
                }
                
                forecast.setPressure(item.getPressure());
                forecast.setHumidity(item.getHumidity());
                forecast.setWindSpeed(item.getSpeed());
                forecast.setWindDeg(item.getDeg());
                forecast.setClouds(item.getClouds());
                forecast.setRainAmount(item.getRain());
                forecast.setSunrise(item.getSunrise());
                forecast.setSunset(item.getSunset());
                
                if (item.getWeather() != null && !item.getWeather().isEmpty()) {
                    OpenWeatherClimateResponse.Weather weather = item.getWeather().get(0);
                    forecast.setIconId(String.valueOf(weather.getId()));
                    forecast.setDescription(weather.getDescription());
                    forecast.setMain(weather.getMain());
                }
                
                forecasts.add(forecast);
            }
        }
        
        return forecasts;
    }


    public HourlyForecastResponse getHourlyForecastFor4Days(double latitude, double longitude) throws IOException {
        return weatherApi.getHourlyForecastFor4Days(latitude, longitude, UNITS, LANG, API_KEY).execute().body();
    }

    public ForecastResponse get3HourForecastFor5Days(double latitude, double longitude) throws IOException {
        return weatherApi.get3HourForecastFor5Days(latitude, longitude, UNITS, LANG, API_KEY).execute().body();
    }

    public DailyForecastResponse getDailyForecastFor16Days(double latitude, double longitude) throws IOException {
        return weatherApi.getDailyForecastFor16Days(latitude, longitude, UNITS, LANG, API_KEY).execute().body();
    }


} 