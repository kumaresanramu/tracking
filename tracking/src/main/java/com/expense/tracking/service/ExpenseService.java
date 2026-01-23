package com.expense.tracking.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.CategoryResponse;
import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.exception.ResourceNotFoundException;
import com.expense.tracking.exception.ValidationException;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;
    
    public ExpenseResponse createExpense(ExpenseRequest request) {
        log.debug("Creating expense with amount: {} and date: {}", request.getAmount(), request.getDate());
        
        // Validate input
        validateExpenseRequest(request);
        
        // Find category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        
        // Create expense entity
        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .date(request.getDate())
                .category(category)
                .description(request.getDescription())
                .paymentMethod(request.getPaymentMethod())
                .tags(request.getTags())
                .build();
        
        // Save expense
        Expense savedExpense = expenseRepository.save(expense);
        log.info("Created expense with id: {}", savedExpense.getId());
        
        // Check budget thresholds and trigger immediate alerts if needed
        checkBudgetThresholds(savedExpense);
        
        return mapToResponse(savedExpense);
    }
    
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByMonth(int year, int month) {
        log.debug("Fetching expenses for year: {} and month: {}", year, month);
        
        // Validate month and year
        validateMonthAndYear(year, month);
        
        List<Expense> expenses = expenseRepository.findByYearAndMonth(year, month);
        log.debug("Found {} expenses for {}/{}", expenses.size(), month, year);
        
        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        log.debug("Updating expense with id: {}", id);
        
        // Validate input
        validateExpenseRequest(request);
        
        // Find existing expense with category loaded
        Expense expense = expenseRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        
        // Store old amount for budget calculation
        BigDecimal oldAmount = expense.getAmount();
        
        // Find category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        
        // Update expense fields
        expense.setAmount(request.getAmount());
        expense.setDate(request.getDate());
        expense.setCategory(category);
        expense.setDescription(request.getDescription());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setTags(request.getTags());
        
        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Updated expense with id: {}", updatedExpense.getId());
        
        // Check budget thresholds if amount increased
        if (request.getAmount().compareTo(oldAmount) > 0) {
            checkBudgetThresholds(updatedExpense);
        }
        
        return mapToResponse(updatedExpense);
    }
    
    public void deleteExpense(Long id) {
        log.debug("Deleting expense with id: {}", id);
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        
        expenseRepository.deleteById(id);
        log.info("Deleted expense with id: {}", id);
    }
    
    private void validateExpenseRequest(ExpenseRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive");
        }
        
        if (request.getDate() == null) {
            throw new ValidationException("Date is required");
        }
        
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Date cannot be in the future");
        }
        
        if (request.getCategoryId() == null) {
            throw new ValidationException("Category ID is required");
        }
        
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new ValidationException("Description cannot exceed 500 characters");
        }
    }
    
    private void validateMonthAndYear(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ValidationException("Month must be between 1 and 12");
        }
        
        if (year < 1900 || year > LocalDate.now().getYear() + 10) {
            throw new ValidationException("Year must be between 1900 and " + (LocalDate.now().getYear() + 10));
        }
    }
    
    private ExpenseResponse mapToResponse(Expense expense) {
        CategoryResponse categoryResponse = null;
        if (expense.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(expense.getCategory().getId())
                    .name(expense.getCategory().getName())
                    .color(expense.getCategory().getColor())
                    .description(expense.getCategory().getDescription())
                    .fullPath(expense.getCategory().getFullPath())
                    .build();
        }
        
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .category(categoryResponse)
                .description(expense.getDescription())
                .paymentMethod(expense.getPaymentMethod())
                .tags(expense.getTags())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
    
    public List<ExpenseResponse> batchCreateExpenses(List<ExpenseRequest> requests) {
        log.debug("Batch creating {} expenses", requests.size());
        
        if (requests.isEmpty()) {
            throw new ValidationException("Request list cannot be empty");
        }
        
        if (requests.size() > 100) {
            throw new ValidationException("Batch size cannot exceed 100 expenses");
        }
        
        List<ExpenseResponse> responses = requests.stream()
                .map(this::createExpense)
                .collect(Collectors.toList());
        
        log.info("Batch created {} expenses", responses.size());
        return responses;
    }
    
    public List<ExpenseResponse> batchUpdateExpenses(List<ExpenseRequest> requests) {
        log.debug("Batch updating {} expenses", requests.size());
        
        if (requests.isEmpty()) {
            throw new ValidationException("Request list cannot be empty");
        }
        
        if (requests.size() > 100) {
            throw new ValidationException("Batch size cannot exceed 100 expenses");
        }
        
        List<ExpenseResponse> responses = requests.stream()
                .map(request -> {
                    if (request.getId() == null) {
                        throw new ValidationException("Expense ID is required for batch update");
                    }
                    return updateExpense(request.getId(), request);
                })
                .collect(Collectors.toList());
        
        log.info("Batch updated {} expenses", responses.size());
        return responses;
    }
    
    /**
     * Check budget thresholds and trigger immediate alerts if needed
     */
    private void checkBudgetThresholds(Expense expense) {
        try {
            // Calculate current month spending
            LocalDate expenseDate = expense.getDate();
            LocalDate startOfMonth = expenseDate.withDayOfMonth(1);
            LocalDate endOfMonth = expenseDate.withDayOfMonth(expenseDate.lengthOfMonth());
            
            List<Expense> monthlyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(startOfMonth, endOfMonth);
            double totalSpent = monthlyExpenses.stream()
                    .mapToDouble(e -> e.getAmount().doubleValue())
                    .sum();
            
            // TODO: Get actual budget from user settings - using default for now
            double monthlyBudget = 10000.0; // Default budget
            
            if (monthlyBudget <= 0) {
                log.debug("No budget set, skipping budget threshold check");
                return;
            }
            
            double percentage = (totalSpent / monthlyBudget) * 100;
            
            log.debug("Budget check: spent ₹{} of ₹{} ({}%)", totalSpent, monthlyBudget, String.format("%.1f", percentage));
            
            // Get user notification settings to check threshold
            var settings = notificationService.getNotificationSettings();
            int warningThreshold = settings != null && settings.getBudgetWarningThreshold() != null 
                ? settings.getBudgetWarningThreshold() 
                : 80; // Default to 80%
            
            // Trigger immediate alerts based on thresholds
            if (percentage >= 100 && !hasRecentBudgetAlert(expense.getDate(), "EXCEEDED")) {
                triggerImmediateBudgetAlert(totalSpent, monthlyBudget, percentage, true);
            } else if (percentage >= warningThreshold && !hasRecentBudgetAlert(expense.getDate(), "WARNING")) {
                triggerImmediateBudgetAlert(totalSpent, monthlyBudget, percentage, false);
            }
            
        } catch (Exception e) {
            log.error("Error checking budget thresholds", e);
        }
    }
    
    /**
     * Check if there's already a recent budget alert to avoid spam
     */
    private boolean hasRecentBudgetAlert(LocalDate expenseDate, String alertType) {
        // Check if there's already a budget alert in the last 24 hours
        // This prevents spam notifications for the same threshold
        // TODO: Implement proper alert tracking - for now return false to allow alerts
        return false;
    }
    
    /**
     * Trigger immediate budget alert notification
     */
    private void triggerImmediateBudgetAlert(double totalSpent, double monthlyBudget, double percentage, boolean exceeded) {
        try {
            String title = exceeded ? "Budget Exceeded!" : "Budget Warning";
            String message = String.format("You've spent ₹%.0f (%.0f%%) of your ₹%.0f monthly budget.", 
                    totalSpent, percentage, monthlyBudget);
            
            // Create immediate notification
            var notification = notificationService.createBudgetAlert(percentage, totalSpent, monthlyBudget);
            
            if (notification != null) {
                log.info("Triggered immediate budget alert: {} - {}", title, message);
            }
            
        } catch (Exception e) {
            log.error("Error triggering immediate budget alert", e);
        }
    }
    
    /**
     * Get current month budget spending summary
     */
    public BudgetSummary getCurrentMonthBudgetSummary() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        
        List<Expense> monthlyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(startOfMonth, now);
        double totalSpent = monthlyExpenses.stream()
                .mapToDouble(expense -> expense.getAmount().doubleValue())
                .sum();
        
        // TODO: Get actual budget from user settings
        double monthlyBudget = 10000.0; // Default budget
        double percentage = monthlyBudget > 0 ? (totalSpent / monthlyBudget) * 100 : 0;
        
        return BudgetSummary.builder()
                .monthlyBudget(monthlyBudget)
                .totalSpent(totalSpent)
                .remainingBudget(monthlyBudget - totalSpent)
                .percentage(percentage)
                .isOverBudget(percentage >= 100)
                .isNearThreshold(percentage >= 80)
                .build();
    }
    
    /**
     * Data class for budget summary information
     */
    public static class BudgetSummary {
        private double monthlyBudget;
        private double totalSpent;
        private double remainingBudget;
        private double percentage;
        private boolean isOverBudget;
        private boolean isNearThreshold;
        
        public static BudgetSummaryBuilder builder() {
            return new BudgetSummaryBuilder();
        }
        
        // Getters
        public double getMonthlyBudget() { return monthlyBudget; }
        public double getTotalSpent() { return totalSpent; }
        public double getRemainingBudget() { return remainingBudget; }
        public double getPercentage() { return percentage; }
        public boolean isOverBudget() { return isOverBudget; }
        public boolean isNearThreshold() { return isNearThreshold; }
        
        public static class BudgetSummaryBuilder {
            private double monthlyBudget;
            private double totalSpent;
            private double remainingBudget;
            private double percentage;
            private boolean isOverBudget;
            private boolean isNearThreshold;
            
            public BudgetSummaryBuilder monthlyBudget(double monthlyBudget) {
                this.monthlyBudget = monthlyBudget;
                return this;
            }
            
            public BudgetSummaryBuilder totalSpent(double totalSpent) {
                this.totalSpent = totalSpent;
                return this;
            }
            
            public BudgetSummaryBuilder remainingBudget(double remainingBudget) {
                this.remainingBudget = remainingBudget;
                return this;
            }
            
            public BudgetSummaryBuilder percentage(double percentage) {
                this.percentage = percentage;
                return this;
            }
            
            public BudgetSummaryBuilder isOverBudget(boolean isOverBudget) {
                this.isOverBudget = isOverBudget;
                return this;
            }
            
            public BudgetSummaryBuilder isNearThreshold(boolean isNearThreshold) {
                this.isNearThreshold = isNearThreshold;
                return this;
            }
            
            public BudgetSummary build() {
                BudgetSummary summary = new BudgetSummary();
                summary.monthlyBudget = this.monthlyBudget;
                summary.totalSpent = this.totalSpent;
                summary.remainingBudget = this.remainingBudget;
                summary.percentage = this.percentage;
                summary.isOverBudget = this.isOverBudget;
                summary.isNearThreshold = this.isNearThreshold;
                return summary;
            }
        }
    }
}