package com.expense.tracking.config;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for generating VAPID (Voluntary Application Server Identification) keys
 * used for push notification authentication.
 */
@Component
@Slf4j
public class VapidKeyGenerator {
    
    static {
        // Add BouncyCastle provider for elliptic curve cryptography
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Generates a new VAPID key pair using P-256 elliptic curve.
     * 
     * @return VapidKeyPair containing base64url-encoded public and private keys
     * @throws RuntimeException if key generation fails
     */
    public VapidKeyPair generateVapidKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("P-256");
            keyPairGenerator.initialize(ecSpec);
            
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
            
            // Convert keys to base64url format as required by VAPID spec
            String publicKeyBase64 = encodePublicKey(publicKey);
            String privateKeyBase64 = encodePrivateKey(privateKey);
            
            log.info("Generated new VAPID key pair");
            return new VapidKeyPair(publicKeyBase64, privateKeyBase64);
            
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            log.error("Failed to generate VAPID keys", e);
            throw new RuntimeException("Failed to generate VAPID keys", e);
        }
    }
    
    /**
     * Encodes an EC public key to base64url format for VAPID.
     * The public key is encoded as an uncompressed point (0x04 + x + y coordinates).
     */
    private String encodePublicKey(ECPublicKey publicKey) {
        // Get the x and y coordinates of the public key point
        byte[] x = publicKey.getW().getAffineX().toByteArray();
        byte[] y = publicKey.getW().getAffineY().toByteArray();
        
        // Ensure coordinates are exactly 32 bytes (remove leading zeros if present)
        x = padOrTrim(x, 32);
        y = padOrTrim(y, 32);
        
        // Create uncompressed point format: 0x04 + x + y
        byte[] uncompressedPoint = new byte[65];
        uncompressedPoint[0] = 0x04;
        System.arraycopy(x, 0, uncompressedPoint, 1, 32);
        System.arraycopy(y, 0, uncompressedPoint, 33, 32);
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressedPoint);
    }
    
    /**
     * Encodes an EC private key to base64url format for VAPID.
     */
    private String encodePrivateKey(ECPrivateKey privateKey) {
        byte[] privateKeyBytes = privateKey.getS().toByteArray();
        // Ensure private key is exactly 32 bytes
        privateKeyBytes = padOrTrim(privateKeyBytes, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(privateKeyBytes);
    }
    
    /**
     * Pads or trims a byte array to the specified length.
     */
    private byte[] padOrTrim(byte[] input, int targetLength) {
        if (input.length == targetLength) {
            return input;
        } else if (input.length > targetLength) {
            // Remove leading zeros
            byte[] trimmed = new byte[targetLength];
            System.arraycopy(input, input.length - targetLength, trimmed, 0, targetLength);
            return trimmed;
        } else {
            // Pad with leading zeros
            byte[] padded = new byte[targetLength];
            System.arraycopy(input, 0, padded, targetLength - input.length, input.length);
            return padded;
        }
    }
    
    /**
     * Data class to hold VAPID key pair.
     */
    public static class VapidKeyPair {
        private final String publicKey;
        private final String privateKey;
        
        public VapidKeyPair(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
        
        public String getPublicKey() {
            return publicKey;
        }
        
        public String getPrivateKey() {
            return privateKey;
        }
        
        @Override
        public String toString() {
            return String.format("VapidKeyPair{publicKey='%s', privateKey='[REDACTED]'}", 
                publicKey.substring(0, Math.min(20, publicKey.length())) + "...");
        }
    }
}