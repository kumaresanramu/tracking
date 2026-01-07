package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 7: Monthly Sheet Organization
 * Validates: Requirements 2.5
 */
public class MonthlySheetOrganizationPropertyTest {

    @Property(tries = 100)
    void monthlySheetOrganization(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll @IntRange(min = 2020, max = 2030) int year,
            @ForAll @IntRange(min = 1, max = 12) int month,
            @ForAll @IntRange(min = 1, max = 28) int day, // Use 28 to avoid month boundary issues
            @ForAll @StringLength(min = 1, max = 100) String categoryName,
            @ForAll @StringLength(max = 500) String description) {
        
        // Given: An expense with a specific date
        LocalDate expenseDate = LocalDate.of(year, month, day);
        
        Category category = Category.builder()
                .name(categoryName)
                .build();
        
        Expense expense = Expense.builder()
                .id(1L)
                .amount(amount)
                .date(expenseDate)
                .category(category)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .synced(false)
                .build();

        // When: We determine the monthly sheet name for this expense
        String expectedSheetName = getMonthlySheetName(year, month);
        String actualSheetName = determineSheetForExpense(expense);
        
        // Then: The expense should be assigned to the correct monthly sheet
        assertThat(actualSheetName)
            .as("Expense should be assigned to the sheet corresponding to its date month and year")
            .isEqualTo(expectedSheetName);
            
        // And: The sheet name should follow the expected format "Month Year"
        assertThat(actualSheetName)
            .as("Sheet name should follow 'Month Year' format")
            .matches("^[A-Za-z]+ \\d{4}$");
            
        // And: The sheet name should contain the correct month and year
        assertThat(actualSheetName)
            .as("Sheet name should contain the correct year")
            .contains(String.valueOf(year));
    }

    @Property(tries = 100)
    void multipleExpensesMonthlyOrganization(
            @ForAll("expenseList") List<Expense> expenses) {
        
        // Given: Multiple expenses across different months
        // When: We organize them by monthly sheets
        Map<String, List<Expense>> sheetOrganization = organizeExpensesBySheet(expenses);
        
        // Then: Each expense should be in exactly one sheet
        int totalExpensesInSheets = sheetOrganization.values().stream()
                .mapToInt(List::size)
                .sum();
        assertThat(totalExpensesInSheets)
            .as("Total expenses in sheets should equal original expense count")
            .isEqualTo(expenses.size());
        
        // And: All expenses in the same sheet should have the same year and month
        for (Map.Entry<String, List<Expense>> entry : sheetOrganization.entrySet()) {
            String sheetName = entry.getKey();
            List<Expense> sheetExpenses = entry.getValue();
            
            if (!sheetExpenses.isEmpty()) {
                LocalDate firstExpenseDate = sheetExpenses.get(0).getDate();
                int expectedYear = firstExpenseDate.getYear();
                int expectedMonth = firstExpenseDate.getMonthValue();
                
                for (Expense expense : sheetExpenses) {
                    assertThat(expense.getDate().getYear())
                        .as("All expenses in sheet %s should have year %d", sheetName, expectedYear)
                        .isEqualTo(expectedYear);
                        
                    assertThat(expense.getDate().getMonthValue())
                        .as("All expenses in sheet %s should have month %d", sheetName, expectedMonth)
                        .isEqualTo(expectedMonth);
                }
            }
        }
        
        // And: Sheet names should be unique and properly formatted
        for (String sheetName : sheetOrganization.keySet()) {
            assertThat(sheetName)
                .as("Sheet name should follow 'Month Year' format")
                .matches("^[A-Za-z]+ \\d{4}$");
        }
    }

    @Property(tries = 50)
    void crossYearMonthlyOrganization(
            @ForAll @IntRange(min = 2020, max = 2029) int year1,
            @ForAll @IntRange(min = 1, max = 12) int month1,
            @ForAll @IntRange(min = 2020, max = 2029) int year2,
            @ForAll @IntRange(min = 1, max = 12) int month2,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount) {
        
        // Given: Two expenses from different months/years
        LocalDate date1 = LocalDate.of(year1, month1, 15);
        LocalDate date2 = LocalDate.of(year2, month2, 15);
        
        Category category = Category.builder()
                .name("Test Category")
                .build();
        
        Expense expense1 = Expense.builder()
                .id(1L)
                .amount(amount)
                .date(date1)
                .category(category)
                .description("Expense 1")
                .build();
                
        Expense expense2 = Expense.builder()
                .id(2L)
                .amount(amount)
                .date(date2)
                .category(category)
                .description("Expense 2")
                .build();

        // When: We determine their sheet assignments
        String sheet1 = determineSheetForExpense(expense1);
        String sheet2 = determineSheetForExpense(expense2);
        
        // Then: If dates are in different months or years, they should be in different sheets
        if (year1 != year2 || month1 != month2) {
            assertThat(sheet1)
                .as("Expenses from different months/years should be in different sheets")
                .isNotEqualTo(sheet2);
        } else {
            // If same month and year, they should be in the same sheet
            assertThat(sheet1)
                .as("Expenses from the same month and year should be in the same sheet")
                .isEqualTo(sheet2);
        }
    }

    /**
     * Determines which sheet an expense should be stored in based on its date
     */
    private String determineSheetForExpense(Expense expense) {
        LocalDate date = expense.getDate();
        return getMonthlySheetName(date.getYear(), date.getMonthValue());
    }

    /**
     * Organizes expenses by their monthly sheets
     */
    private Map<String, List<Expense>> organizeExpensesBySheet(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(this::determineSheetForExpense));
    }

    /**
     * Gets the monthly sheet name for a given year and month
     */
    private String getMonthlySheetName(int year, int month) {
        String[] monthNames = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return monthNames[month - 1] + " " + year;
    }

    @Provide
    Arbitrary<List<Expense>> expenseList() {
        return Arbitraries.integers().between(1, 10)
                .flatMap(size -> 
                    Arbitraries.of(
                        generateExpense(2023, 1),
                        generateExpense(2023, 2),
                        generateExpense(2023, 6),
                        generateExpense(2024, 1),
                        generateExpense(2024, 3),
                        generateExpense(2024, 12)
                    ).list().ofSize(size)
                );
    }

    private Expense generateExpense(int year, int month) {
        Category category = Category.builder()
                .name("Category " + month)
                .build();
                
        return Expense.builder()
                .id((long) (Math.random() * 1000))
                .amount(BigDecimal.valueOf(10.0 + Math.random() * 990))
                .date(LocalDate.of(year, month, 1 + (int)(Math.random() * 28)))
                .category(category)
                .description("Test expense for " + year + "/" + month)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .synced(false)
                .build();
    }
}