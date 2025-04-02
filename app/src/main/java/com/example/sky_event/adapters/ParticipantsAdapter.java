package com.example.sky_event.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sky_event.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder> {
    
    private List<String> participants;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    
    public ParticipantsAdapter(List<String> participants) {
        this.participants = participants;
    }
    
    public void updateData(List<String> newParticipants) {
        this.participants = newParticipants;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        String name = participants.get(position);
        holder.textViewName.setText(name);
        
        String joinDate = "Присоединился " + dateFormat.format(new Date());
        holder.textViewDate.setText(joinDate);
    }
    
    @Override
    public int getItemCount() {
        return participants.size();
    }
    
    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDate;
        
        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewParticipantName);
            textViewDate = itemView.findViewById(R.id.textViewParticipantDate);
        }
    }
} 