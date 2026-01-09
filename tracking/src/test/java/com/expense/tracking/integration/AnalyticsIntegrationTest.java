package com.expense.tracking.integration;

import com.expense.tracking.dto.CategoryBreakdownResponse;
import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.MonthlyTrendResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.service.AnalyticsService;
import com.expense.tracking.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AnalyticsIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test categories
        foodCategory = Category.builder()
                .name("Food")
                .color("#FF5722")
                .description("Food and dining expenses")
                .build();
        foodCategory = categoryRepository.save(foodCategory);

        transportCategory = Category.builder()
                .name("Transport")
                .color("#2196F3")
                .description("Transportation expenses")
                .build();
        transportCategory = categoryRepository.save(transportCategory);
    }

    @Test
    void testCompleteAnalyticsWorkflow() {
        LocalDate currentDate = LocalDate.now();
        
        // Create expenses for current month
        createExpense(new BigDecimal("25.50"), currentDate, foodCategory, "Lunch");
        createExpense(new BigDecimal("15.75"), currentDate, foodCategory, "Coffee");
        createExpense(new BigDecimal("45.00"), currentDate, transportCategory, "Gas");
        
        // Create expenses for previous month
        LocalDate previousMonth = currentDate.minusMonths(1);
        createExpense(new BigDecimal("30.00"), previousMonth, foodCategory, "Dinner");
        createExpense(new BigDecimal("20.00"), previousMonth, transportCategory, "Bus ticket");

        // Test category breakdown for current month
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(
                currentDate.getYear(), currentDate.getMonthValue());
        
        assertThat(breakdown).hasSize(2);
        
        // Verify food category breakdown
        CategoryBreakdownResponse foodBreakdown = breakdown.stream()
                .filter(cb -> cb.getCategoryName().equals("Food"))
                .findFirst()
                .orElse(null);
        assertThat(foodBreakdown).isNotNull();
        assertThat(foodBreakdown.getTotalAmount()).isEqualTo(new BigDecimal("41.25"));
        assertThat(foodBreakdown.getExpenseCount()).isEqualTo(2);

        // Verify transport category breakdown
        CategoryBreakdownResponse transportBreakdown = breakdown.stream()
                .filter(cb -> cb.getCategoryName().equals("Transport"))
                .findFirst()
                .orElse(null);
        assertThat(transportBreakdown).isNotNull();
        assertThat(transportBreakdown.getTotalAmount()).isEqualTo(new BigDecimal("45.00"));
        assertThat(transportBreakdown.getExpenseCount()).isEqualTo(1);

        // Test monthly trends
        List<MonthlyTrendResponse> trends = analyticsService.getMonthlyTrends(2);
        assertThat(trends).hasSize(2);

        // Test expense summary
        Map<String, Object> summary = analyticsService.getExpenseSummary(
                currentDate.getYear(), currentDate.getMonthValue());
        
        BigDecimal totalAmount = (BigDecimal) summary.get("totalAmount");
        assertThat(totalAmount).isEqualTo(new BigDecimal("86.25"));
        
        Long expenseCount = (Long) summary.get("expenseCount");
        assertThat(expenseCount).isEqualTo(3L);
    }

    @Test
    void testAnalyticsWithHierarchicalCategories() {
        // Create parent category
        Category parentCategory = Category.builder()
                .name("Housing")
                .color("#4CAF50")
                .description("Housing related expenses")
                .build();
        parentCategory = categoryRepository.save(parentCategory);

        // Create child categories
        Category utilitiesCategory = Category.builder()
                .name("Utilities")
                .color("#4CAF50")
                .description("Utility bills")
                .parent(parentCategory)
                .build();
        utilitiesCategory = categoryRepository.save(utilitiesCategory);

        Category rentCategory = Category.builder()
                .name("Rent")
                .color("#4CAF50")
                .description("Monthly rent")
                .parent(parentCategory)
                .build();
        rentCategory = categoryRepository.save(rentCategory);

        LocalDate currentDate = LocalDate.now();
        
        // Create expenses with hierarchical categories
        createExpense(new BigDecimal("1200.00"), currentDate, rentCategory, "Monthly rent");
        createExpense(new BigDecimal("150.00"), currentDate, utilitiesCategory, "Electricity bill");
        createExpense(new BigDecimal("80.00"), currentDate, utilitiesCategory, "Water bill");

        // Test category breakdown
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(
                currentDate.getYear(), currentDate.getMonthValue());
        
        assertThat(breakdown).hasSize(2); // Should have rent and utilities as separate categories
        
        // Verify utilities category total
        CategoryBreakdownResponse utilitiesBreakdown = breakdown.stream()
                .filter(cb -> cb.getCategoryName().equals("Utilities"))
                .findFirst()
                .orElse(null);
        assertThat(utilitiesBreakdown).isNotNull();
        assertThat(utilitiesBreakdown.getTotalAmount()).isEqualTo(new BigDecimal("230.00"));
        assertThat(utilitiesBreakdown.getExpenseCount()).isEqualTo(2);
    }

    @Test
    void testAnalyticsWithNoData() {
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        
        // Test category breakdown with no data
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(
                futureDate.getYear(), futureDate.getMonthValue());
        assertThat(breakdown).isEmpty();

        // Test expense summary with no data
        Map<String, Object> summary = analyticsService.getExpenseSummary(
                futureDate.getYear(), futureDate.getMonthValue());
        
        BigDecimal totalAmount = (BigDecimal) summary.get("totalAmount");
        assertThat(totalAmount).isEqualTo(BigDecimal.ZERO);
        
        Long expenseCount = (Long) summary.get("expenseCount");
        assertThat(expenseCount).isEqualTo(0L);
    }

    private void createExpense(BigDecimal amount, LocalDate date, Category category, String description) {
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(amount)
                .date(date)
                .categoryId(category.getId())
                .description(description)
                .build();
        
        expenseService.createExpense(request);
    }
}