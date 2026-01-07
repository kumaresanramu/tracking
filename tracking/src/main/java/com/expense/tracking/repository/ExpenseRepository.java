package com.expense.tracking.repository;

import com.expense.tracking.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    // Find expenses by month and year
    @Query("SELECT e FROM Expense e JOIN FETCH e.category WHERE YEAR(e.date) = :year AND MONTH(e.date) = :month ORDER BY e.date DESC")
    List<Expense> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    // Find expenses by date range
    List<Expense> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);
    
    // Find expenses by category
    List<Expense> findByCategoryIdOrderByDateDesc(Long categoryId);
    
    // Find unsynced expenses
    List<Expense> findBySyncedFalseOrderByCreatedAtAsc();
    
    // Find expenses by category and date range
    @Query("SELECT e FROM Expense e WHERE e.category.id = :categoryId AND e.date BETWEEN :startDate AND :endDate ORDER BY e.date DESC")
    List<Expense> findByCategoryAndDateRange(@Param("categoryId") Long categoryId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    // Find expense by ID with category loaded
    @Query("SELECT e FROM Expense e JOIN FETCH e.category WHERE e.id = :id")
    Optional<Expense> findByIdWithCategory(@Param("id") Long id);
}