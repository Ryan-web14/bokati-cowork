-- Catalogue de services prédéfinis pour préremplissage des lignes de devis/factures
CREATE TABLE IF NOT EXISTS service_catalog_item (
    id            BIGINT         NOT NULL PRIMARY KEY,
    item_code     VARCHAR(100)   NOT NULL UNIQUE,
    name          VARCHAR(255)   NOT NULL,
    description   TEXT,
    category      VARCHAR(100),
    unit          VARCHAR(50),
    unit_price    NUMERIC(19, 4) NOT NULL,
    currency      VARCHAR(3)     NOT NULL DEFAULT 'XAF',
    tax_rule_code VARCHAR(50),
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    display_order INTEGER        NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_catalog_item_code     ON service_catalog_item(item_code);
CREATE INDEX IF NOT EXISTS idx_catalog_item_category ON service_catalog_item(category);
CREATE INDEX IF NOT EXISTS idx_catalog_item_active   ON service_catalog_item(active, display_order);
CREATE INDEX IF NOT EXISTS idx_catalog_item_trgm     ON service_catalog_item USING gin(name gin_trgm_ops);

INSERT INTO sequence_definition
    (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 1220001, 'service_catalog_item', 'Article catalogue services', 'Séquence des articles du catalogue de services', 'SVC', null, '{PREFIX}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'service_catalog_item');