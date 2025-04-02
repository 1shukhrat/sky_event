package com.example.sky_event.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenWeatherCurrentResponse {
    private String name;
    private Main main;
    private List<Weather> weather;
    private Wind wind;
    private Clouds clouds;
    private Rain rain;
    private long dt;
    private Sys sys;
    
    public String getName() {
        return name;
    }
    
    public Main getMain() {
        return main;
    }
    
    public List<Weather> getWeather() {
        return weather;
    }
    
    public Wind getWind() {
        return wind;
    }
    
    public Clouds getClouds() {
        return clouds;
    }
    
    public Rain getRain() {
        return rain;
    }
    
    public long getDt() {
        return dt;
    }
    
    public Sys getSys() {
        return sys;
    }
    
    public static class Main {
        private double temp;
        
        @SerializedName("feels_like")
        private double feelsLike;
        
        @SerializedName("temp_min")
        private double tempMin;
        
        @SerializedName("temp_max")
        private double tempMax;
        
        private int pressure;
        private int humidity;
        
        public double getTemp() {
            return temp;
        }
        
        public double getFeelsLike() {
            return feelsLike;
        }
        
        public double getTempMin() {
            return tempMin;
        }
        
        public double getTempMax() {
            return tempMax;
        }
        
        public int getPressure() {
            return pressure;
        }
        
        public int getHumidity() {
            return humidity;
        }
    }
    
    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
        
        public int getId() {
            return id;
        }
        
        public String getMain() {
            return main;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getIcon() {
            return icon;
        }
    }
    
    public static class Wind {
        private double speed;
        private int deg;
        private double gust;
        
        public double getSpeed() {
            return speed;
        }
        
        public int getDeg() {
            return deg;
        }
        
        public double getGust() {
            return gust;
        }
    }
    
    public static class Clouds {
        private int all;
        
        public int getAll() {
            return all;
        }
    }
    
    public static class Rain {
        @SerializedName("1h")
        private double oneHour;
        
        @SerializedName("3h")
        private double threeHour;
        
        public double getOneHour() {
            return oneHour;
        }
        
        public double getThreeHour() {
            return threeHour;
        }
    }
    
    public static class Sys {
        private String country;
        private long sunrise;
        private long sunset;
        
        public String getCountry() {
            return country;
        }
        
        public long getSunrise() {
            return sunrise;
        }
        
        public long getSunset() {
            return sunset;
        }
    }
} 