-- Tranche 8 · les parametres de la plateforme. Une ligne n'existe que pour une valeur modifiee :
-- sans elle, c'est application.yml qui fait foi.

CREATE TABLE IF NOT EXISTS platform_setting (
    setting_key VARCHAR(160) PRIMARY KEY,
    value       TEXT         NOT NULL,
    updated_by  VARCHAR(150),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Les horaires propres a une ressource · sans ligne, elle suit les horaires globaux.
CREATE TABLE IF NOT EXISTS resource_opening_hours (
    id          BIGINT PRIMARY KEY,
    resource_id BIGINT      NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    open_time   TIME        NOT NULL,
    close_time  TIME        NOT NULL,
    updated_by  VARCHAR(150),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_resource_opening_hours_range CHECK (open_time < close_time),
    CONSTRAINT ck_resource_opening_hours_day
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);
CREATE INDEX IF NOT EXISTS idx_resource_opening_hours_resource ON resource_opening_hours (resource_id);
