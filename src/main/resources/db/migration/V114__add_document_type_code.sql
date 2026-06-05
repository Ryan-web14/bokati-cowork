ALTER TABLE document
    ADD COLUMN IF NOT EXISTS type_code VARCHAR(50);

UPDATE document d
SET type_code = dt.code
FROM document_type dt
WHERE d.document_type_id = dt.id
  AND d.type_code IS NULL;