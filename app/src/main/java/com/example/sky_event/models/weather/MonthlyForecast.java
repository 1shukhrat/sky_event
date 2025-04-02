package com.example.sky_event.models.weather;

import java.util.Date;
import java.util.List;

public class MonthlyForecast {
    private Date date;
    private double tempDay;
    private double tempMin;
    private double tempMax;
    private double tempNight;
    private double tempEve;
    private double tempMorn;
    private double feelsLikeDay;
    private double feelsLikeNight;
    private double feelsLikeEve;
    private double feelsLikeMorn;
    private int pressure;
    private int humidity;
    private double windSpeed;
    private int windDeg;
    private int clouds;
    private double rainAmount;
    private long sunrise;
    private long sunset;
    private String iconId;
    private String description;
    private String main;
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public double getTempDay() {
        return tempDay;
    }
    
    public void setTempDay(double tempDay) {
        this.tempDay = tempDay;
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
    
    public double getTempNight() {
        return tempNight;
    }
    
    public void setTempNight(double tempNight) {
        this.tempNight = tempNight;
    }
    
    public double getTempEve() {
        return tempEve;
    }
    
    public void setTempEve(double tempEve) {
        this.tempEve = tempEve;
    }
    
    public double getTempMorn() {
        return tempMorn;
    }
    
    public void setTempMorn(double tempMorn) {
        this.tempMorn = tempMorn;
    }
    
    public double getFeelsLikeDay() {
        return feelsLikeDay;
    }
    
    public void setFeelsLikeDay(double feelsLikeDay) {
        this.feelsLikeDay = feelsLikeDay;
    }
    
    public double getFeelsLikeNight() {
        return feelsLikeNight;
    }
    
    public void setFeelsLikeNight(double feelsLikeNight) {
        this.feelsLikeNight = feelsLikeNight;
    }
    
    public double getFeelsLikeEve() {
        return feelsLikeEve;
    }
    
    public void setFeelsLikeEve(double feelsLikeEve) {
        this.feelsLikeEve = feelsLikeEve;
    }
    
    public double getFeelsLikeMorn() {
        return feelsLikeMorn;
    }
    
    public void setFeelsLikeMorn(double feelsLikeMorn) {
        this.feelsLikeMorn = feelsLikeMorn;
    }
    
    public int getPressure() {
        return pressure;
    }
    
    public void setPressure(int pressure) {
        this.pressure = pressure;
    }
    
    public int getHumidity() {
        return humidity;
    }
    
    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }
    
    public double getWindSpeed() {
        return windSpeed;
    }
    
    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }
    
    public int getWindDeg() {
        return windDeg;
    }
    
    public void setWindDeg(int windDeg) {
        this.windDeg = windDeg;
    }
    
    public int getClouds() {
        return clouds;
    }
    
    public void setClouds(int clouds) {
        this.clouds = clouds;
    }
    
    public double getRainAmount() {
        return rainAmount;
    }
    
    public void setRainAmount(double rainAmount) {
        this.rainAmount = rainAmount;
    }
    
    public long getSunrise() {
        return sunrise;
    }
    
    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }
    
    public long getSunset() {
        return sunset;
    }
    
    public void setSunset(long sunset) {
        this.sunset = sunset;
    }
    
    public String getIconId() {
        return iconId;
    }
    
    public void setIconId(String iconId) {
        this.iconId = iconId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMain() {
        return main;
    }
    
    public void setMain(String main) {
        this.main = main;
    }
} 