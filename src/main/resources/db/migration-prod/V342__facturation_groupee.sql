-- Lot C13 (facturation, complements) : facturation groupee periodique. Un lancement rassemble les
-- consommations en attente (elements facturables) d'une periode, client par client, en apercu ;
-- apres exclusions eventuelles, il emet une facture par client. Une consommation facturee porte
-- sa facture et ne revient jamais dans un autre lancement.

CREATE TABLE IF NOT EXISTS billing_batch_run
(
    id           BIGINT PRIMARY KEY,
    run_number   VARCHAR(40)  NOT NULL UNIQUE,
    period_from  DATE         NOT NULL,
    period_to    DATE         NOT NULL,
    title        VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_by   VARCHAR(180),
    issued_by    VARCHAR(180),
    issued_at    TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_batch_run_status CHECK (status IN ('PREVIEW', 'ISSUED', 'CANCELLED')),
    CONSTRAINT ck_billing_batch_run_period CHECK (period_to >= period_from)
);

CREATE TABLE IF NOT EXISTS billing_batch_run_item
(
    id               BIGINT PRIMARY KEY,
    run_id           BIGINT         NOT NULL REFERENCES billing_batch_run (id),
    customer_type    VARCHAR(60)    NOT NULL,
    customer_code    VARCHAR(120)   NOT NULL,
    customer_name    VARCHAR(255),
    billing_company  VARCHAR(120),
    billable_numbers TEXT           NOT NULL,
    item_count       INTEGER        NOT NULL,
    total_amount     NUMERIC(19, 4) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    excluded         BOOLEAN        NOT NULL DEFAULT FALSE,
    invoice_number   VARCHAR(80),
    error            VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS ix_billing_batch_run_item_run ON billing_batch_run_item (run_id);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3480001, 'billing_batch_run', 'Lancement de facturation groupée', 'Numero des lancements de facturation groupee', 'LFG', NULL,
       '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'billing_batch_run');
