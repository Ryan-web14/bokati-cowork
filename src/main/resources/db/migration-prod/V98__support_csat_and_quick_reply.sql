ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS csat_score         INTEGER,
    ADD COLUMN IF NOT EXISTS csat_comment        TEXT,
    ADD COLUMN IF NOT EXISTS csat_submitted_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS csat_email_sent_at  TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS support_quick_reply (
    id          BIGSERIAL    PRIMARY KEY,
    category    VARCHAR(60)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_support_quick_reply_category
    ON support_quick_reply (category)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_support_ticket_closed_csat
    ON support_ticket (closed_at, csat_email_sent_at)
    WHERE status = 'CLOSED';

CREATE INDEX IF NOT EXISTS idx_support_ticket_sla
    ON support_ticket (first_response_due_at, resolution_due_at, status);
