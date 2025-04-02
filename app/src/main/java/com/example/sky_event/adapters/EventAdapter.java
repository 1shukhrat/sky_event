package com.example.sky_event.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sky_event.R;
import com.example.sky_event.models.event.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private final EventClickListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());

    public interface EventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(EventClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView dateTextView;
        private final TextView locationTextView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewEventName);
            dateTextView = itemView.findViewById(R.id.textViewEventDate);
            locationTextView = itemView.findViewById(R.id.textViewEventLocation);
        }

        public void bind(Event event, EventClickListener listener) {
            nameTextView.setText(event.getName());
            
            if (event.getDate() != null) {
                dateTextView.setText(dateFormat.format(event.getDate()));
            } else {
                dateTextView.setText(R.string.auto_select_date);
            }
            
            locationTextView.setText(event.getLocation());
            
            itemView.setOnClickListener(v -> listener.onEventClick(event));
        }
    }
} 