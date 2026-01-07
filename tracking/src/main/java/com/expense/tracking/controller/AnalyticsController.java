package com.expense.tracking.controller;

import com.expense.tracking.dto.CategoryBreakdownResponse;
import com.expense.tracking.dto.MonthlyTrendResponse;
import com.expense.tracking.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For PWA frontend integration
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/monthly-trends")
    public ResponseEntity<List<MonthlyTrendResponse>> getMonthlyTrends(
            @RequestParam(defaultValue = "12") int months) {
        
        // Validate months parameter
        if (months < 1 || months > 60) {
            throw new IllegalArgumentException("Months must be between 1 and 60");
        }
        
        List<MonthlyTrendResponse> trends = analyticsService.getMonthlyTrends(months);
        return ResponseEntity.ok(trends);
    }
    
    @GetMapping("/category-breakdown/{year}/{month}")
    public ResponseEntity<List<CategoryBreakdownResponse>> getCategoryBreakdown(
            @PathVariable int year,
            @PathVariable int month) {
        
        // Validate month and year
        validateMonthAndYear(year, month);
        
        List<CategoryBreakdownResponse> breakdown = analyticsService.getCategoryBreakdown(year, month);
        return ResponseEntity.ok(breakdown);
    }
    
    @GetMapping("/summary/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getExpenseSummary(
            @PathVariable int year,
            @PathVariable int month) {
        
        // Validate month and year
        validateMonthAndYear(year, month);
        
        Map<String, Object> summary = analyticsService.getExpenseSummary(year, month);
        return ResponseEntity.ok(summary);
    }
    
    private void validateMonthAndYear(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100");
        }
    }
}