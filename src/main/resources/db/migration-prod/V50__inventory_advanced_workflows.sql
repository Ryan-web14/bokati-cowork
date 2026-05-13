CREATE TABLE IF NOT EXISTS stock_transfer_workflow (
    id BIGINT PRIMARY KEY,
    transfer_code VARCHAR(100) NOT NULL UNIQUE,
    item_id BIGINT NOT NULL,
    from_location_id BIGINT NOT NULL,
    to_location_id BIGINT NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_by VARCHAR(120),
    approved_by VARCHAR(120),
    shipped_by VARCHAR(120),
    received_by VARCHAR(120),
    movement_code VARCHAR(120),
    reason TEXT,
    requested_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP(6) WITH TIME ZONE,
    shipped_at TIMESTAMP(6) WITH TIME ZONE,
    received_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_stock_transfer_workflow_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_stock_transfer_workflow_from_location FOREIGN KEY (from_location_id) REFERENCES inventory_location (id),
    CONSTRAINT fk_stock_transfer_workflow_to_location FOREIGN KEY (to_location_id) REFERENCES inventory_location (id),
    CONSTRAINT chk_stock_transfer_workflow_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_transfer_workflow_status ON stock_transfer_workflow (status);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_workflow_item ON stock_transfer_workflow (item_id);

CREATE TABLE IF NOT EXISTS inventory_unit_conversion (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    from_unit_code VARCHAR(80) NOT NULL,
    to_unit_code VARCHAR(80) NOT NULL,
    factor NUMERIC(19, 6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_inventory_unit_conversion_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT uq_inventory_unit_conversion UNIQUE (item_id, from_unit_code, to_unit_code),
    CONSTRAINT chk_inventory_unit_conversion_factor CHECK (factor > 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_unit_conversion_item ON inventory_unit_conversion (item_id);

CREATE TABLE IF NOT EXISTS purchase_approval_rule (
    id BIGINT PRIMARY KEY,
    approval_level VARCHAR(40) NOT NULL UNIQUE,
    min_amount BIGINT NOT NULL,
    max_amount BIGINT,
    required_approvals INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_purchase_approval_rule_amount CHECK (min_amount >= 0 AND (max_amount IS NULL OR max_amount >= min_amount)),
    CONSTRAINT chk_purchase_approval_rule_required CHECK (required_approvals > 0)
);

CREATE TABLE IF NOT EXISTS purchase_approval_step (
    id BIGINT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    approval_level VARCHAR(40) NOT NULL,
    approved_by VARCHAR(120) NOT NULL,
    approved_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_purchase_approval_step_order FOREIGN KEY (purchase_order_id) REFERENCES inventory_purchase_order (id)
);

CREATE INDEX IF NOT EXISTS idx_purchase_approval_step_order ON purchase_approval_step (purchase_order_id);

INSERT INTO purchase_approval_rule (id, approval_level, min_amount, max_amount, required_approvals, active, created_at)
SELECT 91001, 'MANAGER', 1, 499999, 1, TRUE, now()
WHERE NOT EXISTS (SELECT 1 FROM purchase_approval_rule WHERE approval_level = 'MANAGER');

INSERT INTO purchase_approval_rule (id, approval_level, min_amount, max_amount, required_approvals, active, created_at)
SELECT 91002, 'DIRECTOR', 500000, 4999999, 2, TRUE, now()
WHERE NOT EXISTS (SELECT 1 FROM purchase_approval_rule WHERE approval_level = 'DIRECTOR');

INSERT INTO purchase_approval_rule (id, approval_level, min_amount, max_amount, required_approvals, active, created_at)
SELECT 91003, 'EXECUTIVE', 5000000, NULL, 3, TRUE, now()
WHERE NOT EXISTS (SELECT 1 FROM purchase_approval_rule WHERE approval_level = 'EXECUTIVE');
