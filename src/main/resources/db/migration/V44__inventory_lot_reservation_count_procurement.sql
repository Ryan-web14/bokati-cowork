CREATE TABLE IF NOT EXISTS stock_lot (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    lot_number VARCHAR(120) NOT NULL,
    expiry_date DATE,
    received_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    initial_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    remaining_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_stock_lot_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_stock_lot_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_lot_item_location ON stock_lot (item_id, location_id);
CREATE INDEX IF NOT EXISTS idx_stock_lot_expiry ON stock_lot (expiry_date);
CREATE INDEX IF NOT EXISTS idx_stock_lot_remaining ON stock_lot (remaining_quantity);

CREATE TABLE IF NOT EXISTS stock_reservation (
    id BIGINT PRIMARY KEY,
    reservation_code VARCHAR(100) NOT NULL,
    item_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    reference_type VARCHAR(60),
    reference_code VARCHAR(120),
    reserved_by VARCHAR(120),
    reserved_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP(6) WITH TIME ZONE,
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_stock_reservation_code UNIQUE (reservation_code),
    CONSTRAINT fk_stock_reservation_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_stock_reservation_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_status_expiry ON stock_reservation (status, expires_at);
CREATE INDEX IF NOT EXISTS idx_stock_reservation_reference ON stock_reservation (reference_type, reference_code);

CREATE TABLE IF NOT EXISTS inventory_count (
    id BIGINT PRIMARY KEY,
    count_code VARCHAR(100) NOT NULL,
    location_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(120),
    notes TEXT,
    started_at TIMESTAMP(6) WITH TIME ZONE,
    reviewed_at TIMESTAMP(6) WITH TIME ZONE,
    validated_at TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_count_code UNIQUE (count_code),
    CONSTRAINT fk_inventory_count_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS inventory_count_item (
    id BIGINT PRIMARY KEY,
    inventory_count_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    expected_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    counted_quantity NUMERIC(19, 4),
    variance_quantity NUMERIC(19, 4),
    notes TEXT,
    CONSTRAINT uk_inventory_count_item UNIQUE (inventory_count_id, item_id),
    CONSTRAINT fk_inventory_count_item_count FOREIGN KEY (inventory_count_id) REFERENCES inventory_count (id),
    CONSTRAINT fk_inventory_count_item_item FOREIGN KEY (item_id) REFERENCES inventory_item (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_count_status ON inventory_count (status);
CREATE INDEX IF NOT EXISTS idx_inventory_count_location ON inventory_count (location_id);

CREATE TABLE IF NOT EXISTS inventory_supplier (
    id BIGINT PRIMARY KEY,
    supplier_code VARCHAR(100) NOT NULL,
    name VARCHAR(220) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(80),
    tax_id VARCHAR(120),
    address TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_supplier_code UNIQUE (supplier_code)
);

CREATE TABLE IF NOT EXISTS purchase_request (
    id BIGINT PRIMARY KEY,
    request_code VARCHAR(100) NOT NULL,
    location_id BIGINT,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    requested_by VARCHAR(120),
    approved_by VARCHAR(120),
    rejection_reason TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_purchase_request_code UNIQUE (request_code),
    CONSTRAINT fk_purchase_request_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS purchase_request_line (
    id BIGINT PRIMARY KEY,
    purchase_request_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    estimated_unit_cost BIGINT,
    CONSTRAINT fk_purchase_request_line_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_request (id),
    CONSTRAINT fk_purchase_request_line_item FOREIGN KEY (item_id) REFERENCES inventory_item (id)
);

CREATE TABLE IF NOT EXISTS inventory_purchase_order (
    id BIGINT PRIMARY KEY,
    order_code VARCHAR(100) NOT NULL,
    supplier_id BIGINT NOT NULL,
    location_id BIGINT,
    source_request_code VARCHAR(100),
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    expected_delivery_date DATE,
    ordered_by VARCHAR(120),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_purchase_order_code UNIQUE (order_code),
    CONSTRAINT fk_inventory_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES inventory_supplier (id),
    CONSTRAINT fk_inventory_purchase_order_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS inventory_purchase_order_line (
    id BIGINT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    ordered_quantity NUMERIC(19, 4) NOT NULL,
    received_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    unit_cost BIGINT,
    CONSTRAINT fk_inventory_po_line_order FOREIGN KEY (purchase_order_id) REFERENCES inventory_purchase_order (id),
    CONSTRAINT fk_inventory_po_line_item FOREIGN KEY (item_id) REFERENCES inventory_item (id)
);

CREATE TABLE IF NOT EXISTS goods_receipt (
    id BIGINT PRIMARY KEY,
    receipt_code VARCHAR(100) NOT NULL,
    purchase_order_id BIGINT,
    location_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    received_by VARCHAR(120),
    received_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    posted_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_goods_receipt_code UNIQUE (receipt_code),
    CONSTRAINT fk_goods_receipt_po FOREIGN KEY (purchase_order_id) REFERENCES inventory_purchase_order (id),
    CONSTRAINT fk_goods_receipt_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE TABLE IF NOT EXISTS goods_receipt_line (
    id BIGINT PRIMARY KEY,
    goods_receipt_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    received_quantity NUMERIC(19, 4) NOT NULL,
    unit_cost BIGINT,
    lot_number VARCHAR(120),
    expiry_date DATE,
    CONSTRAINT fk_goods_receipt_line_receipt FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipt (id),
    CONSTRAINT fk_goods_receipt_line_item FOREIGN KEY (item_id) REFERENCES inventory_item (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_supplier_name ON inventory_supplier (name);
CREATE INDEX IF NOT EXISTS idx_purchase_request_status ON purchase_request (status);
CREATE INDEX IF NOT EXISTS idx_inventory_po_status ON inventory_purchase_order (status);
CREATE INDEX IF NOT EXISTS idx_goods_receipt_status ON goods_receipt (status);

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 440001, 'stock_reservation', 'Reservation stock', 'Sequence des reservations de stock', 'RSV', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'stock_reservation');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 440002, 'inventory_count', 'Inventaire physique', 'Sequence des inventaires physiques', 'CNT', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'inventory_count');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 440003, 'supplier', 'Fournisseur', 'Sequence des fournisseurs inventaire', 'SUP', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'supplier');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 440004, 'purchase_request', 'Demande achat', 'Sequence des demandes achat inventaire', 'DA', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'purchase_request');
