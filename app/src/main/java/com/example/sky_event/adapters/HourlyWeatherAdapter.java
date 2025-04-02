package com.example.sky_event.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sky_event.R;
import com.example.sky_event.models.weather.HourlyForecast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HourlyWeatherAdapter extends ListAdapter<HourlyForecast, HourlyWeatherAdapter.HourlyViewHolder> {
    private static final SimpleDateFormat hourFormat = new SimpleDateFormat("HH:00", Locale.getDefault());
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
    
    public HourlyWeatherAdapter() {
        super(new HourlyDiffCallback());
    }
    
    @NonNull
    @Override
    public HourlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new HourlyViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HourlyViewHolder holder, int position) {
        holder.bind(getItem(position), position);
    }
    
    static class HourlyViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeTextView;
        private final TextView temperatureTextView;
        private final ImageView weatherIconImageView;
        private final TextView descriptionTextView;
        
        public HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            temperatureTextView = itemView.findViewById(R.id.temperatureTextView);
            weatherIconImageView = itemView.findViewById(R.id.weatherIconImageView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
        }
        
        public void bind(HourlyForecast forecast, int position) {
            Date forecastTime = forecast.getTime();
            
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            
            Calendar forecastDate = Calendar.getInstance();
            forecastDate.setTime(forecastTime);
            
            String timeString;
            if (position == 0) {
                timeString = "Сейчас";
            } else if (forecastDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && 
                forecastDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                timeString = hourFormat.format(forecastTime);
            } else if (forecastDate.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) && 
                       forecastDate.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR)) {
                timeString = "Завтра\n" + hourFormat.format(forecastTime);
            } else {
                timeString = hourFormat.format(forecastTime) + "\n" + dateFormat.format(forecastTime);
            }
            
            timeTextView.setText(timeString);
            temperatureTextView.setText(String.format("%d°C", forecast.getTemperature()));
            descriptionTextView.setText(forecast.getDescription());
            
            int iconResId = getWeatherIconResource(forecast.getIcon());
            weatherIconImageView.setImageResource(iconResId);
        }
        
        private int getWeatherIconResource(String icon) {
            switch (icon) {
                case "01d": return R.drawable.ic_clear_day;
                case "01n": return R.drawable.ic_clear_night;
                case "02d": return R.drawable.ic_partly_cloudy_day;
                case "02n": return R.drawable.ic_partly_cloudy_night;
                case "03d":
                case "03n": return R.drawable.ic_cloudy;
                case "04d":
                case "04n": return R.drawable.ic_cloudy;
                case "09d":
                case "09n": return R.drawable.ic_rain;
                case "10d": return R.drawable.ic_rainy_day;
                case "10n": return R.drawable.ic_rainy_night;
                case "11d":
                case "11n": return R.drawable.ic_thunderstorm;
                case "13d":
                case "13n": return R.drawable.ic_snow;
                case "50d":
                case "50n": return R.drawable.ic_fog;
                default: return R.drawable.ic_weather;
            }
        }
    }
    
    private static class HourlyDiffCallback extends DiffUtil.ItemCallback<HourlyForecast> {
        @Override
        public boolean areItemsTheSame(@NonNull HourlyForecast oldItem, @NonNull HourlyForecast newItem) {
            return oldItem.getTime().equals(newItem.getTime());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull HourlyForecast oldItem, @NonNull HourlyForecast newItem) {
            return oldItem.getTemperature() == newItem.getTemperature() &&
                   oldItem.getIcon().equals(newItem.getIcon()) &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.isHasRain() == newItem.isHasRain();
        }
    }
} 