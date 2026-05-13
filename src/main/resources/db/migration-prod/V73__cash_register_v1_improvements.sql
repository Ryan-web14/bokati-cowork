CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE cash_register
    ADD COLUMN IF NOT EXISTS business_entity_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS device_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS cash_control_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS max_cash_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE cash_session
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS expected_closing_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS counted_closing_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS variance_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS variance_reason TEXT,
    ADD COLUMN IF NOT EXISTS closing_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;

ALTER TABLE cash_movement
    ADD COLUMN IF NOT EXISTS reason TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_session_register_open
    ON cash_session(cash_register_id)
    WHERE status = 'OPEN';

CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_session_cashier_open
    ON cash_session(opened_by)
    WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS idx_cash_register_active_location
    ON cash_register(active, location_code);

CREATE INDEX IF NOT EXISTS idx_cash_register_business
    ON cash_register(business_entity_code);

CREATE INDEX IF NOT EXISTS idx_cash_session_status_opened
    ON cash_session(status, opened_at DESC);

CREATE INDEX IF NOT EXISTS idx_cash_movement_type_created
    ON cash_movement(movement_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cash_register_code_trgm
    ON cash_register USING gin (register_code gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_register_name_trgm
    ON cash_register USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_register_location_trgm
    ON cash_register USING gin (location_code gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_session_number_trgm
    ON cash_session USING gin (session_number gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_session_opened_by_trgm
    ON cash_session USING gin (opened_by gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_movement_number_trgm
    ON cash_movement USING gin (movement_number gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_movement_reference_trgm
    ON cash_movement USING gin (reference_code gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_movement_created_by_trgm
    ON cash_movement USING gin (created_by gin_trgm_ops);
