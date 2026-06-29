-- Phase 2 SEFC : signature HMAC-SHA256
ALTER TABLE billing_document
    ADD COLUMN fiscal_signature     VARCHAR(512),
    ADD COLUMN signature_algorithm  VARCHAR(40) DEFAULT 'HMAC-SHA256',
    ADD COLUMN signed_at            TIMESTAMPTZ;
