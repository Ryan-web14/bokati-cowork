-- Journal d'audit fiscal SEFC
CREATE TABLE fiscal_audit_log (
    id           BIGINT        PRIMARY KEY,
    actor_code   VARCHAR(120),
    action       VARCHAR(80)   NOT NULL,
    entity_type  VARCHAR(60)   NOT NULL,
    entity_id    VARCHAR(120)  NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    ip_address   VARCHAR(60),
    user_agent   TEXT,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fiscal_audit_entity ON fiscal_audit_log (entity_type, entity_id);
CREATE INDEX idx_fiscal_audit_action ON fiscal_audit_log (action, created_at);
