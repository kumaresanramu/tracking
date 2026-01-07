package com.expense.tracking.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderPreferencesRequest {
    
    @NotNull(message = "Reminder ID is required")
    private Long reminderId;
    
    @Min(value = 1, message = "Days before must be at least 1")
    @Max(value = 30, message = "Days before must not exceed 30")
    private Integer daysBefore;
    
    private LocalTime notificationTime;
    
    @Builder.Default
    private Boolean weekendsOnly = false;
    
    @Builder.Default
    private Boolean weekdaysOnly = false;
    
    private Set<DayOfWeek> specificDays;
}