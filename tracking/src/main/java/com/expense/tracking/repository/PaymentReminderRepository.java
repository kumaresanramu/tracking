package com.expense.tracking.repository;

import com.expense.tracking.entity.PaymentReminder;
import com.expense.tracking.entity.ReminderFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {
    
    // Find active reminders
    List<PaymentReminder> findByActiveTrueOrderByDueDateAsc();
    
    // Find reminders by frequency
    List<PaymentReminder> findByFrequencyAndActiveTrueOrderByDueDateAsc(ReminderFrequency frequency);
    
    // Find reminders by category
    List<PaymentReminder> findByCategoryIdAndActiveTrueOrderByDueDateAsc(Long categoryId);
    
    // Find reminders due within a certain number of days
    @Query("SELECT r FROM PaymentReminder r WHERE r.active = true AND " +
           "(r.dueDate - CURRENT_DATE) <= r.daysBefore AND " +
           "(r.dueDate - CURRENT_DATE) >= 0 " +
           "ORDER BY r.dueDate ASC")
    List<PaymentReminder> findDueReminders();
    
    // Find reminders due on a specific date
    @Query("SELECT r FROM PaymentReminder r WHERE r.active = true AND " +
           "(r.dueDate - :date) = r.daysBefore")
    List<PaymentReminder> findRemindersDueOnDate(@Param("date") LocalDate date);
    
    // Find overdue reminders (past due date and not paid)
    @Query("SELECT r FROM PaymentReminder r WHERE r.active = true AND " +
           "r.dueDate < CURRENT_DATE AND " +
           "(r.lastPaid IS NULL OR r.lastPaid < r.dueDate)")
    List<PaymentReminder> findOverdueReminders();
}