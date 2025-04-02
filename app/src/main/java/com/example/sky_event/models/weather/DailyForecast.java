package com.example.sky_event.models.weather;

import java.util.Date;

public class DailyForecast {
    private Date date;
    private int minTemperature;
    private int maxTemperature;
    private String icon;
    private String description;
    private int windSpeed;
    private boolean hasRain;

    public DailyForecast(Date date, int minTemperature, int maxTemperature, String icon, 
                          String description, int windSpeed, boolean hasRain) {
        this.date = date;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.icon = icon;
        this.description = description;
        this.windSpeed = windSpeed;
        this.hasRain = hasRain;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(int minTemperature) {
        this.minTemperature = minTemperature;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(int maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(int windSpeed) {
        this.windSpeed = windSpeed;
    }

    public boolean isHasRain() {
        return hasRain;
    }

    public void setHasRain(boolean hasRain) {
        this.hasRain = hasRain;
    }
} 