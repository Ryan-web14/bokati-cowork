-- Lot C4 (facturation, complements) : bons de livraison. Piece non fiscale tiree d'un devis
-- accepte ou d'une facture emise ; livraisons partielles, reste a livrer, sortie de stock a la
-- livraison quand billing.stock.issue-on vaut DELIVERY, facture des quantites livrees d'un devis.

CREATE TABLE IF NOT EXISTS billing_delivery_note
(
    id                     BIGINT PRIMARY KEY,
    note_number            VARCHAR(40)  NOT NULL UNIQUE,
    source_document_number VARCHAR(80)  NOT NULL,
    source_document_type   VARCHAR(30)  NOT NULL,
    customer_type          VARCHAR(60),
    customer_code          VARCHAR(120),
    customer_name          VARCHAR(255),
    status                 VARCHAR(20)  NOT NULL,
    delivery_address       TEXT,
    notes                  TEXT,
    show_prices            BOOLEAN      NOT NULL DEFAULT FALSE,
    stock_issued           BOOLEAN      NOT NULL DEFAULT FALSE,
    delivered_at           TIMESTAMPTZ,
    delivered_by           VARCHAR(180),
    received_by            VARCHAR(255),
    signed_copy_reference  VARCHAR(120),
    invoice_number         VARCHAR(80),
    cancelled_at           TIMESTAMPTZ,
    cancelled_by           VARCHAR(180),
    cancel_reason          VARCHAR(500),
    created_by             VARCHAR(180),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_delivery_note_status CHECK (status IN ('DRAFT', 'DELIVERED', 'CANCELLED', 'INVOICED'))
);

CREATE INDEX IF NOT EXISTS ix_billing_delivery_note_source ON billing_delivery_note (source_document_number);
CREATE INDEX IF NOT EXISTS ix_billing_delivery_note_customer ON billing_delivery_note (customer_type, customer_code);

CREATE TABLE IF NOT EXISTS billing_delivery_note_line
(
    id             BIGINT PRIMARY KEY,
    note_id        BIGINT         NOT NULL REFERENCES billing_delivery_note (id),
    source_line_id BIGINT         NOT NULL,
    line_order     INTEGER        NOT NULL,
    item_code      VARCHAR(100),
    source_type    VARCHAR(80),
    line_type      VARCHAR(30),
    description    VARCHAR(4000)  NOT NULL,
    unit           VARCHAR(50),
    quantity       NUMERIC(19, 4) NOT NULL,
    unit_price     NUMERIC(19, 4),
    CONSTRAINT ck_billing_delivery_note_line_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS ix_billing_delivery_note_line_note ON billing_delivery_note_line (note_id);
CREATE INDEX IF NOT EXISTS ix_billing_delivery_note_line_source ON billing_delivery_note_line (source_line_id);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3410001, 'billing_delivery_note', 'Bon de livraison', 'Numero des bons de livraison', 'BL', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1,
       'MONTHLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'billing_delivery_note');
