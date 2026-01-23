package com.expense.tracking.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending bulk push notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkNotificationRequest {
    
    /**
     * The notification payload to send.
     */
    @NotNull(message = "Notification payload is required")
    @Valid
    private NotificationPayloadDto payload;
    
    /**
     * Optional list of specific user IDs to send to.
     * If null or empty, sends to all active subscriptions.
     */
    private List<Long> userIds;
    
    /**
     * Optional list of specific subscription endpoints to send to.
     * If provided, takes precedence over userIds.
     */
    private List<String> endpoints;
    
    /**
     * Whether to send only to active subscriptions.
     * Defaults to true.
     */
    @Builder.Default
    private Boolean activeOnly = true;
}