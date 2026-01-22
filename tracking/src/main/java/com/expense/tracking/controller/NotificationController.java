package com.expense.tracking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expense.tracking.dto.NotificationRequest;
import com.expense.tracking.dto.NotificationResponse;
import com.expense.tracking.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
    
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications() {
        return ResponseEntity.ok(notificationService.getUnreadNotifications());
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications() {
        return ResponseEntity.ok(notificationService.getRecentNotifications());
    }
    
    @GetMapping("/count/unread")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }
    
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.createNotification(request));
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
    
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/process-due")
    public ResponseEntity<List<NotificationResponse>> processDueNotifications() {
        return ResponseEntity.ok(notificationService.processDueNotifications());
    }
    
    // Quick notification creation endpoints
    @PostMapping("/daily-reminder")
    public ResponseEntity<NotificationResponse> createDailyReminder() {
        return ResponseEntity.ok(notificationService.createDailyReminder());
    }
    
    @PostMapping("/budget-alert")
    public ResponseEntity<NotificationResponse> createBudgetAlert(
            @RequestParam double percentage,
            @RequestParam double spent,
            @RequestParam double budget) {
        return ResponseEntity.ok(notificationService.createBudgetAlert(percentage, spent, budget));
    }
    
    @PostMapping("/streak-reward")
    public ResponseEntity<NotificationResponse> createStreakReward(@RequestParam int days) {
        return ResponseEntity.ok(notificationService.createStreakReward(days));
    }
    
    @PostMapping("/weekly-summary")
    public ResponseEntity<NotificationResponse> createWeeklySummary(
            @RequestParam double totalSpent,
            @RequestParam String topCategory) {
        return ResponseEntity.ok(notificationService.createWeeklySummary(totalSpent, topCategory));
    }
    
    // Enhanced notification endpoints with smart data calculation
    @PostMapping("/smart-budget-alert")
    public ResponseEntity<NotificationResponse> createSmartBudgetAlert(@RequestParam double monthlyBudget) {
        return ResponseEntity.ok(notificationService.createSmartBudgetAlert(monthlyBudget));
    }
    
    @PostMapping("/smart-streak-reward")
    public ResponseEntity<NotificationResponse> createSmartStreakReward() {
        return ResponseEntity.ok(notificationService.createSmartStreakReward());
    }
    
    @PostMapping("/smart-weekly-summary")
    public ResponseEntity<NotificationResponse> createSmartWeeklySummary() {
        return ResponseEntity.ok(notificationService.createSmartWeeklySummary());
    }
}