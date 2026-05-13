CREATE TABLE IF NOT EXISTS booking (
    id BIGINT PRIMARY KEY,
    booking_number VARCHAR(80) NOT NULL UNIQUE,
    resource_id BIGINT NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(60),
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    booking_unit VARCHAR(40) NOT NULL,
    payment_mode VARCHAR(40) NOT NULL,
    subscription_number VARCHAR(80),
    pass_number VARCHAR(80),
    entitlement_code VARCHAR(100),
    unit_price NUMERIC(19, 4),
    subtotal_amount NUMERIC(19, 4),
    total_amount NUMERIC(19, 4),
    currency VARCHAR(10),
    notes TEXT,
    cancellation_reason TEXT,
    metadata_json JSONB,
    confirmed_at TIMESTAMP,
    started_event_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_booking_resource FOREIGN KEY (resource_id) REFERENCES resource(id),
    CONSTRAINT ck_booking_time_range CHECK (ended_at > started_at),
    CONSTRAINT ck_booking_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_booking_number ON booking(booking_number);
CREATE INDEX IF NOT EXISTS idx_booking_resource_time ON booking(resource_id, started_at, ended_at);
CREATE INDEX IF NOT EXISTS idx_booking_owner ON booking(owner_type, owner_code);
CREATE INDEX IF NOT EXISTS idx_booking_status ON booking(status);
CREATE INDEX IF NOT EXISTS idx_booking_subscription_number ON booking(subscription_number);
CREATE INDEX IF NOT EXISTS idx_booking_pass_number ON booking(pass_number);
CREATE INDEX IF NOT EXISTS idx_booking_entitlement_code ON booking(entitlement_code);

CREATE TABLE IF NOT EXISTS booking_line (
    id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    line_type VARCHAR(40) NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    unit_price NUMERIC(19, 4),
    amount NUMERIC(19, 4),
    currency VARCHAR(10),
    entitlement_code VARCHAR(100),
    billable_number VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_line_booking FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX IF NOT EXISTS idx_booking_line_booking ON booking_line(booking_id);

CREATE TABLE IF NOT EXISTS booking_participant (
    id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    member_code VARCHAR(120),
    name VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_participant_booking FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX IF NOT EXISTS idx_booking_participant_booking ON booking_participant(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_participant_member ON booking_participant(member_code);

CREATE TABLE IF NOT EXISTS booking_status_history (
    id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    changed_by VARCHAR(120),
    reason TEXT,
    changed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_status_history_booking FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX IF NOT EXISTS idx_booking_status_history_booking ON booking_status_history(booking_id);

CREATE TABLE IF NOT EXISTS booking_event (
    id BIGINT PRIMARY KEY,
    event_number VARCHAR(80) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    payload_json JSONB,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_event_booking FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX IF NOT EXISTS idx_booking_event_number ON booking_event(event_number);
CREATE INDEX IF NOT EXISTS idx_booking_event_booking ON booking_event(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_event_type ON booking_event(event_type);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_booking_number_trgm
ON booking USING gin (normalize_text(booking_number) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_booking_owner_code_trgm
ON booking USING gin (normalize_text(owner_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_booking_contact_name_trgm
ON booking USING gin (normalize_text(contact_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_booking_contact_email_trgm
ON booking USING gin (normalize_text(contact_email) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_booking(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    booking_number TEXT,
    resource_code TEXT,
    owner_code TEXT,
    status TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        b.id,
        b.booking_number,
        r.code AS resource_code,
        b.owner_code,
        b.status,
        GREATEST(
            similarity(normalize_text(b.booking_number), normalize_text(p_query)),
            similarity(normalize_text(b.owner_code), normalize_text(p_query)),
            similarity(normalize_text(COALESCE(b.contact_name, '')), normalize_text(p_query)),
            similarity(normalize_text(COALESCE(b.contact_email, '')), normalize_text(p_query)),
            similarity(normalize_text(r.code), normalize_text(p_query)),
            similarity(normalize_text(r.name), normalize_text(p_query))
        ) AS score
    FROM booking b
    JOIN resource r ON r.id = b.resource_id
    WHERE b.deleted = false
      AND (
            normalize_text(b.booking_number) % normalize_text(p_query)
         OR normalize_text(b.owner_code) % normalize_text(p_query)
         OR normalize_text(COALESCE(b.contact_name, '')) % normalize_text(p_query)
         OR normalize_text(COALESCE(b.contact_email, '')) % normalize_text(p_query)
         OR normalize_text(r.code) % normalize_text(p_query)
         OR normalize_text(r.name) % normalize_text(p_query)
      )
    ORDER BY score DESC
    LIMIT 20;
$$;
