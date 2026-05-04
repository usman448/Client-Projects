package com.app.stockscout.data.remote;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.widget.Toast;
import com.app.stockscout.data.local.AppDatabase;
import com.app.stockscout.data.model.Pick;
import com.app.stockscout.data.model.SyncPickRequest;
import com.app.stockscout.utils.NetworkUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final int MAX_RETRIES = 3;

    private Context context;
    private AppDatabase database;
    private ApiService apiService;
    private ExecutorService executorService;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;

    public SyncManager(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.apiService = RetrofitClient.getApiService();
        this.executorService = Executors.newSingleThreadExecutor();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Register network callback
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            networkCallback = new NetworkCallback();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            // When internet becomes available, sync immediately
            android.util.Log.d("SyncManager", "Internet connected! Syncing pending picks...");
            syncPendingPicks();
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            android.util.Log.d("SyncManager", "Internet lost");
        }
    }

    /**
     * Sync all pending picks to server
     */
    public void syncPendingPicks() {
        executorService.execute(() -> {
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(context)) {
                android.util.Log.d("SyncManager", "No internet connection. Sync postponed.");
                return;
            }

            // Get all unsynced picks
            List<Pick> pendingPicks = database.pickDao().getUnsyncedPicks();

            if (pendingPicks.isEmpty()) {
                android.util.Log.d("SyncManager", "No pending picks to sync");
                return;
            }

            android.util.Log.d("SyncManager", "Found " + pendingPicks.size() + " pending picks to sync");

            // Sync each pick
            for (Pick pick : pendingPicks) {
                // Skip picks that have exceeded the retry limit
                if (pick.getRetryCount() >= MAX_RETRIES) {
                    android.util.Log.e("SyncManager",
                            "Pick " + pick.getItemCode() + " exceeded max retries ("
                                    + MAX_RETRIES + "). Marking as permanently failed.");
                    // Mark as permanently failed so it no longer pollutes the pending queue
                    executorService.execute(() -> {
                        pick.setSynced(true);          // Remove from pending queue
                        pick.setRetryCount(-1);        // Sentinel: -1 = permanently failed
                        database.pickDao().updatePick(pick);
                    });
                    continue;
                }

                syncSinglePick(pick);
            }
        });
    }

    /**
     * Sync a single pick record
     */
    private void syncSinglePick(final Pick pick) {
        SyncPickRequest request = new SyncPickRequest(
                pick.getItemCode(),
                pick.getNewQuantity(),
                pick.getTimestamp()
        );

        android.util.Log.d("SyncManager", "Syncing pick: " + pick.getItemCode() + ", Quantity: " + pick.getNewQuantity());

        apiService.recordPick(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                executorService.execute(() -> {
                    if (response.isSuccessful()) {
                        // Success - mark as synced
                        pick.setSynced(true);
                        database.pickDao().updatePick(pick);
                        android.util.Log.d("SyncManager", "Successfully synced: " + pick.getItemCode());
                    } else {
                        // Failed - increment retry count
                        pick.setRetryCount(pick.getRetryCount() + 1);
                        database.pickDao().updatePick(pick);
                        android.util.Log.e("SyncManager", "Sync failed for " + pick.getItemCode() +
                                ". Retry count: " + pick.getRetryCount() + ", Response code: " + response.code());
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                executorService.execute(() -> {
                    // Network failure - increment retry count
                    pick.setRetryCount(pick.getRetryCount() + 1);
                    database.pickDao().updatePick(pick);
                    android.util.Log.e("SyncManager", "Network error for " + pick.getItemCode() +
                            ". Retry count: " + pick.getRetryCount() + ", Error: " + t.getMessage());
                });
            }
        });
    }

    /**
     * Queue a pick for sync
     */
    public void queuePick(Pick pick) {
        executorService.execute(() -> {
            // Insert pick into database
            database.pickDao().insertPick(pick);
            android.util.Log.d("SyncManager", "Pick queued: " + pick.getItemCode());

            // Try to sync immediately
            syncPendingPicks();
        });
    }

    /**
     * Force sync from UI (called when user clicks sync button)
     */
    public void forceSync() {
        android.util.Log.d("SyncManager", "Force sync requested by user");
        syncPendingPicks();
    }

    /**
     * Get count of pending picks
     */
    public int getPendingCount() {
        return database.pickDao().getPendingSyncCount();
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (networkCallback != null && connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}