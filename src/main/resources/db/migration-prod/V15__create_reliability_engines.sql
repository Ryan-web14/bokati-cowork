CREATE TABLE IF NOT EXISTS idempotency_record (
    id BIGINT PRIMARY KEY,
    operation VARCHAR(150) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_body TEXT,
    error_body TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_operation_key UNIQUE (operation, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_status ON idempotency_record(status);

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT PRIMARY KEY,
    event_type VARCHAR(150) NOT NULL,
    aggregate_type VARCHAR(150) NOT NULL,
    aggregate_id VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL,
    last_error TEXT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_available_at
    ON outbox_event(status, available_at);
