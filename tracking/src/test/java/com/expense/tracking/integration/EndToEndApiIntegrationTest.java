package com.expense.tracking.integration;

import com.expense.tracking.dto.*;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.ReminderFrequency;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.repository.PaymentReminderRepository;
import com.expense.tracking.repository.SyncOperationRepository;
import com.expense.tracking.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EndToEndApiIntegrationTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PaymentReminderService paymentReminderService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PaymentReminderRepository paymentReminderRepository;

    @Autowired
    private SyncOperationRepository syncOperationRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        syncOperationRepository.deleteAll();
        paymentReminderRepository.deleteAll();
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create a test category
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF0000")
                .description("Test category for API integration")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testCompleteExpenseServiceWorkflow() {
        // Test 1: Create an expense via service
        ExpenseRequest createRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("125.75"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Service integration test expense")
                .build();

        ExpenseResponse createdExpense = expenseService.createExpense(createRequest);
        
        assertThat(createdExpense).isNotNull();
        assertThat(createdExpense.getAmount()).isEqualTo(new BigDecimal("125.75"));
        assertThat(createdExpense.getDescription()).isEqualTo("Service integration test expense");
        assertThat(createdExpense.getCategory().getName()).isEqualTo("Test Category");

        Long expenseId = createdExpense.getId();

        // Test 2: Get expenses by month via service
        LocalDate now = LocalDate.now();
        List<ExpenseResponse> monthlyExpenses = expenseService.getExpensesByMonth(now.getYear(), now.getMonthValue());
        
        assertThat(monthlyExpenses).hasSize(1);
        assertThat(monthlyExpenses.get(0).getId()).isEqualTo(expenseId);
        assertThat(monthlyExpenses.get(0).getAmount()).isEqualTo(new BigDecimal("125.75"));

        // Test 3: Update the expense via service
        ExpenseRequest updateRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("150.25"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Updated service integration test expense")
                .build();

        ExpenseResponse updatedExpense = expenseService.updateExpense(expenseId, updateRequest);
        
        assertThat(updatedExpense.getAmount()).isEqualTo(new BigDecimal("150.25"));
        assertThat(updatedExpense.getDescription()).isEqualTo("Updated service integration test expense");

        // Test 4: Delete the expense via service
        expenseService.deleteExpense(expenseId);

        // Test 5: Verify the expense was deleted
        List<ExpenseResponse> finalExpenses = expenseService.getExpensesByMonth(now.getYear(), now.getMonthValue());
        assertThat(finalExpenses).isEmpty();
    }

    @Test
    void testCategoryServiceWorkflow() {
        // Test 1: Get all categories
        List<CategoryResponse> categories = categoryService.getAllCategories();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Test Category");

        // Test 2: Create a new category
        CategoryRequest createRequest = CategoryRequest.builder()
                .name("New Category")
                .color("#00FF00")
                .description("New category for testing")
                .build();

        CategoryResponse newCategory = categoryService.createCategory(createRequest);
        assertThat(newCategory.getName()).isEqualTo("New Category");
        assertThat(newCategory.getColor()).isEqualTo("#00FF00");

        // Test 3: Verify both categories exist
        List<CategoryResponse> allCategories = categoryService.getAllCategories();
        assertThat(allCategories).hasSize(2);
    }

    @Test
    void testPaymentReminderServiceWorkflow() {
        // Test 1: Create a payment reminder
        PaymentReminderRequest createRequest = PaymentReminderRequest.builder()
                .name("Service Test Reminder")
                .amount(new BigDecimal("200.00"))
                .dueDate(LocalDate.now().plusDays(5))
                .frequency(ReminderFrequency.MONTHLY)
                .categoryId(testCategory.getId())
                .daysBefore(3)
                .preferredNotificationTime(LocalTime.of(10, 0))
                .enableEmailNotification(true)
                .customMessage("Service test reminder message")
                .build();

        PaymentReminderResponse createdReminder = paymentReminderService.createReminder(createRequest);
        
        assertThat(createdReminder).isNotNull();
        assertThat(createdReminder.getName()).isEqualTo("Service Test Reminder");
        assertThat(createdReminder.getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(createdReminder.getFrequency()).isEqualTo(ReminderFrequency.MONTHLY);

        Long reminderId = createdReminder.getId();

        // Test 2: Get all reminders
        List<PaymentReminderResponse> allReminders = paymentReminderService.getAllReminders();
        assertThat(allReminders).hasSize(1);
        assertThat(allReminders.get(0).getId()).isEqualTo(reminderId);
        assertThat(allReminders.get(0).getName()).isEqualTo("Service Test Reminder");

        // Test 3: Get reminder by ID
        PaymentReminderResponse retrievedReminder = paymentReminderService.getReminderById(reminderId);
        assertThat(retrievedReminder.getName()).isEqualTo("Service Test Reminder");
        assertThat(retrievedReminder.getAmount()).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    void testAnalyticsServiceWorkflow() {
        // Create some test expenses first
        ExpenseRequest expense1 = ExpenseRequest.builder()
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Analytics test expense 1")
                .build();

        ExpenseRequest expense2 = ExpenseRequest.builder()
                .amount(new BigDecimal("75.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Analytics test expense 2")
                .build();

        expenseService.createExpense(expense1);
        expenseService.createExpense(expense2);

        LocalDate now = LocalDate.now();

        // Test 1: Get category breakdown
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(now.getYear(), now.getMonthValue());
        
        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).getCategoryName()).isEqualTo("Test Category");
        assertThat(breakdown.get(0).getTotalAmount()).isEqualTo(new BigDecimal("125.00"));
        assertThat(breakdown.get(0).getExpenseCount()).isEqualTo(2);

        // Test 2: Get monthly trends
        List<MonthlyTrendResponse> trends = analyticsService.getMonthlyTrends(3);
        assertThat(trends).isNotEmpty();

        // Test 3: Get expense summary
        var summary = analyticsService.getExpenseSummary(now.getYear(), now.getMonthValue());
        
        assertThat(summary).containsKey("totalAmount");
        assertThat(summary).containsKey("expenseCount");
        
        BigDecimal totalAmount = (BigDecimal) summary.get("totalAmount");
        assertThat(totalAmount).isEqualTo(new BigDecimal("125.00"));
        
        Long expenseCount = (Long) summary.get("expenseCount");
        assertThat(expenseCount).isEqualTo(2L);
    }

    @Test
    void testSyncServiceWorkflow() {
        // Test 1: Get sync status
        SyncStatusInfo status = syncService.getSyncStatus();
        
        assertThat(status).isNotNull();
        assertThat(status.getPendingOperations()).isNotNull();
        assertThat(status.getInProgressOperations()).isNotNull();
        assertThat(status.getFailedOperations()).isNotNull();

        // Test 2: Trigger manual sync (should complete without errors)
        var syncFuture = syncService.syncPendingChanges();
        assertThat(syncFuture).isNotNull();
    }

    @Test
    void testCompleteWorkflowIntegration() {
        // Test complete workflow: Create category, create expense, create reminder, analyze
        
        // 1. Create a category
        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Integration Category")
                .color("#FF9800")
                .description("Category for integration testing")
                .build();
        
        CategoryResponse category = categoryService.createCategory(categoryRequest);
        
        // 2. Create expenses
        ExpenseRequest expenseRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .categoryId(category.getId())
                .description("Integration test expense")
                .build();
        
        ExpenseResponse expense = expenseService.createExpense(expenseRequest);
        
        // 3. Create a payment reminder
        PaymentReminderRequest reminderRequest = PaymentReminderRequest.builder()
                .name("Integration Reminder")
                .amount(new BigDecimal("100.00"))
                .dueDate(LocalDate.now().plusDays(7))
                .frequency(ReminderFrequency.MONTHLY)
                .categoryId(category.getId())
                .daysBefore(3)
                .build();
        
        PaymentReminderResponse reminder = paymentReminderService.createReminder(reminderRequest);
        
        // 4. Analyze the data
        LocalDate now = LocalDate.now();
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(now.getYear(), now.getMonthValue());
        
        // Verify integration
        assertThat(category.getName()).isEqualTo("Integration Category");
        assertThat(expense.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(reminder.getName()).isEqualTo("Integration Reminder");
        
        // Should have at least 1 category (integration category)
        assertThat(breakdown).hasSizeGreaterThanOrEqualTo(1);
        
        // Find the integration category breakdown
        CategoryBreakdownResponse integrationBreakdown = breakdown.stream()
                .filter(cb -> cb.getCategoryName().equals("Integration Category"))
                .findFirst()
                .orElse(null);
        
        assertThat(integrationBreakdown).isNotNull();
        assertThat(integrationBreakdown.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(integrationBreakdown.getExpenseCount()).isEqualTo(1);
    }
}