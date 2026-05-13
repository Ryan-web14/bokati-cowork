CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS wallet_hold (
    id BIGINT PRIMARY KEY,
    hold_number VARCHAR(100) NOT NULL UNIQUE,
    wallet_id BIGINT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    source_type VARCHAR(80),
    source_code VARCHAR(120),
    expires_at TIMESTAMPTZ,
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_wallet_hold_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_wallet_hold_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS cash_register (
    id BIGINT PRIMARY KEY,
    register_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    location_code VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cash_session (
    id BIGINT PRIMARY KEY,
    session_number VARCHAR(100) NOT NULL UNIQUE,
    cash_register_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    opened_by VARCHAR(120) NOT NULL,
    closed_by VARCHAR(120),
    opening_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    closing_amount NUMERIC(19,4),
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    CONSTRAINT fk_cash_session_register FOREIGN KEY (cash_register_id) REFERENCES cash_register(id),
    CONSTRAINT ck_cash_session_amounts CHECK (opening_amount >= 0 AND (closing_amount IS NULL OR closing_amount >= 0))
);

CREATE TABLE IF NOT EXISTS cash_movement (
    id BIGINT PRIMARY KEY,
    movement_number VARCHAR(100) NOT NULL UNIQUE,
    cash_session_id BIGINT NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_type VARCHAR(80),
    reference_code VARCHAR(120),
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cash_movement_session FOREIGN KEY (cash_session_id) REFERENCES cash_session(id),
    CONSTRAINT ck_cash_movement_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS payment_reconciliation_batch (
    id BIGINT PRIMARY KEY,
    batch_number VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS billing_clause_template (
    id BIGINT PRIMARY KEY,
    clause_code VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    document_type VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_hold_number ON wallet_hold(hold_number);
CREATE INDEX IF NOT EXISTS idx_wallet_hold_wallet_status ON wallet_hold(wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_wallet_hold_expiry ON wallet_hold(status, expires_at);

CREATE INDEX IF NOT EXISTS idx_cash_register_code ON cash_register(register_code);
CREATE INDEX IF NOT EXISTS idx_cash_session_number ON cash_session(session_number);
CREATE INDEX IF NOT EXISTS idx_cash_session_register_status ON cash_session(cash_register_id, status);
CREATE INDEX IF NOT EXISTS idx_cash_movement_session ON cash_movement(cash_session_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cash_movement_reference ON cash_movement(reference_type, reference_code);

CREATE INDEX IF NOT EXISTS idx_payment_reconciliation_batch_number ON payment_reconciliation_batch(batch_number);
CREATE INDEX IF NOT EXISTS idx_payment_reconciliation_provider_status ON payment_reconciliation_batch(provider, status);

CREATE INDEX IF NOT EXISTS idx_billing_document_number_trgm ON billing_document USING gin (document_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_billing_document_customer_name_trgm ON billing_document USING gin (customer_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_billing_document_customer_code_trgm ON billing_document USING gin (customer_code gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_billing_document_customer_email_trgm ON billing_document USING gin (customer_email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_billing_document_title_trgm ON billing_document USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_billing_document_source_code_trgm ON billing_document USING gin (source_code gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_billing_document_line_description_trgm ON billing_document_line USING gin (description gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_payment_intent_number_trgm ON payment_intent USING gin (intent_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payment_intent_customer_code_trgm ON payment_intent USING gin (customer_code gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payment_intent_purpose_trgm ON payment_intent USING gin (purpose gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payment_intent_source_code_trgm ON payment_intent USING gin (source_code gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payment_intent_idempotency_trgm ON payment_intent USING gin (idempotency_key gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_number_trgm ON payment_transaction USING gin (transaction_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_provider_reference_trgm ON payment_transaction USING gin (provider_reference gin_trgm_ops);

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 690001, 'wallet_hold', 'Reservation wallet', 'Sequence des holds wallet', 'WHL', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 8, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_hold');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 690002, 'cash_register', 'Caisse', 'Sequence des caisses', 'CSR', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'cash_register');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 690003, 'cash_session', 'Session caisse', 'Sequence des sessions caisse', 'CSS', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'cash_session');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 690004, 'cash_movement', 'Mouvement caisse', 'Sequence des mouvements caisse', 'CSM', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 8, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'cash_movement');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 690005, 'payment_reconciliation_batch', 'Lot rapprochement paiement', 'Sequence des lots de rapprochement paiement', 'REC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'payment_reconciliation_batch');

INSERT INTO billing_clause_template
(id, clause_code, title, body, document_type, active, display_order, created_at)
SELECT 690101, 'CG_PAYMENT_TERMS', 'Conditions de paiement', 'Paiement exigible a la date indiquee sur le document. Tout retard peut entrainer une suspension du service et des frais applicables.', NULL, TRUE, 10, NOW()
WHERE NOT EXISTS (SELECT 1 FROM billing_clause_template WHERE clause_code = 'CG_PAYMENT_TERMS');

INSERT INTO billing_clause_template
(id, clause_code, title, body, document_type, active, display_order, created_at)
SELECT 690102, 'CG_QUOTE_VALIDITY', 'Validite du devis', 'Le devis est valable uniquement jusqu a sa date d expiration. Les prix peuvent etre revises apres cette date.', 'QUOTE', TRUE, 20, NOW()
WHERE NOT EXISTS (SELECT 1 FROM billing_clause_template WHERE clause_code = 'CG_QUOTE_VALIDITY');

INSERT INTO billing_clause_template
(id, clause_code, title, body, document_type, active, display_order, created_at)
SELECT 690103, 'CG_NON_REFUNDABLE_PASS', 'Pass et abonnements non remboursables', 'Les pass et abonnements emis sont non remboursables, sauf obligation legale ou decision administrative explicite.', 'INVOICE', TRUE, 30, NOW()
WHERE NOT EXISTS (SELECT 1 FROM billing_clause_template WHERE clause_code = 'CG_NON_REFUNDABLE_PASS');

INSERT INTO billing_clause_template
(id, clause_code, title, body, document_type, active, display_order, created_at)
SELECT 690104, 'CG_TAX_NOTICE', 'Fiscalite Congo', 'Les montants taxables peuvent inclure la TVA Congo et les centimes additionnels selon les regles fiscales configurees.', NULL, TRUE, 40, NOW()
WHERE NOT EXISTS (SELECT 1 FROM billing_clause_template WHERE clause_code = 'CG_TAX_NOTICE');
