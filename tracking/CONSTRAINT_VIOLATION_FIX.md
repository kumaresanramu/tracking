# Database Constraint Violation Fix

## Issue Description
The application was experiencing `DataIntegrityViolationException` errors when creating push subscriptions due to unique constraint violations on the `endpoint` column in the `push_subscriptions` table.

## Root Cause
The `PushSubscriptionService.createOrUpdateSubscription()` method had a race condition where:
1. It would check if a subscription exists for an endpoint
2. If not found, it would create a new subscription
3. Between steps 1 and 2, another request could create a subscription with the same endpoint
4. This caused a unique constraint violation when saving the new subscription

## Solution
Enhanced the `createOrUpdateSubscription()` method to handle `DataIntegrityViolationException`:

1. **Primary Logic**: Check for existing subscription and update if found, create new if not found
2. **Fallback Logic**: If constraint violation occurs during save:
   - Catch the `DataIntegrityViolationException`
   - Re-query for existing subscription (in case it was created by another thread)
   - If found, update the existing subscription
   - If still not found, re-throw the original exception

## Code Changes

### PushSubscriptionService.java
- Added try-catch block around the save operation
- Added fallback logic to handle race conditions
- Improved logging for better debugging

### Test Coverage
- Added `PushSubscriptionServiceTest.java` with comprehensive test cases
- Tests cover normal operation, existing subscription updates, and constraint violation handling

## Benefits
1. **Eliminates Race Conditions**: Handles concurrent subscription creation attempts
2. **Improved Reliability**: No more constraint violation errors for duplicate endpoints
3. **Better User Experience**: Subscription requests succeed even under high concurrency
4. **Maintains Data Integrity**: Ensures only one subscription per endpoint

## Additional Fix: Settings Button Removal
Removed the settings button from the Reminders page as requested:
- Location: `tracking/src/main/resources/static/index.html`
- Removed the "⚙️ Settings" button from the reminders page header
- Users can still access notification settings through the main Settings page