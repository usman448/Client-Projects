package com.app.stockscout.data.model;

/**
 * Request model for syncing picks to remote server
 * This matches the expected JSON format of the mock API
 */
public class SyncPickRequest {
    private String itemCode;
    private int newQuantity;
    private long timestamp;

    public SyncPickRequest(String itemCode, int newQuantity, long timestamp) {
        this.itemCode = itemCode;
        this.newQuantity = newQuantity;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public int getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(int newQuantity) {
        this.newQuantity = newQuantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}