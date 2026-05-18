-- Bloc 4 : escompte pour paiement anticipé
CREATE TABLE IF NOT EXISTS billing_document_early_payment_discount (
    id                  BIGINT        NOT NULL PRIMARY KEY,
    document_id         BIGINT        NOT NULL,
    discount_rate       NUMERIC(9,4)  NOT NULL,
    if_paid_before      DATE          NOT NULL,
    computed_amount     NUMERIC(19,4) NOT NULL,
    label               VARCHAR(200),
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_early_discount_document FOREIGN KEY (document_id) REFERENCES billing_document(id)
);

CREATE INDEX IF NOT EXISTS idx_early_discount_document ON billing_document_early_payment_discount(document_id);
