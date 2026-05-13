CREATE TABLE IF NOT EXISTS billing_document (
    id BIGINT PRIMARY KEY,
    document_number VARCHAR(100) NOT NULL UNIQUE,
    document_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    customer_type VARCHAR(60) NOT NULL,
    customer_code VARCHAR(120) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    customer_phone VARCHAR(60),
    billing_address_json JSONB,
    source_type VARCHAR(80),
    source_code VARCHAR(120),
    title VARCHAR(255),
    description TEXT,
    terms TEXT,
    currency VARCHAR(3) NOT NULL,
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    additional_cent_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    balance_due NUMERIC(19,4) NOT NULL DEFAULT 0,
    issue_date DATE NOT NULL,
    due_date DATE,
    issued_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_billing_document_amounts CHECK (
        subtotal_amount >= 0 AND discount_amount >= 0 AND taxable_amount >= 0
        AND vat_amount >= 0 AND additional_cent_amount >= 0 AND tax_amount >= 0
        AND total_amount >= 0 AND paid_amount >= 0 AND balance_due >= 0
    )
);

CREATE TABLE IF NOT EXISTS billing_document_line (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    line_order INTEGER NOT NULL,
    line_type VARCHAR(40) NOT NULL,
    item_code VARCHAR(120),
    description TEXT NOT NULL,
    detailed_description TEXT,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    discount_rate NUMERIC(9,4),
    discount_amount NUMERIC(19,4) NOT NULL,
    taxable BOOLEAN NOT NULL,
    vat_rate NUMERIC(9,4),
    additional_cent_rate NUMERIC(9,4),
    subtotal_amount NUMERIC(19,4) NOT NULL,
    taxable_amount NUMERIC(19,4) NOT NULL,
    vat_amount NUMERIC(19,4) NOT NULL,
    additional_cent_amount NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    source_type VARCHAR(80),
    source_code VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_billing_line_document FOREIGN KEY (document_id) REFERENCES billing_document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS billing_document_discount (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    discount_code VARCHAR(120),
    description VARCHAR(255) NOT NULL,
    discount_type VARCHAR(40) NOT NULL,
    value NUMERIC(19,4) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    CONSTRAINT fk_billing_discount_document FOREIGN KEY (document_id) REFERENCES billing_document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS billing_document_tax (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    tax_code VARCHAR(120) NOT NULL,
    tax_name VARCHAR(255) NOT NULL,
    rate NUMERIC(9,4) NOT NULL,
    taxable_amount NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL,
    CONSTRAINT fk_billing_tax_document FOREIGN KEY (document_id) REFERENCES billing_document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS billing_document_clause (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    clause_code VARCHAR(120),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_billing_clause_document FOREIGN KEY (document_id) REFERENCES billing_document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS billing_tax_rule (
    id BIGINT PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    country_code VARCHAR(3),
    tax_type VARCHAR(60) NOT NULL,
    rate NUMERIC(9,4) NOT NULL,
    applies_on VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE,
    valid_until DATE
);

CREATE TABLE IF NOT EXISTS payment_intent (
    id BIGINT PRIMARY KEY,
    intent_number VARCHAR(100) NOT NULL UNIQUE,
    customer_type VARCHAR(60) NOT NULL,
    customer_code VARCHAR(120) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    purpose VARCHAR(120),
    source_type VARCHAR(80),
    source_code VARCHAR(120),
    idempotency_key VARCHAR(180) UNIQUE,
    expires_at TIMESTAMPTZ,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_payment_intent_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGINT PRIMARY KEY,
    transaction_number VARCHAR(100) NOT NULL UNIQUE,
    payment_intent_id BIGINT NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
    provider VARCHAR(80),
    provider_reference VARCHAR(180),
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    paid_at TIMESTAMPTZ,
    received_by VARCHAR(120),
    failure_reason TEXT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_transaction_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intent(id),
    CONSTRAINT ck_payment_transaction_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS payment_allocation (
    id BIGINT PRIMARY KEY,
    payment_transaction_id BIGINT NOT NULL,
    billing_document_number VARCHAR(120) NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,
    allocated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_allocation_transaction FOREIGN KEY (payment_transaction_id) REFERENCES payment_transaction(id),
    CONSTRAINT ck_payment_allocation_amount CHECK (allocated_amount > 0)
);

CREATE TABLE IF NOT EXISTS wallet_account (
    id BIGINT PRIMARY KEY,
    wallet_number VARCHAR(100) NOT NULL UNIQUE,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    available_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    ledger_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    held_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wallet_owner_currency UNIQUE (owner_type, owner_code, currency),
    CONSTRAINT ck_wallet_balances CHECK (available_balance >= 0 AND ledger_balance >= 0 AND held_balance >= 0)
);

CREATE TABLE IF NOT EXISTS wallet_ledger_entry (
    id BIGINT PRIMARY KEY,
    entry_number VARCHAR(100) NOT NULL UNIQUE,
    wallet_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after NUMERIC(19,4) NOT NULL,
    entry_type VARCHAR(40) NOT NULL,
    source_type VARCHAR(80),
    source_code VARCHAR(120),
    reference VARCHAR(180),
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_wallet_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_wallet_ledger_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_billing_document_number ON billing_document(document_number);
CREATE INDEX IF NOT EXISTS idx_billing_document_customer ON billing_document(customer_type, customer_code);
CREATE INDEX IF NOT EXISTS idx_billing_document_status ON billing_document(document_type, status);
CREATE INDEX IF NOT EXISTS idx_billing_document_source ON billing_document(source_type, source_code);
CREATE INDEX IF NOT EXISTS idx_billing_line_document ON billing_document_line(document_id, line_order);
CREATE INDEX IF NOT EXISTS idx_billing_tax_rule_active ON billing_tax_rule(code, active);

CREATE INDEX IF NOT EXISTS idx_payment_intent_number ON payment_intent(intent_number);
CREATE INDEX IF NOT EXISTS idx_payment_intent_customer ON payment_intent(customer_type, customer_code);
CREATE INDEX IF NOT EXISTS idx_payment_intent_source ON payment_intent(source_type, source_code);
CREATE INDEX IF NOT EXISTS idx_payment_intent_status ON payment_intent(status);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_intent ON payment_transaction(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payment_allocation_document ON payment_allocation(billing_document_number);

CREATE INDEX IF NOT EXISTS idx_wallet_account_number ON wallet_account(wallet_number);
CREATE INDEX IF NOT EXISTS idx_wallet_account_owner ON wallet_account(owner_type, owner_code, currency);
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_wallet ON wallet_ledger_entry(wallet_id, created_at DESC);

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680001, 'billing_quote', 'Devis', 'Sequence des devis', 'QUO', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_quote');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680002, 'billing_invoice', 'Facture', 'Sequence des factures', 'INV', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_invoice');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680003, 'billing_proforma', 'Facture proforma', 'Sequence des proformas', 'PROF', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_proforma');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680004, 'billing_credit_note', 'Avoir', 'Sequence des avoirs', 'CRN', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_credit_note');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680005, 'billing_debit_note', 'Note debit', 'Sequence des notes de debit', 'DBN', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_debit_note');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680006, 'billing_customer', 'Client facture manuel', 'Sequence des clients snapshot manuels billing', 'BCL', null, '{PREFIX}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_customer');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680007, 'payment_intent', 'Payment intent', 'Sequence des intentions de paiement', 'PIN', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'payment_intent');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680008, 'payment_transaction', 'Transaction paiement', 'Sequence des transactions paiement', 'PAY', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'payment_transaction');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680009, 'wallet_account', 'Wallet', 'Sequence des portefeuilles internes', 'WAL', null, '{PREFIX}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_account');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 680010, 'wallet_entry', 'Mouvement wallet', 'Sequence du ledger wallet', 'WLE', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 8, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_entry');

INSERT INTO billing_tax_rule
(id, code, name, country_code, tax_type, rate, applies_on, active, valid_from, valid_until)
SELECT 680101, 'TVA_CG_18', 'TVA Congo 18%', 'CG', 'VAT', 18, 'TAXABLE_AMOUNT', TRUE, CURRENT_DATE, NULL
WHERE NOT EXISTS (SELECT 1 FROM billing_tax_rule WHERE code = 'TVA_CG_18');

INSERT INTO billing_tax_rule
(id, code, name, country_code, tax_type, rate, applies_on, active, valid_from, valid_until)
SELECT 680102, 'CAC_CG_ON_VAT_5', 'Centimes additionnels Congo 5% sur TVA', 'CG', 'ADDITIONAL_CENT', 5, 'VAT_AMOUNT', TRUE, CURRENT_DATE, NULL
WHERE NOT EXISTS (SELECT 1 FROM billing_tax_rule WHERE code = 'CAC_CG_ON_VAT_5');
