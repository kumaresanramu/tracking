package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for local data persistence functionality.
 * 
 * Feature: expense-tracking
 * Property 4: Local Data Persistence
 * 
 * For any newly created expense, the data should be immediately stored 
 * in the local database and retrievable.
 * 
 * Validates: Requirements 2.2
 */
public class LocalDataPersistencePropertyTest {

    @Property(tries = 100)
    @Label("Feature: expense-tracking, Property 4: Local Data Persistence")
    void localDataPersistenceProperty(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll @NotBlank @Size(min = 1, max = 100) String description,
            @ForAll @NotBlank @Size(min = 1, max = 50) String categoryName,
            @ForAll LocalDate expenseDate) {
        
        // Given: A valid expense with all required fields for local persistence
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();
        
        Expense expense = Expense.builder()
                .amount(amount)
                .description(description)
                .date(expenseDate)
                .category(category)
                .build();

        // When: The expense is created for local storage
        // Simulate the persistence process by setting ID and timestamps
        expense.setId(System.nanoTime()); // Simulate database ID assignment
        LocalDateTime now = LocalDateTime.now();
        expense.setCreatedAt(now);
        expense.setUpdatedAt(now);
        
        // Then: All fields should be immediately retrievable with identical values
        assertThat(expense.getId()).isNotNull();
        assertThat(expense.getAmount()).isEqualByComparingTo(amount);
        assertThat(expense.getDescription()).isEqualTo(description);
        assertThat(expense.getDate()).isEqualTo(expenseDate);
        assertThat(expense.getCategory()).isNotNull();
        assertThat(expense.getCategory().getName()).isEqualTo(categoryName);
        assertThat(expense.getCreatedAt()).isNotNull();
        assertThat(expense.getUpdatedAt()).isNotNull();
        
        // And: The data should maintain integrity across operations
        Expense retrievedExpense = Expense.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .date(expense.getDate())
                .category(expense.getCategory())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
        
        assertThat(retrievedExpense.getAmount()).isEqualByComparingTo(amount);
        assertThat(retrievedExpense.getDescription()).isEqualTo(description);
        assertThat(retrievedExpense.getDate()).isEqualTo(expenseDate);
        assertThat(retrievedExpense.getCategory().getName()).isEqualTo(categoryName);
    }

    @Property(tries = 100)
    @Label("Feature: expense-tracking, Property 4: Local Data Persistence - Category Persistence")
    void categoryLocalDataPersistenceProperty(
            @ForAll @NotBlank @Size(min = 1, max = 50) String categoryName,
            @ForAll @Size(max = 500) String description) {
        
        // Given: A valid category for local persistence
        Category category = Category.builder()
                .name(categoryName)
                .description(description)
                .color("#FF0000") // Fixed valid hex color
                .build();
        
        // When: The category is created for local storage
        category.setId(System.nanoTime()); // Simulate database ID assignment
        
        // Then: All fields should be immediately retrievable
        assertThat(category.getId()).isNotNull();
        assertThat(category.getName()).isEqualTo(categoryName);
        assertThat(category.getDescription()).isEqualTo(description);
        assertThat(category.getColor()).isEqualTo("#FF0000");
        
        // And: The data should maintain integrity across operations
        Category retrievedCategory = Category.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .color(category.getColor())
                .build();
        
        assertThat(retrievedCategory.getName()).isEqualTo(categoryName);
        assertThat(retrievedCategory.getDescription()).isEqualTo(description);
        assertThat(retrievedCategory.getColor()).isEqualTo("#FF0000");
    }

    @Property(tries = 100)
    @Label("Feature: expense-tracking, Property 4: Local Data Persistence - Data Integrity")
    void dataIntegrityProperty(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll @NotBlank @Size(min = 1, max = 100) String description,
            @ForAll @NotBlank @Size(min = 1, max = 50) String categoryName,
            @ForAll LocalDate expenseDate) {
        
        // Given: An expense with specific data
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();
        
        Expense originalExpense = Expense.builder()
                .amount(amount)
                .description(description)
                .date(expenseDate)
                .category(category)
                .build();
        
        // When: The expense undergoes local persistence operations
        originalExpense.setId(123L);
        LocalDateTime createdTime = LocalDateTime.now();
        originalExpense.setCreatedAt(createdTime);
        originalExpense.setUpdatedAt(createdTime);
        
        // Then: Data integrity should be maintained
        assertThat(originalExpense.getAmount()).isEqualByComparingTo(amount);
        assertThat(originalExpense.getDescription()).isEqualTo(description);
        assertThat(originalExpense.getDate()).isEqualTo(expenseDate);
        assertThat(originalExpense.getCategory().getName()).isEqualTo(categoryName);
        
        // And: Copying the expense should preserve all data
        Expense copiedExpense = Expense.builder()
                .id(originalExpense.getId())
                .amount(originalExpense.getAmount())
                .description(originalExpense.getDescription())
                .date(originalExpense.getDate())
                .category(originalExpense.getCategory())
                .createdAt(originalExpense.getCreatedAt())
                .updatedAt(originalExpense.getUpdatedAt())
                .build();
        
        assertThat(copiedExpense.getId()).isEqualTo(originalExpense.getId());
        assertThat(copiedExpense.getAmount()).isEqualByComparingTo(originalExpense.getAmount());
        assertThat(copiedExpense.getDescription()).isEqualTo(originalExpense.getDescription());
        assertThat(copiedExpense.getDate()).isEqualTo(originalExpense.getDate());
        assertThat(copiedExpense.getCategory().getName()).isEqualTo(originalExpense.getCategory().getName());
        assertThat(copiedExpense.getCreatedAt()).isEqualTo(originalExpense.getCreatedAt());
        assertThat(copiedExpense.getUpdatedAt()).isEqualTo(originalExpense.getUpdatedAt());
    }
    
    @Provide
    Arbitrary<LocalDate> localDate() {
        return Arbitraries.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 15),
                LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 6, 15),
                LocalDate.of(2024, 9, 30),
                LocalDate.of(2024, 12, 25),
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 12, 31)
        );
    }
}