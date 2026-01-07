package com.expense.tracking.controller;

import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For PWA frontend integration
public class ExpenseController {
    
    private final ExpenseService expenseService;
    
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.createExpense(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByMonth(
            @PathVariable int year, 
            @PathVariable int month) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByMonth(year, month);
        return ResponseEntity.ok(expenses);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id, 
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/batch")
    public ResponseEntity<List<ExpenseResponse>> batchCreateExpenses(@Valid @RequestBody List<ExpenseRequest> requests) {
        List<ExpenseResponse> responses = expenseService.batchCreateExpenses(requests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }
    
    @PutMapping("/batch")
    public ResponseEntity<List<ExpenseResponse>> batchUpdateExpenses(@Valid @RequestBody List<ExpenseRequest> requests) {
        List<ExpenseResponse> responses = expenseService.batchUpdateExpenses(requests);
        return ResponseEntity.ok(responses);
    }
}