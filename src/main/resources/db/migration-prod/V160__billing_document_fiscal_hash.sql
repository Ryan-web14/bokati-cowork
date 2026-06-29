-- Phase 2 SEFC : chaînage cryptographique SHA-256
ALTER TABLE billing_document
    ADD COLUMN previous_hash   VARCHAR(512),
    ADD COLUMN current_hash    VARCHAR(512),
    ADD COLUMN hash_algorithm  VARCHAR(40) DEFAULT 'SHA-256';

-- Index utilisé par findLastValidatedHash() (SELECT FOR UPDATE)
CREATE INDEX idx_billing_document_chain ON billing_document (document_type, validated_at DESC)
    WHERE locked = TRUE;
