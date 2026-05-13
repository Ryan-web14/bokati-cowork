ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180),
    ADD COLUMN IF NOT EXISTS hold_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS recurrence_group_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS billable_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS checked_out_at TIMESTAMP;

ALTER TABLE booking_line
    ADD COLUMN IF NOT EXISTS billable_number VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS uk_booking_idempotency_not_null
ON booking(idempotency_key)
WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_booking_hold_number ON booking(hold_number);
CREATE INDEX IF NOT EXISTS idx_booking_recurrence_group ON booking(recurrence_group_number);
CREATE INDEX IF NOT EXISTS idx_booking_billable_number ON booking(billable_number);
