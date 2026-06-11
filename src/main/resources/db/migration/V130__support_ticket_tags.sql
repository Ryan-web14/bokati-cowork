CREATE TABLE IF NOT EXISTS support_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS support_ticket_tag (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_ticket(id),
    tag_id BIGINT NOT NULL REFERENCES support_tag(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_support_ticket_tag UNIQUE (ticket_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_tag_ticket ON support_ticket_tag (ticket_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_tag_tag ON support_ticket_tag (tag_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_category_priority ON support_ticket (category, priority);
CREATE INDEX IF NOT EXISTS idx_support_ticket_related ON support_ticket (related_type, related_code);
