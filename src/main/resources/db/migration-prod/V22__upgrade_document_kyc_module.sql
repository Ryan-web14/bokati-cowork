ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS multiple_allowed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allowed_mime_types TEXT,
    ADD COLUMN IF NOT EXISTS max_file_size_bytes BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'document_type_owner_type_check'
    ) THEN
        ALTER TABLE document_type DROP CONSTRAINT document_type_owner_type_check;
    END IF;

    ALTER TABLE document_type
        ADD CONSTRAINT document_type_owner_type_check
        CHECK (
            owner_type IS NULL OR owner_type IN (
                'CUSTOMER',
                'MEMBER',
                'BUSINESS',
                'CONTRACT',
                'INVOICE',
                'PAYMENT',
                'PROPOSAL',
                'ASSET'
            )
        );
END $$;

ALTER TABLE document
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(150),
    ADD COLUMN IF NOT EXISTS checksum_sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS current_version_number INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

UPDATE document
SET deleted = FALSE
WHERE deleted IS NULL;

ALTER TABLE document
    ALTER COLUMN deleted SET DEFAULT FALSE;

ALTER TABLE document_review
    ADD COLUMN IF NOT EXISTS version_number INTEGER,
    ADD COLUMN IF NOT EXISTS rejection_reason_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS rejection_reason_detail TEXT;

ALTER TABLE document_signature
    ADD COLUMN IF NOT EXISTS signature_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS user_agent TEXT;

ALTER TABLE document_requirement
    ADD COLUMN IF NOT EXISTS customer_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS business_legal_form VARCHAR(80);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'document_requirement_owner_type_check'
    ) THEN
        ALTER TABLE document_requirement DROP CONSTRAINT document_requirement_owner_type_check;
    END IF;

    ALTER TABLE document_requirement
        ADD CONSTRAINT document_requirement_owner_type_check
        CHECK (
            owner_type IN (
                'CUSTOMER',
                'MEMBER',
                'BUSINESS',
                'CONTRACT',
                'INVOICE',
                'PAYMENT',
                'PROPOSAL',
                'ASSET'
            )
        );
END $$;

CREATE TABLE IF NOT EXISTS document_version (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    storage_provider VARCHAR(80) NOT NULL,
    storage_path TEXT NOT NULL,
    original_file_name VARCHAR(350) NOT NULL,
    stored_file_name VARCHAR(350) NOT NULL,
    mime_type_declared VARCHAR(150),
    mime_type_detected VARCHAR(150) NOT NULL,
    file_extension VARCHAR(20),
    checksum_sha256 VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    upload_status VARCHAR(30) NOT NULL,
    antivirus_status VARCHAR(30) NOT NULL,
    uploaded_by BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_document_version_document FOREIGN KEY (document_id) REFERENCES document(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_document_version_number
    ON document_version(document_id, version_number);

CREATE INDEX IF NOT EXISTS idx_document_version_document
    ON document_version(document_id);

CREATE TABLE IF NOT EXISTS kyc_case (
    id BIGINT PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    owner_type VARCHAR(30) NOT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMP,
    completed_at TIMESTAMP,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    decision_comment TEXT
);

ALTER TABLE kyc_document
    ADD COLUMN IF NOT EXISTS kyc_case_id BIGINT,
    ADD COLUMN IF NOT EXISTS owner_id BIGINT;

UPDATE kyc_document
SET owner_id = COALESCE(member_id, customer_id)
WHERE owner_id IS NULL;

ALTER TABLE kyc_document
    ALTER COLUMN owner_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_kyc_document_case'
    ) THEN
        ALTER TABLE kyc_document
            ADD CONSTRAINT fk_kyc_document_case
            FOREIGN KEY (kyc_case_id) REFERENCES kyc_case(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_kyc_document_document'
    ) THEN
        ALTER TABLE kyc_document
            ADD CONSTRAINT fk_kyc_document_document
            FOREIGN KEY (document_id) REFERENCES document(id);
    END IF;
END $$;

INSERT INTO sequence_definition (
    id, code, name, description, prefix, suffix, pattern,
    padding, initial_value, increment_step, reset_policy, enabled, system_managed,
    created_at, updated_at
)
SELECT 75, 'kyccase', 'Dossier KYC', 'Séquence des dossiers KYC', 'KCS', null, '{PREFIX}-{YYYY}{MM}-{SEQ}',
       6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM sequence_definition WHERE code = 'kyccase'
);

INSERT INTO sequence_definition (
    id, code, name, description, prefix, suffix, pattern,
    padding, initial_value, increment_step, reset_policy, enabled, system_managed,
    created_at, updated_at
)
SELECT 76, 'document_version', 'Version de document', 'Séquence technique des versions documentaires', 'DVR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}',
       6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM sequence_definition WHERE code = 'document_version'
);

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1001, 'MEMBER_ID_CARD', 'Member ID Card', 'KYC', 'MEMBER',
       'Primary member identity document', TRUE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_ID_CARD');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1002, 'MEMBER_PASSPORT', 'Member Passport', 'KYC', 'MEMBER',
       'Alternative member identity document', TRUE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_PASSPORT');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1003, 'BUSINESS_RCCM', 'Business RCCM', 'KYC', 'BUSINESS',
       'Business registration certificate', TRUE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'BUSINESS_RCCM');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1004, 'BUSINESS_NIU', 'Business NIU', 'KYC', 'BUSINESS',
       'Business tax identification document', TRUE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'BUSINESS_NIU');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1005, 'CONTRACT_SIGNED_COPY', 'Signed Contract Copy', 'LEGAL', 'CONTRACT',
       'Signed final contract copy', TRUE, FALSE, TRUE, TRUE, TRUE,
       FALSE, 'application/pdf', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CONTRACT_SIGNED_COPY');

INSERT INTO document_requirement (id, owner_type, document_type_code, document_type_name, required, active)
SELECT 2001, 'MEMBER', 'MEMBER_ID_CARD', 'Member ID Card', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2001);

INSERT INTO document_requirement (id, owner_type, document_type_code, document_type_name, required, active)
SELECT 2002, 'BUSINESS', 'BUSINESS_RCCM', 'Business RCCM', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2002);

INSERT INTO document_requirement (id, owner_type, document_type_code, document_type_name, required, active)
SELECT 2003, 'BUSINESS', 'BUSINESS_NIU', 'Business NIU', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2003);
