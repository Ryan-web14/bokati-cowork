CREATE TABLE IF NOT EXISTS inventory_item_serial (
    id              BIGINT          NOT NULL,
    item_id         BIGINT          NOT NULL REFERENCES inventory_item(id),
    location_id     BIGINT          NOT NULL REFERENCES inventory_location(id),
    serial_number   VARCHAR(200)    NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE',
    lot_id          BIGINT          REFERENCES stock_lot(id),
    received_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    issued_at       TIMESTAMPTZ,
    CONSTRAINT pk_inventory_item_serial PRIMARY KEY (id),
    CONSTRAINT uq_item_serial UNIQUE (item_id, serial_number)
);

CREATE INDEX IF NOT EXISTS idx_serial_item_location ON inventory_item_serial(item_id, location_id);
CREATE INDEX IF NOT EXISTS idx_serial_status ON inventory_item_serial(status);
