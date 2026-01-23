package com.expense.tracking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.expense.tracking.config.VapidConfig;
import com.expense.tracking.dto.PushSubscriptionRequest;
import com.expense.tracking.entity.PushSubscription;
import com.expense.tracking.repository.PushSubscriptionRepository;

/**
 * Test class for PushSubscriptionService.
 * Tests the constraint violation handling and subscription management.
 */
@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {
    
    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;
    
    @Mock
    private VapidConfig.VapidKeyPair vapidKeyPair;
    
    @InjectMocks
    private PushSubscriptionService pushSubscriptionService;
    
    private PushSubscriptionRequest validRequest;
    private PushSubscription existingSubscription;
    
    @BeforeEach
    void setUp() {
        validRequest = new PushSubscriptionRequest();
        validRequest.setEndpoint("https://fcm.googleapis.com/fcm/send/test-endpoint");
        validRequest.setP256dhKey("dGVzdC1wMjU2ZGgtcHVibGljLWtleQ==");
        validRequest.setAuthKey("dGVzdC1hdXRoLXNlY3JldA==");
        validRequest.setUserId(1L);
        
        existingSubscription = PushSubscription.builder()
            .id(1L)
            .endpoint(validRequest.getEndpoint())
            .p256dhKey("old-key")
            .authKey("old-auth")
            .userId(1L)
            .active(true)
            .build();
    }
    
    @Test
    void testCreateOrUpdateSubscription_NewSubscription_Success() {
        // Arrange
        when(pushSubscriptionRepository.findByEndpoint(validRequest.getEndpoint()))
            .thenReturn(Optional.empty());
        when(pushSubscriptionRepository.save(any(PushSubscription.class)))
            .thenAnswer(invocation -> {
                PushSubscription subscription = invocation.getArgument(0);
                subscription.setId(1L);
                return subscription;
            });
        
        // Act
        PushSubscription result = pushSubscriptionService.createOrUpdateSubscription(validRequest, "test-user-agent");
        
        // Assert
        assertNotNull(result);
        assertEquals(validRequest.getEndpoint(), result.getEndpoint());
        assertEquals(validRequest.getP256dhKey(), result.getP256dhKey());
        assertEquals(validRequest.getAuthKey(), result.getAuthKey());
        assertTrue(result.getActive());
        
        verify(pushSubscriptionRepository).findByEndpoint(validRequest.getEndpoint());
        verify(pushSubscriptionRepository).save(any(PushSubscription.class));
    }
    
    @Test
    void testCreateOrUpdateSubscription_ExistingSubscription_Success() {
        // Arrange
        when(pushSubscriptionRepository.findByEndpoint(validRequest.getEndpoint()))
            .thenReturn(Optional.of(existingSubscription));
        when(pushSubscriptionRepository.save(any(PushSubscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        PushSubscription result = pushSubscriptionService.createOrUpdateSubscription(validRequest, "test-user-agent");
        
        // Assert
        assertNotNull(result);
        assertEquals(validRequest.getEndpoint(), result.getEndpoint());
        assertEquals(validRequest.getP256dhKey(), result.getP256dhKey());
        assertEquals(validRequest.getAuthKey(), result.getAuthKey());
        assertTrue(result.getActive());
        
        verify(pushSubscriptionRepository).findByEndpoint(validRequest.getEndpoint());
        verify(pushSubscriptionRepository).save(existingSubscription);
    }
    
    @Test
    void testCreateOrUpdateSubscription_ConstraintViolation_HandledSuccessfully() {
        // Arrange - simulate race condition
        when(pushSubscriptionRepository.findByEndpoint(validRequest.getEndpoint()))
            .thenReturn(Optional.empty()) // First call returns empty (no existing subscription)
            .thenReturn(Optional.of(existingSubscription)); // Second call returns existing (after constraint violation)
        
        when(pushSubscriptionRepository.save(any(PushSubscription.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'endpoint'")) // First save fails
            .thenAnswer(invocation -> invocation.getArgument(0)); // Second save succeeds
        
        // Act
        PushSubscription result = pushSubscriptionService.createOrUpdateSubscription(validRequest, "test-user-agent");
        
        // Assert
        assertNotNull(result);
        assertEquals(validRequest.getEndpoint(), result.getEndpoint());
        assertEquals(validRequest.getP256dhKey(), result.getP256dhKey());
        assertEquals(validRequest.getAuthKey(), result.getAuthKey());
        assertTrue(result.getActive());
        
        verify(pushSubscriptionRepository, times(2)).findByEndpoint(validRequest.getEndpoint());
        verify(pushSubscriptionRepository, times(2)).save(any(PushSubscription.class));
    }
    
    @Test
    void testCreateOrUpdateSubscription_ConstraintViolation_NoExistingSubscription_ThrowsException() {
        // Arrange - simulate constraint violation but no existing subscription found
        when(pushSubscriptionRepository.findByEndpoint(validRequest.getEndpoint()))
            .thenReturn(Optional.empty()); // Both calls return empty
        
        when(pushSubscriptionRepository.save(any(PushSubscription.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'endpoint'"));
        
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            pushSubscriptionService.createOrUpdateSubscription(validRequest, "test-user-agent");
        });
        
        verify(pushSubscriptionRepository, times(2)).findByEndpoint(validRequest.getEndpoint());
        verify(pushSubscriptionRepository, times(1)).save(any(PushSubscription.class));
    }
}