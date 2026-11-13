-- Lot C11 (facturation, complements) : reclamations sur facture. Une reclamation ouverte suspend
-- les relances de la facture ; elle se regle par un avoir (total ou partiel) ou par un refus motive.

CREATE TABLE IF NOT EXISTS billing_claim_reason
(
    id            BIGINT PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    label         VARCHAR(150) NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO billing_claim_reason (id, code, label, display_order)
SELECT v.id, v.code, v.label, v.ord
FROM (VALUES (3460001, 'ERREUR_PRIX', 'Erreur de prix ou de remise', 1),
             (3460002, 'QUANTITE', 'Quantité ou durée facturée erronée', 2),
             (3460003, 'SERVICE_NON_RENDU', 'Prestation non rendue ou annulée', 3),
             (3460004, 'QUALITE', 'Prestation insatisfaisante', 4),
             (3460005, 'DOUBLON', 'Facturé deux fois', 5),
             (3460006, 'AUTRE', 'Autre motif', 99)) AS v(id, code, label, ord)
WHERE NOT EXISTS (SELECT 1 FROM billing_claim_reason r WHERE r.code = v.code);

CREATE TABLE IF NOT EXISTS billing_claim
(
    id                 BIGINT PRIMARY KEY,
    claim_number       VARCHAR(40)    NOT NULL UNIQUE,
    document_number    VARCHAR(80)    NOT NULL,
    customer_type      VARCHAR(60),
    customer_code      VARCHAR(120),
    reason_code        VARCHAR(40)    NOT NULL,
    description        TEXT,
    line_orders        VARCHAR(500),
    requested_amount   NUMERIC(19, 4),
    status             VARCHAR(30)    NOT NULL,
    channel            VARCHAR(20)    NOT NULL,
    opened_by          VARCHAR(180),
    granted_amount     NUMERIC(19, 4),
    credit_note_number VARCHAR(80),
    decision_comment   VARCHAR(2000),
    decided_by         VARCHAR(180),
    decided_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_claim_status CHECK (status IN ('OPEN', 'ACCEPTED', 'PARTIALLY_ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_billing_claim_channel CHECK (channel IN ('STAFF', 'PORTAL'))
);

CREATE INDEX IF NOT EXISTS ix_billing_claim_document ON billing_claim (document_number, status);
CREATE INDEX IF NOT EXISTS ix_billing_claim_customer ON billing_claim (customer_type, customer_code);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3460010, 'billing_claim', 'Réclamation', 'Numero des reclamations sur facture', 'RCL', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1,
       'MONTHLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'billing_claim');
