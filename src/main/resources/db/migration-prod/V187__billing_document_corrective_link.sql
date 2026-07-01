-- ============================================================
-- V187: Lien document-correctif (avoirs et factures rectificatives)
-- ============================================================
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS original_document_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS original_document_type   VARCHAR(40),
    ADD COLUMN IF NOT EXISTS credit_note_reason        TEXT;

CREATE INDEX IF NOT EXISTS idx_bd_original_document
    ON billing_document(original_document_number)
    WHERE original_document_number IS NOT NULL;
