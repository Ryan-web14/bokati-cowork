-- Phase 1 SEFC : verrouillage + numéro fiscal + date fiscale
ALTER TABLE billing_document
    ADD COLUMN locked         BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN validated_at   TIMESTAMPTZ,
    ADD COLUMN fiscal_date    DATE,
    ADD COLUMN fiscal_number  VARCHAR(80);

CREATE INDEX idx_billing_document_fiscal ON billing_document (document_type, locked, validated_at)
    WHERE locked = TRUE;

CREATE UNIQUE INDEX idx_billing_document_fiscal_number ON billing_document (fiscal_number)
    WHERE fiscal_number IS NOT NULL;
