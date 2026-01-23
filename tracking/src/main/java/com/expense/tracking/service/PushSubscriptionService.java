package com.expense.tracking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.config.VapidConfig;
import com.expense.tracking.dto.PushSubscriptionRequest;
import com.expense.tracking.entity.PushSubscription;
import com.expense.tracking.exception.ResourceNotFoundException;
import com.expense.tracking.exception.ValidationException;
import com.expense.tracking.repository.PushSubscriptionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing push subscriptions.
 * Handles subscription validation, creation, updates, and cleanup.
 */
@Service
@Slf4j
public class PushSubscriptionService {
    
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final VapidConfig.VapidKeyPair vapidKeyPair;
    
    public PushSubscriptionService(PushSubscriptionRepository pushSubscriptionRepository,
                                 VapidConfig.VapidKeyPair vapidKeyPair) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.vapidKeyPair = vapidKeyPair;
    }
    
    /**
     * Creates or updates a push subscription.
     * 
     * @param request the subscription request
     * @param userAgent the user agent from the HTTP request
     * @return the created or updated subscription
     */
    @Transactional
    public PushSubscription createOrUpdateSubscription(PushSubscriptionRequest request, String userAgent) {
        validateSubscriptionRequest(request);
        
        Optional<PushSubscription> existingSubscription = 
            pushSubscriptionRepository.findByEndpoint(request.getEndpoint());
        
        PushSubscription subscription;
        if (existingSubscription.isPresent()) {
            subscription = updateExistingSubscription(existingSubscription.get(), request, userAgent);
            log.info("Updated existing push subscription for endpoint: {}", request.getEndpoint());
        } else {
            subscription = createNewSubscription(request, userAgent);
            log.info("Created new push subscription for endpoint: {}", request.getEndpoint());
        }
        
        return pushSubscriptionRepository.save(subscription);
    }
    
    /**
     * Deactivates a push subscription by endpoint.
     * 
     * @param endpoint the push service endpoint
     * @return true if a subscription was deactivated, false if not found
     */
    @Transactional
    public boolean deactivateSubscription(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new ValidationException("Endpoint cannot be null or empty");
        }
        
        int updated = pushSubscriptionRepository.deactivateByEndpoint(endpoint);
        if (updated > 0) {
            log.info("Deactivated push subscription for endpoint: {}", endpoint);
            return true;
        } else {
            log.warn("No active subscription found for endpoint: {}", endpoint);
            return false;
        }
    }
    
    /**
     * Deactivates all push subscriptions for a user.
     * 
     * @param userId the user ID
     * @return the number of subscriptions deactivated
     */
    @Transactional
    public int deactivateUserSubscriptions(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        int updated = pushSubscriptionRepository.deactivateAllByUserId(userId);
        log.info("Deactivated {} push subscriptions for user: {}", updated, userId);
        return updated;
    }
    
    /**
     * Gets all active subscriptions for a user.
     * 
     * @param userId the user ID
     * @return list of active subscriptions
     */
    public List<PushSubscription> getUserSubscriptions(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        return pushSubscriptionRepository.findActiveByUserId(userId);
    }
    
    /**
     * Gets all active subscriptions.
     * 
     * @return list of all active subscriptions
     */
    public List<PushSubscription> getAllActiveSubscriptions() {
        return pushSubscriptionRepository.findAllActive();
    }
    
    /**
     * Gets a subscription by ID.
     * 
     * @param subscriptionId the subscription ID
     * @return the subscription
     * @throws ResourceNotFoundException if not found
     */
    public PushSubscription getSubscriptionById(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new ValidationException("Subscription ID cannot be null");
        }
        
        return pushSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Push subscription not found with id: " + subscriptionId));
    }
    
    /**
     * Gets subscription statistics.
     * 
     * @return subscription statistics
     */
    public SubscriptionStatistics getSubscriptionStatistics() {
        Long totalActive = pushSubscriptionRepository.countAllActive();
        
        // Get stale subscriptions count
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<PushSubscription> staleSubscriptions = 
            pushSubscriptionRepository.findStaleSubscriptions(10, thirtyDaysAgo);
        
        return SubscriptionStatistics.builder()
            .totalActiveSubscriptions(totalActive)
            .staleSubscriptionsCount((long) staleSubscriptions.size())
            .build();
    }
    
    /**
     * Cleans up stale subscriptions.
     * This should be called periodically to remove old, unused subscriptions.
     * 
     * @return the number of subscriptions cleaned up
     */
    @Transactional
    public int cleanupStaleSubscriptions() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deleted = pushSubscriptionRepository.deleteStaleSubscriptions(10, thirtyDaysAgo);
        
        if (deleted > 0) {
            log.info("Cleaned up {} stale push subscriptions", deleted);
        }
        
        return deleted;
    }
    
    /**
     * Validates a subscription request.
     */
    private void validateSubscriptionRequest(PushSubscriptionRequest request) {
        if (request == null) {
            throw new ValidationException("Subscription request cannot be null");
        }
        
        if (request.getEndpoint() == null || request.getEndpoint().trim().isEmpty()) {
            throw new ValidationException("Endpoint is required");
        }
        
        if (request.getP256dhKey() == null || request.getP256dhKey().trim().isEmpty()) {
            throw new ValidationException("P256DH key is required");
        }
        
        if (request.getAuthKey() == null || request.getAuthKey().trim().isEmpty()) {
            throw new ValidationException("Auth key is required");
        }
        
        // Validate endpoint URL format
        if (!request.getEndpoint().startsWith("https://")) {
            throw new ValidationException("Endpoint must be a valid HTTPS URL");
        }
        
        // Validate key formats (basic validation)
        try {
            java.util.Base64.getDecoder().decode(request.getP256dhKey());
            java.util.Base64.getDecoder().decode(request.getAuthKey());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Keys must be valid base64-encoded strings");
        }
    }
    
    /**
     * Updates an existing subscription with new data.
     */
    private PushSubscription updateExistingSubscription(PushSubscription existing, 
                                                       PushSubscriptionRequest request, 
                                                       String userAgent) {
        existing.setP256dhKey(request.getP256dhKey());
        existing.setAuthKey(request.getAuthKey());
        existing.setUserAgent(userAgent != null ? userAgent : request.getUserAgent());
        existing.setUserId(request.getUserId());
        existing.setActive(true);
        existing.updateTimestamp();
        existing.resetFailureCount();
        
        return existing;
    }
    
    /**
     * Creates a new subscription from the request.
     */
    private PushSubscription createNewSubscription(PushSubscriptionRequest request, String userAgent) {
        return PushSubscription.builder()
            .endpoint(request.getEndpoint())
            .p256dhKey(request.getP256dhKey())
            .authKey(request.getAuthKey())
            .userAgent(userAgent != null ? userAgent : request.getUserAgent())
            .userId(request.getUserId())
            .active(true)
            .build();
    }
    
    /**
     * Gets the VAPID public key for client-side push subscription.
     */
    public String getVapidPublicKey() {
        return vapidKeyPair.getPublicKey();
    }
    
    /**
     * Data class for subscription statistics.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubscriptionStatistics {
        private Long totalActiveSubscriptions;
        private Long staleSubscriptionsCount;
    }
}