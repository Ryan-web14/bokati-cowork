ALTER TABLE notification_message
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_notification_message_recipient_unread
    ON notification_message (recipient_email, read_at)
    WHERE read_at IS NULL;
