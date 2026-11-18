-- Lot R5c des ressources : participants, caution, depassement et non-presentation, supplements
-- des reservations, lots de ressources.

ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS included_participants   INTEGER,
    ADD COLUMN IF NOT EXISTS extra_participant_price INTEGER,
    ADD COLUMN IF NOT EXISTS deposit_amount          INTEGER;

ALTER TABLE resource_policy
    ADD COLUMN IF NOT EXISTS overrun_grace_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS no_show_fee           INTEGER;

ALTER TABLE booking_line
    ADD COLUMN IF NOT EXISTS item_code VARCHAR(80);

CREATE TABLE IF NOT EXISTS resource_bundle (
    id               BIGINT PRIMARY KEY,
    code             VARCHAR(40)  NOT NULL UNIQUE,
    name             VARCHAR(160) NOT NULL,
    description      VARCHAR(500),
    discount_percent NUMERIC(5, 2),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_resource_bundle_discount CHECK (discount_percent IS NULL OR (discount_percent >= 0 AND discount_percent <= 100))
);

CREATE TABLE IF NOT EXISTS resource_bundle_item (
    id          BIGINT PRIMARY KEY,
    bundle_id   BIGINT  NOT NULL REFERENCES resource_bundle (id) ON DELETE CASCADE,
    resource_id BIGINT  NOT NULL REFERENCES resource (id),
    quantity    INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS ix_resource_bundle_item_bundle ON resource_bundle_item (bundle_id);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3780001, 'resource_bundle', 'Lot de ressources', 'Séquence des lots de ressources réservées ensemble', 'LOT', NULL,
       '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'resource_bundle');
