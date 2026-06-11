ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS escalation_level INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS escalation_reason TEXT,
    ADD COLUMN IF NOT EXISTS last_sla_alert_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_support_ticket_escalation_level ON support_ticket (escalation_level);
