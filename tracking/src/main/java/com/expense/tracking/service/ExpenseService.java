package com.expense.tracking.service;

import com.expense.tracking.dto.CategoryResponse;
import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.entity.SyncOperationType;
import com.expense.tracking.exception.GoogleSheetsException;
import com.expense.tracking.exception.ResourceNotFoundException;
import com.expense.tracking.exception.ValidationException;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final GoogleSheetsService googleSheetsService;
    private final SyncService syncService;
    
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
                .synced(false) // Will be synced later
                .build();
        
        // Save expense
        Expense savedExpense = expenseRepository.save(expense);
        log.info("Created expense with id: {}", savedExpense.getId());
        
        // Queue for sync to Google Sheets
        queueExpenseForSync(savedExpense, SyncOperationType.CREATE);
        
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
        expense.setSynced(false); // Mark as unsynced after update
        
        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Updated expense with id: {}", updatedExpense.getId());
        
        // Queue updated expense for sync to Google Sheets
        queueExpenseForSync(updatedExpense, SyncOperationType.UPDATE);
        
        return mapToResponse(updatedExpense);
    }
    
    public void deleteExpense(Long id) {
        log.debug("Deleting expense with id: {}", id);
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        
        expenseRepository.deleteById(id);
        log.info("Deleted expense with id: {}", id);
        
        // Queue for deletion sync to Google Sheets
        syncService.queueForSync(SyncOperationType.DELETE, id, "Expense", expense);
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
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .synced(expense.getSynced())
                .build();
    }
    
    @Async
    private void syncExpenseToGoogleSheets(Expense expense) {
        try {
            if (googleSheetsService.isConnected()) {
                googleSheetsService.syncExpenseToSheet(expense);
                
                // Mark as synced
                expense.setSynced(true);
                expenseRepository.save(expense);
                
                log.info("Successfully synced expense {} to Google Sheets", expense.getId());
            } else {
                log.warn("Google Sheets not connected, expense {} will remain unsynced", expense.getId());
            }
        } catch (Exception e) {
            log.error("Failed to sync expense {} to Google Sheets: {}", expense.getId(), e.getMessage());
            // Don't throw exception to avoid breaking the main flow
            // The expense will remain marked as unsynced for later retry
        }
    }
    
    private void queueExpenseForSync(Expense expense, SyncOperationType operationType) {
        try {
            syncService.queueForSync(operationType, expense.getId(), "Expense", expense);
            log.debug("Queued expense {} for {} sync", expense.getId(), operationType);
        } catch (Exception e) {
            log.error("Failed to queue expense {} for sync: {}", expense.getId(), e.getMessage());
        }
    }
    
    public List<ExpenseResponse> batchCreateExpenses(List<ExpenseRequest> requests) {
        log.debug("Batch creating {} expenses", requests.size());
        
        if (requests == null || requests.isEmpty()) {
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
        
        if (requests == null || requests.isEmpty()) {
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