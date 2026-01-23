package com.expense.tracking.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.service.NotificationPermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing notification permissions.
 */
@RestController
@RequestMapping("/api/notification-permissions")
@RequiredArgsConstructor
@Slf4j
public class NotificationPermissionController {
    
    private final NotificationPermissionService notificationPermissionService;
    
    /**
     * Get current notification permission status.
     */
    @GetMapping("/status")
    public ResponseEntity<NotificationPermissionService.NotificationPermissionStatus> getPermissionStatus() {
        return ResponseEntity.ok(notificationPermissionService.getPermissionStatus());
    }
    
    /**
     * Request permission for a notification channel.
     */
    @PostMapping("/request")
    public ResponseEntity<PermissionRequestResponse> requestPermission(@RequestBody PermissionRequest request) {
        try {
            NotificationChannel channel = NotificationChannel.valueOf(request.getChannel().toUpperCase());
            boolean granted = notificationPermissionService.requestNotificationPermission(channel, request.getReason());
            
            return ResponseEntity.ok(PermissionRequestResponse.builder()
                    .success(granted)
                    .message(granted ? "Permission granted" : "Permission denied")
                    .channel(channel.name())
                    .build());
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(PermissionRequestResponse.builder()
                            .success(false)
                            .message("Invalid channel: " + request.getChannel())
                            .build());
        } catch (Exception e) {
            log.error("Error requesting permission for channel: {}", request.getChannel(), e);
            return ResponseEntity.internalServerError()
                    .body(PermissionRequestResponse.builder()
                            .success(false)
                            .message("Internal server error")
                            .build());
        }
    }
    
    /**
     * Revoke permission for a notification channel.
     */
    @PostMapping("/revoke")
    public ResponseEntity<PermissionRequestResponse> revokePermission(@RequestBody Map<String, String> request) {
        try {
            String channelName = request.get("channel");
            NotificationChannel channel = NotificationChannel.valueOf(channelName.toUpperCase());
            
            notificationPermissionService.handlePermissionRevocation(channel);
            
            return ResponseEntity.ok(PermissionRequestResponse.builder()
                    .success(true)
                    .message("Permission revoked and cleanup completed")
                    .channel(channel.name())
                    .build());
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(PermissionRequestResponse.builder()
                            .success(false)
                            .message("Invalid channel: " + request.get("channel"))
                            .build());
        } catch (Exception e) {
            log.error("Error revoking permission for channel: {}", request.get("channel"), e);
            return ResponseEntity.internalServerError()
                    .body(PermissionRequestResponse.builder()
                            .success(false)
                            .message("Internal server error")
                            .build());
        }
    }
    
    /**
     * Validate and enforce current notification preferences.
     */
    @PostMapping("/validate")
    public ResponseEntity<Void> validatePreferences() {
        try {
            notificationPermissionService.validateAndEnforcePreferences();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error validating preferences", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Request DTO for permission requests.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionRequest {
        private String channel;
        private String reason;
    }
    
    /**
     * Response DTO for permission requests.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionRequestResponse {
        private boolean success;
        private String message;
        private String channel;
    }
}