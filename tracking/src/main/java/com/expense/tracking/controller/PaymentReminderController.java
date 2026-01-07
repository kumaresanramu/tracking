package com.expense.tracking.controller;

import com.expense.tracking.dto.PaymentReminderRequest;
import com.expense.tracking.dto.PaymentReminderResponse;
import com.expense.tracking.service.PaymentReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentReminderController {
    
    private final PaymentReminderService paymentReminderService;
    
    @PostMapping
    public ResponseEntity<PaymentReminderResponse> createReminder(@Valid @RequestBody PaymentReminderRequest request) {
        log.info("Creating payment reminder: {}", request.getName());
        PaymentReminderResponse response = paymentReminderService.createReminder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<PaymentReminderResponse>> getAllReminders() {
        log.debug("Fetching all payment reminders");
        List<PaymentReminderResponse> reminders = paymentReminderService.getAllReminders();
        return ResponseEntity.ok(reminders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentReminderResponse> getReminderById(@PathVariable Long id) {
        log.debug("Fetching payment reminder with id: {}", id);
        PaymentReminderResponse reminder = paymentReminderService.getReminderById(id);
        return ResponseEntity.ok(reminder);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PaymentReminderResponse> updateReminder(
            @PathVariable Long id, 
            @Valid @RequestBody PaymentReminderRequest request) {
        log.info("Updating payment reminder with id: {}", id);
        PaymentReminderResponse response = paymentReminderService.updateReminder(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        log.info("Deleting payment reminder with id: {}", id);
        paymentReminderService.deleteReminder(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<PaymentReminderResponse>> getUpcomingReminders() {
        log.debug("Fetching upcoming payment reminders");
        List<PaymentReminderResponse> reminders = paymentReminderService.getUpcomingReminders();
        return ResponseEntity.ok(reminders);
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<PaymentReminderResponse>> getOverdueReminders() {
        log.debug("Fetching overdue payment reminders");
        List<PaymentReminderResponse> reminders = paymentReminderService.getOverdueReminders();
        return ResponseEntity.ok(reminders);
    }
    
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<PaymentReminderResponse> markReminderAsPaid(@PathVariable Long id) {
        log.info("Marking payment reminder as paid: {}", id);
        PaymentReminderResponse response = paymentReminderService.markReminderAsPaid(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/schedule-notification")
    public ResponseEntity<Void> scheduleCustomNotification(
            @PathVariable Long id, 
            @RequestParam String notificationTime) {
        log.info("Scheduling custom notification for reminder ID: {} at {}", id, notificationTime);
        
        try {
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(notificationTime);
            paymentReminderService.scheduleCustomNotification(id, dateTime);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Invalid notification time format: {}", notificationTime, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/preferences")
    public ResponseEntity<Void> updateReminderPreferences(
            @PathVariable Long id, 
            @Valid @RequestBody com.expense.tracking.dto.ReminderPreferencesRequest preferences) {
        log.info("Updating preferences for reminder ID: {}", id);
        paymentReminderService.updateReminderPreferences(id, preferences);
        return ResponseEntity.ok().build();
    }
}