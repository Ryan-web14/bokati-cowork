ALTER TABLE kyc_case
    ADD COLUMN IF NOT EXISTS assigned_to BIGINT,
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sla_deadline TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_reminder_sent_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reminder_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS risk_level VARCHAR(30) NOT NULL DEFAULT 'LOW',
    ADD COLUMN IF NOT EXISTS kyc_level INTEGER NOT NULL DEFAULT 1;

ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS auto_approve BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_approve_after_days INTEGER;

ALTER TABLE subscription_plan
    ADD COLUMN IF NOT EXISTS required_kyc_level INTEGER NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS kyc_case_note (
    id BIGINT PRIMARY KEY,
    kyc_case_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    internal BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_kyc_case_note_case FOREIGN KEY (kyc_case_id) REFERENCES kyc_case(id)
);

CREATE INDEX IF NOT EXISTS idx_kyc_case_note_case_created
    ON kyc_case_note(kyc_case_id, created_at DESC);

CREATE TABLE IF NOT EXISTS kyc_document_ocr_result (
    id BIGINT PRIMARY KEY,
    kyc_document_id BIGINT NOT NULL UNIQUE,
    extracted_first_name VARCHAR(150),
    extracted_last_name VARCHAR(150),
    extracted_date_of_birth DATE,
    extracted_expiry_date DATE,
    extracted_document_number VARCHAR(150),
    extracted_nationality VARCHAR(150),
    confidence_score NUMERIC(5,4),
    raw_ocr_json TEXT,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_kyc_ocr_document FOREIGN KEY (kyc_document_id) REFERENCES kyc_document(id)
);

CREATE TABLE IF NOT EXISTS kyc_cross_validation_rule (
    id BIGINT PRIMARY KEY,
    document_type_code1 VARCHAR(150) NOT NULL,
    document_type_code2 VARCHAR(150) NOT NULL,
    field_to_compare VARCHAR(80) NOT NULL,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_kyc_case_status_review
    ON kyc_case(status, submitted_at, assigned_to);

CREATE INDEX IF NOT EXISTS idx_kyc_case_risk_level
    ON kyc_case(risk_level);

CREATE INDEX IF NOT EXISTS idx_kyc_document_expiry_status
    ON kyc_document(expiry_date, status);
