-- Add per-field requirement flags to document_type
-- Note: auto_approve and auto_approve_after_days were already added in V85
ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS requires_issue_date      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_document_number BOOLEAN NOT NULL DEFAULT FALSE;
