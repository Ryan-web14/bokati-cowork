ALTER TABLE cash_movement
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS document_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS flow_category VARCHAR(120),
    ADD COLUMN IF NOT EXISTS counterparty_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS counterparty_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS counterparty_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS metadata_json JSONB;

CREATE INDEX IF NOT EXISTS idx_cash_movement_document
    ON cash_movement(document_type, document_number);

CREATE INDEX IF NOT EXISTS idx_cash_movement_flow_category
    ON cash_movement(flow_category);

CREATE INDEX IF NOT EXISTS idx_cash_movement_counterparty
    ON cash_movement(counterparty_type, counterparty_code);

CREATE INDEX IF NOT EXISTS idx_cash_movement_document_number_trgm
    ON cash_movement USING gin (document_number gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_movement_flow_category_trgm
    ON cash_movement USING gin (flow_category gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_cash_movement_counterparty_name_trgm
    ON cash_movement USING gin (counterparty_name gin_trgm_ops);
