package com.example.sky_event.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.adapters.EventAdapter;
import com.example.sky_event.databinding.FragmentEventsBinding;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.viewmodels.EventViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class EventsFragment extends Fragment implements EventAdapter.EventClickListener {

    private FragmentEventsBinding binding;
    private EventViewModel eventViewModel;
    private EventAdapter eventAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        setupRecyclerView();
        setupFab();
        checkUserAuthentication();
        observeViewModel();
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(this);
        binding.recyclerViewEvents.setAdapter(eventAdapter);
    }

    private void setupFab() {
        binding.fabAddEvent.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_createEventFragment);
        });
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Navigation.findNavController(requireView()).navigate(R.id.profileFragment);
            Toast.makeText(requireContext(), "Войдите в аккаунт для просмотра событий", Toast.LENGTH_SHORT).show();
        } else {
            binding.progressBar.setVisibility(View.VISIBLE);
            eventViewModel.fetchUserEvents();
        }
    }

    private void observeViewModel() {
        eventViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            binding.progressBar.setVisibility(View.GONE);
            updateEvents(events);
        });
        
        eventViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        
        eventViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    private void updateEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            binding.textViewNoEvents.setVisibility(View.VISIBLE);
            binding.recyclerViewEvents.setVisibility(View.GONE);
        } else {
            binding.textViewNoEvents.setVisibility(View.GONE);
            binding.recyclerViewEvents.setVisibility(View.VISIBLE);
            
            for (Event event : events) {
                if (event.getLatitude() != 0 && event.getLongitude() != 0) {
                    loadWeatherForEvent(event);
                }
            }
            
            eventAdapter.setEvents(events);
        }
    }
    
    private void loadWeatherForEvent(Event event) {
        double latitude = event.getLatitude();
        double longitude = event.getLongitude();
        
        if (latitude != 0 && longitude != 0) {
            eventViewModel.getWeatherRepository().getCurrentWeather(latitude, longitude)
                .observe(getViewLifecycleOwner(), weatherResponse -> {
                    if (weatherResponse != null && event.getWeatherCondition() != null) {
                        event.getWeatherCondition().setCurrentTemperature(weatherResponse.getMain().getTemp());
                        event.getWeatherCondition().setCurrentHumidity(weatherResponse.getMain().getHumidity());
                        event.getWeatherCondition().setCurrentWindSpeed(weatherResponse.getWind().getSpeed());
                        
                        boolean isClear = false;
                        if (!weatherResponse.getWeather().isEmpty()) {
                            String weatherMain = weatherResponse.getWeather().get(0).getMain().toLowerCase();
                            isClear = weatherMain.contains("clear") || weatherMain.contains("sun");
                        }
                        event.getWeatherCondition().setNoRain(isClear);
                        
                        eventAdapter.notifyDataSetChanged();
                    }
                });
        }
    }

    @Override
    public void onEventClick(Event event) {
        Bundle bundle = new Bundle();
        bundle.putString("eventId", event.getId());
        Navigation.findNavController(requireView()).navigate(
                R.id.action_eventsFragment_to_eventDetailsFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 