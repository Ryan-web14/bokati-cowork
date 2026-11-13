-- Lot B10 (facturation) : catalogue. Prix par segment de client et dates, avec l'historique des
-- changements de prix ; lien vers l'article d'inventaire ; compte de produit propre a l'article,
-- prioritaire sur le compte par type de ligne.

CREATE TABLE IF NOT EXISTS service_catalog_price
(
    id          BIGINT PRIMARY KEY,
    item_code   VARCHAR(80)    NOT NULL,
    segment     VARCHAR(20)    NOT NULL,
    unit_price  NUMERIC(19, 4) NOT NULL,
    valid_from  DATE           NOT NULL,
    valid_until DATE,
    reason      VARCHAR(500),
    created_by  VARCHAR(180),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_service_catalog_price_segment CHECK (segment IN ('ALL', 'MEMBER', 'COMPANY', 'VISITOR'))
);

CREATE INDEX IF NOT EXISTS ix_service_catalog_price_item ON service_catalog_price (item_code, segment, valid_from);

ALTER TABLE service_catalog_item ADD COLUMN IF NOT EXISTS inventory_item_code VARCHAR(80);
ALTER TABLE service_catalog_item ADD COLUMN IF NOT EXISTS revenue_account VARCHAR(20);
