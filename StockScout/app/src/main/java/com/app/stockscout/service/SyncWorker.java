package com.app.stockscout.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.app.stockscout.data.remote.SyncManager;

/**
 * WorkManager worker for background sync
 * Automatically retries sync when network is available
 *
 * This worker runs in the background and syncs pending picks
 * to the remote server when network connectivity is available.
 */
public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Create SyncManager instance
            SyncManager syncManager = new SyncManager(getApplicationContext());

            // Sync all pending picks
            syncManager.syncPendingPicks();

            // Return success - work is complete
            return Result.success();
        } catch (Exception e) {
            // Log the error (you can add proper logging here)
            e.printStackTrace();

            // Retry on failure - WorkManager will retry automatically
            return Result.retry();
        }
    }
}