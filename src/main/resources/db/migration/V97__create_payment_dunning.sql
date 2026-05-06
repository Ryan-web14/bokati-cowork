CREATE TABLE IF NOT EXISTS payment_dunning_attempt (
    id                     BIGSERIAL PRIMARY KEY,
    payment_intent_id      BIGINT       NOT NULL,
    subscription_number    VARCHAR(120),
    attempt_number         INT          NOT NULL,
    scheduled_at           TIMESTAMPTZ  NOT NULL,
    executed_at            TIMESTAMPTZ,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    new_transaction_number VARCHAR(120),
    failure_reason         TEXT,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dunning_status_scheduled
    ON payment_dunning_attempt (status, scheduled_at);

CREATE INDEX IF NOT EXISTS idx_dunning_intent
    ON payment_dunning_attempt (payment_intent_id);