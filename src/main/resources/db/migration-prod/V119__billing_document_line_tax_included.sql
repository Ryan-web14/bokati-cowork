ALTER TABLE billing_document_line
    ADD COLUMN IF NOT EXISTS tax_included boolean NOT NULL DEFAULT false;
