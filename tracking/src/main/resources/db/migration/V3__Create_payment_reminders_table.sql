-- Create payment reminders table
CREATE TABLE payment_reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    category_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_paid DATE,
    days_before INTEGER NOT NULL DEFAULT 3,
    preferred_notification_time TIME NOT NULL DEFAULT '09:00:00',
    enable_email_notification BOOLEAN NOT NULL DEFAULT TRUE,
    enable_push_notification BOOLEAN NOT NULL DEFAULT TRUE,
    custom_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_payment_reminders_due_date (due_date),
    INDEX idx_payment_reminders_active (active),
    INDEX idx_payment_reminders_category (category_id)
);

-- Create reminder preferences table
CREATE TABLE reminder_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reminder_id BIGINT NOT NULL,
    notification_days VARCHAR(20) NOT NULL DEFAULT 'WEEKDAYS',
    quiet_hours_start TIME DEFAULT '22:00:00',
    quiet_hours_end TIME DEFAULT '08:00:00',
    snooze_duration_minutes INTEGER DEFAULT 60,
    max_notifications_per_day INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (reminder_id) REFERENCES payment_reminders(id) ON DELETE CASCADE,
    UNIQUE KEY uk_reminder_preferences_reminder_id (reminder_id)
);

-- Insert sample payment reminders for testing
INSERT INTO payment_reminders (name, amount, due_date, frequency, days_before, custom_message) VALUES
('Electricity Bill', 150.00, '2026-02-15', 'MONTHLY', 3, 'Don''t forget to pay the electricity bill!'),
('Internet Bill', 80.00, '2026-02-01', 'MONTHLY', 2, 'Monthly internet payment due'),
('Car Insurance', 1200.00, '2026-06-01', 'YEARLY', 7, 'Annual car insurance renewal');