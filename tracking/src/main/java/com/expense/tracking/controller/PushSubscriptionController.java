package com.expense.tracking.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expense.tracking.dto.BulkNotificationRequest;
import com.expense.tracking.dto.NotificationPayloadDto;
import com.expense.tracking.dto.PushSubscriptionRequest;
import com.expense.tracking.dto.PushSubscriptionResponse;
import com.expense.tracking.entity.PushSubscription;
import com.expense.tracking.exception.ResourceNotFoundException;
import com.expense.tracking.service.PushNotificationService;
import com.expense.tracking.service.PushSubscriptionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing push subscriptions and sending push notifications.
 */
@RestController
@RequestMapping("/api/push-subscriptions")
@Slf4j
public class PushSubscriptionController {
    
    private final PushSubscriptionService pushSubscriptionService;
    private final PushNotificationService pushNotificationService;
    
    public PushSubscriptionController(PushSubscriptionService pushSubscriptionService,
                                    PushNotificationService pushNotificationService) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.pushNotificationService = pushNotificationService;
    }
    
    /**
     * Subscribe to push notifications.
     * Creates a new push subscription or updates an existing one.
     */
    @PostMapping("/subscribe")
    public ResponseEntity<PushSubscriptionResponse> subscribe(
            @Valid @RequestBody PushSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userAgent = request.getUserAgent() != null ? 
                request.getUserAgent() : httpRequest.getHeader("User-Agent");
            
            PushSubscription subscription = pushSubscriptionService.createOrUpdateSubscription(request, userAgent);
            PushSubscriptionResponse response = PushSubscriptionResponse.fromEntity(subscription);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to create/update push subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Unsubscribe from push notifications.
     * Deactivates the subscription for the given endpoint.
     */
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint) {
        try {
            boolean deactivated = pushSubscriptionService.deactivateSubscription(endpoint);
            if (deactivated) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to unsubscribe from push notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all push subscriptions for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PushSubscriptionResponse>> getUserSubscriptions(@PathVariable Long userId) {
        try {
            List<PushSubscription> subscriptions = pushSubscriptionService.getUserSubscriptions(userId);
            List<PushSubscriptionResponse> responses = subscriptions.stream()
                .map(PushSubscriptionResponse::fromEntity)
                .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Failed to get user subscriptions for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all active push subscriptions (admin endpoint).
     */
    @GetMapping("/all")
    public ResponseEntity<List<PushSubscriptionResponse>> getAllActiveSubscriptions() {
        try {
            List<PushSubscription> subscriptions = pushSubscriptionService.getAllActiveSubscriptions();
            List<PushSubscriptionResponse> responses = subscriptions.stream()
                .map(PushSubscriptionResponse::fromEntity)
                .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Failed to get all active subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get subscription statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<SubscriptionStats> getSubscriptionStats() {
        try {
            PushSubscriptionService.SubscriptionStatistics stats = pushSubscriptionService.getSubscriptionStatistics();
            
            SubscriptionStats response = SubscriptionStats.builder()
                .totalActiveSubscriptions(stats.getTotalActiveSubscriptions())
                .staleSubscriptionsCount(stats.getStaleSubscriptionsCount())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get subscription statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Send a test notification to a specific subscription.
     */
    @PostMapping("/{subscriptionId}/test")
    public ResponseEntity<NotificationResult> sendTestNotification(@PathVariable Long subscriptionId) {
        try {
            PushSubscription subscription = pushSubscriptionService.getSubscriptionById(subscriptionId);
            
            if (!subscription.getActive()) {
                return ResponseEntity.badRequest()
                    .body(NotificationResult.builder()
                        .success(false)
                        .message("Subscription is not active")
                        .build());
            }
            
            // Create test notification payload
            PushNotificationService.NotificationPayload payload = 
                new PushNotificationService.NotificationPayload.Builder()
                    .title("Test Notification")
                    .body("This is a test notification from your expense tracker")
                    .icon("/icons/icon-192x192.svg")
                    .tag("test")
                    .build();
            
            CompletableFuture<Boolean> result = pushNotificationService.sendPushNotification(subscription, payload);
            Boolean success = result.get(); // Wait for completion
            
            NotificationResult response = NotificationResult.builder()
                .success(success)
                .message(success ? "Test notification sent successfully" : "Failed to send test notification")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to send test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(NotificationResult.builder()
                    .success(false)
                    .message("Internal server error: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Send bulk notifications.
     */
    @PostMapping("/notify")
    public ResponseEntity<BulkNotificationResult> sendBulkNotifications(
            @Valid @RequestBody BulkNotificationRequest request) {
        
        try {
            PushNotificationService.NotificationPayload payload = request.getPayload().toServicePayload();
            CompletableFuture<Integer> resultFuture;
            
            if (request.getEndpoints() != null && !request.getEndpoints().isEmpty()) {
                // Send to specific endpoints
                List<PushSubscription> subscriptions = request.getEndpoints().stream()
                    .map(endpoint -> {
                        try {
                            return pushSubscriptionService.getAllActiveSubscriptions().stream()
                                .filter(sub -> sub.getEndpoint().equals(endpoint))
                                .findFirst();
                        } catch (Exception e) {
                            log.warn("Error finding subscription for endpoint: {}", endpoint, e);
                            return Optional.<PushSubscription>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(sub -> !request.getActiveOnly() || sub.getActive())
                    .toList();
                
                resultFuture = pushNotificationService.sendBulkNotifications(subscriptions, payload);
                
            } else if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                // Send to specific users
                List<PushSubscription> subscriptions = request.getUserIds().stream()
                    .flatMap(userId -> pushSubscriptionService.getUserSubscriptions(userId).stream())
                    .toList();
                
                resultFuture = pushNotificationService.sendBulkNotifications(subscriptions, payload);
                
            } else {
                // Send to all active subscriptions
                resultFuture = pushNotificationService.sendToAllActiveSubscriptions(payload);
            }
            
            Integer successCount = resultFuture.get(); // Wait for completion
            
            BulkNotificationResult response = BulkNotificationResult.builder()
                .successCount(successCount)
                .message(String.format("Sent %d notifications successfully", successCount))
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to send bulk notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BulkNotificationResult.builder()
                    .successCount(0)
                    .message("Failed to send notifications: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Send a simple notification to all active subscriptions.
     */
    @PostMapping("/notify/simple")
    public ResponseEntity<BulkNotificationResult> sendSimpleNotification(
            @Valid @RequestBody NotificationPayloadDto payload) {
        
        try {
            PushNotificationService.NotificationPayload servicePayload = payload.toServicePayload();
            CompletableFuture<Integer> resultFuture = 
                pushNotificationService.sendToAllActiveSubscriptions(servicePayload);
            
            Integer successCount = resultFuture.get(); // Wait for completion
            
            BulkNotificationResult response = BulkNotificationResult.builder()
                .successCount(successCount)
                .message(String.format("Sent %d notifications successfully", successCount))
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to send simple notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BulkNotificationResult.builder()
                    .successCount(0)
                    .message("Failed to send notification: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Response DTO for subscription statistics.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubscriptionStats {
        private Long totalActiveSubscriptions;
        private Long staleSubscriptionsCount;
    }
    
    /**
     * Response DTO for notification results.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationResult {
        private Boolean success;
        private String message;
    }
    
    /**
     * Response DTO for bulk notification results.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkNotificationResult {
        private Integer successCount;
        private String message;
    }
}