ALTER TABLE cash_register
    ADD COLUMN IF NOT EXISTS manager_email VARCHAR(180),
    ADD COLUMN IF NOT EXISTS last_anomaly_alert_sent_at TIMESTAMPTZ;
