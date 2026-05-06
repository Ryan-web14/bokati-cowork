-- Colonnes supplémentaires sur crm_lead
ALTER TABLE crm_lead
    ADD COLUMN IF NOT EXISTS lead_number           VARCHAR(80) UNIQUE,
    ADD COLUMN IF NOT EXISTS last_activity_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS dormant_alert_sent_at TIMESTAMPTZ;

-- Remplir lead_number pour les lignes existantes
UPDATE crm_lead SET lead_number = 'LDN-' || id WHERE lead_number IS NULL;

-- Table opportunités
CREATE TABLE IF NOT EXISTS crm_opportunity (
    id                   BIGSERIAL    PRIMARY KEY,
    opportunity_number   VARCHAR(80)  NOT NULL UNIQUE,
    lead_id              BIGINT       NOT NULL REFERENCES crm_lead(id),
    title                VARCHAR(200) NOT NULL,
    estimated_amount     NUMERIC(19,4),
    probability          INTEGER,
    stage                VARCHAR(60)  NOT NULL DEFAULT 'OPEN',
    expected_close_date  DATE,
    assigned_to          BIGINT,
    notes                TEXT,
    won_at               TIMESTAMPTZ,
    lost_at              TIMESTAMPTZ,
    lost_reason          TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_crm_lead_number       ON crm_lead(lead_number);
CREATE INDEX IF NOT EXISTS idx_crm_lead_stage        ON crm_lead(stage);
CREATE INDEX IF NOT EXISTS idx_crm_lead_assigned     ON crm_lead(assigned_to);
CREATE INDEX IF NOT EXISTS idx_crm_lead_last_activity ON crm_lead(last_activity_at);
CREATE INDEX IF NOT EXISTS idx_crm_opp_lead          ON crm_opportunity(lead_id);
CREATE INDEX IF NOT EXISTS idx_crm_opp_stage         ON crm_opportunity(stage);
CREATE INDEX IF NOT EXISTS idx_crm_opp_assigned      ON crm_opportunity(assigned_to);
