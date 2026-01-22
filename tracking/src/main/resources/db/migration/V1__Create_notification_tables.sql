-- Create notification tables for the expense tracking application

-- Notification types enum (represented as table for better flexibility)
CREATE TABLE notification_types (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default notification types
INSERT INTO notification_types (id, name, description) VALUES
(1, 'DAILY_EXPENSE_REMINDER', 'Daily reminder to log expenses'),
(2, 'RECURRING_BILL_ALERT', 'Alert for upcoming recurring bills'),
(3, 'BUDGET_THRESHOLD_WARNING', 'Warning when approaching budget limit'),
(4, 'BUDGET_EXCEEDED_ALERT', 'Alert when budget is exceeded'),
(5, 'WEEKLY_SUMMARY', 'Weekly expense summary'),
(6, 'MONTHLY_REPORT', 'Monthly expense report'),
(7, 'STREAK_REWARD', 'Reward for expense tracking streak'),
(8, 'BADGE_EARNED', 'Achievement badge earned'),
(9, 'OVERDUE_EXPENSE', 'Overdue expense reminder'),
(10, 'CUSTOM_REMINDER', 'Custom user-defined reminder');

-- Notification channels enum
CREATE TABLE notification_channels (
    id BIGINT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default notification channels
INSERT INTO notification_channels (id, name, description) VALUES
(1, 'IN_APP', 'In-application notifications'),
(2, 'EMAIL', 'Email notifications'),
(3, 'PUSH', 'Push notifications'),
(4, 'SMS', 'SMS notifications');

-- Notification frequency enum
CREATE TABLE notification_frequencies (
    id BIGINT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default notification frequencies
INSERT INTO notification_frequencies (id, name, description) VALUES
(1, 'ONCE', 'One-time notification'),
(2, 'DAILY', 'Daily recurring notification'),
(3, 'WEEKLY', 'Weekly recurring notification'),
(4, 'MONTHLY', 'Monthly recurring notification'),
(5, 'YEARLY', 'Yearly recurring notification');

-- Main notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    type_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    frequency_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    icon VARCHAR(50),
    priority INTEGER DEFAULT 1 CHECK (priority BETWEEN 1 AND 3),
    is_read BOOLEAN DEFAULT FALSE,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (type_id) REFERENCES notification_types(id),
    FOREIGN KEY (channel_id) REFERENCES notification_channels(id),
    FOREIGN KEY (frequency_id) REFERENCES notification_frequencies(id),
    
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_type_id (type_id),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_scheduled_at (scheduled_at),
    INDEX idx_notifications_created_at (created_at)
);

-- Notification settings table
CREATE TABLE notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    
    -- Daily reminders
    enable_daily_reminder BOOLEAN DEFAULT TRUE,
    daily_reminder_time TIME DEFAULT '20:00:00',
    
    -- Budget alerts
    enable_budget_alerts BOOLEAN DEFAULT TRUE,
    budget_warning_threshold INTEGER DEFAULT 80 CHECK (budget_warning_threshold BETWEEN 50 AND 100),
    
    -- Weekly summary
    enable_weekly_summary BOOLEAN DEFAULT TRUE,
    weekly_summary_time TIME DEFAULT '09:00:00',
    weekly_summary_day INTEGER DEFAULT 1 CHECK (weekly_summary_day BETWEEN 1 AND 7), -- 1=Monday, 7=Sunday
    
    -- Gamification
    enable_streak_rewards BOOLEAN DEFAULT TRUE,
    enable_badges BOOLEAN DEFAULT TRUE,
    
    -- Quiet hours
    quiet_hours_start TIME DEFAULT '22:00:00',
    quiet_hours_end TIME DEFAULT '08:00:00',
    
    -- Email notifications
    enable_email_notifications BOOLEAN DEFAULT FALSE,
    email_address VARCHAR(255),
    
    -- Push notifications
    enable_push_notifications BOOLEAN DEFAULT TRUE,
    
    -- SMS notifications
    enable_sms_notifications BOOLEAN DEFAULT FALSE,
    phone_number VARCHAR(20),
    
    -- Timezone
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_notification_settings_user_id (user_id)
);

-- Notification delivery log (for tracking sent notifications)
CREATE TABLE notification_delivery_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, SENT, DELIVERED, FAILED, BOUNCED
    attempt_count INTEGER DEFAULT 1,
    error_message TEXT,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES notification_channels(id),
    
    INDEX idx_delivery_log_notification_id (notification_id),
    INDEX idx_delivery_log_status (status),
    INDEX idx_delivery_log_created_at (created_at)
);

-- Notification templates (for reusable notification content)
CREATE TABLE notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    message_template TEXT NOT NULL,
    icon VARCHAR(50),
    default_priority INTEGER DEFAULT 1 CHECK (default_priority BETWEEN 1 AND 3),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (type_id) REFERENCES notification_types(id),
    
    INDEX idx_notification_templates_type_id (type_id),
    INDEX idx_notification_templates_is_active (is_active)
);

-- Insert default notification templates
INSERT INTO notification_templates (type_id, name, title_template, message_template, icon, default_priority) VALUES
(1, 'Daily Expense Reminder', 'Time to log your expenses! 📝', 'Don''t forget to record your expenses for today. Keep your financial tracking on point!', '📝', 1),
(2, 'Recurring Bill Alert', 'Upcoming Bill: {billName} 💰', 'Your {billName} bill of ${amount} is due on {dueDate}. Don''t forget to pay it!', '💰', 2),
(3, 'Budget Warning', 'Budget Alert: {percentage}% Used ⚠️', 'You''ve used {percentage}% of your monthly budget. You have ${remaining} left to spend.', '⚠️', 2),
(4, 'Budget Exceeded', 'Budget Exceeded! 🚨', 'You''ve exceeded your monthly budget by ${amount}. Time to review your spending!', '🚨', 3),
(5, 'Weekly Summary', 'Your Weekly Expense Summary 📊', 'This week you spent ${totalSpent}. Your top category was {topCategory} with ${categoryAmount}.', '📊', 1),
(6, 'Monthly Report', 'Monthly Expense Report 📈', 'Your monthly expense report is ready! Total spent: ${totalSpent} across {transactionCount} transactions.', '📈', 1),
(7, 'Streak Reward', 'Congratulations! {days} Day Streak! 🎉', 'Amazing! You''ve been tracking expenses for {days} days straight. Keep up the great work!', '🎉', 1),
(8, 'Badge Earned', 'New Badge Earned: {badgeName} 🏆', 'Congratulations! You''ve earned the {badgeName} badge for {achievement}.', '🏆', 1),
(9, 'Overdue Expense', 'Overdue: {expenseName} ⏰', 'Your {expenseName} expense was due on {dueDate}. Please update your records.', '⏰', 2),
(10, 'Custom Reminder', '{title}', '{message}', '🔔', 1);

-- User notification preferences (many-to-many relationship)
CREATE TABLE user_notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (type_id) REFERENCES notification_types(id),
    FOREIGN KEY (channel_id) REFERENCES notification_channels(id),
    
    UNIQUE KEY unique_user_type_channel (user_id, type_id, channel_id),
    INDEX idx_user_notification_prefs_user_id (user_id),
    INDEX idx_user_notification_prefs_type_id (type_id)
);