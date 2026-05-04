package com.app.stockscout.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

/**
 * Barcode scanner using Google ML Kit
 *
 * Why ML Kit?
 * - Free and easy to use
 * - Supports multiple barcode formats (UPC, EAN, GS1, QR codes)
 * - Works offline
 * - Good performance on device
 * - Regular updates from Google
 */
public class BarcodeScanner {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    /**
     * Configure scanner for supported formats
     */
    private static BarcodeScannerOptions getOptions() {
        return new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_QR_CODE
                )
                .build();
    }

    /**
     * Check if camera permission is granted
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Process scanned barcode image
     * This is a wrapper - actual scanning happens in the activity with camera preview
     */
    public static void scanImage(InputImage image, BarcodeCallback callback) {
        com.google.mlkit.vision.barcode.BarcodeScanner scanner =
                BarcodeScanning.getClient(getOptions());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes != null && !barcodes.isEmpty()) {
                        String rawValue = barcodes.get(0).getRawValue();
                        callback.onSuccess(rawValue);
                    } else {
                        callback.onError("No barcode found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface BarcodeCallback {
        void onSuccess(String barcode);
        void onError(String error);
    }
}