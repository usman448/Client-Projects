package com.app.stockscout.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;
import com.app.stockscout.data.local.Converters;
import java.util.List;

/**
 * Item model representing a product in inventory
 * This is the main data entity for items
 */
@Entity(tableName = "items")
@TypeConverters(Converters.class)
public class Item {
    @PrimaryKey
    @NonNull  // This is required by Room - primary keys cannot be NULL in SQLite
    private String itemCode;      // Internal item code e.g., "WGT-A"

    private String name;           // Product name
    private String unitOfMeasure;  // e.g., "each", "kg", "box"
    private int quantity;          // Current on-hand quantity
    private List<String> aliases;  // List of aliases (UPC, EAN, etc.)

    // Constructor
    public Item(@NonNull String itemCode, String name, String unitOfMeasure,
                int quantity, List<String> aliases) {
        this.itemCode = itemCode;
        this.name = name;
        this.unitOfMeasure = unitOfMeasure;
        this.quantity = quantity;
        this.aliases = aliases;
    }

    // Getters and Setters
    @NonNull
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(@NonNull String itemCode) {
        this.itemCode = itemCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
}