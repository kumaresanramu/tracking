package com.expense.tracking.integration;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.entity.SyncOperation;
import com.expense.tracking.entity.SyncOperationType;
import com.expense.tracking.entity.SyncStatus;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.repository.SyncOperationRepository;
import com.expense.tracking.service.GoogleSheetsService;
import com.expense.tracking.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GoogleSheetsIntegrationTest {

    @Autowired
    private GoogleSheetsService googleSheetsService;

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
                .description("Test category for Google Sheets integration")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testGoogleSheetsConnectionStatus() {
        // Test connection status (should be false in test environment)
        boolean isConnected = googleSheetsService.isConnected();
        assertThat(isConnected).isFalse(); // Google Sheets is disabled in test profile
    }

    @Test
    void testExpenseSyncWorkflow() {
        // Create an expense
        Expense expense = Expense.builder()
                .amount(new BigDecimal("75.50"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Google Sheets sync test")
                .synced(false)
                .build();

        expense = expenseRepository.save(expense);

        // Test sync to Google Sheets (should handle gracefully when disabled)
        googleSheetsService.syncExpenseToSheet(expense);
        
        // Verify expense still exists and is marked appropriately
        Expense savedExpense = expenseRepository.findById(expense.getId()).orElse(null);
        assertThat(savedExpense).isNotNull();
        assertThat(savedExpense.getAmount()).isEqualTo(new BigDecimal("75.50"));
    }

    @Test
    void testSyncServiceIntegrationWithGoogleSheets() {
        // Create an expense
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Sync service integration test")
                .synced(false)
                .build();

        expense = expenseRepository.save(expense);

        // Queue the expense for sync
        syncService.queueForSync(SyncOperationType.CREATE, expense.getId(), "Expense", expense);

        // Verify the sync operation was queued
        List<SyncOperation> queuedOperations = syncOperationRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING);
        assertThat(queuedOperations).hasSize(1);

        SyncOperation operation = queuedOperations.get(0);
        assertThat(operation.getOperationType()).isEqualTo(SyncOperationType.CREATE);
        assertThat(operation.getEntityId()).isEqualTo(expense.getId());

        // Process the sync operation
        syncService.processSyncOperation(operation);

        // Verify the operation was processed
        SyncOperation processedOperation = syncOperationRepository.findById(operation.getId()).orElse(null);
        assertThat(processedOperation).isNotNull();
        assertThat(processedOperation.getStatus()).isEqualTo(SyncStatus.COMPLETED);

        // Verify the expense was marked as synced
        Expense syncedExpense = expenseRepository.findById(expense.getId()).orElse(null);
        assertThat(syncedExpense).isNotNull();
        assertThat(syncedExpense.getSynced()).isTrue();
    }

    @Test
    void testMonthlySheetCreation() {
        LocalDate testDate = LocalDate.now();
        
        // Test monthly sheet creation (should handle gracefully when Google Sheets is disabled)
        // This should not throw an exception when Google Sheets is disabled
        if (googleSheetsService.isConnected()) {
            googleSheetsService.createMonthlySheet(testDate.getYear(), testDate.getMonthValue());
        }
        
        // No exception should be thrown, and the method should complete successfully
        // In a real environment with Google Sheets enabled, this would create the sheet
        assertThat(googleSheetsService.isConnected()).isFalse(); // Should be false in test environment
    }

    @Test
    void testExpenseUpdateSync() {
        // Create an expense
        Expense expense = Expense.builder()
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Update sync test")
                .synced(true)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        expense = expenseRepository.save(expense);

        // Update the expense
        expense.setAmount(new BigDecimal("60.00"));
        expense.setDescription("Updated sync test");
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setSynced(false);
        expense = expenseRepository.save(expense);

        // Queue the update for sync
        syncService.queueForSync(SyncOperationType.UPDATE, expense.getId(), "Expense", expense);

        // Verify the update operation was queued
        List<SyncOperation> updateOperations = syncOperationRepository.findByEntityIdAndEntityTypeAndStatus(
                expense.getId(), "Expense", SyncStatus.PENDING);
        assertThat(updateOperations).hasSize(1);
        assertThat(updateOperations.get(0).getOperationType()).isEqualTo(SyncOperationType.UPDATE);

        // Process the sync operation
        syncService.processSyncOperation(updateOperations.get(0));

        // Verify the operation completed successfully
        SyncOperation processedOperation = syncOperationRepository.findById(updateOperations.get(0).getId()).orElse(null);
        assertThat(processedOperation).isNotNull();
        assertThat(processedOperation.getStatus()).isEqualTo(SyncStatus.COMPLETED);
    }

    @Test
    void testSyncRetryMechanism() {
        // Create an expense
        Expense expense = Expense.builder()
                .amount(new BigDecimal("25.00"))
                .date(LocalDate.now())
                .category(testCategory)
                .description("Retry mechanism test")
                .synced(false)
                .build();

        expense = expenseRepository.save(expense);

        // Create a failed sync operation manually
        SyncOperation failedOperation = SyncOperation.builder()
                .operationType(SyncOperationType.CREATE)
                .entityType("Expense")
                .entityId(expense.getId())
                .status(SyncStatus.FAILED)
                .attemptCount(1)
                .errorMessage("Simulated failure")
                .nextRetryAt(LocalDateTime.now().minusMinutes(1)) // Ready for retry
                .build();

        syncOperationRepository.save(failedOperation);

        // Process the failed operation (should retry)
        syncService.processSyncOperation(failedOperation);

        // Verify the operation was retried and completed
        SyncOperation retriedOperation = syncOperationRepository.findById(failedOperation.getId()).orElse(null);
        assertThat(retriedOperation).isNotNull();
        assertThat(retriedOperation.getAttemptCount()).isEqualTo(2);
        assertThat(retriedOperation.getStatus()).isEqualTo(SyncStatus.COMPLETED);
    }

    @Test
    void testGetExpensesFromSheet() {
        LocalDate testDate = LocalDate.now();
        
        // Test retrieving expenses from Google Sheets (should return empty list when disabled)
        List<Expense> expenses = googleSheetsService.getExpensesFromSheet(testDate.getYear(), testDate.getMonthValue());
        
        assertThat(expenses).isNotNull();
        assertThat(expenses).isEmpty(); // Should be empty when Google Sheets is disabled
    }
}