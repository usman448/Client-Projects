package com.app.stockscout.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.app.stockscout.data.local.AppDatabase;
import com.app.stockscout.data.model.Item;
import com.app.stockscout.data.model.Pick;
import com.app.stockscout.data.remote.ApiService;
import com.app.stockscout.data.remote.RetrofitClient;
import com.app.stockscout.data.remote.SyncManager;
import com.app.stockscout.utils.GS1Parser;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemRepository {

    private AppDatabase database;
    private ApiService apiService;
    private SyncManager syncManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Context context;

    public ItemRepository(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.apiService = RetrofitClient.getApiService();
        this.syncManager = new SyncManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchItemsFromRemote(final DataCallback<List<Item>> callback) {
        apiService.getItems().enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    final List<Item> items = response.body();

                    executorService.execute(() -> {
                        database.itemDao().deleteAllItems();
                        database.itemDao().insertAllItems(items);
                    });

                    mainHandler.post(() -> callback.onSuccess(items));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to fetch: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    public List<Item> getLocalItems() {
        return database.itemDao().getAllItems();
    }

    public Item getItemByCode(String itemCode) {
        return database.itemDao().getItemByCode(itemCode);
    }

    public Item resolveItem(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String cleanInput = input.trim();

        String gtin = GS1Parser.extractGTIN(cleanInput);
        if (gtin != null) {
            Item item = findItemByAlias(gtin);
            if (item != null) return item;
        }

        Item item = database.itemDao().getItemByCode(cleanInput);
        if (item != null) return item;

        item = findItemByAlias(cleanInput);
        return item;
    }

    private Item findItemByAlias(String alias) {
        List<Item> allItems = database.itemDao().getAllItems();
        for (Item item : allItems) {
            if (item.getItemCode().equals(alias)) {
                return item;
            }
            if (item.getAliases() != null && item.getAliases().contains(alias)) {
                return item;
            }
        }
        return null;
    }

    public void pickItem(Item item, final PickCallback callback) {
        if (item.getQuantity() <= 0) {
            mainHandler.post(() -> callback.onError("Quantity is zero"));
            return;
        }

        executorService.execute(() -> {
            try {
                int newQuantity = item.getQuantity() - 1;
                database.itemDao().updateQuantity(item.getItemCode(), newQuantity);
                item.setQuantity(newQuantity);

                Pick pick = new Pick(item.getItemCode(), newQuantity);
                syncManager.queuePick(pick);

                mainHandler.post(() -> callback.onSuccess(item));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Pick failed: " + e.getMessage()));
            }
        });
    }

    // ADD THIS METHOD - For manual sync
    public void syncPendingPicks() {
        if (syncManager != null) {
            syncManager.syncPendingPicks();
        }
    }

    // ADD THIS METHOD - Get pending sync count
    public int getPendingSyncCount() {
        return database.pickDao().getPendingSyncCount();
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public interface PickCallback {
        void onSuccess(Item updatedItem);
        void onError(String error);
    }
}