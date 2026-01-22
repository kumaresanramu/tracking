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
}