package com.app.stockscout.data.model;

/**
 * GS1 parsed data structure
 * Extracts relevant information from GS1 barcode format
 * GS1 format: (01)GTIN(17)EXPIRY(10)LOT_NUMBER
 */
public class GS1Data {
    private String gtin;        // Global Trade Item Number (item identifier)
    private String expiryDate;  // Expiration date
    private String lotNumber;   // Batch/Lot number
    private String serialNumber; // Serial number if present

    public GS1Data(String gtin, String expiryDate, String lotNumber, String serialNumber) {
        this.gtin = gtin;
        this.expiryDate = expiryDate;
        this.lotNumber = lotNumber;
        this.serialNumber = serialNumber;
    }

    // Getters
    public String getGtin() { return gtin; }
    public String getExpiryDate() { return expiryDate; }
    public String getLotNumber() { return lotNumber; }
    public String getSerialNumber() { return serialNumber; }
}
