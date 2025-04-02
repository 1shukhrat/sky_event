package com.example.sky_event.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenWeatherForecastResponse {
    private String cod;
    private int message;
    private int cnt;
    private List<ForecastItem> list;
    private City city;
    
    public String getCod() {
        return cod;
    }
    
    public int getMessage() {
        return message;
    }
    
    public int getCnt() {
        return cnt;
    }
    
    public List<ForecastItem> getList() {
        return list;
    }
    
    public City getCity() {
        return city;
    }
    
    public static class ForecastItem {
        private long dt;
        private Main main;
        private List<Weather> weather;
        private Clouds clouds;
        private Wind wind;
        private int visibility;
        private double pop;
        private Rain rain;
        private Snow snow;
        
        @SerializedName("dt_txt")
        private String dtTxt;
        
        public long getDt() {
            return dt;
        }
        
        public Main getMain() {
            return main;
        }
        
        public List<Weather> getWeather() {
            return weather;
        }
        
        public Clouds getClouds() {
            return clouds;
        }
        
        public Wind getWind() {
            return wind;
        }
        
        public int getVisibility() {
            return visibility;
        }
        
        public double getPop() {
            return pop;
        }
        
        public Rain getRain() {
            return rain;
        }
        
        public Snow getSnow() {
            return snow;
        }
        
        public String getDtTxt() {
            return dtTxt;
        }
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
        
        @SerializedName("sea_level")
        private int seaLevel;
        
        @SerializedName("grnd_level")
        private int grndLevel;
        
        @SerializedName("temp_kf")
        private double tempKf;
        
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
        
        public int getSeaLevel() {
            return seaLevel;
        }
        
        public int getGrndLevel() {
            return grndLevel;
        }
        
        public double getTempKf() {
            return tempKf;
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
    
    public static class Clouds {
        private int all;
        
        public int getAll() {
            return all;
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
    
    public static class Rain {
        @SerializedName("3h")
        private double threeHour;
        
        public double getThreeHour() {
            return threeHour;
        }
    }
    
    public static class Snow {
        @SerializedName("3h")
        private double threeHour;
        
        public double getThreeHour() {
            return threeHour;
        }
    }
    
    public static class City {
        private int id;
        private String name;
        private Coord coord;
        private String country;
        private int population;
        private int timezone;
        private long sunrise;
        private long sunset;
        
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
        
        public long getSunrise() {
            return sunrise;
        }
        
        public long getSunset() {
            return sunset;
        }
    }
    
    public static class Coord {
        private double lat;
        private double lon;
        
        public double getLat() {
            return lat;
        }
        
        public double getLon() {
            return lon;
        }
    }
} 