-- Lot B0 du module booking : qui a cree la reservation, et par quel canal.

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(150),
    ADD COLUMN IF NOT EXISTS channel    VARCHAR(20);

CREATE INDEX IF NOT EXISTS ix_booking_channel ON booking (channel);
