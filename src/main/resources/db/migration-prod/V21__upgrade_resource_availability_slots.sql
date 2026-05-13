ALTER TABLE resource_availability
    ADD COLUMN IF NOT EXISTS slot_duration_minutes INT NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS total_capacity INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS remaining_capacity INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE resource_availability
SET total_capacity = 1
WHERE total_capacity IS NULL;

UPDATE resource_availability
SET remaining_capacity = CASE
    WHEN available IS TRUE THEN total_capacity
    ELSE 0
END
WHERE remaining_capacity IS NULL OR remaining_capacity = 1;

-- Index deferred to V30 where started_at/ended_at columns are first added.
