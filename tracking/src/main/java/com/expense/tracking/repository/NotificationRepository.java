package com.expense.tracking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expense.tracking.entity.Notification;
import com.expense.tracking.entity.NotificationChannel;
import com.expense.tracking.entity.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find unread notifications
    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();
    
    // Find unsent notifications that are due
    @Query("SELECT n FROM Notification n WHERE n.isSent = false AND n.scheduledFor <= :now")
    List<Notification> findDueNotifications(@Param("now") LocalDateTime now);
    
    // Find notifications by type
    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);
    
    // Find notifications by channel
    List<Notification> findByChannelOrderByCreatedAtDesc(NotificationChannel channel);
    
    // Find recent notifications (last 30 days)
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("since") LocalDateTime since);
    
    // Count unread notifications
    long countByIsReadFalse();
    
    // Find overdue notifications
    @Query("SELECT n FROM Notification n WHERE n.scheduledFor < :now AND n.isSent = false")
    List<Notification> findOverdueNotifications(@Param("now") LocalDateTime now);
    
    // Find notifications for specific time range
    @Query("SELECT n FROM Notification n WHERE n.scheduledFor BETWEEN :start AND :end")
    List<Notification> findNotificationsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}