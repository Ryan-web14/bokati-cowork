CREATE TABLE IF NOT EXISTS crm_proposal_line (
    id                  BIGSERIAL       PRIMARY KEY,
    proposal_id         BIGINT          NOT NULL REFERENCES crm_commercial_proposal(id) ON DELETE CASCADE,
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    service_type        VARCHAR(60),
    service_ref_code    VARCHAR(120),
    label               VARCHAR(300)    NOT NULL,
    description         TEXT,
    quantity            NUMERIC(10,2)   NOT NULL DEFAULT 1,
    unit_price          NUMERIC(19,4)   NOT NULL DEFAULT 0,
    discount_percent    NUMERIC(5,2)    NOT NULL DEFAULT 0,
    tax_rate            NUMERIC(5,4)    NOT NULL DEFAULT 0,
    tax_included        BOOLEAN         NOT NULL DEFAULT FALSE,
    subtotal_amount     NUMERIC(19,4)   NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(19,4)   NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency            VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_crm_proposal_line_proposal ON crm_proposal_line(proposal_id);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 181001, 'crm_proposal', 'Proposition commerciale', 'Séquence des propositions CRM', 'PROP', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'crm_proposal');
