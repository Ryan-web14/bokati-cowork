ALTER TABLE stock_movement
    ADD COLUMN IF NOT EXISTS reversed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS reversal_of_movement_id BIGINT,
    ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS reversed_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS reversal_reason TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_stock_movement_reversal_of'
    ) THEN
        ALTER TABLE stock_movement
            ADD CONSTRAINT fk_stock_movement_reversal_of
                FOREIGN KEY (reversal_of_movement_id) REFERENCES stock_movement (id) NOT VALID;
    END IF;
END $$;

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

ALTER TABLE inventory_purchase_order
    ADD COLUMN IF NOT EXISTS approval_level VARCHAR(40) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS total_amount BIGINT;

ALTER TABLE goods_receipt
    ADD COLUMN IF NOT EXISTS invoice_document_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS delivery_note_document_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS proof_document_code VARCHAR(120);

ALTER TABLE goods_receipt_line
    ADD COLUMN IF NOT EXISTS rejected_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS quality_accepted BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS backorder_quantity NUMERIC(19, 4);
