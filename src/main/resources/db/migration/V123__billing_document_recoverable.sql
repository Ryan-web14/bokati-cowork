-- Articles remis au client à récupérer en fin de prestation (clé, badge, équipement, etc.)
CREATE TABLE IF NOT EXISTS billing_document_recoverable (
    id                   BIGINT         NOT NULL PRIMARY KEY,
    recoverable_number   VARCHAR(100)   NOT NULL UNIQUE,
    document_id          BIGINT         NOT NULL,
    item_description     VARCHAR(500)   NOT NULL,
    quantity             NUMERIC(19, 4) NOT NULL DEFAULT 1,
    unit                 VARCHAR(50),
    source_type          VARCHAR(80),
    source_code          VARCHAR(120),
    status               VARCHAR(40)    NOT NULL DEFAULT 'PENDING',
    notes                TEXT,
    recovered_at         TIMESTAMPTZ,
    recovered_by         VARCHAR(120),
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_recoverable_document FOREIGN KEY (document_id) REFERENCES billing_document(id),
    CONSTRAINT ck_recoverable_status   CHECK (status IN ('PENDING', 'RECOVERED', 'PARTIALLY_RECOVERED', 'WRITTEN_OFF')),
    CONSTRAINT ck_recoverable_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_recoverable_document ON billing_document_recoverable(document_id);
CREATE INDEX IF NOT EXISTS idx_recoverable_number   ON billing_document_recoverable(recoverable_number);
CREATE INDEX IF NOT EXISTS idx_recoverable_status   ON billing_document_recoverable(status);

INSERT INTO sequence_definition
    (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 1230001, 'billing_recoverable', 'Article à récupérer', 'Séquence des articles à récupérer sur factures', 'REC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_recoverable');
