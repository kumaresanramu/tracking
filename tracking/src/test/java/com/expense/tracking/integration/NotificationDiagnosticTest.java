package com.expense.tracking.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.dto.NotificationSettingsRequest;
import com.expense.tracking.dto.NotificationSettingsResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.Notification;
import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.repository.NotificationRepository;
import com.expense.tracking.repository.NotificationSettingsRepository;
import com.expense.tracking.service.EmailNotificationService;
import com.expense.tracking.service.ExpenseService;
import com.expense.tracking.service.NotificationService;
import com.expense.tracking.service.NotificationSettingsService;
import com.expense.tracking.service.PushNotificationService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationDiagnosticTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSettingsService notificationSettingsService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSettingsRepository notificationSettingsRepository;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Value("${email.notifications.enabled:false}")
    private boolean emailNotificationsEnabled;

    @Value("${vapid.public.key:}")
    private String vapidPublicKey;

    @Value("${vapid.private.key:}")
    private String vapidPrivateKey;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        notificationRepository.deleteAll();
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        notificationSettingsRepository.deleteAll();

        // Create a test category
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF0000")
                .description("Test category for notification tests")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void diagnoseNotificationSystemConfiguration() {
        System.out.println("=== NOTIFICATION SYSTEM DIAGNOSTIC ===");
        System.out.println();

        // Check service availability
        System.out.println("1. SERVICE AVAILABILITY:");
        System.out.println("   ✓ NotificationService: Available");
        System.out.println("   ✓ NotificationSettingsService: Available");
        System.out.println("   " + (pushNotificationService != null ? "✓" : "✗") + 
                          " PushNotificationService: " + (pushNotificationService != null ? "Available" : "NOT AVAILABLE"));
        System.out.println("   " + (emailNotificationService != null ? "✓" : "✗") + 
                          " EmailNotificationService: " + (emailNotificationService != null ? "Available" : "NOT AVAILABLE"));
        System.out.println();

        // Check configuration
        System.out.println("2. CONFIGURATION STATUS:");
        System.out.println("   Email Notifications Enabled: " + emailNotificationsEnabled);
        System.out.println("   VAPID Public Key Set: " + (!vapidPublicKey.isEmpty()));
        System.out.println("   VAPID Private Key Set: " + (!vapidPrivateKey.isEmpty()));
        System.out.println();

        // Check notification settings
        System.out.println("3. NOTIFICATION SETTINGS:");
        NotificationSettingsResponse settings = notificationSettingsService.getSettings();
        if (settings != null) {
            System.out.println("   ✓ Default settings found:");
            System.out.println("     - Daily Reminders: " + settings.getEnableDailyReminder());
            System.out.println("     - Weekly Summary: " + settings.getEnableWeeklySummary());
            System.out.println("     - Budget Alerts: " + settings.getEnableBudgetAlerts());
            System.out.println("     - Email Notifications: " + settings.getEnableEmailNotifications());
            System.out.println("     - Budget Warning Threshold: " + settings.getBudgetWarningThreshold() + "%");
            System.out.println("     - Email Address: " + settings.getEmailAddress());
        } else {
            System.out.println("   ✗ No default notification settings found");
            System.out.println("   This is likely why you're not receiving notifications!");
        }
        System.out.println();
    }

    @Test
    void testBudgetAlertTriggerWithSettings() {
        System.out.println("=== TESTING BUDGET ALERT WITH PROPER SETTINGS ===");
        System.out.println();

        // First, create notification settings with email enabled
        NotificationSettingsRequest settingsRequest = NotificationSettingsRequest.builder()
                .enableBudgetAlerts(true)
                .enableEmailNotifications(true)
                .budgetWarningThreshold(80)
                .emailAddress("test@example.com")
                .build();

        NotificationSettingsResponse settings = notificationSettingsService.updateSettings(settingsRequest);
        System.out.println("1. Created notification settings:");
        System.out.println("   - Budget Alerts: " + settings.getEnableBudgetAlerts());
        System.out.println("   - Email Notifications: " + settings.getEnableEmailNotifications());
        System.out.println("   - Warning Threshold: " + settings.getBudgetWarningThreshold() + "%");
        System.out.println("   - Email Address: " + settings.getEmailAddress());
        System.out.println();

        // Create an expense that should trigger a budget alert (85% of 10000 = 8500)
        ExpenseRequest expenseRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("8500.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Large expense to trigger budget alert")
                .build();

        System.out.println("2. Creating expense to trigger budget alert:");
        System.out.println("   - Amount: $8500.00 (85% of $10000 budget)");
        System.out.println("   - This should trigger a budget warning");
        System.out.println();

        ExpenseResponse createdExpense = expenseService.createExpense(expenseRequest);
        
        assertThat(createdExpense).isNotNull();
        System.out.println("3. Expense created successfully:");
        System.out.println("   - ID: " + createdExpense.getId());
        System.out.println("   - Amount: $" + createdExpense.getAmount());
        System.out.println();

        // Check if notifications were created
        List<Notification> notifications = notificationRepository.findAll();
        System.out.println("4. Notifications created: " + notifications.size());
        
        if (notifications.isEmpty()) {
            System.out.println("   ✗ NO NOTIFICATIONS CREATED!");
            System.out.println("   This indicates the budget alert trigger is not working properly.");
            
            // Check budget summary
            ExpenseService.BudgetSummary summary = expenseService.getCurrentMonthBudgetSummary();
            System.out.println("   Budget Summary:");
            System.out.println("     - Total Spent: $" + summary.getTotalSpent());
            System.out.println("     - Monthly Budget: $" + summary.getMonthlyBudget());
            System.out.println("     - Percentage: " + summary.getPercentage() + "%");
            System.out.println("     - Over Budget: " + summary.isOverBudget());
            System.out.println("     - Near Threshold: " + summary.isNearThreshold());
        } else {
            for (Notification notification : notifications) {
                System.out.println("   ✓ Notification created:");
                System.out.println("     - Type: " + notification.getType());
                System.out.println("     - Title: " + notification.getTitle());
                System.out.println("     - Message: " + notification.getMessage());
                System.out.println("     - Channel: " + notification.getChannel());
            }
        }
        System.out.println();
    }

    @Test
    void testEmailNotificationConfiguration() {
        System.out.println("=== TESTING EMAIL NOTIFICATION CONFIGURATION ===");
        System.out.println();

        System.out.println("1. Email Service Status:");
        if (emailNotificationService != null) {
            System.out.println("   ✓ EmailNotificationService is available");
            System.out.println("   ✓ Email notifications are enabled in configuration");
        } else {
            System.out.println("   ✗ EmailNotificationService is NOT available");
            System.out.println("   ✗ Email notifications are disabled in configuration");
            System.out.println("   To fix this, set: email.notifications.enabled=true");
        }
        System.out.println();

        System.out.println("2. Configuration Values:");
        System.out.println("   - email.notifications.enabled: " + emailNotificationsEnabled);
        System.out.println();

        if (emailNotificationService != null) {
            // Create notification settings with email
            NotificationSettingsRequest settingsRequest = NotificationSettingsRequest.builder()
                    .enableEmailNotifications(true)
                    .emailAddress("test@example.com")
                    .enableDailyReminder(true)
                    .build();

            notificationSettingsService.updateSettings(settingsRequest);

            // Test creating a daily reminder
            System.out.println("3. Testing email notification creation:");
            try {
                var response = notificationService.createDailyReminder();
                System.out.println("   ✓ Daily reminder created: " + response.getTitle());
                
                // Check if it has email channel
                List<Notification> notifications = notificationRepository.findAll();
                if (!notifications.isEmpty()) {
                    Notification notification = notifications.get(0);
                    boolean hasEmailChannel = notification.getChannel() == NotificationChannel.EMAIL;
                    System.out.println("   Email channel included: " + hasEmailChannel);
                }
            } catch (Exception e) {
                System.out.println("   ✗ Error creating daily reminder: " + e.getMessage());
            }
        }
        System.out.println();
    }

    @Test
    void testPushNotificationConfiguration() {
        System.out.println("=== TESTING PUSH NOTIFICATION CONFIGURATION ===");
        System.out.println();

        System.out.println("1. Push Service Status:");
        if (pushNotificationService != null) {
            System.out.println("   ✓ PushNotificationService is available");
            System.out.println("   ✓ VAPID keys are configured");
        } else {
            System.out.println("   ✗ PushNotificationService is NOT available");
            System.out.println("   ✗ VAPID keys are not configured");
            System.out.println("   To fix this, set environment variables:");
            System.out.println("     - VAPID_PUBLIC_KEY=<your_public_key>");
            System.out.println("     - VAPID_PRIVATE_KEY=<your_private_key>");
        }
        System.out.println();

        System.out.println("2. VAPID Configuration:");
        System.out.println("   - Public Key Set: " + (!vapidPublicKey.isEmpty()));
        System.out.println("   - Private Key Set: " + (!vapidPrivateKey.isEmpty()));
        if (!vapidPublicKey.isEmpty()) {
            System.out.println("   - Public Key Preview: " + vapidPublicKey.substring(0, Math.min(20, vapidPublicKey.length())) + "...");
        }
        System.out.println();

        if (pushNotificationService != null) {
            System.out.println("3. Testing push notification payload creation:");
            try {
                PushNotificationService.NotificationPayload payload = 
                    new PushNotificationService.NotificationPayload.Builder()
                        .title("Test Push Notification")
                        .body("This is a test push notification")
                        .icon("/icons/icon-192x192.svg")
                        .build();
                
                System.out.println("   ✓ Push payload created successfully");
                System.out.println("     - Title: " + payload.getTitle());
                System.out.println("     - Body: " + payload.getBody());
            } catch (Exception e) {
                System.out.println("   ✗ Error creating push payload: " + e.getMessage());
            }
        }
        System.out.println();
    }

    @Test
    void testNotificationDeliveryFlow() {
        System.out.println("=== TESTING COMPLETE NOTIFICATION DELIVERY FLOW ===");
        System.out.println();

        // Setup notification settings
        NotificationSettingsRequest settingsRequest = NotificationSettingsRequest.builder()
                .enableBudgetAlerts(true)
                .enableEmailNotifications(true)
                .enableDailyReminder(true)
                .enableWeeklySummary(true)
                .budgetWarningThreshold(80)
                .emailAddress("user@example.com")
                .build();

        NotificationSettingsResponse settings = notificationSettingsService.updateSettings(settingsRequest);
        System.out.println("1. Notification settings configured:");
        System.out.println("   - Budget Alerts: " + settings.getEnableBudgetAlerts());
        System.out.println("   - Email: " + settings.getEnableEmailNotifications());
        System.out.println("   - Daily Reminders: " + settings.getEnableDailyReminder());
        System.out.println("   - Weekly Summary: " + settings.getEnableWeeklySummary());
        System.out.println();

        // Test each notification type
        System.out.println("2. Testing notification types:");
        
        // Daily Reminder
        try {
            var dailyReminder = notificationService.createDailyReminder();
            System.out.println("   ✓ Daily Reminder: " + dailyReminder.getTitle());
        } catch (Exception e) {
            System.out.println("   ✗ Daily Reminder failed: " + e.getMessage());
        }

        // Weekly Summary
        try {
            var weeklySummary = notificationService.createSmartWeeklySummary();
            System.out.println("   ✓ Weekly Summary: " + weeklySummary.getTitle());
        } catch (Exception e) {
            System.out.println("   ✗ Weekly Summary failed: " + e.getMessage());
        }

        // Budget Alert
        try {
            var budgetAlert = notificationService.createBudgetAlert(85.0, 8500.0, 10000.0);
            System.out.println("   ✓ Budget Alert: " + budgetAlert.getTitle());
        } catch (Exception e) {
            System.out.println("   ✗ Budget Alert failed: " + e.getMessage());
        }

        // Check total notifications created
        long totalNotifications = notificationRepository.count();
        System.out.println();
        System.out.println("3. Total notifications in database: " + totalNotifications);

        // Analyze notification channels
        List<Notification> allNotifications = notificationRepository.findAll();
        System.out.println("4. Notification channel analysis:");
        for (Notification notification : allNotifications) {
            System.out.println("   - " + notification.getType() + ": " + notification.getChannel());
        }
        System.out.println();

        System.out.println("=== DIAGNOSTIC SUMMARY ===");
        System.out.println("Services Available:");
        System.out.println("  - NotificationService: ✓");
        System.out.println("  - PushNotificationService: " + (pushNotificationService != null ? "✓" : "✗"));
        System.out.println("  - EmailNotificationService: " + (emailNotificationService != null ? "✓" : "✗"));
        System.out.println();
        System.out.println("Configuration Issues:");
        if (pushNotificationService == null) {
            System.out.println("  ✗ Push notifications disabled - missing VAPID keys");
        }
        if (emailNotificationService == null) {
            System.out.println("  ✗ Email notifications disabled - email.notifications.enabled=false");
        }
        if (pushNotificationService != null && emailNotificationService != null) {
            System.out.println("  ✓ All notification services properly configured");
        }
        System.out.println();
    }
}