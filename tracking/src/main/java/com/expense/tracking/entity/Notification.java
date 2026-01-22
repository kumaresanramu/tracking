package com.expense.tracking.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSent = false;
    
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    
    @Column(name = "action_label", length = 100)
    private String actionLabel;
    
    @Column(name = "icon", length = 50)
    private String icon;
    
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 1; // 1 = low, 2 = medium, 3 = high
    
    // Helper methods
    public boolean isOverdue() {
        return scheduledFor != null && LocalDateTime.now().isAfter(scheduledFor) && !isSent;
    }
    
    public boolean isScheduled() {
        return scheduledFor != null && LocalDateTime.now().isBefore(scheduledFor);
    }
    
    public boolean isDue() {
        return scheduledFor != null && LocalDateTime.now().isAfter(scheduledFor.minusMinutes(5));
    }
}