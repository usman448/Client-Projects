package com.app.stockscout;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.app.stockscout.service.SyncWorker;
import com.app.stockscout.utils.DataInitializer;
import java.util.concurrent.TimeUnit;


public class StockScoutApplication extends Application {

    // WorkManager job tag — used to avoid duplicate scheduling
    private static final String SYNC_WORK_TAG = "stockscout_sync_picks";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize sample data in background (NOT on main thread)
        DataInitializer.initializeData(this);

        // Schedule periodic background sync using WorkManager.
        // WorkManager guarantees execution even if the app is killed:
        //   - Runs every 15 minutes (minimum allowed by WorkManager).
        //   - Requires an active internet connection before firing.
        //   - KEEP_EXISTING avoids re-scheduling if already queued.
        schedulePendingPicksSync();
    }

    private void schedulePendingPicksSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag(SYNC_WORK_TAG)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SYNC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,   // Don't restart the timer if already scheduled
                syncRequest
        );
    }
}