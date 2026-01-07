package com.expense.tracking.property;

import com.expense.tracking.entity.Category;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: expense-tracking, Property 2: Category Hierarchy Display
 * Validates: Requirements 1.2
 */
public class CategoryHierarchyDisplayPropertyTest {

    @Property(tries = 100)
    void categoryHierarchyDisplay(
            @ForAll @StringLength(min = 1, max = 100) String parentName,
            @ForAll("subcategoryNames") List<String> subcategoryNames) {
        
        // Given: A category with subcategories
        Category parent = Category.builder()
                .name(parentName)
                .subcategories(new ArrayList<>())
                .build();
        
        List<Category> expectedSubcategories = new ArrayList<>();
        for (String subcategoryName : subcategoryNames) {
            Category subcategory = Category.builder()
                    .name(subcategoryName)
                    .parent(parent)
                    .build();
            expectedSubcategories.add(subcategory);
            parent.getSubcategories().add(subcategory);
        }
        
        // When: Selecting that category to display subcategories
        List<Category> actualSubcategories = parent.getSubcategories();
        
        // Then: Should display all and only its direct subcategories
        assertThat(actualSubcategories).hasSize(subcategoryNames.size());
        
        // Verify all expected subcategories are present
        List<String> actualSubcategoryNames = actualSubcategories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        
        assertThat(actualSubcategoryNames).containsExactlyInAnyOrderElementsOf(subcategoryNames);
        
        // Verify each subcategory has the correct parent
        for (Category subcategory : actualSubcategories) {
            assertThat(subcategory.getParent()).isEqualTo(parent);
            assertThat(subcategory.getParent().getName()).isEqualTo(parentName);
        }
        
        // Verify parent-child relationship integrity
        for (Category subcategory : expectedSubcategories) {
            assertThat(parent.getSubcategories()).contains(subcategory);
            assertThat(subcategory.getParent()).isEqualTo(parent);
        }
    }

    @Property(tries = 100)
    void rootCategoryHasNoParent(
            @ForAll @StringLength(min = 1, max = 100) String categoryName) {
        
        // Given: A root category (no parent)
        Category rootCategory = Category.builder()
                .name(categoryName)
                .parent(null)
                .build();
        
        // When: Checking if it's a root category
        // Then: Should be identified as root category with no parent
        assertThat(rootCategory.isRootCategory()).isTrue();
        assertThat(rootCategory.getParent()).isNull();
        assertThat(rootCategory.getFullPath()).isEqualTo(categoryName);
    }

    @Property(tries = 100)
    void subcategoryHasCorrectParent(
            @ForAll @StringLength(min = 1, max = 100) String parentName,
            @ForAll @StringLength(min = 1, max = 100) String childName) {
        
        Assume.that(!parentName.equals(childName)); // Ensure parent and child have different names
        
        // Given: A parent category and a subcategory
        Category parent = Category.builder()
                .name(parentName)
                .subcategories(new ArrayList<>())
                .build();
        
        Category child = Category.builder()
                .name(childName)
                .parent(parent)
                .build();
        
        parent.getSubcategories().add(child);
        
        // When: Checking the subcategory's parent relationship
        // Then: Should correctly identify parent and generate full path
        assertThat(child.isRootCategory()).isFalse();
        assertThat(child.getParent()).isEqualTo(parent);
        assertThat(child.getParent().getName()).isEqualTo(parentName);
        assertThat(child.getFullPath()).isEqualTo(parentName + " > " + childName);
        assertThat(parent.getSubcategories()).contains(child);
    }
    
    @Provide
    Arbitrary<List<String>> subcategoryNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5)
                .map(list -> list.stream().distinct().collect(Collectors.toList())); // Ensure unique names
    }
}