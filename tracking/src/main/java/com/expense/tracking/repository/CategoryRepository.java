package com.expense.tracking.repository;

import com.expense.tracking.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Find root categories (categories without parent)
    List<Category> findByParentIsNullOrderByName();
    
    // Find subcategories of a parent category
    List<Category> findByParentIdOrderByName(Long parentId);
    
    // Find category by name
    Optional<Category> findByName(String name);
    
    // Find category by name and parent
    Optional<Category> findByNameAndParentId(String name, Long parentId);
    
    // Check if category exists by name
    boolean existsByName(String name);
    
    // Get all categories in hierarchical order
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories ORDER BY c.parent.id NULLS FIRST, c.name")
    List<Category> findAllWithSubcategories();
    
    // Find categories by parent with subcategories loaded
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories WHERE c.parent.id = :parentId ORDER BY c.name")
    List<Category> findByParentIdWithSubcategories(@Param("parentId") Long parentId);
}