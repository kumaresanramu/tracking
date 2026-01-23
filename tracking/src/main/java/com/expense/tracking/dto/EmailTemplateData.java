package com.expense.tracking.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.expense.tracking.entity.Expense;

import lombok.Builder;
import lombok.Data;

public class EmailTemplateData {

    @Data
    @Builder
    public static class DailyReminderData {
        private String userName;
        private LocalDate date;
        private int streakDays;
        private double monthlyBudget;
        private double currentSpending;
        private String topCategory;
    }

    @Data
    @Builder
    public static class WeeklySummaryData {
        private String userName;
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private double totalSpent;
        private Map<String, Double> categoryBreakdown;
        private List<Expense> topExpenses;
        private String chartImageUrl;
    }

    @Data
    @Builder
    public static class BudgetAlertData {
        private String userName;
        private double budgetAmount;
        private double currentSpending;
        private double percentage;
        private String alertType; // 'warning' or 'exceeded'
        private List<String> topCategories;
    }
}