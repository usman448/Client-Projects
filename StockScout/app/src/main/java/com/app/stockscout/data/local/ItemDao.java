package com.app.stockscout.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.app.stockscout.data.model.Item;
import java.util.List;

/**
 * Data Access Object for Item operations
 * Handles all database operations for items
 */
@Dao
public interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItem(Item item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllItems(List<Item> items);

    @Update
    void updateItem(Item item);

    @Query("SELECT * FROM items")
    List<Item> getAllItems();

    @Query("SELECT * FROM items WHERE itemCode = :itemCode")
    Item getItemByCode(String itemCode);

    @Query("UPDATE items SET quantity = :newQuantity WHERE itemCode = :itemCode")
    void updateQuantity(String itemCode, int newQuantity);

    @Query("DELETE FROM items")
    void deleteAllItems();
}