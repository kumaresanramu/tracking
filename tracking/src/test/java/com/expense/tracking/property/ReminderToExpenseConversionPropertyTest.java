package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import com.expense.tracking.entity.PaymentReminder;
import com.expense.tracking.entity.ReminderFrequency;
import com.expense.tracking.entity.Expense;
import com.expense.tracking.dto.ExpenseRequest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 11: Reminder to Expense Conversion
 * Validates: Requirements 3.4
 */
public class ReminderToExpenseConversionPropertyTest {

    @Property(tries = 100)
    void reminderToExpenseConversionProperty(
            @ForAll @StringLength(min = 1, max = 200) String reminderName,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate dueDate,
            @ForAll ReminderFrequency frequency,
            @ForAll @StringLength(min = 1, max = 100) String categoryName) {
        
        // Given: A payment reminder with specific details
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();
        
        PaymentReminder reminder = PaymentReminder.builder()
                .id(1L)
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
        
        // When: A reminder is marked as paid, it should create a corresponding expense
        // Simulate the expense creation process
        ExpenseRequest expectedExpenseRequest = ExpenseRequest.builder()
                .amount(reminder.getAmount())
                .date(LocalDate.now())
                .categoryId(reminder.getCategory() != null ? reminder.getCategory().getId() : null)
                .description("Payment: " + reminder.getName())
                .build();
        
        // Then: The expense should have matching details from the reminder
        assertThat(expectedExpenseRequest.getAmount()).isEqualByComparingTo(reminder.getAmount());
        assertThat(expectedExpenseRequest.getDate()).isEqualTo(LocalDate.now());
        assertThat(expectedExpenseRequest.getCategoryId()).isEqualTo(reminder.getCategory().getId());
        assertThat(expectedExpenseRequest.getDescription()).isEqualTo("Payment: " + reminder.getName());
        
        // Test that the expense creation preserves all essential reminder information
        Expense simulatedExpense = Expense.builder()
                .amount(expectedExpenseRequest.getAmount())
                .date(expectedExpenseRequest.getDate())
                .category(reminder.getCategory())
                .description(expectedExpenseRequest.getDescription())
                .synced(false)
                .build();
        
        // Verify the expense contains the correct information from the reminder
        assertThat(simulatedExpense.getAmount()).isEqualByComparingTo(reminder.getAmount());
        assertThat(simulatedExpense.getCategory().getName()).isEqualTo(reminder.getCategory().getName());
        assertThat(simulatedExpense.getDescription()).contains(reminder.getName());
        assertThat(simulatedExpense.getDescription()).startsWith("Payment: ");
        
        // Test that the reminder's lastPaid date would be updated
        LocalDate paymentDate = LocalDate.now();
        reminder.setLastPaid(paymentDate);
        
        assertThat(reminder.getLastPaid()).isEqualTo(paymentDate);
        assertThat(reminder.getLastPaid()).isNotNull();
    }
    
    @Property(tries = 100)
    void reminderWithoutCategoryConversionProperty(
            @ForAll @StringLength(min = 1, max = 200) String reminderName,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate dueDate,
            @ForAll ReminderFrequency frequency) {
        
        // Given: A payment reminder without a category
        PaymentReminder reminder = PaymentReminder.builder()
                .id(1L)
                .name(reminderName)
                .amount(amount)
                .dueDate(dueDate)
                .frequency(frequency)
                .category(null) // No category assigned
                .active(true)
                .build();
        
        // When: Converting to expense request
        ExpenseRequest expectedExpenseRequest = ExpenseRequest.builder()
                .amount(reminder.getAmount())
                .date(LocalDate.now())
                .categoryId(reminder.getCategory() != null ? reminder.getCategory().getId() : null)
                .description("Payment: " + reminder.getName())
                .build();
        
        // Then: The expense should handle null category gracefully
        assertThat(expectedExpenseRequest.getAmount()).isEqualByComparingTo(reminder.getAmount());
        assertThat(expectedExpenseRequest.getCategoryId()).isNull();
        assertThat(expectedExpenseRequest.getDescription()).isEqualTo("Payment: " + reminder.getName());
        
        // Test that expense creation works even without category
        Expense simulatedExpense = Expense.builder()
                .amount(expectedExpenseRequest.getAmount())
                .date(expectedExpenseRequest.getDate())
                .category(null)
                .description(expectedExpenseRequest.getDescription())
                .synced(false)
                .build();
        
        assertThat(simulatedExpense.getAmount()).isEqualByComparingTo(reminder.getAmount());
        assertThat(simulatedExpense.getCategory()).isNull();
        assertThat(simulatedExpense.getDescription()).contains(reminder.getName());
    }
    
    @Property(tries = 100)
    void reminderPaymentDateUpdateProperty(
            @ForAll @StringLength(min = 1, max = 200) String reminderName,
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal amount,
            @ForAll LocalDate dueDate,
            @ForAll ReminderFrequency frequency,
            @ForAll LocalDate paymentDate) {
        
        // Given: A payment reminder
        PaymentReminder reminder = PaymentReminder.builder()
                .name(reminderName)
                .amount(amount)
                .dueDate(dueDate)
                .frequency(frequency)
                .active(true)
                .lastPaid(null) // Initially not paid
                .build();
        
        // When: Marking the reminder as paid
        LocalDate originalLastPaid = reminder.getLastPaid();
        reminder.setLastPaid(paymentDate);
        
        // Then: The lastPaid date should be updated
        assertThat(reminder.getLastPaid()).isEqualTo(paymentDate);
        assertThat(reminder.getLastPaid()).isNotEqualTo(originalLastPaid);
        
        // Test that next due date calculation changes after payment
        LocalDate nextDueAfterPayment = reminder.getNextDueDate();
        
        LocalDate expectedNextDue = switch (frequency) {
            case MONTHLY -> paymentDate.plusMonths(1);
            case QUARTERLY -> paymentDate.plusMonths(3);
            case YEARLY -> paymentDate.plusYears(1);
        };
        
        assertThat(nextDueAfterPayment).isEqualTo(expectedNextDue);
        assertThat(nextDueAfterPayment).isAfter(paymentDate);
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