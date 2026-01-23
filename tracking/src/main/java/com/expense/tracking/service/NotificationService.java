package com.expense.tracking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.EmailTemplateData;
import com.expense.tracking.dto.NotificationRequest;
import com.expense.tracking.dto.NotificationResponse;
import com.expense.tracking.dto.NotificationSettingsResponse;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.entity.Notification;
import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.entity.NotificationType;
import com.expense.tracking.repository.ExpenseRepository;
import com.expense.tracking.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationPermissionService notificationPermissionService;
    
    @Autowired(required = false)
    private PushNotificationService pushNotificationService;
    
    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;
    
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        // Get user notification settings to determine channels
        NotificationSettingsResponse settingsResponse = notificationSettingsService.getSettings();
        
        // Check if the notification type is enabled
        if (!isNotificationTypeEnabled(request.getType(), settingsResponse)) {
            log.info("Notification type {} is disabled in user settings", request.getType());
            return null;
        }
        
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .channel(request.getChannel()) // This will be overridden by user preferences during sending
                .scheduledFor(request.getScheduledFor())
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .icon(request.getIcon())
                .priority(request.getPriority())
                .build();
        
        notification = notificationRepository.save(notification);
        log.info("Created notification: {} for type: {}", notification.getId(), notification.getType());
        
        return mapToResponse(notification);
    }
    
    private boolean isNotificationTypeEnabled(NotificationType type, NotificationSettingsResponse settingsResponse) {
        return switch (type) {
            case DAILY_EXPENSE_REMINDER -> settingsResponse.getEnableDailyReminder();
            case BUDGET_THRESHOLD_WARNING, BUDGET_EXCEEDED_ALERT -> settingsResponse.getEnableBudgetAlerts();
            case WEEKLY_SUMMARY -> settingsResponse.getEnableWeeklySummary();
            case STREAK_REWARD -> settingsResponse.getEnableStreakRewards();
            default -> true; // Enable other types by default
        };
    }
    
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<NotificationResponse> getUnreadNotifications() {
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<NotificationResponse> getRecentNotifications() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        return notificationRepository.findRecentNotifications(thirtyDaysAgo)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }
    
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        
        return mapToResponse(notification);
    }
    
    @Transactional
    public void markAllAsRead() {
        List<Notification> unreadNotifications = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }
    
    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
    
    // Scheduled notification processing
    @Transactional
    public List<NotificationResponse> processDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> dueNotifications = notificationRepository.findDueNotifications(now);
        
        for (Notification notification : dueNotifications) {
            // Mark as sent and set sent timestamp
            notification.setIsSent(true);
            notification.setSentAt(now);
            
            // Here you would integrate with actual notification services
            // (Push notification service, Email service, etc.)
            sendNotification(notification);
        }
        
        notificationRepository.saveAll(dueNotifications);
        
        return dueNotifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    // Quick notification creation methods
    public NotificationResponse createDailyReminder() {
        NotificationRequest request = NotificationRequest.builder()
                .title("Daily Expense Reminder")
                .message("Don't forget to log today's expenses! 📝")
                .type(NotificationType.DAILY_EXPENSE_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .icon("📝")
                .actionUrl("/expenses")
                .actionLabel("Log Expense")
                .priority(2)
                .build();
        
        return createNotification(request);
    }
    
    public NotificationResponse createBudgetAlert(double percentage, double spent, double budget) {
        String title = percentage >= 100 ? "Budget Exceeded!" : "Budget Warning";
        String message = String.format("You've spent %.0f%% of your budget.", percentage);
        
        NotificationRequest request = NotificationRequest.builder()
                .title(title)
                .message(message)
                .type(percentage >= 100 ? NotificationType.BUDGET_EXCEEDED_ALERT : NotificationType.BUDGET_THRESHOLD_WARNING)
                .channel(NotificationChannel.IN_APP)
                .icon(percentage >= 100 ? "🚨" : "⚠️")
                .actionUrl("/dashboard")
                .actionLabel("View Budget Overview")
                .priority(3)
                .build();
        
        return createNotification(request);
    }
    
    public NotificationResponse createStreakReward(int days) {
        NotificationRequest request = NotificationRequest.builder()
                .title("Streak Reward! 🎉")
                .message(String.format("You've logged expenses %d days in a row!", days))
                .type(NotificationType.STREAK_REWARD)
                .channel(NotificationChannel.IN_APP)
                .icon("🎉")
                .actionUrl("/dashboard")
                .actionLabel("View")
                .priority(1)
                .build();
        
        return createNotification(request);
    }
    
    public NotificationResponse createWeeklySummary(double totalSpent, String topCategory) {
        NotificationRequest request = NotificationRequest.builder()
                .title("Weekly Summary")
                .message(String.format("This week you spent ₹%.0f. Top category: %s.", totalSpent, topCategory))
                .type(NotificationType.WEEKLY_SUMMARY)
                .channel(NotificationChannel.IN_APP)
                .icon("📊")
                .actionUrl("/analytics")
                .actionLabel("View Report")
                .priority(2)
                .build();
        
        return createNotification(request);
    }
    
    private void sendNotification(Notification notification) {
        // Get effective channels based on user preferences and permissions
        Set<NotificationChannel> effectiveChannels = notificationPermissionService.getEffectiveChannels();
        
        if (effectiveChannels.isEmpty()) {
            log.info("No effective notification channels available for notification: {}", notification.getTitle());
            return;
        }
        
        log.info("Sending {} notification: {} via effective channels: {}", 
                notification.getType(), notification.getTitle(), effectiveChannels);
        
        // Send via each effective channel
        for (NotificationChannel channel : effectiveChannels) {
            if (notificationPermissionService.canSendNotification(notification.getType().name(), channel)) {
                switch (channel) {
                    case PUSH -> sendPushNotification(notification);
                    case EMAIL -> sendEmailNotification(notification);
                    case IN_APP -> log.info("In-app notification ready for display");
                }
            } else {
                log.debug("Cannot send notification via channel {} due to permission restrictions", channel);
            }
        }
    }
    
    private boolean isUrgentNotification(NotificationType type) {
        return type == NotificationType.BUDGET_EXCEEDED_ALERT || 
               type == NotificationType.BUDGET_THRESHOLD_WARNING;
    }
    
    private void sendPushNotification(Notification notification) {
        if (pushNotificationService == null) {
            log.warn("Push notification service not available");
            return;
        }
        
        try {
            PushNotificationService.NotificationPayload payload = createPushPayload(notification);
            CompletableFuture<Integer> future = pushNotificationService.sendToAllActiveSubscriptions(payload);
            
            future.whenComplete((count, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send push notification: {}", notification.getTitle(), throwable);
                } else {
                    log.info("Push notification sent to {} subscribers: {}", count, notification.getTitle());
                }
            });
        } catch (Exception e) {
            log.error("Error creating push notification payload", e);
        }
    }
    
    private void sendEmailNotification(Notification notification) {
        if (emailNotificationService == null) {
            log.warn("Email notification service not available");
            return;
        }
        
        NotificationSettingsResponse settingsResponse = notificationSettingsService.getSettings();
        
        if (!settingsResponse.getEnableEmailNotifications() || settingsResponse.getEmailAddress() == null) {
            log.debug("Email notifications disabled or no email address configured");
            return;
        }
        
        try {
            CompletableFuture<Void> future = sendEmailByType(notification, settingsResponse);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send email notification: {}", notification.getTitle(), throwable);
                } else {
                    log.info("Email notification sent successfully: {}", notification.getTitle());
                }
            });
        } catch (Exception e) {
            log.error("Error sending email notification", e);
        }
    }
    
    private PushNotificationService.NotificationPayload createPushPayload(Notification notification) {
        PushNotificationService.NotificationPayload.Builder builder = new PushNotificationService.NotificationPayload.Builder()
                .title(notification.getTitle())
                .body(notification.getMessage())
                .icon("/icons/icon-192x192.svg")
                .badge("/icons/icon.svg")
                .tag(notification.getType().toString().toLowerCase())
                .requireInteraction(isUrgentNotification(notification.getType()));
        
        // Add action buttons for specific notification types
        if (notification.getActionUrl() != null && notification.getActionLabel() != null) {
            PushNotificationService.NotificationAction[] actions = {
                new PushNotificationService.NotificationAction("open", notification.getActionLabel(), "📱"),
                new PushNotificationService.NotificationAction("dismiss", "Dismiss", "❌")
            };
            builder.actions(actions);
        }
        
        // Add custom data
        Map<String, Object> data = Map.of(
            "notificationId", notification.getId(),
            "type", notification.getType().toString(),
            "actionUrl", notification.getActionUrl() != null ? notification.getActionUrl() : "/dashboard",
            "timestamp", notification.getCreatedAt().toString()
        );
        builder.data(data);
        
        return builder.build();
    }
    
    private CompletableFuture<Void> sendEmailByType(Notification notification, NotificationSettingsResponse settingsResponse) {
        switch (notification.getType()) {
            case DAILY_EXPENSE_REMINDER -> {
                EmailTemplateData.DailyReminderData data = createDailyReminderData(settingsResponse);
                return emailNotificationService.sendDailyReminder(settingsResponse.getEmailAddress(), data);
            }
            case WEEKLY_SUMMARY -> {
                EmailTemplateData.WeeklySummaryData data = createWeeklySummaryData(settingsResponse);
                return emailNotificationService.sendWeeklySummary(settingsResponse.getEmailAddress(), data);
            }
            case BUDGET_THRESHOLD_WARNING, BUDGET_EXCEEDED_ALERT -> {
                EmailTemplateData.BudgetAlertData data = createBudgetAlertData(notification, settingsResponse);
                return emailNotificationService.sendBudgetAlert(settingsResponse.getEmailAddress(), data);
            }
            default -> {
                // For other notification types, send as custom reminder
                return emailNotificationService.sendCustomReminder(
                    settingsResponse.getEmailAddress(), 
                    notification.getTitle(), 
                    notification.getMessage()
                );
            }
        }
    }
    
    private EmailTemplateData.DailyReminderData createDailyReminderData(NotificationSettingsResponse settingsResponse) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        // Calculate current month expenses
        List<Expense> monthlyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(startOfMonth, today);
        double currentSpending = monthlyExpenses.stream()
                .mapToDouble(expense -> expense.getAmount().doubleValue())
                .sum();
        
        // Calculate streak days
        long streakDays = monthlyExpenses.stream()
                .map(Expense::getDate)
                .distinct()
                .count();
        
        // Find top category
        String topCategory = monthlyExpenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                    expense -> expense.getCategory().getName(),
                    Collectors.summingDouble(expense -> expense.getAmount().doubleValue())
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No expenses yet");
        
        return EmailTemplateData.DailyReminderData.builder()
                .userName("User") // TODO: Get actual user name when user management is implemented
                .date(today)
                .streakDays((int) streakDays)
                .monthlyBudget(10000.0) // TODO: Get from user budget settings
                .currentSpending(currentSpending)
                .topCategory(topCategory)
                .build();
    }
    
    private EmailTemplateData.WeeklySummaryData createWeeklySummaryData(NotificationSettingsResponse settingsResponse) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        
        List<Expense> weeklyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(weekStart, today);
        
        double totalSpent = weeklyExpenses.stream()
                .mapToDouble(expense -> expense.getAmount().doubleValue())
                .sum();
        
        Map<String, Double> categoryBreakdown = weeklyExpenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                    expense -> expense.getCategory().getName(),
                    Collectors.summingDouble(expense -> expense.getAmount().doubleValue())
                ));
        
        List<Expense> topExpenses = weeklyExpenses.stream()
                .sorted((e1, e2) -> e2.getAmount().compareTo(e1.getAmount()))
                .limit(5)
                .collect(Collectors.toList());
        
        return EmailTemplateData.WeeklySummaryData.builder()
                .userName("User") // TODO: Get actual user name
                .weekStart(weekStart)
                .weekEnd(today)
                .totalSpent(totalSpent)
                .categoryBreakdown(categoryBreakdown)
                .topExpenses(topExpenses)
                .chartImageUrl(null) // TODO: Generate chart image URL
                .build();
    }
    
    private EmailTemplateData.BudgetAlertData createBudgetAlertData(Notification notification, NotificationSettingsResponse settingsResponse) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        List<Expense> monthlyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(startOfMonth, today);
        double currentSpending = monthlyExpenses.stream()
                .mapToDouble(expense -> expense.getAmount().doubleValue())
                .sum();
        
        double budgetAmount = 10000.0; // TODO: Get from user budget settings
        double percentage = budgetAmount > 0 ? (currentSpending / budgetAmount) * 100 : 0;
        
        List<String> topCategories = monthlyExpenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                    expense -> expense.getCategory().getName(),
                    Collectors.summingDouble(expense -> expense.getAmount().doubleValue())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        String alertType = notification.getType() == NotificationType.BUDGET_EXCEEDED_ALERT ? "exceeded" : "warning";
        
        return EmailTemplateData.BudgetAlertData.builder()
                .userName("User") // TODO: Get actual user name
                .budgetAmount(budgetAmount)
                .currentSpending(currentSpending)
                .percentage(percentage)
                .alertType(alertType)
                .topCategories(topCategories)
                .build();
    }
    
    /**
     * Get notification settings for budget threshold checking
     */
    public NotificationSettingsResponse getNotificationSettings() {
        return notificationSettingsService.getSettings();
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .channel(notification.getChannel())
                .createdAt(notification.getCreatedAt())
                .scheduledFor(notification.getScheduledFor())
                .sentAt(notification.getSentAt())
                .isRead(notification.getIsRead())
                .isSent(notification.getIsSent())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .icon(notification.getIcon())
                .priority(notification.getPriority())
                .timeAgo(formatTimeAgo(notification.getCreatedAt()))
                .isOverdue(notification.isOverdue())
                .isScheduled(notification.isScheduled())
                .isDue(notification.isDue())
                .build();
    }
    
    private String formatTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + " hours ago";
        
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + " days ago";
        
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    // Enhanced notification creation methods with real data calculation
    public NotificationResponse createSmartBudgetAlert(double monthlyBudget) {
        // Calculate current month expenses
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        List<Expense> monthlyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(startOfMonth, endOfMonth);
        double totalSpent = monthlyExpenses.stream()
                .mapToDouble(expense -> expense.getAmount().doubleValue())
                .sum();
        
        double percentage = monthlyBudget > 0 ? (totalSpent / monthlyBudget) * 100 : 0;
        
        String title = percentage >= 100 ? "Budget Exceeded!" : "Budget Warning";
        String message = String.format("You've spent %.0f%% of your budget.", percentage);
        
        NotificationRequest request = NotificationRequest.builder()
                .title(title)
                .message(message)
                .type(percentage >= 100 ? NotificationType.BUDGET_EXCEEDED_ALERT : NotificationType.BUDGET_THRESHOLD_WARNING)
                .channel(NotificationChannel.IN_APP)
                .icon(percentage >= 100 ? "🚨" : "⚠️")
                .actionUrl("/dashboard")
                .actionLabel("View Budget Overview")
                .priority(3)
                .build();
        
        return createNotification(request);
    }
    
    public NotificationResponse createSmartStreakReward() {
        // Calculate streak days for current month
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        
        List<Expense> monthlyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(startOfMonth, now);
        
        // Count unique days with expenses
        long uniqueDays = monthlyExpenses.stream()
                .map(Expense::getDate)
                .distinct()
                .count();
        
        NotificationRequest request = NotificationRequest.builder()
                .title("Streak Reward! 🎉")
                .message(String.format("You've logged expenses %d days in a row!", uniqueDays))
                .type(NotificationType.STREAK_REWARD)
                .channel(NotificationChannel.IN_APP)
                .icon("🎉")
                .actionUrl("/dashboard")
                .actionLabel("View")
                .priority(1)
                .build();
        
        return createNotification(request);
    }
    
    public NotificationResponse createSmartWeeklySummary() {
        // Calculate last 7 days expenses
        LocalDate now = LocalDate.now();
        LocalDate weekAgo = now.minusDays(7);
        
        List<Expense> weeklyExpenses = expenseRepository.findByDateBetweenOrderByDateDesc(weekAgo, now);
        
        double totalSpent = weeklyExpenses.stream()
                .mapToDouble(expense -> expense.getAmount().doubleValue())
                .sum();
        
        // Find top category
        Map<String, Double> categoryTotals = weeklyExpenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                    expense -> expense.getCategory().getName(),
                    Collectors.summingDouble(expense -> expense.getAmount().doubleValue())
                ));
        
        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No expenses");
        
        NotificationRequest request = NotificationRequest.builder()
                .title("Weekly Summary")
                .message(String.format("This week you spent ₹%.0f. Top category: %s.", totalSpent, topCategory))
                .type(NotificationType.WEEKLY_SUMMARY)
                .channel(NotificationChannel.IN_APP)
                .icon("📊")
                .actionUrl("/analytics")
                .actionLabel("View Report")
                .priority(2)
                .build();
        
        return createNotification(request);
    }
    
    /**
     * Send a test email notification.
     */
    public void sendTestEmail(String email, String type) {
        try {
            EmailTemplateData.DailyReminderData testData = EmailTemplateData.DailyReminderData.builder()
                    .userName("Test User")
                    .date(LocalDate.now())
                    .streakDays(5)
                    .monthlyBudget(10000.0)
                    .currentSpending(3500.0)
                    .topCategory("Food")
                    .build();
            
            emailNotificationService.sendDailyReminder(email, testData);
            log.info("Test email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", email, e);
            throw new RuntimeException("Failed to send test email", e);
        }
    }
}