-- Lot C7 (facturation, complements) : depots de garantie (cautions). Une caution n'est ni taxable ni
-- un produit : elle est portee au compte 165 a la facture, puis restituee, retenue (avec motif) ou
-- imputee sur une facture. Registre des cautions et de leurs mouvements, comptabilises.

ALTER TABLE service_catalog_item ADD COLUMN IF NOT EXISTS security_deposit BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS billing_security_deposit
(
    id                  BIGINT PRIMARY KEY,
    deposit_number      VARCHAR(40)    NOT NULL UNIQUE,
    document_number     VARCHAR(80)    NOT NULL UNIQUE,
    subscription_number VARCHAR(80),
    customer_type       VARCHAR(60),
    customer_code       VARCHAR(120),
    customer_name       VARCHAR(255),
    amount              NUMERIC(19, 4) NOT NULL,
    released_amount     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    retained_amount     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    applied_amount      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency            VARCHAR(3)     NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    wallet_entry_number VARCHAR(60),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_security_deposit_status CHECK (status IN ('HELD', 'PARTIALLY_SETTLED', 'SETTLED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS ix_billing_security_deposit_customer ON billing_security_deposit (customer_type, customer_code);
CREATE INDEX IF NOT EXISTS ix_billing_security_deposit_subscription ON billing_security_deposit (subscription_number);

CREATE TABLE IF NOT EXISTS billing_security_deposit_movement
(
    id                     BIGINT PRIMARY KEY,
    movement_number        VARCHAR(40)    NOT NULL UNIQUE,
    deposit_id             BIGINT         NOT NULL REFERENCES billing_security_deposit (id),
    kind                   VARCHAR(20)    NOT NULL,
    amount                 NUMERIC(19, 4) NOT NULL,
    reason                 VARCHAR(500),
    target_document_number VARCHAR(80),
    movement_date          DATE           NOT NULL,
    wallet_settled         BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by             VARCHAR(180),
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_security_deposit_movement_kind CHECK (kind IN ('RELEASE', 'RETAIN', 'APPLY')),
    CONSTRAINT ck_billing_security_deposit_movement_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS ix_billing_security_deposit_movement_deposit ON billing_security_deposit_movement (deposit_id);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT v.id, v.code, v.name, v.description, v.prefix, NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', TRUE, FALSE, now(), now()
FROM (VALUES (3430001, 'billing_security_deposit', 'Dépôt de garantie', 'Numero des depots de garantie', 'CAU'),
             (3430002, 'billing_security_deposit_movement', 'Mouvement de dépôt de garantie', 'Numero des mouvements de caution', 'MVC'))
         AS v(id, code, name, description, prefix)
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = v.code);

-- Mouvement de caution : D 165 ; C client (restitution, imputation) ou C produit (retenue).
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT 3430010, 'SALES_SECURITY_DEPOSIT_MOVEMENT', 1, 'SECURITY_DEPOSIT_MOVEMENT', 'Restitution, retenue ou imputation d''une caution',
       'D 165 ; C client (restitution, imputation sur une facture) ou C produit (retenue pour degats ou impayes).', 'SALES', 10, NULL,
       '{journalCode}', '{kindLabel} de la caution {depositNumber}',
       '[{"side":"DEBIT","account":"{securityDepositAccount}","amount":"amount","reference":"{depositNumber}"},{"side":"CREDIT","account":"{counterAccount}","amount":"amount","thirdParty":"{?thirdParty}","reference":"{reference}"}]',
       FALSE, '{"depositNumber":"CAU-1","kindLabel":"Restitution","journalCode":"VT","securityDepositAccount":"165","counterAccount":"411","thirdParty":"CUS-1","reference":"FAC-1","amount":200000}',
       DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = 'SALES_SECURITY_DEPOSIT_MOVEMENT');
