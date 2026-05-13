INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1006, 'CONTRACT_DRAFT', 'Contract Draft', 'LEGAL', NULL,
       'Generated contract draft PDF', FALSE, FALSE, FALSE, FALSE, TRUE,
       TRUE, 'application/pdf', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CONTRACT_DRAFT');
