package com.expense.tracking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    
    private Long id;
    private BigDecimal amount;
    private LocalDate date;
    private CategoryResponse category;
    private String description;
    private String paymentMethod;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}