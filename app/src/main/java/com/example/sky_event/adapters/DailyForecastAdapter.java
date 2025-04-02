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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.DailyViewHolder> {

    private List<DailyForecast> dailyForecasts = new ArrayList<>();
    private static final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE", Locale.getDefault());

    public void updateDailyForecasts(List<ForecastResponse.ForecastItem> forecastItems) {
        this.dailyForecasts = processForecastItems(forecastItems);
        notifyDataSetChanged();
    }

    private List<DailyForecast> processForecastItems(List<ForecastResponse.ForecastItem> forecastItems) {
        Map<String, DailyForecast> dailyMap = new HashMap<>();
        
        for (ForecastResponse.ForecastItem item : forecastItems) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(item.getDt() * 1000L);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            String dateKey = calendar.getTimeInMillis() + "";

            if (!dailyMap.containsKey(dateKey)) {
                DailyForecast dailyForecast = new DailyForecast();
                dailyForecast.setDate(calendar.getTime());
                dailyForecast.setMinTemp(item.getMain().getTemp_min());
                dailyForecast.setMaxTemp(item.getMain().getTemp_max());
                if (!item.getWeather().isEmpty()) {
                    dailyForecast.setDescription(item.getWeather().get(0).getDescription());
                    dailyForecast.setIconCode(item.getWeather().get(0).getIcon());
                }
                dailyMap.put(dateKey, dailyForecast);
            } else {
                DailyForecast dailyForecast = dailyMap.get(dateKey);
                if (item.getMain().getTemp_min() < dailyForecast.getMinTemp()) {
                    dailyForecast.setMinTemp(item.getMain().getTemp_min());
                }
                if (item.getMain().getTemp_max() > dailyForecast.getMaxTemp()) {
                    dailyForecast.setMaxTemp(item.getMain().getTemp_max());
                }
                dailyMap.put(dateKey, dailyForecast);
            }
        }
        
        List<DailyForecast> result = new ArrayList<>(dailyMap.values());
        result.sort((f1, f2) -> Long.compare(f1.getDate().getTime(), f2.getDate().getTime()));
        return result;
    }

    @NonNull
    @Override
    public DailyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_forecast, parent, false);
        return new DailyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyViewHolder holder, int position) {
        DailyForecast dailyForecast = dailyForecasts.get(position);
        holder.bind(dailyForecast);
    }

    @Override
    public int getItemCount() {
        return dailyForecasts.size();
    }

    static class DailyViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayText;
        private final TextView tempRangeText;
        private final TextView descriptionText;
        private final ImageView weatherIcon;

        public DailyViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dateTextView);
            tempRangeText = itemView.findViewById(R.id.temperatureTextView);
            descriptionText = itemView.findViewById(R.id.descriptionTextView);
            weatherIcon = itemView.findViewById(R.id.weatherIconImageView);
        }

        public void bind(DailyForecast dailyForecast) {
            dayText.setText(formatDay(dailyForecast.getDate()));
            String tempRange = Math.round(dailyForecast.getMinTemp()) + "°C - " 
                    + Math.round(dailyForecast.getMaxTemp()) + "°C";
            tempRangeText.setText(tempRange);
            descriptionText.setText(dailyForecast.getDescription());

            if (dailyForecast.getIconCode() != null) {
                String iconUrl = "https://openweathermap.org/img/wn/" + dailyForecast.getIconCode() + "@2x.png";
                Glide.with(weatherIcon.getContext())
                        .load(iconUrl)
                        .into(weatherIcon);
            }
        }

        private String formatDay(Date date) {
            String dayName = dayFormatter.format(date);
            return Character.toUpperCase(dayName.charAt(0)) + dayName.substring(1);
        }
    }

    public static class DailyForecast {
        private Date date;
        private double minTemp;
        private double maxTemp;
        private String description;
        private String iconCode;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public double getMinTemp() {
            return minTemp;
        }

        public void setMinTemp(double minTemp) {
            this.minTemp = minTemp;
        }

        public double getMaxTemp() {
            return maxTemp;
        }

        public void setMaxTemp(double maxTemp) {
            this.maxTemp = maxTemp;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIconCode() {
            return iconCode;
        }

        public void setIconCode(String iconCode) {
            this.iconCode = iconCode;
        }
    }
} 