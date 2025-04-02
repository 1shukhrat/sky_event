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
import com.example.sky_event.databinding.ItemMonthlyForecastBinding;
import com.example.sky_event.models.weather.MonthlyForecast;
import com.example.sky_event.utils.WeatherUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MonthlyForecastAdapter extends ListAdapter<MonthlyForecast, MonthlyForecastAdapter.MonthlyViewHolder> {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM", new Locale("ru"));
    
    public MonthlyForecastAdapter() {
        super(new MonthlyForecastDiffCallback());
    }

    @NonNull
    @Override
    public MonthlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMonthlyForecastBinding binding = ItemMonthlyForecastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MonthlyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthlyViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class MonthlyViewHolder extends RecyclerView.ViewHolder {
        private final ItemMonthlyForecastBinding binding;

        MonthlyViewHolder(ItemMonthlyForecastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MonthlyForecast forecast) {
            binding.dateText.setText(dateFormat.format(forecast.getDate()));
            binding.tempText.setText(String.format(Locale.getDefault(), 
                    "%d°/%d°", Math.round(forecast.getTempMin()), Math.round(forecast.getTempMax())));
            binding.weatherIcon.setImageResource(WeatherUtils.getWeatherIconResource(forecast.getIconId()));
            binding.weatherConditionText.setText(forecast.getDescription());
            binding.windText.setText(String.format(Locale.getDefault(), 
                    "%.1f м/с", forecast.getWindSpeed()));
            binding.humidityText.setText(String.format(Locale.getDefault(), 
                    "%d%%", forecast.getHumidity()));
            
            if (forecast.getRainAmount() > 0) {
                binding.rainText.setVisibility(View.VISIBLE);
                binding.rainText.setText(String.format(Locale.getDefault(), 
                        "%.1f мм", forecast.getRainAmount()));
            } else {
                binding.rainText.setVisibility(View.GONE);
            }
        }
    }

    private static class MonthlyForecastDiffCallback extends DiffUtil.ItemCallback<MonthlyForecast> {
        @Override
        public boolean areItemsTheSame(@NonNull MonthlyForecast oldItem, @NonNull MonthlyForecast newItem) {
            return oldItem.getDate().equals(newItem.getDate());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MonthlyForecast oldItem, @NonNull MonthlyForecast newItem) {
            return oldItem.getTempMin() == newItem.getTempMin() &&
                   oldItem.getTempMax() == newItem.getTempMax() &&
                   oldItem.getWindSpeed() == newItem.getWindSpeed() &&
                   oldItem.getHumidity() == newItem.getHumidity() &&
                   oldItem.getRainAmount() == newItem.getRainAmount() &&
                   oldItem.getDescription().equals(newItem.getDescription());
        }
    }
} 