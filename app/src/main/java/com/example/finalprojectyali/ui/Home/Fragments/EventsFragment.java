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
import com.example.finalprojectyali.Extras.Utils;
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
        long tenSecondsFromNow = System.currentTimeMillis() + 10_000;
        Utils.scheduleNotification(
                getContext(),
                tenSecondsFromNow,
                "Break time",
                "Stand up and stretch!",
                R.drawable.baseline_notification_important      // or 0 for default
        );
        rv = v.findViewById(R.id.events_rv);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adGoing = new EventAdapter(requireContext(), going, this);
        adPending = new EventAdapter(requireContext(), pending, this);
        adAvail = new EventAdapter(requireContext(), available, this);

        ConcatAdapter concat = new ConcatAdapter(
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
    public void onItemClick(int pos) {
        // find which adapter triggered the click
        // simplistic approach: search lists
        Event e = null;
        if (pos < going.size()) e = going.get(pos);
        else if (pos < going.size() + pending.size()) e = pending.get(pos - going.size());
        else e = available.get(pos - going.size() - pending.size());

        switch (e.getStatus()) {
            case ACCEPTED:
                Intent i = new Intent(requireContext(), EventActivity.class);
                i.putExtra("event_id", e.getKey());
                i.putExtra("event_name", e.getName());
                i.putExtra("event_desc", e.getDescription());
                i.putExtra("event_loc", e.getLocationAddress());
                i.putExtra("event_owner", e.getOwnerUid());
                i.putExtra("event_time", e.getEventDate());
                startActivity(i);
                break;
            case PENDING:
                // no‑op or maybe let user cancel request
                break;
            case AVAILABLE:
                EventRepository.requestJoin(e.getKey(), v -> {/*toast*/});
                break;
        }
    }

    @Override
    public boolean onItemLongClick(int p) {
        return false;
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
