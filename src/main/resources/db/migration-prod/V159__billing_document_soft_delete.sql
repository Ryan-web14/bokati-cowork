-- Suppression logique des documents fiscaux
ALTER TABLE billing_document
    ADD COLUMN deleted_at     TIMESTAMPTZ,
    ADD COLUMN deleted_by     VARCHAR(120),
    ADD COLUMN delete_reason  TEXT;

CREATE INDEX idx_billing_document_deleted ON billing_document (deleted_at)
    WHERE deleted_at IS NOT NULL;
