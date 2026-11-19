-- Booking, lots B9a et B9b : reponse au rappel (je viens, j'annule), liberation anticipee,
-- planning du jour et series (index).

ALTER TABLE booking ADD COLUMN IF NOT EXISTS attendance_answer VARCHAR(20);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS attendance_answered_at TIMESTAMPTZ;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS released_early_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_booking_started_at_day ON booking (started_at) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS ix_booking_recurrence_group ON booking (recurrence_group_number) WHERE recurrence_group_number IS NOT NULL;
