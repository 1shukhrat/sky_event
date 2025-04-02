package com.example.sky_event.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.event.WeatherCondition;
import com.example.sky_event.utils.DateFormatter;
import com.example.sky_event.viewmodels.EventEditorViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EventEditorFragment extends Fragment {
    
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 1001;
    
    private EventEditorViewModel viewModel;
    private EditText nameEditText;
    private EditText descriptionEditText;
    private EditText locationEditText;
    private TextView dateTextView;
    private Button selectDateButton;
    private Button findOptimalDateButton;
    private SeekBar minTempSeekBar;
    private TextView minTempTextView;
    private SeekBar maxTempSeekBar;
    private TextView maxTempTextView;
    private SeekBar maxWindSeekBar;
    private TextView maxWindTextView;
    private CheckBox noRainCheckBox;
    private ChipGroup weatherConditionsChipGroup;
    private FloatingActionButton saveFab;
    private View progressBar;
    
    private String eventId;
    private Event currentEvent;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_editor, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
        setupViewModel();
        setupSeekBars();
        setupFragmentResultListener();
    }
    
    private void initViews(View view) {
        nameEditText = view.findViewById(R.id.event_name_edit_text);
        descriptionEditText = view.findViewById(R.id.event_description_edit_text);
        locationEditText = view.findViewById(R.id.event_location_edit_text);
        dateTextView = view.findViewById(R.id.event_date_text_view);
        selectDateButton = view.findViewById(R.id.select_date_button);
        findOptimalDateButton = view.findViewById(R.id.find_optimal_date_button);
        minTempSeekBar = view.findViewById(R.id.min_temp_seek_bar);
        minTempTextView = view.findViewById(R.id.min_temp_text_view);
        maxTempSeekBar = view.findViewById(R.id.max_temp_seek_bar);
        maxTempTextView = view.findViewById(R.id.max_temp_text_view);
        maxWindSeekBar = view.findViewById(R.id.max_wind_seek_bar);
        maxWindTextView = view.findViewById(R.id.max_wind_text_view);
        noRainCheckBox = view.findViewById(R.id.no_rain_checkbox);
        weatherConditionsChipGroup = view.findViewById(R.id.weather_conditions_chip_group);
        saveFab = view.findViewById(R.id.save_fab);
        progressBar = view.findViewById(R.id.progress_bar);
    }
    
    private void setupListeners() {
        selectDateButton.setOnClickListener(v -> showDatePicker());
        findOptimalDateButton.setOnClickListener(v -> findOptimalDate());
        saveFab.setOnClickListener(v -> saveEvent());
        locationEditText.setOnClickListener(v -> navigateToMap());
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(EventEditorViewModel.class);
        
        viewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                currentEvent = event;
                updateUI(event);
            }
        });
        
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Snackbar.make(requireView(), "Событие сохранено", Snackbar.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getSuggestedDates().observe(getViewLifecycleOwner(), dates -> {
            if (dates != null && !dates.isEmpty()) {
                showSuggestedDatesDialog(dates);
            }
        });
        
        viewModel.isLoadingDates().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            findOptimalDateButton.setEnabled(!isLoading);
        });
        
        if (eventId != null && !eventId.isEmpty()) {
            viewModel.loadEvent(eventId);
        } else {
            viewModel.createNewEvent();
        }
    }
    
    private void setupSeekBars() {
        minTempSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minTemp = progress - 20;
                minTempTextView.setText(minTemp + "°C");
                
                if (fromUser && maxTempSeekBar.getProgress() < progress) {
                    maxTempSeekBar.setProgress(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        maxTempSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int maxTemp = progress - 20;
                maxTempTextView.setText(maxTemp + "°C");
                
                if (fromUser && minTempSeekBar.getProgress() > progress) {
                    minTempSeekBar.setProgress(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        maxWindSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxWindTextView.setText(progress + " м/с");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void updateUI(Event event) {
        nameEditText.setText(event.getName());
        descriptionEditText.setText(event.getDescription());
        locationEditText.setText(event.getLocation());
        
        if (event.getDate() != null) {
            dateTextView.setText(DateFormatter.formatDateForDisplay(event.getDate()));
        }
        
        WeatherCondition weatherCondition = event.getWeatherCondition();
        if (weatherCondition != null) {
            int minTemp = (int) weatherCondition.getMinTemperature();
            int maxTemp = (int) weatherCondition.getMaxTemperature();
            int maxWind = (int) weatherCondition.getMaxWindSpeed();
            
            minTempSeekBar.setProgress(minTemp + 20);
            maxTempSeekBar.setProgress(maxTemp + 20);
            maxWindSeekBar.setProgress(maxWind);
            
            noRainCheckBox.setChecked(weatherCondition.isNoRain());
            
            updateWeatherConditionChips(weatherCondition.getAllowedConditions());
        }
    }
    
    private void updateWeatherConditionChips(List<String> allowedConditions) {
        weatherConditionsChipGroup.removeAllViews();
        
        String[] conditions = {"Clear", "Clouds", "Rain", "Snow", "Thunderstorm", "Drizzle", "Fog", "Mist"};
        
        for (String condition : conditions) {
            Chip chip = new Chip(requireContext());
            chip.setText(translateCondition(condition));
            chip.setCheckable(true);
            chip.setChecked(allowedConditions.contains(condition));
            weatherConditionsChipGroup.addView(chip);
        }
    }
    
    private String translateCondition(String condition) {
        switch (condition) {
            case "Clear": return "Ясно";
            case "Clouds": return "Облачно";
            case "Rain": return "Дождь";
            case "Snow": return "Снег";
            case "Thunderstorm": return "Гроза";
            case "Drizzle": return "Морось";
            case "Fog": return "Туман";
            case "Mist": return "Дымка";
            default: return condition;
        }
    }
    
    private String untranslateCondition(String translatedCondition) {
        switch (translatedCondition) {
            case "Ясно": return "Clear";
            case "Облачно": return "Clouds";
            case "Дождь": return "Rain";
            case "Снег": return "Snow";
            case "Гроза": return "Thunderstorm";
            case "Морось": return "Drizzle";
            case "Туман": return "Fog";
            case "Дымка": return "Mist";
            default: return translatedCondition;
        }
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (currentEvent.getDate() != null) {
            calendar.setTime(currentEvent.getDate());
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    currentEvent.setDate(calendar.getTime());
                    dateTextView.setText(DateFormatter.formatDateForDisplay(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    
    private void findOptimalDate() {
        updateEventFromUI();
        viewModel.findOptimalDates(currentEvent);
    }
    
    private void showSuggestedDatesDialog(List<Date> dates) {
        String[] dateStrings = new String[dates.size()];
        for (int i = 0; i < dates.size(); i++) {
            dateStrings[i] = DateFormatter.formatDateForDisplay(dates.get(i)) + " (" + 
                    DateFormatter.formatWeekday(dates.get(i)) + ")";
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Рекомендуемые даты")
                .setItems(dateStrings, (dialog, which) -> {
                    Date selectedDate = dates.get(which);
                    currentEvent.setDate(selectedDate);
                    dateTextView.setText(DateFormatter.formatDateForDisplay(selectedDate));
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    private void updateEventFromUI() {
        currentEvent.setName(nameEditText.getText().toString().trim());
        currentEvent.setDescription(descriptionEditText.getText().toString().trim());
        currentEvent.setLocation(locationEditText.getText().toString().trim());
        
        WeatherCondition weatherCondition = currentEvent.getWeatherCondition();
        weatherCondition.setMinTemperature(minTempSeekBar.getProgress() - 20);
        weatherCondition.setMaxTemperature(maxTempSeekBar.getProgress() - 20);
        weatherCondition.setMaxWindSpeed(maxWindSeekBar.getProgress());
        weatherCondition.setNoRain(noRainCheckBox.isChecked());
        
        List<String> allowedConditions = new ArrayList<>();
        for (int i = 0; i < weatherConditionsChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) weatherConditionsChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                allowedConditions.add(untranslateCondition(chip.getText().toString()));
            }
        }
        weatherCondition.setAllowedConditions(allowedConditions);
    }
    
    private void saveEvent() {
        updateEventFromUI();
        viewModel.saveEvent(currentEvent);
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_editor, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteEvent();
            return true;
        } else if (item.getItemId() == R.id.action_add_to_calendar) {
            addToCalendar();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            shareEvent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void deleteEvent() {
        if (currentEvent.getId() != null && !currentEvent.getId().isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Удаление события")
                    .setMessage("Вы действительно хотите удалить это событие?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        viewModel.deleteEvent(currentEvent.getId());
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        }
    }
    
    private void addToCalendar() {
        if (checkCalendarPermission()) {
            updateEventFromUI();
            viewModel.addEventToCalendar(currentEvent);
        }
    }
    
    private void shareEvent() {
        updateEventFromUI();
        viewModel.shareEvent(currentEvent);
    }
    
    private boolean checkCalendarPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                    CALENDAR_PERMISSION_REQUEST_CODE
            );
            return false;
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addToCalendar();
            } else {
                Toast.makeText(requireContext(), "Необходимо разрешение для работы с календарем", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener("locationSelection", getViewLifecycleOwner(), (requestKey, result) -> {
            if (result.containsKey("latitude") && result.containsKey("longitude")) {
                if (currentEvent != null) {
                    currentEvent.setLatitude(result.getDouble("latitude"));
                    currentEvent.setLongitude(result.getDouble("longitude"));
                    String address = result.getString("address", "");
                    currentEvent.setLocation(address);
                    locationEditText.setText(address);
                }
            }
        });
    }
    
    private void navigateToMap() {
        Bundle args = new Bundle();
        args.putBoolean("selectLocation", true);
        if (currentEvent != null && currentEvent.getLatitude() != 0 && currentEvent.getLongitude() != 0) {
            args.putFloat("latitude", (float)currentEvent.getLatitude());
            args.putFloat("longitude", (float)currentEvent.getLongitude());
        }
        Navigation.findNavController(requireView()).navigate(R.id.action_eventEditorFragment_to_mapFragment, args);
    }
} 