-- Advance payments on billing documents
CREATE TABLE IF NOT EXISTS billing_document_advance (
    id                   BIGINT        NOT NULL PRIMARY KEY,
    document_id          BIGINT        NOT NULL,
    advance_type         VARCHAR(20)   NOT NULL,
    advance_value        NUMERIC(19,4) NOT NULL,
    computed_amount      NUMERIC(19,4) NOT NULL,
    included_line_orders TEXT,
    excluded_line_orders TEXT,
    payment_reference    VARCHAR(100),
    reference_label      VARCHAR(200),
    due_date             DATE,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    paid_at              TIMESTAMPTZ,
    notes                TEXT,
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_advance_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_advance_document ON billing_document_advance(document_id);

-- Early payment discounts on billing documents
CREATE TABLE IF NOT EXISTS billing_document_early_payment_discount (
    id              BIGINT        NOT NULL PRIMARY KEY,
    document_id     BIGINT        NOT NULL,
    discount_rate   NUMERIC(9,4)  NOT NULL,
    if_paid_before  DATE          NOT NULL,
    computed_amount NUMERIC(19,4) NOT NULL,
    label           VARCHAR(200),
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_early_discount_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_early_discount_document ON billing_document_early_payment_discount(document_id);

-- Electronic signatures on billing documents
CREATE TABLE IF NOT EXISTS billing_document_signature (
    id                     BIGINT      NOT NULL PRIMARY KEY,
    document_id            BIGINT      NOT NULL,
    signer_name            VARCHAR(200),
    signer_email           VARCHAR(200),
    signature_token        VARCHAR(64) NOT NULL UNIQUE,
    signature_image_base64 TEXT,
    ip_address             VARCHAR(60),
    user_agent             TEXT,
    custom_message         TEXT,
    signed_at              TIMESTAMPTZ,
    expires_at             TIMESTAMPTZ NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_signature_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_signature_document ON billing_document_signature(document_id);
CREATE INDEX IF NOT EXISTS idx_signature_token    ON billing_document_signature(signature_token);