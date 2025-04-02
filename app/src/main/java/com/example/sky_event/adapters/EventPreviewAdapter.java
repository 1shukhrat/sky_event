package com.example.sky_event.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sky_event.R;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.utils.DateFormatter;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class EventPreviewAdapter extends RecyclerView.Adapter<EventPreviewAdapter.EventViewHolder> {

    private final List<Event> events;
    private final OnEventClickListener onEventClickListener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventPreviewAdapter(List<Event> events, OnEventClickListener onEventClickListener) {
        this.events = events;
        this.onEventClickListener = onEventClickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_preview, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventNameTextView;
        private final TextView eventDateTextView;
        private final TextView eventLocationTextView;
        private final MaterialCardView cardView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameTextView = itemView.findViewById(R.id.textViewEventName);
            eventDateTextView = itemView.findViewById(R.id.textViewEventDate);
            eventLocationTextView = itemView.findViewById(R.id.textViewEventLocation);
            cardView = itemView.findViewById(R.id.cardViewEvent);
        }

        public void bind(Event event) {
            eventNameTextView.setText(event.getName());
            
            if (event.getDate() != null) {
                String formattedDate = DateFormatter.formatDateForDisplay(event.getDate());
                String weekday = DateFormatter.formatWeekday(event.getDate());
                eventDateTextView.setText(String.format("%s (%s)", formattedDate, weekday));
            } else {
                eventDateTextView.setText("Дата не указана");
            }
            
            eventLocationTextView.setText(event.getLocation());
            
            cardView.setOnClickListener(v -> {
                if (onEventClickListener != null) {
                    onEventClickListener.onEventClick(event);
                }
            });
        }
    }
} 