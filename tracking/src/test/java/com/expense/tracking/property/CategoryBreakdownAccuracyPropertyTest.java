package com.expense.tracking.property;

import com.expense.tracking.dto.CategoryBreakdownResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.service.AnalyticsService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 16: Category Breakdown Accuracy
 * Validates: Requirements 5.2
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CategoryBreakdownAccuracyPropertyTest {

    @Autowired
    private AnalyticsService analyticsService;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    @Property(tries = 100)
    void categoryBreakdownAccuracy(
            @ForAll @Size(min = 3, max = 20) List<@From("expenseData") ExpenseData> expenseDataList,
            @ForAll @From("monthYear") MonthYear monthYear) {
        
        // Given: A selected month with expenses across different categories
        List<Category> categories = expenseDataList.stream()
                .map(data -> Category.builder()
                        .name(data.categoryName)
                        .color("#2196F3")
                        .build())
                .distinct()
                .collect(Collectors.toList());
        
        // Save categories first
        categoryRepository.saveAll(categories);
        
        // Create expenses for the specific month
        List<Expense> expenses = expenseDataList.stream()
                .map(data -> {
                    Category category = categories.stream()
                            .filter(c -> c.getName().equals(data.categoryName))
                            .findFirst()
                            .orElse(categories.get(0));
                    
                    LocalDate expenseDate = LocalDate.of(monthYear.year, monthYear.month, 15);
                    
                    return Expense.builder()
                            .amount(data.amount)
                            .date(expenseDate)
                            .category(category)
                            .description(data.description)
                            .synced(false)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Save expenses
        expenseRepository.saveAll(expenses);
        
        // When: Getting category breakdown for the selected month
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(
                monthYear.year, monthYear.month);
        
        // Then: The category-wise breakdown should show correct totals for each category
        assertThat(breakdown).isNotNull();
        
        // Group actual expenses by category for verification
        Map<String, List<Expense>> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(expense -> expense.getCategory().getName()));
        
        // Verify that all categories with expenses are represented
        assertThat(breakdown).hasSameSizeAs(expensesByCategory.keySet());
        
        // Calculate total amount across all categories
        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Verify each category's breakdown accuracy
        for (CategoryBreakdownResponse categoryBreakdown : breakdown) {
            String categoryName = categoryBreakdown.getCategoryName();
            List<Expense> categoryExpenses = expensesByCategory.get(categoryName);
            
            assertThat(categoryExpenses).isNotNull()
                    .withFailMessage("Category %s should have expenses", categoryName);
            
            // Calculate expected values for this category
            BigDecimal expectedTotal = categoryExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long expectedCount = categoryExpenses.size();
            
            BigDecimal expectedPercentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? expectedTotal.divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            
            // Verify breakdown data matches actual expenses
            assertThat(categoryBreakdown.getTotalAmount())
                    .isEqualByComparingTo(expectedTotal)
                    .withFailMessage("Total amount for category %s should be %s but was %s", 
                            categoryName, expectedTotal, categoryBreakdown.getTotalAmount());
            
            assertThat(categoryBreakdown.getExpenseCount())
                    .isEqualTo(expectedCount)
                    .withFailMessage("Expense count for category %s should be %d but was %d", 
                            categoryName, expectedCount, categoryBreakdown.getExpenseCount());
            
            assertThat(categoryBreakdown.getPercentage())
                    .isEqualByComparingTo(expectedPercentage)
                    .withFailMessage("Percentage for category %s should be %s but was %s", 
                            categoryName, expectedPercentage, categoryBreakdown.getPercentage());
            
            // Verify category metadata
            assertThat(categoryBreakdown.getCategoryId()).isNotNull();
            assertThat(categoryBreakdown.getCategoryName()).isEqualTo(categoryName);
        }
        
        // Verify that breakdown is sorted by total amount descending
        for (int i = 1; i < breakdown.size(); i++) {
            BigDecimal current = breakdown.get(i).getTotalAmount();
            BigDecimal previous = breakdown.get(i - 1).getTotalAmount();
            assertThat(current).isLessThanOrEqualTo(previous)
                    .withFailMessage("Breakdown should be sorted by total amount descending");
        }
        
        // Verify that all percentages sum to approximately 100%
        BigDecimal totalPercentage = breakdown.stream()
                .map(CategoryBreakdownResponse::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(totalPercentage)
                .isBetween(BigDecimal.valueOf(99.9), BigDecimal.valueOf(100.1))
                .withFailMessage("Total percentages should sum to approximately 100%% but was %s", totalPercentage);
    }

    @Provide
    Arbitrary<ExpenseData> expenseData() {
        return Combinators.combine(
                Arbitraries.bigDecimals()
                        .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(999.99))
                        .ofScale(2),
                Arbitraries.of("Food", "Transportation", "Entertainment", "Utilities", "Healthcare", "Shopping"),
                Arbitraries.strings().alpha().ofMaxLength(100)
        ).as(ExpenseData::new);
    }
    
    @Provide
    Arbitrary<MonthYear> monthYear() {
        return Combinators.combine(
                Arbitraries.integers().between(2023, 2024),
                Arbitraries.integers().between(1, 12)
        ).as(MonthYear::new);
    }
    
    private static class ExpenseData {
        final BigDecimal amount;
        final String categoryName;
        final String description;
        
        ExpenseData(BigDecimal amount, String categoryName, String description) {
            this.amount = amount;
            this.categoryName = categoryName;
            this.description = description;
        }
    }
    
    private static class MonthYear {
        final int year;
        final int month;
        
        MonthYear(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }
}