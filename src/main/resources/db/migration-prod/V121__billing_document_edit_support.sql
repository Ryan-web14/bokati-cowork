-- Champ notes internes (non affiché sur PDF client)
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS internal_notes TEXT;

-- Historique des modifications post-création
CREATE TABLE IF NOT EXISTS billing_document_edit_history (
    id            BIGINT       NOT NULL PRIMARY KEY,
    document_id   BIGINT       NOT NULL,
    edit_type     VARCHAR(50)  NOT NULL,
    changed_by    VARCHAR(120) NOT NULL,
    changed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    snapshot_json JSONB,
    CONSTRAINT fk_edit_history_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_edit_history_document   ON billing_document_edit_history(document_id);
CREATE INDEX IF NOT EXISTS idx_edit_history_changed_at ON billing_document_edit_history(changed_at);