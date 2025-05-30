package com.example.finalprojectyali.ui.Home;

import static com.example.finalprojectyali.ui.Home.MainActivity.current_user;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.IngredientsAdapter;
import com.example.finalprojectyali.Extras.GroupRepository;
import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.Models.Ingredient;
import com.example.finalprojectyali.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays a single group, its members count, its ingredient list,
 * and a running total of cost in ₪.  Admins can edit group meta & members.
 */
public class GroupActivity extends AppCompatActivity {

    /* ─── UI ────────────────────────────────────────────────────────── */
    private MaterialToolbar toolbar;
    private TextView membersCountTextView;
    private TextView ShekelsSumTextView;
    private RecyclerView ingredientsRecyclerView;
    private FloatingActionButton addIngredientFab;

    /* ─── Data ──────────────────────────────────────────────────────── */
    private final List<Ingredient> ingredientList = new ArrayList<>();
    private IngredientsAdapter ingredientsAdapter;
    private Group group;
    private boolean isAdmin = false;
    private double totalShekels = 0;

    /* ─── Firebase ──────────────────────────────────────────────────── */
    private final DatabaseReference ROOT = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference groupRef;
    private DatabaseReference ingredientsRef;
    private ValueEventListener ingredientsListener;

    /* ──────────────────────────────────────────────────────────────── */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group);

        /* ---- group from intent ---- */
        group = getGroupFromIntent();
        if (group == null) { finish(); return; }

        /* ---- Firebase refs ---- */
        groupRef       = ROOT.child("Groups").child(group.getKey());
        ingredientsRef = groupRef.child("ingredients");

        /* ---- views ---- */
        toolbar               = findViewById(R.id.groupToolbar);
        membersCountTextView  = findViewById(R.id.groupMembersCountTextView);
        ShekelsSumTextView    = findViewById(R.id.groupShekelSumTextView);
        ingredientsRecyclerView = findViewById(R.id.ingredientsRecyclerView);
        addIngredientFab      = findViewById(R.id.addIngredientFab);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(group.getName());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        membersCountTextView.setText(String.valueOf(group.getMembersCount()));

        /* ---- permissions ---- */
        GroupRepository.isCurrentUserOwner(group.getKey(), isOwner -> {
            isAdmin = Boolean.TRUE.equals(isOwner);
            invalidateOptionsMenu();
        });

        /* ---- RecyclerView ---- */
        ingredientsAdapter = new IngredientsAdapter(
                this,
                ingredientList,
                new IngredientsAdapter.IngredientActions() {
                    @Override public void onIngredientChecked(int pos, boolean checked) {
                        Ingredient ing = ingredientList.get(pos);
                        ing.setAcquired(checked);
                        if (checked) {
                            ing.setAcquiredBy(current_user.getName());
                            ing.setAcquiredAt(System.currentTimeMillis());
                        } else {
                            ing.setAcquiredBy("");
                            ing.setAcquiredAt(0);
                        }
                        updateIngredientInFirebase(ing);
                        ingredientsAdapter.notifyItemChanged(pos);
                    }
                    @Override public void onItemClick(int pos) {
                        showEditIngredientDialog(pos, ingredientList.get(pos));
                    }
                    @Override public void onItemLongClick(int pos) {
                        if (isAdmin) confirmDeleteIngredient(pos);
                        else Toast.makeText(GroupActivity.this,
                                "Only the owner can remove items", Toast.LENGTH_SHORT).show();
                    }
                });

        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ingredientsRecyclerView.setAdapter(ingredientsAdapter);

        addIngredientFab.setOnClickListener(v -> showAddIngredientDialog());
    }

    @Override protected void onStart() { super.onStart(); startListeningForIngredients(); }

    @Override protected void onStop() {
        super.onStop();
        if (ingredientsListener != null) ingredientsRef.removeEventListener(ingredientsListener);
    }

    /* ─── Realtime updates ─────────────────────────────────────────── */
    private void startListeningForIngredients() {

        ingredientsListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {

                List<Ingredient> fresh = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Ingredient ing = snap.getValue(Ingredient.class);
                    if (ing != null) { ing.setKey(snap.getKey()); fresh.add(ing); }
                }
                ingredientList.clear(); ingredientList.addAll(fresh);
                ingredientsAdapter.updateIngredients(fresh);
                calculateAndDisplayTotalShekels();
            }
            @Override public void onCancelled(DatabaseError error) {
                Toast.makeText(GroupActivity.this,"Failed to load ingredients",Toast.LENGTH_SHORT).show();
                Log.e("GroupActivity","ingredients load cancelled",error.toException());
            }
        };
        ingredientsRef.addValueEventListener(ingredientsListener);
    }

    /** Σ(price × qty) → TextView + DB */
    private void calculateAndDisplayTotalShekels() {
        totalShekels = 0;
        for (Ingredient ing : ingredientList)
            totalShekels += ing.getPrice() * ing.getQuantity();

        ShekelsSumTextView.setText(String.format("₪ %.2f", totalShekels));
        groupRef.child("totalShekels").setValue(totalShekels);
    }

    /* ─── Toolbar ──────────────────────────────────────────────────── */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        if (isAdmin) getMenuInflater().inflate(R.menu.menu_group_toolbar, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit_group) {
            openGroupSettingsDialog();    // ← NEW
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ════════════════════════════════════════════════════════════════ */
    /*                       GROUP-EDIT DIALOG                         */
    /* ════════════════════════════════════════════════════════════════ */
    private void openGroupSettingsDialog() {

        /* Step 1: fetch members & their names */
        groupRef.child("members").get().addOnSuccessListener(membersSnap -> {

            if (!membersSnap.exists()) { Toast.makeText(this,"No members?",Toast.LENGTH_SHORT).show(); return; }

            List<String> uids  = new ArrayList<>();
            for (DataSnapshot s : membersSnap.getChildren()) uids.add(s.getKey());

            List<Task<DataSnapshot>> nameTasks = new ArrayList<>();
            DatabaseReference usersRef = ROOT.child("Users");
            for (String uid : uids)
                nameTasks.add(usersRef.child(uid).child("name").get());

            Tasks.whenAllSuccess(nameTasks).addOnSuccessListener(res -> {

                List<String> names = new ArrayList<>();
                for (Object o : res) {
                    DataSnapshot ds = (DataSnapshot) o;
                    names.add(ds.exists() ? ds.getValue(String.class) : "Unknown");
                }
                showGroupEditDialog(uids, names);

            }).addOnFailureListener(e ->
                    Toast.makeText(this,"Failed to load users: "+e.getMessage(),Toast.LENGTH_SHORT).show());

        }).addOnFailureListener(e ->
                Toast.makeText(this,"Failed: "+e.getMessage(),Toast.LENGTH_SHORT).show());
    }

    /** Builds and shows one dialog with name/description fields and member check-boxes. */
    private void showGroupEditDialog(List<String> uids, List<String> names) {

        int pad = (int)(16 * getResources().getDisplayMetrics().density);

        /* Dynamic layout: EditTexts + check-box list */
        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad,pad,pad,pad);
        scroll.addView(container);

        EditText nameEt = new EditText(this);
        nameEt.setHint("Group name"); nameEt.setText(group.getName());
        container.addView(nameEt);

        EditText descEt = new EditText(this);
        descEt.setHint("Description"); descEt.setText(group.getDescription());
        container.addView(descEt);

        /* Check-boxes */
        Map<CheckBox,String> cbToUid = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String uid = uids.get(i);
            if (uid.equals(group.getOwnerUid())) continue;   // owner can’t be removed
            CheckBox cb = new CheckBox(this);
            cb.setText("Remove " + names.get(i));
            container.addView(cb);
            cbToUid.put(cb, uid);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit group")
                .setView(scroll)
                .setPositiveButton("Save", (d, w) -> {

                    /* ---- gather removals ---- */
                    List<String> toRemove = new ArrayList<>();
                    for (Map.Entry<CheckBox,String> e : cbToUid.entrySet())
                        if (e.getKey().isChecked()) toRemove.add(e.getValue());

                    /* ---- update name/description ---- */
                    String newName = nameEt.getText().toString().trim();
                    String newDesc = descEt.getText().toString().trim();

                    Task<Void> nameDescTask = GroupRepository.updateGroup(
                            group.getKey(), newName, newDesc,
                            v -> {}, e -> {});

                    /* ---- removals fan-out ---- */
                    Task<Void> removalsTask;
                    if (toRemove.isEmpty()) {
                        removalsTask = Tasks.forResult(null);
                    } else {
                        Map<String,Object> updates = new HashMap<>();
                        for (String uid : toRemove) {
                            updates.put("/Groups/"+group.getKey()+"/members/"+uid, null);
                            updates.put("/Users/"+uid+"/Groups/"+group.getKey(), null);
                        }
                        updates.put("/Groups/"+group.getKey()+"/membersCount",
                                ServerValue.increment(-toRemove.size()));
                        removalsTask = ROOT.updateChildren(updates);
                    }

                    Tasks.whenAll(nameDescTask, removalsTask)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this,"Group updated",Toast.LENGTH_SHORT).show();
                                /* Update local copy + UI immediately */
                                group.setName(newName); group.setDescription(newDesc);
                                group.setMembersCount(group.getMembersCount() - toRemove.size());
                                toolbar.setTitle(newName);
                                membersCountTextView.setText(String.valueOf(group.getMembersCount()));
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,"Update failed: "+e.getMessage(),Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ════════════════════════════════════════════════════════════════ */
    /*                 INGREDIENT DIALOG HELPERS                       */
    /* ════════════════════════════════════════════════════════════════ */
    private void showEditIngredientDialog(int pos, Ingredient ing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_ingredient, null);
        EditText nameEt     = v.findViewById(R.id.editName);
        EditText priceEt    = v.findViewById(R.id.editPrice);
        EditText quantityEt = v.findViewById(R.id.editQuantity);

        nameEt.setText(ing.getName());
        priceEt.setText(String.valueOf(ing.getPrice()));
        quantityEt.setText(String.valueOf(ing.getQuantity()));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit ingredient")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    ing.setName(nameEt.getText().toString().trim());
                    ing.setPrice(Double.parseDouble(priceEt.getText().toString()));
                    ing.setQuantity(Integer.parseInt(quantityEt.getText().toString()));
                    updateIngredientInFirebase(ing);
                    ingredientsAdapter.notifyItemChanged(pos);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddIngredientDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_ingredient, null);
        EditText nameEt     = v.findViewById(R.id.editName);
        EditText priceEt    = v.findViewById(R.id.editPrice);
        EditText quantityEt = v.findViewById(R.id.editQuantity);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add ingredient")
                .setView(v)
                .setPositiveButton("Add", (d, w) -> {
                    String name = nameEt.getText().toString().trim();
                    String pStr = priceEt.getText().toString().trim();
                    String qStr = quantityEt.getText().toString().trim();
                    if (name.isEmpty() || pStr.isEmpty() || qStr.isEmpty()) {
                        Toast.makeText(this,"All fields required",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double price = Double.parseDouble(pStr);
                        int qty      = Integer.parseInt(qStr);

                        String id = ingredientsRef.push().getKey();
                        Ingredient ing = new Ingredient(name, price, qty,false,"",0);
                        ing.setKey(id);
                        ingredientsRef.child(id).setValue(ing);

                    } catch (NumberFormatException e) {
                        Toast.makeText(this,"Invalid numbers",Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteIngredient(int pos) {
        Ingredient ing = ingredientList.get(pos);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove ingredient?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) ->
                        ingredientsRef.child(ing.getKey()).removeValue())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ─── Helpers ─────────────────────────────────────────────────── */
    private void updateIngredientInFirebase(Ingredient ing) {
        ingredientsRef.child(ing.getKey()).setValue(ing);
    }

    /** Build a Group object from Intent extras */
    private Group getGroupFromIntent() {
        if (getIntent() == null) return null;
        return new Group(
                getIntent().getStringExtra("group_name"),
                getIntent().getStringExtra("group_description"),
                null,
                getIntent().getIntExtra("group_membersCount",0),
                null,
                getIntent().getStringExtra("group_key"),
                getIntent().getStringExtra("group_ownerUid"),
                null,
                getIntent().getStringExtra("group_joinCode")
        );
    }
}
