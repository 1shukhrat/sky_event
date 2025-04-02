package com.example.sky_event.models.weather;

import java.util.Date;

public class HourlyForecast {
    private Date time;
    private int temperature;
    private String icon;
    private String description;
    private int windSpeed;
    private boolean hasRain;

    public HourlyForecast(Date time, int temperature, String icon, String description, int windSpeed) {
        this.time = time;
        this.temperature = temperature;
        this.icon = icon;
        this.description = description;
        this.windSpeed = windSpeed;
        this.hasRain = false;
    }
    
    public HourlyForecast(Date time, int temperature, String icon, String description, int windSpeed, boolean hasRain) {
        this.time = time;
        this.temperature = temperature;
        this.icon = icon;
        this.description = description;
        this.windSpeed = windSpeed;
        this.hasRain = hasRain;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
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