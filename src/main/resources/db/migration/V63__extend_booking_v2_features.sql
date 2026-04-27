ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180),
    ADD COLUMN IF NOT EXISTS hold_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS recurrence_group_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS billable_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS checked_out_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_booking_idempotency_not_null
ON booking(idempotency_key)
WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_booking_hold_number ON booking(hold_number);
CREATE INDEX IF NOT EXISTS idx_booking_recurrence_group ON booking(recurrence_group_number);
CREATE INDEX IF NOT EXISTS idx_booking_billable_number ON booking(billable_number);

CREATE TABLE IF NOT EXISTS booking_hold (
    id BIGINT PRIMARY KEY,
    hold_number VARCHAR(80) NOT NULL UNIQUE,
    resource_id BIGINT NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    quantity INTEGER NOT NULL,
    idempotency_key VARCHAR(180),
    status VARCHAR(40) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_hold_resource FOREIGN KEY (resource_id) REFERENCES resource(id),
    CONSTRAINT ck_booking_hold_time_range CHECK (ended_at > started_at),
    CONSTRAINT ck_booking_hold_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_booking_hold_number_table ON booking_hold(hold_number);
CREATE INDEX IF NOT EXISTS idx_booking_hold_resource_time ON booking_hold(resource_id, started_at, ended_at);
CREATE INDEX IF NOT EXISTS idx_booking_hold_status_expires ON booking_hold(status, expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_booking_hold_idempotency_not_null
ON booking_hold(idempotency_key)
WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS booking_recurrence_group (
    id BIGINT PRIMARY KEY,
    group_number VARCHAR(80) NOT NULL UNIQUE,
    resource_code VARCHAR(100) NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    frequency VARCHAR(40) NOT NULL,
    interval_value INTEGER NOT NULL,
    occurrences INTEGER NOT NULL,
    first_start_at TIMESTAMP NOT NULL,
    first_end_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_booking_recurrence_occurrences CHECK (occurrences > 0),
    CONSTRAINT ck_booking_recurrence_interval CHECK (interval_value > 0)
);

CREATE INDEX IF NOT EXISTS idx_booking_recurrence_group_number
ON booking_recurrence_group(group_number);

CREATE TABLE IF NOT EXISTS booking_audience_policy (
    id BIGINT PRIMARY KEY,
    policy_number VARCHAR(80) NOT NULL UNIQUE,
    audience_type VARCHAR(60) NOT NULL,
    resource_type_code VARCHAR(100),
    resource_group_code VARCHAR(100),
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    max_active_bookings INTEGER,
    max_bookings_per_day INTEGER,
    max_bookings_per_week INTEGER,
    max_bookings_per_month INTEGER,
    max_no_shows_per_month INTEGER,
    min_booking_notice_minutes INTEGER,
    max_booking_duration_minutes INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_booking_audience_policy_audience
ON booking_audience_policy(audience_type);
CREATE INDEX IF NOT EXISTS idx_booking_audience_policy_resource_type
ON booking_audience_policy(resource_type_code);

CREATE TABLE IF NOT EXISTS booking_quota_override (
    id BIGINT PRIMARY KEY,
    override_number VARCHAR(80) NOT NULL UNIQUE,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    resource_code VARCHAR(100),
    extra_active_bookings INTEGER,
    extra_bookings_per_day INTEGER,
    extra_bookings_per_week INTEGER,
    extra_bookings_per_month INTEGER,
    reason TEXT,
    approved_by VARCHAR(120),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_booking_quota_override_owner
ON booking_quota_override(owner_type, owner_code);
CREATE INDEX IF NOT EXISTS idx_booking_quota_override_active
ON booking_quota_override(active, valid_from, valid_until);

CREATE TABLE IF NOT EXISTS booking_notification_outbox (
    id BIGINT PRIMARY KEY,
    notification_number VARCHAR(80) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT,
    scheduled_at TIMESTAMP NOT NULL,
    dispatched_at TIMESTAMP,
    status VARCHAR(40) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_notification_booking FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX IF NOT EXISTS idx_booking_notification_number
ON booking_notification_outbox(notification_number);
CREATE INDEX IF NOT EXISTS idx_booking_notification_due
ON booking_notification_outbox(status, scheduled_at);
