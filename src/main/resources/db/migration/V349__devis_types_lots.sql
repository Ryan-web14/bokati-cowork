-- Lot C14 (facturation, complements) : devis types et lots d'articles. Un modele de devis
-- (« bureau privatif 12 mois », « seminaire journee ») cree un devis pret a ajuster ; un article
-- compose se vend a prix global, son produit se ventile entre ses composants et son stock sort
-- composant par composant.

CREATE TABLE IF NOT EXISTS billing_quote_template
(
    id            BIGINT PRIMARY KEY,
    code          VARCHAR(60)  NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    validity_days INTEGER      NOT NULL DEFAULT 30,
    terms         TEXT,
    lines_json    TEXT         NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(180),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_quote_template_validity CHECK (validity_days BETWEEN 1 AND 365)
);

CREATE TABLE IF NOT EXISTS billing_catalog_component
(
    id               BIGINT PRIMARY KEY,
    parent_item_code VARCHAR(80)    NOT NULL,
    component_code   VARCHAR(100)   NOT NULL,
    component_source VARCHAR(20)    NOT NULL,
    quantity         NUMERIC(19, 4) NOT NULL,
    display_order    INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT ck_billing_catalog_component_source CHECK (component_source IN ('SERVICE_CATALOG', 'INVENTORY_ITEM')),
    CONSTRAINT ck_billing_catalog_component_quantity CHECK (quantity > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_billing_catalog_component ON billing_catalog_component (parent_item_code, component_source, component_code);
