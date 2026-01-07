package com.expense.tracking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncStatusInfo {
    private long pendingOperations;
    private long inProgressOperations;
    private long failedOperations;
    private boolean isOnline;
    private String lastSyncTime;
}