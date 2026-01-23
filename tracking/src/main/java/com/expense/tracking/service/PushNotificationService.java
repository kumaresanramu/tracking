package com.expense.tracking.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.expense.tracking.config.VapidConfig;
import com.expense.tracking.entity.PushSubscription;
import com.expense.tracking.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending push notifications using the Web Push Protocol with VAPID authentication.
 * Handles JWT token generation, HTTP client communication with browser push services,
 * and notification payload creation.
 */
@Service
@Slf4j
public class PushNotificationService {
    
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final VapidConfig.VapidKeyPair vapidKeyPair;
    
    public PushNotificationService(PushSubscriptionRepository pushSubscriptionRepository, 
                                 VapidConfig.VapidKeyPair vapidKeyPair) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.vapidKeyPair = vapidKeyPair;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(5);
        
        if (!vapidKeyPair.isValid()) {
            throw new IllegalStateException("VAPID configuration is invalid");
        }
    }
    
    /**
     * Sends a push notification to a specific subscription.
     * 
     * @param subscription the push subscription to send to
     * @param payload the notification payload
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> sendPushNotification(PushSubscription subscription, NotificationPayload payload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                return sendNotificationInternal(subscription, jsonPayload);
            } catch (Exception e) {
                log.error("Failed to send push notification to endpoint: {}", subscription.getEndpoint(), e);
                handleNotificationFailure(subscription);
                return false;
            }
        }, executorService);
    }
    
    /**
     * Sends push notifications to multiple subscriptions in bulk.
     * 
     * @param subscriptions list of push subscriptions
     * @param payload the notification payload
     * @return CompletableFuture that completes when all notifications are processed
     */
    public CompletableFuture<Integer> sendBulkNotifications(List<PushSubscription> subscriptions, NotificationPayload payload) {
        List<CompletableFuture<Boolean>> futures = subscriptions.stream()
            .map(subscription -> sendPushNotification(subscription, payload))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                int successCount = 0;
                for (CompletableFuture<Boolean> future : futures) {
                    try {
                        if (future.get()) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.warn("Error getting bulk notification result", e);
                    }
                }
                log.info("Bulk notification completed: {}/{} successful", successCount, subscriptions.size());
                return successCount;
            });
    }
    
    /**
     * Sends notifications to all active subscriptions.
     * 
     * @param payload the notification payload
     * @return CompletableFuture with the number of successful sends
     */
    public CompletableFuture<Integer> sendToAllActiveSubscriptions(NotificationPayload payload) {
        List<PushSubscription> activeSubscriptions = pushSubscriptionRepository.findAllActive();
        log.info("Sending notification to {} active subscriptions", activeSubscriptions.size());
        return sendBulkNotifications(activeSubscriptions, payload);
    }
    
    /**
     * Sends notifications to all active subscriptions for a specific user.
     * 
     * @param userId the user ID
     * @param payload the notification payload
     * @return CompletableFuture with the number of successful sends
     */
    public CompletableFuture<Integer> sendToUserSubscriptions(Long userId, NotificationPayload payload) {
        List<PushSubscription> userSubscriptions = pushSubscriptionRepository.findActiveByUserId(userId);
        log.info("Sending notification to {} subscriptions for user {}", userSubscriptions.size(), userId);
        return sendBulkNotifications(userSubscriptions, payload);
    }
    
    /**
     * Internal method to send a notification to a single subscription.
     */
    private boolean sendNotificationInternal(PushSubscription subscription, String jsonPayload) throws Exception {
        String vapidToken = generateVapidToken(subscription.getEndpoint());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(subscription.getEndpoint()))
            .header("Authorization", "vapid t=" + vapidToken + ", k=" + vapidKeyPair.getPublicKey())
            .header("Content-Type", "application/json")
            .header("Content-Encoding", "aes128gcm")
            .header("TTL", "86400") // 24 hours
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            handleNotificationSuccess(subscription);
            log.debug("Push notification sent successfully to endpoint: {}", subscription.getEndpoint());
            return true;
        } else if (response.statusCode() == 410 || response.statusCode() == 404) {
            // Subscription is no longer valid
            log.warn("Push subscription is no longer valid (HTTP {}): {}", response.statusCode(), subscription.getEndpoint());
            deactivateSubscription(subscription);
            return false;
        } else {
            log.warn("Push notification failed with HTTP {}: {}", response.statusCode(), response.body());
            handleNotificationFailure(subscription);
            return false;
        }
    }
    
    /**
     * Generates a VAPID JWT token for authentication.
     * 
     * @param audience the push service endpoint (audience for the JWT)
     * @return the JWT token string
     */
    private String generateVapidToken(String audience) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        String vapidPrivateKey = vapidKeyPair.getPrivateKey();
        if (vapidPrivateKey == null || vapidPrivateKey.isEmpty()) {
            throw new IllegalStateException("VAPID private key is not configured");
        }
        
        // Decode the base64url-encoded private key
        byte[] privateKeyBytes = Base64.getUrlDecoder().decode(vapidPrivateKey);
        
        // Create the private key object
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        
        // Create the JWT algorithm
        Algorithm algorithm = Algorithm.ECDSA256(null, (java.security.interfaces.ECPrivateKey) privateKey);
        
        // Extract the origin from the endpoint URL for the audience
        String aud = extractOrigin(audience);
        
        // Generate the JWT token
        return JWT.create()
            .withAudience(aud)
            .withSubject(vapidKeyPair.getSubject())
            .withExpiresAt(Date.from(Instant.now().plus(Duration.ofHours(12))))
            .withIssuedAt(Date.from(Instant.now()))
            .sign(algorithm);
    }
    
    /**
     * Extracts the origin (protocol + host + port) from a URL.
     */
    private String extractOrigin(String url) {
        try {
            URI uri = URI.create(url);
            String origin = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                origin += ":" + uri.getPort();
            }
            return origin;
        } catch (Exception e) {
            log.warn("Failed to extract origin from URL: {}", url, e);
            return url; // Fallback to the full URL
        }
    }
    
    /**
     * Handles successful notification delivery.
     */
    private void handleNotificationSuccess(PushSubscription subscription) {
        try {
            pushSubscriptionRepository.updateLastUsed(subscription.getEndpoint());
            pushSubscriptionRepository.resetFailureCount(subscription.getEndpoint());
        } catch (Exception e) {
            log.warn("Failed to update subscription success status", e);
        }
    }
    
    /**
     * Handles failed notification delivery.
     */
    private void handleNotificationFailure(PushSubscription subscription) {
        try {
            pushSubscriptionRepository.incrementFailureCount(subscription.getEndpoint());
            
            // Deactivate subscription if it has too many failures
            if (subscription.getFailureCount() != null && subscription.getFailureCount() > 10) {
                deactivateSubscription(subscription);
            }
        } catch (Exception e) {
            log.warn("Failed to update subscription failure status", e);
        }
    }
    
    /**
     * Deactivates a push subscription.
     */
    private void deactivateSubscription(PushSubscription subscription) {
        try {
            pushSubscriptionRepository.deactivateByEndpoint(subscription.getEndpoint());
            log.info("Deactivated push subscription: {}", subscription.getEndpoint());
        } catch (Exception e) {
            log.warn("Failed to deactivate subscription", e);
        }
    }
    
    /**
     * Data class representing a push notification payload.
     */
    public static class NotificationPayload {
        private String title;
        private String body;
        private String icon;
        private String badge;
        private String tag;
        private boolean requireInteraction;
        private Object data;
        private NotificationAction[] actions;
        
        public NotificationPayload() {}
        
        public NotificationPayload(String title, String body) {
            this.title = title;
            this.body = body;
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public boolean isRequireInteraction() { return requireInteraction; }
        public void setRequireInteraction(boolean requireInteraction) { this.requireInteraction = requireInteraction; }
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        
        public NotificationAction[] getActions() { return actions; }
        public void setActions(NotificationAction[] actions) { this.actions = actions; }
        
        /**
         * Builder pattern for creating notification payloads.
         */
        public static class Builder {
            private final NotificationPayload payload = new NotificationPayload();
            
            public Builder title(String title) {
                payload.setTitle(title);
                return this;
            }
            
            public Builder body(String body) {
                payload.setBody(body);
                return this;
            }
            
            public Builder icon(String icon) {
                payload.setIcon(icon);
                return this;
            }
            
            public Builder badge(String badge) {
                payload.setBadge(badge);
                return this;
            }
            
            public Builder tag(String tag) {
                payload.setTag(tag);
                return this;
            }
            
            public Builder requireInteraction(boolean requireInteraction) {
                payload.setRequireInteraction(requireInteraction);
                return this;
            }
            
            public Builder data(Object data) {
                payload.setData(data);
                return this;
            }
            
            public Builder actions(NotificationAction... actions) {
                payload.setActions(actions);
                return this;
            }
            
            public NotificationPayload build() {
                return payload;
            }
        }
    }
    
    /**
     * Data class representing a notification action button.
     */
    public static class NotificationAction {
        private String action;
        private String title;
        private String icon;
        
        public NotificationAction() {}
        
        public NotificationAction(String action, String title) {
            this.action = action;
            this.title = title;
        }
        
        public NotificationAction(String action, String title, String icon) {
            this.action = action;
            this.title = title;
            this.icon = icon;
        }
        
        // Getters and setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
}