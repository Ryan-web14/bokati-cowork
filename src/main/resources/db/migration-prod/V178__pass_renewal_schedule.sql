CREATE TABLE IF NOT EXISTS pass_renewal_schedule (
    id                   BIGINT          PRIMARY KEY,
    pass_id              BIGINT          NOT NULL UNIQUE REFERENCES subscription_pass(id),
    duration             INTEGER         NOT NULL,
    duration_unit        VARCHAR(20)     NOT NULL,
    next_renewal_date    TIMESTAMPTZ     NOT NULL,
    current_period_start TIMESTAMPTZ,
    current_period_end   TIMESTAMPTZ,
    status               VARCHAR(40)     NOT NULL DEFAULT 'ACTIVE',
    retry_count          INTEGER         NOT NULL DEFAULT 0,
    last_attempt_at      TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_renewal_pass   ON pass_renewal_schedule(pass_id);
CREATE INDEX IF NOT EXISTS idx_pass_renewal_status ON pass_renewal_schedule(status, next_renewal_date);
