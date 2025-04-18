package com.example.sky_event.models.weather;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class HourlyForecastResponse {
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
        private CurrentWeatherResponse.Main main;
        private List<CurrentWeatherResponse.Weather> weather;
        private CurrentWeatherResponse.Clouds clouds;
        private CurrentWeatherResponse.Wind wind;
        private int visibility;
        private double pop;
        private Rain rain;
        private String dt_txt;

        public long getDt() {
            return dt;
        }

        public CurrentWeatherResponse.Main getMain() {
            return main;
        }

        public List<CurrentWeatherResponse.Weather> getWeather() {
            return weather;
        }

        public CurrentWeatherResponse.Clouds getClouds() {
            return clouds;
        }

        public CurrentWeatherResponse.Wind getWind() {
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

        public String getDt_txt() {
            return dt_txt;
        }
    }

    public static class Rain {
        @SerializedName("1h")
        private float threeHour;

        public float getThreeHour() {
            return threeHour;
        }

        public void setThreeHour(float threeHour) {
            this.threeHour = threeHour;
        }
    }

    public static class Snow {
        @SerializedName("1h")
        private float threeHour;

        public float getThreeHour() {
            return threeHour;
        }

        public void setThreeHour(float threeHour) {
            this.threeHour = threeHour;
        }
    }

    public static class City {
        private long id;
        private String name;
        private CurrentWeatherResponse.Coord coord;
        private String country;
        private int population;
        private int timezone;
        private long sunrise;
        private long sunset;

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public CurrentWeatherResponse.Coord getCoord() {
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
}
