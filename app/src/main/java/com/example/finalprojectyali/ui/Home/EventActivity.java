package com.example.finalprojectyali.ui.Home;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.IngredientsAdapter;
import com.example.finalprojectyali.Extras.EventRepository;
import com.example.finalprojectyali.Extras.GuiderDialog;
import com.example.finalprojectyali.Models.Event;
import com.example.finalprojectyali.Models.Ingredient;
import com.example.finalprojectyali.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.android.material.navigation.NavigationView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class EventActivity extends AppCompatActivity
        implements IngredientsAdapter.IngredientActions {

    /* UI */
    private MaterialToolbar toolbar;
    private TextView totalTv, perTv, membersTv;
    private RecyclerView ingRv;
    private ExtendedFloatingActionButton fab;

    /* data */
    private final List<Ingredient> ingList = new ArrayList<>();
    private IngredientsAdapter ingAdapter;

    private Event event;
    private String myUid;
    private String myName;
    private boolean isOwner;

    /* Firebase */
    private DatabaseReference evRef;
    private ValueEventListener liveListener;

    /* ─────────────────────────────────────────────── */
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);

        event = getIntentEvent();          // uses long eventDate
        if (event == null) {
            finish();
            return;
        }

        myUid = FirebaseAuth.getInstance().getUid();
        myName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        isOwner = myUid != null && myUid.equals(event.getOwnerUid());

        // Fetch the actual user name from Users database
        if (myUid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users").child(myUid);
            userRef.child("name").get().addOnSuccessListener(nameSnapshot -> {
                String userName = nameSnapshot.getValue(String.class);
                if (userName != null && !userName.trim().isEmpty()) {
                    myName = userName;
                    android.util.Log.d("EventActivity", "Updated myName from database: " + myName);
                }
            }).addOnFailureListener(e -> {
                android.util.Log.e("EventActivity", "Failed to fetch user name", e);
            });
        }

        // Debug logging
        android.util.Log.d("EventActivity", "myUid: " + myUid);
        android.util.Log.d("EventActivity", "event.getOwnerUid(): " + event.getOwnerUid());
        android.util.Log.d("EventActivity", "isOwner: " + isOwner);

        evRef = FirebaseDatabase.getInstance()
                .getReference("Events").child(event.getKey());

        bindViews();
        populateHeader();
        listenLive();

        new GuiderDialog(this, "EventActivity",
                "Manage your event ingredients here. Add items to your shopping list, check them off when acquired, and coordinate with other members.").startDialog();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.eventToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Ensure we always show event name, not app name
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        totalTv = findViewById(R.id.ev_total_tv);
        perTv = findViewById(R.id.ev_per_member_tv);
        membersTv = findViewById(R.id.ev_members_count_tv);

        ingRv = findViewById(R.id.ev_ingredients_rv);
        ingAdapter = new IngredientsAdapter(this, ingList, this);
        ingAdapter.setUserInfo(myUid, isOwner);
        ingRv.setLayoutManager(new LinearLayoutManager(this));
        ingRv.setAdapter(ingAdapter);

        fab = findViewById(R.id.ev_add_fab);
        fab.setOnClickListener(v -> showIngredientDialog(null));
    }

    private void populateHeader() {
        Date d = new Date(event.getEventDate());
        String dt = new SimpleDateFormat("dd MMM yyyy, HH:mm",
                Locale.getDefault()).format(d);

        // Always set the toolbar title to event name
        String eventName = event.getName();
        if (eventName == null || eventName.trim().isEmpty()) {
            eventName = "Event"; // fallback
        }
        
        toolbar.setTitle(eventName);
        toolbar.setSubtitle(dt);
        
        // Also set it via support action bar to be extra sure
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(eventName);
            getSupportActionBar().setSubtitle(dt);
        }
        
        android.util.Log.d("EventActivity", "Setting toolbar title to: " + eventName);

        TextView dateTv = findViewById(R.id.ev_date_tv);
        TextView locTv = findViewById(R.id.ev_loc_tv);
        dateTv.setText(dt);
        locTv.setText(event.getLocationAddress());
    }

    /* menu */
    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        // Debug logging
        android.util.Log.d("EventActivity", "onCreateOptionsMenu called, isOwner: " + isOwner);
        if (isOwner) {
            getMenuInflater().inflate(R.menu.menu_event_toolbar, m);
            android.util.Log.d("EventActivity", "Menu inflated for owner");
        } else {
            android.util.Log.d("EventActivity", "Not owner, no menu");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem i) {
        if (i.getItemId() == R.id.action_event_edit) {
            openEditDialog();
            return true;
        }
        return super.onOptionsItemSelected(i);
    }

    /* live sync */
    private void listenLive() {
        liveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                Event e = ds.getValue(Event.class);
                if (e == null) return;
                event = e;                               // keep local copy fresh
                populateHeader();                        // fix 1970 issue

                /* ingredients */
                ingList.clear();
                if (ds.child("ingredients").exists())
                    for (DataSnapshot s : ds.child("ingredients").getChildren())
                        ingList.add(s.getValue(Ingredient.class));
                ingAdapter.notifyDataSetChanged();

                /* members count */
                int accepted = 0;
                if (e.getMembers() != null)
                    for (String st : e.getMembers().values())
                        if ("accepted".equals(st)) accepted++;

                updateTotals(accepted);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError ignore) {
            }
        };
        evRef.addValueEventListener(liveListener);
    }

    private void updateTotals(int accepted) {
        int sum = 0;
        for (Ingredient i : ingList) sum += (int) (i.getPrice() * i.getQuantity());
        int each = accepted == 0 ? 0 : sum / accepted;

        totalTv.setText("₪" + sum);
        perTv.setText("₪" + each);
        membersTv.setText(String.valueOf(accepted)); // This remains the same as it's a count
    }

    /* admin edit */
    private void openEditDialog() {
        DatabaseReference memRef = evRef.child("members");
        memRef.get().addOnSuccessListener(snap -> {

            List<String> pending = new ArrayList<>();
            snap.getChildren().forEach(s -> {
                if ("pending".equals(s.getValue(String.class)))
                    pending.add(s.getKey());
            });

            View v = getLayoutInflater().inflate(R.layout.dialog_event_edit, null);
            EditText nameEt = v.findViewById(R.id.de_name);
            EditText descEt = v.findViewById(R.id.de_desc);
            EditText locEt = v.findViewById(R.id.de_loc);
            EditText dateEt = v.findViewById(R.id.de_date);

            nameEt.setText(event.getName());
            descEt.setText(event.getDescription());
            locEt.setText(event.getLocationAddress());

            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getEventDate());

            dateEt.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm",
                    Locale.getDefault()).format(cal.getTime()));

            dateEt.setOnClickListener(vw -> {
                new DatePickerDialog(this,
                        R.style.DatePickerTheme,
                        (d, yr, mo, dy) -> {
                            cal.set(yr, mo, dy);
                            new TimePickerDialog(this,
                                    R.style.TimePickerTheme,
                                    (t, hr, mn) -> {
                                        cal.set(Calendar.HOUR_OF_DAY, hr);
                                        cal.set(Calendar.MINUTE, mn);
                                        dateEt.setText(new SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm", Locale.getDefault())
                                                .format(cal.getTime()));
                                    }, cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    DateFormat.is24HourFormat(this)).show();
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)).show();
            });

            /* pending members with accept/reject buttons */
            LinearLayout box = v.findViewById(R.id.de_pending_box);
            
            if (pending.isEmpty()) {
                TextView noPending = new TextView(this);
                noPending.setText("No pending requests");
                noPending.setTextColor(Color.GRAY);
                noPending.setPadding(0, 16, 0, 16);
                box.addView(noPending);
            } else {
                TextView header = new TextView(this);
                header.setText("Pending Requests:");
                header.setTextColor(Color.WHITE);
                header.setTypeface(null, Typeface.BOLD);
                header.setPadding(0, 16, 0, 8);
                box.addView(header);
                
                for (String uid : pending) {
                    // Get user name from database
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("Users").child(uid);
                    
                    LinearLayout memberRow = new LinearLayout(this);
                    memberRow.setOrientation(LinearLayout.HORIZONTAL);
                    memberRow.setPadding(0, 8, 0, 8);
                    memberRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    
                    TextView memberName = new TextView(this);
                    memberName.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    memberName.setTextColor(Color.WHITE);
                    
                    // Fetch user name
                    userRef.child("name").get().addOnSuccessListener(nameSnap -> {
                        String name = nameSnap.getValue(String.class);
                        memberName.setText(name != null ? name : uid);
                    });
                    
                    // Accept button
                    android.widget.Button acceptBtn = new android.widget.Button(this);
                    acceptBtn.setText("✓");
                    acceptBtn.setTextColor(Color.WHITE);
                    acceptBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
                    acceptBtn.setOnClickListener(view -> {
                        EventRepository.acceptMember(event.getKey(), uid);
                        EventRepository.sendNotification(uid, 
                                "Request Accepted!", 
                                "You've been accepted to join " + event.getName(),
                                "event_accepted", 
                                event.getKey(), 
                                event.getName());
                        acceptBtn.setEnabled(false);
                        acceptBtn.setText("✓");
                    });
                    
                    // Reject button
                    android.widget.Button rejectBtn = new android.widget.Button(this);
                    rejectBtn.setText("✗");
                    rejectBtn.setTextColor(Color.WHITE);
                    rejectBtn.setBackgroundColor(Color.parseColor("#F44336"));
                    rejectBtn.setOnClickListener(view -> {
                        EventRepository.rejectMember(event.getKey(), uid);
                        EventRepository.sendNotification(uid, 
                                "Request Rejected", 
                                "Your request to join " + event.getName() + " was not approved.",
                                "event_rejected", 
                                event.getKey(), 
                                event.getName());
                        memberRow.setVisibility(View.GONE);
                    });
                    
                    memberRow.addView(memberName);
                    memberRow.addView(acceptBtn);
                    memberRow.addView(rejectBtn);
                    box.addView(memberRow);
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Edit event")
                    .setView(v)
                    .setPositiveButton("Save", (d, w) -> {

                        String newName = nameEt.getText().toString().trim();
                        String newDesc = descEt.getText().toString().trim();
                        long newWhen = cal.getTimeInMillis();      // long!
                        String newLoc = locEt.getText().toString().trim();

                        EventRepository.updateEvent(event.getKey(),
                                newName, newDesc,
                                newWhen, newLoc);

                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /* ingredient callbacks */
    @Override
    public void onIngredientChecked(int pos, boolean checked) {
        Ingredient ing = ingList.get(pos);
        
        // Check if user can acquire/free this ingredient
        if (checked && ing.isAcquired() && !myUid.equals(ing.getAcquiredByUID())) {
            Toast.makeText(this, "This ingredient is already acquired by " + ing.getAcquiredByName(), Toast.LENGTH_SHORT).show();
            // Reset checkbox to original state
            ingAdapter.notifyItemChanged(pos);
            return;
        }
        
        if (!checked && ing.isAcquired() && !myUid.equals(ing.getAcquiredByUID()) && !isOwner) {
            Toast.makeText(this, "Only " + ing.getAcquiredByName() + " or the admin can free this ingredient", Toast.LENGTH_SHORT).show();
            // Reset checkbox to original state
            ingAdapter.notifyItemChanged(pos);
            return;
        }

        DatabaseReference ref = evRef.child("ingredients").child(ing.getKey());

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData cur) {
                Ingredient i = cur.getValue(Ingredient.class);
                if (i == null) return Transaction.abort();
                i.setAcquired(checked);
                i.setAcquiredByUID(checked ? myUid : null);
                i.setAcquiredByName(checked ? myName : null);
                i.setAcquiredAt(checked ? System.currentTimeMillis() : 0);
                cur.setValue(i);
                return Transaction.success(cur);
            }

            @Override
            public void onComplete(DatabaseError e,
                                   boolean c, DataSnapshot ignore) {
            }
        });
    }

    @Override
    public void onItemClick(int pos) {
        Ingredient ing = ingList.get(pos);
        if (canEdit(ing)) {
            showIngredientDialog(ing);
        } else {
            String message = getPermissionMessage(ing, "edit");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemLongClick(int pos) {
        Ingredient ing = ingList.get(pos);
        
        if (!canEdit(ing) && !canDelete(ing)) {
            String message = getPermissionMessage(ing, "modify");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        // Build options based on permissions
        java.util.List<String> options = new java.util.ArrayList<>();
        if (canEdit(ing)) {
            options.add("Edit");
        }
        if (canDelete(ing)) {
            options.add("Delete");
        }

        new AlertDialog.Builder(this)
                .setItems(options.toArray(new String[0]), (d, i) -> {
                    String selectedOption = options.get(i);
                    if ("Edit".equals(selectedOption)) {
                        showIngredientDialog(ing);
                    } else if ("Delete".equals(selectedOption)) {
                        confirmDelete(ing);
                    }
                }).show();
    }

    /**
     * Check if user can edit this ingredient
     * Rules: Admin can edit any ingredient, users can only edit ingredients they created
     */
    private boolean canEdit(Ingredient ing) {
        if (isOwner) return true; // Admin can edit any ingredient
        if (myUid == null) return false;
        return myUid.equals(ing.getCreatedByUID()); // User can only edit ingredients they created
    }

    /**
     * Check if user can delete this ingredient
     * Rules: Same as edit, but also check if ingredient is acquired
     */
    private boolean canDelete(Ingredient ing) {
        if (!canEdit(ing)) return false; // Must be able to edit to delete
        
        // If ingredient is acquired, it can't be deleted unless freed first
        if (ing.isAcquired()) {
            return false;
        }
        
        return true;
    }

    /**
     * Get appropriate permission message for user
     */
    private String getPermissionMessage(Ingredient ing, String action) {
        if (isOwner) {
            if (ing.isAcquired()) {
                return "This ingredient has been acquired by " + ing.getAcquiredByName() + 
                       ". Please ask them to free it first before making changes.";
            }
            return "Unknown permission issue.";
        }
        
        if (ing.getCreatedByUID() != null && !myUid.equals(ing.getCreatedByUID())) {
            String creatorName = ing.getCreatedByName() != null ? ing.getCreatedByName() : "another member";
            return "You can only " + action + " ingredients that you added. This ingredient was added by " + creatorName + ".";
        }
        
        if (ing.isAcquired() && !myUid.equals(ing.getAcquiredByUID())) {
            return "This ingredient has been acquired by " + ing.getAcquiredByName() + 
                   ". It cannot be modified until they free it.";
        }
        
        return "You don't have permission to " + action + " this ingredient.";
    }

    /**
     * Confirm deletion with user
     */
    private void confirmDelete(Ingredient ing) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Ingredient")
                .setMessage("Are you sure you want to delete \"" + ing.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    evRef.child("ingredients").child(ing.getKey()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Ingredient deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete ingredient", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* add / edit ingredient */
    private void showIngredientDialog(Ingredient existing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_ingredient, null);
        EditText n = v.findViewById(R.id.di_name_et);
        EditText q = v.findViewById(R.id.di_qty_et);
        EditText p = v.findViewById(R.id.di_price_et);

        boolean edit = existing != null;
        if (edit) {
            n.setText(existing.getName());
            q.setText(String.valueOf(existing.getQuantity()));
            p.setText(String.valueOf(existing.getPrice()));
        }

        new AlertDialog.Builder(this)
                .setTitle(edit ? "Edit ingredient" : "Add ingredient")
                .setView(v)
                .setPositiveButton(edit ? "Save" : "Add", (d, w) -> {
                    String name = n.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name?", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int qty = Integer.parseInt(q.getText().toString().trim());
                        double pr = Double.parseDouble(p.getText().toString().trim());

                        if (edit) {
                            existing.setName(name);
                            existing.setQuantity(qty);
                            existing.setPrice(pr);
                            evRef.child("ingredients").child(existing.getKey()).setValue(existing);
                        } else {
                            String key = evRef.child("ingredients").push().getKey();
                            long currentTime = System.currentTimeMillis();
                            Ingredient ing = new Ingredient(name, pr, qty, false, 
                                    myUid, myName, currentTime, // Creator info
                                    null, null, 0);  // Acquisition info (empty initially)
                            ing.setKey(key);
                            evRef.child("ingredients").child(key).setValue(ing);
                        }
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* cleanup */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveListener != null) evRef.removeEventListener(liveListener);
    }

    /* intent helper */
    private Event getIntentEvent() {
        Intent i = getIntent();
        if (i == null) return null;
        
        // Check if we have complete event data from Intent
        String eventId = i.getStringExtra("event_id");
        String eventName = i.getStringExtra("event_name");
        String eventOwner = i.getStringExtra("event_owner");
        
        if (eventId != null && eventOwner != null) {
            // Complete event data available from Intent
            Event e = new Event();
            e.setKey(eventId);
            e.setName(eventName);
            e.setDescription(i.getStringExtra("event_desc"));
            e.setLocationAddress(i.getStringExtra("event_loc"));
            e.setOwnerUid(eventOwner);
            e.setEventDate(i.getLongExtra("event_time", 0));
            return e;
        } else if (eventId != null) {
            // Only event ID available, fetch from Firebase synchronously
            // Note: This is a temporary solution - ideally should be async
            android.util.Log.d("EventActivity", "Fetching event data from Firebase for ID: " + eventId);
            return fetchEventFromFirebase(eventId);
        }
        
        return null;
    }
    
    private Event fetchEventFromFirebase(String eventId) {
        // Create a temporary event with the provided event name if available
        Event tempEvent = new Event();
        tempEvent.setKey(eventId);
        
        // Use event name from intent if available, otherwise use a placeholder
        Intent intent = getIntent();
        String eventName = intent.getStringExtra("event_name");
        if (eventName != null && !eventName.trim().isEmpty()) {
            tempEvent.setName(eventName);
            android.util.Log.d("EventActivity", "Using event name from intent: " + eventName);
        } else {
            tempEvent.setName("Loading Event...");
            android.util.Log.d("EventActivity", "No event name in intent, using placeholder");
        }
        
        tempEvent.setDescription("");
        tempEvent.setLocationAddress("");
        tempEvent.setOwnerUid(""); // Will be updated by Firebase listener
        tempEvent.setEventDate(System.currentTimeMillis());
        
        // Immediately try to fetch the real event name from Firebase
        DatabaseReference eventRef = FirebaseDatabase.getInstance()
                .getReference("Events").child(eventId);
        
        eventRef.child("name").get().addOnSuccessListener(nameSnapshot -> {
            String realEventName = nameSnapshot.getValue(String.class);
            if (realEventName != null && !realEventName.trim().isEmpty()) {
                tempEvent.setName(realEventName);
                populateHeader(); // Update UI immediately with correct name
                android.util.Log.d("EventActivity", "Updated event name from Firebase: " + realEventName);
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e("EventActivity", "Failed to fetch event name", e);
        });
        
        return tempEvent;
    }
}
