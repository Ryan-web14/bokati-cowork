ALTER TABLE business_entity
    ADD COLUMN IF NOT EXISTS entity_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS legal_form VARCHAR(40),
    ADD COLUMN IF NOT EXISTS niu_number VARCHAR(18),
    ADD COLUMN IF NOT EXISTS rccm_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tax_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS activity VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_id BIGINT,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS base_currency_id BIGINT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE business_entity
    ALTER COLUMN deleted SET DEFAULT FALSE;

UPDATE business_entity
SET deleted = FALSE
WHERE deleted IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_business_entity_address'
    ) THEN
        ALTER TABLE business_entity
            ADD CONSTRAINT fk_business_entity_address
            FOREIGN KEY (address_id) REFERENCES address(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_business_entity_currency'
    ) THEN
        ALTER TABLE business_entity
            ADD CONSTRAINT fk_business_entity_currency
            FOREIGN KEY (base_currency_id) REFERENCES currency(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_business_entity_code ON business_entity(entity_code);
CREATE INDEX IF NOT EXISTS idx_business_entity_name ON business_entity(name);
CREATE INDEX IF NOT EXISTS idx_business_entity_niu_number ON business_entity(niu_number);
CREATE INDEX IF NOT EXISTS idx_business_entity_rccm_number ON business_entity(rccm_number);
CREATE INDEX IF NOT EXISTS idx_business_entity_deleted ON business_entity(deleted);
