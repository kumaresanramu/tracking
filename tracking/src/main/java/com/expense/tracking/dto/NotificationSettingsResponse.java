package com.expense.tracking.dto;

import java.time.LocalTime;
import java.util.Set;

import com.expense.tracking.entity.NotificationChannel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSettingsResponse {
    
    private Long id;
    private String userId;
    
    // Daily reminder settings
    private Boolean enableDailyReminder;
    private LocalTime dailyReminderTime;
    
    // Budget alert settings
    private Boolean enableBudgetAlerts;
    private Integer budgetWarningThreshold;
    
    // Weekly summary settings
    private Boolean enableWeeklySummary;
    private Integer weeklySummaryDay;
    private LocalTime weeklySummaryTime;
    
    // Gamification settings
    private Boolean enableStreakRewards;
    private Boolean enableBadges;
    
    // Quiet hours
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private Boolean isInQuietHours;
    
    // Preferred channels
    private Set<NotificationChannel> preferredChannels;
    
    // Email settings
    private String emailAddress;
    private Boolean enableEmailNotifications;
}