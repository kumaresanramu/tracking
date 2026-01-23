package com.expense.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating push subscriptions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscriptionRequest {
    
    /**
     * The push service endpoint URL where notifications should be sent.
     */
    @NotBlank(message = "Endpoint is required")
    private String endpoint;
    
    /**
     * The P256DH key used for message encryption.
     * Base64-encoded public key for ECDH key agreement.
     */
    @NotBlank(message = "P256DH key is required")
    private String p256dhKey;
    
    /**
     * The authentication secret used for message authentication.
     * Base64-encoded random bytes for HMAC authentication.
     */
    @NotBlank(message = "Auth key is required")
    private String authKey;
    
    /**
     * User agent string of the browser creating this subscription.
     */
    private String userAgent;
    
    /**
     * User ID to associate with this subscription.
     * In a real application, this would typically come from authentication context.
     */
    private Long userId;
    
    /**
     * Keys object containing the encryption keys.
     * This is typically how the browser provides the keys in the subscription object.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Keys {
        @NotBlank(message = "P256DH key is required")
        private String p256dh;
        
        @NotBlank(message = "Auth key is required")
        private String auth;
    }
    
    /**
     * Alternative constructor that accepts keys as a nested object.
     * This matches the typical browser push subscription format.
     */
    public static PushSubscriptionRequest fromBrowserSubscription(String endpoint, Keys keys, String userAgent, Long userId) {
        return PushSubscriptionRequest.builder()
            .endpoint(endpoint)
            .p256dhKey(keys.getP256dh())
            .authKey(keys.getAuth())
            .userAgent(userAgent)
            .userId(userId)
            .build();
    }
}