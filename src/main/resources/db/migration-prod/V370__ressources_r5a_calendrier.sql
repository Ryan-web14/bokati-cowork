-- Lot R5a des ressources : battement et avance maximale des politiques, nature et repetition des
-- fermetures, jours feries des regles tarifaires, jours particuliers du calendrier.

ALTER TABLE resource_policy
    ADD COLUMN IF NOT EXISTS buffer_minutes   INTEGER,
    ADD COLUMN IF NOT EXISTS max_advance_days INTEGER;

ALTER TABLE resource_closure
    ADD COLUMN IF NOT EXISTS kind             VARCHAR(20) NOT NULL DEFAULT 'CLOSURE',
    ADD COLUMN IF NOT EXISTS recurrence       VARCHAR(10) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS recurrence_until DATE;

ALTER TABLE resource_pricing_rule
    ADD COLUMN IF NOT EXISTS holiday_mode VARCHAR(10);

CREATE TABLE IF NOT EXISTS resource_calendar_day (
    id            BIGINT PRIMARY KEY,
    day_date      DATE         NOT NULL,
    resource_id   BIGINT REFERENCES resource (id) ON DELETE CASCADE,
    kind          VARCHAR(20)  NOT NULL,
    label         VARCHAR(160),
    opening_hours VARCHAR(200),
    created_by    VARCHAR(150),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_resource_calendar_day_kind CHECK (kind IN ('HOLIDAY', 'SPECIAL_HOURS', 'CLOSED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_resource_calendar_day_general
    ON resource_calendar_day (day_date) WHERE resource_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_resource_calendar_day_resource
    ON resource_calendar_day (day_date, resource_id) WHERE resource_id IS NOT NULL;
