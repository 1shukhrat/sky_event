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
import com.example.sky_event.models.weather.DailyForecast;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DailyWeatherAdapter extends ListAdapter<DailyForecast, DailyWeatherAdapter.DailyViewHolder> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
    
    public DailyWeatherAdapter() {
        super(new DailyDiffCallback());
    }
    
    @NonNull
    @Override
    public DailyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new DailyViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DailyViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
    
    static class DailyViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;
        private final TextView temperatureTextView;
        private final ImageView weatherIconImageView;
        private final TextView descriptionTextView;
        private final TextView windTextView;
        private final TextView rainTextView;
        
        public DailyViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            temperatureTextView = itemView.findViewById(R.id.temperatureTextView);
            weatherIconImageView = itemView.findViewById(R.id.weatherIconImageView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            windTextView = itemView.findViewById(R.id.windTextView);
            rainTextView = itemView.findViewById(R.id.rainTextView);
        }
        
        public void bind(DailyForecast forecast) {
            String date = dateFormat.format(forecast.getDate());
            date = Character.toUpperCase(date.charAt(0)) + date.substring(1);
            dateTextView.setText(date);
            
            temperatureTextView.setText(String.format("%d°C - %d°C", 
                    forecast.getMinTemperature(), forecast.getMaxTemperature()));
            
            descriptionTextView.setText(forecast.getDescription());
            
            windTextView.setText(String.format(
                    itemView.getContext().getString(R.string.wind_speed),
                    forecast.getWindSpeed()));
            
            rainTextView.setText(forecast.isHasRain() ? 
                    "Возможны осадки" : "Без осадков");
            
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
    
    private static class DailyDiffCallback extends DiffUtil.ItemCallback<DailyForecast> {
        @Override
        public boolean areItemsTheSame(@NonNull DailyForecast oldItem, @NonNull DailyForecast newItem) {
            return oldItem.getDate().equals(newItem.getDate());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull DailyForecast oldItem, @NonNull DailyForecast newItem) {
            return oldItem.getMinTemperature() == newItem.getMinTemperature() &&
                   oldItem.getMaxTemperature() == newItem.getMaxTemperature() &&
                   oldItem.getIcon().equals(newItem.getIcon()) &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.getWindSpeed() == newItem.getWindSpeed() &&
                   oldItem.isHasRain() == newItem.isHasRain();
        }
    }
} 