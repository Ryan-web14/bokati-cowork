-- Bloc 2 : acompte sur devis / facture
CREATE TABLE IF NOT EXISTS billing_document_advance (
    id                      BIGINT       NOT NULL PRIMARY KEY,
    document_id             BIGINT       NOT NULL REFERENCES billing_document(id),
    advance_type            VARCHAR(20)  NOT NULL,           -- PERCENTAGE | FIXED_AMOUNT
    advance_value           NUMERIC(19,4) NOT NULL,          -- valeur saisie (% ou montant)
    computed_amount         NUMERIC(19,4) NOT NULL,          -- montant calculé et stocké
    included_line_orders    TEXT,                            -- virgule-séparés, null = toutes
    excluded_line_orders    TEXT,                            -- virgule-séparés, null = aucune
    payment_reference       VARCHAR(100),                    -- référence à rappeler
    reference_label         VARCHAR(200),                    -- libellé affiché sur le document
    due_date                DATE,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    paid_at                 TIMESTAMPTZ,
    notes                   TEXT,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_advance_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_advance_document ON billing_document_advance(document_id);