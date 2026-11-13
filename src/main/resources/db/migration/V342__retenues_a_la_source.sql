-- Lot C6 (facturation, complements) : retenues a la source sur les reglements des clients
-- (administrations, grandes entreprises). La retenue solde la facture a hauteur de son montant ;
-- l'attestation de retenue est suivie. Ecriture : D compte de la retenue / C 411 du client.

CREATE TABLE IF NOT EXISTS billing_withholding_type
(
    id            BIGINT PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    label         VARCHAR(150) NOT NULL,
    rate          NUMERIC(9, 4),
    base          VARCHAR(10)  NOT NULL,
    account       VARCHAR(20)  NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_withholding_type_base CHECK (base IN ('TTC', 'HT', 'TVA'))
);

INSERT INTO billing_withholding_type (id, code, label, rate, base, account, display_order)
SELECT v.id, v.code, v.label, v.rate, v.base, v.account, v.ord
FROM (VALUES (3420001, 'TVA_RETENUE', 'Retenue de TVA', 100, 'TVA', '4449', 1),
             (3420002, 'PRECOMPTE', 'Précompte', NULL, 'HT', '4492', 2)) AS v(id, code, label, rate, base, account, ord)
WHERE NOT EXISTS (SELECT 1 FROM billing_withholding_type t WHERE t.code = v.code);

CREATE TABLE IF NOT EXISTS billing_withholding
(
    id                    BIGINT PRIMARY KEY,
    withholding_number    VARCHAR(40)    NOT NULL UNIQUE,
    document_number       VARCHAR(80)    NOT NULL,
    type_code             VARCHAR(40)    NOT NULL,
    account               VARCHAR(20)    NOT NULL,
    base_amount           NUMERIC(19, 4),
    rate                  NUMERIC(9, 4),
    amount                NUMERIC(19, 4) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    customer_type         VARCHAR(60),
    customer_code         VARCHAR(120),
    certificate_reference VARCHAR(120),
    certificate_document  VARCHAR(120),
    certificate_at        TIMESTAMPTZ,
    status                VARCHAR(20)    NOT NULL,
    withheld_on           DATE           NOT NULL,
    recorded_by           VARCHAR(180),
    cancelled_at          TIMESTAMPTZ,
    cancelled_by          VARCHAR(180),
    cancel_reason         VARCHAR(500),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_withholding_status CHECK (status IN ('RECORDED', 'CANCELLED')),
    CONSTRAINT ck_billing_withholding_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS ix_billing_withholding_document ON billing_withholding (document_number);
CREATE INDEX IF NOT EXISTS ix_billing_withholding_customer ON billing_withholding (customer_type, customer_code);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3420010, 'billing_withholding', 'Retenue à la source', 'Numero des retenues a la source', 'RAS', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1,
       'MONTHLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'billing_withholding');

-- Retenue : la creance du client est soldee par la creance sur l'Etat.
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT 3420020, 'SALES_WITHHOLDING', 1, 'WITHHOLDING_RECORDED', 'Retenue a la source sur un reglement client',
       'D compte de la retenue (Etat) ; C client (lettre avec la facture).', 'SALES', 10, NULL, '{journalCode}',
       'Retenue {withholdingNumber} sur {documentNumber}',
       '[{"side":"DEBIT","account":"{withholdingAccount}","amount":"amount","reference":"{withholdingNumber}"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}"}]',
       FALSE, '{"documentNumber":"FAC-1","withholdingNumber":"RAS-1","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","withholdingAccount":"4449","amount":180000}',
       DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = 'SALES_WITHHOLDING');
