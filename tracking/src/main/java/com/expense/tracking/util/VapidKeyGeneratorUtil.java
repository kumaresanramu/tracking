package com.expense.tracking.util;

import com.expense.tracking.config.VapidKeyGenerator;

/**
 * Utility to generate VAPID keys for push notifications.
 * Run this class to generate new VAPID keys.
 */
public class VapidKeyGeneratorUtil {
    
    public static void main(String[] args) {
        System.out.println("=== VAPID Key Generator ===");
        System.out.println();
        
        VapidKeyGenerator generator = new VapidKeyGenerator();
        VapidKeyGenerator.VapidKeyPair keyPair = generator.generateVapidKeys();
        
        System.out.println("✓ VAPID Keys Generated Successfully!");
        System.out.println();
        System.out.println("Add these environment variables to your system:");
        System.out.println();
        System.out.println("VAPID_PUBLIC_KEY=" + keyPair.getPublicKey());
        System.out.println("VAPID_PRIVATE_KEY=" + keyPair.getPrivateKey());
        System.out.println("VAPID_SUBJECT=mailto:your_email@example.com");
        System.out.println();
        System.out.println("=== Windows (Command Prompt) ===");
        System.out.println("set VAPID_PUBLIC_KEY=" + keyPair.getPublicKey());
        System.out.println("set VAPID_PRIVATE_KEY=" + keyPair.getPrivateKey());
        System.out.println("set VAPID_SUBJECT=mailto:your_email@example.com");
        System.out.println();
        System.out.println("=== Windows (PowerShell) ===");
        System.out.println("$env:VAPID_PUBLIC_KEY=\"" + keyPair.getPublicKey() + "\"");
        System.out.println("$env:VAPID_PRIVATE_KEY=\"" + keyPair.getPrivateKey() + "\"");
        System.out.println("$env:VAPID_SUBJECT=\"mailto:your_email@example.com\"");
        System.out.println();
        System.out.println("=== Linux/Mac ===");
        System.out.println("export VAPID_PUBLIC_KEY=" + keyPair.getPublicKey());
        System.out.println("export VAPID_PRIVATE_KEY=" + keyPair.getPrivateKey());
        System.out.println("export VAPID_SUBJECT=mailto:your_email@example.com");
        System.out.println();
        System.out.println("⚠️  IMPORTANT: Keep your private key secure and never share it publicly!");
        System.out.println("✓  After setting these variables, restart your application.");
    }
}