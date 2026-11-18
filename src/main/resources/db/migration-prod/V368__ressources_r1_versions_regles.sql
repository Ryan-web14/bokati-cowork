-- Ressources, lot R1 : versions des regles tarifaires (photo complete a chaque changement).
CREATE TABLE IF NOT EXISTS resource_pricing_rule_version (
    id            BIGINT PRIMARY KEY,
    rule_short_id BIGINT       NOT NULL,
    resource_code VARCHAR(80),
    version       INTEGER      NOT NULL,
    change_type   VARCHAR(20)  NOT NULL,
    snapshot      TEXT,
    changed_by    VARCHAR(150),
    changed_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_resource_pricing_rule_version_rule ON resource_pricing_rule_version (rule_short_id, version);
