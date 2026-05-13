ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;
