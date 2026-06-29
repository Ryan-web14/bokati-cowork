-- V170: KYC front/back support, per-case requirements, case-scoped review

-- 1. Add requires_back_side to document_type
ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS requires_back_side BOOLEAN DEFAULT FALSE;

-- 2. Add back_document_id to kyc_document
ALTER TABLE kyc_document
    ADD COLUMN IF NOT EXISTS back_document_id BIGINT;

ALTER TABLE kyc_document
    ADD CONSTRAINT fk_kyc_document_back_document
        FOREIGN KEY (back_document_id) REFERENCES document (id);

-- 3. Create kyc_case_requirement table
CREATE TABLE IF NOT EXISTS kyc_case_requirement
(
    id                 BIGINT       NOT NULL,
    kyc_case_id        BIGINT       NOT NULL,
    document_type_code VARCHAR(150) NOT NULL,
    document_type_name VARCHAR(200),
    required           BOOLEAN      NOT NULL DEFAULT TRUE,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_kyc_case_requirement PRIMARY KEY (id),
    CONSTRAINT fk_kyc_case_requirement_case FOREIGN KEY (kyc_case_id) REFERENCES kyc_case (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_case_requirement_case_type
    ON kyc_case_requirement (kyc_case_id, document_type_code);

CREATE INDEX IF NOT EXISTS idx_kyc_case_requirement_case
    ON kyc_case_requirement (kyc_case_id);

-- 4. Seed: mark identity documents as requiring back side
UPDATE document_type
SET requires_back_side = TRUE
WHERE code IN ('MEMBER_ID_CARD');

-- 5. Add new document types
INSERT INTO document_type (id, code, name, category, owner_type, description, required,
                           requires_expiry_date, requires_review, requires_signature, active,
                           multiple_allowed, requires_back_side, allowed_mime_types, max_file_size_bytes)
SELECT 170001, 'MEMBER_DRIVERS_LICENSE', 'Permis de conduire', 'KYC', 'MEMBER',
       'Permis de conduire du membre', FALSE, TRUE, TRUE, FALSE, TRUE,
       FALSE, TRUE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_DRIVERS_LICENSE');

INSERT INTO document_type (id, code, name, category, owner_type, description, required,
                           requires_expiry_date, requires_review, requires_signature, active,
                           multiple_allowed, requires_back_side, allowed_mime_types, max_file_size_bytes)
SELECT 170002, 'MEMBER_NIU', 'Numéro d''Identification Unique (NIU)', 'KYC', 'MEMBER',
       'Numéro d''identification unique du membre', FALSE, FALSE, TRUE, FALSE, TRUE,
       FALSE, FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_NIU');
