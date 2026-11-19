-- Booking, lot B9c : acompte qui confirme la reservation (fiabilite du client ou demande de l'equipe).

ALTER TABLE booking ADD COLUMN IF NOT EXISTS deposit_amount NUMERIC(19, 2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS deposit_paid_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_booking_owner_cancelled ON booking (owner_type, owner_code, status, started_at);
