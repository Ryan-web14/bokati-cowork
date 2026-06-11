ALTER TABLE stock_movement
    ADD COLUMN IF NOT EXISTS reason_code VARCHAR(40);
