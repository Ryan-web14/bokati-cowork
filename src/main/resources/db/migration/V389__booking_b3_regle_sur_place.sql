-- Booking, lot B3 : rendez-vous sans paiement, regle sur place (decisions D1 a D3).
-- Marque de la reservation, rappel avant l'echeance, option par client posee par l'equipe.

ALTER TABLE booking ADD COLUMN IF NOT EXISTS pay_on_site BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS payment_reminder_sent_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS booking_pay_on_site_grant
(
    id                    BIGINT PRIMARY KEY,
    owner_type            VARCHAR(30)  NOT NULL,
    owner_code            VARCHAR(300) NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    reason                VARCHAR(500),
    revoked_automatically BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by            VARCHAR(180),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_booking_pay_on_site_grant UNIQUE (owner_type, owner_code)
);
