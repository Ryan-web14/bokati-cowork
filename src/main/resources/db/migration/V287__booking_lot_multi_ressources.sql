-- Reservation multi-ressources : un lot regroupe les reservations faites d'un seul geste par
-- l'equipe, pour un ou plusieurs membres sur une ou plusieurs ressources. Chaque membre recoit une
-- seule facture pour l'ensemble de ses reservations du lot.

CREATE TABLE IF NOT EXISTS booking_batch (
    id                   BIGINT        PRIMARY KEY,
    batch_number         VARCHAR(80)   NOT NULL UNIQUE,
    idempotency_key      VARCHAR(180)  UNIQUE,
    payment_mode         VARCHAR(40)   NOT NULL,
    billing_company_code VARCHAR(120),
    booking_count        INTEGER       NOT NULL,
    member_count         INTEGER       NOT NULL,
    notes                TEXT,
    created_by           VARCHAR(180),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_booking_batch_count CHECK (booking_count > 0)
);

ALTER TABLE booking ADD COLUMN IF NOT EXISTS batch_number VARCHAR(80);
CREATE INDEX IF NOT EXISTS idx_booking_batch_number ON booking(batch_number);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2870001, 'booking_batch', 'Lot de reservations', 'Sequence des lots de reservations multi-ressources', 'BKL', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'booking_batch');
