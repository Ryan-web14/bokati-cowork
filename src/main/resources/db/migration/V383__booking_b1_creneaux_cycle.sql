-- Lot B1 du module booking : creneau retenu des la reservation en attente, echeance de paiement,
-- reglement constate, coordonnees des options d'invites.

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS slots_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS payment_due_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS paid_at        TIMESTAMPTZ;

-- Les reservations confirmees ou en cours retiennent deja leurs creneaux.
UPDATE booking SET slots_reserved = TRUE WHERE status IN ('CONFIRMED', 'IN_PROGRESS');

-- Reglement : jusqu'ici, confirmee valait reglee (hors droits d'abonnement et de pass).
UPDATE booking SET paid_at = confirmed_at
WHERE paid_at IS NULL AND confirmed_at IS NOT NULL
  AND status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED')
  AND (payment_mode IS NULL OR payment_mode NOT IN ('SUBSCRIPTION', 'PASS'));

CREATE INDEX IF NOT EXISTS ix_booking_payment_due ON booking (payment_due_at) WHERE status = 'PENDING_PAYMENT';

ALTER TABLE booking_hold
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(180),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(40);
