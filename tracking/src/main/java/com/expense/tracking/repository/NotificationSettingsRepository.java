package com.expense.tracking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.expense.tracking.entity.NotificationSettings;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    
    Optional<NotificationSettings> findByUserId(String userId);
    
    default NotificationSettings findByUserIdOrDefault(String userId) {
        return findByUserId(userId).orElse(NotificationSettings.builder().userId(userId).build());
    }
}