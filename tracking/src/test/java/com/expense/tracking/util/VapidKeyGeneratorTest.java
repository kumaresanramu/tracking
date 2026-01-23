package com.expense.tracking.util;

import org.junit.jupiter.api.Test;

import com.expense.tracking.config.VapidKeyGenerator;

public class VapidKeyGeneratorTest {
    
    @Test
    void generateVapidKeysForConfiguration() {
        System.out.println("=== VAPID Key Generator ===");
        System.out.println();
        
        VapidKeyGenerator generator = new VapidKeyGenerator();
        VapidKeyGenerator.VapidKeyPair keyPair = generator.generateVapidKeys();
        
        System.out.println("✓ VAPID Keys Generated Successfully!");
        System.out.println();
        System.out.println("=== COPY THESE ENVIRONMENT VARIABLES ===");
        System.out.println();
        System.out.println("VAPID_PUBLIC_KEY=" + keyPair.getPublicKey());
        System.out.println("VAPID_PRIVATE_KEY=" + keyPair.getPrivateKey());
        System.out.println("VAPID_SUBJECT=mailto:your_email@example.com");
        System.out.println();
        System.out.println("=== Windows PowerShell Commands ===");
        System.out.println("$env:VAPID_PUBLIC_KEY=\"" + keyPair.getPublicKey() + "\"");
        System.out.println("$env:VAPID_PRIVATE_KEY=\"" + keyPair.getPrivateKey() + "\"");
        System.out.println("$env:VAPID_SUBJECT=\"mailto:your_email@example.com\"");
        System.out.println();
        System.out.println("=== Windows Command Prompt Commands ===");
        System.out.println("set VAPID_PUBLIC_KEY=" + keyPair.getPublicKey());
        System.out.println("set VAPID_PRIVATE_KEY=" + keyPair.getPrivateKey());
        System.out.println("set VAPID_SUBJECT=mailto:your_email@example.com");
        System.out.println();
        System.out.println("⚠️  IMPORTANT: Keep your private key secure!");
        System.out.println("✓  After setting these variables, restart your application.");
        System.out.println();
    }
}