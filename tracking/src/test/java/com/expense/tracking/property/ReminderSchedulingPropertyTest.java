package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.PaymentReminder;
import com.expense.tracking.entity.ReminderFrequency;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 8: Reminder Scheduling
 * Validates: Requirements 3.1
 */
public class ReminderSchedulingPropertyTest {

    @Property(tries = 100)
    void reminderSchedulingProperty(
            @ForAll @StringLength(min = 1, max = 200) String reminderName,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate dueDate,
            @ForAll ReminderFrequency frequency,
            @ForAll @StringLength(min = 1, max = 100) String categoryName) {
        
        // Given: A recurring expense setup with valid parameters
        Category category = Category.builder()
                .name(categoryName)
                .build();
        
        PaymentReminder reminder = PaymentReminder.builder()
                .name(reminderName)
                .amount(amount)
                .dueDate(dueDate)
                .frequency(frequency)
                .category(category)
                .active(true)
                .daysBefore(3)
                .preferredNotificationTime(LocalTime.of(9, 0))
                .enableEmailNotification(true)
                .enablePushNotification(true)
                .build();
        
        // When: The system creates reminder entries
        // Then: The reminder should have the correct frequency and due dates
        assertThat(reminder.getName()).isEqualTo(reminderName);
        assertThat(reminder.getAmount()).isEqualByComparingTo(amount);
        assertThat(reminder.getDueDate()).isEqualTo(dueDate);
        assertThat(reminder.getFrequency()).isEqualTo(frequency);
        assertThat(reminder.getCategory().getName()).isEqualTo(categoryName);
        assertThat(reminder.getActive()).isTrue();
        
        // Test next due date calculation based on frequency
        LocalDate nextDueDate = reminder.getNextDueDate();
        
        // If no payment has been made, next due date should be the original due date
        if (reminder.getLastPaid() == null) {
            assertThat(nextDueDate).isEqualTo(dueDate);
        }
        
        // Test with a payment made
        LocalDate paymentDate = dueDate.minusDays(1);
        reminder.setLastPaid(paymentDate);
        
        LocalDate expectedNextDue = switch (frequency) {
            case MONTHLY -> paymentDate.plusMonths(1);
            case QUARTERLY -> paymentDate.plusMonths(3);
            case YEARLY -> paymentDate.plusYears(1);
        };
        
        assertThat(reminder.getNextDueDate()).isEqualTo(expectedNextDue);
        
        // Test that the reminder maintains its scheduling properties
        assertThat(reminder.getDaysBefore()).isEqualTo(3);
        assertThat(reminder.getPreferredNotificationTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(reminder.getEnableEmailNotification()).isTrue();
        assertThat(reminder.getEnablePushNotification()).isTrue();
    }
    
    @Property(tries = 100)
    void reminderDueDateCalculationProperty(
            @ForAll LocalDate baseDate,
            @ForAll ReminderFrequency frequency,
            @ForAll @StringLength(min = 1, max = 200) String reminderName,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount) {
        
        // Given: A payment reminder with a specific frequency
        PaymentReminder reminder = PaymentReminder.builder()
                .name(reminderName)
                .amount(amount)
                .dueDate(baseDate)
                .frequency(frequency)
                .active(true)
                .lastPaid(baseDate)
                .build();
        
        // When: Calculating the next due date
        LocalDate nextDueDate = reminder.getNextDueDate();
        
        // Then: The next due date should be correctly calculated based on frequency
        LocalDate expectedNextDue = switch (frequency) {
            case MONTHLY -> baseDate.plusMonths(1);
            case QUARTERLY -> baseDate.plusMonths(3);
            case YEARLY -> baseDate.plusYears(1);
        };
        
        assertThat(nextDueDate).isEqualTo(expectedNextDue);
        
        // Test that the calculation is consistent
        LocalDate secondCalculation = reminder.getNextDueDate();
        assertThat(secondCalculation).isEqualTo(nextDueDate);
        
        // Test that the next due date is always after the last paid date
        assertThat(nextDueDate).isAfter(baseDate);
    }
    
    @Provide
    Arbitrary<LocalDate> localDate() {
        return Arbitraries.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 15),
                LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 6, 15),
                LocalDate.of(2024, 9, 30),
                LocalDate.of(2024, 12, 25),
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 12, 31)
        );
    }
    
    @Provide
    Arbitrary<ReminderFrequency> reminderFrequency() {
        return Arbitraries.of(ReminderFrequency.values());
    }
}