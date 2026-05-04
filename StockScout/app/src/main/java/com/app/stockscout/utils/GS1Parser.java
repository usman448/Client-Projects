package com.app.stockscout.utils;

import com.app.stockscout.data.model.GS1Data;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for GS1 barcode format.
 * Extracts GTIN, expiry date, lot number from GS1 string.
 *
 * GS1 format example: (01)01234567890123(17)250101(10)ABC123
 *   (01) = GTIN-14 (14 digits)
 *   (17) = Expiry date (YYMMDD)
 *   (10) = Batch/Lot number
 *   (21) = Serial number
 *
 * NOTE: A plain 12/13/14-digit string is a regular UPC/EAN barcode, NOT GS1.
 * GS1 strings always contain Application Identifier (AI) parentheses or the
 * FNC1 character.  We treat a string as GS1 only if it has at least one AI
 * in parentheses form, e.g. "(01)...".
 */
public class GS1Parser {

    // Regex for a single Application Identifier block: (NN)value
    private static final Pattern AI_PATTERN =
            Pattern.compile("\\((\\d{2,4})\\)([^()]+)");

    /**
     * Returns true only if the input looks like a GS1 composite string,
     * i.e. it contains at least one parenthesised Application Identifier.
     * A bare 12/13/14-digit string is treated as a plain barcode alias.
     */
    public static boolean isGS1String(String input) {
        if (input == null) return false;
        return AI_PATTERN.matcher(input).find();
    }

    /**
     * Parse GS1 barcode string and extract embedded data.
     *
     * @param gs1String Raw GS1 barcode string (must contain AI parentheses)
     * @return GS1Data object with parsed fields, or null if not a valid GS1 string
     */
    public static GS1Data parseGS1(String gs1String) {
        if (!isGS1String(gs1String)) {
            return null;   // Plain UPC/EAN — not a GS1 string
        }

        String gtin = null;
        String expiryDate = null;
        String lotNumber = null;
        String serialNumber = null;

        Matcher matcher = AI_PATTERN.matcher(gs1String);
        while (matcher.find()) {
            String ai    = matcher.group(1);
            String value = matcher.group(2);

            switch (ai) {
                case "01": gtin         = value; break;  // GTIN-14
                case "17": expiryDate   = value; break;  // Expiry YYMMDD
                case "10": lotNumber    = value; break;  // Batch/Lot
                case "21": serialNumber = value; break;  // Serial number
            }
        }

        return gtin != null ? new GS1Data(gtin, expiryDate, lotNumber, serialNumber) : null;
    }

    /**
     * Extract GTIN from GS1 string for lookup.
     * Returns null if the input is not a GS1 string (so plain UPC/EAN
     * aliases flow through normal alias matching instead).
     */
    public static String extractGTIN(String gs1String) {
        GS1Data data = parseGS1(gs1String);
        return data != null ? data.getGtin() : null;
    }
}