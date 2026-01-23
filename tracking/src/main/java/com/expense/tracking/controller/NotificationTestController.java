package com.expense.tracking.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expense.tracking.dto.NotificationRequest;
import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.entity.NotificationType;
import com.expense.tracking.service.NotificationService;
import com.expense.tracking.service.PushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test controller for debugging notification issues.
 */
@RestController
@RequestMapping("/api/test/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestController {
    
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    
    /**
     * Test push notification delivery
     */
    @PostMapping("/test-push")
    public ResponseEntity<String> testPushNotification() {
        try {
            // Create test notification payload
            PushNotificationService.NotificationPayload payload = 
                new PushNotificationService.NotificationPayload.Builder()
                    .title("Test Push Notification")
                    .body("This is a test notification to verify push delivery is working")
                    .icon("/icons/icon-192x192.svg")
                    .tag("test-push")
                    .requireInteraction(false)
                    .build();
            
            CompletableFuture<Integer> result = pushNotificationService.sendToAllActiveSubscriptions(payload);
            Integer count = result.get(); // Wait for completion
            
            return ResponseEntity.ok("Test push notification sent to " + count + " subscribers");
            
        } catch (Exception e) {
            log.error("Failed to send test push notification", e);
            return ResponseEntity.internalServerError()
                .body("Failed to send test push notification: " + e.getMessage());
        }
    }
    
    /**
     * Test budget alert notification
     */
    @PostMapping("/test-budget-alert")
    public ResponseEntity<String> testBudgetAlert(@RequestParam(defaultValue = "85") double percentage) {
        try {
            double budget = 10000.0;
            double spent = (percentage / 100.0) * budget;
            
            var notification = notificationService.createBudgetAlert(percentage, spent, budget);
            
            if (notification != null) {
                return ResponseEntity.ok("Budget alert notification created: " + notification.getTitle());
            } else {
                return ResponseEntity.ok("Budget alert notification was not created (possibly disabled in settings)");
            }
            
        } catch (Exception e) {
            log.error("Failed to create test budget alert", e);
            return ResponseEntity.internalServerError()
                .body("Failed to create test budget alert: " + e.getMessage());
        }
    }
    
    /**
     * Test payment reminder notification
     */
    @PostMapping("/test-payment-reminder")
    public ResponseEntity<String> testPaymentReminder() {
        try {
            NotificationRequest request = NotificationRequest.builder()
                    .title("Test Payment Reminder")
                    .message("This is a test payment reminder notification")
                    .type(NotificationType.PAYMENT_REMINDER)
                    .channel(NotificationChannel.IN_APP)
                    .icon("💰")
                    .actionUrl("/reminders")
                    .actionLabel("View Reminders")
                    .priority(2)
                    .build();
            
            var notification = notificationService.createNotification(request);
            
            if (notification != null) {
                return ResponseEntity.ok("Payment reminder notification created: " + notification.getTitle());
            } else {
                return ResponseEntity.ok("Payment reminder notification was not created");
            }
            
        } catch (Exception e) {
            log.error("Failed to create test payment reminder", e);
            return ResponseEntity.internalServerError()
                .body("Failed to create test payment reminder: " + e.getMessage());
        }
    }
    
    /**
     * Get notification system status
     */
    @GetMapping("/status")
    public ResponseEntity<NotificationSystemStatus> getNotificationSystemStatus() {
        try {
            // Check push notification service
            boolean pushServiceAvailable = pushNotificationService != null;
            
            // Get notification settings
            var settings = notificationService.getNotificationSettings();
            
            // Count unread notifications
            long unreadCount = notificationService.getUnreadCount();
            
            NotificationSystemStatus status = NotificationSystemStatus.builder()
                    .pushServiceAvailable(pushServiceAvailable)
                    .budgetAlertsEnabled(settings != null && settings.getEnableBudgetAlerts())
                    .dailyRemindersEnabled(settings != null && settings.getEnableDailyReminder())
                    .unreadNotificationCount(unreadCount)
                    .budgetWarningThreshold(settings != null && settings.getBudgetWarningThreshold() != null ? settings.getBudgetWarningThreshold() : 80)
                    .build();
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Failed to get notification system status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Trigger budget check manually
     */
    @PostMapping("/trigger-budget-check")
    public ResponseEntity<String> triggerBudgetCheck(@RequestParam(defaultValue = "1000") double expenseAmount) {
        try {
            // This would normally be called by ExpenseService when creating an expense
            // We'll simulate it by calling the budget alert creation directly
            
            // Calculate current month spending (simplified)
            double monthlyBudget = 10000.0;
            double currentSpending = expenseAmount; // Simplified for testing
            double percentage = (currentSpending / monthlyBudget) * 100;
            
            if (percentage >= 80) {
                var notification = notificationService.createBudgetAlert(percentage, currentSpending, monthlyBudget);
                if (notification != null) {
                    return ResponseEntity.ok("Budget alert triggered: " + notification.getTitle());
                } else {
                    return ResponseEntity.ok("Budget alert not created (possibly disabled)");
                }
            } else {
                return ResponseEntity.ok("Expense amount (" + expenseAmount + ") does not trigger budget alert (threshold: 80%)");
            }
            
        } catch (Exception e) {
            log.error("Failed to trigger budget check", e);
            return ResponseEntity.internalServerError()
                .body("Failed to trigger budget check: " + e.getMessage());
        }
    }
    
    /**
     * Data class for notification system status
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationSystemStatus {
        private boolean pushServiceAvailable;
        private boolean budgetAlertsEnabled;
        private boolean dailyRemindersEnabled;
        private long unreadNotificationCount;
        private Integer budgetWarningThreshold;
    }
}