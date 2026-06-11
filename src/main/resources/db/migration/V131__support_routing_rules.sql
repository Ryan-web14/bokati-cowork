CREATE TABLE IF NOT EXISTS support_routing_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    category VARCHAR(40),
    priority VARCHAR(40),
    owner_type VARCHAR(40),
    related_type VARCHAR(40),
    assigned_to BIGINT,
    team_code VARCHAR(60),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_support_routing_rule_active_sort ON support_routing_rule (active, sort_order);
