CREATE TABLE IF NOT EXISTS inventory_reorder_rule (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    location_id BIGINT,
    min_quantity NUMERIC(19, 4) NOT NULL,
    max_quantity NUMERIC(19, 4),
    reorder_quantity NUMERIC(19, 4) NOT NULL,
    preferred_supplier_code VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_reorder_item_location UNIQUE (item_id, location_id),
    CONSTRAINT fk_inventory_reorder_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_inventory_reorder_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS inventory_alert (
    id BIGINT PRIMARY KEY,
    alert_code VARCHAR(100) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    item_id BIGINT,
    location_id BIGINT,
    asset_code VARCHAR(100),
    current_quantity NUMERIC(19, 4),
    threshold_quantity NUMERIC(19, 4),
    message TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMP(6) WITH TIME ZONE,
    resolved_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_inventory_alert_code UNIQUE (alert_code),
    CONSTRAINT fk_inventory_alert_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_inventory_alert_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_reorder_item ON inventory_reorder_rule (item_id);
CREATE INDEX IF NOT EXISTS idx_inventory_reorder_location ON inventory_reorder_rule (location_id);
CREATE INDEX IF NOT EXISTS idx_inventory_reorder_active ON inventory_reorder_rule (active);
CREATE INDEX IF NOT EXISTS idx_inventory_alert_status_type ON inventory_alert (status, alert_type);
CREATE INDEX IF NOT EXISTS idx_inventory_alert_item_location ON inventory_alert (item_id, location_id);
CREATE INDEX IF NOT EXISTS idx_inventory_alert_created_at ON inventory_alert (created_at);

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 430001, 'inventory_alert', 'Alerte inventaire', 'Sequence des alertes inventaire', 'IAL', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'inventory_alert');
