package com.example.finalprojectyali.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Models.Event;
import com.example.finalprojectyali.R;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.Holder> {

    private final Context ctx;
    private final List<Event> data;
    private final RecyclerViewFunctionalities cb;
    private final SimpleDateFormat fmt =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public EventAdapter(Context c, List<Event> d, RecyclerViewFunctionalities cb) {
        this.ctx = c;
        this.data = d;
        this.cb = cb;
    }

    /*────────────────────────────────────────────────────────────*/

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View v1 = LayoutInflater.from(ctx).inflate(R.layout.event_item, p, false);
        return new Holder(v1);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Event e = data.get(pos);

        h.title.setText(e.getName());
        h.desc.setText(e.getDescription());
        h.date.setText(fmt.format(e.getEventDate()));

        // members currently "accepted"
        int going = 0;
        for (String st : e.getMembers().values())
            if ("accepted".equals(st)) going++;
        h.members.setText(String.valueOf(going));

        // status chip
        switch (e.getStatus()) {
            case ACCEPTED:
                h.status.setText("✔ Going");
                h.status.setChipBackgroundColorResource(R.color.success_color);
                h.status.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
                break;

            case PENDING:
                h.status.setText("⏳ Pending");
                h.status.setChipBackgroundColorResource(R.color.accent_color);
                h.status.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
                break;

            case AVAILABLE:
                h.status.setText("➕ Available");
                h.status.setChipBackgroundColorResource(R.color.primary_color);
                h.status.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
                break;
        }

        /* callbacks */
        h.itemView.setOnClickListener(v -> cb.onEventClick(e));
        h.itemView.setOnLongClickListener(v -> cb.onEventLongClick(e));
        
        // Map icon click listener
        h.mapIconContainer.setOnClickListener(v -> cb.onMapIconClick(e));
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    /*────────────────────────────────────────────────────────────*/

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, desc, date, members;
        Chip     status;
        View     mapIconContainer;

        Holder(@NonNull View v) {
            super(v);
            title   = v.findViewById(R.id.ev_title);
            desc    = v.findViewById(R.id.ev_desc);
            date    = v.findViewById(R.id.ev_date);
            members = v.findViewById(R.id.ev_members_count);
            status  = v.findViewById(R.id.ev_status_chip);
            mapIconContainer = v.findViewById(R.id.ev_map_icon_container);
        }
    }
}
