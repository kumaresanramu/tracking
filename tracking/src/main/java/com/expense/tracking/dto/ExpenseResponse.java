package com.expense.tracking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}