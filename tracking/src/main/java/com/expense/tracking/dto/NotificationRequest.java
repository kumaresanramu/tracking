package com.expense.tracking.dto;

import java.time.LocalDateTime;

import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;
    
    private LocalDateTime scheduledFor;
    
    @Size(max = 500, message = "Action URL must not exceed 500 characters")
    private String actionUrl;
    
    @Size(max = 100, message = "Action label must not exceed 100 characters")
    private String actionLabel;
    
    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;
    
    @Builder.Default
    private Integer priority = 1;
}