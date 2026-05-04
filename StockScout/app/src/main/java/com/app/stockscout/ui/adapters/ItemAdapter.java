package com.app.stockscout.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.stockscout.R;
import com.app.stockscout.data.model.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying items list in MainActivity
 *
 * This adapter converts Item objects into views that can be displayed
 * in a RecyclerView. It handles:
 * - Creating new view holders
 * - Binding data to views
 * - Handling click events on items
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    // List of items to display
    private List<Item> items = new ArrayList<>();

    // Click listener for item clicks
    private OnItemClickListener listener;

    // Context for accessing resources
    private Context context;

    /**
     * Constructor for ItemAdapter
     * @param context The activity context
     * @param listener Callback for when an item is clicked
     */
    public ItemAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Update the adapter with new data
     * @param items New list of items to display
     */
    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged(); // Refresh all visible items
    }

    /**
     * Called when RecyclerView needs a new ViewHolder
     * Inflates the layout for a single item row
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout XML file
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called to display data at a specific position
     * Binds item data to the views in the ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the item at this position
        Item item = items.get(position);

        // Set the data to the views
        holder.itemCodeText.setText(item.getItemCode());
        holder.nameText.setText(item.getName());
        holder.quantityText.setText("Quantity: " + item.getQuantity() + " " + item.getUnitOfMeasure());

        // Set click listener on the entire row
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    /**
     * Returns the total number of items in the list
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder class - holds references to views for a single item row
     * This improves performance by avoiding repeated findViewById calls
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemCodeText;    // Displays item code
        TextView nameText;         // Displays item name
        TextView quantityText;     // Displays quantity and unit

        /**
         * Constructor - finds all views in the item layout
         * @param itemView The inflated view for this item
         */
        ViewHolder(View itemView) {
            super(itemView);
            // Find all TextViews in the layout
            itemCodeText = itemView.findViewById(R.id.itemCodeText);
            nameText = itemView.findViewById(R.id.nameText);
            quantityText = itemView.findViewById(R.id.quantityText);
        }
    }

    /**
     * Interface for item click callbacks
     * Implementing classes (like MainActivity) will handle the click
     */
    public interface OnItemClickListener {
        /**
         * Called when an item in the list is clicked
         * @param item The clicked Item object
         */
        void onItemClick(Item item);
    }
}