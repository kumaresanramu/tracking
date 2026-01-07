package com.expense.tracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "payment_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderFrequency frequency;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "last_paid")
    private LocalDate lastPaid;
    
    // User-configurable notification preferences
    @Column(name = "days_before")
    @Builder.Default
    private Integer daysBefore = 3; // Default: 3 days before due date
    
    @Column(name = "preferred_notification_time")
    @Builder.Default
    private LocalTime preferredNotificationTime = LocalTime.of(9, 0); // Default: 9:00 AM
    
    @Column(name = "enable_email_notification")
    @Builder.Default
    private Boolean enableEmailNotification = true;
    
    @Column(name = "enable_push_notification")
    @Builder.Default
    private Boolean enablePushNotification = true;
    
    @Column(name = "custom_message", length = 500)
    private String customMessage;
    
    // Helper method to calculate next due date based on frequency
    public LocalDate getNextDueDate() {
        if (lastPaid == null) {
            return dueDate;
        }
        
        return switch (frequency) {
            case MONTHLY -> lastPaid.plusMonths(1);
            case QUARTERLY -> lastPaid.plusMonths(3);
            case YEARLY -> lastPaid.plusYears(1);
        };
    }
    
    // Helper method to check if reminder is due
    public boolean isDue() {
        LocalDate nextDue = getNextDueDate();
        LocalDate notificationDate = nextDue.minusDays(daysBefore);
        return LocalDate.now().isEqual(notificationDate) || LocalDate.now().isAfter(notificationDate);
    }
}