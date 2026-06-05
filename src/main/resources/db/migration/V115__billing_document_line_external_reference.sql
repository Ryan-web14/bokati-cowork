ALTER TABLE billing_document_line
    ADD COLUMN IF NOT EXISTS unit               VARCHAR(30),
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS notes              TEXT,
    ADD COLUMN IF NOT EXISTS optional           BOOLEAN NOT NULL DEFAULT FALSE;