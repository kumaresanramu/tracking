package com.expense.tracking.service;

import com.expense.tracking.dto.CategoryRequest;
import com.expense.tracking.dto.CategoryResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.exception.ResourceNotFoundException;
import com.expense.tracking.exception.ValidationException;
import com.expense.tracking.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    /**
     * Get all categories in hierarchical structure
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all categories");
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByName();
        return rootCategories.stream()
                .map(this::convertToResponseWithSubcategories)
                .collect(Collectors.toList());
    }
    
    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return convertToResponseWithSubcategories(category);
    }
    
    /**
     * Create a new category
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        log.debug("Creating new category: {}", request.getName());
        
        // Validate category name uniqueness
        if (categoryRepository.existsByName(request.getName())) {
            throw new ValidationException("Category with name '" + request.getName() + "' already exists");
        }
        
        Category category = Category.builder()
                .name(request.getName())
                .color(request.getColor())
                .description(request.getDescription())
                .build();
        
        // Set parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));
            category.setParent(parent);
        }
        
        Category savedCategory = categoryRepository.save(category);
        log.info("Created category: {} with id: {}", savedCategory.getName(), savedCategory.getId());
        
        return convertToResponse(savedCategory);
    }
    
    /**
     * Update an existing category
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.debug("Updating category with id: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        // Check name uniqueness if name is being changed
        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new ValidationException("Category with name '" + request.getName() + "' already exists");
        }
        
        category.setName(request.getName());
        category.setColor(request.getColor());
        category.setDescription(request.getDescription());
        
        // Update parent if provided
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new ValidationException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        
        Category savedCategory = categoryRepository.save(category);
        log.info("Updated category: {} with id: {}", savedCategory.getName(), savedCategory.getId());
        
        return convertToResponse(savedCategory);
    }
    
    /**
     * Delete a category
     */
    public void deleteCategory(Long id) {
        log.debug("Deleting category with id: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        // Check if category has subcategories
        if (!category.getSubcategories().isEmpty()) {
            throw new ValidationException("Cannot delete category with subcategories. Delete subcategories first.");
        }
        
        categoryRepository.delete(category);
        log.info("Deleted category with id: {}", id);
    }
    
    /**
     * Get subcategories of a parent category
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Long parentId) {
        log.debug("Fetching subcategories for parent id: {}", parentId);
        
        // Verify parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent category not found with id: " + parentId);
        }
        
        List<Category> subcategories = categoryRepository.findByParentIdOrderByName(parentId);
        return subcategories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert Category entity to CategoryResponse DTO
     */
    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .description(category.getDescription())
                .fullPath(category.getFullPath())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .isRootCategory(category.isRootCategory())
                .build();
    }
    
    /**
     * Convert Category entity to CategoryResponse DTO with subcategories
     */
    private CategoryResponse convertToResponseWithSubcategories(Category category) {
        List<CategoryResponse> subcategoryResponses = category.getSubcategories().stream()
                .map(this::convertToResponseWithSubcategories)
                .collect(Collectors.toList());
        
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .description(category.getDescription())
                .fullPath(category.getFullPath())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subcategories(subcategoryResponses)
                .isRootCategory(category.isRootCategory())
                .build();
    }
}