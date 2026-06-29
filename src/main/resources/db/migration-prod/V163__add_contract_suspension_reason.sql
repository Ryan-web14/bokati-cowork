-- Contract: add suspension_reason, renewal tracking, review fields
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS renewed_from_code VARCHAR(120);
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS review_comment TEXT;

UPDATE contract_record SET suspension_reason = termination_reason, termination_reason = NULL
WHERE status = 'SUSPENDED' AND termination_reason IS NOT NULL;

-- Contract templates table
CREATE TABLE IF NOT EXISTS contract_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    language VARCHAR(10) DEFAULT 'fr',
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO contract_template (code, name, description) VALUES
    ('membership-agreement', 'Membership Agreement', 'Standard membership contract template'),
    ('business-service-agreement', 'Business Service Agreement', 'Business service contract template'),
    ('contrat-domiciliation', 'Contrat de domiciliation', 'Domiciliation contract template'),
    ('subscription-pass-non-refundable', 'Subscription / Pass Non Refundable Agreement', 'Non refundable subscription, pass and addon contract template')
ON CONFLICT (code) DO NOTHING;
