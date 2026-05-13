ALTER TABLE asset
    ADD COLUMN IF NOT EXISTS useful_life_months INTEGER,
    ADD COLUMN IF NOT EXISTS residual_value BIGINT;

ALTER TABLE asset_assignment
    ADD COLUMN IF NOT EXISTS checkout_condition VARCHAR(40),
    ADD COLUMN IF NOT EXISTS checkout_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS return_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS receiver_signature_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS asset_location_history (
    id BIGINT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    from_location_id BIGINT,
    to_location_id BIGINT,
    changed_by VARCHAR(120),
    reason TEXT,
    changed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_asset_location_history_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT fk_asset_location_history_from_location FOREIGN KEY (from_location_id) REFERENCES inventory_location (id),
    CONSTRAINT fk_asset_location_history_to_location FOREIGN KEY (to_location_id) REFERENCES inventory_location (id)
);

CREATE INDEX IF NOT EXISTS idx_asset_location_history_asset ON asset_location_history (asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_location_history_changed_at ON asset_location_history (changed_at);
