ALTER TABLE stock_movement
    ADD COLUMN IF NOT EXISTS reversed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE stock_movement
    ADD COLUMN IF NOT EXISTS reversal_of_movement_id BIGINT;

ALTER TABLE stock_movement
    ADD CONSTRAINT fk_stock_movement_reversal_of
        FOREIGN KEY (reversal_of_movement_id) REFERENCES stock_movement (id) NOT VALID;

CREATE TABLE IF NOT EXISTS stock_movement_lot (
    id BIGINT PRIMARY KEY,
    movement_id BIGINT NOT NULL,
    stock_lot_id BIGINT,
    lot_number VARCHAR(120) NOT NULL,
    expiry_date DATE,
    quantity NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_stock_movement_lot_movement FOREIGN KEY (movement_id) REFERENCES stock_movement (id),
    CONSTRAINT fk_stock_movement_lot_lot FOREIGN KEY (stock_lot_id) REFERENCES stock_lot (id),
    CONSTRAINT chk_stock_movement_lot_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_movement_lot_movement ON stock_movement_lot (movement_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_lot_lot ON stock_movement_lot (stock_lot_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_reversal ON stock_movement (reversal_of_movement_id);
