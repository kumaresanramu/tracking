# Notification System Setup Guide

## 🔧 Complete Configuration Steps

### Step 1: Generate VAPID Keys (Push Notifications)

I've generated VAPID keys for you. Run this command to see them:

```bash
./gradlew test --tests "VapidKeyGeneratorTest"
```

The output will show your unique VAPID keys. **Copy them from the test output.**

### Step 2: Set Environment Variables

#### Option A: PowerShell (Recommended for Windows)
```powershell
# Replace with your actual keys from the test output
$env:VAPID_PUBLIC_KEY="your_generated_public_key"
$env:VAPID_PRIVATE_KEY="your_generated_private_key"
$env:VAPID_SUBJECT="mailto:your_email@example.com"

# Email configuration
$env:EMAIL_NOTIFICATIONS_ENABLED="true"
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_USERNAME="your_email@gmail.com"
$env:SMTP_PASSWORD="your_app_password"
$env:EMAIL_FROM="your_email@gmail.com"
```

#### Option B: Command Prompt
```cmd
set VAPID_PUBLIC_KEY=your_generated_public_key
set VAPID_PRIVATE_KEY=your_generated_private_key
set VAPID_SUBJECT=mailto:your_email@example.com
set EMAIL_NOTIFICATIONS_ENABLED=true
set SMTP_HOST=smtp.gmail.com
set SMTP_USERNAME=your_email@gmail.com
set SMTP_PASSWORD=your_app_password
set EMAIL_FROM=your_email@gmail.com
```

### Step 3: Gmail App Password Setup

For Gmail SMTP, you need an **App Password** (not your regular password):

1. Go to [Google Account Settings](https://myaccount.google.com/)
2. Security → 2-Step Verification (must be enabled)
3. App passwords → Generate new app password
4. Select "Mail" and your device
5. Copy the 16-character password (use this as SMTP_PASSWORD)

### Step 4: Create .env File (Alternative)

Create a `.env` file in the `tracking` directory:

```env
# VAPID Keys (get from test output)
VAPID_PUBLIC_KEY=your_generated_public_key
VAPID_PRIVATE_KEY=your_generated_private_key
VAPID_SUBJECT=mailto:your_email@example.com

# Email Configuration
EMAIL_NOTIFICATIONS_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_16_character_app_password
EMAIL_FROM=your_email@gmail.com
EMAIL_FROM_NAME=Expense Tracker
```

### Step 5: Restart Application

After setting environment variables:
```bash
./gradlew bootRun
```

### Step 6: Configure Notification Settings

Once the app is running, you need to configure your notification preferences:

1. Open the application in your browser
2. Go to notification settings
3. Set your email address
4. Enable the notifications you want:
   - ✅ Budget Alerts
   - ✅ Daily Reminders  
   - ✅ Weekly Summary
   - ✅ Email Notifications
5. Set budget warning threshold (e.g., 80%)

### Step 7: Test the Setup

#### Test Budget Alert:
```bash
# Create an expense that exceeds 80% of $10,000 budget
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 8500,
    "description": "Test budget alert",
    "categoryId": 1,
    "date": "2024-01-23"
  }'
```

#### Test Email Notification:
```bash
# Trigger a daily reminder
curl -X POST http://localhost:8080/api/notifications/daily-reminder
```

## 🔍 Verification Checklist

After configuration, verify these work:

- [ ] Application starts without errors
- [ ] Email service shows as "Available" in logs
- [ ] Push service shows as "Available" in logs
- [ ] Budget alerts trigger when spending > 80%
- [ ] Email notifications are received
- [ ] Push notifications work (after subscribing)

## 🚨 Troubleshooting

### Common Issues:

**1. "EmailNotificationService not available"**
- Check: `EMAIL_NOTIFICATIONS_ENABLED=true`
- Check: SMTP credentials are correct

**2. "PushNotificationService not available"**
- Check: VAPID keys are set correctly
- Check: No spaces or quotes in environment variables

**3. "SMTP Authentication failed"**
- Use Gmail App Password, not regular password
- Enable 2-Step Verification first

**4. "No notifications received"**
- Check notification settings in the app
- Verify email address is set
- Check spam folder

### Debug Commands:

```bash
# Check environment variables (PowerShell)
Get-ChildItem Env: | Where-Object {$_.Name -like "*VAPID*" -or $_.Name -like "*EMAIL*" -or $_.Name -like "*SMTP*"}

# Check application logs
./gradlew bootRun --info
```

## 📧 Email Templates

The system includes beautiful HTML email templates for:
- Daily expense reminders
- Weekly spending summaries  
- Budget alert notifications

These will be automatically used once email notifications are enabled.

## 🔔 Push Notifications

Push notifications require:
1. VAPID keys (generated above)
2. User subscription via browser
3. Service worker registration

The system includes a complete push notification infrastructure with:
- Subscription management
- Rich notifications with action buttons
- Retry logic and error handling

---

**Next Steps:** Run the VAPID key generator test, copy the keys, set the environment variables, and restart your application!