package com.example.sky_event.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.sky_event.database.converters.DateConverter;

import java.util.Date;

@Entity(tableName = "weather")
public class WeatherEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private double latitude;
    private double longitude;
    private String location;
    
    private double temperature;
    private double tempMin;
    private double tempMax;
    private double feelsLike;
    private double windSpeed;
    private int humidity;
    private int pressure;
    private String description;
    private String icon;
    private String mainCondition;
    private boolean hasRain;
    
    private Date timestamp;
    private Date forecastDate;
    private long expiryTime;
    
    public WeatherEntity() {
        this.timestamp = new Date();
        this.expiryTime = System.currentTimeMillis() + (3 * 60 * 60 * 1000);
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public double getTempMin() {
        return tempMin;
    }
    
    public void setTempMin(double tempMin) {
        this.tempMin = tempMin;
    }
    
    public double getTempMax() {
        return tempMax;
    }
    
    public void setTempMax(double tempMax) {
        this.tempMax = tempMax;
    }
    
    public double getFeelsLike() {
        return feelsLike;
    }
    
    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }
    
    public double getWindSpeed() {
        return windSpeed;
    }
    
    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }
    
    public int getHumidity() {
        return humidity;
    }
    
    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }
    
    public int getPressure() {
        return pressure;
    }
    
    public void setPressure(int pressure) {
        this.pressure = pressure;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getMainCondition() {
        return mainCondition;
    }
    
    public void setMainCondition(String mainCondition) {
        this.mainCondition = mainCondition;
    }
    
    public boolean isHasRain() {
        return hasRain;
    }
    
    public void setHasRain(boolean hasRain) {
        this.hasRain = hasRain;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public Date getForecastDate() {
        return forecastDate;
    }
    
    public void setForecastDate(Date forecastDate) {
        this.forecastDate = forecastDate;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
} 