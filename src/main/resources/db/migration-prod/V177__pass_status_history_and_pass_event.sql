CREATE TABLE IF NOT EXISTS pass_status_history (
    id          BIGINT          PRIMARY KEY,
    pass_id     BIGINT          NOT NULL REFERENCES subscription_pass(id),
    from_status VARCHAR(40),
    to_status   VARCHAR(40)     NOT NULL,
    reason      TEXT,
    changed_by  VARCHAR(120),
    changed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_status_history_pass ON pass_status_history(pass_id);

CREATE TABLE IF NOT EXISTS pass_event (
    id           BIGINT          PRIMARY KEY,
    pass_id      BIGINT          NOT NULL REFERENCES subscription_pass(id),
    event_type   VARCHAR(80)     NOT NULL,
    payload_json JSONB,
    occurred_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_event_pass ON pass_event(pass_id);
CREATE INDEX IF NOT EXISTS idx_pass_event_type ON pass_event(event_type);
