-- Booking, lot B5 : rendez-vous pris au telephone (decision D5) : prospect du CRM relie, note d'appel.

ALTER TABLE booking ADD COLUMN IF NOT EXISTS lead_number VARCHAR(80);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS call_note VARCHAR(1000);

CREATE INDEX IF NOT EXISTS ix_booking_lead_number ON booking (lead_number) WHERE lead_number IS NOT NULL;
