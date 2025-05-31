package com.example.finalprojectyali.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Models.Event;
import com.example.finalprojectyali.Models.Event.Status;
import com.example.finalprojectyali.R;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Simple 1‑column adapter. XML: event_item.xml                            */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.Holder> {

    private final Context ctx;
    private final List<Event> data;
    private final RecyclerViewFunctionalities cb;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public EventAdapter(Context c, List<Event> d, RecyclerViewFunctionalities cb){
        this.ctx=c; this.data=d; this.cb=cb; }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p, int v){
        View v1 = LayoutInflater.from(ctx).inflate(R.layout.item_event, p, false);
        return new Holder(v1);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int pos){
        Event e = data.get(pos);
        h.title.setText(e.getName());
        h.date.setText(fmt.format(e.getEventDate()));
        
        // Set participant count (you can modify this based on actual data)
        h.participantCount.setText("0"); // Replace with actual participant count if available

        // status chip text & color
        switch(e.getStatus()){
            case ACCEPTED:  
                h.status.setText("✔️ Going");
                h.status.setChipBackgroundColorResource(R.color.success_color);
                h.status.setTextColor(ContextCompat.getColor(ctx, R.color.white));
                break;
            case PENDING:   
                h.status.setText("⏳ Pending");
                h.status.setChipBackgroundColorResource(R.color.accent_color);
                h.status.setTextColor(ContextCompat.getColor(ctx, R.color.white));
                break;
            case AVAILABLE: 
                h.status.setText("➕ Available");
                h.status.setChipBackgroundColorResource(R.color.primary_color);
                h.status.setTextColor(ContextCompat.getColor(ctx, R.color.white));
                break;
        }

        // click on whole card
        h.itemView.setOnClickListener(v -> cb.onItemClick(pos));

        // menu dots click
        h.menuDots.setOnClickListener(v -> {
            // Handle menu click here
            cb.onItemClick(pos); // For now, just trigger item click
        });
    }

    @Override public int getItemCount(){ return data==null?0:data.size(); }

    class Holder extends RecyclerView.ViewHolder{
        TextView title, date, participantCount; 
        Chip status; 
        View menuDots;
        Holder(@NonNull View v){ super(v);
            title = v.findViewById(R.id.event_name);
            date = v.findViewById(R.id.event_date);
            status = v.findViewById(R.id.status_chip);
            participantCount = v.findViewById(R.id.participant_count);
            menuDots = v.findViewById(R.id.dots);
        }
    }
}
