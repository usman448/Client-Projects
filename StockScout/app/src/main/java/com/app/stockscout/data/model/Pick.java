package com.app.stockscout.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

/**
 * Pick record representing a picking operation
 * Tracks picks that need to be synced with remote server
 */
@Entity(tableName = "picks")
public class Pick {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String itemCode;
    private int newQuantity;
    private long timestamp;
    private boolean synced;
    private int retryCount;

    // Constructor
    public Pick(String itemCode, int newQuantity) {
        this.itemCode = itemCode;
        this.newQuantity = newQuantity;
        this.timestamp = new Date().getTime();
        this.synced = false;
        this.retryCount = 0;
    }

    // Required empty constructor for Room
    public Pick() {
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public int getNewQuantity() {
        return newQuantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSynced() {
        return synced;
    }

    public int getRetryCount() {
        return retryCount;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public void setNewQuantity(int newQuantity) {
        this.newQuantity = newQuantity;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}