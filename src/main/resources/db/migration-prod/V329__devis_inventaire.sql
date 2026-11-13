-- Lot B7 (facturation) : devis et inventaire. Le stock des articles d'un devis est reserve a son
-- acceptation (dans la limite du disponible), libere au refus, a l'expiration ou a la revision, et
-- sort a l'emission de la facture (parametre billing.stock.issue-on).

CREATE TABLE IF NOT EXISTS billing_stock_reservation
(
    id               BIGINT PRIMARY KEY,
    document_number  VARCHAR(60)    NOT NULL,
    line_order       INTEGER,
    item_code        VARCHAR(80)    NOT NULL,
    location_code    VARCHAR(80)    NOT NULL,
    reservation_code VARCHAR(80)    NOT NULL,
    quantity         NUMERIC(19, 4) NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    closed_by        VARCHAR(60),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    closed_at        TIMESTAMPTZ,
    CONSTRAINT ck_billing_stock_reservation_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED'))
);

CREATE INDEX IF NOT EXISTS ix_billing_stock_reservation_document ON billing_stock_reservation (document_number, status);
