ALTER TABLE stock_lot
    ADD COLUMN IF NOT EXISTS quarantined BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS quarantine_reason TEXT,
    ADD COLUMN IF NOT EXISTS ownership_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS owner_code VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_stock_lot_quarantined ON stock_lot (quarantined);
CREATE INDEX IF NOT EXISTS idx_stock_lot_owner ON stock_lot (ownership_type, owner_code);
