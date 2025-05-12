package com.example.finalprojectyali.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
 * Adapter for displaying a list of Ingredient objects in a RecyclerView.
 */
public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder> {

    public interface IngredientActions {

        void onIngredientChecked(int position, boolean isChecked);

        void onItemClick(int position);

        void onItemLongClick(int position);
    }

    private final List<Ingredient> ingredientList;
    private final Context context;
    private final IngredientActions ingredientActions;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public IngredientsAdapter(Context context, List<Ingredient> ingredientList, IngredientActions ingredientActions) {
        this.context = context;
        this.ingredientList = ingredientList;
        this.ingredientActions = ingredientActions;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.ingredient_item, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Ingredient ingredient = ingredientList.get(position);
        holder.nameTextView.setText(ingredient.getName());
        if (ingredient.isAcquired()) {
            holder.nameTextView.setPaintFlags(holder.nameTextView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.nameTextView.setPaintFlags(holder.nameTextView.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }
        holder.quantityTextView.setText(String.valueOf(ingredient.getQuantity()));
        holder.priceTextView.setText(context.getString(R.string.ingredient_price_format, ingredient.getPrice()));

        holder.acquiredCheckBox.setChecked(ingredient.isAcquired());

        // Show/hide acquired info
        if (ingredient.isAcquired()) {
            holder.acquiredByTextView.setVisibility(View.VISIBLE);
            holder.acquiredDateTextView.setVisibility(View.VISIBLE);
                        
            holder.acquiredByTextView.setText(context.getString(R.string.ingredient_acquired_by, ingredient.getAcquiredBy() != null ? ingredient.getAcquiredBy() : "Unknown"));
            holder.acquiredDateTextView.setText(formatDate(ingredient.getAcquiredAt()));
        } else {
            holder.acquiredByTextView.setVisibility(View.GONE);
            holder.acquiredDateTextView.setVisibility(View.GONE);
        }

        // Checkbox listener
        holder.acquiredCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ingredientActions != null) {
                ingredientActions.onIngredientChecked(holder.getBindingAdapterPosition(), isChecked);
            }
        });

        // Item click listeners
        holder.itemView.setOnClickListener(v -> {
            if (ingredientActions != null) {
                ingredientActions.onItemClick(holder.getBindingAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (ingredientActions != null) {
                ingredientActions.onItemLongClick(holder.getBindingAdapterPosition());
                return true;
            }
            return false;
        });
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "N/A";
        }
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return "Invalid Date";
        }
    }

    @Override
    public int getItemCount() {
        return ingredientList != null ? ingredientList.size() : 0;
    }

    public List<Ingredient> getIngredientList() {
        return ingredientList;
    }

    class IngredientViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, quantityTextView, priceTextView, acquiredByTextView, acquiredDateTextView;
        CheckBox acquiredCheckBox;
        ConstraintLayout layout;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.ingredientNameTextView);
            quantityTextView = itemView.findViewById(R.id.ingredientQuantityTextView);
            priceTextView = itemView.findViewById(R.id.ingredientPriceTextView);
            acquiredByTextView = itemView.findViewById(R.id.ingredientAcquiredByTextView);
            acquiredDateTextView = itemView.findViewById(R.id.ingredientAcquiredDateTextView);
            acquiredCheckBox = itemView.findViewById(R.id.ingredientAcquiredCheckBox);
            layout = itemView.findViewById(R.id.ingredientItemLayout);


        }
    }
}
