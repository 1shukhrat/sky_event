package com.example.sky_event.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.databinding.FragmentEventDetailsBinding;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.event.WeatherCondition;

import com.example.sky_event.viewmodels.EventViewModel;
import com.example.sky_event.utils.LiveDataBus;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class EventDetailsFragment extends Fragment {
    private FragmentEventDetailsBinding binding;
    private EventViewModel eventViewModel;
    private String eventId;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());

    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        getEventIdFromArguments();
        setupButtons();
        observeViewModel();
        
        if (eventId != null) {
            eventViewModel.fetchEvent(eventId);
        }
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                updateUI(event);
                if (event.getLatitude() != 0 && event.getLongitude() != 0) {
                    fetchWeatherData(event.getLatitude(), event.getLongitude());
                }
            }
        });
        LiveDataBus.getInstance().with("weather_condition_changed").observe(getViewLifecycleOwner(), data -> {
            if (data != null && data instanceof Map) {
                Map<String, Object> weatherData = (Map<String, Object>) data;
                String eventId = (String) weatherData.get("eventId");
                
                if (eventId != null && eventViewModel.getEvent().getValue() != null && eventId.equals(eventViewModel.getEvent().getValue().getId())) {
                    double temperature = (double) weatherData.get("temperature");
                    double windSpeed = (double) weatherData.get("windSpeed");
                    String condition = (String) weatherData.get("condition");
                    boolean hasRain = (boolean) weatherData.get("hasRain");
                    int humidity = ((Number) weatherData.get("humidity")).intValue();
                    boolean isSuitable = (boolean) weatherData.get("isSuitable");
                    WeatherCondition weatherCondition = eventViewModel.getEvent().getValue().getWeatherCondition();
                    weatherCondition.setCurrentTemperature(temperature);
                    weatherCondition.setCurrentWindSpeed(windSpeed);
                    weatherCondition.setCurrentHumidity(humidity);
                    updateWeatherConditionStatus(isSuitable);
                    Event currentEvent = eventViewModel.getEvent().getValue();
                    if (currentEvent != null) {
                        fetchWeatherData(currentEvent.getLatitude(), currentEvent.getLongitude());
                    }
                }
            }
        });
        
        // Очищаем LiveData при закрытии фрагмента
        getViewLifecycleOwner().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    LiveDataBus.getInstance().with("weather_condition_changed").setValue(null);
                }
            }
        });
    }

    private void getEventIdFromArguments() {
        if (getArguments() != null && getArguments().containsKey("eventId")) {
            eventId = getArguments().getString("eventId");
        }
    }
    


    private void setupButtons() {
        binding.fabEdit.setOnClickListener(v -> navigateToEdit());
        binding.fabParticipate.setOnClickListener(v -> participateInEvent());
        binding.buttonOpenMap.setOnClickListener(v -> openMap());
        binding.cardViewParticipants.setOnClickListener(v -> openParticipantsList());
        binding.fabShare.setOnClickListener(v -> shareEvent());
        binding.fabCancelParticipation.setOnClickListener(v -> cancelParticipation());
        binding.fabDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        binding.buttonCopyLink.setOnClickListener(v -> copyEventLink());
    }

    private void navigateToEdit() {
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        Navigation.findNavController(requireView()).navigate(
                R.id.action_eventDetailsFragment_to_createEventFragment, args);
    }
    
    private void participateInEvent() {
        Event currentEvent = eventViewModel.getEvent().getValue();
        if (currentEvent != null) {
            if (eventViewModel.hasUserJoinedEvent(currentEvent.getId())) {
                Toast.makeText(requireContext(), "Вы уже присоединились к этому событию", Toast.LENGTH_SHORT).show();
                return;
            }
            
            eventViewModel.joinEvent(currentEvent.getId());
            Toast.makeText(requireContext(), "Вы подтвердили участие в событии", Toast.LENGTH_SHORT).show();
            updateParticipationButtonsVisibility(true);
        }
    }

    private void cancelParticipation() {
        Event currentEvent = eventViewModel.getEvent().getValue();
        if (currentEvent != null) {
            eventViewModel.leaveEvent(currentEvent.getId());
            Toast.makeText(requireContext(), "Вы отменили участие в событии", Toast.LENGTH_SHORT).show();
            updateParticipationButtonsVisibility(false);
        }
    }
    
    private void shareEvent() {
        Event currentEvent = eventViewModel.getEvent().getValue();
        if (currentEvent != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentEvent.getName());
            
            String eventLink = generateEventLink(currentEvent.getId());
            
            String shareMessage = "Приглашаю на событие: " + currentEvent.getName() + "\n" +
                    "Дата: " + dateFormat.format(currentEvent.getDate()) + "\n" +
                    "Место: " + currentEvent.getLocation() + "\n\n" +
                    currentEvent.getDescription() + "\n\n" +
                    "Присоединяйтесь: " + eventLink;
            
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Поделиться событием"));
        }
    }
    
    private String generateEventLink(String eventId) {
        return "https://skyevent.app/event/" + eventId;
    }
    
    private void openMap() {
        Event currentEvent = eventViewModel.getEvent().getValue();
        if (currentEvent != null) {
            if (currentEvent.getLatitude() != 0 && currentEvent.getLongitude() != 0) {
                Bundle args = new Bundle();
                args.putFloat("latitude", (float) currentEvent.getLatitude());
                args.putFloat("longitude", (float) currentEvent.getLongitude());
                Navigation.findNavController(requireView()).navigate(
                        R.id.action_eventDetailsFragment_to_mapFragment, args);
            } else {
                Toast.makeText(requireContext(), "Координаты не указаны", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openParticipantsList() {
        if (eventId != null) {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_eventDetailsFragment_to_eventParticipantsFragment, args);
        }
    }

    private void observeViewModel() {
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                if (event.getId() != null && !event.getId().isEmpty()) {
                    if (event.getName() == null && eventId.equals(event.getId())) {
                        Toast.makeText(requireContext(), "Событие было удалено", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    } else {
                        updateUI(event);
                    }
                }
            }
        });
        
        eventViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
        
        eventViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    private void updateUI(Event event) {
        binding.textViewEventName.setText(event.getName());
        binding.textViewEventDescription.setText(event.getDescription());
        
        binding.chipCategory.setText(event.getCategory());
        
        if (event.getDate() != null) {
            binding.textViewEventDate.setText(dateFormat.format(event.getDate()));
        } else {
            binding.textViewEventDate.setText("Дата будет выбрана автоматически");
        }
        
        binding.textViewEventLocation.setText(event.getLocation());
        
        updateParticipantsInfo(event);
        updateWeatherInfo(event);
        updateParticipationButtonsVisibility(eventViewModel.hasUserJoinedEvent(event.getId()));
        
        String eventLink = generateEventLink(event.getId());
        binding.textViewEventLink.setText(eventLink);
    }
    
    private void updateParticipantsInfo(Event event) {
        String participantsText = event.getParticipantsCount() + " человек";
        if (event.getParticipantsCount() % 10 == 1 && event.getParticipantsCount() % 100 != 11) {
            participantsText += " подтвердил участие";
        } else {
            participantsText += " подтвердили участие";
        }
        binding.textViewParticipantsCount.setText(participantsText);
    }
    
    private void updateWeatherInfo(Event event) {
        WeatherCondition weatherCondition = event.getWeatherCondition();
        if (weatherCondition != null) {
            String tempText = String.format(Locale.getDefault(), "от %.0f°C до %.0f°C", 
                    weatherCondition.getMinTemperature(), weatherCondition.getMaxTemperature());
            binding.textViewWeatherTemp.setText(tempText);
            
            binding.imageViewWeatherIcon.setImageResource(getWeatherIconResource(weatherCondition));
            
            String condition = weatherCondition.isNoRain() ? "Без осадков" : "Возможны осадки";
            binding.textViewWeatherCondition.setText(condition);
            
            String windSpeed = String.format(Locale.getDefault(), 
                    "Максимальная скорость ветра: %.0f м/с", 
                    weatherCondition.getMaxWindSpeed());
            binding.textViewWeatherWind.setText(windSpeed);
            
            String rain = weatherCondition.isNoRain() ? 
                    "Требуется солнечная погода" : 
                    "Допустимы осадки";
            binding.textViewWeatherRain.setText(rain);
            
            binding.textViewWeatherHumidity.setText(
                    String.format(Locale.getDefault(), "Максимальная влажность: %d%%", weatherCondition.getMaxHumidity()));
        }
    }
    
    private int getWeatherIconResource(WeatherCondition condition) {
        if (condition.isNoRain()) {
            return R.drawable.ic_weather_sunny;
        } else {
            return R.drawable.ic_weather_partly_cloudy_day;
        }
    }
    
    private String getWindDirection() {
        String[] directions = {"северный", "северо-восточный", "восточный", "юго-восточный", 
                "южный", "юго-западный", "западный", "северо-западный"};
        return directions[(int)(Math.random() * directions.length)];
    }

    private void updateParticipationButtonsVisibility(boolean isParticipating) {
        if (isParticipating) {
            binding.fabParticipate.setVisibility(View.GONE);
            binding.fabCancelParticipation.setVisibility(View.VISIBLE);
        } else {
            binding.fabParticipate.setVisibility(View.VISIBLE);
            binding.fabCancelParticipation.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_details, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление события")
                .setMessage("Вы уверены, что хотите удалить это событие?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteEvent())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteEvent() {
        if (eventId != null) {
            eventViewModel.deleteEvent(eventId);
        }
    }

    private void copyEventLink() {
        Event currentEvent = eventViewModel.getEvent().getValue();
        if (currentEvent != null) {
            String eventLink = generateEventLink(currentEvent.getId());
            
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Ссылка на событие", eventLink);
            clipboard.setPrimaryClip(clip);
            
            Toast.makeText(requireContext(), "Ссылка скопирована", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Запрашиваем погодные данные для конкретной локации
    private void fetchWeatherData(double latitude, double longitude) {
        // Здесь можно было бы обратиться к API погоды или репозиторию
        // Но в данном случае это делается через сервис WeatherMonitoringService
    }

    // Обновляем статус соответствия погодных условий требованиям события
    private void updateWeatherConditionStatus(boolean isSuitable) {
        if (binding != null) {
            if (isSuitable) {
                binding.cardViewWeather.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.green_100));
                binding.textViewWeatherStatus.setText("Погодные условия подходят");
                binding.textViewWeatherStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.green_700));
            } else {
                binding.cardViewWeather.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.red_100));
                binding.textViewWeatherStatus.setText("Погодные условия не соответствуют требованиям");
                binding.textViewWeatherStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.red_700));
            }
        }
    }
} 