ALTER TABLE ticket_attachment
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(150),
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS internal BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS uploaded_by BIGINT,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_ticket_attachment_ticket_active ON ticket_attachment (ticket_id, active);
