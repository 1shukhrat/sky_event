package com.example.sky_event.models.weather;

import com.google.gson.annotations.SerializedName;

import java.time.ZoneId;
import java.util.List;

public class DailyForecastResponse {
    private City city;
    private String cod;
    private List<WeatherData> list;

    // Getters and Setters
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
    public String getCod() { return cod; }
    public void setCod(String cod) { this.cod = cod; }

    public List<WeatherData> getList() { return list; }
    public void setList(List<WeatherData> list) { this.list = list; }

    public static class City {
        private int id;
        private String name;
        private Coord coord;
        private ZoneId timezone;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Coord getCoord() { return coord; }
        public void setCoord(Coord coord) { this.coord = coord; }
        public ZoneId getTimezone() { return timezone; }
        public void setTimezone(ZoneId timezone) { this.timezone = timezone; }
    }

    public static class Coord {
        private double lat;
        private double lon;

        // Getters and Setters
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }

    public static class WeatherData {
        private long dt;
        private Temp temp;
        private int pressure;
        private int humidity;
        private List<Weather> weather;
        private float speed;
        private int deg;
        private Float gust;
        private int clouds;
        private Float rain;
        private Float snow;
        private float pop;

        // Getters and Setters
        public long getDt() { return dt; }
        public void setDt(long dt) { this.dt = dt; }
        public Temp getTemp() { return temp; }
        public void setTemp(Temp temp) { this.temp = temp; }
        public int getPressure() { return pressure; }
        public void setPressure(int pressure) { this.pressure = pressure; }
        public int getHumidity() { return humidity; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
        public List<Weather> getWeather() { return weather; }
        public void setWeather(List<Weather> weather) { this.weather = weather; }
        public float getSpeed() { return speed; }
        public void setSpeed(float speed) { this.speed = speed; }
        public int getDeg() { return deg; }
        public void setDeg(int deg) { this.deg = deg; }
        public Float getGust() { return gust; }
        public void setGust(Float gust) { this.gust = gust; }
        public int getClouds() { return clouds; }
        public void setClouds(int clouds) { this.clouds = clouds; }
        public Float getRain() { return rain; }
        public void setRain(Float rain) { this.rain = rain; }
        public Float getSnow() { return snow; }
        public void setSnow(Float snow) { this.snow = snow; }
        public float getPop() { return pop; }
        public void setPop(float pop) { this.pop = pop; }
    }

    public static class Temp {
        private float day;
        private float min;
        private float max;
        private float night;
        private float eve;
        private float morn;

        // Getters and Setters
        public float getDay() { return day; }
        public void setDay(float day) { this.day = day; }
        public float getMin() { return min; }
        public void setMin(float min) { this.min = min; }
        public float getMax() { return max; }
        public void setMax(float max) { this.max = max; }
        public float getNight() { return night; }
        public void setNight(float night) { this.night = night; }
        public float getEve() { return eve; }
        public void setEve(float eve) { this.eve = eve; }
        public float getMorn() { return morn; }
        public void setMorn(float morn) { this.morn = morn; }
    }

    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getMain() { return main; }
        public void setMain(String main) { this.main = main; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
}