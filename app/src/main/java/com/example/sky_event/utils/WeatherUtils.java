package com.example.sky_event.utils;

import com.example.sky_event.R;

public class WeatherUtils {

    public static int getWeatherIconResource(String iconId) {
        if (iconId == null || iconId.isEmpty()) {
            return R.drawable.ic_clear_day;
        }
        
        switch (iconId) {
            case "01d":
                return R.drawable.ic_clear_day;
            case "01n":
                return R.drawable.ic_clear_night;
            case "02d":
                return R.drawable.ic_partly_cloudy_day;
            case "02n":
                return R.drawable.ic_partly_cloudy_night;
            case "03d":
            case "03n":
                return R.drawable.ic_cloudy;
            case "04d":
            case "04n":
                return R.drawable.ic_cloudy;
            case "09d":
            case "09n":
                return R.drawable.ic_rain;
            case "10d":
                return R.drawable.ic_rainy_day;
            case "10n":
                return R.drawable.ic_rainy_night;
            case "11d":
            case "11n":
                return R.drawable.ic_thunderstorm;
            case "13d":
            case "13n":
                return R.drawable.ic_snow;
            case "50d":
            case "50n":
                return R.drawable.ic_fog;
            
            // Для идентификаторов погоды из климатического API
            case "200":
            case "201":
            case "202":
            case "210":
            case "211":
            case "212":
            case "221":
            case "230":
            case "231":
            case "232":
                return R.drawable.ic_thunderstorm;
            case "300":
            case "301":
            case "302":
            case "310":
            case "311":
            case "312":
            case "313":
            case "314":
            case "321":
                return R.drawable.ic_rain;
            case "500":
            case "501":
            case "502":
            case "503":
            case "504":
            case "511":
            case "520":
            case "521":
            case "522":
            case "531":
                return R.drawable.ic_rain;
            case "600":
            case "601":
            case "602":
            case "611":
            case "612":
            case "613":
            case "615":
            case "616":
            case "620":
            case "621":
            case "622":
                return R.drawable.ic_snow;
            case "701":
            case "711":
            case "721":
            case "731":
            case "741":
            case "751":
            case "761":
            case "762":
            case "771":
            case "781":
                return R.drawable.ic_fog;
            case "800":
                return R.drawable.ic_clear_day;
            case "801":
                return R.drawable.ic_partly_cloudy_day;
            case "802":
            case "803":
            case "804":
                return R.drawable.ic_cloudy;
            default:
                return R.drawable.ic_clear_day;
        }
    }
} 