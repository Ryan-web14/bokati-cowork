CREATE TABLE IF NOT EXISTS inventory_category (
    id BIGINT PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    parent_category_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_category_code UNIQUE (code),
    CONSTRAINT fk_inventory_category_parent FOREIGN KEY (parent_category_id) REFERENCES inventory_category (id)
);

CREATE TABLE IF NOT EXISTS inventory_unit (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    unit_type VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_unit_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS inventory_item (
    id BIGINT PRIMARY KEY,
    item_code VARCHAR(80) NOT NULL,
    name VARCHAR(220) NOT NULL,
    description TEXT,
    psku VARCHAR(120),
    short_code VARCHAR(80),
    display_code VARCHAR(120),
    identification_code VARCHAR(120),
    specification TEXT,
    search_text TEXT,
    category_id BIGINT,
    unit_id BIGINT,
    item_type VARCHAR(40) NOT NULL,
    tracking_type VARCHAR(40) NOT NULL,
    default_cost BIGINT,
    sale_price BIGINT,
    taxable BOOLEAN NOT NULL DEFAULT FALSE,
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    requires_expiry_date BOOLEAN NOT NULL DEFAULT FALSE,
    requires_lot_number BOOLEAN NOT NULL DEFAULT FALSE,
    requires_serial_number BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_item_code UNIQUE (item_code),
    CONSTRAINT uk_inventory_item_psku UNIQUE (psku),
    CONSTRAINT uk_inventory_item_short_code UNIQUE (short_code),
    CONSTRAINT uk_inventory_item_display_code UNIQUE (display_code),
    CONSTRAINT uk_inventory_item_identification_code UNIQUE (identification_code),
    CONSTRAINT fk_inventory_item_category FOREIGN KEY (category_id) REFERENCES inventory_category (id),
    CONSTRAINT fk_inventory_item_unit FOREIGN KEY (unit_id) REFERENCES inventory_unit (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_category_active ON inventory_category (active);
CREATE INDEX IF NOT EXISTS idx_inventory_unit_active ON inventory_unit (active);
CREATE INDEX IF NOT EXISTS idx_inventory_item_name ON inventory_item (name);
CREATE INDEX IF NOT EXISTS idx_inventory_item_category ON inventory_item (category_id);
CREATE INDEX IF NOT EXISTS idx_inventory_item_unit ON inventory_item (unit_id);
CREATE INDEX IF NOT EXISTS idx_inventory_item_type ON inventory_item (item_type);
CREATE INDEX IF NOT EXISTS idx_inventory_item_active ON inventory_item (active);
CREATE INDEX IF NOT EXISTS idx_inventory_item_search_text ON inventory_item USING gin (to_tsvector('simple', coalesce(search_text, '')));

INSERT INTO inventory_unit (id, code, name, unit_type, active, created_at, updated_at)
SELECT 380001, 'UNIT', 'Unite', 'UNIT', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_unit WHERE code = 'UNIT');

INSERT INTO inventory_unit (id, code, name, unit_type, active, created_at, updated_at)
SELECT 380002, 'BOX', 'Boite', 'BOX', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_unit WHERE code = 'BOX');

INSERT INTO inventory_unit (id, code, name, unit_type, active, created_at, updated_at)
SELECT 380003, 'PACK', 'Pack', 'PACK', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_unit WHERE code = 'PACK');

INSERT INTO inventory_category (id, code, name, description, active, created_at, updated_at)
SELECT 380101, 'SNACK', 'Snack', 'Produits consommables vendus ou consommes sur site', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_category WHERE code = 'SNACK');

INSERT INTO inventory_category (id, code, name, description, active, created_at, updated_at)
SELECT 380102, 'OFFICE_SUPPLY', 'Fournitures bureau', 'Fournitures et consommables administratifs', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_category WHERE code = 'OFFICE_SUPPLY');

INSERT INTO inventory_category (id, code, name, description, active, created_at, updated_at)
SELECT 380103, 'EQUIPMENT', 'Equipements', 'Equipements individualises et assets', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_category WHERE code = 'EQUIPMENT');
