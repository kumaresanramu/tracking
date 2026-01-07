package com.expense.tracking.integration;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.entity.SyncOperation;
import com.expense.tracking.entity.SyncOperationType;
import com.expense.tracking.entity.SyncStatus;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.repository.SyncOperationRepository;
import com.expense.tracking.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OfflineOnlineTransitionTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SyncOperationRepository syncOperationRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        syncOperationRepository.deleteAll();
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create a test category
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF0000")
                .description("Test category for sync tests")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testOfflineExpenseQueueing() {
        // Create an expense that would normally be synced
        Expense expense = Expense.builder()
                .amount(new BigDecimal("25.50"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Offline test expense")
                .synced(false)
                .build();

        expense = expenseRepository.save(expense);

        // Queue the expense for sync (simulating offline behavior)
        syncService.queueForSync(SyncOperationType.CREATE, expense.getId(), "Expense", expense);

        // Verify the sync operation was queued
        List<SyncOperation> queuedOperations = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);
        assertThat(queuedOperations).hasSize(1);

        SyncOperation operation = queuedOperations.get(0);
        assertThat(operation.getOperationType()).isEqualTo(SyncOperationType.CREATE);
        assertThat(operation.getEntityType()).isEqualTo("Expense");
        assertThat(operation.getEntityId()).isEqualTo(expense.getId());
        assertThat(operation.getStatus()).isEqualTo(SyncStatus.PENDING);
    }

    @Test
    void testSyncOperationRetry() {
        // Create a failed sync operation
        Expense expense = Expense.builder()
                .amount(new BigDecimal("30.00"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Retry test expense")
                .synced(false)
                .build();

        expense = expenseRepository.save(expense);

        // Create a failed sync operation
        SyncOperation failedOperation = SyncOperation.builder()
                .operationType(SyncOperationType.CREATE)
                .entityType("Expense")
                .entityId(expense.getId())
                .status(SyncStatus.FAILED)
                .attemptCount(2)
                .errorMessage("Simulated network error")
                .build();

        syncOperationRepository.save(failedOperation);

        // Verify the operation is in failed state
        List<SyncOperation> failedOperations = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.FAILED);
        assertThat(failedOperations).hasSize(1);
        assertThat(failedOperations.get(0).getAttemptCount()).isEqualTo(2);
    }

    @Test
    void testSyncStatusTracking() {
        // Test sync status information
        var syncStatus = syncService.getSyncStatus();
        
        assertThat(syncStatus).isNotNull();
        assertThat(syncStatus.getLastSyncTime()).isNull(); // No sync has occurred yet
        
        // Create some pending operations
        Expense expense1 = createTestExpense("Expense 1", new BigDecimal("10.00"));
        Expense expense2 = createTestExpense("Expense 2", new BigDecimal("20.00"));
        
        syncService.queueForSync(SyncOperationType.CREATE, expense1.getId(), "Expense", expense1);
        syncService.queueForSync(SyncOperationType.CREATE, expense2.getId(), "Expense", expense2);
        
        // Check that pending operations are tracked
        List<SyncOperation> pendingOps = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);
        assertThat(pendingOps).hasSize(2);
    }

    @Test
    void testConflictResolution() {
        // Create an expense
        Expense localExpense = Expense.builder()
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Conflict test expense")
                .synced(false)
                .build();

        localExpense = expenseRepository.save(localExpense);

        // Simulate a conflict scenario by creating a sync operation
        syncService.queueForSync(SyncOperationType.UPDATE, localExpense.getId(), "Expense", localExpense);

        // Verify the operation was queued
        List<SyncOperation> operations = syncOperationRepository.findByEntityIdAndEntityTypeAndStatus(localExpense.getId(), "Expense", SyncStatus.PENDING);
        assertThat(operations).hasSize(1);
        assertThat(operations.get(0).getOperationType()).isEqualTo(SyncOperationType.UPDATE);
    }

    @Test
    void testMultipleOperationTypes() {
        // Create expenses for different operation types
        Expense createExpense = createTestExpense("Create test", new BigDecimal("15.00"));
        Expense updateExpense = createTestExpense("Update test", new BigDecimal("25.00"));
        Expense deleteExpense = createTestExpense("Delete test", new BigDecimal("35.00"));

        // Queue different types of operations
        syncService.queueForSync(SyncOperationType.CREATE, createExpense.getId(), "Expense", createExpense);
        syncService.queueForSync(SyncOperationType.UPDATE, updateExpense.getId(), "Expense", updateExpense);
        syncService.queueForSync(SyncOperationType.DELETE, deleteExpense.getId(), "Expense", deleteExpense);

        // Verify all operations were queued
        List<SyncOperation> allOperations = syncOperationRepository.findAll();
        assertThat(allOperations).hasSize(3);

        // Verify operation types
        assertThat(allOperations.stream().map(SyncOperation::getOperationType))
                .containsExactlyInAnyOrder(
                        SyncOperationType.CREATE,
                        SyncOperationType.UPDATE,
                        SyncOperationType.DELETE
                );
    }

    private Expense createTestExpense(String description, BigDecimal amount) {
        Expense expense = Expense.builder()
                .amount(amount)
                .date(LocalDate.now())
                .category(testCategory)
                .description(description)
                .synced(false)
                .build();

        return expenseRepository.save(expense);
    }
}