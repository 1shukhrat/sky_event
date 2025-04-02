package com.example.sky_event.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sky_event.R;
import com.example.sky_event.adapters.DateSuggestionAdapter;
import com.example.sky_event.databinding.FragmentDatePickerBinding;
import com.example.sky_event.models.DateSuggestion;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.models.weather.ForecastResponse;
import com.example.sky_event.viewmodels.EventViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatePickerFragment extends Fragment implements DateSuggestionAdapter.DateSelectionListener {
    private FragmentDatePickerBinding binding;
    private EventViewModel eventViewModel;
    private DateSuggestionAdapter adapter;
    private Event event;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDatePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        setupToolbar();
        setupRecyclerView();
        
        getEventFromArguments();
        observeViewModel();
        
        if (event != null) {
            binding.textViewEventName.setText(event.getName());
            binding.chipCategory.setText(event.getCategory());
            
            String location = event.getLocation();
            double lat = event.getLatitude();
            double lng = event.getLongitude();
            
            binding.textViewLocation.setText(location);
            
            if (lat != 0 && lng != 0) {
                loadWeatherForLocation();
            } else {
                binding.textViewError.setText("Для поиска оптимальной даты необходимо указать местоположение");
                binding.textViewError.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }
        }
    }
    
    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
    }
    
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(requireView()).popBackStack());
    }
    
    private void setupRecyclerView() {
        adapter = new DateSuggestionAdapter(this);
        binding.recyclerViewDateSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewDateSuggestions.setAdapter(adapter);
    }
    
    private void getEventFromArguments() {
        if (getArguments() != null) {
            event = getArguments().getParcelable("event");
            
            if (event != null && event.getWeatherCondition() == null) {
                event.setWeatherCondition(new WeatherCondition());
            }
        }
        
        if (event == null) {
            event = new Event();
            event.setWeatherCondition(new WeatherCondition());
        }
    }
    
    private void observeViewModel() {
        eventViewModel.getForecast().observe(getViewLifecycleOwner(), this::processWeatherForecast);
        
        eventViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.textViewError.setText(error);
                binding.textViewError.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        
        eventViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.recyclerViewDateSuggestions.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void loadWeatherForLocation() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewError.setVisibility(View.GONE);
        binding.recyclerViewDateSuggestions.setVisibility(View.GONE);
        
        eventViewModel.loadEventWeather(event.getLatitude(), event.getLongitude());
    }
    
    private void processWeatherForecast(ForecastResponse forecastResponse) {
        if (forecastResponse == null || forecastResponse.getList() == null || forecastResponse.getList().isEmpty()) {
            binding.textViewError.setText("Не удалось получить прогноз погоды");
            binding.textViewError.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
            return;
        }
        
        if (event == null) {
            event = new Event();
        }
        
        WeatherCondition conditions = event.getWeatherCondition();
        if (conditions == null) {
            conditions = new WeatherCondition();
            event.setWeatherCondition(conditions);
        }
        
        List<DateSuggestion> suggestions = findOptimalDates(forecastResponse, conditions);
        updateSuggestionsList(suggestions);
    }
    
    private List<DateSuggestion> findOptimalDates(ForecastResponse forecast, WeatherCondition conditions) {
        List<DateSuggestion> suggestions = new ArrayList<>();
        
        if (conditions == null) {
            conditions = new WeatherCondition();
        }
        
        double minTemp = conditions.getMinTemperature();
        double maxTemp = conditions.getMaxTemperature();
        double maxWind = conditions.getMaxWindSpeed();
        boolean noRain = conditions.isNoRain();
        
        for (ForecastResponse.ForecastItem item : forecast.getList()) {
            try {
                Date date = new Date(item.getDt() * 1000);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                
                if (hour >= 9 && hour <= 21) {
                    double temp = item.getMain().getTemp();
                    double feelsLike = item.getMain().getFeels_like();
                    double wind = item.getWind().getSpeed();
                    int humidity = item.getMain().getHumidity();
                    
                    boolean isRainy = false;
                    String mainWeather = "";
                    String description = "";
                    if (item.getWeather() != null && !item.getWeather().isEmpty()) {
                        mainWeather = item.getWeather().get(0).getMain();
                        description = item.getWeather().get(0).getDescription();
                        isRainy = mainWeather.toLowerCase().contains("rain") || 
                                  mainWeather.toLowerCase().contains("drizzle");
                    }
                    
                    boolean tempOk = temp >= minTemp && temp <= maxTemp;
                    boolean windOk = maxWind <= 0 || wind <= maxWind;
                    boolean rainOk = !noRain || !isRainy;
                    boolean humidityOk = humidity <= conditions.getMaxHumidity();
                    
                    if (tempOk && windOk && rainOk && humidityOk) {
                        DateSuggestion suggestion = new DateSuggestion();
                        suggestion.setDate(date);
                        suggestion.setTemperature(temp);
                        suggestion.setFeelsLike(feelsLike);
                        suggestion.setWindSpeed(wind);
                        suggestion.setHumidity(humidity);
                        suggestion.setWeatherDescription(description.isEmpty() ? mainWeather : description);
                        suggestion.setRainy(isRainy);
                        
                        double weatherScore = conditions.calculateComfortScore(temp, wind, isRainy, humidity);
                        double timeScore = calculateTimeScore(hour, calendar);
                        
                        suggestion.setWeatherScore(weatherScore);
                        suggestion.setTimeScore(timeScore);
                        
                        double finalScore = (weatherScore * 0.7) + (timeScore * 0.3);
                        suggestion.setScore(finalScore);
                        
                        suggestions.add(suggestion);
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        suggestions.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return suggestions.size() > 40 ? suggestions.subList(0, 40) : suggestions;
    }
    
    private double calculateTimeScore(int hour, Calendar calendar) {
        double score = 50;
        
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
        
        if (isWeekend) {
            score += 20;
        }
        
        if (hour >= 10 && hour <= 16) {
            score += 30;
        } else if (hour >= 17 && hour <= 19) {
            score += 15;
        }
        
        int daysFromNow = (int)((calendar.getTimeInMillis() - System.currentTimeMillis()) / (24 * 60 * 60 * 1000));
        
        if (daysFromNow == 0) {
            score += 20;
        } else if (daysFromNow == 1) {
            score += 15;
        } else if (daysFromNow == 2) {
            score += 10;
        } else if (daysFromNow <= 5) {
            score += 5;
        }
        
        return Math.min(100, score);
    }
    
    private void updateSuggestionsList(List<DateSuggestion> suggestions) {
        if (suggestions.isEmpty()) {
            binding.textViewError.setText("Не найдено подходящих дат для заданных погодных условий");
            binding.textViewError.setVisibility(View.VISIBLE);
        } else {
            binding.textViewError.setVisibility(View.GONE);
            adapter.setDateSuggestions(suggestions);
        }
        
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewDateSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
    }
    
    @Override
    public void onDateSelected(DateSuggestion suggestion) {
        if (event != null) {
            event.setDate(suggestion.getDate());
            
            Bundle result = new Bundle();
            result.putLong("timestamp", suggestion.getDate().getTime());
            result.putParcelable("event", event);
            getParentFragmentManager().setFragmentResult("dateSelection", result);
        } else {
            Bundle result = new Bundle();
            result.putLong("timestamp", suggestion.getDate().getTime());
            getParentFragmentManager().setFragmentResult("dateSelection", result);
        }
        
        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 