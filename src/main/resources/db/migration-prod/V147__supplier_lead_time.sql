ALTER TABLE inventory_supplier
    ADD COLUMN IF NOT EXISTS average_lead_time_days INTEGER;

CREATE TABLE IF NOT EXISTS inventory_supplier_item (
    id                  BIGINT          NOT NULL,
    supplier_id         BIGINT          NOT NULL REFERENCES inventory_supplier(id),
    item_id             BIGINT          NOT NULL REFERENCES inventory_item(id),
    supplier_item_code  VARCHAR(100),
    unit_price          BIGINT,
    lead_time_days      INTEGER,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    preferred           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_inventory_supplier_item PRIMARY KEY (id),
    CONSTRAINT uq_supplier_item          UNIQUE (supplier_id, item_id)
);

CREATE INDEX IF NOT EXISTS idx_supplier_item_item     ON inventory_supplier_item(item_id);
CREATE INDEX IF NOT EXISTS idx_supplier_item_supplier ON inventory_supplier_item(supplier_id);
