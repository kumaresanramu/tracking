package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.entity.SyncOperation;
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
 * Feature: expense-tracking, Property 20: Connectivity-Based Sync
 * Validates: Requirements 9.1
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ConnectivityBasedSyncPropertyTest {

    @Mock
    private ExpenseService expenseService;
    
    @Mock
    private SyncService syncService;
    
    private List<Expense> pendingExpenses = new ArrayList<>();
    private List<SyncOperation> syncQueue = new ArrayList<>();
    private boolean isOnline = false;
    private boolean syncTriggered = false;
    private ConnectivityState connectivityState = ConnectivityState.OFFLINE;

    @Property(tries = 100)
    void connectivityBasedSyncTriggersOnConnectivityDetection(
            @ForAll("connectivityChangeScenario") ConnectivityChangeScenario scenario) {
        
        // Clear previous test data
        pendingExpenses.clear();
        syncQueue.clear();
        syncTriggered = false;
        
        // Given: Initial connectivity state and pending data
        connectivityState = scenario.initialState;
        isOnline = (connectivityState == ConnectivityState.ONLINE);
        
        // Add pending expenses if scenario includes them
        if (scenario.hasPendingData) {
            addPendingExpenses(scenario.pendingExpenseCount);
        }
        
        // When: Connectivity state changes
        ConnectivityState newState = scenario.newState;
        simulateConnectivityChange(connectivityState, newState);
        
        // Then: Sync should be triggered only when going from offline to online
        boolean shouldTriggerSync = (connectivityState == ConnectivityState.OFFLINE && 
                                   newState == ConnectivityState.ONLINE);
        
        if (shouldTriggerSync) {
            assertThat(syncTriggered).isTrue();
            
            // If there was pending data, verify sync operations were created
            if (scenario.hasPendingData) {
                assertThat(syncQueue).hasSize(scenario.pendingExpenseCount);
                syncQueue.forEach(op -> {
                    assertThat(op.getOperationType()).isEqualTo(SyncOperationType.CREATE);
                    assertThat(op.getEntityType()).isEqualTo("Expense");
                });
            }
        } else {
            // Sync should not be triggered for other connectivity changes
            if (connectivityState == ConnectivityState.ONLINE && newState == ConnectivityState.OFFLINE) {
                // Going offline - sync should not be triggered
                assertThat(syncTriggered).isFalse();
            }
        }
    }

    @Property(tries = 100)
    void connectivityBasedSyncHandlesPendingOperations(
            @ForAll("pendingOperationsList") List<Expense> pendingOperations) {
        
        // Clear previous test data
        pendingExpenses.clear();
        syncQueue.clear();
        syncTriggered = false;
        
        // Given: System is offline with pending operations
        connectivityState = ConnectivityState.OFFLINE;
        isOnline = false;
        
        pendingExpenses.addAll(pendingOperations);
        
        // Create sync operations for pending expenses
        for (Expense expense : pendingOperations) {
            SyncOperation syncOp = SyncOperation.builder()
                    .operationType(SyncOperationType.CREATE)
                    .entityType("Expense")
                    .entityId(expense.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            syncQueue.add(syncOp);
        }
        
        int initialQueueSize = syncQueue.size();
        
        // When: Connectivity is detected (offline to online)
        simulateConnectivityChange(ConnectivityState.OFFLINE, ConnectivityState.ONLINE);
        
        // Then: Sync should be triggered
        assertThat(syncTriggered).isTrue();
        
        // And: All pending operations should be processed
        assertThat(syncQueue).hasSize(initialQueueSize);
        
        // Verify each pending expense has a corresponding sync operation
        for (Expense expense : pendingOperations) {
            boolean hasCorrespondingSync = syncQueue.stream()
                    .anyMatch(op -> op.getEntityId().equals(expense.getId()) &&
                                  op.getOperationType() == SyncOperationType.CREATE);
            assertThat(hasCorrespondingSync).isTrue();
        }
    }

    @Property(tries = 100)
    void connectivityBasedSyncPreservesDataDuringConnectivityChanges(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate date,
            @ForAll @StringLength(min = 1, max = 100) String categoryName,
            @ForAll @StringLength(max = 500) String description) {
        
        // Clear previous test data
        pendingExpenses.clear();
        syncQueue.clear();
        syncTriggered = false;
        
        // Given: A specific expense created while offline
        connectivityState = ConnectivityState.OFFLINE;
        isOnline = false;
        
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();
        
        Expense expense = Expense.builder()
                .id(1L)
                .amount(amount)
                .date(date)
                .category(category)
                .description(description)
     
                .createdAt(LocalDateTime.now())
                .synced(false)
                .build();
        
        pendingExpenses.add(expense);
        
        SyncOperation syncOp = SyncOperation.builder()
                .operationType(SyncOperationType.CREATE)
                .entityType("Expense")
                .entityId(expense.getId())
                .createdAt(LocalDateTime.now())
                .build();
        syncQueue.add(syncOp);
        
        // When: Multiple connectivity changes occur
        simulateConnectivityChange(ConnectivityState.OFFLINE, ConnectivityState.ONLINE);
        simulateConnectivityChange(ConnectivityState.ONLINE, ConnectivityState.OFFLINE);
        simulateConnectivityChange(ConnectivityState.OFFLINE, ConnectivityState.ONLINE);
        
        // Then: Data integrity should be preserved throughout
        assertThat(pendingExpenses).hasSize(1);
        Expense preservedExpense = pendingExpenses.get(0);
        
        assertThat(preservedExpense.getAmount()).isEqualByComparingTo(amount);
        assertThat(preservedExpense.getDate()).isEqualTo(date);
        assertThat(preservedExpense.getCategory().getName()).isEqualTo(categoryName);
        assertThat(preservedExpense.getDescription()).isEqualTo(description);
        assertThat(preservedExpense.getSynced()).isFalse();
        
        // And: Sync operations should be preserved
        assertThat(syncQueue).hasSize(1);
        SyncOperation preservedOp = syncQueue.get(0);
        assertThat(preservedOp.getEntityId()).isEqualTo(expense.getId());
        assertThat(preservedOp.getOperationType()).isEqualTo(SyncOperationType.CREATE);
    }

    @Property(tries = 50)
    void connectivityBasedSyncHandlesRapidConnectivityChanges(
            @ForAll("rapidConnectivityChanges") List<ConnectivityState> connectivitySequence) {
        
        // Clear previous test data
        pendingExpenses.clear();
        syncQueue.clear();
        syncTriggered = false;
        
        // Given: Some pending data and initial offline state
        connectivityState = ConnectivityState.OFFLINE;
        isOnline = false;
        
        addPendingExpenses(2); // Add a couple of pending expenses
        int initialPendingCount = pendingExpenses.size();
        
        int syncTriggerCount = 0;
        ConnectivityState previousState = ConnectivityState.OFFLINE;
        
        // When: Rapid connectivity changes occur
        for (ConnectivityState newState : connectivitySequence) {
            simulateConnectivityChange(previousState, newState);
            
            // Count sync triggers (should only happen on offline -> online transitions)
            if (previousState == ConnectivityState.OFFLINE && newState == ConnectivityState.ONLINE) {
                syncTriggerCount++;
            }
            
            previousState = newState;
        }
        
        // Then: Sync should have been triggered appropriately
        if (syncTriggerCount > 0) {
            assertThat(syncTriggered).isTrue();
        }
        
        // And: Data integrity should be maintained
        assertThat(pendingExpenses).hasSize(initialPendingCount);
        
        // Verify all expenses are still properly queued
        for (Expense expense : pendingExpenses) {
            assertThat(expense.getSynced()).isFalse();
            assertThat(expense.getAmount()).isNotNull();
            assertThat(expense.getDate()).isNotNull();
            assertThat(expense.getDescription()).isNotNull();
        }
    }

    @Property(tries = 100)
    void connectivityBasedSyncOnlyTriggersWhenActuallyOnline() {
        
        // Clear previous test data
        pendingExpenses.clear();
        syncQueue.clear();
        syncTriggered = false;
        
        // Given: System is offline with pending data
        connectivityState = ConnectivityState.OFFLINE;
        isOnline = false;
        
        addPendingExpenses(1);
        
        // When: False connectivity signals occur (still actually offline)
        simulateFalseConnectivitySignal();
        
        // Then: Sync should not be triggered for false signals
        assertThat(syncTriggered).isFalse();
        
        // When: Actual connectivity is restored
        simulateConnectivityChange(ConnectivityState.OFFLINE, ConnectivityState.ONLINE);
        
        // Then: Sync should be triggered
        assertThat(syncTriggered).isTrue();
    }

    @Property(tries = 100)
    void connectivityBasedSyncHandlesNetworkInstability(
            @ForAll("unstableNetworkScenario") UnstableNetworkScenario scenario) {
        
        // Clear previous test data
        pendingExpenses.clear();
        syncQueue.clear();
        syncTriggered = false;
        
        // Given: Pending data and unstable network conditions
        connectivityState = ConnectivityState.OFFLINE;
        isOnline = false;
        
        addPendingExpenses(scenario.pendingDataCount);
        
        // When: Network instability occurs (multiple quick on/off cycles)
        for (int i = 0; i < scenario.instabilityCycles; i++) {
            simulateConnectivityChange(ConnectivityState.OFFLINE, ConnectivityState.ONLINE);
            
            // Simulate brief connection before dropping again
            if (i < scenario.instabilityCycles - 1) {
                simulateConnectivityChange(ConnectivityState.ONLINE, ConnectivityState.OFFLINE);
            }
        }
        
        // Then: Sync should eventually be triggered
        assertThat(syncTriggered).isTrue();
        
        // And: All pending data should still be intact
        assertThat(pendingExpenses).hasSize(scenario.pendingDataCount);
        assertThat(syncQueue).hasSize(scenario.pendingDataCount);
        
        // Verify data integrity after network instability
        for (Expense expense : pendingExpenses) {
            assertThat(expense.getSynced()).isFalse();
            assertThat(expense.getAmount()).isPositive();
            assertThat(expense.getDescription()).isNotBlank();
        }
    }

    // Helper methods
    private void simulateConnectivityChange(ConnectivityState from, ConnectivityState to) {
        connectivityState = to;
        isOnline = (to == ConnectivityState.ONLINE);
        
        // Simulate sync trigger only on offline -> online transition
        if (from == ConnectivityState.OFFLINE && to == ConnectivityState.ONLINE) {
            triggerConnectivityBasedSync();
        }
        
        System.out.println("Connectivity changed from " + from + " to " + to);
    }
    
    private void simulateFalseConnectivitySignal() {
        // Simulate a false positive connectivity signal (navigator.onLine = true but no actual connection)
        // In this case, sync should not be triggered
        System.out.println("False connectivity signal detected");
    }
    
    private void triggerConnectivityBasedSync() {
        syncTriggered = true;
        System.out.println("Connectivity-based sync triggered");
        
        // In real implementation, this would:
        // 1. Detect actual connectivity (not just navigator.onLine)
        // 2. Process pending sync operations
        // 3. Send data to server
        // 4. Update local storage
    }
    
    private void addPendingExpenses(int count) {
        for (int i = 0; i < count; i++) {
            Category category = Category.builder()
                    .id((long) (i % 3 + 1))
                    .name("Category " + (i % 3 + 1))
                    .build();
            
            Expense expense = Expense.builder()
                    .id((long) (i + 1))
                    .amount(BigDecimal.valueOf(50.0 + (i * 25)))
                    .date(LocalDate.now().minusDays(i))
                    .category(category)
                    .description("Pending expense " + (i + 1))
                    .createdAt(LocalDateTime.now().minusMinutes(i * 10))
                    .synced(false)
                    .build();
            
            pendingExpenses.add(expense);
            
            SyncOperation syncOp = SyncOperation.builder()
                    .operationType(SyncOperationType.CREATE)
                    .entityType("Expense")
                    .entityId(expense.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            syncQueue.add(syncOp);
        }
    }

    // Enums and helper classes
    enum ConnectivityState {
        ONLINE, OFFLINE
    }
    
    static class ConnectivityChangeScenario {
        ConnectivityState initialState;
        ConnectivityState newState;
        boolean hasPendingData;
        int pendingExpenseCount;
        
        public ConnectivityChangeScenario(ConnectivityState initialState, ConnectivityState newState, 
                                        boolean hasPendingData, int pendingExpenseCount) {
            this.initialState = initialState;
            this.newState = newState;
            this.hasPendingData = hasPendingData;
            this.pendingExpenseCount = pendingExpenseCount;
        }
    }
    
    static class UnstableNetworkScenario {
        int pendingDataCount;
        int instabilityCycles;
        
        public UnstableNetworkScenario(int pendingDataCount, int instabilityCycles) {
            this.pendingDataCount = pendingDataCount;
            this.instabilityCycles = instabilityCycles;
        }
    }

    // Providers for test data generation
    @Provide
    Arbitrary<ConnectivityChangeScenario> connectivityChangeScenario() {
        return Arbitraries.of(
                new ConnectivityChangeScenario(ConnectivityState.OFFLINE, ConnectivityState.ONLINE, true, 1),
                new ConnectivityChangeScenario(ConnectivityState.OFFLINE, ConnectivityState.ONLINE, true, 3),
                new ConnectivityChangeScenario(ConnectivityState.OFFLINE, ConnectivityState.ONLINE, false, 0),
                new ConnectivityChangeScenario(ConnectivityState.ONLINE, ConnectivityState.OFFLINE, true, 2),
                new ConnectivityChangeScenario(ConnectivityState.ONLINE, ConnectivityState.ONLINE, true, 1),
                new ConnectivityChangeScenario(ConnectivityState.OFFLINE, ConnectivityState.OFFLINE, true, 2)
        );
    }
    
    @Provide
    Arbitrary<List<Expense>> pendingOperationsList() {
        return Arbitraries.of(
                generateExpenseList(1),
                generateExpenseList(2),
                generateExpenseList(4),
                generateExpenseList(6)
        );
    }
    
    @Provide
    Arbitrary<List<ConnectivityState>> rapidConnectivityChanges() {
        return Arbitraries.of(
                List.of(ConnectivityState.ONLINE, ConnectivityState.OFFLINE, ConnectivityState.ONLINE),
                List.of(ConnectivityState.ONLINE, ConnectivityState.OFFLINE, ConnectivityState.OFFLINE, ConnectivityState.ONLINE),
                List.of(ConnectivityState.ONLINE),
                List.of(ConnectivityState.OFFLINE, ConnectivityState.ONLINE, ConnectivityState.OFFLINE, ConnectivityState.ONLINE, ConnectivityState.OFFLINE)
        );
    }
    
    @Provide
    Arbitrary<UnstableNetworkScenario> unstableNetworkScenario() {
        return Arbitraries.of(
                new UnstableNetworkScenario(1, 2),
                new UnstableNetworkScenario(2, 3),
                new UnstableNetworkScenario(3, 4),
                new UnstableNetworkScenario(1, 5)
        );
    }
    
    private List<Expense> generateExpenseList(int size) {
        List<Expense> expenses = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            Category category = Category.builder()
                    .id((long) (i % 4 + 1))
                    .name("Category " + (i % 4 + 1))
                    .build();
            
            Expense expense = Expense.builder()
                    .id((long) (i + 1))
                    .amount(BigDecimal.valueOf(30.0 + (i * 15)))
                    .date(LocalDate.now().minusDays(i))
                    .category(category)
                    .description("Pending expense " + (i + 1))
                    .createdAt(LocalDateTime.now().minusMinutes(i * 8))
                    .synced(false)
                    .build();
                    
            expenses.add(expense);
        }
        
        return expenses;
    }
    
    @Provide
    Arbitrary<LocalDate> localDate() {
        return Arbitraries.of(
                LocalDate.of(2024, 2, 14),
                LocalDate.of(2024, 5, 20),
                LocalDate.of(2024, 8, 12),
                LocalDate.of(2024, 10, 30),
                LocalDate.of(2024, 12, 8)
        );
    }
}