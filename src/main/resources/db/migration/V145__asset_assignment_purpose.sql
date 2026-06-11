ALTER TABLE asset_assignment
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(500);
