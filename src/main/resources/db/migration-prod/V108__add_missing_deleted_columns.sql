-- resource_pricing_rule: entity maps deleted but column was never added
ALTER TABLE resource_pricing_rule
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_resource_pricing_rule_deleted ON resource_pricing_rule(deleted);