package com.example.finalprojectyali.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Extras.Utils; // Assuming Utils.checkInterfaceValid is still relevant
import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The GroupAdapter class is an adapter for the RecyclerView widget that handles a list of Group objects.
 * It provides a view for each Group item in the list, and it binds data from the Group objects to the corresponding views.
 * It also listens to user interactions, such as clicking or long pressing an item in the list, and it triggers the appropriate actions
 * via the RecyclerViewFunctionalities interface.
 *
 * @author (Adapted from original by Ofek Almog)
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    /**
     * An instance of the RecyclerViewFunctionalities interface that provides callbacks for the adapter's actions.
     */
    private final RecyclerViewFunctionalities recyclerViewFunctionalities;

    /**
     * The context of the adapter, used for inflating the item views.
     */
    private final Context context;

    /**
     * The list of Group objects that this adapter is handling.
     */
    private final List<Group> groupList;

    /**
     * Date formatter.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    /**
     * Creates a new instance of the GroupAdapter class.
     *
     * @param context                     The context of the adapter.
     * @param groupList                   The list of Group objects to be handled by the adapter.
     * @param recyclerViewFunctionalities An instance of the RecyclerViewFunctionalities interface that provides callbacks for the adapter's actions.
     */
    public GroupAdapter(Context context, List<Group> groupList, RecyclerViewFunctionalities recyclerViewFunctionalities) {
        this.context = context;
        this.groupList = groupList;
        this.recyclerViewFunctionalities = recyclerViewFunctionalities;
    }

    /**
     * Creates a new GroupViewHolder instance by inflating the item view from the specified layout.
     *
     * @param parent   The parent ViewGroup in which the item view will be contained.
     * @param viewType The type of the view to be inflated.
     * @return The new GroupViewHolder instance.
     */
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the new group_item layout
        View view = inflater.inflate(R.layout.group_item, parent, false); // Pass parent and false
        return new GroupViewHolder(view, recyclerViewFunctionalities);
    }

    /**
     * Binds the data from the Group object at the specified position in the list to the corresponding views in the holder.
     *
     * @param holder   The GroupViewHolder that holds the views to be bound.
     * @param position The position of the Group object in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.groupNameTextView.setText(group.getName());
        holder.groupDescriptionTextView.setText(group.getDescription());
        holder.groupMembersCountTextView.setText("Members: " + group.getMembersCount());
        holder.groupCreationDateTextView.setText(formatDate(group.getCreationDate()));
    }

    /**
     * Formats the date object into a readable string.
     *
     * @param date The date to format.
     * @return Formatted date string or "N/A" if date is null.
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        try {
            return dateFormat.format(date);
        } catch (Exception e) {
            // Log error or handle gracefully
            return "Invalid Date";
        }
    }


    /**
     * Gets the number of items in the list.
     *
     * @return The number of Group objects in the list.
     */
    @Override
    public int getItemCount() {
        return groupList != null ? groupList.size() : 0; // Add null check
    }

    /**
     * Returns the list of Group objects handled by this adapter.
     *
     * @return The list of Group objects.
     */
    public List<Group> getGroupList() {
        return this.groupList;
    }

    /**
     * The GroupViewHolder class represents a view holder for the RecyclerView items in the GroupAdapter.
     * It holds references to the views in the item layout, and it sets listeners for user interactions.
     */
    class GroupViewHolder extends RecyclerView.ViewHolder {

        /**
         * The TextViews that display the properties of the group.
         */
        TextView groupNameTextView, groupDescriptionTextView, groupMembersCountTextView, groupCreationDateTextView;
        ImageView groupCode;
        /**
         * The ConstraintLayout field represents the layout of the group item.
         */
        ConstraintLayout constraintLayout;

        /**
         * Constructs a new GroupViewHolder object.
         *
         * @param itemView                    The View object corresponding to the group item layout.
         * @param recyclerViewFunctionalities The interface implementing the RecyclerView functionalities.
         */
        public GroupViewHolder(@NonNull View itemView, RecyclerViewFunctionalities recyclerViewFunctionalities) {
            super(itemView);
            // Find all views by id in group_item layout.
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            groupDescriptionTextView = itemView.findViewById(R.id.groupDescriptionTextView);
            groupMembersCountTextView = itemView.findViewById(R.id.groupMembersCountTextView);
            groupCreationDateTextView = itemView.findViewById(R.id.groupCreationDateTextView);
            groupCode = itemView.findViewById(R.id.addLinkImageView); // Assuming you have a TextView for group code
            constraintLayout = itemView.findViewById(R.id.groupItemLayout); // ID from the ConstraintLayout in group_item.xml

            groupCode.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (recyclerViewFunctionalities != null && pos != RecyclerView.NO_POSITION) {
                    setClipboard(itemView.getContext(), groupList.get(pos).getJoinCode()); // 4 Letter code
                }
            });

            // Set listener for item click (using the interface)
            itemView.setOnClickListener(view -> {
                int pos = getBindingAdapterPosition();
                // Use your Utils check or directly call if always valid
                if (recyclerViewFunctionalities != null && pos != RecyclerView.NO_POSITION) {
                    // Optionally add Utils.checkInterfaceValid(recyclerViewFunctionalities, pos) if needed
                    recyclerViewFunctionalities.onItemClick(pos);
                }
            });

            // Set listener for item long click (using the interface)
            itemView.setOnLongClickListener(view -> {
                int pos = getBindingAdapterPosition();
                if (recyclerViewFunctionalities != null && pos != RecyclerView.NO_POSITION) {
                    // Optionally add Utils.checkInterfaceValid(recyclerViewFunctionalities, pos) if needed
                    recyclerViewFunctionalities.onItemLongClick(pos);
                    return true; // Indicate event was consumed
                }
                return false; // Indicate event was not consumed
            });
        }

        private void setClipboard(Context context, String text) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                clipboard.setPrimaryClip(clip);
            }
        }
    }
}