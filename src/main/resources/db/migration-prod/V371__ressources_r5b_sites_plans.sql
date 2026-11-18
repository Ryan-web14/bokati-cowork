-- Lot R5b des ressources : sites, etages et plans, placement des ressources, flux iCalendar.

CREATE TABLE IF NOT EXISTS resource_site (
    id                 BIGINT PRIMARY KEY,
    code               VARCHAR(40)  NOT NULL UNIQUE,
    name               VARCHAR(160) NOT NULL,
    address            VARCHAR(300),
    city               VARCHAR(120),
    phone              VARCHAR(40),
    time_zone          VARCHAR(60)  NOT NULL DEFAULT 'Africa/Brazzaville',
    opening_hours_note VARCHAR(300),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS resource_floor (
    id                 BIGINT PRIMARY KEY,
    code               VARCHAR(40)  NOT NULL UNIQUE,
    site_id            BIGINT       NOT NULL REFERENCES resource_site (id),
    name               VARCHAR(160) NOT NULL,
    level              INTEGER,
    plan_document_code VARCHAR(60),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS site_id    BIGINT REFERENCES resource_site (id),
    ADD COLUMN IF NOT EXISTS floor_id   BIGINT REFERENCES resource_floor (id),
    ADD COLUMN IF NOT EXISTS plan_x     NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS plan_y     NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS ical_token VARCHAR(60);

CREATE INDEX IF NOT EXISTS ix_resource_site ON resource (site_id);
CREATE INDEX IF NOT EXISTS ix_resource_floor ON resource (floor_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_resource_ical_token ON resource (ical_token) WHERE ical_token IS NOT NULL;

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3770001, 'resource_site', 'Site', 'Séquence des sites des ressources', 'SIT', NULL,
       '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'resource_site');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3770002, 'resource_floor', 'Étage', 'Séquence des étages des sites', 'FLR', NULL,
       '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'resource_floor');
