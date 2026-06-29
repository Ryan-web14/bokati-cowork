CREATE TABLE IF NOT EXISTS contract_signing_token (
    id          BIGSERIAL PRIMARY KEY,
    token       UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    contract_id BIGINT NOT NULL REFERENCES contract_record(id),
    contract_code VARCHAR(120) NOT NULL,
    signer_email VARCHAR(300) NOT NULL,
    signer_name  VARCHAR(300),
    expires_at   TIMESTAMPTZ NOT NULL,
    signed_at    TIMESTAMPTZ,
    ip_address   VARCHAR(45),
    user_agent   TEXT,
    consent_text TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_signing_token_token ON contract_signing_token(token);
CREATE INDEX IF NOT EXISTS idx_signing_token_contract ON contract_signing_token(contract_id);
