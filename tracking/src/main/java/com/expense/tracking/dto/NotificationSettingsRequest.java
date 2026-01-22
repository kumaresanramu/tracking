package com.expense.tracking.dto;

import java.time.LocalTime;
import java.util.Set;

import com.expense.tracking.entity.NotificationChannel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsRequest {
    
    // Daily reminder settings
    private Boolean enableDailyReminder;
    private LocalTime dailyReminderTime;
    
    // Budget alert settings
    private Boolean enableBudgetAlerts;
    
    @Min(value = 50, message = "Budget warning threshold must be at least 50%")
    @Max(value = 100, message = "Budget warning threshold must not exceed 100%")
    private Integer budgetWarningThreshold;
    
    // Weekly summary settings
    private Boolean enableWeeklySummary;
    
    @Min(value = 1, message = "Weekly summary day must be between 1 (Monday) and 7 (Sunday)")
    @Max(value = 7, message = "Weekly summary day must be between 1 (Monday) and 7 (Sunday)")
    private Integer weeklySummaryDay;
    
    private LocalTime weeklySummaryTime;
    
    // Gamification settings
    private Boolean enableStreakRewards;
    private Boolean enableBadges;
    
    // Quiet hours
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    
    // Preferred channels
    private Set<NotificationChannel> preferredChannels;
    
    // Email settings
    @Email(message = "Please provide a valid email address")
    private String emailAddress;
    
    private Boolean enableEmailNotifications;
}