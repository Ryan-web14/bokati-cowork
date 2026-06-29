CREATE INDEX IF NOT EXISTS idx_notification_message_channel_recipient_unread
    ON notification_message (channel, recipient_email)
    WHERE read_at IS NULL;
