package com.example.sky_event.api;

import com.example.sky_event.api.models.OpenWeatherCurrentResponse;
import com.example.sky_event.api.models.OpenWeatherForecastResponse;
import com.example.sky_event.api.models.OpenWeatherClimateResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherMapService {
    @GET("weather")
    Call<OpenWeatherCurrentResponse> getCurrentWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
    
    @GET("forecast")
    Call<OpenWeatherForecastResponse> getForecast(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
    
    @GET("forecast/climate")
    Call<OpenWeatherClimateResponse> getClimate30DayForecast(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
} 