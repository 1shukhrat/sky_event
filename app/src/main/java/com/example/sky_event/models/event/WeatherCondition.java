package com.example.sky_event.models.event;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class WeatherCondition implements Parcelable {
    private double minTemperature;
    private double maxTemperature;
    private double maxWindSpeed;
    private boolean noRain;
    private List<String> allowedConditions;
    private int maxHumidity;
    private double currentTemperature;
    private double currentWindSpeed;
    private int currentHumidity;

    public WeatherCondition() {
        this.minTemperature = 15;
        this.maxTemperature = 30;
        this.maxWindSpeed = 10;
        this.noRain = true;
        this.allowedConditions = new ArrayList<>();
        this.maxHumidity = 75;
        this.currentTemperature = 0;
        this.currentWindSpeed = 0;
        this.currentHumidity = 0;
    }
    
    protected WeatherCondition(Parcel in) {
        minTemperature = in.readDouble();
        maxTemperature = in.readDouble();
        maxWindSpeed = in.readDouble();
        noRain = in.readByte() != 0;
        allowedConditions = new ArrayList<>();
        in.readStringList(allowedConditions);
        maxHumidity = in.readInt();
        currentTemperature = in.readDouble();
        currentWindSpeed = in.readDouble();
        currentHumidity = in.readInt();
    }

    public static final Creator<WeatherCondition> CREATOR = new Creator<WeatherCondition>() {
        @Override
        public WeatherCondition createFromParcel(Parcel in) {
            return new WeatherCondition(in);
        }

        @Override
        public WeatherCondition[] newArray(int size) {
            return new WeatherCondition[size];
        }
    };

    public double getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public double getMaxWindSpeed() {
        return maxWindSpeed;
    }

    public void setMaxWindSpeed(double maxWindSpeed) {
        this.maxWindSpeed = maxWindSpeed;
    }

    public boolean isNoRain() {
        return noRain;
    }

    public void setNoRain(boolean noRain) {
        this.noRain = noRain;
    }

    public List<String> getAllowedConditions() {
        return allowedConditions;
    }

    public void setAllowedConditions(List<String> allowedConditions) {
        this.allowedConditions = allowedConditions;
    }
    
    public int getMaxHumidity() {
        return maxHumidity;
    }
    
    public void setMaxHumidity(int maxHumidity) {
        this.maxHumidity = maxHumidity;
    }
    
    public double getCurrentTemperature() {
        return currentTemperature;
    }
    
    public void setCurrentTemperature(double currentTemperature) {
        this.currentTemperature = currentTemperature;
    }
    
    public double getCurrentWindSpeed() {
        return currentWindSpeed;
    }
    
    public void setCurrentWindSpeed(double currentWindSpeed) {
        this.currentWindSpeed = currentWindSpeed;
    }
    
    public int getCurrentHumidity() {
        return currentHumidity;
    }
    
    public void setCurrentHumidity(int currentHumidity) {
        this.currentHumidity = currentHumidity;
    }

    public boolean isSuitableWeather(double temperature, double windSpeed, String condition, boolean hasRain) {
        return isSuitableWeather(temperature, windSpeed, condition, hasRain, 50);
    }
    
    public boolean isSuitableWeather(double temperature, double windSpeed, String condition, boolean hasRain, int humidity) {
        if (temperature < minTemperature || temperature > maxTemperature) {
            return false;
        }
        
        if (windSpeed > maxWindSpeed) {
            return false;
        }
        
        if (noRain && hasRain) {
            return false;
        }
        
        if (humidity > maxHumidity) {
            return false;
        }
        
        if (!allowedConditions.isEmpty()) {
            boolean isAllowedCondition = false;
            
            for (String allowed : allowedConditions) {
                if (condition.equalsIgnoreCase(allowed) || 
                        (condition.toLowerCase().contains(allowed.toLowerCase()))) {
                    isAllowedCondition = true;
                    break;
                }
            }
            
            if (!isAllowedCondition) {
                return false;
            }
        } else if (condition.equalsIgnoreCase("Thunderstorm") || 
                condition.equalsIgnoreCase("Snow") || 
                condition.equalsIgnoreCase("Extreme")) {
            return false;
        }
        
        return true;
    }
    
    public double calculateComfortScore(double temperature, double windSpeed, boolean hasRain, int humidity) {
        double score = 100;
        
        double tempScore = 100 - (Math.abs(temperature - 22) * 3);
        double windScore = 100 - (windSpeed * 5);
        double rainScore = hasRain ? 50 : 100;
        double humidityScore = 100 - (humidity * 0.5);
        
        score = (tempScore * 0.4) + (windScore * 0.3) + (rainScore * 0.2) + (humidityScore * 0.1);
        
        return Math.max(0, Math.min(100, score));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(minTemperature);
        dest.writeDouble(maxTemperature);
        dest.writeDouble(maxWindSpeed);
        dest.writeByte((byte) (noRain ? 1 : 0));
        dest.writeStringList(allowedConditions);
        dest.writeInt(maxHumidity);
        dest.writeDouble(currentTemperature);
        dest.writeDouble(currentWindSpeed);
        dest.writeInt(currentHumidity);
    }
} 