package com.expense.tracking.controller;

import com.expense.tracking.dto.ReminderPreferencesRequest;
import com.expense.tracking.dto.ReminderPreferencesResponse;
import com.expense.tracking.service.ReminderPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/reminder-preferences")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReminderPreferencesController {
    
    private final ReminderPreferencesService reminderPreferencesService;
    
    @PostMapping
    public ResponseEntity<ReminderPreferencesResponse> createOrUpdatePreferences(@Valid @RequestBody ReminderPreferencesRequest request) {
        log.info("Creating/updating reminder preferences for reminder ID: {}", request.getReminderId());
        ReminderPreferencesResponse response = reminderPreferencesService.createOrUpdatePreferences(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/reminder/{reminderId}")
    public ResponseEntity<ReminderPreferencesResponse> getPreferencesByReminderId(@PathVariable Long reminderId) {
        log.debug("Fetching reminder preferences for reminder ID: {}", reminderId);
        Optional<ReminderPreferencesResponse> preferences = reminderPreferencesService.getPreferencesByReminderId(reminderId);
        
        return preferences.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReminderPreferencesResponse> getPreferencesById(@PathVariable Long id) {
        log.debug("Fetching reminder preferences with ID: {}", id);
        ReminderPreferencesResponse preferences = reminderPreferencesService.getPreferencesById(id);
        return ResponseEntity.ok(preferences);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePreferences(@PathVariable Long id) {
        log.info("Deleting reminder preferences with ID: {}", id);
        reminderPreferencesService.deletePreferences(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/reminder/{reminderId}")
    public ResponseEntity<Void> deletePreferencesByReminderId(@PathVariable Long reminderId) {
        log.info("Deleting reminder preferences for reminder ID: {}", reminderId);
        reminderPreferencesService.deletePreferencesByReminderId(reminderId);
        return ResponseEntity.noContent().build();
    }
}