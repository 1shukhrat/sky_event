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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sky_event.adapters.ParticipantsAdapter;
import com.example.sky_event.databinding.FragmentEventParticipantsBinding;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.viewmodels.EventViewModel;

import java.util.ArrayList;
import java.util.List;

public class EventParticipantsFragment extends Fragment {
    private FragmentEventParticipantsBinding binding;
    private EventViewModel eventViewModel;
    private ParticipantsAdapter adapter;
    private String eventId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventParticipantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        getEventIdFromArguments();
        setupToolbar();
        setupRecyclerView();
        observeViewModel();
        
        if (eventId != null) {
            eventViewModel.fetchEvent(eventId);
            eventViewModel.fetchEventParticipants(eventId);
        }
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
    }

    private void getEventIdFromArguments() {
        if (getArguments() != null && getArguments().containsKey("eventId")) {
            eventId = getArguments().getString("eventId");
        }
    }
    
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(requireView()).popBackStack());
    }
    
    private void setupRecyclerView() {
        adapter = new ParticipantsAdapter(new ArrayList<>());
        binding.recyclerViewParticipants.setAdapter(adapter);
        binding.recyclerViewParticipants.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void observeViewModel() {
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                updateEventInfo(event);
            }
        });
        
        eventViewModel.getEventParticipants().observe(getViewLifecycleOwner(), participants -> {
            updateParticipantsList(participants);
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
    
    private void updateEventInfo(Event event) {
        binding.textViewEventName.setText(event.getName());
        binding.chipCategory.setText(event.getCategory());
        
        String participantsText = event.getParticipantsCount() + " человек";
        if (event.getParticipantsCount() % 10 == 1 && event.getParticipantsCount() % 100 != 11) {
            participantsText += " подтвердил участие";
        } else {
            participantsText += " подтвердили участие";
        }
        binding.textViewParticipantsCount.setText(participantsText);
    }
    
    private void updateParticipantsList(List<String> participants) {
        if (participants.isEmpty()) {
            binding.textViewEmptyList.setVisibility(View.VISIBLE);
            binding.recyclerViewParticipants.setVisibility(View.GONE);
        } else {
            binding.textViewEmptyList.setVisibility(View.GONE);
            binding.recyclerViewParticipants.setVisibility(View.VISIBLE);
            adapter.updateData(participants);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 