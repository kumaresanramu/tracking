package com.expense.tracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncOperation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncOperationType operationType;
    
    @Column(nullable = false)
    private Long entityId;
    
    @Column(nullable = false)
    private String entityType;
    
    @Column(columnDefinition = "TEXT")
    private String entityData; // JSON representation of the entity
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastAttemptAt;
    
    @Column
    @Builder.Default
    private Integer attemptCount = 0;
    
    @Column
    private String errorMessage;
    
    @Column
    private LocalDateTime nextRetryAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}