package com.example.sky_event.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.databinding.FragmentCreateEventBinding;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.viewmodels.EventViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateEventFragment extends Fragment {
    private FragmentCreateEventBinding binding;
    private EventViewModel eventViewModel;
    private Event event;
    private final Calendar calendar = Calendar.getInstance();
    private double latitude;
    private double longitude;
    private boolean isEditMode = false;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());
    private static final String PREF_NAME = "create_event_data";
    private static final String KEY_EVENT_DATA = "event_data";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.progressBar.setVisibility(View.GONE);
        
        setupViewModel();
        setupCategoryDropdown();
        
        String eventId = getEventIdFromArguments();
        if (eventId != null) {
            isEditMode = true;
            binding.textViewCreateTitle.setText("Редактирование мероприятия");
            eventViewModel.fetchEvent(eventId);
        } else {
            isEditMode = false;
            binding.textViewCreateTitle.setText("Новое мероприятие");
            event = new Event();
            event.setId(UUID.randomUUID().toString());
            restoreEventData();
            updateUI();
        }
        
        setupListeners();
        observeViewModel();
        setupFragmentResultListener();
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
    }

    private String getEventIdFromArguments() {
        if (getArguments() != null && getArguments().containsKey("eventId")) {
            return getArguments().getString("eventId");
        }
        return null;
    }

    private void setupListeners() {
        binding.textViewDate.setOnClickListener(v -> showDateTimePicker());
        binding.buttonAutoDate.setOnClickListener(v -> findOptimalDate());
        binding.buttonSelectLocation.setOnClickListener(v -> navigateToMap());
        binding.buttonSave.setOnClickListener(v -> saveEvent());
        binding.buttonCancel.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    private void observeViewModel() {
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (isEditMode) {
                    if (result.getId() != null && !result.getId().isEmpty()) {
                        event = result;
                        updateUI();
                    }
                } else {
                    if (result.getId() == null) {
                        event = result;
                    }
                }
            }
        });
        
        eventViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
        
        eventViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.buttonSave.setEnabled(!isLoading);
            binding.buttonCancel.setEnabled(!isLoading);
        });
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Спорт", "Развлечения", "Образование", "Культура", "Бизнес", "Технологии", "Другое"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown,
                categories);
        binding.dropdownCategory.setAdapter(adapter);
        binding.dropdownCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = adapter.getItem(position);
            if (event != null) {
                event.setCategory(selectedCategory);
            }
        });
        
        if (event != null && event.getCategory() != null) {
            binding.dropdownCategory.setText(event.getCategory(), false);
        }
    }

    private void updateUI() {
        if (event != null) {
            binding.editTextName.setText(event.getName());
            binding.editTextDescription.setText(event.getDescription());
            
            binding.dropdownCategory.setText(event.getCategory(), false);
            
            if (event.getDate() != null) {
                calendar.setTime(event.getDate());
                updateDateText();
            }
            
            binding.textViewLocation.setText(event.getLocation());
            
            latitude = event.getLatitude();
            longitude = event.getLongitude();
            
            WeatherCondition weatherCondition = event.getWeatherCondition();
            if (weatherCondition != null) {
                binding.editTextMinTemp.setText(String.valueOf(weatherCondition.getMinTemperature()));
                binding.editTextMaxTemp.setText(String.valueOf(weatherCondition.getMaxTemperature()));
                binding.editTextWindSpeed.setText(String.valueOf(weatherCondition.getMaxWindSpeed()));
                binding.switchNoRain.setChecked(weatherCondition.isNoRain());
            }
        }
    }

    private void showDateTimePicker() {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            showTimePicker();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            
            updateDateText();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void updateDateText() {
        if (event != null) {
            event.setDate(calendar.getTime());
        }
        binding.textViewDate.setText(dateFormat.format(calendar.getTime()));
    }

    private void findOptimalDate() {
        if (binding == null) return;
        
        if (event == null) {
            event = new Event();
        }
        
        String name = binding.editTextName.getText().toString().trim();
        if (!name.isEmpty()) {
            event.setName(name);
        } else {
            event.setName("Новое событие");
        }
        
        String category = binding.dropdownCategory.getText().toString().trim();
        if (!category.isEmpty()) {
            event.setCategory(category);
        } else {
            event.setCategory("Другое");
        }
        
        String location = binding.textViewLocation.getText().toString().trim();
        if (!location.isEmpty()) {
            event.setLocation(location);
        }
        
        if (event.getLatitude() == 0 || event.getLongitude() == 0) {
            Toast.makeText(getContext(), "Необходимо выбрать местоположение для поиска оптимальной даты", Toast.LENGTH_SHORT).show();
            return;
        }
        
        WeatherCondition weatherCondition = event.getWeatherCondition();
        if (weatherCondition == null) {
            weatherCondition = new WeatherCondition();
            event.setWeatherCondition(weatherCondition);
        }
        
        updateWeatherConditionsFromUI();
        
        android.util.Log.d("CreateEventFragment", "WeatherCondition before navigation: " + 
                "minTemp=" + event.getWeatherCondition().getMinTemperature() + 
                ", maxTemp=" + event.getWeatherCondition().getMaxTemperature() + 
                ", maxWind=" + event.getWeatherCondition().getMaxWindSpeed() + 
                ", noRain=" + event.getWeatherCondition().isNoRain());
        
        android.util.Log.d("CreateEventFragment", "Event location before navigation: " + 
                event.getLocation() + " [" + event.getLatitude() + ", " + event.getLongitude() + "]");
        
        Bundle args = new Bundle();
        args.putParcelable("event", event);
        Navigation.findNavController(requireView()).navigate(
                R.id.action_createEventFragment_to_datePickerFragment, args);
    }

    private void updateWeatherConditionsFromUI() {
        try {
            WeatherCondition conditions = event.getWeatherCondition();
            
            String minTempStr = binding.editTextMinTemp.getText().toString().trim();
            if (!minTempStr.isEmpty()) {
                conditions.setMinTemperature(Double.parseDouble(minTempStr));
            }
            
            String maxTempStr = binding.editTextMaxTemp.getText().toString().trim();
            if (!maxTempStr.isEmpty()) {
                conditions.setMaxTemperature(Double.parseDouble(maxTempStr));
            }
            
            String windSpeedStr = binding.editTextWindSpeed.getText().toString().trim();
            if (!windSpeedStr.isEmpty()) {
                conditions.setMaxWindSpeed(Double.parseDouble(windSpeedStr));
            } else {
                conditions.setMaxWindSpeed(0);
            }
            
            conditions.setNoRain(binding.switchNoRain.isChecked());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Некорректные значения погодных условий", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener("locationSelection", this, (requestKey, result) -> {
            if (result.containsKey("latitude") && result.containsKey("longitude")) {
                latitude = result.getDouble("latitude");
                longitude = result.getDouble("longitude");
                String address = result.getString("address", "");
                
                if (event != null) {
                    event.setLatitude(latitude);
                    event.setLongitude(longitude);
                    event.setLocation(address);
                }
                
                binding.textViewLocation.setText(address);
            }
        });
        
        getParentFragmentManager().setFragmentResultListener("dateSelection", this, (requestKey, result) -> {
            if (result.containsKey("timestamp")) {
                long timestamp = result.getLong("timestamp");
                
                if (result.containsKey("event")) {
                    Event returnedEvent = result.getParcelable("event");
                    
                    if (returnedEvent != null) {
                        android.util.Log.d("CreateEventFragment", "Received full event from DatePickerFragment");
                        
                        String currentId = (event != null) ? event.getId() : null;
                        String currentUserId = (event != null) ? event.getUserId() : null;
                        
                        event = returnedEvent;
                        
                        if (currentId != null) {
                            event.setId(currentId);
                        }
                        
                        if (currentUserId != null) {
                            event.setUserId(currentUserId);
                        }
                        
                        updateUI();
                        
                        android.util.Log.d("CreateEventFragment", "Event location after receiving: " + 
                                event.getLocation() + " [" + event.getLatitude() + ", " + event.getLongitude() + "]");
                    }
                } else {
                    Date selectedDate = new Date(timestamp);
                    
                    if (event != null) {
                        event.setDate(selectedDate);
                        calendar.setTime(selectedDate);
                        updateDateText();
                    }
                }
                
                Toast.makeText(requireContext(), "Дата успешно выбрана", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMap() {
        Bundle args = new Bundle();
        args.putBoolean("selectLocation", true);
        if (latitude != 0 && longitude != 0) {
            args.putFloat("latitude", (float)latitude);
            args.putFloat("longitude", (float)longitude);
        }
        Navigation.findNavController(requireView()).navigate(R.id.action_createEventFragment_to_mapFragment, args);
    }

    private void saveEvent() {
        if (validateInputs()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            updateEventFromInputs();
            
            if (isEditMode) {
                eventViewModel.updateEvent(event);
            } else {
                eventViewModel.saveEvent(event);
            }
            
            clearSavedData();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    private boolean validateInputs() {
        String name = binding.editTextName.getText().toString().trim();
        String location = binding.textViewLocation.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            binding.editTextName.setError("Введите название события");
            return false;
        }
        
        if (TextUtils.isEmpty(location)) {
            Toast.makeText(requireContext(), "Выберите местоположение", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            double minTemp = Double.parseDouble(binding.editTextMinTemp.getText().toString());
            double maxTemp = Double.parseDouble(binding.editTextMaxTemp.getText().toString());
            
            if (minTemp > maxTemp) {
                binding.editTextMinTemp.setError("Минимальная температура не может быть больше максимальной");
                return false;
            }
            
            String windSpeedStr = binding.editTextWindSpeed.getText().toString().trim();
            if (!windSpeedStr.isEmpty()) {
                double windSpeed = Double.parseDouble(windSpeedStr);
                if (windSpeed < 0) {
                    binding.editTextWindSpeed.setError("Скорость ветра не может быть отрицательной");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Введите корректные значения для погодных условий", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    private void updateEventFromInputs() {
        String name = binding.editTextName.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String category = binding.dropdownCategory.getText().toString().trim();
        
        event.setName(name);
        event.setDescription(description);
        event.setCategory(category);
        
        if (event.getDate() == null) {
            event.setDate(calendar.getTime());
        }
        
        event.setLocation(binding.textViewLocation.getText().toString().trim());
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        
        updateWeatherConditionsFromUI();
        
        if (event.getUserId() == null || event.getUserId().isEmpty()) {
            event.setUserId(getCurrentUserId());
        }
        
        saveEventData();
    }

    private void saveEventData() {
        if (!isEditMode && event != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            Gson gson = new Gson();
            String eventJson = gson.toJson(event);
            
            editor.putString(KEY_EVENT_DATA, eventJson);
            editor.putFloat(KEY_LATITUDE, (float) latitude);
            editor.putFloat(KEY_LONGITUDE, (float) longitude);
            editor.apply();
        }
    }
    
    private void restoreEventData() {
        if (!isEditMode) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String eventJson = prefs.getString(KEY_EVENT_DATA, "");
            latitude = prefs.getFloat(KEY_LATITUDE, 0);
            longitude = prefs.getFloat(KEY_LONGITUDE, 0);
            
            if (!eventJson.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    Event savedEvent = gson.fromJson(eventJson, Event.class);
                    if (savedEvent != null) {
                        String originalId = event.getId();
                        
                        event.setName(savedEvent.getName());
                        event.setDescription(savedEvent.getDescription());
                        event.setCategory(savedEvent.getCategory());
                        event.setLocation(savedEvent.getLocation());
                        event.setLatitude(savedEvent.getLatitude());
                        event.setLongitude(savedEvent.getLongitude());
                        event.setWeatherCondition(savedEvent.getWeatherCondition());
                        
                        if (savedEvent.getDate() != null) {
                            event.setDate(savedEvent.getDate());
                            calendar.setTime(savedEvent.getDate());
                        }
                    }
                } catch (Exception e) {

                }
            }
        }
    }
    
    private void clearSavedData() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        if (isEditMode || (event != null && event.getId() != null && eventViewModel.getEvent().getValue() != null)) {
            clearSavedData();
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (!isEditMode && event != null) {
            updateEventFromInputs();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (!isEditMode && event != null) {
            updateEventFromInputs();
        }
    }
} 