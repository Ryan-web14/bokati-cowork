ALTER TABLE stock_level
    ADD CONSTRAINT chk_stock_level_quantities
        CHECK (quantity_on_hand >= 0 OR quantity_available < 0) NOT VALID;

ALTER TABLE stock_lot
    ADD CONSTRAINT chk_stock_lot_quantities
        CHECK (initial_quantity >= 0 AND remaining_quantity >= 0 AND remaining_quantity <= initial_quantity) NOT VALID;

ALTER TABLE stock_reservation
    ADD CONSTRAINT chk_stock_reservation_quantity
        CHECK (quantity > 0) NOT VALID;

ALTER TABLE stock_movement
    ADD CONSTRAINT chk_stock_movement_quantity
        CHECK (quantity > 0) NOT VALID;

ALTER TABLE inventory_count_item
    ADD CONSTRAINT chk_inventory_count_item_quantities
        CHECK (expected_quantity >= 0 AND (counted_quantity IS NULL OR counted_quantity >= 0)) NOT VALID;

ALTER TABLE purchase_request_line
    ADD CONSTRAINT chk_purchase_request_line_quantity
        CHECK (quantity > 0) NOT VALID;

ALTER TABLE inventory_purchase_order_line
    ADD CONSTRAINT chk_inventory_po_line_quantities
        CHECK (ordered_quantity > 0 AND received_quantity >= 0 AND received_quantity <= ordered_quantity) NOT VALID;

ALTER TABLE goods_receipt_line
    ADD CONSTRAINT chk_goods_receipt_line_quantity
        CHECK (received_quantity > 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_lot_item_location_lot_expiry
    ON stock_lot (item_id, location_id, lot_number, COALESCE(expiry_date, DATE '9999-12-31'));

CREATE INDEX IF NOT EXISTS idx_purchase_request_created_at ON purchase_request (created_at);
CREATE INDEX IF NOT EXISTS idx_inventory_purchase_order_created_at ON inventory_purchase_order (created_at);
CREATE INDEX IF NOT EXISTS idx_goods_receipt_received_at ON goods_receipt (received_at);
