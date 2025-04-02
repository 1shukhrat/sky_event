package com.example.sky_event.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sky_event.databinding.ItemDateOptionBinding;
import com.example.sky_event.models.DateSuggestion;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateSuggestionAdapter extends RecyclerView.Adapter<DateSuggestionAdapter.DateSuggestionViewHolder> {
    
    private final List<DateSuggestion> dateSuggestions = new ArrayList<>();
    private final DateSelectionListener listener;
    private final SimpleDateFormat dayFormat;
    private final SimpleDateFormat timeFormat;
    
    public interface DateSelectionListener {
        void onDateSelected(DateSuggestion suggestion);
    }
    
    public DateSuggestionAdapter(DateSelectionListener listener) {
        this.listener = listener;
        this.dayFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    public void setDateSuggestions(List<DateSuggestion> suggestions) {
        dateSuggestions.clear();
        dateSuggestions.addAll(suggestions);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public DateSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDateOptionBinding binding = ItemDateOptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DateSuggestionViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DateSuggestionViewHolder holder, int position) {
        DateSuggestion suggestion = dateSuggestions.get(position);
        holder.bind(suggestion);
    }
    
    @Override
    public int getItemCount() {
        return dateSuggestions.size();
    }
    
    public class DateSuggestionViewHolder extends RecyclerView.ViewHolder {
        private final ItemDateOptionBinding binding;
        
        public DateSuggestionViewHolder(@NonNull ItemDateOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        public void bind(DateSuggestion suggestion) {
            Date date = suggestion.getDate();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            
            String formattedDay = formatDay(date);
            binding.textViewDay.setText(formattedDay);
            
            binding.textViewTime.setText(timeFormat.format(date));
            binding.textViewTemp.setText(String.format("%.1f°C", suggestion.getTemperature()));
            binding.textViewWind.setText(String.format("%.1f м/с", suggestion.getWindSpeed()));
            
            String weatherDescription = suggestion.getWeatherDescription();
            if (weatherDescription == null || weatherDescription.isEmpty()) {
                weatherDescription = suggestion.isRainy() ? "Дождь" : "Ясно";
            }
            binding.textViewWeather.setText(translateWeatherDescription(weatherDescription));
            
            int qualityScore = (int) Math.round(suggestion.getScore());
            int weatherScore = (int) Math.round(suggestion.getWeatherScore());
            int timeScore = (int) Math.round(suggestion.getTimeScore());
            
            binding.textViewQuality.setText(String.format("Оценка: %d%% (погода: %d%%, время: %d%%)", 
                    qualityScore, weatherScore, timeScore));
            binding.progressIndicatorQuality.setProgress(qualityScore);
            
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append(String.format("Температура: %.1f°C (ощущается как %.1f°C)\n", 
                    suggestion.getTemperature(), suggestion.getFeelsLike()));
            detailsBuilder.append(String.format("Ветер: %.1f м/с\n", suggestion.getWindSpeed()));
            detailsBuilder.append(String.format("Влажность: %d%%", suggestion.getHumidity()));
            
            binding.textViewDetails.setText(detailsBuilder.toString());
            
            binding.buttonSelect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDateSelected(suggestion);
                }
            });
            
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDateSelected(suggestion);
                }
            });
        }
        
        private String formatDay(Date date) {
            Calendar today = Calendar.getInstance();
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTime(date);
            
            if (isSameDay(today, dateCalendar)) {
                return "Сегодня";
            } else if (isNextDay(today, dateCalendar)) {
                return "Завтра";
            } else {
                return dayFormat.format(date);
            }
        }
        
        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
        
        private boolean isNextDay(Calendar cal1, Calendar cal2) {
            Calendar tomorrow = (Calendar) cal1.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            return tomorrow.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   tomorrow.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
        
        private String translateWeatherDescription(String description) {
            String lowerDesc = description.toLowerCase();
            
            if (lowerDesc.contains("clear")) {
                return "Ясно";
            } else if (lowerDesc.contains("cloud")) {
                return "Облачно";
            } else if (lowerDesc.contains("rain") || lowerDesc.contains("drizzle")) {
                return "Дождь";
            } else if (lowerDesc.contains("snow")) {
                return "Снег";
            } else if (lowerDesc.contains("thunder") || lowerDesc.contains("storm")) {
                return "Гроза";
            } else if (lowerDesc.contains("mist") || lowerDesc.contains("fog")) {
                return "Туман";
            } else {
                return description;
            }
        }
    }
} 