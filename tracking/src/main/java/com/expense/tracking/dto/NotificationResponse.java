package com.expense.tracking.dto;

import java.time.LocalDateTime;

import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.entity.NotificationType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationChannel channel;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledFor;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private Boolean isSent;
    private String actionUrl;
    private String actionLabel;
    private String icon;
    private Integer priority;
    private String timeAgo;
    private Boolean isOverdue;
    private Boolean isScheduled;
    private Boolean isDue;
}