-- Lot R5e des ressources : couts des ressources (marges).

CREATE TABLE IF NOT EXISTS resource_cost (
    id          BIGINT PRIMARY KEY,
    resource_id BIGINT         NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    kind        VARCHAR(20)    NOT NULL,
    basis       VARCHAR(20)    NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    label       VARCHAR(200),
    valid_from  DATE,
    valid_until DATE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_resource_cost_kind CHECK (kind IN ('RENT', 'CLEANING', 'ENERGY', 'DEPRECIATION', 'SERVICES', 'OTHER')),
    CONSTRAINT ck_resource_cost_basis CHECK (basis IN ('MONTHLY', 'PER_BOOKING', 'PER_HOUR')),
    CONSTRAINT ck_resource_cost_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS ix_resource_cost_resource ON resource_cost (resource_id);
