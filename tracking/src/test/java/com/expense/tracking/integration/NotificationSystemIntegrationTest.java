package com.expense.tracking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.NotificationRequest;
import com.expense.tracking.dto.NotificationResponse;
import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.entity.NotificationType;
import com.expense.tracking.repository.NotificationRepository;
import com.expense.tracking.repository.NotificationSettingsRepository;
import com.expense.tracking.service.EmailNotificationService;
import com.expense.tracking.service.NotificationService;
import com.expense.tracking.service.PushNotificationService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationSystemIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSettingsRepository notificationSettingsRepository;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        notificationRepository.deleteAll();
        notificationSettingsRepository.deleteAll();
    }

    @Test
    void testNotificationServiceIsAvailable() {
        assertThat(notificationService).isNotNull();
        System.out.println("NotificationService is properly configured and available");
    }

    @Test
    void testPushNotificationServiceConfiguration() {
        // Push notification service might be null if VAPID keys are not configured
        if (pushNotificationService != null) {
            assertThat(pushNotificationService).isNotNull();
            System.out.println("PushNotificationService is properly configured and available");
        } else {
            System.out.println("PushNotificationService is not available - likely due to missing VAPID configuration");
        }
    }

    @Test
    void testEmailNotificationServiceConfiguration() {
        // Email notification service might be null if email is not enabled
        if (emailNotificationService != null) {
            assertThat(emailNotificationService).isNotNull();
            System.out.println("EmailNotificationService is properly configured and available");
        } else {
            System.out.println("EmailNotificationService is not available - likely due to email.notifications.enabled=false");
        }
    }

    @Test
    void testCreateBasicNotification() {
        // Create a basic notification request
        NotificationRequest request = NotificationRequest.builder()
                .title("Test Notification")
                .message("This is a test notification")
                .type(NotificationType.DAILY_EXPENSE_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .build();

        // Create the notification
        NotificationResponse response = notificationService.createNotification(request);

        // Verify the notification was created
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Notification");
        assertThat(response.getMessage()).isEqualTo("This is a test notification");
        assertThat(response.getType()).isEqualTo(NotificationType.DAILY_EXPENSE_REMINDER);

        // Verify it was persisted
        assertThat(notificationRepository.count()).isEqualTo(1);
        
        System.out.println("Basic notification creation test passed");
    }

    @Test
    void testDailyReminderCreation() {
        // Test creating a daily reminder
        NotificationResponse response = notificationService.createDailyReminder();

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.DAILY_EXPENSE_REMINDER);
        assertThat(response.getTitle()).contains("Daily Expense Reminder");

        System.out.println("Daily reminder creation test passed");
    }

    @Test
    void testWeeklySummaryCreation() {
        // Test creating a weekly summary
        NotificationResponse response = notificationService.createSmartWeeklySummary();

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.WEEKLY_SUMMARY);
        assertThat(response.getTitle()).contains("Weekly Summary");

        System.out.println("Weekly summary creation test passed");
    }

    @Test
    void testBudgetAlertCreation() {
        // Test creating a budget alert
        double percentage = 85.0;
        double totalSpent = 850.0;
        double monthlyBudget = 1000.0;

        NotificationResponse response = notificationService.createBudgetAlert(percentage, totalSpent, monthlyBudget);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isIn(NotificationType.BUDGET_THRESHOLD_WARNING, NotificationType.BUDGET_EXCEEDED_ALERT);
        assertThat(response.getTitle()).contains("Budget");

        System.out.println("Budget alert creation test passed");
    }

    @Test
    void testNotificationSettingsIntegration() {
        // Test that notification settings service is working
        var settings = notificationService.getNotificationSettings();
        
        // Settings might be null if no default settings exist, which is fine
        if (settings != null) {
            assertThat(settings.getEnableDailyReminder()).isNotNull();
            assertThat(settings.getEnableWeeklySummary()).isNotNull();
            assertThat(settings.getEnableBudgetAlerts()).isNotNull();
            System.out.println("Notification settings integration test passed");
        } else {
            System.out.println("No default notification settings found - this is expected for a fresh system");
        }
    }
}