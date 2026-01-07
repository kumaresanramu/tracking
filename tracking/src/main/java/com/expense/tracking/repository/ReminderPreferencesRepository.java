package com.expense.tracking.repository;

import com.expense.tracking.entity.ReminderPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReminderPreferencesRepository extends JpaRepository<ReminderPreferences, Long> {
    
    // Find preferences by reminder ID
    Optional<ReminderPreferences> findByReminderId(Long reminderId);
    
    // Check if preferences exist for a reminder
    boolean existsByReminderId(Long reminderId);
    
    // Delete preferences by reminder ID
    void deleteByReminderId(Long reminderId);
}