package com.expense.tracking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.tracking.dto.NotificationSettingsRequest;
import com.expense.tracking.dto.NotificationSettingsResponse;
import com.expense.tracking.service.NotificationSettingsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingsController {
    
    private final NotificationSettingsService settingsService;
    
    @GetMapping
    public ResponseEntity<NotificationSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }
    
    @PutMapping
    public ResponseEntity<NotificationSettingsResponse> updateSettings(@Valid @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }
    
    @PostMapping("/reset")
    public ResponseEntity<Void> resetToDefaults() {
        settingsService.resetToDefaults();
        return ResponseEntity.ok().build();
    }
}