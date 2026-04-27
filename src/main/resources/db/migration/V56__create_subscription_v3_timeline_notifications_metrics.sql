CREATE TABLE IF NOT EXISTS subscription_timeline_event (
    id BIGINT PRIMARY KEY,
    event_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT,
    owner_type VARCHAR(60),
    owner_code VARCHAR(120),
    event_type VARCHAR(80) NOT NULL,
    source_type VARCHAR(80),
    source_id VARCHAR(120),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    payload_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_timeline_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE INDEX IF NOT EXISTS idx_subscription_timeline_subscription ON subscription_timeline_event(subscription_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_subscription_timeline_owner ON subscription_timeline_event(owner_type, owner_code, occurred_at);
CREATE INDEX IF NOT EXISTS idx_subscription_timeline_type ON subscription_timeline_event(event_type, occurred_at);
CREATE INDEX IF NOT EXISTS idx_subscription_timeline_source ON subscription_timeline_event(source_type, source_id);

CREATE TABLE IF NOT EXISTS subscription_notification_outbox (
    id BIGINT PRIMARY KEY,
    notification_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT,
    owner_type VARCHAR(60),
    owner_code VARCHAR(120),
    notification_type VARCHAR(80) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body TEXT,
    payload_json JSONB,
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    dispatched_at TIMESTAMPTZ,
    status VARCHAR(40) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_notification_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_subscription_notification_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_notification_status_schedule ON subscription_notification_outbox(status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_subscription_notification_subscription ON subscription_notification_outbox(subscription_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_notification_owner ON subscription_notification_outbox(owner_type, owner_code, status);
