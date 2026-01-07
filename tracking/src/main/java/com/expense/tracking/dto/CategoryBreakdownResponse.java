package com.expense.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownResponse {
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal totalAmount;
    private Long expenseCount;
    private BigDecimal percentage;
    private List<CategoryBreakdownResponse> subcategories;
}