-- Payment schedule: echéancier lié à une BillingDocument
CREATE TABLE payment_schedule (
    id                      BIGSERIAL PRIMARY KEY,
    schedule_number         VARCHAR(100)   NOT NULL UNIQUE,
    billing_document_number VARCHAR(100)   NOT NULL,
    status                  VARCHAR(40)    NOT NULL DEFAULT 'ACTIVE',
    total_amount            NUMERIC(19, 4) NOT NULL,
    paid_amount             NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency                VARCHAR(3)     NOT NULL,
    notes                   TEXT,
    created_at              TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_schedule_number   ON payment_schedule (schedule_number);
CREATE INDEX idx_payment_schedule_document ON payment_schedule (billing_document_number);
CREATE INDEX idx_payment_schedule_status   ON payment_schedule (status);

-- Acomptes / échéances de l'échéancier
CREATE TABLE payment_schedule_installment (
    id                  BIGSERIAL PRIMARY KEY,
    installment_number  VARCHAR(100)   NOT NULL UNIQUE,
    schedule_id         BIGINT         NOT NULL,
    installment_order   INTEGER        NOT NULL,
    label               VARCHAR(200),
    amount              NUMERIC(19, 4) NOT NULL,
    due_date            DATE           NOT NULL,
    paid_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status              VARCHAR(40)    NOT NULL DEFAULT 'PENDING',
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_installment_schedule FOREIGN KEY (schedule_id) REFERENCES payment_schedule (id)
);

CREATE INDEX idx_installment_number     ON payment_schedule_installment (installment_number);
CREATE INDEX idx_installment_schedule   ON payment_schedule_installment (schedule_id);
CREATE INDEX idx_installment_status_due ON payment_schedule_installment (status, due_date);

-- Séquences métier
INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 820001, 'payment_schedule', 'Echeancier de paiement', 'Sequence des echeanciers de paiement', 'SCH', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'payment_schedule');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 820002, 'payment_schedule_installment', 'Acompte echeancier', 'Sequence des acomptes des echeanciers', 'INST', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'payment_schedule_installment');