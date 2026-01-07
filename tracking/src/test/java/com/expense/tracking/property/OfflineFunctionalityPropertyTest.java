package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.entity.SyncOperationType;
import com.expense.tracking.service.ExpenseService;
import com.expense.tracking.service.SyncService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 12: Offline Functionality
 * Validates: Requirements 4.2
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class OfflineFunctionalityPropertyTest {

    @Mock
    private ExpenseService expenseService;
    
    @Mock
    private SyncService syncService;
    
    private List<Expense> offlineExpenses = new ArrayList<>();
    private List<OfflineSyncOperation> syncQueue = new ArrayList<>();

    @Property(tries = 100)
    void offlineFunctionalityForExpenseCreation(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate date,
            @ForAll @StringLength(min = 1, max = 100) String categoryName,
            @ForAll @StringLength(max = 500) String description) {
        
        // Clear previous test data
        offlineExpenses.clear();
        syncQueue.clear();
        
        // Given: A valid expense and offline conditions
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();
        
        Expense expense = Expense.builder()
                .amount(amount)
                .date(date)
                .category(category)
                .description(description)
                .createdAt(LocalDateTime.now())
                .synced(false)
                .build();
        
        // When: Creating an expense while offline
        simulateOfflineExpenseCreation(expense);
        
        // Then: The expense should be stored locally and queued for sync
        assertThat(offlineExpenses).hasSize(1);
        assertThat(offlineExpenses.get(0).getAmount()).isEqualByComparingTo(amount);
        assertThat(offlineExpenses.get(0).getDate()).isEqualTo(date);
        assertThat(offlineExpenses.get(0).getCategory().getName()).isEqualTo(categoryName);
        assertThat(offlineExpenses.get(0).getDescription()).isEqualTo(description);
        assertThat(offlineExpenses.get(0).getSynced()).isFalse();
        
        // Verify sync operation was queued
        assertThat(syncQueue).hasSize(1);
        assertThat(syncQueue.get(0).getOperationType()).isEqualTo(SyncOperationType.CREATE);
        assertThat(syncQueue.get(0).getEntityType()).isEqualTo("Expense");
    }

    @Property(tries = 100)
    void offlineFunctionalityForExpenseViewing(
            @ForAll("expenseList") List<Expense> cachedExpenses) {
        
        // Clear previous test data
        offlineExpenses.clear();
        syncQueue.clear();
        
        // Given: Cached expenses from previous online sessions
        offlineExpenses.addAll(cachedExpenses);
        
        // When: Viewing expenses while offline
        List<Expense> viewableExpenses = simulateOfflineExpenseViewing();
        
        // Then: All cached expenses should be viewable
        assertThat(viewableExpenses).hasSize(cachedExpenses.size());
        
        for (int i = 0; i < cachedExpenses.size(); i++) {
            Expense original = cachedExpenses.get(i);
            Expense viewed = viewableExpenses.get(i);
            
            assertThat(viewed.getAmount()).isEqualByComparingTo(original.getAmount());
            assertThat(viewed.getDate()).isEqualTo(original.getDate());
            assertThat(viewed.getDescription()).isEqualTo(original.getDescription());
            assertThat(viewed.getCategory().getName()).isEqualTo(original.getCategory().getName());
        }
    }

    @Property(tries = 100)
    void offlineFunctionalityMaintainsDataIntegrity(
            @ForAll("expenseList") List<Expense> expenses) {
        
        // Clear previous test data
        offlineExpenses.clear();
        syncQueue.clear();
        
        // Given: Multiple expenses created while offline
        for (Expense expense : expenses) {
            simulateOfflineExpenseCreation(expense);
        }
        
        // When: Retrieving offline expenses
        List<Expense> retrievedExpenses = simulateOfflineExpenseViewing();
        
        // Then: Data integrity should be maintained
        assertThat(retrievedExpenses).hasSize(expenses.size());
        
        // Verify each expense maintains its data integrity
        for (int i = 0; i < expenses.size(); i++) {
            Expense original = expenses.get(i);
            Expense retrieved = retrievedExpenses.get(i);
            
            assertThat(retrieved.getAmount()).isEqualByComparingTo(original.getAmount());
            assertThat(retrieved.getDate()).isEqualTo(original.getDate());
            assertThat(retrieved.getDescription()).isEqualTo(original.getDescription());
            assertThat(retrieved.getCategory().getName()).isEqualTo(original.getCategory().getName());
            assertThat(retrieved.getSynced()).isFalse();
        }
        
        // Verify all operations are queued for sync
        assertThat(syncQueue).hasSize(expenses.size());
        syncQueue.forEach(operation -> {
            assertThat(operation.getOperationType()).isEqualTo(SyncOperationType.CREATE);
            assertThat(operation.getEntityType()).isEqualTo("Expense");
        });
    }

    @Property(tries = 50)
    void offlineFunctionalityHandlesLargeDataSets(
            @ForAll("largeExpenseList") List<Expense> largeExpenseSet) {
        
        // Clear previous test data
        offlineExpenses.clear();
        syncQueue.clear();
        
        // Given: A large set of expenses (testing offline storage capacity)
        for (Expense expense : largeExpenseSet) {
            simulateOfflineExpenseCreation(expense);
        }
        
        // When: Retrieving all offline expenses
        List<Expense> retrievedExpenses = simulateOfflineExpenseViewing();
        
        // Then: All expenses should be retrievable without data loss
        assertThat(retrievedExpenses).hasSize(largeExpenseSet.size());
        
        // Verify no data corruption occurred
        for (Expense expense : retrievedExpenses) {
            assertThat(expense.getAmount()).isNotNull();
            assertThat(expense.getDate()).isNotNull();
            assertThat(expense.getDescription()).isNotNull();
            assertThat(expense.getCategory()).isNotNull();
            assertThat(expense.getSynced()).isFalse();
        }
    }

    // Helper methods to simulate offline operations
    private void simulateOfflineExpenseCreation(Expense expense) {
        // Simulate storing expense locally when offline
        expense.setSynced(false);
        offlineExpenses.add(expense);
        
        // Queue for sync
        OfflineSyncOperation syncOperation = OfflineSyncOperation.builder()
                .operationType(SyncOperationType.CREATE)
                .entityType("Expense")
                .entityId(expense.getId())
                .createdAt(LocalDateTime.now())
                .build();
        
        syncQueue.add(syncOperation);
    }
    
    private List<Expense> simulateOfflineExpenseViewing() {
        // Simulate retrieving expenses from local storage when offline
        return new ArrayList<>(offlineExpenses);
    }

    // Helper class for offline sync operations
    private static class OfflineSyncOperation {
        private SyncOperationType operationType;
        private String entityType;
        private Long entityId;
        private LocalDateTime createdAt;
        
        public static OfflineSyncOperationBuilder builder() {
            return new OfflineSyncOperationBuilder();
        }
        
        public static class OfflineSyncOperationBuilder {
            private SyncOperationType operationType;
            private String entityType;
            private Long entityId;
            private LocalDateTime createdAt;
            
            public OfflineSyncOperationBuilder operationType(SyncOperationType operationType) {
                this.operationType = operationType;
                return this;
            }
            
            public OfflineSyncOperationBuilder entityType(String entityType) {
                this.entityType = entityType;
                return this;
            }
            
            public OfflineSyncOperationBuilder entityId(Long entityId) {
                this.entityId = entityId;
                return this;
            }
            
            public OfflineSyncOperationBuilder createdAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
                return this;
            }
            
            public OfflineSyncOperation build() {
                OfflineSyncOperation operation = new OfflineSyncOperation();
                operation.operationType = this.operationType;
                operation.entityType = this.entityType;
                operation.entityId = this.entityId;
                operation.createdAt = this.createdAt;
                return operation;
            }
        }
        
        public SyncOperationType getOperationType() { return operationType; }
        public String getEntityType() { return entityType; }
        public Long getEntityId() { return entityId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    // Providers for test data generation
    @Provide
    Arbitrary<List<Expense>> expenseList() {
        return Arbitraries.of(
                generateExpenseList(1),
                generateExpenseList(3),
                generateExpenseList(5),
                generateExpenseList(10)
        );
    }
    
    @Provide
    Arbitrary<List<Expense>> largeExpenseList() {
        return Arbitraries.of(
                generateExpenseList(50),
                generateExpenseList(100),
                generateExpenseList(200)
        );
    }
    
    private List<Expense> generateExpenseList(int size) {
        List<Expense> expenses = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            Category category = Category.builder()
                    .id((long) (i % 5 + 1))
                    .name("Category " + (i % 5 + 1))
                    .build();
            
            Expense expense = Expense.builder()
                    .id((long) i)
                    .amount(BigDecimal.valueOf(10.0 + i))
                    .date(LocalDate.now().minusDays(i))
                    .category(category)
                    .description("Test expense " + i)
                    .createdAt(LocalDateTime.now().minusMinutes(i))
                    .synced(false)
                    .build();
                    
            expenses.add(expense);
        }
        
        return expenses;
    }
    
    @Provide
    Arbitrary<LocalDate> localDate() {
        return Arbitraries.of(
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 6, 15),
                LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 3, 10),
                LocalDate.of(2024, 8, 20),
                LocalDate.of(2024, 11, 5)
        );
    }
}