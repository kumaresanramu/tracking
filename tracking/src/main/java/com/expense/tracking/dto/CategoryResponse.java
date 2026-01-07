package com.expense.tracking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    
    private Long id;
    private String name;
    private String color;
    private String description;
    private String fullPath;
    private Long parentId;
    private List<CategoryResponse> subcategories;
    private boolean isRootCategory;
}