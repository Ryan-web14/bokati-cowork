-- Bloc 5 : champs formulaire sur billing_document
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS customer_reference  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS po_number           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS project_code        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS salesperson_code    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS delivery_address_json JSONB,
    ADD COLUMN IF NOT EXISTS language            VARCHAR(5)   DEFAULT 'fr',
    ADD COLUMN IF NOT EXISTS exchange_rate       NUMERIC(19,6);

-- Bloc 5 : champs formulaire sur billing_document_line
ALTER TABLE billing_document_line
    ADD COLUMN IF NOT EXISTS external_reference  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS notes               TEXT,
    ADD COLUMN IF NOT EXISTS optional            BOOLEAN      NOT NULL DEFAULT FALSE;
