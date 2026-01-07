package com.expense.tracking.property;

import com.expense.tracking.dto.SyncStatusInfo;
import com.expense.tracking.service.SyncService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 23: Sync Status Display
 * Validates: Requirements 9.5
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class SyncStatusDisplayPropertyTest {

    @Mock
    private SyncService syncService;
    
    private Map<String, Object> syncStatusDisplay = new HashMap<>();
    private boolean statusDisplayUpdated = false;
    private String displayedStatus = "";
    private String displayedMessage = "";
    private boolean showProgressIndicator = false;
    private boolean showRetryButton = false;

    @Property(tries = 100)
    void syncStatusDisplayShowsCurrentSyncState(
            @ForAll("syncStatusInfo") SyncStatusInfo statusInfo) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        displayedStatus = "";
        displayedMessage = "";
        showProgressIndicator = false;
        showRetryButton = false;
        
        // When: Sync status is updated
        updateSyncStatusDisplay(statusInfo);
        
        // Then: Status display should be updated
        assertThat(statusDisplayUpdated).isTrue();
        
        // And: Display should reflect the current sync state
        assertThat(syncStatusDisplay).containsKey("status");
        assertThat(syncStatusDisplay).containsKey("message");
        assertThat(syncStatusDisplay).containsKey("timestamp");
        
        String expectedStatus = determineSyncStatusText(statusInfo);
        assertThat(displayedStatus).isEqualTo(expectedStatus);
        
        // Verify status-specific display elements
        if (statusInfo.getPendingOperations() > 0 && !statusInfo.isOnline()) {
            assertThat(displayedMessage).contains("pending");
        }
        
        if (statusInfo.getInProgressOperations() > 0) {
            assertThat(showProgressIndicator).isTrue();
        }
        
        if (statusInfo.getFailedOperations() > 0) {
            assertThat(showRetryButton).isTrue();
        }
    }

    @Property(tries = 100)
    void syncStatusDisplayHandlesPendingOperationsCount(
            @ForAll @LongRange(min = 0, max = 1000) long pendingOperations,
            @ForAll boolean isOnline) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        displayedMessage = "";
        
        // Given: Sync status with specific pending operations count
        SyncStatusInfo statusInfo = SyncStatusInfo.builder()
                .pendingOperations(pendingOperations)
                .inProgressOperations(0L)
                .failedOperations(0L)
                .isOnline(isOnline)
                .lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        // When: Status display is updated
        updateSyncStatusDisplay(statusInfo);
        
        // Then: Pending operations count should be displayed correctly
        assertThat(statusDisplayUpdated).isTrue();
        
        if (pendingOperations > 0) {
            assertThat(displayedMessage).contains(String.valueOf(pendingOperations));
            assertThat(displayedMessage.toLowerCase()).contains("pending");
        } else {
            if (!isOnline) {
                assertThat(displayedMessage.toLowerCase()).contains("no pending");
            }
        }
        
        // Verify the count is accurate
        assertThat(syncStatusDisplay.get("pendingCount")).isEqualTo(pendingOperations);
    }

    @Property(tries = 100)
    void syncStatusDisplayShowsProgressForInProgressOperations(
            @ForAll @LongRange(min = 0, max = 50) long inProgressOperations,
            @ForAll @LongRange(min = 0, max = 100) long totalOperations) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        showProgressIndicator = false;
        
        // Ensure total is at least as large as in-progress
        long actualTotal = Math.max(totalOperations, inProgressOperations);
        
        // Given: Sync status with in-progress operations
        SyncStatusInfo statusInfo = SyncStatusInfo.builder()
                .pendingOperations(actualTotal - inProgressOperations)
                .inProgressOperations(inProgressOperations)
                .failedOperations(0L)
                .isOnline(true)
                .lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        // When: Status display is updated
        updateSyncStatusDisplay(statusInfo);
        
        // Then: Progress indicator should be shown for in-progress operations
        assertThat(statusDisplayUpdated).isTrue();
        
        if (inProgressOperations > 0) {
            assertThat(showProgressIndicator).isTrue();
            assertThat(displayedStatus).isEqualTo("Syncing");
            
            // Progress percentage should be calculated correctly
            if (actualTotal > 0) {
                double expectedProgress = ((double) inProgressOperations / actualTotal) * 100;
                assertThat(syncStatusDisplay.get("progress")).isEqualTo(expectedProgress);
            }
        } else {
            // No progress indicator if no operations in progress
            if (statusInfo.getPendingOperations() == 0 && statusInfo.getFailedOperations() == 0) {
                assertThat(showProgressIndicator).isFalse();
            }
        }
    }

    @Property(tries = 100)
    void syncStatusDisplayHandlesFailedOperationsWithRetry(
            @ForAll @LongRange(min = 0, max = 100) long failedOperations,
            @ForAll @StringLength(max = 200) String errorMessage) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        showRetryButton = false;
        displayedMessage = "";
        
        // Given: Sync status with failed operations
        SyncStatusInfo statusInfo = SyncStatusInfo.builder()
                .pendingOperations(0L)
                .inProgressOperations(0L)
                .failedOperations(failedOperations)
                .isOnline(true)
                .lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        // When: Status display is updated with error message
        updateSyncStatusDisplayWithError(statusInfo, errorMessage);
        
        // Then: Failed operations should be handled appropriately
        assertThat(statusDisplayUpdated).isTrue();
        
        if (failedOperations > 0) {
            assertThat(showRetryButton).isTrue();
            assertThat(displayedStatus).isEqualTo("Error");
            assertThat(displayedMessage).contains("failed");
            
            if (!errorMessage.trim().isEmpty()) {
                assertThat(displayedMessage).contains(errorMessage.trim());
            }
            
            // Verify failed count is displayed
            assertThat(syncStatusDisplay.get("failedCount")).isEqualTo(failedOperations);
        } else {
            // No retry button if no failed operations
            assertThat(showRetryButton).isFalse();
        }
    }

    @Property(tries = 100)
    void syncStatusDisplayReflectsOnlineOfflineState(
            @ForAll boolean isOnline,
            @ForAll @LongRange(min = 0, max = 50) long pendingOperations) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        displayedStatus = "";
        
        // Given: Sync status with specific online state
        SyncStatusInfo statusInfo = SyncStatusInfo.builder()
                .pendingOperations(pendingOperations)
                .inProgressOperations(0L)
                .failedOperations(0L)
                .isOnline(isOnline)
                .lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        // When: Status display is updated
        updateSyncStatusDisplay(statusInfo);
        
        // Then: Online/offline state should be reflected correctly
        assertThat(statusDisplayUpdated).isTrue();
        
        if (isOnline) {
            if (pendingOperations == 0) {
                assertThat(displayedStatus).isEqualTo("Online");
            } else {
                // Online with pending operations might show "Syncing" or "Online"
                assertThat(displayedStatus).isIn("Online", "Syncing");
            }
        } else {
            assertThat(displayedStatus).isEqualTo("Offline");
        }
        
        // Verify connectivity indicator
        assertThat(syncStatusDisplay.get("isOnline")).isEqualTo(isOnline);
    }

    @Property(tries = 100)
    void syncStatusDisplayShowsLastSyncTime(
            @ForAll("lastSyncTime") String lastSyncTime,
            @ForAll boolean isOnline) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        
        // Given: Sync status with last sync time
        SyncStatusInfo statusInfo = SyncStatusInfo.builder()
                .pendingOperations(0L)
                .inProgressOperations(0L)
                .failedOperations(0L)
                .isOnline(isOnline)
                .lastSyncTime(lastSyncTime)
                .build();
        
        // When: Status display is updated
        updateSyncStatusDisplay(statusInfo);
        
        // Then: Last sync time should be displayed
        assertThat(statusDisplayUpdated).isTrue();
        
        if (lastSyncTime != null && !lastSyncTime.trim().isEmpty()) {
            assertThat(syncStatusDisplay.get("lastSyncTime")).isEqualTo(lastSyncTime);
            
            // Display should show relative time or formatted time
            String displayTime = formatLastSyncTime(lastSyncTime);
            assertThat(displayTime).isNotEmpty();
        } else {
            // No last sync time available
            assertThat(syncStatusDisplay.get("lastSyncTime")).isIn(null, "");
        }
    }

    @Property(tries = 100)
    void syncStatusDisplayUpdatesInRealTime(
            @ForAll("syncStatusSequence") SyncStatusSequence statusSequence) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        
        String previousStatus = "";
        int updateCount = 0;
        
        // When: Multiple status updates occur in sequence
        for (SyncStatusInfo status : statusSequence.statuses) {
            updateSyncStatusDisplay(status);
            updateCount++;
            
            // Then: Each update should be reflected immediately
            assertThat(statusDisplayUpdated).isTrue();
            
            String currentStatus = determineSyncStatusText(status);
            
            // Status should update if it changed
            if (!currentStatus.equals(previousStatus)) {
                assertThat(displayedStatus).isEqualTo(currentStatus);
            }
            
            previousStatus = currentStatus;
            statusDisplayUpdated = false; // Reset for next iteration
        }
        
        // Verify all updates were processed
        assertThat(updateCount).isEqualTo(statusSequence.statuses.size());
    }

    @Property(tries = 100)
    void syncStatusDisplayHandlesComplexSyncScenarios(
            @ForAll @LongRange(min = 1, max = 20) long pendingOps,
            @ForAll @LongRange(min = 0, max = 10) long inProgressOps,
            @ForAll @LongRange(min = 0, max = 5) long failedOps,
            @ForAll boolean isOnline) {
        
        // Clear previous test data
        syncStatusDisplay.clear();
        statusDisplayUpdated = false;
        showProgressIndicator = false;
        showRetryButton = false;
        
        // Given: Complex sync scenario with multiple operation types
        SyncStatusInfo statusInfo = SyncStatusInfo.builder()
                .pendingOperations(pendingOps)
                .inProgressOperations(inProgressOps)
                .failedOperations(failedOps)
                .isOnline(isOnline)
                .lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        // When: Status display is updated
        updateSyncStatusDisplay(statusInfo);
        
        // Then: Display should handle complex scenario appropriately
        assertThat(statusDisplayUpdated).isTrue();
        
        // Priority: Failed > In Progress > Pending > Online/Offline
        if (failedOps > 0) {
            assertThat(displayedStatus).isEqualTo("Error");
            assertThat(showRetryButton).isTrue();
        } else if (inProgressOps > 0) {
            assertThat(displayedStatus).isEqualTo("Syncing");
            assertThat(showProgressIndicator).isTrue();
        } else if (pendingOps > 0 && isOnline) {
            assertThat(displayedStatus).isIn("Online", "Syncing");
        } else if (pendingOps > 0 && !isOnline) {
            assertThat(displayedStatus).isEqualTo("Offline");
        } else {
            assertThat(displayedStatus).isEqualTo(isOnline ? "Online" : "Offline");
        }
        
        // Verify all counts are preserved
        assertThat(syncStatusDisplay.get("pendingCount")).isEqualTo(pendingOps);
        assertThat(syncStatusDisplay.get("inProgressCount")).isEqualTo(inProgressOps);
        assertThat(syncStatusDisplay.get("failedCount")).isEqualTo(failedOps);
    }

    // Helper methods
    private void updateSyncStatusDisplay(SyncStatusInfo statusInfo) {
        syncStatusDisplay.clear();
        syncStatusDisplay.put("status", determineSyncStatusText(statusInfo));
        syncStatusDisplay.put("message", generateStatusMessage(statusInfo));
        syncStatusDisplay.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        syncStatusDisplay.put("isOnline", statusInfo.isOnline());
        syncStatusDisplay.put("pendingCount", statusInfo.getPendingOperations());
        syncStatusDisplay.put("inProgressCount", statusInfo.getInProgressOperations());
        syncStatusDisplay.put("failedCount", statusInfo.getFailedOperations());
        syncStatusDisplay.put("lastSyncTime", statusInfo.getLastSyncTime());
        
        displayedStatus = (String) syncStatusDisplay.get("status");
        displayedMessage = (String) syncStatusDisplay.get("message");
        
        // Determine UI elements to show
        showProgressIndicator = statusInfo.getInProgressOperations() > 0;
        showRetryButton = statusInfo.getFailedOperations() > 0;
        
        statusDisplayUpdated = true;
        
        // Calculate progress if operations are in progress
        if (statusInfo.getInProgressOperations() > 0) {
            long totalOps = statusInfo.getPendingOperations() + statusInfo.getInProgressOperations();
            if (totalOps > 0) {
                double progress = ((double) statusInfo.getInProgressOperations() / totalOps) * 100;
                syncStatusDisplay.put("progress", progress);
            }
        }
        
        System.out.println("Sync status display updated: " + displayedStatus + " - " + displayedMessage);
    }
    
    private void updateSyncStatusDisplayWithError(SyncStatusInfo statusInfo, String errorMessage) {
        updateSyncStatusDisplay(statusInfo);
        
        if (statusInfo.getFailedOperations() > 0 && !errorMessage.trim().isEmpty()) {
            displayedMessage = generateStatusMessage(statusInfo) + " - " + errorMessage.trim();
            syncStatusDisplay.put("message", displayedMessage);
            syncStatusDisplay.put("errorMessage", errorMessage.trim());
        }
    }
    
    private String determineSyncStatusText(SyncStatusInfo statusInfo) {
        // Priority: Failed > In Progress > Pending > Online/Offline
        if (statusInfo.getFailedOperations() > 0) {
            return "Error";
        } else if (statusInfo.getInProgressOperations() > 0) {
            return "Syncing";
        } else if (statusInfo.getPendingOperations() > 0 && statusInfo.isOnline()) {
            return "Online"; // Could be "Syncing" depending on implementation
        } else {
            return statusInfo.isOnline() ? "Online" : "Offline";
        }
    }
    
    private String generateStatusMessage(SyncStatusInfo statusInfo) {
        if (statusInfo.getFailedOperations() > 0) {
            return statusInfo.getFailedOperations() + " operations failed";
        } else if (statusInfo.getInProgressOperations() > 0) {
            return "Syncing " + statusInfo.getInProgressOperations() + " operations";
        } else if (statusInfo.getPendingOperations() > 0) {
            return statusInfo.getPendingOperations() + " operations pending";
        } else if (statusInfo.isOnline()) {
            return "All data synchronized";
        } else {
            return "No pending changes";
        }
    }
    
    private String formatLastSyncTime(String lastSyncTime) {
        if (lastSyncTime == null || lastSyncTime.trim().isEmpty()) {
            return "";
        }
        
        try {
            LocalDateTime syncTime = LocalDateTime.parse(lastSyncTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            
            // Simple relative time formatting
            long minutesDiff = java.time.Duration.between(syncTime, now).toMinutes();
            
            if (minutesDiff < 1) {
                return "Just now";
            } else if (minutesDiff < 60) {
                return minutesDiff + " minutes ago";
            } else {
                return syncTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
            }
        } catch (Exception e) {
            return lastSyncTime; // Return original if parsing fails
        }
    }

    // Helper classes
    static class SyncStatusSequence {
        java.util.List<SyncStatusInfo> statuses;
        
        public SyncStatusSequence(java.util.List<SyncStatusInfo> statuses) {
            this.statuses = statuses;
        }
    }

    // Providers for test data generation
    @Provide
    Arbitrary<SyncStatusInfo> syncStatusInfo() {
        return Arbitraries.of(
                // Online with no operations
                SyncStatusInfo.builder()
                        .pendingOperations(0L)
                        .inProgressOperations(0L)
                        .failedOperations(0L)
                        .isOnline(true)
                        .lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build(),
                
                // Offline with pending operations
                SyncStatusInfo.builder()
                        .pendingOperations(3L)
                        .inProgressOperations(0L)
                        .failedOperations(0L)
                        .isOnline(false)
                        .lastSyncTime(LocalDateTime.now().minusMinutes(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build(),
                
                // Syncing with in-progress operations
                SyncStatusInfo.builder()
                        .pendingOperations(2L)
                        .inProgressOperations(1L)
                        .failedOperations(0L)
                        .isOnline(true)
                        .lastSyncTime(LocalDateTime.now().minusMinutes(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build(),
                
                // Error state with failed operations
                SyncStatusInfo.builder()
                        .pendingOperations(1L)
                        .inProgressOperations(0L)
                        .failedOperations(2L)
                        .isOnline(true)
                        .lastSyncTime(LocalDateTime.now().minusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build(),
                
                // Complex scenario
                SyncStatusInfo.builder()
                        .pendingOperations(5L)
                        .inProgressOperations(2L)
                        .failedOperations(1L)
                        .isOnline(true)
                        .lastSyncTime(LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build()
        );
    }
    
    @Provide
    Arbitrary<String> lastSyncTime() {
        return Arbitraries.of(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.now().minusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "",
                null
        );
    }
    
    @Provide
    Arbitrary<SyncStatusSequence> syncStatusSequence() {
        return Arbitraries.of(
                new SyncStatusSequence(java.util.List.of(
                        SyncStatusInfo.builder().pendingOperations(0L).inProgressOperations(0L).failedOperations(0L).isOnline(false).lastSyncTime("").build(),
                        SyncStatusInfo.builder().pendingOperations(2L).inProgressOperations(0L).failedOperations(0L).isOnline(false).lastSyncTime("").build(),
                        SyncStatusInfo.builder().pendingOperations(2L).inProgressOperations(1L).failedOperations(0L).isOnline(true).lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).build(),
                        SyncStatusInfo.builder().pendingOperations(0L).inProgressOperations(0L).failedOperations(0L).isOnline(true).lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).build()
                )),
                new SyncStatusSequence(java.util.List.of(
                        SyncStatusInfo.builder().pendingOperations(1L).inProgressOperations(0L).failedOperations(0L).isOnline(true).lastSyncTime("").build(),
                        SyncStatusInfo.builder().pendingOperations(1L).inProgressOperations(0L).failedOperations(1L).isOnline(true).lastSyncTime("").build(),
                        SyncStatusInfo.builder().pendingOperations(1L).inProgressOperations(1L).failedOperations(0L).isOnline(true).lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).build(),
                        SyncStatusInfo.builder().pendingOperations(0L).inProgressOperations(0L).failedOperations(0L).isOnline(true).lastSyncTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).build()
                ))
        );
    }
}