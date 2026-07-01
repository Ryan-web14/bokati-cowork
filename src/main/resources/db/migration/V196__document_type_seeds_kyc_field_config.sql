-- Seed per-field upload requirements for standard KYC document types

-- CNI — Carte Nationale d'Identité
UPDATE document_type
SET requires_document_number = TRUE,
    requires_issue_date      = TRUE,
    requires_expiry_date     = TRUE,
    requires_back_side       = TRUE,
    max_file_size_bytes      = 10485760
WHERE code = 'CNI';

-- PASSEPORT
UPDATE document_type
SET requires_document_number = TRUE,
    requires_issue_date      = TRUE,
    requires_expiry_date     = TRUE,
    requires_back_side       = FALSE,
    max_file_size_bytes      = 10485760
WHERE code = 'PASSEPORT';

-- NIU — Numéro d'Identification Unique (fiscal ID)
UPDATE document_type
SET requires_document_number = TRUE,
    requires_issue_date      = FALSE,
    requires_expiry_date     = TRUE,
    requires_back_side       = FALSE,
    max_file_size_bytes      = 10485760
WHERE code = 'NIU';

-- JUSTIFICATIF_DOMICILE — no structured fields, file only
UPDATE document_type
SET requires_document_number = FALSE,
    requires_issue_date      = FALSE,
    requires_expiry_date     = FALSE,
    requires_back_side       = FALSE,
    auto_approve             = TRUE,
    auto_approve_after_days  = 3,
    max_file_size_bytes      = 10485760
WHERE code = 'JUSTIFICATIF_DOMICILE';

-- REGISTRE_COMMERCE — trade register, number required
UPDATE document_type
SET requires_document_number = TRUE,
    requires_issue_date      = TRUE,
    requires_expiry_date     = FALSE,
    requires_back_side       = FALSE,
    max_file_size_bytes      = 10485760
WHERE code = 'REGISTRE_COMMERCE';

-- PHOTO_IDENTITE — portrait photo, no metadata
UPDATE document_type
SET requires_document_number = FALSE,
    requires_issue_date      = FALSE,
    requires_expiry_date     = FALSE,
    requires_back_side       = FALSE,
    auto_approve             = TRUE,
    auto_approve_after_days  = 2,
    max_file_size_bytes      = 5242880
WHERE code = 'PHOTO_IDENTITE';
