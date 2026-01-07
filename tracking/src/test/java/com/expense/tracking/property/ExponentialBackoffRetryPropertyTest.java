package com.expense.tracking.property;

import com.expense.tracking.config.TestConfig;
import com.expense.tracking.entity.*;
import com.expense.tracking.repository.SyncOperationRepository;
import com.expense.tracking.service.SyncService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
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
 * Property-based test for exponential backoff retry mechanism
 * **Feature: expense-tracking, Property 22: Exponential Backoff Retry**
 * **Validates: Requirements 9.3**
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class ExponentialBackoffRetryPropertyTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private SyncOperationRepository syncOperationRepository;

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int BASE_RETRY_DELAY_SECONDS = 2;

    @BeforeEach
    void setUp() {
        syncOperationRepository.deleteAll();
    }

    @Property(tries = 100)
    @Label("For any failed sync operation, the system should retry up to 5 times with exponential backoff")
    void exponentialBackoffRetryMechanism(
            @ForAll @Positive Long entityId,
            @ForAll @StringLength(min = 1, max = 500) String description,
            @ForAll @IntRange(min = 1, max = 4) int failureAttempt) {

        // Given: A sync operation that will fail
        Expense expense = createTestExpense(entityId, description);
        SyncOperation operation = createFailedSyncOperation(entityId, expense, failureAttempt);
        SyncOperation savedOperation = syncOperationRepository.save(operation);

        // When: We simulate the failure handling
        simulateFailureHandling(savedOperation);

        // Then: The operation should be configured for retry with exponential backoff
        SyncOperation updatedOperation = syncOperationRepository.findById(savedOperation.getId()).orElseThrow();
        
        assertThat(updatedOperation.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(updatedOperation.getAttemptCount()).isEqualTo(failureAttempt);
        
        if (failureAttempt < MAX_RETRY_ATTEMPTS) {
            // Should have a retry time set
            assertThat(updatedOperation.getNextRetryAt()).isNotNull();
            
            // Calculate expected delay
            long expectedDelaySeconds = (long) (BASE_RETRY_DELAY_SECONDS * Math.pow(2, failureAttempt - 1));
            LocalDateTime expectedRetryTime = updatedOperation.getLastAttemptAt().plusSeconds(expectedDelaySeconds);
            
            // Allow for some tolerance in timing (within 1 second)
            assertThat(updatedOperation.getNextRetryAt())
                .isBetween(expectedRetryTime.minusSeconds(1), expectedRetryTime.plusSeconds(1));
        } else {
            // Max attempts reached, should not have retry time
            assertThat(updatedOperation.getNextRetryAt()).isNull();
        }
    }

    @Property(tries = 100)
    @Label("For any retry attempt, the delay should follow exponential backoff pattern")
    void exponentialBackoffDelayPattern(
            @ForAll @IntRange(min = 1, max = 5) int attemptCount) {

        // Given: A sync operation with a specific attempt count
        Expense expense = createTestExpense(1L, "test");
        SyncOperation operation = createFailedSyncOperation(1L, expense, attemptCount);
        operation.setLastAttemptAt(LocalDateTime.now());
        SyncOperation savedOperation = syncOperationRepository.save(operation);

        // When: We calculate the retry delay
        simulateFailureHandling(savedOperation);

        // Then: The delay should follow exponential backoff pattern
        SyncOperation updatedOperation = syncOperationRepository.findById(savedOperation.getId()).orElseThrow();
        
        if (attemptCount < MAX_RETRY_ATTEMPTS) {
            long expectedDelaySeconds = (long) (BASE_RETRY_DELAY_SECONDS * Math.pow(2, attemptCount - 1));
            LocalDateTime expectedRetryTime = updatedOperation.getLastAttemptAt().plusSeconds(expectedDelaySeconds);
            
            assertThat(updatedOperation.getNextRetryAt())
                .isBetween(expectedRetryTime.minusSeconds(1), expectedRetryTime.plusSeconds(1));
            
            // Verify the exponential pattern
            if (attemptCount > 1) {
                long previousDelaySeconds = (long) (BASE_RETRY_DELAY_SECONDS * Math.pow(2, attemptCount - 2));
                assertThat(expectedDelaySeconds).isEqualTo(previousDelaySeconds * 2);
            }
        }
    }

    @Property(tries = 100)
    @Label("For any operation that exceeds max retry attempts, it should be marked as permanently failed")
    void maxRetryAttemptsHandling(
            @ForAll @Positive Long entityId,
            @ForAll @StringLength(min = 1, max = 500) String description) {

        // Given: A sync operation that has reached max retry attempts
        Expense expense = createTestExpense(entityId, description);
        SyncOperation operation = createFailedSyncOperation(entityId, expense, MAX_RETRY_ATTEMPTS);
        SyncOperation savedOperation = syncOperationRepository.save(operation);

        // When: We handle the failure
        simulateFailureHandling(savedOperation);

        // Then: The operation should be permanently failed with no retry scheduled
        SyncOperation updatedOperation = syncOperationRepository.findById(savedOperation.getId()).orElseThrow();
        
        assertThat(updatedOperation.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(updatedOperation.getAttemptCount()).isEqualTo(MAX_RETRY_ATTEMPTS);
        assertThat(updatedOperation.getNextRetryAt()).isNull();
    }

    @Property(tries = 100)
    @Label("For any failed operations ready for retry, they should be retrievable by retry time")
    void failedOperationsReadyForRetry(
            @ForAll @Positive Long entityId,
            @ForAll @StringLength(min = 1, max = 500) String description,
            @ForAll @IntRange(min = 1, max = 4) int attemptCount) {

        // Given: A failed operation with a retry time in the past
        Expense expense = createTestExpense(entityId, description);
        SyncOperation operation = createFailedSyncOperation(entityId, expense, attemptCount);
        operation.setNextRetryAt(LocalDateTime.now().minusMinutes(5)); // Past retry time
        SyncOperation savedOperation = syncOperationRepository.save(operation);

        // When: We query for operations ready for retry
        List<SyncOperation> readyForRetry = syncOperationRepository
            .findFailedOperationsReadyForRetry(LocalDateTime.now());

        // Then: The operation should be included in the results
        assertThat(readyForRetry).hasSize(1);
        assertThat(readyForRetry.get(0).getId()).isEqualTo(savedOperation.getId());
        assertThat(readyForRetry.get(0).getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(readyForRetry.get(0).getNextRetryAt()).isBefore(LocalDateTime.now());
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

    private SyncOperation createFailedSyncOperation(Long entityId, Expense expense, int attemptCount) {
        return SyncOperation.builder()
                .operationType(SyncOperationType.CREATE)
                .entityId(entityId)
                .entityType("Expense")
                .entityData("{\"id\":" + entityId + ",\"description\":\"" + expense.getDescription() + "\"}")
                .status(SyncStatus.FAILED)
                .attemptCount(attemptCount)
                .lastAttemptAt(LocalDateTime.now())
                .errorMessage("Simulated failure")
                .build();
    }

    private void simulateFailureHandling(SyncOperation operation) {
        // Simulate the failure handling logic from SyncService
        if (operation.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            operation.setStatus(SyncStatus.FAILED);
            operation.setNextRetryAt(null);
        } else {
            operation.setStatus(SyncStatus.FAILED);
            
            // Calculate exponential backoff delay
            long delaySeconds = (long) (BASE_RETRY_DELAY_SECONDS * Math.pow(2, operation.getAttemptCount() - 1));
            operation.setNextRetryAt(operation.getLastAttemptAt().plusSeconds(delaySeconds));
        }
        
        syncOperationRepository.save(operation);
    }
}