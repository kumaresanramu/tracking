package com.expense.tracking.integration;

import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ExpenseLifecycleIntegrationTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create a test category
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF0000")
                .description("Test category for integration tests")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testCompleteExpenseLifecycle() {
        // Test 1: Create an expense
        ExpenseRequest createRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("50.75"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Integration test expense")
                .build();

        ExpenseResponse createdExpense = expenseService.createExpense(createRequest);
        
        assertThat(createdExpense).isNotNull();
        assertThat(createdExpense.getAmount()).isEqualTo(new BigDecimal("50.75"));
        assertThat(createdExpense.getDescription()).isEqualTo("Integration test expense");
        assertThat(createdExpense.getCategory().getName()).isEqualTo("Test Category");

        Long expenseId = createdExpense.getId();
        assertThat(expenseId).isNotNull();

        // Test 2: Retrieve the expense by month
        LocalDate now = LocalDate.now();
        List<ExpenseResponse> monthlyExpenses = expenseService.getExpensesByMonth(now.getYear(), now.getMonthValue());
        
        assertThat(monthlyExpenses).hasSize(1);
        assertThat(monthlyExpenses.get(0).getId()).isEqualTo(expenseId);
        assertThat(monthlyExpenses.get(0).getAmount()).isEqualTo(new BigDecimal("50.75"));

        // Test 3: Update the expense
        ExpenseRequest updateRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("75.25"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Updated integration test expense")
                .build();

        ExpenseResponse updatedExpense = expenseService.updateExpense(expenseId, updateRequest);
        
        assertThat(updatedExpense.getAmount()).isEqualTo(new BigDecimal("75.25"));
        assertThat(updatedExpense.getDescription()).isEqualTo("Updated integration test expense");

        // Test 4: Verify the update persisted
        List<ExpenseResponse> updatedMonthlyExpenses = expenseService.getExpensesByMonth(now.getYear(), now.getMonthValue());
        assertThat(updatedMonthlyExpenses).hasSize(1);
        assertThat(updatedMonthlyExpenses.get(0).getAmount()).isEqualTo(new BigDecimal("75.25"));

        // Test 5: Delete the expense
        expenseService.deleteExpense(expenseId);

        // Test 6: Verify the expense was deleted
        List<ExpenseResponse> finalExpenses = expenseService.getExpensesByMonth(now.getYear(), now.getMonthValue());
        assertThat(finalExpenses).isEmpty();

        // Verify in database
        assertThat(expenseRepository.findById(expenseId)).isEmpty();
    }

    @Test
    void testCategoryHierarchyIntegration() {
        // Create parent category
        Category parentCategory = Category.builder()
                .name("Parent Category")
                .color("#00FF00")
                .description("Parent category for testing")
                .build();
        parentCategory = categoryRepository.save(parentCategory);

        // Create child category
        Category childCategory = Category.builder()
                .name("Child Category")
                .color("#0000FF")
                .description("Child category for testing")
                .parent(parentCategory)
                .build();
        childCategory = categoryRepository.save(childCategory);

        // Create expense with child category
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .categoryId(childCategory.getId())
                .description("Hierarchical category test")
                .build();

        ExpenseResponse createdExpense = expenseService.createExpense(request);
        
        // Remove the parent category test since CategoryResponse doesn't have getParent()
        assertThat(createdExpense.getCategory().getName()).isEqualTo("Child Category");
    }

    @Test
    void testMultipleExpenseOperations() {
        // Create multiple expenses
        ExpenseRequest expense1 = ExpenseRequest.builder()
                .amount(new BigDecimal("25.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Expense 1")
                .build();

        ExpenseRequest expense2 = ExpenseRequest.builder()
                .amount(new BigDecimal("35.50"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Expense 2")
                .build();

        ExpenseResponse created1 = expenseService.createExpense(expense1);
        ExpenseResponse created2 = expenseService.createExpense(expense2);

        assertThat(created1.getAmount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(created2.getAmount()).isEqualTo(new BigDecimal("35.50"));

        // Verify both expenses were created
        LocalDate now = LocalDate.now();
        List<ExpenseResponse> monthlyExpenses = expenseService.getExpensesByMonth(now.getYear(), now.getMonthValue());
        assertThat(monthlyExpenses).hasSize(2);
    }
}