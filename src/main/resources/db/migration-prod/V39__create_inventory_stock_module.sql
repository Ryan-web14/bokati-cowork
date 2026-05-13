CREATE TABLE IF NOT EXISTS inventory_location (
    id BIGINT PRIMARY KEY,
    location_code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    location_type VARCHAR(40) NOT NULL,
    parent_location_id BIGINT,
    business_code VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_location_code UNIQUE (location_code),
    CONSTRAINT fk_inventory_location_parent FOREIGN KEY (parent_location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS stock_level (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    quantity_on_hand NUMERIC(19, 4) NOT NULL DEFAULT 0,
    quantity_reserved NUMERIC(19, 4) NOT NULL DEFAULT 0,
    quantity_available NUMERIC(19, 4) NOT NULL DEFAULT 0,
    average_cost BIGINT,
    last_movement_at TIMESTAMP(6) WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_level_item_location UNIQUE (item_id, location_id),
    CONSTRAINT fk_stock_level_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_stock_level_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS stock_movement (
    id BIGINT PRIMARY KEY,
    movement_code VARCHAR(90) NOT NULL,
    item_id BIGINT NOT NULL,
    location_from_id BIGINT,
    location_to_id BIGINT,
    movement_type VARCHAR(40) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit_cost BIGINT,
    total_cost BIGINT,
    reference_type VARCHAR(60),
    reference_code VARCHAR(120),
    reason TEXT,
    allow_negative_override BOOLEAN NOT NULL DEFAULT FALSE,
    performed_by VARCHAR(120),
    performed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_movement_code UNIQUE (movement_code),
    CONSTRAINT fk_stock_movement_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_stock_movement_location_from FOREIGN KEY (location_from_id) REFERENCES inventory_location (id),
    CONSTRAINT fk_stock_movement_location_to FOREIGN KEY (location_to_id) REFERENCES inventory_location (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_location_active ON inventory_location (active);
CREATE INDEX IF NOT EXISTS idx_inventory_location_business ON inventory_location (business_code);
CREATE INDEX IF NOT EXISTS idx_stock_level_item ON stock_level (item_id);
CREATE INDEX IF NOT EXISTS idx_stock_level_location ON stock_level (location_id);
CREATE INDEX IF NOT EXISTS idx_stock_level_available ON stock_level (quantity_available);
CREATE INDEX IF NOT EXISTS idx_stock_movement_item_date ON stock_movement (item_id, performed_at);
CREATE INDEX IF NOT EXISTS idx_stock_movement_from ON stock_movement (location_from_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_to ON stock_movement (location_to_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_reference ON stock_movement (reference_type, reference_code);
CREATE INDEX IF NOT EXISTS idx_stock_movement_type_date ON stock_movement (movement_type, performed_at);

INSERT INTO inventory_location (id, location_code, name, description, location_type, active, created_at, updated_at)
SELECT 390001, 'LOC-MAIN', 'Stock principal', 'Localisation principale de stock', 'WAREHOUSE', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_location WHERE location_code = 'LOC-MAIN');
