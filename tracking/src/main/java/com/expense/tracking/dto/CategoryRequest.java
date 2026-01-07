package com.expense.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {
    
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;
    
    private Long parentId;
    
    @Size(max = 7, message = "Color must be a valid hex code (e.g., #FF5733)")
    private String color;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}