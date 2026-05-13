ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS booking_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS portal_visible BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS display_order INT,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

UPDATE resource
SET booking_enabled = TRUE
WHERE booking_enabled IS NULL;

UPDATE resource
SET portal_visible = TRUE
WHERE portal_visible IS NULL;

UPDATE resource
SET deleted = FALSE
WHERE deleted IS NULL;

ALTER TABLE resource
    ALTER COLUMN booking_enabled SET DEFAULT TRUE,
    ALTER COLUMN portal_visible SET DEFAULT TRUE,
    ALTER COLUMN deleted SET DEFAULT FALSE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_closure'
          AND column_name = 'ended_at'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_closure'
          AND column_name = 'end_at'
    ) THEN
        ALTER TABLE resource_closure RENAME COLUMN ended_at TO end_at;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_closure'
          AND column_name = 'started_at'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_closure'
          AND column_name = 'start_at'
    ) THEN
        ALTER TABLE resource_closure RENAME COLUMN started_at TO start_at;
    END IF;
END $$;

ALTER TABLE resource_closure
    ADD COLUMN IF NOT EXISTS start_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS end_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reason VARCHAR(350),
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE resource_closure
SET active = TRUE
WHERE active IS NULL;

UPDATE resource_closure
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE resource_closure
SET updated_at = NOW()
WHERE updated_at IS NULL;

ALTER TABLE resource_closure
    ALTER COLUMN active SET DEFAULT TRUE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_availability'
          AND column_name = 'start_at'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_availability'
          AND column_name = 'started_at'
    ) THEN
        ALTER TABLE resource_availability RENAME COLUMN start_at TO started_at;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_availability'
          AND column_name = 'end_at'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_availability'
          AND column_name = 'ended_at'
    ) THEN
        ALTER TABLE resource_availability RENAME COLUMN end_at TO ended_at;
    END IF;
END $$;

ALTER TABLE resource_availability
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS available BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS slot_duration_minutes INT DEFAULT 30,
    ADD COLUMN IF NOT EXISTS total_capacity INT,
    ADD COLUMN IF NOT EXISTS remaining_capacity INT,
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE resource_availability
SET available = TRUE
WHERE available IS NULL;

UPDATE resource_availability
SET slot_duration_minutes = 30
WHERE slot_duration_minutes IS NULL;

UPDATE resource_availability
SET total_capacity = COALESCE(total_capacity, 1)
WHERE total_capacity IS NULL;

UPDATE resource_availability
SET remaining_capacity = COALESCE(remaining_capacity, total_capacity, 1)
WHERE remaining_capacity IS NULL;

UPDATE resource_availability
SET active = TRUE
WHERE active IS NULL;

UPDATE resource_availability
SET version = 0
WHERE version IS NULL;

UPDATE resource_availability
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE resource_availability
SET updated_at = NOW()
WHERE updated_at IS NULL;

ALTER TABLE resource_availability
    ALTER COLUMN available SET DEFAULT TRUE,
    ALTER COLUMN slot_duration_minutes SET DEFAULT 30,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN version SET DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_availability_slot
    ON resource_availability(resource_id, started_at, ended_at);

ALTER TABLE resource_pricing_rule
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

UPDATE resource_pricing_rule
SET active = TRUE
WHERE active IS NULL;

UPDATE resource_pricing_rule
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE resource_pricing_rule
SET updated_at = NOW()
WHERE updated_at IS NULL;

ALTER TABLE resource_pricing_rule
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE resource_group
    ADD COLUMN IF NOT EXISTS portal_visible BOOLEAN DEFAULT FALSE;

UPDATE resource_group
SET portal_visible = FALSE
WHERE portal_visible IS NULL;

ALTER TABLE resource_group
    ALTER COLUMN portal_visible SET DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_resource_group_id ON resource(group_id);
CREATE INDEX IF NOT EXISTS idx_resource_policy_id ON resource(policy_id);
CREATE INDEX IF NOT EXISTS idx_resource_code ON resource(code);
CREATE INDEX IF NOT EXISTS idx_resource_closure_resource_id ON resource_closure(resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_pricing_rule_resource_id ON resource_pricing_rule(resource_id);
