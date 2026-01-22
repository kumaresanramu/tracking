package com.expense.tracking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.NotificationSettingsRequest;
import com.expense.tracking.dto.NotificationSettingsResponse;
import com.expense.tracking.entity.NotificationSettings;
import com.expense.tracking.repository.NotificationSettingsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsService {
    
    private final NotificationSettingsRepository settingsRepository;
    private static final String DEFAULT_USER_ID = "default";
    
    public NotificationSettingsResponse getSettings() {
        return getSettings(DEFAULT_USER_ID);
    }
    
    public NotificationSettingsResponse getSettings(String userId) {
        NotificationSettings settings = settingsRepository.findByUserIdOrDefault(userId);
        return mapToResponse(settings);
    }
    
    @Transactional
    public NotificationSettingsResponse updateSettings(NotificationSettingsRequest request) {
        return updateSettings(DEFAULT_USER_ID, request);
    }
    
    @Transactional
    public NotificationSettingsResponse updateSettings(String userId, NotificationSettingsRequest request) {
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElse(NotificationSettings.builder().userId(userId).build());
        
        // Update settings from request
        if (request.getEnableDailyReminder() != null) {
            settings.setEnableDailyReminder(request.getEnableDailyReminder());
        }
        if (request.getDailyReminderTime() != null) {
            settings.setDailyReminderTime(request.getDailyReminderTime());
        }
        if (request.getEnableBudgetAlerts() != null) {
            settings.setEnableBudgetAlerts(request.getEnableBudgetAlerts());
        }
        if (request.getBudgetWarningThreshold() != null) {
            settings.setBudgetWarningThreshold(request.getBudgetWarningThreshold());
        }
        if (request.getEnableWeeklySummary() != null) {
            settings.setEnableWeeklySummary(request.getEnableWeeklySummary());
        }
        if (request.getWeeklySummaryDay() != null) {
            settings.setWeeklySummaryDay(request.getWeeklySummaryDay());
        }
        if (request.getWeeklySummaryTime() != null) {
            settings.setWeeklySummaryTime(request.getWeeklySummaryTime());
        }
        if (request.getEnableStreakRewards() != null) {
            settings.setEnableStreakRewards(request.getEnableStreakRewards());
        }
        if (request.getEnableBadges() != null) {
            settings.setEnableBadges(request.getEnableBadges());
        }
        if (request.getQuietHoursStart() != null) {
            settings.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            settings.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        if (request.getPreferredChannels() != null) {
            settings.setPreferredChannels(request.getPreferredChannels());
        }
        if (request.getEmailAddress() != null) {
            settings.setEmailAddress(request.getEmailAddress());
        }
        if (request.getEnableEmailNotifications() != null) {
            settings.setEnableEmailNotifications(request.getEnableEmailNotifications());
        }
        
        settings = settingsRepository.save(settings);
        log.info("Updated notification settings for user: {}", userId);
        
        return mapToResponse(settings);
    }
    
    @Transactional
    public void resetToDefaults() {
        resetToDefaults(DEFAULT_USER_ID);
    }
    
    @Transactional
    public void resetToDefaults(String userId) {
        settingsRepository.findByUserId(userId).ifPresent(settingsRepository::delete);
        log.info("Reset notification settings to defaults for user: {}", userId);
    }
    
    private NotificationSettingsResponse mapToResponse(NotificationSettings settings) {
        return NotificationSettingsResponse.builder()
                .id(settings.getId())
                .userId(settings.getUserId())
                .enableDailyReminder(settings.getEnableDailyReminder())
                .dailyReminderTime(settings.getDailyReminderTime())
                .enableBudgetAlerts(settings.getEnableBudgetAlerts())
                .budgetWarningThreshold(settings.getBudgetWarningThreshold())
                .enableWeeklySummary(settings.getEnableWeeklySummary())
                .weeklySummaryDay(settings.getWeeklySummaryDay())
                .weeklySummaryTime(settings.getWeeklySummaryTime())
                .enableStreakRewards(settings.getEnableStreakRewards())
                .enableBadges(settings.getEnableBadges())
                .quietHoursStart(settings.getQuietHoursStart())
                .quietHoursEnd(settings.getQuietHoursEnd())
                .isInQuietHours(settings.isInQuietHours())
                .preferredChannels(settings.getPreferredChannels())
                .emailAddress(settings.getEmailAddress())
                .enableEmailNotifications(settings.getEnableEmailNotifications())
                .build();
    }
}