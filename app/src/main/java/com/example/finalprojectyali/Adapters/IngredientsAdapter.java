package com.example.finalprojectyali.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Models.Ingredient;
import com.example.finalprojectyali.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for a list of {@link Ingredient}.
 */
public class IngredientsAdapter
        extends RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder> {

    /* ─── Callbacks ─────────────────────────────────────────── */
    public interface IngredientActions {
        void onIngredientChecked(int position, boolean isChecked);

        void onItemClick(int position);

        void onItemLongClick(int position);
    }

    /* ─── Data ──────────────────────────────────────────────── */
    private final List<Ingredient> ingredientList;
    private final Context context;
    private final IngredientActions ingredientActions;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    
    private String currentUserUid;
    private boolean isOwner;

    public IngredientsAdapter(Context ctx,
                              List<Ingredient> list,
                              IngredientActions actions) {
        this.context = ctx;
        this.ingredientList = list;
        this.ingredientActions = actions;
    }

    /**
     * Update user information for permission checking
     */
    public void setUserInfo(String userUid, boolean isEventOwner) {
        this.currentUserUid = userUid;
        this.isOwner = isEventOwner;
    }

    /* ─── Required overrides ───────────────────────────────── */
    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.ingredient_item, parent, false);
        return new IngredientViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Ingredient ing = ingredientList.get(position);

        holder.nameTextView.setText(ing.getName());
        holder.quantityTextView.setText(String.valueOf(ing.getQuantity()));
        holder.priceTextView.setText(
                context.getString(R.string.ingredient_price_format, ing.getPrice()));

        // Show creator information
        if (ing.getCreatedByName() != null && !ing.getCreatedByName().trim().isEmpty()) {
            holder.createdByTextView.setVisibility(View.VISIBLE);
            holder.createdByTextView.setText("Added by " + ing.getCreatedByName());
        } else {
            holder.createdByTextView.setVisibility(View.GONE);
        }

        holder.acquiredCheckBox.setOnCheckedChangeListener(null);
        holder.acquiredCheckBox.setChecked(ing.isAcquired());
        
        // Enable/disable checkbox based on user permissions
        boolean canInteractWithCheckbox = canUserInteractWithCheckbox(ing);
        holder.acquiredCheckBox.setEnabled(canInteractWithCheckbox);
        holder.acquiredCheckBox.setAlpha(canInteractWithCheckbox ? 1.0f : 0.5f);

        if (ing.isAcquired()) {
            holder.nameTextView.setPaintFlags(
                    holder.nameTextView.getPaintFlags()
                            | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            
            // Show inline acquired name
            if (ing.getAcquiredByName() != null && !ing.getAcquiredByName().isEmpty()) {
                holder.acquiredNameTextView.setVisibility(View.VISIBLE);
                holder.acquiredNameTextView.setText("by " + ing.getAcquiredByName());
            } else {
                holder.acquiredNameTextView.setVisibility(View.GONE);
            }
            
            // Hide expanded container since we're showing inline name
            holder.acquiredInfoContainer.setVisibility(View.GONE);
        } else {
            holder.nameTextView.setPaintFlags(
                    holder.nameTextView.getPaintFlags()
                            & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            holder.acquiredNameTextView.setVisibility(View.GONE);
            holder.acquiredInfoContainer.setVisibility(View.GONE);
        }

        holder.acquiredCheckBox.setOnCheckedChangeListener(
                (btn, isChecked) ->
                        ingredientActions.onIngredientChecked(
                                holder.getBindingAdapterPosition(), isChecked));

        holder.itemView.setOnClickListener(
                v -> ingredientActions.onItemClick(holder.getBindingAdapterPosition()));

        holder.itemView.setOnLongClickListener(v -> {
            ingredientActions.onItemLongClick(holder.getBindingAdapterPosition());
            return true;
        });
    }

    /**
     * Check if current user can interact with the checkbox for this ingredient
     */
    private boolean canUserInteractWithCheckbox(Ingredient ing) {
        if (currentUserUid == null) return false;
        
        // If ingredient is not acquired, anyone can acquire it
        if (!ing.isAcquired()) return true;
        
        // If ingredient is acquired, only the person who acquired it or admin can free it
        if (ing.isAcquired()) {
            return currentUserUid.equals(ing.getAcquiredByUID()) || isOwner;
        }
        
        return false;
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }

    /* ─── Public helper ────────────────────────────────────── */
    public void updateIngredients(List<Ingredient> newList) {
        ingredientList.clear();
        ingredientList.addAll(newList);
        notifyDataSetChanged();
    }

    /* ─── Internals ────────────────────────────────────────── */
    private String formatDate(long ts) {
        return ts > 0 ? dateFormat.format(new Date(ts)) : "N/A";
    }

    /* ─── ViewHolder ───────────────────────────────────────── */
    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, quantityTextView, priceTextView,
                acquiredByTextView, acquiredDateTextView, acquiredNameTextView, createdByTextView;
        CheckBox acquiredCheckBox;
        ConstraintLayout layout;
        LinearLayout acquiredInfoContainer;

        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.ingredientNameTextView);
            quantityTextView = itemView.findViewById(R.id.ingredientQuantityTextView);
            priceTextView = itemView.findViewById(R.id.ingredientPriceTextView);
            acquiredByTextView = itemView.findViewById(R.id.ingredientAcquiredByTextView);
            acquiredDateTextView = itemView.findViewById(R.id.ingredientAcquiredDateTextView);
            acquiredNameTextView = itemView.findViewById(R.id.ingredientAcquiredNameTextView);
            createdByTextView = itemView.findViewById(R.id.ingredientCreatedByTextView);
            acquiredCheckBox = itemView.findViewById(R.id.ingredientAcquiredCheckBox);
            layout = itemView.findViewById(R.id.ingredientItemLayout);
            acquiredInfoContainer = itemView.findViewById(R.id.acquiredInfoContainer);
        }
    }
}
