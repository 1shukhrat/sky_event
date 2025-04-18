package com.example.sky_event.network;

import com.example.sky_event.models.weather.CurrentWeatherResponse;
import com.example.sky_event.models.weather.DailyForecastResponse;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.models.weather.HourlyForecastResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("weather")
    Call<CurrentWeatherResponse> getCurrentWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units,
            @Query("lang") String lang,
            @Query("appid") String apiKey);

    @GET("forecast")
    Call<ForecastResponse> get3HourForecastFor5Days(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units,
            @Query("lang") String lang,
            @Query("appid") String apiKey);

    @GET("forecast/hourly")
    Call<HourlyForecastResponse> getHourlyForecastFor4Days(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units,
            @Query("lang") String lang,
            @Query("appid") String apiKey);

    @GET("forecast/daily")
    Call<DailyForecastResponse> getDailyForecastFor16Days(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units,
            @Query("lang") String lang,
            @Query("appid") String apiKey);





    @GET("weather")
    Call<CurrentWeatherResponse> getCurrentWeatherByCity(
            @Query("q") String city,
            @Query("units") String units,
            @Query("appid") String apiKey);
            
    @GET("forecast")
    Call<ForecastResponse> getForecastByCity(
            @Query("q") String city,
            @Query("units") String units,
            @Query("appid") String apiKey);
} 