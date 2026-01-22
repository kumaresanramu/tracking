package com.expense.tracking.entity;

import java.time.LocalTime;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // User identifier (for future multi-user support)
    @Column(name = "user_id")
    @Builder.Default
    private String userId = "default";
    
    // Daily expense reminder settings
    @Column(name = "enable_daily_reminder")
    @Builder.Default
    private Boolean enableDailyReminder = true;
    
    @Column(name = "daily_reminder_time")
    @Builder.Default
    private LocalTime dailyReminderTime = LocalTime.of(20, 0); // 8:00 PM
    
    // Budget alert settings
    @Column(name = "enable_budget_alerts")
    @Builder.Default
    private Boolean enableBudgetAlerts = true;
    
    @Column(name = "budget_warning_threshold")
    @Builder.Default
    private Integer budgetWarningThreshold = 80; // 80%
    
    // Weekly summary settings
    @Column(name = "enable_weekly_summary")
    @Builder.Default
    private Boolean enableWeeklySummary = true;
    
    @Column(name = "weekly_summary_day")
    @Builder.Default
    private Integer weeklySummaryDay = 7; // Sunday = 7
    
    @Column(name = "weekly_summary_time")
    @Builder.Default
    private LocalTime weeklySummaryTime = LocalTime.of(9, 0); // 9:00 AM
    
    // Gamification settings
    @Column(name = "enable_streak_rewards")
    @Builder.Default
    private Boolean enableStreakRewards = true;
    
    @Column(name = "enable_badges")
    @Builder.Default
    private Boolean enableBadges = true;
    
    // Quiet hours
    @Column(name = "quiet_hours_start")
    @Builder.Default
    private LocalTime quietHoursStart = LocalTime.of(22, 0); // 10:00 PM
    
    @Column(name = "quiet_hours_end")
    @Builder.Default
    private LocalTime quietHoursEnd = LocalTime.of(8, 0); // 8:00 AM
    
    // Preferred channels
    @ElementCollection(targetClass = NotificationChannel.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "notification_channels", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "channel")
    @Builder.Default
    private Set<NotificationChannel> preferredChannels = Set.of(NotificationChannel.IN_APP, NotificationChannel.PUSH);
    
    // Email settings
    @Column(name = "email_address", length = 255)
    private String emailAddress;
    
    @Column(name = "enable_email_notifications")
    @Builder.Default
    private Boolean enableEmailNotifications = false;
    
    // Helper methods
    public boolean isInQuietHours() {
        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            // Same day quiet hours (e.g., 22:00 to 23:59)
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        } else {
            // Overnight quiet hours (e.g., 22:00 to 08:00)
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }
    }
}