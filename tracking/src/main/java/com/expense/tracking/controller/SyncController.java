package com.expense.tracking.controller;

import com.expense.tracking.dto.SyncStatusInfo;
import com.expense.tracking.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {
    
    private final SyncService syncService;
    
    /**
     * Get current sync status
     */
    @GetMapping("/status")
    public ResponseEntity<SyncStatusInfo> getSyncStatus() {
        SyncStatusInfo status = syncService.getSyncStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * Manually trigger sync of pending changes
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerSync() {
        log.info("Manual sync triggered");
        CompletableFuture<Void> syncFuture = syncService.syncPendingChanges();
        return ResponseEntity.ok("Sync triggered successfully");
    }
}