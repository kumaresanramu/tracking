package com.expense.tracking.service;

import com.expense.tracking.dto.CategoryBreakdownResponse;
import com.expense.tracking.dto.MonthlyTrendResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {
    
    private final ExpenseRepository expenseRepository;
    
    public List<MonthlyTrendResponse> getMonthlyTrends(int months) {
        log.debug("Fetching monthly trends for last {} months", months);
        
        List<MonthlyTrendResponse> trends = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        for (int i = 0; i < months; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            List<Expense> expenses = expenseRepository.findByYearAndMonth(
                targetMonth.getYear(), 
                targetMonth.getMonthValue()
            );
            
            BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long expenseCount = expenses.size();
            BigDecimal averageExpense = expenseCount > 0 
                ? totalAmount.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            
            trends.add(MonthlyTrendResponse.builder()
                .month(targetMonth)
                .totalAmount(totalAmount)
                .expenseCount(expenseCount)
                .averageExpense(averageExpense)
                .build());
        }
        
        // Sort by month ascending (oldest first)
        trends.sort(Comparator.comparing(MonthlyTrendResponse::getMonth));
        
        log.debug("Generated {} monthly trend entries", trends.size());
        return trends;
    }
    
    public List<CategoryBreakdownResponse> getCategoryBreakdown(int year, int month) {
        log.debug("Fetching category breakdown for {}/{}", month, year);
        
        List<Expense> expenses = expenseRepository.findByYearAndMonth(year, month);
        
        if (expenses.isEmpty()) {
            log.debug("No expenses found for {}/{}", month, year);
            return Collections.emptyList();
        }
        
        // Calculate total amount for percentage calculations
        BigDecimal totalAmount = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Group expenses by category
        Map<Category, List<Expense>> expensesByCategory = expenses.stream()
            .collect(Collectors.groupingBy(Expense::getCategory));
        
        // Build category breakdown responses
        List<CategoryBreakdownResponse> breakdown = new ArrayList<>();
        
        for (Map.Entry<Category, List<Expense>> entry : expensesByCategory.entrySet()) {
            Category category = entry.getKey();
            List<Expense> categoryExpenses = entry.getValue();
            
            BigDecimal categoryTotal = categoryExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? categoryTotal.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
            
            CategoryBreakdownResponse categoryBreakdown = CategoryBreakdownResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .categoryColor(category.getColor())
                .totalAmount(categoryTotal)
                .expenseCount((long) categoryExpenses.size())
                .percentage(percentage)
                .subcategories(new ArrayList<>()) // Will be populated if needed for hierarchical categories
                .build();
            
            breakdown.add(categoryBreakdown);
        }
        
        // Sort by total amount descending
        breakdown.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        
        log.debug("Generated breakdown for {} categories with total amount: {}", 
            breakdown.size(), totalAmount);
        
        return breakdown;
    }
    
    public Map<String, Object> getExpenseSummary(int year, int month) {
        log.debug("Fetching expense summary for {}/{}", month, year);
        
        List<Expense> expenses = expenseRepository.findByYearAndMonth(year, month);
        
        BigDecimal totalAmount = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long expenseCount = expenses.size();
        
        BigDecimal averageExpense = expenseCount > 0 
            ? totalAmount.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        Optional<BigDecimal> maxExpense = expenses.stream()
            .map(Expense::getAmount)
            .max(BigDecimal::compareTo);
        
        Optional<BigDecimal> minExpense = expenses.stream()
            .map(Expense::getAmount)
            .min(BigDecimal::compareTo);
        
        // Get unique categories count
        long uniqueCategories = expenses.stream()
            .map(expense -> expense.getCategory().getId())
            .distinct()
            .count();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAmount", totalAmount);
        summary.put("expenseCount", expenseCount);
        summary.put("averageExpense", averageExpense);
        summary.put("maxExpense", maxExpense.orElse(BigDecimal.ZERO));
        summary.put("minExpense", minExpense.orElse(BigDecimal.ZERO));
        summary.put("uniqueCategories", uniqueCategories);
        summary.put("year", year);
        summary.put("month", month);
        
        log.debug("Generated expense summary: {} expenses, total: {}", expenseCount, totalAmount);
        
        return summary;
    }
}