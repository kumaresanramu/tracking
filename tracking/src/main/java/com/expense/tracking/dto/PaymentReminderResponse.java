package com.expense.tracking.dto;

import com.expense.tracking.entity.ReminderFrequency;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReminderResponse {
    
    private Long id;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
    private ReminderFrequency frequency;
    private CategoryResponse category;
    private Boolean active;
    private LocalDate lastPaid;
    
    // Notification preferences
    private Integer daysBefore;
    private LocalTime preferredNotificationTime;
    private Boolean enableEmailNotification;
    private Boolean enablePushNotification;
    private String customMessage;
    
    // Computed fields
    private LocalDate nextDueDate;
    private Boolean isDue;
    private Boolean isOverdue;
}