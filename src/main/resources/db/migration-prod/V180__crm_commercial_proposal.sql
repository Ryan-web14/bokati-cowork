CREATE TABLE IF NOT EXISTS crm_commercial_proposal (
    id                          BIGSERIAL       PRIMARY KEY,
    proposal_number             VARCHAR(80)     NOT NULL UNIQUE,
    opportunity_id              BIGINT          REFERENCES crm_opportunity(id),
    lead_id                     BIGINT          REFERENCES crm_lead(id),
    title                       VARCHAR(300)    NOT NULL,
    recipient_type              VARCHAR(60),
    recipient_code              VARCHAR(120),
    recipient_name              VARCHAR(200),
    recipient_email             VARCHAR(200),
    status                      VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    valid_until                 DATE,
    subtotal_amount             NUMERIC(19,4)   NOT NULL DEFAULT 0,
    tax_amount                  NUMERIC(19,4)   NOT NULL DEFAULT 0,
    total_amount                NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency                    VARCHAR(10),
    notes                       TEXT,
    internal_notes              TEXT,
    converted_invoice_number    VARCHAR(120),
    converted_contract_code     VARCHAR(120),
    converted_subscription_number VARCHAR(120),
    converted_pass_number       VARCHAR(120),
    sent_at                     TIMESTAMPTZ,
    viewed_at                   TIMESTAMPTZ,
    accepted_at                 TIMESTAMPTZ,
    rejected_at                 TIMESTAMPTZ,
    rejection_reason            TEXT,
    document_code               VARCHAR(120),
    created_by                  BIGINT,
    assigned_to                 BIGINT,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_crm_proposal_status CHECK (status IN (
        'DRAFT','SENT','VIEWED','ACCEPTED','REJECTED','EXPIRED','CONVERTED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_crm_proposal_number      ON crm_commercial_proposal(proposal_number);
CREATE INDEX IF NOT EXISTS idx_crm_proposal_opportunity ON crm_commercial_proposal(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_crm_proposal_lead        ON crm_commercial_proposal(lead_id);
CREATE INDEX IF NOT EXISTS idx_crm_proposal_status      ON crm_commercial_proposal(status);
