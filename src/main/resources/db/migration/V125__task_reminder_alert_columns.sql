ALTER TABLE task_item
    ADD COLUMN IF NOT EXISTS due_soon_alert_sent_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS overdue_alert_sent_at TIMESTAMP;
