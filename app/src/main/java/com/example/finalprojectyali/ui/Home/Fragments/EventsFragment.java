package com.example.finalprojectyali.ui.Home.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.EventAdapter;
import com.example.finalprojectyali.Adapters.RecyclerViewFunctionalities;
import com.example.finalprojectyali.Extras.EventRepository;
import com.example.finalprojectyali.Models.Event;
import com.example.finalprojectyali.Models.Event.Status;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Home.EventActivity; // TODO make activity
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows three categories: Going, Pending, Available
 */
public class EventsFragment extends Fragment implements RecyclerViewFunctionalities {

    private RecyclerView rv;

    private final List<Event> going = new ArrayList<>();
    private final List<Event> pending = new ArrayList<>();
    private final List<Event> available = new ArrayList<>();

    private final Map<String, Event> allEvents = new HashMap<>();
    private final Map<String, String> myStatuses = new HashMap<>(); // id -> status

    private EventAdapter adGoing, adPending, adAvail;

    private DatabaseReference eventsRef;
    private ChildEventListener eventsListener;
    private DatabaseReference userEvRef;
    private ValueEventListener statusListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle b) {
        return i.inflate(R.layout.fragment_events, c, false); // TODO(🖼️)
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        rv = v.findViewById(R.id.events_rv);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adGoing = new EventAdapter(requireContext(), going, this);
        adPending = new EventAdapter(requireContext(), pending, this);
        adAvail = new EventAdapter(requireContext(), available, this);

        ConcatAdapter concat = new ConcatAdapter( //מחבר את כל הרשימות לרשימה אחת
                new SectionHeader("Going"), adGoing,
                new SectionHeader("Pending"), adPending,
                new SectionHeader("Available"), adAvail);
        rv.setAdapter(concat);

        // Firebase refs
        eventsRef = FirebaseDatabase.getInstance().getReference("Events");
        userEvRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getUid())
                .child("Events");

        attachStatusListener();   // 1st: know my statuses
        attachEventsListener();   // 2nd: live events
    }

    /*──────────────── helpers ───────────────*/
    private void attachStatusListener() {
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                myStatuses.clear();
                for (DataSnapshot c : ds.getChildren())
                    myStatuses.put(c.getKey(), c.getValue(String.class));
                distribute();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.e("EventsFrag", "status", e.toException());
            }
        };
        userEvRef.addValueEventListener(statusListener);
    }

    private void attachEventsListener() {
        eventsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot s, String p) {
                upd(s);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, String p) {
                upd(s);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
                allEvents.remove(s.getKey());
                distribute();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.e("EventsFrag", "events", e.toException());
            }

            private void upd(DataSnapshot s) {
                allEvents.put(s.getKey(), s.getValue(Event.class));
                distribute();
            }
        };
        eventsRef.addChildEventListener(eventsListener);
    }

    private void distribute() {
        going.clear();
        pending.clear();
        available.clear();
        for (Event e : allEvents.values()) {
            String st = myStatuses.get(e.getKey());
            if ("accepted".equals(st)) {
                e.setStatus(Status.ACCEPTED);
                going.add(e);
            } else if ("pending".equals(st)) {
                e.setStatus(Status.PENDING);
                pending.add(e);
            } else {
                e.setStatus(Status.AVAILABLE);
                available.add(e);
            }
        }
        adGoing.notifyDataSetChanged();
        adPending.notifyDataSetChanged();
        adAvail.notifyDataSetChanged();
    }

    /*──────────────── RecyclerViewFunctionalities ───────────────*/
    @Override
    public void onEventClick(Event e) {

        switch (e.getStatus()) {
            case ACCEPTED:
                Intent i = new Intent(requireContext(), EventActivity.class);
                i.putExtra("event_id",   e.getKey());
                i.putExtra("event_name", e.getName());
                i.putExtra("event_desc", e.getDescription());
                i.putExtra("event_loc",  e.getLocationAddress());
                i.putExtra("event_owner",e.getOwnerUid());
                i.putExtra("event_time", e.getEventDate());
                startActivity(i);
                break;

            case PENDING:
                // let the user cancel a request here if you want
                break;

            case AVAILABLE:
                EventRepository.requestJoin(e.getKey(), v -> {/* toast, snackbar, etc. */});
                break;
        }
    }

    @Override public boolean onEventLongClick(Event e) { 
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return false;
        
        boolean isOwner = currentUid.equals(e.getOwnerUid());
        
        if (isOwner) {
            // Show delete option for event owner
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete \"" + e.getName() + "\"? This action cannot be undone and will remove the event for all members.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        EventRepository.deleteEvent(e.getKey(), 
                            v -> {
                                Toast.makeText(requireContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show();
                            },
                            error -> {
                                Toast.makeText(requireContext(), "Failed to delete event: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Show leave option for regular member
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Leave Event")
                    .setMessage("Are you sure you want to leave \"" + e.getName() + "\"?")
                    .setPositiveButton("Leave", (dialog, which) -> {
                        EventRepository.leaveEvent(e.getKey(),
                            v -> {
                                Toast.makeText(requireContext(), "Left event successfully", Toast.LENGTH_SHORT).show();
                            },
                            error -> {
                                Toast.makeText(requireContext(), "Failed to leave event: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        
        return true;
    }

    @Override
    public void onMapIconClick(Event e) {
        String address = e.getLocationAddress();
        if (address != null && !address.trim().isEmpty()) {
            // Create intent to open Google Maps with the address
            String uri = "geo:0,0?q=" + android.net.Uri.encode(address);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            
            // Check if Google Maps is installed
            if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback: open in browser
                String browserUri = "https://maps.google.com/?q=" + android.net.Uri.encode(address);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(browserUri));
                startActivity(browserIntent);
            }
        } else {
            Toast.makeText(requireContext(), "No location available for this event", Toast.LENGTH_SHORT).show();
        }
    }

    /*──────────────── Section header adapter ───────────────*/
    private static class SectionHeader extends RecyclerView.Adapter<SectionHeader.H> {
        private final String title;

        SectionHeader(String t) {
            this.title = t;
        }

        @NonNull
        @Override
        public H onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View v1 = LayoutInflater.from(p.getContext()).inflate(R.layout.section_header, p, false); // TODO(🖼️)
            return new H(v1);
        }

        @Override
        public void onBindViewHolder(@NonNull H h, int p) {
            h.t.setText(title);
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        static class H extends RecyclerView.ViewHolder {
            TextView t;

            H(View v) {
                super(v);
                t = v.findViewById(R.id.section_title);
            }
        }
    }

    /*──────────────── cleanup ───────────────*/
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statusListener != null) userEvRef.removeEventListener(statusListener);
        if (eventsListener != null) eventsRef.removeEventListener(eventsListener);
    }
}
