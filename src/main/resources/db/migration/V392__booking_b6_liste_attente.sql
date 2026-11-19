-- Booking, lot B6 : liste d'attente refaite (reservation creee a l'acceptation, courriel d'offre).

ALTER TABLE booking_waitlist_entry ADD COLUMN IF NOT EXISTS booking_number VARCHAR(120);
ALTER TABLE booking_waitlist_entry ADD COLUMN IF NOT EXISTS notified_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_booking_waitlist_status_start ON booking_waitlist_entry (status, started_at);
CREATE INDEX IF NOT EXISTS ix_booking_notification_reminder ON booking_notification_outbox (booking_id, notification_type, scheduled_at);
