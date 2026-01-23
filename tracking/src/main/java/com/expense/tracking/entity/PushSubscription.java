package com.expense.tracking.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a push notification subscription for a user.
 * Contains the necessary information to send push notifications via the Web Push Protocol.
 */
@Entity
@Table(name = "push_subscriptions", indexes = {
    @Index(name = "idx_push_subscriptions_endpoint", columnList = "endpoint"),
    @Index(name = "idx_push_subscriptions_user_id", columnList = "user_id"),
    @Index(name = "idx_push_subscriptions_active", columnList = "active"),
    @Index(name = "idx_push_subscriptions_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The push service endpoint URL where notifications should be sent.
     * This is unique per subscription and provided by the browser.
     */
    @Column(unique = true, nullable = false, length = 500)
    @NotBlank(message = "Endpoint is required")
    private String endpoint;
    
    /**
     * The P256DH key used for message encryption.
     * Base64-encoded public key for ECDH key agreement.
     */
    @Column(name = "p256dh_key", nullable = false, length = 100)
    @NotBlank(message = "P256DH key is required")
    private String p256dhKey;
    
    /**
     * The authentication secret used for message authentication.
     * Base64-encoded random bytes for HMAC authentication.
     */
    @Column(name = "auth_key", nullable = false, length = 50)
    @NotBlank(message = "Auth key is required")
    private String authKey;
    
    /**
     * User agent string of the browser that created this subscription.
     * Useful for debugging and analytics.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * User ID associated with this subscription.
     * In a real application, this would be a foreign key to a User entity.
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * Whether this subscription is currently active.
     * Inactive subscriptions should not receive notifications.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    /**
     * Timestamp when this subscription was created.
     */
    @Column(name = "created_at", nullable = false)
    @NotNull
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Timestamp when this subscription was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    @NotNull
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * Timestamp when this subscription was last used to send a notification.
     * Useful for cleanup of stale subscriptions.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    /**
     * Number of failed delivery attempts for this subscription.
     * Used to identify and clean up invalid subscriptions.
     */
    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;
    
    /**
     * Timestamp of the last failed delivery attempt.
     */
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;
    
    /**
     * Updates the updatedAt timestamp to the current time.
     * Should be called before saving changes to the entity.
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Marks this subscription as used by updating the lastUsedAt timestamp.
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
        updateTimestamp();
    }
    
    /**
     * Increments the failure count and updates the last failure timestamp.
     */
    public void recordFailure() {
        this.failureCount = (this.failureCount == null ? 0 : this.failureCount) + 1;
        this.lastFailureAt = LocalDateTime.now();
        updateTimestamp();
    }
    
    /**
     * Resets the failure count to zero.
     * Should be called after a successful delivery.
     */
    public void resetFailureCount() {
        this.failureCount = 0;
        this.lastFailureAt = null;
        updateTimestamp();
    }
    
    /**
     * Checks if this subscription should be considered stale.
     * A subscription is stale if it hasn't been used in a long time or has many failures.
     */
    public boolean isStale() {
        // Consider stale if more than 10 failures
        if (failureCount != null && failureCount > 10) {
            return true;
        }
        
        // Consider stale if not used in the last 30 days
        if (lastUsedAt != null && lastUsedAt.isBefore(LocalDateTime.now().minusDays(30))) {
            return true;
        }
        
        // Consider stale if created more than 30 days ago and never used
        if (lastUsedAt == null && createdAt.isBefore(LocalDateTime.now().minusDays(30))) {
            return true;
        }
        
        return false;
    }
}