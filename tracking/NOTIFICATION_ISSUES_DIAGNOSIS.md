# Notification System Issues Diagnosis

## Issues Identified

Based on the test results and configuration analysis, here are the issues preventing notifications from working:

### 1. **Email Notifications Disabled**
**Problem**: `email.notifications.enabled=false` (default value)
**Impact**: EmailNotificationService is not available, so no email notifications are sent
**Solution**: Set environment variable `EMAIL_NOTIFICATIONS_ENABLED=true`

### 2. **Missing VAPID Keys**
**Problem**: VAPID keys are not configured (empty environment variables)
**Impact**: PushNotificationService is not available, so no push notifications are sent
**Solution**: Generate and set VAPID keys:
```bash
# Generate VAPID keys (you can use online generators or the VapidKeyGenerator utility)
VAPID_PUBLIC_KEY=your_generated_public_key
VAPID_PRIVATE_KEY=your_generated_private_key
```

### 3. **Missing SMTP Configuration**
**Problem**: SMTP credentials are not configured
**Impact**: Even if email notifications are enabled, emails cannot be sent
**Solution**: Configure SMTP settings:
```bash
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

### 4. **No Default Notification Settings**
**Problem**: Users don't have notification settings configured by default
**Impact**: Even if services are available, notifications won't be sent without user preferences
**Solution**: Create default notification settings or ensure users configure them

## Current Configuration Status

✗ **Email Notifications**: DISABLED (email.notifications.enabled=false)
✗ **Push Notifications**: DISABLED (missing VAPID keys)
✗ **SMTP Configuration**: MISSING (no credentials set)
✓ **Notification Service**: Available (core service works)
✓ **Database**: Working (notifications can be stored)

## How to Fix

### Step 1: Enable Email Notifications
Create a `.env` file or set environment variables:
```bash
EMAIL_NOTIFICATIONS_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
EMAIL_FROM=your_email@gmail.com
```

### Step 2: Generate and Configure VAPID Keys
```bash
# Use the VapidKeyGenerator utility or online generator
VAPID_PUBLIC_KEY=your_generated_public_key
VAPID_PRIVATE_KEY=your_generated_private_key
VAPID_SUBJECT=mailto:your_email@gmail.com
```

### Step 3: Configure User Notification Settings
Users need to:
1. Set their email address in notification settings
2. Enable the types of notifications they want
3. Set budget warning thresholds

### Step 4: Test the Configuration
After setting the environment variables, restart the application and:
1. Create notification settings with email enabled
2. Add an expense that exceeds 80% of budget
3. Check if notifications are created and sent

## Why You're Not Getting Notifications

1. **Budget Alerts**: Not working because both email and push services are disabled
2. **Email Notifications**: Not working because email.notifications.enabled=false
3. **Push Notifications**: Not working because VAPID keys are missing
4. **Daily/Weekly Reminders**: Same issues - services are disabled

## Testing Commands

After fixing the configuration, you can test with:
```bash
# Test email notifications
curl -X POST http://localhost:8080/api/notifications/test-email

# Test push notifications (after subscribing)
curl -X POST http://localhost:8080/api/push-subscriptions/test

# Create a large expense to trigger budget alert
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -d '{"amount": 8500, "description": "Test budget alert", "categoryId": 1}'
```

## Next Steps

1. Set the required environment variables
2. Restart the application
3. Configure notification settings in the UI
4. Test by creating expenses that trigger budget alerts
5. Verify notifications are received via email and/or push