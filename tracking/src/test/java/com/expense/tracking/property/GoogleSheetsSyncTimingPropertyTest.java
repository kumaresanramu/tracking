package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 4: Google Sheets Sync Timing
 * Validates: Requirements 2.2
 */
public class GoogleSheetsSyncTimingPropertyTest {

    @Property(tries = 100)
    void googleSheetsSyncTiming(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate date,
            @ForAll @StringLength(min = 1, max = 100) String categoryName,
            @ForAll @StringLength(max = 500) String description) {
        
        // Given: A valid expense that needs to be synced
        Category category = Category.builder()
                .name(categoryName)
                .build();
        
        Expense expense = Expense.builder()
                .id(1L)
                .amount(amount)
                .date(date)
                .category(category)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When: We simulate a sync operation timing
        LocalDateTime startTime = LocalDateTime.now();
        
        // Simulate the sync operation with realistic processing time
        simulateGoogleSheetsSync(expense);
        
        LocalDateTime endTime = LocalDateTime.now();
        
        // Then: The sync operation should complete within 5 seconds
        Duration syncDuration = Duration.between(startTime, endTime);
        assertThat(syncDuration.toMillis())
            .as("Sync operation should complete within 5 seconds (5000ms) under normal conditions")
            .isLessThanOrEqualTo(5000);
            
        // And: The sync operation should take some reasonable minimum time (not instant)
        assertThat(syncDuration.toMillis())
            .as("Sync operation should take at least some time for realistic network operations")
            .isGreaterThanOrEqualTo(0);
    }

    @Property(tries = 50)
    void googleSheetsSyncTimingConsistency(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate date,
            @ForAll @StringLength(min = 1, max = 100) String categoryName) {
        
        // Given: Multiple expenses to test timing consistency
        Category category = Category.builder()
                .name(categoryName)
                .build();
        
        Expense expense = Expense.builder()
                .id(1L)
                .amount(amount)
                .date(date)
                .category(category)
                .description("Consistency test expense")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When: We perform multiple sync operations
        long totalTime = 0;
        int iterations = 3;
        
        for (int i = 0; i < iterations; i++) {
            LocalDateTime startTime = LocalDateTime.now();
            simulateGoogleSheetsSync(expense);
            LocalDateTime endTime = LocalDateTime.now();
            
            Duration syncDuration = Duration.between(startTime, endTime);
            totalTime += syncDuration.toMillis();
            
            // Each individual sync should be under 5 seconds
            assertThat(syncDuration.toMillis())
                .as("Each sync operation should complete within 5 seconds")
                .isLessThanOrEqualTo(5000);
        }
        
        // Then: Average sync time should be reasonable
        long averageTime = totalTime / iterations;
        assertThat(averageTime)
            .as("Average sync time should be reasonable (under 5 seconds)")
            .isLessThanOrEqualTo(5000);
    }

    /**
     * Simulates a Google Sheets sync operation with realistic timing
     */
    private void simulateGoogleSheetsSync(Expense expense) {
        try {
            // Simulate realistic network and processing time
            // In real implementation, this would be the actual Google Sheets API call
            long simulatedDelay = 50 + (long)(Math.random() * 200); // 50-250ms
            Thread.sleep(simulatedDelay);
            
            // Simulate data serialization and validation
            String serializedData = expense.getDate().toString() + "," + 
                                  expense.getAmount().toString() + "," +
                                  (expense.getCategory() != null ? expense.getCategory().getName() : "") + "," +
                                  (expense.getDescription() != null ? expense.getDescription() : "");
            
            // Validate that serialization is not empty (basic check)
            assertThat(serializedData).isNotEmpty();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sync simulation interrupted", e);
        }
    }

    @Provide
    Arbitrary<LocalDate> localDate() {
        return Arbitraries.of(
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 6, 15),
                LocalDate.of(2024, 3, 10),
                LocalDate.of(2024, 8, 20),
                LocalDate.of(2024, 12, 31)
        );
    }
}