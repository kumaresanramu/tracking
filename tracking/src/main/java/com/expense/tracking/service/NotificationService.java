package com.expense.tracking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.dto.NotificationRequest;
import com.expense.tracking.dto.NotificationResponse;
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
    
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .channel(request.getChannel())
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
        // This is where you would integrate with actual notification services
        log.info("Sending {} notification: {} via {}", 
                notification.getType(), notification.getTitle(), notification.getChannel());
        
        switch (notification.getChannel()) {
            case PUSH -> sendPushNotification(notification);
            case EMAIL -> sendEmailNotification(notification);
            case IN_APP -> log.info("In-app notification ready for display");
        }
    }
    
    private void sendPushNotification(Notification notification) {
        // Integrate with push notification service (Firebase, etc.)
        log.info("Would send push notification: {}", notification.getTitle());
    }
    
    private void sendEmailNotification(Notification notification) {
        // Integrate with email service
        log.info("Would send email notification: {}", notification.getTitle());
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
}