-- Lot B11 (facturation) : affaires. Une affaire regroupe une vente a un client, du premier devis au
-- dernier paiement : devis et versions, proformas, acomptes, factures, avoirs, paiements.

CREATE TABLE IF NOT EXISTS billing_deal
(
    id                  BIGINT PRIMARY KEY,
    code                VARCHAR(40)    NOT NULL UNIQUE,
    customer_type       VARCHAR(30),
    customer_code       VARCHAR(80),
    customer_name       VARCHAR(255)   NOT NULL,
    title               VARCHAR(500)   NOT NULL,
    owner               VARCHAR(180),
    owner_name          VARCHAR(255),
    expected_amount     NUMERIC(19, 4),
    currency            VARCHAR(3),
    probability         INTEGER,
    stage               VARCHAR(20)    NOT NULL,
    lost_reason         VARCHAR(1000),
    expected_close_date DATE,
    closed_at           TIMESTAMPTZ,
    source              VARCHAR(20)    NOT NULL,
    crm_opportunity_ref VARCHAR(60),
    analytic_section    VARCHAR(60),
    notes               TEXT,
    created_by          VARCHAR(180),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_deal_stage CHECK (stage IN ('PROSPECTING', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST')),
    CONSTRAINT ck_billing_deal_source CHECK (source IN ('MANUAL', 'CRM', 'AUTO')),
    CONSTRAINT ck_billing_deal_probability CHECK (probability IS NULL OR probability BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS ix_billing_deal_customer ON billing_deal (customer_type, customer_code);
CREATE INDEX IF NOT EXISTS ix_billing_deal_stage ON billing_deal (stage);

ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS deal_code VARCHAR(40);
CREATE INDEX IF NOT EXISTS ix_billing_document_deal ON billing_document (deal_code);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3270003, 'billing_deal', 'Affaire', 'Numero des affaires commerciales', 'AFF', NULL, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1,
       'YEARLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'billing_deal');
