package com.expense.tracking.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.expense.tracking.entity.NotificationSettings;
import com.expense.tracking.repository.NotificationSettingsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulerService {
    
    private final NotificationSettingsRepository settingsRepository;
    private final NotificationService notificationService;
    
    // Run every minute to check for scheduled notifications
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void checkScheduledNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();
            
            // Get all notification settings
            List<NotificationSettings> allSettings = settingsRepository.findAll();
            
            for (NotificationSettings settings : allSettings) {
                // Check daily reminders
                if (settings.getEnableDailyReminder() && settings.getDailyReminderTime() != null) {
                    LocalTime reminderTime = settings.getDailyReminderTime();
                    
                    // Check if current time matches reminder time (within 1 minute)
                    if (isTimeMatch(currentTime, reminderTime)) {
                        log.info("Triggering daily reminder for user: {}", settings.getUserId());
                        notificationService.createDailyReminder();
                    }
                }
                
                // Check weekly summary (Sunday at specified time)
                if (settings.getEnableWeeklySummary() && settings.getWeeklySummaryTime() != null) {
                    LocalTime summaryTime = settings.getWeeklySummaryTime();
                    
                    // Check if it's Sunday and time matches
                    if (now.getDayOfWeek().getValue() == 7 && isTimeMatch(currentTime, summaryTime)) {
                        log.info("Triggering weekly summary for user: {}", settings.getUserId());
                        notificationService.createSmartWeeklySummary();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in notification scheduler", e);
        }
    }
    
    private boolean isTimeMatch(LocalTime currentTime, LocalTime targetTime) {
        // Check if times match within 1 minute
        int currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();
        int targetMinutes = targetTime.getHour() * 60 + targetTime.getMinute();
        
        return Math.abs(currentMinutes - targetMinutes) <= 1;
    }
    
    // Manual trigger for testing
    public void triggerDailyReminders() {
        List<NotificationSettings> allSettings = settingsRepository.findAll();
        
        for (NotificationSettings settings : allSettings) {
            if (settings.getEnableDailyReminder()) {
                log.info("Manually triggering daily reminder for user: {}", settings.getUserId());
                notificationService.createDailyReminder();
            }
        }
    }
    
    // Manual trigger for weekly summaries
    public void triggerWeeklySummaries() {
        List<NotificationSettings> allSettings = settingsRepository.findAll();
        
        for (NotificationSettings settings : allSettings) {
            if (settings.getEnableWeeklySummary()) {
                log.info("Manually triggering weekly summary for user: {}", settings.getUserId());
                notificationService.createSmartWeeklySummary();
            }
        }
    }
}