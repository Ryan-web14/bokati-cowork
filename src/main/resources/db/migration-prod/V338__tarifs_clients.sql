-- Lot C8 (facturation, complements) : tarifs negocies par client (contrat-cadre). Prix fixe ou taux
-- de remise propre a une organisation ou un membre, date ; prioritaire sur le prix du segment et
-- sur celui de la fiche (decision CD8). Un nouveau tarif du meme article arrete le precedent la veille.

CREATE TABLE IF NOT EXISTS billing_customer_price
(
    id                 BIGINT PRIMARY KEY,
    customer_type      VARCHAR(60)    NOT NULL,
    customer_code      VARCHAR(120)   NOT NULL,
    item_code          VARCHAR(80)    NOT NULL,
    unit_price         NUMERIC(19, 4),
    discount_rate      NUMERIC(9, 4),
    currency           VARCHAR(3)     NOT NULL DEFAULT 'XAF',
    valid_from         DATE           NOT NULL,
    valid_until        DATE,
    contract_reference VARCHAR(120),
    reason             VARCHAR(500),
    created_by         VARCHAR(180),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_customer_price_value CHECK ((unit_price IS NULL) <> (discount_rate IS NULL)),
    CONSTRAINT ck_billing_customer_price_rate CHECK (discount_rate IS NULL OR (discount_rate >= 0 AND discount_rate <= 100)),
    CONSTRAINT ck_billing_customer_price_dates CHECK (valid_until IS NULL OR valid_until >= valid_from)
);

CREATE INDEX IF NOT EXISTS ix_billing_customer_price_customer ON billing_customer_price (customer_type, customer_code, item_code, valid_from);
