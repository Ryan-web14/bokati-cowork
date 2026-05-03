CREATE TABLE IF NOT EXISTS pawapay_deposit (
    id BIGINT PRIMARY KEY,
    deposit_id VARCHAR(36) NOT NULL UNIQUE,
    payment_intent_id BIGINT NOT NULL,
    payment_transaction_id BIGINT NOT NULL,
    intent_number VARCHAR(100) NOT NULL,
    transaction_number VARCHAR(100) NOT NULL,
    customer_type VARCHAR(60) NOT NULL,
    customer_code VARCHAR(120) NOT NULL,
    payer_type VARCHAR(30) NOT NULL DEFAULT 'MMO',
    phone_number VARCHAR(40) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    client_reference_id VARCHAR(120) NOT NULL,
    customer_message VARCHAR(22) NOT NULL,
    callback_url TEXT,
    metadata_json JSONB,
    request_payload_json JSONB,
    provider_response_json JSONB,
    status VARCHAR(40) NOT NULL,
    provider_message TEXT,
    failure_reason TEXT,
    last_status_checked_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pawapay_deposit_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intent(id),
    CONSTRAINT fk_pawapay_deposit_transaction FOREIGN KEY (payment_transaction_id) REFERENCES payment_transaction(id),
    CONSTRAINT ck_pawapay_deposit_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_intent ON pawapay_deposit(intent_number);
CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_transaction ON pawapay_deposit(transaction_number);
CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_status ON pawapay_deposit(status);
CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_provider ON pawapay_deposit(provider);
