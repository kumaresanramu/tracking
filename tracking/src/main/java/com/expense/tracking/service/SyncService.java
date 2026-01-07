package com.expense.tracking.service;

import com.expense.tracking.entity.*;
import com.expense.tracking.repository.SyncOperationRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.dto.SyncStatusInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    
    private final SyncOperationRepository syncOperationRepository;
    private final ExpenseRepository expenseRepository;
    private final ObjectMapper objectMapper;
    
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int BASE_RETRY_DELAY_SECONDS = 2;
    
    /**
     * Queue an operation for sync
     */
    @Transactional
    public void queueForSync(SyncOperationType operationType, Long entityId, String entityType, Object entityData) {
        try {
            // Check if there's already a pending operation for this entity
            List<SyncOperation> existingOperations = syncOperationRepository
                .findByEntityIdAndEntityTypeAndStatus(entityId, entityType, SyncStatus.PENDING);
            
            if (!existingOperations.isEmpty()) {
                log.debug("Operation already queued for entity {} of type {}", entityId, entityType);
                return;
            }
            
            String jsonData = objectMapper.writeValueAsString(entityData);
            
            SyncOperation operation = SyncOperation.builder()
                .operationType(operationType)
                .entityId(entityId)
                .entityType(entityType)
                .entityData(jsonData)
                .status(SyncStatus.PENDING)
                .attemptCount(0)
                .build();
            
            syncOperationRepository.save(operation);
            log.info("Queued {} operation for {} with id {}", operationType, entityType, entityId);
            
        } catch (Exception e) {
            log.error("Failed to queue sync operation for entity {} of type {}", entityId, entityType, e);
        }
    }
    
    /**
     * Process all pending sync operations
     */
    @Async
    public CompletableFuture<Void> syncPendingChanges() {
        log.info("Starting sync of pending changes");
        
        try {
            List<SyncOperation> pendingOperations = syncOperationRepository
                .findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);
            
            for (SyncOperation operation : pendingOperations) {
                processSyncOperation(operation);
            }
            
            // Also process failed operations that are ready for retry
            List<SyncOperation> retryOperations = syncOperationRepository
                .findFailedOperationsReadyForRetry(LocalDateTime.now());
            
            for (SyncOperation operation : retryOperations) {
                processSyncOperation(operation);
            }
            
            log.info("Completed sync of pending changes");
            
        } catch (Exception e) {
            log.error("Error during sync process", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Process a single sync operation
     */
    @Transactional
    public void processSyncOperation(SyncOperation operation) {
        try {
            operation.setStatus(SyncStatus.IN_PROGRESS);
            operation.setLastAttemptAt(LocalDateTime.now());
            operation.setAttemptCount(operation.getAttemptCount() + 1);
            syncOperationRepository.save(operation);
            
            // For now, we'll simulate successful sync
            // In a real implementation, this would call the appropriate sync method
            boolean success = simulateSync(operation);
            
            if (success) {
                operation.setStatus(SyncStatus.COMPLETED);
                operation.setErrorMessage(null);
                
                // Mark the entity as synced if it's an expense
                if ("Expense".equals(operation.getEntityType())) {
                    markExpenseAsSynced(operation.getEntityId());
                }
                
                log.info("Successfully synced {} operation for {} with id {}", 
                    operation.getOperationType(), operation.getEntityType(), operation.getEntityId());
            } else {
                handleSyncFailure(operation);
            }
            
            syncOperationRepository.save(operation);
            
        } catch (Exception e) {
            log.error("Error processing sync operation {}", operation.getId(), e);
            operation.setStatus(SyncStatus.FAILED);
            operation.setErrorMessage(e.getMessage());
            handleSyncFailure(operation);
            syncOperationRepository.save(operation);
        }
    }
    
    /**
     * Handle sync failure with exponential backoff
     */
    private void handleSyncFailure(SyncOperation operation) {
        if (operation.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            operation.setStatus(SyncStatus.FAILED);
            log.error("Max retry attempts reached for operation {}", operation.getId());
        } else {
            operation.setStatus(SyncStatus.FAILED);
            
            // Calculate exponential backoff delay
            long delaySeconds = (long) (BASE_RETRY_DELAY_SECONDS * Math.pow(2, operation.getAttemptCount() - 1));
            operation.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            
            log.warn("Sync operation {} failed, will retry in {} seconds (attempt {}/{})", 
                operation.getId(), delaySeconds, operation.getAttemptCount(), MAX_RETRY_ATTEMPTS);
        }
    }
    
    /**
     * Simulate sync operation (placeholder for actual Google Sheets integration)
     */
    private boolean simulateSync(SyncOperation operation) {
        // For testing purposes, we'll simulate successful sync
        // In a real implementation, this would integrate with GoogleSheetsService
        try {
            Thread.sleep(10); // Simulate some processing time
            return true; // Simulate successful sync
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Mark expense as synced
     */
    private void markExpenseAsSynced(Long expenseId) {
        Optional<Expense> expenseOpt = expenseRepository.findById(expenseId);
        if (expenseOpt.isPresent()) {
            Expense expense = expenseOpt.get();
            expense.setSynced(true);
            expenseRepository.save(expense);
        }
    }
    
    /**
     * Handle sync conflicts by timestamp
     */
    public void handleSyncConflict(Expense local, Expense remote) {
        log.info("Handling sync conflict for expense {}", local.getId());
        
        // Use the most recent timestamp to resolve conflicts
        LocalDateTime localTimestamp = local.getUpdatedAt() != null ? local.getUpdatedAt() : local.getCreatedAt();
        LocalDateTime remoteTimestamp = remote.getUpdatedAt() != null ? remote.getUpdatedAt() : remote.getCreatedAt();
        
        if (localTimestamp.isAfter(remoteTimestamp)) {
            log.info("Local version is newer, keeping local changes for expense {}", local.getId());
            // Queue the local version for sync
            queueForSync(SyncOperationType.UPDATE, local.getId(), "Expense", local);
        } else {
            log.info("Remote version is newer, updating local data for expense {}", local.getId());
            // Update local with remote data
            local.setAmount(remote.getAmount());
            local.setDate(remote.getDate());
            local.setCategory(remote.getCategory());
            local.setDescription(remote.getDescription());
            local.setUpdatedAt(remote.getUpdatedAt());
            local.setSynced(true);
            expenseRepository.save(local);
        }
    }
    
    /**
     * Get sync status information
     */
    public SyncStatusInfo getSyncStatus() {
        long pendingCount = syncOperationRepository.countByStatus(SyncStatus.PENDING);
        long inProgressCount = syncOperationRepository.countByStatus(SyncStatus.IN_PROGRESS);
        long failedCount = syncOperationRepository.countByStatus(SyncStatus.FAILED);
        
        return SyncStatusInfo.builder()
            .pendingOperations(pendingCount)
            .inProgressOperations(inProgressCount)
            .failedOperations(failedCount)
            .isOnline(true) // Simplified for now - could be enhanced with actual connectivity check
            .build();
    }
    
    /**
     * Scheduled task to automatically sync pending changes
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void scheduledSync() {
        // For now, we'll always attempt sync
        // In a real implementation, this would check connectivity first
        syncPendingChanges();
    }
}