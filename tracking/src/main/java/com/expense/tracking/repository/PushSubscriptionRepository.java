package com.expense.tracking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.expense.tracking.entity.PushSubscription;

/**
 * Repository interface for managing PushSubscription entities.
 * Provides custom queries for push notification subscription management.
 */
@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    
    /**
     * Finds a push subscription by its endpoint URL.
     * 
     * @param endpoint the push service endpoint URL
     * @return Optional containing the subscription if found
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);
    
    /**
     * Finds all active push subscriptions for a specific user.
     * 
     * @param userId the user ID
     * @return list of active subscriptions for the user
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.userId = :userId AND ps.active = true")
    List<PushSubscription> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * Finds all active push subscriptions.
     * 
     * @return list of all active subscriptions
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.active = true")
    List<PushSubscription> findAllActive();
    
    /**
     * Finds all push subscriptions that should be considered stale.
     * A subscription is stale if:
     * - It has more than the specified failure count
     * - It hasn't been used in the specified number of days
     * - It was created more than the specified days ago and never used
     * 
     * @param maxFailures maximum number of failures before considering stale
     * @param daysUnused number of days without use before considering stale
     * @return list of stale subscriptions
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE " +
           "ps.failureCount > :maxFailures OR " +
           "(ps.lastUsedAt IS NOT NULL AND ps.lastUsedAt < :unusedThreshold) OR " +
           "(ps.lastUsedAt IS NULL AND ps.createdAt < :unusedThreshold)")
    List<PushSubscription> findStaleSubscriptions(
        @Param("maxFailures") Integer maxFailures,
        @Param("unusedThreshold") LocalDateTime unusedThreshold
    );
    
    /**
     * Finds push subscriptions that have failed recently and might need attention.
     * 
     * @param minFailures minimum number of failures to include
     * @param since only include subscriptions that failed after this time
     * @return list of subscriptions with recent failures
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE " +
           "ps.failureCount >= :minFailures AND " +
           "ps.lastFailureAt > :since")
    List<PushSubscription> findSubscriptionsWithRecentFailures(
        @Param("minFailures") Integer minFailures,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Counts the number of active subscriptions for a user.
     * 
     * @param userId the user ID
     * @return number of active subscriptions
     */
    @Query("SELECT COUNT(ps) FROM PushSubscription ps WHERE ps.userId = :userId AND ps.active = true")
    Long countActiveByUserId(@Param("userId") Long userId);
    
    /**
     * Counts the total number of active subscriptions.
     * 
     * @return total number of active subscriptions
     */
    @Query("SELECT COUNT(ps) FROM PushSubscription ps WHERE ps.active = true")
    Long countAllActive();
    
    /**
     * Deactivates a push subscription by endpoint.
     * 
     * @param endpoint the push service endpoint URL
     * @return number of subscriptions updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET ps.active = false, ps.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ps.endpoint = :endpoint")
    int deactivateByEndpoint(@Param("endpoint") String endpoint);
    
    /**
     * Deactivates all push subscriptions for a user.
     * 
     * @param userId the user ID
     * @return number of subscriptions updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET ps.active = false, ps.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ps.userId = :userId")
    int deactivateAllByUserId(@Param("userId") Long userId);
    
    /**
     * Updates the last used timestamp for a subscription.
     * 
     * @param endpoint the push service endpoint URL
     * @return number of subscriptions updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET ps.lastUsedAt = CURRENT_TIMESTAMP, " +
           "ps.updatedAt = CURRENT_TIMESTAMP WHERE ps.endpoint = :endpoint")
    int updateLastUsed(@Param("endpoint") String endpoint);
    
    /**
     * Increments the failure count for a subscription.
     * 
     * @param endpoint the push service endpoint URL
     * @return number of subscriptions updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET " +
           "ps.failureCount = COALESCE(ps.failureCount, 0) + 1, " +
           "ps.lastFailureAt = CURRENT_TIMESTAMP, " +
           "ps.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ps.endpoint = :endpoint")
    int incrementFailureCount(@Param("endpoint") String endpoint);
    
    /**
     * Resets the failure count for a subscription.
     * 
     * @param endpoint the push service endpoint URL
     * @return number of subscriptions updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET " +
           "ps.failureCount = 0, " +
           "ps.lastFailureAt = NULL, " +
           "ps.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ps.endpoint = :endpoint")
    int resetFailureCount(@Param("endpoint") String endpoint);
    
    /**
     * Deletes all stale subscriptions.
     * This is a cleanup operation that should be run periodically.
     * 
     * @param maxFailures maximum number of failures before deletion
     * @param unusedThreshold subscriptions unused before this time will be deleted
     * @return number of subscriptions deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PushSubscription ps WHERE " +
           "ps.failureCount > :maxFailures OR " +
           "(ps.lastUsedAt IS NOT NULL AND ps.lastUsedAt < :unusedThreshold) OR " +
           "(ps.lastUsedAt IS NULL AND ps.createdAt < :unusedThreshold)")
    int deleteStaleSubscriptions(
        @Param("maxFailures") Integer maxFailures,
        @Param("unusedThreshold") LocalDateTime unusedThreshold
    );
    
    /**
     * Checks if a subscription exists and is active for the given endpoint.
     * 
     * @param endpoint the push service endpoint URL
     * @return true if an active subscription exists
     */
    @Query("SELECT COUNT(ps) > 0 FROM PushSubscription ps WHERE ps.endpoint = :endpoint AND ps.active = true")
    boolean existsActiveByEndpoint(@Param("endpoint") String endpoint);
}