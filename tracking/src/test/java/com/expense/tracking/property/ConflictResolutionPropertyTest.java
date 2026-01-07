package com.expense.tracking.property;

import com.expense.tracking.config.TestConfig;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.service.SyncService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.time.api.DateTimes;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for conflict resolution by timestamp
 * **Feature: expense-tracking, Property 21: Conflict Resolution by Timestamp**
 * **Validates: Requirements 9.2**
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class ConflictResolutionPropertyTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
    }

    @Property(tries = 100)
    @Label("For any sync conflict between local and remote data, the entry with the most recent timestamp should take precedence")
    void conflictResolutionByTimestamp(
            @ForAll @Positive Long expenseId,
            @ForAll @StringLength(min = 1, max = 500) String localDescription,
            @ForAll @StringLength(min = 1, max = 500) String remoteDescription,
            @ForAll("localTimestamp") LocalDateTime localTimestamp,
            @ForAll("remoteTimestamp") LocalDateTime remoteTimestamp) {

        // Given: Two versions of the same expense with different timestamps
        Expense localExpense = createTestExpense(expenseId, localDescription, localTimestamp);
        Expense remoteExpense = createTestExpense(expenseId, remoteDescription, remoteTimestamp);
        
        // Save the local expense first
        Expense savedLocalExpense = expenseRepository.save(localExpense);

        // When: We handle the sync conflict
        syncService.handleSyncConflict(savedLocalExpense, remoteExpense);

        // Then: The expense with the more recent timestamp should be preserved
        Optional<Expense> resultExpense = expenseRepository.findById(expenseId);
        assertThat(resultExpense).isPresent();
        
        Expense finalExpense = resultExpense.get();
        
        if (localTimestamp.isAfter(remoteTimestamp)) {
            // Local version should be kept
            assertThat(finalExpense.getDescription()).isEqualTo(localDescription);
            assertThat(finalExpense.getUpdatedAt()).isEqualTo(localTimestamp);
            assertThat(finalExpense.getSynced()).isFalse(); // Should be queued for sync
        } else {
            // Remote version should be applied
            assertThat(finalExpense.getDescription()).isEqualTo(remoteDescription);
            assertThat(finalExpense.getUpdatedAt()).isEqualTo(remoteTimestamp);
            assertThat(finalExpense.getSynced()).isTrue(); // Should be marked as synced
        }
    }

    @Property(tries = 100)
    @Label("For any conflict where timestamps are equal, local version should be preserved")
    void conflictResolutionWithEqualTimestamps(
            @ForAll @Positive Long expenseId,
            @ForAll @StringLength(min = 1, max = 500) String localDescription,
            @ForAll @StringLength(min = 1, max = 500) String remoteDescription,
            @ForAll("timestamp") LocalDateTime timestamp) {

        // Given: Two versions of the same expense with identical timestamps
        Expense localExpense = createTestExpense(expenseId, localDescription, timestamp);
        Expense remoteExpense = createTestExpense(expenseId, remoteDescription, timestamp);
        
        // Save the local expense first
        Expense savedLocalExpense = expenseRepository.save(localExpense);

        // When: We handle the sync conflict
        syncService.handleSyncConflict(savedLocalExpense, remoteExpense);

        // Then: The local version should be preserved (tie-breaker)
        Optional<Expense> resultExpense = expenseRepository.findById(expenseId);
        assertThat(resultExpense).isPresent();
        
        Expense finalExpense = resultExpense.get();
        assertThat(finalExpense.getDescription()).isEqualTo(localDescription);
        assertThat(finalExpense.getSynced()).isFalse(); // Should be queued for sync
    }

    @Property(tries = 100)
    @Label("For any conflict resolution, all expense fields should be updated consistently")
    void conflictResolutionUpdatesAllFields(
            @ForAll @Positive Long expenseId,
            @ForAll("localExpenseData") ExpenseData localData,
            @ForAll("remoteExpenseData") ExpenseData remoteData) {

        // Given: Two complete expense versions with different data
        Expense localExpense = createExpenseFromData(expenseId, localData);
        Expense remoteExpense = createExpenseFromData(expenseId, remoteData);
        
        // Save the local expense first
        Expense savedLocalExpense = expenseRepository.save(localExpense);

        // When: We handle the sync conflict
        syncService.handleSyncConflict(savedLocalExpense, remoteExpense);

        // Then: All fields should be updated consistently based on timestamp precedence
        Optional<Expense> resultExpense = expenseRepository.findById(expenseId);
        assertThat(resultExpense).isPresent();
        
        Expense finalExpense = resultExpense.get();
        
        if (localData.updatedAt.isAfter(remoteData.updatedAt)) {
            // Local version should be preserved
            assertThat(finalExpense.getAmount()).isEqualTo(localData.amount);
            assertThat(finalExpense.getDate()).isEqualTo(localData.date);
            assertThat(finalExpense.getDescription()).isEqualTo(localData.description);
            assertThat(finalExpense.getUpdatedAt()).isEqualTo(localData.updatedAt);
        } else {
            // Remote version should be applied
            assertThat(finalExpense.getAmount()).isEqualTo(remoteData.amount);
            assertThat(finalExpense.getDate()).isEqualTo(remoteData.date);
            assertThat(finalExpense.getDescription()).isEqualTo(remoteData.description);
            assertThat(finalExpense.getUpdatedAt()).isEqualTo(remoteData.updatedAt);
        }
    }

    @Provide
    Arbitrary<LocalDateTime> localTimestamp() {
        return DateTimes.dateTimes()
                .between(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2030, 12, 31, 23, 59));
    }

    @Provide
    Arbitrary<LocalDateTime> remoteTimestamp() {
        return DateTimes.dateTimes()
                .between(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2030, 12, 31, 23, 59));
    }

    @Provide
    Arbitrary<LocalDateTime> timestamp() {
        return DateTimes.dateTimes()
                .between(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2030, 12, 31, 23, 59));
    }

    @Provide
    Arbitrary<ExpenseData> localExpenseData() {
        return Combinators.combine(
                Arbitraries.bigDecimals().between(BigDecimal.ONE, BigDecimal.valueOf(10000)),
                Arbitraries.of(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31)),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(100),
                DateTimes.dateTimes().between(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2030, 12, 31, 23, 59))
        ).as(ExpenseData::new);
    }

    @Provide
    Arbitrary<ExpenseData> remoteExpenseData() {
        return Combinators.combine(
                Arbitraries.bigDecimals().between(BigDecimal.ONE, BigDecimal.valueOf(10000)),
                Arbitraries.of(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31)),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(100),
                DateTimes.dateTimes().between(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2030, 12, 31, 23, 59))
        ).as(ExpenseData::new);
    }

    private Expense createTestExpense(Long id, String description, LocalDateTime updatedAt) {
        return Expense.builder()
                .id(id)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .description(description)
                .createdAt(updatedAt.minusHours(1))
                .updatedAt(updatedAt)
                .synced(false)
                .build();
    }

    private Expense createExpenseFromData(Long id, ExpenseData data) {
        return Expense.builder()
                .id(id)
                .amount(data.amount)
                .date(data.date)
                .description(data.description)
                .createdAt(data.updatedAt.minusHours(1))
                .updatedAt(data.updatedAt)
                .synced(false)
                .build();
    }

    private static class ExpenseData {
        final BigDecimal amount;
        final LocalDate date;
        final String description;
        final LocalDateTime updatedAt;

        ExpenseData(BigDecimal amount, LocalDate date, String description, LocalDateTime updatedAt) {
            this.amount = amount;
            this.date = date;
            this.description = description;
            this.updatedAt = updatedAt;
        }
    }
}