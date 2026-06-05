ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS customer_reference  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS po_number           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS project_code        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS salesperson_code    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS delivery_address_json JSONB,
    ADD COLUMN IF NOT EXISTS language            VARCHAR(5) NOT NULL DEFAULT 'fr',
    ADD COLUMN IF NOT EXISTS exchange_rate        NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS payment_reference   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_instructions TEXT,
    ADD COLUMN IF NOT EXISTS bank_details_json   JSONB;