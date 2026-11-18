-- Lot B10 : visibilite des ressources par client (tous, reservee, sur demande) et regles
-- tarifaires reservees a des clients (prix au cas par cas).

ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS visibility_mode VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

CREATE TABLE IF NOT EXISTS resource_audience (
    id            BIGINT PRIMARY KEY,
    resource_id   BIGINT      NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    audience_type VARCHAR(20) NOT NULL,
    audience_code VARCHAR(80) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_resource_audience_type CHECK (audience_type IN ('MEMBER', 'CUSTOMER', 'BUSINESS_ENTITY', 'SEGMENT', 'PLAN')),
    CONSTRAINT ux_resource_audience UNIQUE (resource_id, audience_type, audience_code)
);

CREATE INDEX IF NOT EXISTS ix_resource_audience_key ON resource_audience (audience_type, audience_code);

ALTER TABLE resource_pricing_rule
    ADD COLUMN IF NOT EXISTS audience VARCHAR(2000);
