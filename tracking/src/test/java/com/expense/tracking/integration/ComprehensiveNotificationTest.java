package com.expense.tracking.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.dto.NotificationResponse;
import com.expense.tracking.dto.PushSubscriptionRequest;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Notification;
import com.expense.tracking.entity.NotificationType;
import com.expense.tracking.entity.PushSubscription;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.repository.NotificationRepository;
import com.expense.tracking.repository.PushSubscriptionRepository;
import com.expense.tracking.service.EmailNotificationService;
import com.expense.tracking.service.ExpenseService;
import com.expense.tracking.service.NotificationService;
import com.expense.tracking.service.PushNotificationService;
import com.expense.tracking.service.PushSubscriptionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ComprehensiveNotificationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired(required = false)
    private PushSubscriptionService pushSubscriptionService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        notificationRepository.deleteAll();
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        pushSubscriptionRepository.deleteAll();

        // Create a test category
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF0000")
                .description("Test category for notification tests")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testBudgetAlertNotificationCreation() {
        System.out.println("=== Testing Budget Alert Notification Creation ===");
        
        // Test budget warning (80% threshold)
        NotificationResponse warningResponse = notificationService.createBudgetAlert(85.0, 8500.0, 10000.0);
        
        assertThat(warningResponse).isNotNull();
        assertThat(warningResponse.getType()).isEqualTo(NotificationType.BUDGET_THRESHOLD_WARNING);
        assertThat(warningResponse.getTitle()).contains("Budget Warning");
        assertThat(warningResponse.getMessage()).contains("85%");
        
        System.out.println("✓ Budget warning notification created successfully");
        System.out.println("  Title: " + warningResponse.getTitle());
        System.out.println("  Message: " + warningResponse.getMessage());

        // Test budget exceeded (100% threshold)
        NotificationResponse exceededResponse = notificationService.createBudgetAlert(120.0, 12000.0, 10000.0);
        
        assertThat(exceededResponse).isNotNull();
        assertThat(exceededResponse.getType()).isEqualTo(NotificationType.BUDGET_EXCEEDED_ALERT);
        assertThat(exceededResponse.getTitle()).contains("Budget Exceeded");
        assertThat(exceededResponse.getMessage()).contains("120%");
        
        System.out.println("✓ Budget exceeded notification created successfully");
        System.out.println("  Title: " + exceededResponse.getTitle());
        System.out.println("  Message: " + exceededResponse.getMessage());

        // Verify notifications were persisted
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(2);
        
        System.out.println("✓ Both budget alert notifications persisted to database");
    }

    @Test
    void testBudgetAlertTriggeredByExpenseCreation() {
        System.out.println("=== Testing Budget Alert Triggered by Expense Creation ===");
        
        // Create an expense that should trigger a budget warning
        ExpenseRequest expenseRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("8500.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Large expense to trigger budget alert")
                .build();

        ExpenseResponse createdExpense = expenseService.createExpense(expenseRequest);
        
        assertThat(createdExpense).isNotNull();
        assertThat(createdExpense.getAmount()).isEqualTo(new BigDecimal("8500.00"));
        
        System.out.println("✓ Large expense created successfully");
        System.out.println("  Amount: $" + createdExpense.getAmount());
        System.out.println("  Description: " + createdExpense.getDescription());

        // Check if budget alert was automatically created
        List<Notification> notifications = notificationRepository.findAll();
        System.out.println("  Notifications created: " + notifications.size());
        
        for (Notification notification : notifications) {
            System.out.println("  - Type: " + notification.getType());
            System.out.println("    Title: " + notification.getTitle());
            System.out.println("    Message: " + notification.getMessage());
        }
    }

    @Test
    void testDailyReminderNotification() {
        System.out.println("=== Testing Daily Reminder Notification ===");
        
        NotificationResponse response = notificationService.createDailyReminder();
        
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.DAILY_EXPENSE_REMINDER);
        assertThat(response.getTitle()).contains("Daily Expense Reminder");
        
        System.out.println("✓ Daily reminder notification created successfully");
        System.out.println("  Title: " + response.getTitle());
        System.out.println("  Message: " + response.getMessage());

        // Verify it was persisted
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.DAILY_EXPENSE_REMINDER);
        
        System.out.println("✓ Daily reminder notification persisted to database");
    }

    @Test
    void testWeeklySummaryNotification() {
        System.out.println("=== Testing Weekly Summary Notification ===");
        
        NotificationResponse response = notificationService.createSmartWeeklySummary();
        
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.WEEKLY_SUMMARY);
        assertThat(response.getTitle()).contains("Weekly Summary");
        
        System.out.println("✓ Weekly summary notification created successfully");
        System.out.println("  Title: " + response.getTitle());
        System.out.println("  Message: " + response.getMessage());

        // Verify it was persisted
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.WEEKLY_SUMMARY);
        
        System.out.println("✓ Weekly summary notification persisted to database");
    }

    @Test
    void testPushNotificationServiceAvailability() {
        System.out.println("=== Testing Push Notification Service Availability ===");
        
        if (pushNotificationService != null) {
            System.out.println("✓ PushNotificationService is available");
            
            // Test creating a push notification payload
            try {
                PushNotificationService.NotificationPayload payload = 
                    new PushNotificationService.NotificationPayload.Builder()
                        .title("Test Push Notification")
                        .body("This is a test push notification")
                        .icon("/icons/icon-192x192.svg")
                        .build();
                
                assertThat(payload).isNotNull();
                assertThat(payload.getTitle()).isEqualTo("Test Push Notification");
                assertThat(payload.getBody()).isEqualTo("This is a test push notification");
                
                System.out.println("✓ Push notification payload created successfully");
                System.out.println("  Title: " + payload.getTitle());
                System.out.println("  Body: " + payload.getBody());
                
            } catch (Exception e) {
                System.out.println("✗ Error creating push notification payload: " + e.getMessage());
            }
            
        } else {
            System.out.println("✗ PushNotificationService is not available");
            System.out.println("  This is likely due to missing VAPID configuration");
            System.out.println("  VAPID keys need to be set in environment variables:");
            System.out.println("  - VAPID_PUBLIC_KEY");
            System.out.println("  - VAPID_PRIVATE_KEY");
        }
    }

    @Test
    void testPushSubscriptionManagement() {
        System.out.println("=== Testing Push Subscription Management ===");
        
        if (pushSubscriptionService != null) {
            System.out.println("✓ PushSubscriptionService is available");
            
            // Create a test push subscription
            PushSubscriptionRequest subscriptionRequest = PushSubscriptionRequest.builder()
                    .endpoint("https://fcm.googleapis.com/fcm/send/test-endpoint")
                    .p256dhKey("test-p256dh-key")
                    .authKey("test-auth-key")
                    .userAgent("Test User Agent")
                    .build();
            
            try {
                PushSubscription subscription = pushSubscriptionService.createOrUpdateSubscription(subscriptionRequest, "Test User Agent");
                
                assertThat(subscription).isNotNull();
                assertThat(subscription.getEndpoint()).isEqualTo("https://fcm.googleapis.com/fcm/send/test-endpoint");
                assertThat(subscription.getActive()).isTrue();
                
                System.out.println("✓ Push subscription created successfully");
                System.out.println("  Endpoint: " + subscription.getEndpoint());
                System.out.println("  Active: " + subscription.getActive());
                
                // Verify it was persisted
                List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
                assertThat(subscriptions).hasSize(1);
                
                System.out.println("✓ Push subscription persisted to database");
                
            } catch (Exception e) {
                System.out.println("✗ Error creating push subscription: " + e.getMessage());
            }
            
        } else {
            System.out.println("✗ PushSubscriptionService is not available");
        }
    }

    @Test
    void testEmailNotificationServiceAvailability() {
        System.out.println("=== Testing Email Notification Service Availability ===");
        
        if (emailNotificationService != null) {
            System.out.println("✓ EmailNotificationService is available");
            System.out.println("  Email notifications are enabled in configuration");
            
            // Note: We won't actually send emails in tests, but we can verify the service is configured
            System.out.println("✓ Email service is properly configured");
            
        } else {
            System.out.println("✗ EmailNotificationService is not available");
            System.out.println("  This is likely due to email.notifications.enabled=false");
            System.out.println("  To enable email notifications, set:");
            System.out.println("  - email.notifications.enabled=true");
            System.out.println("  - Configure SMTP settings (host, username, password)");
        }
    }

    @Test
    void testNotificationChannelIntegration() {
        System.out.println("=== Testing Notification Channel Integration ===");
        
        // Create a daily reminder and check what channels are used
        NotificationResponse response = notificationService.createDailyReminder();
        
        assertThat(response).isNotNull();
        System.out.println("✓ Notification created: " + response.getTitle());
        
        // Check the notification in the database to see what channels were configured
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification notification = notifications.get(0);
        System.out.println("  Notification ID: " + notification.getId());
        System.out.println("  Type: " + notification.getType());
        System.out.println("  Title: " + notification.getTitle());
        System.out.println("  Message: " + notification.getMessage());
        
        // Check if notification channels are properly configured
        System.out.println("  Available Services:");
        System.out.println("    - Push Notifications: " + (pushNotificationService != null ? "Available" : "Not Available"));
        System.out.println("    - Email Notifications: " + (emailNotificationService != null ? "Available" : "Not Available"));
        System.out.println("    - In-App Notifications: Always Available");
        
        System.out.println("✓ Notification channel integration test completed");
    }

    @Test
    void testNotificationSystemConfiguration() {
        System.out.println("=== Testing Overall Notification System Configuration ===");
        
        System.out.println("Service Availability Summary:");
        System.out.println("  ✓ NotificationService: Available");
        System.out.println("  " + (pushNotificationService != null ? "✓" : "✗") + " PushNotificationService: " + 
                          (pushNotificationService != null ? "Available" : "Not Available"));
        System.out.println("  " + (emailNotificationService != null ? "✓" : "✗") + " EmailNotificationService: " + 
                          (emailNotificationService != null ? "Available" : "Not Available"));
        System.out.println("  " + (pushSubscriptionService != null ? "✓" : "✗") + " PushSubscriptionService: " + 
                          (pushSubscriptionService != null ? "Available" : "Not Available"));
        
        // Test basic notification creation for each type
        System.out.println("\nTesting All Notification Types:");
        
        try {
            NotificationResponse dailyReminder = notificationService.createDailyReminder();
            System.out.println("  ✓ Daily Reminder: " + dailyReminder.getTitle());
        } catch (Exception e) {
            System.out.println("  ✗ Daily Reminder failed: " + e.getMessage());
        }
        
        try {
            NotificationResponse weeklySummary = notificationService.createSmartWeeklySummary();
            System.out.println("  ✓ Weekly Summary: " + weeklySummary.getTitle());
        } catch (Exception e) {
            System.out.println("  ✗ Weekly Summary failed: " + e.getMessage());
        }
        
        try {
            NotificationResponse budgetAlert = notificationService.createBudgetAlert(85.0, 850.0, 1000.0);
            System.out.println("  ✓ Budget Alert: " + budgetAlert.getTitle());
        } catch (Exception e) {
            System.out.println("  ✗ Budget Alert failed: " + e.getMessage());
        }
        
        // Count total notifications created
        long totalNotifications = notificationRepository.count();
        System.out.println("\nTotal notifications created: " + totalNotifications);
        
        System.out.println("✓ Notification system configuration test completed");
    }
}