package com.expense.tracking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.tracking.service.NotificationSchedulerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notification-scheduler")
@RequiredArgsConstructor
public class NotificationSchedulerController {
    
    private final NotificationSchedulerService schedulerService;
    
    @PostMapping("/trigger-daily-reminders")
    public ResponseEntity<String> triggerDailyReminders() {
        schedulerService.triggerDailyReminders();
        return ResponseEntity.ok("Daily reminders triggered manually");
    }
    
    @PostMapping("/trigger-weekly-summaries")
    public ResponseEntity<String> triggerWeeklySummaries() {
        schedulerService.triggerWeeklySummaries();
        return ResponseEntity.ok("Weekly summaries triggered manually");
    }
    
    @PostMapping("/check-scheduled")
    public ResponseEntity<String> checkScheduledNotifications() {
        schedulerService.checkScheduledNotifications();
        return ResponseEntity.ok("Scheduled notifications check completed");
    }
}