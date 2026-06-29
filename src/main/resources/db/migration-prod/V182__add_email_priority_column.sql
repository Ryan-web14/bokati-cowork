-- Add priority column to notification_message for tracking and monitoring
ALTER TABLE notification_message
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'NORMAL';

CREATE INDEX IF NOT EXISTS idx_notification_message_priority
    ON notification_message(priority, status);

-- Add priority column to email_delivery_log
ALTER TABLE email_delivery_log
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'NORMAL';

CREATE INDEX IF NOT EXISTS idx_email_delivery_log_priority
    ON email_delivery_log(priority, status);
