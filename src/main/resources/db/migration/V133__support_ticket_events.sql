CREATE TABLE IF NOT EXISTS support_ticket_event (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_ticket(id),
    event_type VARCHAR(40) NOT NULL,
    actor_type VARCHAR(20),
    actor_id VARCHAR(120),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_event_ticket ON support_ticket_event (ticket_id, created_at);
CREATE INDEX IF NOT EXISTS idx_support_ticket_event_type ON support_ticket_event (event_type);
