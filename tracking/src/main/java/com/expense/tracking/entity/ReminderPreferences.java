package com.expense.tracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "reminder_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id", nullable = false)
    private PaymentReminder reminder;
    
    @Column(name = "days_before")
    private Integer daysBefore; // 1-30 days before due date
    
    @Column(name = "notification_time")
    private LocalTime notificationTime; // User's preferred time
    
    @Column(name = "weekends_only")
    @Builder.Default
    private Boolean weekendsOnly = false; // Only notify on weekends
    
    @Column(name = "weekdays_only")
    @Builder.Default
    private Boolean weekdaysOnly = false; // Only notify on weekdays
    
    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "reminder_specific_days", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> specificDays; // Specific days of week
    
    // Helper method to check if notification should be sent on a specific day
    public boolean shouldNotifyOnDay(DayOfWeek dayOfWeek) {
        if (specificDays != null && !specificDays.isEmpty()) {
            return specificDays.contains(dayOfWeek);
        }
        
        if (weekendsOnly) {
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }
        
        if (weekdaysOnly) {
            return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        }
        
        return true; // Default: notify on any day
    }
}