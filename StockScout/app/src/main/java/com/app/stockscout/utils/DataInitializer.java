package com.app.stockscout.utils;

import android.content.Context;
import com.app.stockscout.data.local.AppDatabase;
import com.app.stockscout.data.model.Item;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataInitializer {

    private static boolean isInitialized = false;

    public static void initializeData(Context context) {
        // Check if already initialized to avoid duplicate calls
        if (isInitialized) {
            return;
        }

        // Run database operations on background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);

                // Check if data already exists
                if (database.itemDao().getAllItems().isEmpty()) {
                    // Insert sample data
                    List<Item> sampleItems = getSampleItems();
                    database.itemDao().insertAllItems(sampleItems);
                    isInitialized = true;

                    // Log success (optional)
                    android.util.Log.d("DataInitializer", "Sample data inserted successfully. Count: " + sampleItems.size());
                } else {
                    android.util.Log.d("DataInitializer", "Data already exists. Count: " + database.itemDao().getAllItems().size());
                    isInitialized = true;
                }
            } catch (Exception e) {
                android.util.Log.e("DataInitializer", "Error initializing data: " + e.getMessage());
            }
        });
        executor.shutdown();
    }

    private static List<Item> getSampleItems() {
        return Arrays.asList(
                new Item("WGT-A", "Wireless Headphones", "each", 25,
                        Arrays.asList("123456789012", "5901234123457", "WH-1000XM4")),

                new Item("BATT-123", "Rechargeable Battery", "each", 50,
                        Arrays.asList("987654321098", "BAT-LG-18650")),

                new Item("CBL-789", "USB-C Cable", "each", 100,
                        Arrays.asList("456789012345", "USB-C-2M")),

                new Item("CHG-001", "Fast Charger", "each", 30,
                        Arrays.asList("111222333444", "QC-3.0-18W")),

                new Item("SPK-456", "Bluetooth Speaker", "each", 15,
                        Arrays.asList("555666777888", "JBL-FLIP-6")),

                new Item("MIC-789", "USB Microphone", "each", 20,
                        Arrays.asList("999000111222", "BLUE-YETI")),

                new Item("MON-001", "Monitor Stand", "each", 40,
                        Arrays.asList("333444555666", "STAND-001")),

                new Item("KEY-ABC", "Mechanical Keyboard", "each", 12,
                        Arrays.asList("777888999000", "KEYCHRON-K2")),

                new Item("MOU-123", "Wireless Mouse", "each", 60,
                        Arrays.asList("123123123123", "LOGITECH-MX")),

                new Item("PAD-456", "Mouse Pad", "each", 80,
                        Arrays.asList("456456456456", "STEELSERIES-QCK"))
        );
    }
}