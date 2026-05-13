CREATE TABLE IF NOT EXISTS resource_policy (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    min_booking_duration_minutes INT NOT NULL DEFAULT 1,
    max_booking_duration_minutes INT NOT NULL DEFAULT 1,
    min_booking_notice_minutes INT NOT NULL DEFAULT 1,
    cancellation_notice_minutes INT NOT NULL DEFAULT 1,
    allow_cancellation BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

ALTER TABLE resource_policy
    ADD COLUMN IF NOT EXISTS code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS name VARCHAR(250),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS min_booking_duration_minutes INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS max_booking_duration_minutes INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS min_booking_notice_minutes INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS cancellation_notice_minutes INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS allow_cancellation BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE resource_policy
    ALTER COLUMN min_booking_duration_minutes SET DEFAULT 1,
    ALTER COLUMN max_booking_duration_minutes SET DEFAULT 1,
    ALTER COLUMN min_booking_notice_minutes SET DEFAULT 1,
    ALTER COLUMN cancellation_notice_minutes SET DEFAULT 1,
    ALTER COLUMN allow_cancellation SET DEFAULT FALSE,
    ALTER COLUMN active SET DEFAULT TRUE;

UPDATE resource_policy
SET allow_cancellation = FALSE
WHERE allow_cancellation IS NULL;

UPDATE resource_policy
SET active = TRUE
WHERE active IS NULL;

CREATE INDEX IF NOT EXISTS idx_resource_policy_code ON resource_policy(code);
