package com.expense.tracking.service;

import com.expense.tracking.dto.PaymentReminderRequest;
import com.expense.tracking.dto.PaymentReminderResponse;
import com.expense.tracking.dto.CategoryResponse;
import com.expense.tracking.entity.PaymentReminder;
import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.ReminderPreferences;
import com.expense.tracking.repository.PaymentReminderRepository;
import com.expense.tracking.repository.ReminderPreferencesRepository;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderService {
    
    private final PaymentReminderRepository paymentReminderRepository;
    private final ReminderPreferencesRepository reminderPreferencesRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseService expenseService;
    private final ReminderPreferencesService reminderPreferencesService;
    private final NotificationService notificationService;
    
    @Transactional
    public PaymentReminderResponse createReminder(PaymentReminderRequest request) {
        log.info("Creating payment reminder: {}", request.getName());
        
        PaymentReminder reminder = PaymentReminder.builder()
                .name(request.getName())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .frequency(request.getFrequency())
                .daysBefore(request.getDaysBefore())
                .preferredNotificationTime(request.getPreferredNotificationTime())
                .enableEmailNotification(request.getEnableEmailNotification())
                .enablePushNotification(request.getEnablePushNotification())
                .customMessage(request.getCustomMessage())
                .active(request.getActive())
                .build();
        
        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            reminder.setCategory(category);
        }
        
        PaymentReminder savedReminder = paymentReminderRepository.save(reminder);
        log.info("Created payment reminder with id: {}", savedReminder.getId());
        
        return mapToResponse(savedReminder);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentReminderResponse> getAllReminders() {
        log.debug("Fetching all active payment reminders");
        return paymentReminderRepository.findByActiveTrueOrderByDueDateAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PaymentReminderResponse getReminderById(Long id) {
        log.debug("Fetching payment reminder with id: {}", id);
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reminder not found with id: " + id));
        return mapToResponse(reminder);
    }
    
    @Transactional
    public PaymentReminderResponse updateReminder(Long id, PaymentReminderRequest request) {
        log.info("Updating payment reminder with id: {}", id);
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reminder not found with id: " + id));
        
        reminder.setName(request.getName());
        reminder.setAmount(request.getAmount());
        reminder.setDueDate(request.getDueDate());
        reminder.setFrequency(request.getFrequency());
        reminder.setDaysBefore(request.getDaysBefore());
        reminder.setPreferredNotificationTime(request.getPreferredNotificationTime());
        reminder.setEnableEmailNotification(request.getEnableEmailNotification());
        reminder.setEnablePushNotification(request.getEnablePushNotification());
        reminder.setCustomMessage(request.getCustomMessage());
        reminder.setActive(request.getActive());
        
        // Update category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            reminder.setCategory(category);
        } else {
            reminder.setCategory(null);
        }
        
        PaymentReminder updatedReminder = paymentReminderRepository.save(reminder);
        log.info("Updated payment reminder with id: {}", updatedReminder.getId());
        
        return mapToResponse(updatedReminder);
    }
    
    @Transactional
    public void deleteReminder(Long id) {
        log.info("Deleting payment reminder with id: {}", id);
        
        if (!paymentReminderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment reminder not found with id: " + id);
        }
        
        // Delete associated preferences first
        reminderPreferencesRepository.deleteByReminderId(id);
        paymentReminderRepository.deleteById(id);
        
        log.info("Deleted payment reminder with id: {}", id);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentReminderResponse> getUpcomingReminders() {
        log.debug("Fetching upcoming payment reminders");
        return paymentReminderRepository.findDueReminders()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PaymentReminderResponse> getOverdueReminders() {
        log.debug("Fetching overdue payment reminders");
        return paymentReminderRepository.findOverdueReminders()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public PaymentReminderResponse markReminderAsPaid(Long reminderId) {
        log.info("Marking payment reminder as paid: {}", reminderId);
        
        PaymentReminder reminder = paymentReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reminder not found with id: " + reminderId));
        
        // Create expense record
        createExpenseFromReminder(reminder);
        
        // Update last paid date
        reminder.setLastPaid(LocalDate.now());
        PaymentReminder updatedReminder = paymentReminderRepository.save(reminder);
        
        log.info("Marked payment reminder as paid and created expense: {}", reminderId);
        return mapToResponse(updatedReminder);
    }
    
    @Transactional
    public void scheduleCustomNotification(Long reminderId, LocalDateTime notificationTime) {
        log.info("Scheduling custom notification for reminder ID: {} at {}", reminderId, notificationTime);
        
        PaymentReminder reminder = paymentReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reminder not found with id: " + reminderId));
        
        // This is a placeholder for actual notification scheduling
        // In a real application, this would integrate with a job scheduler like Quartz
        // or a message queue system to schedule the notification
        log.info("Custom notification scheduled for reminder: {} at {}", reminder.getName(), notificationTime);
        
        // TODO: Implement actual notification scheduling
        // - Schedule job with Quartz or similar scheduler
        // - Store scheduled notification in database
        // - Handle notification delivery at specified time
    }
    
    @Transactional
    public void updateReminderPreferences(Long reminderId, com.expense.tracking.dto.ReminderPreferencesRequest preferences) {
        log.info("Updating reminder preferences for reminder ID: {}", reminderId);
        
        // Verify reminder exists
        if (!paymentReminderRepository.existsById(reminderId)) {
            throw new ResourceNotFoundException("Payment reminder not found with id: " + reminderId);
        }
        
        // Set the reminder ID in the preferences request
        preferences.setReminderId(reminderId);
        
        // Use the ReminderPreferencesService to create or update preferences
        reminderPreferencesService.createOrUpdatePreferences(preferences);
        
        log.info("Updated reminder preferences for reminder ID: {}", reminderId);
    }
    
    @Scheduled(fixedRate = 3600000) // Check every hour
    @Transactional(readOnly = true)
    public void checkDuePayments() {
        log.debug("Checking for due payment reminders");
        
        List<PaymentReminder> dueReminders = paymentReminderRepository.findDueReminders();
        
        for (PaymentReminder reminder : dueReminders) {
            // Check if we should notify based on preferences
            if (shouldNotifyToday(reminder)) {
                sendNotification(reminder);
            }
        }
        
        log.debug("Processed {} due payment reminders", dueReminders.size());
    }
    
    private void createExpenseFromReminder(PaymentReminder reminder) {
        try {
            // Create expense using ExpenseService
            var expenseRequest = com.expense.tracking.dto.ExpenseRequest.builder()
                    .amount(reminder.getAmount())
                    .date(LocalDate.now())
                    .categoryId(reminder.getCategory() != null ? reminder.getCategory().getId() : null)
                    .description("Payment: " + reminder.getName())
                    .build();
            
            expenseService.createExpense(expenseRequest);
            log.info("Created expense from reminder: {}", reminder.getName());
        } catch (Exception e) {
            log.error("Failed to create expense from reminder: {}", reminder.getName(), e);
            // Don't fail the reminder marking if expense creation fails
        }
    }
    
    private boolean shouldNotifyToday(PaymentReminder reminder) {
        // Check if we have custom preferences
        Optional<ReminderPreferences> preferences = reminderPreferencesRepository.findByReminderId(reminder.getId());
        
        if (preferences.isPresent()) {
            return preferences.get().shouldNotifyOnDay(LocalDate.now().getDayOfWeek());
        }
        
        // Default behavior - notify every day within the notification window
        return true;
    }
    
    private void sendNotification(PaymentReminder reminder) {
        log.info("Sending notification for reminder: {} - Amount: {} - Due: {}", 
                reminder.getName(), reminder.getAmount(), reminder.getNextDueDate());
        
        try {
            // Create notification request
            var notificationRequest = com.expense.tracking.dto.NotificationRequest.builder()
                    .title("Payment Reminder: " + reminder.getName())
                    .message(reminder.getCustomMessage() != null ? 
                            reminder.getCustomMessage() : 
                            String.format("Payment of ₹%.2f is due on %s", 
                                    reminder.getAmount().doubleValue(), 
                                    reminder.getNextDueDate()))
                    .type(com.expense.tracking.entity.NotificationType.PAYMENT_REMINDER)
                    .channel(com.expense.tracking.entity.NotificationChannel.IN_APP)
                    .icon("💰")
                    .actionUrl("/reminders/" + reminder.getId())
                    .actionLabel("View Details")
                    .priority(2)
                    .build();
            
            // Create the notification using NotificationService
            var notification = notificationService.createNotification(notificationRequest);
            
            if (notification != null) {
                log.info("Successfully created notification for payment reminder: {}", reminder.getName());
            }
            
        } catch (Exception e) {
            log.error("Failed to send notification for payment reminder: {}", reminder.getName(), e);
        }
    }
    
    private PaymentReminderResponse mapToResponse(PaymentReminder reminder) {
        CategoryResponse categoryResponse = null;
        if (reminder.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(reminder.getCategory().getId())
                    .name(reminder.getCategory().getName())
                    .parentId(reminder.getCategory().getParent() != null ? 
                             reminder.getCategory().getParent().getId() : null)
                    .build();
        }
        
        LocalDate nextDueDate = reminder.getNextDueDate();
        boolean isDue = reminder.isDue();
        boolean isOverdue = nextDueDate.isBefore(LocalDate.now()) && 
                           (reminder.getLastPaid() == null || reminder.getLastPaid().isBefore(nextDueDate));
        
        return PaymentReminderResponse.builder()
                .id(reminder.getId())
                .name(reminder.getName())
                .amount(reminder.getAmount())
                .dueDate(reminder.getDueDate())
                .frequency(reminder.getFrequency())
                .category(categoryResponse)
                .active(reminder.getActive())
                .lastPaid(reminder.getLastPaid())
                .daysBefore(reminder.getDaysBefore())
                .preferredNotificationTime(reminder.getPreferredNotificationTime())
                .enableEmailNotification(reminder.getEnableEmailNotification())
                .enablePushNotification(reminder.getEnablePushNotification())
                .customMessage(reminder.getCustomMessage())
                .nextDueDate(nextDueDate)
                .isDue(isDue)
                .isOverdue(isOverdue)
                .build();
    }
}