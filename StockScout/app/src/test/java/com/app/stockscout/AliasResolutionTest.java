package com.app.stockscout;

import com.app.stockscout.data.model.Item;
import com.app.stockscout.utils.GS1Parser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for alias resolution logic
 * Tests all required matching scenarios:
 * 1. UPC-A alias matching
 * 2. EAN-13 alias matching
 * 3. GS1 string with embedded GTIN
 * 4. Direct item code match
 * 5. Not found case
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AliasResolutionTest {

    private TestRepository testRepository;
    private Item testItem;

    @Before
    public void setUp() {
        testRepository = new TestRepository();

        // Create test item with various alias types
        List<String> aliases = Arrays.asList(
                "123456789012",      // UPC-A (12 digits)
                "5901234123457",     // EAN-13 (13 digits)
                "01234567890123",    // Alternate alias
                "SupplierAlias-001"  // Text alias
        );

        testItem = new Item(
                "WGT-A",                           // Item code
                "Test Product",                    // Name
                "each",                            // Unit
                10,                                // Quantity
                aliases                            // Aliases list
        );

        testRepository.addItem(testItem);
    }

    /**
     * Test 1: Match by UPC-A alias
     */
    @Test
    public void testMatchByUPCAlias() {
        Item result = testRepository.resolveItem("123456789012");
        assertNotNull("Should find item by UPC-A alias", result);
        assertEquals("Should match correct item", "WGT-A", result.getItemCode());
    }

    /**
     * Test 2: Match by EAN-13 alias
     */
    @Test
    public void testMatchByEAN13Alias() {
        Item result = testRepository.resolveItem("5901234123457");
        assertNotNull("Should find item by EAN-13 alias", result);
        assertEquals("Should match correct item", "WGT-A", result.getItemCode());
    }

    /**
     * Test 3: Match by GS1 barcode with embedded GTIN
     * GS1 format: (01)01234567890123(17)250101(10)ABC123
     * GTIN extracted: 01234567890123
     */
    @Test
    public void testMatchByGS1Barcode() {
        // GS1 barcode containing GTIN that matches an alias
        String gs1Barcode = "(01)01234567890123(17)250101(10)TESTLOT";
        Item result = testRepository.resolveItem(gs1Barcode);
        assertNotNull("Should find item by GS1 barcode GTIN", result);
        assertEquals("Should match correct item", "WGT-A", result.getItemCode());
    }

    /**
     * Test 4: Match by direct item code
     */
    @Test
    public void testMatchByItemCode() {
        Item result = testRepository.resolveItem("WGT-A");
        assertNotNull("Should find item by item code", result);
        assertEquals("Should match correct item", "WGT-A", result.getItemCode());
    }

    /**
     * Test 5: Match by text alias
     */
    @Test
    public void testMatchByTextAlias() {
        Item result = testRepository.resolveItem("SupplierAlias-001");
        assertNotNull("Should find item by text alias", result);
        assertEquals("Should match correct item", "WGT-A", result.getItemCode());
    }

    /**
     * Test 6: Item not found
     */
    @Test
    public void testItemNotFound() {
        Item result = testRepository.resolveItem("NonExistentItem");
        assertNull("Should return null for non-existent item", result);
    }

    /**
     * Test 7: Empty input
     */
    @Test
    public void testEmptyInput() {
        Item result = testRepository.resolveItem("");
        assertNull("Should return null for empty input", result);

        result = testRepository.resolveItem(null);
        assertNull("Should return null for null input", result);
    }

    /**
     * Test Repository implementation for testing
     * This is a simplified version of ItemRepository for testing purposes
     */
    private class TestRepository {
        private List<Item> items = new ArrayList<>();

        void addItem(Item item) {
            items.add(item);
        }

        Item resolveItem(String input) {
            if (input == null || input.isEmpty()) {
                return null;
            }

            String cleanInput = input.trim();

            // Check GS1 extraction
            String gtin = GS1Parser.extractGTIN(cleanInput);
            if (gtin != null) {
                for (Item item : items) {
                    if (item.getAliases() != null && item.getAliases().contains(gtin)) {
                        return item;
                    }
                    if (item.getItemCode().equals(gtin)) {
                        return item;
                    }
                }
            }

            // Check all items
            for (Item item : items) {
                if (item.getItemCode().equals(cleanInput)) {
                    return item;
                }
                if (item.getAliases() != null && item.getAliases().contains(cleanInput)) {
                    return item;
                }
            }
            return null;
        }
    }
}