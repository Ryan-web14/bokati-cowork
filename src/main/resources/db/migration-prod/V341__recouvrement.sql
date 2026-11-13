-- Lot C12 (facturation, complements) : recouvrement. Promesse de paiement datee qui suspend les
-- relances jusqu'a sa date ; plan de reglement transforme en echeancier ; palier de relance par
-- WhatsApp (modele rappel_paiement).

CREATE TABLE IF NOT EXISTS billing_payment_promise
(
    id                 BIGINT PRIMARY KEY,
    promise_number     VARCHAR(40)    NOT NULL UNIQUE,
    document_number    VARCHAR(80)    NOT NULL,
    customer_type      VARCHAR(60),
    customer_code      VARCHAR(120),
    promised_date      DATE           NOT NULL,
    amount             NUMERIC(19, 4) NOT NULL,
    balance_at_promise NUMERIC(19, 4) NOT NULL,
    channel            VARCHAR(20)    NOT NULL,
    note               VARCHAR(1000),
    status             VARCHAR(20)    NOT NULL,
    created_by         VARCHAR(180),
    closed_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_payment_promise_status CHECK (status IN ('OPEN', 'KEPT', 'BROKEN', 'CANCELLED')),
    CONSTRAINT ck_billing_payment_promise_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS ix_billing_payment_promise_document ON billing_payment_promise (document_number, status);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3470001, 'billing_payment_promise', 'Promesse de paiement', 'Numero des promesses de paiement', 'PRM', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1,
       'MONTHLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'billing_payment_promise');

-- Palier de relance par WhatsApp.
ALTER TABLE dunning_step DROP CONSTRAINT IF EXISTS ck_dunning_step_channel;
ALTER TABLE dunning_step ADD CONSTRAINT ck_dunning_step_channel CHECK (channel IN ('EMAIL', 'IN_APP', 'STAFF', 'WHATSAPP'));
