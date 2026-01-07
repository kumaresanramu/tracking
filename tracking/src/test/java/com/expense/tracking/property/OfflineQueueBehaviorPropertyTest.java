package com.expense.tracking.property;

import com.expense.tracking.config.TestConfig;
import com.expense.tracking.entity.*;
import com.expense.tracking.repository.SyncOperationRepository;
import com.expense.tracking.service.SyncService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for offline queue behavior
 * **Feature: expense-tracking, Property 6: Offline Queue Behavior**
 * **Validates: Requirements 2.4**
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class OfflineQueueBehaviorPropertyTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private SyncOperationRepository syncOperationRepository;

    @BeforeEach
    void setUp() {
        syncOperationRepository.deleteAll();
    }

    @Property(tries = 100)
    @Label("For any expense operation performed while offline, the operation should be queued locally")
    void offlineOperationsAreQueuedLocally(
            @ForAll @Positive Long entityId,
            @ForAll @StringLength(min = 1, max = 500) String description,
            @ForAll SyncOperationType operationType) {

        // Given: An expense entity to be synced
        Expense expense = createTestExpense(entityId, description);

        // When: We queue the operation for sync (simulating offline behavior)
        syncService.queueForSync(operationType, entityId, "Expense", expense);

        // Then: The operation should be queued with PENDING status
        List<SyncOperation> queuedOperations = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);
        
        assertThat(queuedOperations).hasSize(1);
        
        SyncOperation queuedOperation = queuedOperations.get(0);
        assertThat(queuedOperation.getOperationType()).isEqualTo(operationType);
        assertThat(queuedOperation.getEntityId()).isEqualTo(entityId);
        assertThat(queuedOperation.getEntityType()).isEqualTo("Expense");
        assertThat(queuedOperation.getStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(queuedOperation.getAttemptCount()).isEqualTo(0);
        assertThat(queuedOperation.getCreatedAt()).isNotNull();
        assertThat(queuedOperation.getEntityData()).isNotNull();
    }

    @Property(tries = 100)
    @Label("For any duplicate operation on the same entity, only one should be queued")
    void duplicateOperationsAreNotQueued(
            @ForAll @Positive Long entityId,
            @ForAll @StringLength(min = 1, max = 500) String description) {

        // Given: An expense entity
        Expense expense = createTestExpense(entityId, description);

        // When: We queue the same operation multiple times
        syncService.queueForSync(SyncOperationType.CREATE, entityId, "Expense", expense);
        syncService.queueForSync(SyncOperationType.CREATE, entityId, "Expense", expense);
        syncService.queueForSync(SyncOperationType.CREATE, entityId, "Expense", expense);

        // Then: Only one operation should be queued
        List<SyncOperation> queuedOperations = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);
        
        assertThat(queuedOperations).hasSize(1);
        
        SyncOperation queuedOperation = queuedOperations.get(0);
        assertThat(queuedOperation.getOperationType()).isEqualTo(SyncOperationType.CREATE);
        assertThat(queuedOperation.getEntityId()).isEqualTo(entityId);
        assertThat(queuedOperation.getEntityType()).isEqualTo("Expense");
        assertThat(queuedOperation.getStatus()).isEqualTo(SyncStatus.PENDING);
    }

    @Property(tries = 100)
    @Label("For any queued operations, they should be retrievable in creation order")
    void queuedOperationsAreRetrievableInOrder(
            @ForAll("expenseList") List<Expense> expenses) {

        // Given: Multiple expense operations to queue
        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            // Use different entity IDs to avoid duplicate filtering
            syncService.queueForSync(SyncOperationType.CREATE, (long) (i + 1), "Expense", expense);
        }

        // When: We retrieve queued operations
        List<SyncOperation> queuedOperations = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);

        // Then: Operations should be returned in creation order
        assertThat(queuedOperations).hasSize(expenses.size());
        
        for (int i = 0; i < queuedOperations.size(); i++) {
            SyncOperation operation = queuedOperations.get(i);
            assertThat(operation.getEntityId()).isEqualTo((long) (i + 1));
            assertThat(operation.getStatus()).isEqualTo(SyncStatus.PENDING);
            
            if (i > 0) {
                // Each operation should be created after the previous one
                assertThat(operation.getCreatedAt()).isAfterOrEqualTo(queuedOperations.get(i - 1).getCreatedAt());
            }
        }
    }

    @Provide
    Arbitrary<List<Expense>> expenseList() {
        return Arbitraries.of(
                createTestExpense(1L, "Test expense 1"),
                createTestExpense(2L, "Test expense 2"),
                createTestExpense(3L, "Test expense 3"),
                createTestExpense(4L, "Test expense 4"),
                createTestExpense(5L, "Test expense 5")
        ).list().ofMinSize(1).ofMaxSize(5);
    }

    private Expense createTestExpense(Long id, String description) {
        return Expense.builder()
                .id(id)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .synced(false)
                .build();
    }
}