package com.expense.tracking.dto;

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
public class ReminderPreferencesResponse {
    
    private Long id;
    private Long reminderId;
    private Integer daysBefore;
    private LocalTime notificationTime;
    private Boolean weekendsOnly;
    private Boolean weekdaysOnly;
    private Set<DayOfWeek> specificDays;
}