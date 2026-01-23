package com.expense.tracking.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.NotificationSettingsResponse;
import com.expense.tracking.entity.NotificationChannel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing notification permissions and enforcement.
 * Handles permission requests, validation, and cleanup when permissions are revoked.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPermissionService {
    
    private final NotificationSettingsService notificationSettingsService;
    private final PushSubscriptionService pushSubscriptionService;
    
    /**
     * Validates if a notification can be sent based on user preferences and permissions.
     */
    public boolean canSendNotification(String notificationType, NotificationChannel channel) {
        NotificationSettingsResponse settings = notificationSettingsService.getSettings();
        
        // Check if the channel is enabled
        if (!settings.getPreferredChannels().contains(channel)) {
            log.debug("Channel {} is not enabled for user", channel);
            return false;
        }
        
        // Check if we're in quiet hours for non-urgent notifications
        if (settings.getIsInQuietHours() && !isUrgentNotificationType(notificationType)) {
            log.debug("Notification blocked due to quiet hours: {}", notificationType);
            return false;
        }
        
        // Channel-specific permission checks
        switch (channel) {
            case EMAIL -> {
                if (!settings.getEnableEmailNotifications() || settings.getEmailAddress() == null) {
                    log.debug("Email notifications disabled or no email address configured");
                    return false;
                }
            }
            case PUSH -> {
                // Push notifications require active subscriptions
                if (pushSubscriptionService.getAllActiveSubscriptions().isEmpty()) {
                    log.debug("No active push subscriptions available");
                    return false;
                }
            }
            case IN_APP -> {
                // In-app notifications are always allowed if the channel is enabled
                return true;
            }
        }
        
        return true;
    }
    
    /**
     * Gets the effective notification channels for a user based on their preferences and permissions.
     */
    public Set<NotificationChannel> getEffectiveChannels() {
        NotificationSettingsResponse settings = notificationSettingsService.getSettings();
        Set<NotificationChannel> preferredChannels = settings.getPreferredChannels();
        
        // Filter out channels that don't have proper permissions
        return preferredChannels.stream()
                .filter(channel -> hasChannelPermission(channel, settings))
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Handles permission revocation cleanup.
     * This method is called when a user revokes permissions for a specific channel.
     */
    @Transactional
    public void handlePermissionRevocation(NotificationChannel channel) {
        log.info("Handling permission revocation for channel: {}", channel);
        
        switch (channel) {
            case PUSH -> {
                // Deactivate all push subscriptions
                try {
                    pushSubscriptionService.getAllActiveSubscriptions()
                            .forEach(subscription -> {
                                try {
                                    pushSubscriptionService.deactivateSubscription(subscription.getEndpoint());
                                } catch (Exception e) {
                                    log.error("Failed to deactivate push subscription: {}", subscription.getEndpoint(), e);
                                }
                            });
                    log.info("Deactivated all push subscriptions due to permission revocation");
                } catch (Exception e) {
                    log.error("Error during push subscription cleanup", e);
                }
            }
            case EMAIL -> {
                // Clear email address from settings
                try {
                    var currentSettings = notificationSettingsService.getSettings();
                    var updateRequest = com.expense.tracking.dto.NotificationSettingsRequest.builder()
                            .enableEmailNotifications(false)
                            .emailAddress(null)
                            .build();
                    notificationSettingsService.updateSettings(updateRequest);
                    log.info("Cleared email settings due to permission revocation");
                } catch (Exception e) {
                    log.error("Error clearing email settings", e);
                }
            }
            case IN_APP -> {
                // In-app notifications don't require special cleanup
                log.info("In-app notification permission revoked - no cleanup required");
            }
        }
    }
    
    /**
     * Requests permission for a new notification type.
     * This method validates the request and updates user preferences if appropriate.
     */
    @Transactional
    public boolean requestNotificationPermission(NotificationChannel channel, String reason) {
        log.info("Requesting permission for channel: {} with reason: {}", channel, reason);
        
        NotificationSettingsResponse currentSettings = notificationSettingsService.getSettings();
        
        // Check if permission is already granted
        if (currentSettings.getPreferredChannels().contains(channel)) {
            log.debug("Permission already granted for channel: {}", channel);
            return true;
        }
        
        // For now, we'll automatically grant the permission and let the frontend handle the actual browser permission request
        // In a real application, this might involve more complex logic or user confirmation
        
        try {
            Set<NotificationChannel> updatedChannels = new java.util.HashSet<>(currentSettings.getPreferredChannels());
            updatedChannels.add(channel);
            
            var updateRequest = com.expense.tracking.dto.NotificationSettingsRequest.builder()
                    .preferredChannels(updatedChannels)
                    .build();
            
            notificationSettingsService.updateSettings(updateRequest);
            log.info("Granted permission for channel: {}", channel);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to grant permission for channel: {}", channel, e);
            return false;
        }
    }
    
    /**
     * Validates notification preferences and ensures they are consistent.
     */
    public void validateAndEnforcePreferences() {
        NotificationSettingsResponse settings = notificationSettingsService.getSettings();
        boolean needsUpdate = false;
        Set<NotificationChannel> validChannels = new java.util.HashSet<>();
        
        // Validate each preferred channel
        for (NotificationChannel channel : settings.getPreferredChannels()) {
            if (hasChannelPermission(channel, settings)) {
                validChannels.add(channel);
            } else {
                log.warn("Removing invalid channel from preferences: {}", channel);
                needsUpdate = true;
            }
        }
        
        // Update preferences if needed
        if (needsUpdate) {
            try {
                var updateRequest = com.expense.tracking.dto.NotificationSettingsRequest.builder()
                        .preferredChannels(validChannels)
                        .build();
                notificationSettingsService.updateSettings(updateRequest);
                log.info("Updated notification preferences to remove invalid channels");
            } catch (Exception e) {
                log.error("Failed to update notification preferences", e);
            }
        }
    }
    
    private boolean hasChannelPermission(NotificationChannel channel, NotificationSettingsResponse settings) {
        return switch (channel) {
            case EMAIL -> settings.getEnableEmailNotifications() && settings.getEmailAddress() != null;
            case PUSH -> !pushSubscriptionService.getAllActiveSubscriptions().isEmpty();
            case IN_APP -> true; // In-app notifications don't require special permissions
        };
    }
    
    private boolean isUrgentNotificationType(String notificationType) {
        return "BUDGET_EXCEEDED_ALERT".equals(notificationType) || 
               "BUDGET_THRESHOLD_WARNING".equals(notificationType);
    }
    
    /**
     * Gets a summary of current notification permissions.
     */
    public NotificationPermissionStatus getPermissionStatus() {
        NotificationSettingsResponse settings = notificationSettingsService.getSettings();
        
        boolean pushAvailable = !pushSubscriptionService.getAllActiveSubscriptions().isEmpty();
        boolean emailAvailable = settings.getEnableEmailNotifications() && settings.getEmailAddress() != null;
        boolean inAppAvailable = true;
        
        return NotificationPermissionStatus.builder()
                .pushEnabled(settings.getPreferredChannels().contains(NotificationChannel.PUSH))
                .pushAvailable(pushAvailable)
                .emailEnabled(settings.getPreferredChannels().contains(NotificationChannel.EMAIL))
                .emailAvailable(emailAvailable)
                .inAppEnabled(settings.getPreferredChannels().contains(NotificationChannel.IN_APP))
                .inAppAvailable(inAppAvailable)
                .inQuietHours(settings.getIsInQuietHours())
                .effectiveChannels(getEffectiveChannels())
                .build();
    }
    
    /**
     * Data class for notification permission status.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationPermissionStatus {
        private boolean pushEnabled;
        private boolean pushAvailable;
        private boolean emailEnabled;
        private boolean emailAvailable;
        private boolean inAppEnabled;
        private boolean inAppAvailable;
        private boolean inQuietHours;
        private Set<NotificationChannel> effectiveChannels;
    }
}