ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS check_in_token VARCHAR(120),
    ADD COLUMN IF NOT EXISTS virtual_meeting_url VARCHAR(500);

CREATE UNIQUE INDEX IF NOT EXISTS uk_booking_check_in_token
    ON booking(check_in_token)
    WHERE check_in_token IS NOT NULL;

ALTER TABLE booking_recurrence_group
    ADD COLUMN IF NOT EXISTS end_date DATE,
    ADD COLUMN IF NOT EXISTS excluded_dates_json JSONB;

CREATE TABLE IF NOT EXISTS booking_waitlist_entry (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(60),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    payment_mode VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    offered_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_waitlist_resource FOREIGN KEY (resource_id) REFERENCES resource(id)
);

CREATE INDEX IF NOT EXISTS idx_booking_waitlist_slot
    ON booking_waitlist_entry(resource_id, started_at, ended_at, status);

CREATE INDEX IF NOT EXISTS idx_booking_waitlist_owner
    ON booking_waitlist_entry(owner_type, owner_code);

CREATE INDEX IF NOT EXISTS idx_booking_started_at
    ON booking(started_at, ended_at);

ALTER TABLE payment_intent
    ADD COLUMN IF NOT EXISTS payment_link_token VARCHAR(120),
    ADD COLUMN IF NOT EXISTS payment_link_expires_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_intent_payment_link_token
    ON payment_intent(payment_link_token)
    WHERE payment_link_token IS NOT NULL;

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS pause_until DATE;

CREATE INDEX IF NOT EXISTS idx_billing_doc_due_date
    ON billing_document(due_date, status)
    WHERE status IN ('ISSUED', 'OVERDUE');

CREATE INDEX IF NOT EXISTS idx_entitlement_grant_owner_valid
    ON entitlement_grant(owner_type, owner_code, status, valid_until);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created
    ON outbox_event(status, created_at)
    WHERE status IN ('PENDING', 'FAILED');
