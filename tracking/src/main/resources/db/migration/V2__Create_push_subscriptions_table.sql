-- Create push_subscriptions table for PWA push notification support

CREATE TABLE push_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint VARCHAR(500) NOT NULL UNIQUE,
    p256dh_key VARCHAR(100) NOT NULL,
    auth_key VARCHAR(50) NOT NULL,
    user_agent VARCHAR(500),
    user_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    failure_count INTEGER DEFAULT 0,
    last_failure_at TIMESTAMP NULL,
    
    INDEX idx_push_subscriptions_endpoint (endpoint),
    INDEX idx_push_subscriptions_user_id (user_id),
    INDEX idx_push_subscriptions_active (active),
    INDEX idx_push_subscriptions_created_at (created_at)
);

-- Add comments for documentation
ALTER TABLE push_subscriptions 
COMMENT = 'Stores push notification subscriptions for PWA functionality';

ALTER TABLE push_subscriptions 
MODIFY COLUMN endpoint VARCHAR(500) NOT NULL UNIQUE 
COMMENT 'Push service endpoint URL provided by browser';

ALTER TABLE push_subscriptions 
MODIFY COLUMN p256dh_key VARCHAR(100) NOT NULL 
COMMENT 'Base64-encoded P256DH public key for message encryption';

ALTER TABLE push_subscriptions 
MODIFY COLUMN auth_key VARCHAR(50) NOT NULL 
COMMENT 'Base64-encoded authentication secret for message authentication';

ALTER TABLE push_subscriptions 
MODIFY COLUMN user_agent VARCHAR(500) 
COMMENT 'Browser user agent string for debugging and analytics';

ALTER TABLE push_subscriptions 
MODIFY COLUMN user_id BIGINT 
COMMENT 'Associated user ID (foreign key to users table when implemented)';

ALTER TABLE push_subscriptions 
MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE 
COMMENT 'Whether this subscription is currently active';

ALTER TABLE push_subscriptions 
MODIFY COLUMN last_used_at TIMESTAMP NULL 
COMMENT 'Timestamp when this subscription was last used to send a notification';

ALTER TABLE push_subscriptions 
MODIFY COLUMN failure_count INTEGER DEFAULT 0 
COMMENT 'Number of failed delivery attempts for cleanup purposes';

ALTER TABLE push_subscriptions 
MODIFY COLUMN last_failure_at TIMESTAMP NULL 
COMMENT 'Timestamp of the last failed delivery attempt';