-- Booking, lot B2b : reports et changements marques sur la reservation.

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS modification_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_modification VARCHAR(300);
