package com.expense.tracking.dto;

import com.expense.tracking.entity.ReminderFrequency;
import jakarta.validation.constraints.*;
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
public class PaymentReminderRequest {
    
    @NotBlank(message = "Reminder name is required")
    @Size(max = 200, message = "Reminder name must not exceed 200 characters")
    private String name;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimal places")
    private BigDecimal amount;
    
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;
    
    @NotNull(message = "Frequency is required")
    private ReminderFrequency frequency;
    
    private Long categoryId;
    
    @Min(value = 1, message = "Days before must be at least 1")
    @Max(value = 30, message = "Days before must not exceed 30")
    @Builder.Default
    private Integer daysBefore = 3;
    
    @Builder.Default
    private LocalTime preferredNotificationTime = LocalTime.of(9, 0);
    
    @Builder.Default
    private Boolean enableEmailNotification = true;
    
    @Builder.Default
    private Boolean enablePushNotification = true;
    
    @Size(max = 500, message = "Custom message must not exceed 500 characters")
    private String customMessage;
    
    @Builder.Default
    private Boolean active = true;
}