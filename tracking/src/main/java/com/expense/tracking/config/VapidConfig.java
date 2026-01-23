package com.expense.tracking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for VAPID (Voluntary Application Server Identification) keys
 * used for push notification authentication.
 */
@Configuration
@Slf4j
public class VapidConfig {
    
    @Value("${vapid.public.key:}")
    private String vapidPublicKey;
    
    @Value("${vapid.private.key:}")
    private String vapidPrivateKey;
    
    @Value("${vapid.subject:mailto:admin@expensetracker.com}")
    private String vapidSubject;
    
    private final VapidKeyGenerator vapidKeyGenerator;
    
    public VapidConfig(VapidKeyGenerator vapidKeyGenerator) {
        this.vapidKeyGenerator = vapidKeyGenerator;
    }
    
    /**
     * Ensures VAPID keys are available, generating them if not configured.
     * This method is called during application startup.
     */
    @Bean
    public VapidKeyPair vapidKeyPair() {
        if (vapidPublicKey.isEmpty() || vapidPrivateKey.isEmpty()) {
            log.warn("VAPID keys not configured in application.properties. Generating new keys...");
            log.warn("For production use, please set VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY environment variables");
            
            VapidKeyGenerator.VapidKeyPair generatedKeys = vapidKeyGenerator.generateVapidKeys();
            
            log.info("Generated VAPID public key: {}", generatedKeys.getPublicKey());
            log.warn("Generated VAPID private key: [REDACTED - check logs for full key]");
            log.debug("Generated VAPID private key: {}", generatedKeys.getPrivateKey());
            
            return new VapidKeyPair(generatedKeys.getPublicKey(), generatedKeys.getPrivateKey(), vapidSubject);
        } else {
            log.info("Using configured VAPID keys");
            return new VapidKeyPair(vapidPublicKey, vapidPrivateKey, vapidSubject);
        }
    }
    
    /**
     * Data class to hold VAPID configuration.
     */
    public static class VapidKeyPair {
        private final String publicKey;
        private final String privateKey;
        private final String subject;
        
        public VapidKeyPair(String publicKey, String privateKey, String subject) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.subject = subject;
        }
        
        public String getPublicKey() {
            return publicKey;
        }
        
        public String getPrivateKey() {
            return privateKey;
        }
        
        public String getSubject() {
            return subject;
        }
        
        /**
         * Validates that all required VAPID configuration is present.
         */
        public boolean isValid() {
            return publicKey != null && !publicKey.isEmpty() &&
                   privateKey != null && !privateKey.isEmpty() &&
                   subject != null && !subject.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("VapidKeyPair{publicKey='%s...', subject='%s'}", 
                publicKey != null ? publicKey.substring(0, Math.min(20, publicKey.length())) : "null",
                subject);
        }
    }
}