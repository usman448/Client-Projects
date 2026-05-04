package com.app.stockscout.ui;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.app.stockscout.R;
import com.app.stockscout.data.ItemRepository;
import com.app.stockscout.data.model.Item;
import com.app.stockscout.utils.NetworkUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemDetailActivity extends AppCompatActivity {

    private TextView itemCodeText, nameText, quantityText, unitText;
    private TextView aliasesText, syncStatusText;
    private Button pickButton, syncButton;
    private ProgressBar progressBar;
    private CardView detailCard;

    private ItemRepository repository;
    private Item currentItem;
    private String itemCode;
    private ExecutorService executorService;
    private Handler handler;

    // Auto-refresh sync status every 3 seconds while screen is visible
    private static final long SYNC_POLL_INTERVAL_MS = 3000;
    private Runnable syncPoller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler();

        initializeViews();
        setupRepository();
        setupListeners();

        itemCode = getIntent().getStringExtra("item_code");
        if (itemCode == null) { finish(); return; }

        loadItemData();
    }

    private void initializeViews() {
        itemCodeText  = findViewById(R.id.itemCodeText);
        nameText      = findViewById(R.id.nameText);
        quantityText  = findViewById(R.id.quantityText);
        unitText      = findViewById(R.id.unitText);
        aliasesText   = findViewById(R.id.aliasesText);
        syncStatusText = findViewById(R.id.syncStatusText);
        pickButton    = findViewById(R.id.pickButton);
        syncButton    = findViewById(R.id.syncButton);
        progressBar   = findViewById(R.id.progressBar);
        detailCard    = findViewById(R.id.detailCard);
    }

    private void setupRepository() {
        repository = new ItemRepository(this);
    }

    private void loadItemData() {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            Item item = repository.getItemByCode(itemCode);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (item != null) {
                    currentItem = item;
                    displayItemDetails();
                } else {
                    Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayItemDetails() {
        if (currentItem == null) return;

        itemCodeText.setText(currentItem.getItemCode());
        nameText.setText(currentItem.getName());
        quantityText.setText(String.valueOf(currentItem.getQuantity()));
        unitText.setText(currentItem.getUnitOfMeasure());

        if (currentItem.getAliases() != null && !currentItem.getAliases().isEmpty()) {
            aliasesText.setText(TextUtils.join("\n• ", currentItem.getAliases()));
        } else {
            aliasesText.setText("No aliases");
        }

        updatePickButtonState();
        refreshSyncStatus();
    }

    private void setupListeners() {
        pickButton.setOnClickListener(v -> performPick());
        syncButton.setOnClickListener(v -> manualSync());
    }

    private void performPick() {
        if (currentItem == null || currentItem.getQuantity() <= 0) {
            Toast.makeText(this, "Cannot pick – item out of stock", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        pickButton.setEnabled(false);

        repository.pickItem(currentItem, new ItemRepository.PickCallback() {
            @Override
            public void onSuccess(Item updatedItem) {
                currentItem = updatedItem;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    quantityText.setText(String.valueOf(currentItem.getQuantity()));
                    updatePickButtonState();

                    // Show immediate feedback then poll for sync result
                    syncStatusText.setText("⏳ Pick recorded – syncing…");
                    syncStatusText.setVisibility(View.VISIBLE);
                    handler.postDelayed(() -> refreshSyncStatus(), 2500);

                    Toast.makeText(ItemDetailActivity.this,
                            "✅ Pick recorded!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    pickButton.setEnabled(true);
                    Toast.makeText(ItemDetailActivity.this,
                            "Pick failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void manualSync() {
        syncStatusText.setText("🔄 Syncing…");
        syncStatusText.setVisibility(View.VISIBLE);
        syncButton.setEnabled(false);

        repository.syncPendingPicks();

        // Re-check status after allowing async work to complete
        handler.postDelayed(() -> {
            syncButton.setEnabled(true);
            refreshSyncStatus();
        }, 3000);
    }

    /**
     * Refreshes the sync-status chip with current pending count + connectivity.
     * Called after pick, after manual sync, and periodically while screen is shown.
     */
    private void refreshSyncStatus() {
        executorService.execute(() -> {
            int pending = repository.getPendingSyncCount();
            boolean online = NetworkUtils.isNetworkAvailable(this);

            runOnUiThread(() -> {
                if (pending > 0) {
                    if (online) {
                        syncStatusText.setText("🔄 Syncing " + pending + " pending pick(s)…");
                    } else {
                        syncStatusText.setText("📴 No connection – " + pending + " pick(s) queued");
                    }
                    syncStatusText.setVisibility(View.VISIBLE);
                    syncButton.setVisibility(View.VISIBLE);
                    syncButton.setText(online ? "Sync Now" : "Will retry when online");
                } else {
                    syncStatusText.setText("✅ All synced");
                    syncStatusText.setVisibility(View.VISIBLE);
                    syncButton.setVisibility(View.GONE);
                    // Auto-hide after 3 s
                    handler.postDelayed(() -> syncStatusText.setVisibility(View.GONE), 3000);
                }
            });
        });
    }

    private void updatePickButtonState() {
        if (currentItem == null) return;
        if (currentItem.getQuantity() <= 0) {
            pickButton.setEnabled(false);
            pickButton.setText("Out of Stock");
        } else {
            pickButton.setEnabled(true);
            pickButton.setText("Pick Item  (−1)");
        }
    }

    /* Start polling sync status while this screen is visible */
    @Override
    protected void onResume() {
        super.onResume();
        syncPoller = new Runnable() {
            @Override public void run() {
                refreshSyncStatus();
                handler.postDelayed(this, SYNC_POLL_INTERVAL_MS);
            }
        };
        handler.postDelayed(syncPoller, SYNC_POLL_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (syncPoller != null) handler.removeCallbacks(syncPoller);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}