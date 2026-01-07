package com.expense.tracking.property;

import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.entity.Category;
import com.expense.tracking.exception.ValidationException;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.service.ExpenseService;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based test for input validation
 * **Feature: expense-tracking, Property 19: Input Validation**
 * **Validates: Requirements 7.4**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InputValidationPropertyTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Create a test category for expenses
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF5733")
                .description("Test category for property tests")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Property(tries = 100)
    @Label("For any incoming expense data, invalid data should be rejected with appropriate error messages, while valid data should be processed successfully")
    void inputValidation(
            @ForAll("expenseRequests") ExpenseRequest request) {

        if (isValidRequest(request)) {
            // Valid requests should be processed successfully
            assertThat(expenseService.createExpense(request)).isNotNull();
        } else {
            // Invalid requests should throw ValidationException
            assertThatThrownBy(() -> expenseService.createExpense(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(getExpectedErrorMessage(request));
        }
    }

    @Provide
    Arbitrary<ExpenseRequest> expenseRequests() {
        return Combinators.combine(
                // Amount - can be null, negative, zero, or positive
                Arbitraries.oneOf(
                        Arbitraries.just((BigDecimal) null),
                        Arbitraries.bigDecimals().between(BigDecimal.valueOf(-1000), BigDecimal.valueOf(0)),
                        Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(10000))
                ),
                // Date - can be null, future, or valid
                Arbitraries.oneOf(
                        Arbitraries.just((LocalDate) null),
                        Arbitraries.of(LocalDate.now().plusDays(1), LocalDate.now().plusYears(1)),
                        Arbitraries.of(LocalDate.now(), LocalDate.now().minusDays(30), LocalDate.of(2023, 1, 1))
                ),
                // Category ID - can be null or valid
                Arbitraries.oneOf(
                        Arbitraries.just((Long) null),
                        Arbitraries.just(999L) // Non-existent category
                ),
                // Description - can be null, empty, valid, or too long
                Arbitraries.oneOf(
                        Arbitraries.just((String) null),
                        Arbitraries.just(""),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(500),
                        Arbitraries.strings().alpha().ofMinLength(501).ofMaxLength(600)
                )
        ).as((amount, date, categoryId, description) -> {
            // Use testCategory ID if available, otherwise use the generated categoryId
            Long finalCategoryId = (categoryId == null || categoryId == 999L) ? categoryId : 
                                  (testCategory != null ? testCategory.getId() : 1L);
            return ExpenseRequest.builder()
                    .amount(amount)
                    .date(date)
                    .categoryId(finalCategoryId)
                    .description(description)
                    .build();
        });
    }

    private boolean isValidRequest(ExpenseRequest request) {
        return request.getAmount() != null &&
               request.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               request.getDate() != null &&
               !request.getDate().isAfter(LocalDate.now()) &&
               request.getCategoryId() != null &&
               (testCategory != null && request.getCategoryId().equals(testCategory.getId())) &&
               (request.getDescription() == null || request.getDescription().length() <= 500);
    }

    private String getExpectedErrorMessage(ExpenseRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Amount must be positive";
        }
        if (request.getDate() == null) {
            return "Date is required";
        }
        if (request.getDate().isAfter(LocalDate.now())) {
            return "Date cannot be in the future";
        }
        if (request.getCategoryId() == null) {
            return "Category ID is required";
        }
        if (testCategory == null || !request.getCategoryId().equals(testCategory.getId())) {
            return "Category not found";
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            return "Description cannot exceed 500 characters";
        }
        return "Validation error";
    }
}