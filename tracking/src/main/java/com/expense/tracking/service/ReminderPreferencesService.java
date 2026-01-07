package com.expense.tracking.service;

import com.expense.tracking.dto.ReminderPreferencesRequest;
import com.expense.tracking.dto.ReminderPreferencesResponse;
import com.expense.tracking.entity.ReminderPreferences;
import com.expense.tracking.entity.PaymentReminder;
import com.expense.tracking.repository.ReminderPreferencesRepository;
import com.expense.tracking.repository.PaymentReminderRepository;
import com.expense.tracking.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderPreferencesService {
    
    private final ReminderPreferencesRepository reminderPreferencesRepository;
    private final PaymentReminderRepository paymentReminderRepository;
    
    @Transactional
    public ReminderPreferencesResponse createOrUpdatePreferences(ReminderPreferencesRequest request) {
        log.info("Creating/updating reminder preferences for reminder ID: {}", request.getReminderId());
        
        // Verify that the reminder exists
        PaymentReminder reminder = paymentReminderRepository.findById(request.getReminderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment reminder not found with id: " + request.getReminderId()));
        
        // Check if preferences already exist
        Optional<ReminderPreferences> existingPreferences = reminderPreferencesRepository.findByReminderId(request.getReminderId());
        
        ReminderPreferences preferences;
        if (existingPreferences.isPresent()) {
            // Update existing preferences
            preferences = existingPreferences.get();
            updatePreferencesFromRequest(preferences, request);
            log.info("Updating existing preferences for reminder ID: {}", request.getReminderId());
        } else {
            // Create new preferences
            preferences = ReminderPreferences.builder()
                    .reminder(reminder)
                    .daysBefore(request.getDaysBefore())
                    .notificationTime(request.getNotificationTime())
                    .weekendsOnly(request.getWeekendsOnly())
                    .weekdaysOnly(request.getWeekdaysOnly())
                    .specificDays(request.getSpecificDays())
                    .build();
            log.info("Creating new preferences for reminder ID: {}", request.getReminderId());
        }
        
        ReminderPreferences savedPreferences = reminderPreferencesRepository.save(preferences);
        log.info("Saved reminder preferences with ID: {}", savedPreferences.getId());
        
        return mapToResponse(savedPreferences);
    }
    
    @Transactional(readOnly = true)
    public Optional<ReminderPreferencesResponse> getPreferencesByReminderId(Long reminderId) {
        log.debug("Fetching reminder preferences for reminder ID: {}", reminderId);
        
        return reminderPreferencesRepository.findByReminderId(reminderId)
                .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public ReminderPreferencesResponse getPreferencesById(Long id) {
        log.debug("Fetching reminder preferences with ID: {}", id);
        
        ReminderPreferences preferences = reminderPreferencesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder preferences not found with id: " + id));
        
        return mapToResponse(preferences);
    }
    
    @Transactional
    public void deletePreferences(Long id) {
        log.info("Deleting reminder preferences with ID: {}", id);
        
        if (!reminderPreferencesRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reminder preferences not found with id: " + id);
        }
        
        reminderPreferencesRepository.deleteById(id);
        log.info("Deleted reminder preferences with ID: {}", id);
    }
    
    @Transactional
    public void deletePreferencesByReminderId(Long reminderId) {
        log.info("Deleting reminder preferences for reminder ID: {}", reminderId);
        
        reminderPreferencesRepository.deleteByReminderId(reminderId);
        log.info("Deleted reminder preferences for reminder ID: {}", reminderId);
    }
    
    private void updatePreferencesFromRequest(ReminderPreferences preferences, ReminderPreferencesRequest request) {
        preferences.setDaysBefore(request.getDaysBefore());
        preferences.setNotificationTime(request.getNotificationTime());
        preferences.setWeekendsOnly(request.getWeekendsOnly());
        preferences.setWeekdaysOnly(request.getWeekdaysOnly());
        preferences.setSpecificDays(request.getSpecificDays());
    }
    
    private ReminderPreferencesResponse mapToResponse(ReminderPreferences preferences) {
        return ReminderPreferencesResponse.builder()
                .id(preferences.getId())
                .reminderId(preferences.getReminder().getId())
                .daysBefore(preferences.getDaysBefore())
                .notificationTime(preferences.getNotificationTime())
                .weekendsOnly(preferences.getWeekendsOnly())
                .weekdaysOnly(preferences.getWeekdaysOnly())
                .specificDays(preferences.getSpecificDays())
                .build();
    }
}