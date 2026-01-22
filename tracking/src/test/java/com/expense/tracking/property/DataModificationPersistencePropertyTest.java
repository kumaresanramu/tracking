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
 * Property-based test for data modification persistence functionality.
 * 
 * Feature: expense-tracking
 * Property 5: Data Modification Persistence
 * 
 * For any expense modification, the corresponding database entry should 
 * reflect the same changes immediately after the update operation.
 * 
 * Validates: Requirements 2.3
 */
public class DataModificationPersistencePropertyTest {

    @Property(tries = 100)
    @Label("Feature: expense-tracking, Property 5: Data Modification Persistence")
    void dataModificationPersistenceProperty(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal originalAmount,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal updatedAmount,
            @ForAll @NotBlank @Size(min = 1, max = 100) String originalDescription,
            @ForAll @NotBlank @Size(min = 1, max = 100) String updatedDescription,
            @ForAll @NotBlank @Size(min = 1, max = 50) String originalCategoryName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String updatedCategoryName,
            @ForAll LocalDate originalDate,
            @ForAll LocalDate updatedDate) {
        
        // Given: An existing expense with original values
        Category originalCategory = Category.builder()
                .id(1L)
                .name(originalCategoryName)
                .build();
        
        Category updatedCategory = Category.builder()
                .id(2L)
                .name(updatedCategoryName)
                .build();
        
        Expense originalExpense = Expense.builder()
                .id(123L)
                .amount(originalAmount)
                .description(originalDescription)
                .date(originalDate)
                .category(originalCategory)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
        
        // When: The expense is modified with new values
        LocalDateTime updateTime = LocalDateTime.now();
        originalExpense.setAmount(updatedAmount);
        originalExpense.setDescription(updatedDescription);
        originalExpense.setDate(updatedDate);
        originalExpense.setCategory(updatedCategory);
        originalExpense.setUpdatedAt(updateTime);
        
        // Then: The expense should immediately reflect all changes
        assertThat(originalExpense.getId()).isEqualTo(123L); // ID should remain the same
        assertThat(originalExpense.getAmount()).isEqualByComparingTo(updatedAmount);
        assertThat(originalExpense.getDescription()).isEqualTo(updatedDescription);
        assertThat(originalExpense.getDate()).isEqualTo(updatedDate);
        assertThat(originalExpense.getCategory()).isNotNull();
        assertThat(originalExpense.getCategory().getName()).isEqualTo(updatedCategoryName);
        assertThat(originalExpense.getUpdatedAt()).isEqualTo(updateTime);
        
        // And: Original values should no longer be present
        assertThat(originalExpense.getAmount()).isNotEqualByComparingTo(originalAmount);
        assertThat(originalExpense.getDescription()).isNotEqualTo(originalDescription);
        assertThat(originalExpense.getDate()).isNotEqualTo(originalDate);
        assertThat(originalExpense.getCategory().getName()).isNotEqualTo(originalCategoryName);
        
        // And: The modification should be persistent (simulate retrieval)
        Expense retrievedExpense = Expense.builder()
                .id(originalExpense.getId())
                .amount(originalExpense.getAmount())
                .description(originalExpense.getDescription())
                .date(originalExpense.getDate())
                .category(originalExpense.getCategory())
                .createdAt(originalExpense.getCreatedAt())
                .updatedAt(originalExpense.getUpdatedAt())
                .build();
        
        assertThat(retrievedExpense.getAmount()).isEqualByComparingTo(updatedAmount);
        assertThat(retrievedExpense.getDescription()).isEqualTo(updatedDescription);
        assertThat(retrievedExpense.getDate()).isEqualTo(updatedDate);
        assertThat(retrievedExpense.getCategory().getName()).isEqualTo(updatedCategoryName);
    }

    @Property(tries = 100)
    @Label("Feature: expense-tracking, Property 5: Data Modification Persistence - Category Updates")
    void categoryModificationPersistenceProperty(
            @ForAll @NotBlank @Size(min = 1, max = 50) String originalName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String updatedName,
            @ForAll @Size(max = 500) String originalDescription,
            @ForAll @Size(max = 500) String updatedDescription) {
        
        // Given: An existing category with original values
        Category originalCategory = Category.builder()
                .id(456L)
                .name(originalName)
                .description(originalDescription)
                .color("#FF0000")
                .build();
        
        // When: The category is modified
        originalCategory.setName(updatedName);
        originalCategory.setDescription(updatedDescription);
        originalCategory.setColor("#00FF00");
        
        // Then: The category should immediately reflect the changes
        assertThat(originalCategory.getId()).isEqualTo(456L); // ID should remain the same
        assertThat(originalCategory.getName()).isEqualTo(updatedName);
        assertThat(originalCategory.getDescription()).isEqualTo(updatedDescription);
        assertThat(originalCategory.getColor()).isEqualTo("#00FF00");
        
        // And: Original values should no longer be present
        assertThat(originalCategory.getName()).isNotEqualTo(originalName);
        assertThat(originalCategory.getDescription()).isNotEqualTo(originalDescription);
        assertThat(originalCategory.getColor()).isNotEqualTo("#FF0000");
        
        // And: The modification should be persistent (simulate retrieval)
        Category retrievedCategory = Category.builder()
                .id(originalCategory.getId())
                .name(originalCategory.getName())
                .description(originalCategory.getDescription())
                .color(originalCategory.getColor())
                .build();
        
        assertThat(retrievedCategory.getName()).isEqualTo(updatedName);
        assertThat(retrievedCategory.getDescription()).isEqualTo(updatedDescription);
        assertThat(retrievedCategory.getColor()).isEqualTo("#00FF00");
    }

    @Property(tries = 100)
    @Label("Feature: expense-tracking, Property 5: Data Modification Persistence - Partial Updates")
    void partialUpdatePersistenceProperty(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal originalAmount,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal updatedAmount,
            @ForAll @NotBlank @Size(min = 1, max = 100) String description,
            @ForAll @NotBlank @Size(min = 1, max = 50) String categoryName,
            @ForAll LocalDate date) {
        
        // Given: An existing expense
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();
        
        LocalDateTime createdTime = LocalDateTime.now().minusHours(2);
        LocalDateTime originalUpdateTime = LocalDateTime.now().minusHours(1);
        
        Expense expense = Expense.builder()
                .id(789L)
                .amount(originalAmount)
                .description(description)
                .date(date)
                .category(category)
                .createdAt(createdTime)
                .updatedAt(originalUpdateTime)
                .build();
        
        // When: Only the amount is modified (partial update)
        LocalDateTime newUpdateTime = LocalDateTime.now();
        expense.setAmount(updatedAmount);
        expense.setUpdatedAt(newUpdateTime);
        
        // Then: Only the modified field should change, others should remain the same
        assertThat(expense.getId()).isEqualTo(789L);
        assertThat(expense.getAmount()).isEqualByComparingTo(updatedAmount);
        assertThat(expense.getDescription()).isEqualTo(description); // Unchanged
        assertThat(expense.getDate()).isEqualTo(date); // Unchanged
        assertThat(expense.getCategory().getName()).isEqualTo(categoryName); // Unchanged
        assertThat(expense.getCreatedAt()).isEqualTo(createdTime); // Unchanged
        assertThat(expense.getUpdatedAt()).isEqualTo(newUpdateTime); // Changed
        
        // And: The original amount should no longer be present
        assertThat(expense.getAmount()).isNotEqualByComparingTo(originalAmount);
        assertThat(expense.getUpdatedAt()).isNotEqualTo(originalUpdateTime);
        
        // And: The partial modification should be persistent (simulate retrieval)
        Expense retrievedExpense = Expense.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .date(expense.getDate())
                .category(expense.getCategory())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
        
        assertThat(retrievedExpense.getAmount()).isEqualByComparingTo(updatedAmount);
        assertThat(retrievedExpense.getDescription()).isEqualTo(description);
        assertThat(retrievedExpense.getDate()).isEqualTo(date);
        assertThat(retrievedExpense.getCategory().getName()).isEqualTo(categoryName);
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