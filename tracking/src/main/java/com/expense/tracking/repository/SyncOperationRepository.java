package com.expense.tracking.repository;

import com.expense.tracking.entity.SyncOperation;
import com.expense.tracking.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncOperationRepository extends JpaRepository<SyncOperation, Long> {
    
    List<SyncOperation> findByStatusOrderByCreatedAtAsc(SyncStatus status);
    
    @Query("SELECT s FROM SyncOperation s WHERE s.status = 'FAILED' AND s.nextRetryAt <= :now ORDER BY s.createdAt ASC")
    List<SyncOperation> findFailedOperationsReadyForRetry(LocalDateTime now);
    
    List<SyncOperation> findByEntityIdAndEntityTypeAndStatus(Long entityId, String entityType, SyncStatus status);
    
    long countByStatus(SyncStatus status);
}