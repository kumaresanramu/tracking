package com.expense.tracking.dto;

import java.time.LocalDateTime;

import com.expense.tracking.entity.PushSubscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for push subscription operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscriptionResponse {
    
    private Long id;
    private String endpoint;
    private String userAgent;
    private Long userId;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUsedAt;
    private Integer failureCount;
    private LocalDateTime lastFailureAt;
    
    /**
     * Creates a response DTO from a PushSubscription entity.
     * Note: Sensitive keys (p256dhKey, authKey) are not included in the response for security.
     */
    public static PushSubscriptionResponse fromEntity(PushSubscription subscription) {
        return PushSubscriptionResponse.builder()
            .id(subscription.getId())
            .endpoint(subscription.getEndpoint())
            .userAgent(subscription.getUserAgent())
            .userId(subscription.getUserId())
            .active(subscription.getActive())
            .createdAt(subscription.getCreatedAt())
            .updatedAt(subscription.getUpdatedAt())
            .lastUsedAt(subscription.getLastUsedAt())
            .failureCount(subscription.getFailureCount())
            .lastFailureAt(subscription.getLastFailureAt())
            .build();
    }
}