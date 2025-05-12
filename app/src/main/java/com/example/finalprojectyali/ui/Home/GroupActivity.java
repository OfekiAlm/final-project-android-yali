package com.example.finalprojectyali.ui.Home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.IngredientsAdapter;
import com.example.finalprojectyali.Extras.GroupRepository;
import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.Models.Ingredient;
import com.example.finalprojectyali.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GroupActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView membersCountTextView;
    private RecyclerView ingredientsRecyclerView;
    private FloatingActionButton addIngredientFab;
    private IngredientsAdapter ingredientsAdapter;
    private Group group;
    private boolean isAdmin = false; // Set this based on your logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group);

        toolbar = findViewById(R.id.groupToolbar);
        membersCountTextView = findViewById(R.id.groupMembersCountTextView);
        ingredientsRecyclerView = findViewById(R.id.ingredientsRecyclerView);
        addIngredientFab = findViewById(R.id.addIngredientFab);

        // Example: get group from intent or ViewModel
        group = getGroupFromIntent();
        if (group == null) {
            finish();
            return;
        }

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(group.getName());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set member count
        membersCountTextView.setText(String.valueOf(group.getMembersCount()));

        // Show/hide 3-dot menu for admin
        // TODO: to implement....
        //isAdmin = GroupRepository.isCurrentUserOwner();
        invalidateOptionsMenu();

        // Ingredients RecyclerView
        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ingredientsAdapter = new IngredientsAdapter(this, group.getIngredientList(), new IngredientsAdapter.IngredientActions() {
            @Override
            public void onIngredientChecked(int position, boolean isChecked) {
                // Handle check/uncheck
                group.getIngredientList().get(position).setAcquired(isChecked);

                // postpone until RecyclerView is idle
                ingredientsRecyclerView.post(() ->
                        ingredientsAdapter.notifyItemChanged(position));
            }

            @Override
            public void onItemClick(int position) {
                Ingredient ing = group.getIngredientList().get(position);
                showEditDialog(position, ing);
            }

            @Override
            public void onItemLongClick(int position) {
                GroupRepository.isCurrentUserOwner(group.getKey(), isOwner -> {
                    if (Boolean.TRUE.equals(isOwner)) {
                        confirmDelete(position);
                    } else {
                        Toast.makeText(GroupActivity.this,
                                "Only the owner can remove items", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        List<Ingredient> list = group.getIngredientList();
        Log.d("GroupActivity", "ingredient list size = " + (list == null ? "null" : list.size()));
        ingredientsRecyclerView.setAdapter(ingredientsAdapter);

        // FAB to add ingredient
        addIngredientFab.setOnClickListener(v -> showAddDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isAdmin) {
            getMenuInflater().inflate(R.menu.menu_group_toolbar, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit_group) {
            // Open group settings
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Group getGroupFromIntent() {
        // TODO: Get group from intent or ViewModel
        // Example:
        // return (Group) getIntent().getSerializableExtra("group");
        ArrayList<Ingredient> exampleIngredients = new ArrayList<>();
        exampleIngredients.add(new Ingredient("Tomatoes", 5.0, 3, false, "", 0));
        exampleIngredients.add(new Ingredient("Cheese", 12.5, 1, false, "", 0));
        exampleIngredients.add(new Ingredient("Bread", 7.0, 2, false, "", 0));
        exampleIngredients.add(new Ingredient("Milk", 6.0, 1, false, "", 0));
        exampleIngredients.add(new Ingredient("Eggs", 10.0, 12, false, "", 0));
        return new Group("Example Group", "Description", "ownerUid", "JOIN123", exampleIngredients);
    }

    /* ──────────────────────────────────────────────────────────── */
    /*  Show a dialog that lets the user edit an ingredient         */
    /* ──────────────────────────────────────────────────────────── */
    private void showEditDialog(int position, Ingredient ing) {

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
                .setPositiveButton("Save", (d, i) -> {
                    ing.setName(nameEt.getText().toString().trim());
                    ing.setPrice(Double.parseDouble(priceEt.getText().toString()));
                    ing.setQuantity(Integer.parseInt(quantityEt.getText().toString()));
                    ingredientsAdapter.notifyItemChanged(position);

                    /* TODO: if you persist to Firebase, call your repo here */
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ───────────────────────────────────────────────────────── */
    /* 1. Show a dialog that lets the user add a brand-new       */
    /*    ingredient                                             */
    /* ───────────────────────────────────────────────────────── */
    private void showAddDialog() {

        View v = getLayoutInflater().inflate(R.layout.dialog_edit_ingredient, null);
        EditText nameEt     = v.findViewById(R.id.editName);
        EditText priceEt    = v.findViewById(R.id.editPrice);
        EditText quantityEt = v.findViewById(R.id.editQuantity);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add ingredient")
                .setView(v)
                .setPositiveButton("Add", (d, i) -> {

                    /* ---- simple validation ---- */
                    String name = nameEt.getText().toString().trim();
                    String priceStr = priceEt.getText().toString().trim();
                    String qtyStr   = quantityEt.getText().toString().trim();

                    if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
                        Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price;
                    int    qty;
                    try {
                        price = Double.parseDouble(priceStr);
                        qty   = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    /* ---- build & insert new ingredient ---- */
                    Ingredient ing = new Ingredient(name, price, qty,
                            false, "", 0 /*acquiredAt*/);

                    int insertAt = group.getIngredientList().size();
                    group.getIngredientList().add(ing);
                    ingredientsAdapter.notifyItemInserted(insertAt);
                    ingredientsRecyclerView.scrollToPosition(insertAt);

                    /* TODO:  persist to Firebase
                       Example (pseudo):
                       GroupRepository.addIngredient(group.getKey(), ing);
                    */
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /* ──────────────────────────────────────────────────────────── */
    /*  Ask for confirmation and remove the ingredient              */
    /* ──────────────────────────────────────────────────────────── */
    private void confirmDelete(int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove ingredient?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    group.getIngredientList().remove(position);
                    ingredientsAdapter.notifyItemRemoved(position);

                    /* TODO: persist delete in Firebase if needed */
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}