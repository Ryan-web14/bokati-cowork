CREATE TABLE IF NOT EXISTS billing_document_signature (
    id                      BIGINT        NOT NULL PRIMARY KEY,
    document_id             BIGINT        NOT NULL,
    signer_name             VARCHAR(200),
    signer_email            VARCHAR(200),
    signature_token         VARCHAR(64)   NOT NULL UNIQUE,
    signature_image_base64  TEXT,
    ip_address              VARCHAR(60),
    user_agent              TEXT,
    custom_message          TEXT,
    signed_at               TIMESTAMPTZ,
    expires_at              TIMESTAMPTZ   NOT NULL,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_signature_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_signature_document  ON billing_document_signature(document_id);
CREATE INDEX IF NOT EXISTS idx_signature_token     ON billing_document_signature(signature_token);
