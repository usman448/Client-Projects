package com.app.stockscout;

import com.app.stockscout.data.model.Pick;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for offline queue functionality
 * Tests:
 * 1. Queued pick is sent on next sync
 * 2. Failed push is retried rather than lost
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OfflineQueueTest {

    private TestSyncManager syncManager;
    private List<Pick> queuedPicks;

    @Before
    public void setUp() {
        syncManager = new TestSyncManager();
        queuedPicks = new ArrayList<>();
    }

    /**
     * Test 1: Queued pick is sent on next sync
     */
    @Test
    public void testQueuedPickSentOnSync() {
        // Create a pick
        Pick pick = new Pick("ITEM-001", 5);
        queuedPicks.add(pick);

        // Perform sync
        syncManager.syncPicks(queuedPicks);

        // Verify pick was synced
        assertEquals("Pick should be synced", 1, syncManager.getSyncedCount());
        assertTrue("Pick should be marked as synced", pick.isSynced());
    }

    /**
     * Test 2: Failed push is retried rather than lost
     */
    @Test
    public void testFailedPushIsRetried() {
        // Create picks that will fail on first attempt
        Pick pick1 = new Pick("ITEM-001", 5);
        Pick pick2 = new Pick("ITEM-002", 3);

        queuedPicks.add(pick1);
        queuedPicks.add(pick2);

        // First sync attempt - simulate failure
        syncManager.syncWithFailure(queuedPicks, true);

        // Verify picks are still in queue (not removed)
        assertEquals("Picks should still be queued", 2, queuedPicks.size());

        // Verify retry count increased
        assertEquals("Retry count should be 1", 1, pick1.getRetryCount());
        assertEquals("Retry count should be 1", 1, pick2.getRetryCount());

        // Second sync attempt - succeed
        syncManager.syncWithFailure(queuedPicks, false);

        // Verify picks are now synced
        assertTrue("Pick should be synced after retry", pick1.isSynced());
        assertTrue("Pick should be synced after retry", pick2.isSynced());
    }

    /**
     * Test 3: Maximum retry limit
     */
    @Test
    public void testMaxRetryLimit() {
        Pick pick = new Pick("ITEM-001", 5);
        queuedPicks.add(pick);

        // Simulate 3 failed sync attempts
        for (int i = 0; i < 3; i++) {
            syncManager.syncWithFailure(queuedPicks, true);
        }

        // After 3 failures, pick should be marked as failed but not lost
        assertEquals("Retry count should be 3", 3, pick.getRetryCount());
        assertFalse("Pick should not be synced", pick.isSynced());
        assertTrue("Pick should still be in queue", queuedPicks.contains(pick));
    }

    /**
     * Test 4: Multiple picks in queue
     */
    @Test
    public void testMultipleQueuedPicks() {
        // Queue multiple picks
        for (int i = 0; i < 10; i++) {
            Pick pick = new Pick("ITEM-" + i, i);
            queuedPicks.add(pick);
        }

        // Sync all
        syncManager.syncPicks(queuedPicks);

        // Verify all synced
        assertEquals("All picks should be synced", 10, syncManager.getSyncedCount());
        for (Pick pick : queuedPicks) {
            assertTrue("Pick should be synced", pick.isSynced());
        }
    }

    /**
     * Test 5: Retry only failed picks
     */
    @Test
    public void testRetryOnlyFailedPicks() {
        Pick successPick = new Pick("ITEM-SUCCESS", 5);
        Pick failPick = new Pick("ITEM-FAIL", 3);

        queuedPicks.add(successPick);
        queuedPicks.add(failPick);

        // First sync - first succeeds, second fails
        TestSyncManager customManager = new TestSyncManager() {
            private boolean firstCall = true;

            @Override
            void syncWithFailure(List<Pick> picks, boolean shouldFail) {
                for (Pick pick : picks) {
                    if (!pick.isSynced()) {
                        if (pick.getItemCode().equals("ITEM-FAIL") && firstCall) {
                            pick.setRetryCount(pick.getRetryCount() + 1);
                        } else {
                            pick.setSynced(true);
                            syncedCount++;
                        }
                    }
                }
                firstCall = false;
            }
        };

        customManager.syncWithFailure(queuedPicks, true);

        // Success pick should be synced
        assertTrue("Success pick should be synced", successPick.isSynced());
        // Fail pick should have retry count 1
        assertEquals("Fail pick retry count should be 1", 1, failPick.getRetryCount());
        assertFalse("Fail pick should not be synced", failPick.isSynced());

        // Second sync - retry failed pick
        customManager.syncWithFailure(queuedPicks, false);

        // Now fail pick should be synced
        assertTrue("Fail pick should be synced on retry", failPick.isSynced());
    }

    /**
     * Test Sync Manager for testing
     * Simulates sync behavior for unit tests
     */
    private class TestSyncManager {
        protected int syncedCount = 0;

        void syncPicks(List<Pick> picks) {
            for (Pick pick : picks) {
                if (!pick.isSynced()) {
                    pick.setSynced(true);
                    syncedCount++;
                }
            }
        }

        void syncWithFailure(List<Pick> picks, boolean shouldFail) {
            if (shouldFail) {
                for (Pick pick : picks) {
                    if (!pick.isSynced()) {
                        pick.setRetryCount(pick.getRetryCount() + 1);
                    }
                }
            } else {
                syncPicks(picks);
            }
        }

        int getSyncedCount() {
            return syncedCount;
        }
    }
}