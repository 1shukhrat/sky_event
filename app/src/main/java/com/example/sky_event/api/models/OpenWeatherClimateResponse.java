package com.example.sky_event.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenWeatherClimateResponse {
    private City city;
    private String cod;
    private double message;
    private int cnt;
    private List<DailyForecast> list;
    
    public City getCity() {
        return city;
    }
    
    public String getCod() {
        return cod;
    }
    
    public double getMessage() {
        return message;
    }
    
    public int getCnt() {
        return cnt;
    }
    
    public List<DailyForecast> getList() {
        return list;
    }
    
    public static class DailyForecast {
        private long dt;
        private long sunrise;
        private long sunset;
        private Temp temp;
        
        @SerializedName("feels_like")
        private FeelsLike feelsLike;
        
        private int pressure;
        private int humidity;
        private List<Weather> weather;
        private double speed;
        private int deg;
        private int clouds;
        private double rain;
        
        public long getDt() {
            return dt;
        }
        
        public long getSunrise() {
            return sunrise;
        }
        
        public long getSunset() {
            return sunset;
        }
        
        public Temp getTemp() {
            return temp;
        }
        
        public FeelsLike getFeelsLike() {
            return feelsLike;
        }
        
        public int getPressure() {
            return pressure;
        }
        
        public int getHumidity() {
            return humidity;
        }
        
        public List<Weather> getWeather() {
            return weather;
        }
        
        public double getSpeed() {
            return speed;
        }
        
        public int getDeg() {
            return deg;
        }
        
        public int getClouds() {
            return clouds;
        }
        
        public double getRain() {
            return rain;
        }
    }
    
    public static class Temp {
        private double day;
        private double min;
        private double max;
        private double night;
        private double eve;
        private double morn;
        
        public double getDay() {
            return day;
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
        
        public double getNight() {
            return night;
        }
        
        public double getEve() {
            return eve;
        }
        
        public double getMorn() {
            return morn;
        }
    }
    
    public static class FeelsLike {
        private double day;
        private double night;
        private double eve;
        private double morn;
        
        public double getDay() {
            return day;
        }
        
        public double getNight() {
            return night;
        }
        
        public double getEve() {
            return eve;
        }
        
        public double getMorn() {
            return morn;
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
    
    public static class City {
        private int id;
        private String name;
        private Coord coord;
        private String country;
        private int population;
        private int timezone;
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public Coord getCoord() {
            return coord;
        }
        
        public String getCountry() {
            return country;
        }
        
        public int getPopulation() {
            return population;
        }
        
        public int getTimezone() {
            return timezone;
        }
    }
    
    public static class Coord {
        private double lon;
        private double lat;
        
        public double getLon() {
            return lon;
        }
        
        public double getLat() {
            return lat;
        }
    }
} 