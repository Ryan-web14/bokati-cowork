DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'member_settings'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'user_settings'
    ) THEN
        EXECUTE 'ALTER TABLE member_settings RENAME TO user_settings';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS user_settings (
    id BIGINT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    langage VARCHAR(50) NOT NULL DEFAULT 'fr',
    time_zone VARCHAR(100) NOT NULL DEFAULT 'Africa/Lagos',
    show_name_on_displays BOOLEAN NOT NULL DEFAULT FALSE,
    show_email BOOLEAN NOT NULL DEFAULT FALSE,
    show_phone BOOLEAN NOT NULL DEFAULT FALSE,
    billing_entity_type VARCHAR(50) NOT NULL DEFAULT 'PERSONAL',
    billing_company_name VARCHAR(150),
    billing_email VARCHAR(255),
    billing_address VARCHAR(255),
    wallet_auto_pop_up BOOLEAN NOT NULL DEFAULT FALSE,
    wallet_auto_topup_threshold INTEGER,
    wallet_auto_topup_amount INTEGER,
    wallet_spending_daily_limit INTEGER,
    wallet_spending_weekly_limit INTEGER,
    wallet_receipt_preference VARCHAR(50) NOT NULL DEFAULT 'EVERY_TRANSACTION',
    CONSTRAINT fk_user_settings_member FOREIGN KEY (member_id) REFERENCES member(id)
);

ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS langage VARCHAR(50) NOT NULL DEFAULT 'fr',
    ADD COLUMN IF NOT EXISTS time_zone VARCHAR(100) NOT NULL DEFAULT 'Africa/Lagos',
    ADD COLUMN IF NOT EXISTS show_name_on_displays BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS show_email BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS show_phone BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS billing_entity_type VARCHAR(50) NOT NULL DEFAULT 'PERSONAL',
    ADD COLUMN IF NOT EXISTS billing_company_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS billing_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS billing_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS wallet_auto_pop_up BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS wallet_auto_topup_threshold INTEGER,
    ADD COLUMN IF NOT EXISTS wallet_auto_topup_amount INTEGER,
    ADD COLUMN IF NOT EXISTS wallet_spending_daily_limit INTEGER,
    ADD COLUMN IF NOT EXISTS wallet_spending_weekly_limit INTEGER,
    ADD COLUMN IF NOT EXISTS wallet_receipt_preference VARCHAR(50) NOT NULL DEFAULT 'EVERY_TRANSACTION';

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_settings_member_id
    ON user_settings(member_id);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    notification_channel VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    preference_name VARCHAR(150),
    preference_description VARCHAR(255),
    CONSTRAINT fk_member_notification FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT uk_notification_preferences_member_event_channel UNIQUE (member_id, event_type, notification_channel)
);

ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50) NOT NULL DEFAULT 'BOOKING_CONFIRMED',
    ADD COLUMN IF NOT EXISTS notification_channel VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS preference_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS preference_description VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_member_id
    ON notification_preferences(member_id);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_event_channel
    ON notification_preferences(event_type, notification_channel);

CREATE TABLE IF NOT EXISTS setting_audit_logs (
    id BIGINT PRIMARY KEY,
    setting_key VARCHAR(150),
    old_value TEXT,
    new_value TEXT,
    changed_by BIGINT,
    changed_at TIMESTAMP WITH TIME ZONE,
    reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_setting_audit_logs_changed_at
    ON setting_audit_logs(changed_at);
