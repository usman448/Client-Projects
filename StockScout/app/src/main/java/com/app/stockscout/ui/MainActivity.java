package com.app.stockscout.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.stockscout.R;
import com.app.stockscout.data.ItemRepository;
import com.app.stockscout.data.model.Item;
import com.app.stockscout.ui.adapters.ItemAdapter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    private EditText searchInput;
    private Button searchButton, scanButton, refreshButton;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;          // ✅ FIX: TextView → View (XML mein LinearLayout hai)
    private TextView syncBadge;      // ✅ FIX: XML mein add kiya
    private TextView apiStatusBar;   // ✅ FIX: XML mein already tha, Java mein add kiya

    private ItemRepository repository;
    private ItemAdapter adapter;
    private List<Item> items;
    private ExecutorService executorService;

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int BARCODE_SCAN_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

        initializeViews();
        setupRecyclerView();
        setupRepository();
        setupListeners();

        // ✅ REQUIREMENT 1: Pull from remote on launch
        fetchItemsFromRemote();
    }

    private void initializeViews() {
        searchInput   = findViewById(R.id.searchInput);
        searchButton  = findViewById(R.id.searchButton);   // ✅ now in XML
        scanButton    = findViewById(R.id.scanButton);
        refreshButton = findViewById(R.id.refreshButton);  // ✅ now wired
        recyclerView  = findViewById(R.id.recyclerView);
        progressBar   = findViewById(R.id.progressBar);
        emptyView     = findViewById(R.id.emptyView);      // ✅ View type
        syncBadge     = findViewById(R.id.syncBadge);      // ✅ now in XML
        apiStatusBar  = findViewById(R.id.apiStatusBar);   // ✅ now initialized
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupRepository() {
        repository = new ItemRepository(this);
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> performSearch());
        scanButton.setOnClickListener(v -> checkCameraPermissionAndScan());

        // ✅ FIX: refreshButton listener add kiya
        refreshButton.setOnClickListener(v -> fetchItemsFromRemote());

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) showAllItems();
            }
        });
    }

    /**
     * REQUIREMENT 1: Fetch from remote, store locally, fall back to local cache on failure.
     */
    private void fetchItemsFromRemote() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // ✅ FIX: apiStatusBar show karo jab fetch start ho
        apiStatusBar.setText("🔄 Loading items from server...");
        apiStatusBar.setVisibility(View.VISIBLE);

        repository.fetchItemsFromRemote(new ItemRepository.DataCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> data) {
                items = data;
                showAllItems();
                progressBar.setVisibility(View.GONE);
                // ✅ FIX: success par status bar hide karo
                apiStatusBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Items updated from server", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                // ✅ FIX: error par offline message dikhao
                apiStatusBar.setText("⚠️ Offline – showing cached data");
                // status bar thodi der baad hide karo
                apiStatusBar.postDelayed(() -> apiStatusBar.setVisibility(View.GONE), 4000);
                loadFromLocal();
                Toast.makeText(MainActivity.this, "Offline – showing cached data", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadFromLocal() {
        executorService.execute(() -> {
            final List<Item> localItems = repository.getLocalItems();
            runOnUiThread(() -> {
                items = localItems;
                showAllItems();
            });
        });
    }

    private void performSearch() {
        String query = searchInput.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            showAllItems();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            final Item foundItem = repository.resolveItem(query);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (foundItem != null) {
                    openItemDetail(foundItem);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Item not found: " + query, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showAllItems() {
        if (items == null || items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setItems(items);
        }
        updateSyncBadge();
    }

    /** Show how many picks are still pending sync */
    private void updateSyncBadge() {
        executorService.execute(() -> {
            int pending = repository.getPendingSyncCount();
            runOnUiThread(() -> {
                if (pending > 0) {
                    syncBadge.setText("⏳ " + pending + " pending sync");
                    syncBadge.setVisibility(View.VISIBLE);
                } else {
                    syncBadge.setVisibility(View.GONE);
                }
            });
        });
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startBarcodeScanner();
        }
    }

    private void startBarcodeScanner() {
        Intent intent = new Intent(this, BarcodeScanActivity.class);
        startActivityForResult(intent, BARCODE_SCAN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BARCODE_SCAN_REQUEST && resultCode == RESULT_OK && data != null) {
            String barcode = data.getStringExtra("barcode");
            if (barcode != null && !barcode.isEmpty()) {
                searchInput.setText(barcode);
                performSearch();
            } else {
                Toast.makeText(this, "No barcode detected. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh quantities after returning from detail screen
        loadFromLocal();
        updateSyncBadge();
    }

    @Override
    public void onItemClick(Item item) {
        openItemDetail(item);
    }

    private void openItemDetail(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_code", item.getItemCode());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner();
            } else {
                Toast.makeText(this, "Camera permission is required for scanning",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }
}