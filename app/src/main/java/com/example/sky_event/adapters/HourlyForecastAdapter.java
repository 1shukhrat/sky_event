package com.example.sky_event.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sky_event.R;
import com.example.sky_event.models.weather.ForecastResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.HourlyViewHolder> {

    private List<ForecastResponse.ForecastItem> hourlyForecast = new ArrayList<>();
    private static final SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public void setHourlyForecast(List<ForecastResponse.ForecastItem> hourlyForecast) {
        this.hourlyForecast = hourlyForecast;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HourlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hourly_forecast, parent, false);
        return new HourlyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyViewHolder holder, int position) {
        ForecastResponse.ForecastItem forecastItem = hourlyForecast.get(position);
        holder.bind(forecastItem);
    }

    @Override
    public int getItemCount() {
        return hourlyForecast.size();
    }

    static class HourlyViewHolder extends RecyclerView.ViewHolder {
        private final TextView hourText;
        private final ImageView weatherIcon;
        private final TextView temperatureText;

        public HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            hourText = itemView.findViewById(R.id.timeTextView);
            weatherIcon = itemView.findViewById(R.id.weatherIconImageView);
            temperatureText = itemView.findViewById(R.id.temperatureTextView);
        }

        public void bind(ForecastResponse.ForecastItem forecastItem) {
            hourText.setText(formatHour(forecastItem.getDt()));
            
            String temp = Math.round(forecastItem.getMain().getTemp()) + "°C";
            temperatureText.setText(temp);

            if (!forecastItem.getWeather().isEmpty()) {
                String iconCode = forecastItem.getWeather().get(0).getIcon();
                String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
                
                Glide.with(weatherIcon.getContext())
                        .load(iconUrl)
                        .into(weatherIcon);
            }
        }

        private String formatHour(long timestamp) {
            return hourFormatter.format(new Date(timestamp * 1000L));
        }
    }
} 