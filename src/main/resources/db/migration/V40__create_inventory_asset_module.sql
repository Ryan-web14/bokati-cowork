CREATE TABLE IF NOT EXISTS asset (
    id BIGINT PRIMARY KEY,
    asset_code VARCHAR(90) NOT NULL,
    item_id BIGINT NOT NULL,
    serial_number VARCHAR(160),
    asset_tag VARCHAR(120),
    status VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE',
    condition VARCHAR(40) NOT NULL DEFAULT 'GOOD',
    location_id BIGINT,
    assigned_to_type VARCHAR(40),
    assigned_to_code VARCHAR(120),
    purchase_date DATE,
    purchase_cost BIGINT,
    warranty_end_date DATE,
    notes TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_asset_code UNIQUE (asset_code),
    CONSTRAINT uk_asset_serial_number UNIQUE (serial_number),
    CONSTRAINT uk_asset_tag UNIQUE (asset_tag),
    CONSTRAINT fk_asset_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_asset_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS asset_assignment (
    id BIGINT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    assignee_type VARCHAR(40) NOT NULL,
    assignee_code VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    start_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    expected_return_at TIMESTAMP(6) WITH TIME ZONE,
    end_at TIMESTAMP(6) WITH TIME ZONE,
    assigned_by VARCHAR(120),
    returned_by VARCHAR(120),
    return_condition VARCHAR(40),
    notes TEXT,
    CONSTRAINT fk_asset_assignment_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

CREATE TABLE IF NOT EXISTS asset_maintenance (
    id BIGINT PRIMARY KEY,
    maintenance_code VARCHAR(90) NOT NULL,
    asset_id BIGINT NOT NULL,
    maintenance_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
    scheduled_at TIMESTAMP(6) WITH TIME ZONE,
    started_at TIMESTAMP(6) WITH TIME ZONE,
    completed_at TIMESTAMP(6) WITH TIME ZONE,
    provider_name VARCHAR(180),
    cost BIGINT,
    description TEXT,
    resolution TEXT,
    CONSTRAINT uk_asset_maintenance_code UNIQUE (maintenance_code),
    CONSTRAINT fk_asset_maintenance_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

CREATE INDEX IF NOT EXISTS idx_asset_item ON asset (item_id);
CREATE INDEX IF NOT EXISTS idx_asset_status ON asset (status);
CREATE INDEX IF NOT EXISTS idx_asset_condition ON asset (condition);
CREATE INDEX IF NOT EXISTS idx_asset_location ON asset (location_id);
CREATE INDEX IF NOT EXISTS idx_asset_assignee ON asset (assigned_to_type, assigned_to_code);
CREATE INDEX IF NOT EXISTS idx_asset_assignment_asset_status ON asset_assignment (asset_id, status);
CREATE INDEX IF NOT EXISTS idx_asset_assignment_assignee ON asset_assignment (assignee_type, assignee_code);
CREATE INDEX IF NOT EXISTS idx_asset_maintenance_asset_status ON asset_maintenance (asset_id, status);
CREATE INDEX IF NOT EXISTS idx_asset_maintenance_scheduled ON asset_maintenance (scheduled_at);
