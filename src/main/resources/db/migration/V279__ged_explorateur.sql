-- GED, lot 1 : un explorateur de fichiers general.
--
-- 1. Un document interne a l'organisation (courrier numerise, PDF a conserver) n'appartient a
--    aucun membre, client ou entreprise : son proprietaire devient facultatif. Le type de
--    proprietaire reste obligatoire (INTERNAL pour ces documents).
-- 2. Une corbeille : un element supprime depuis l'explorateur y reste restaurable. Les elements
--    supprimes ensemble (un dossier et tout son contenu) partagent le meme lot pour etre restaures
--    ensemble.
-- 3. Un type « Document general » sans revue, plusieurs par proprietaire, aux formats larges.

ALTER TABLE document ALTER COLUMN owner_id DROP NOT NULL;

ALTER TABLE document
    ADD COLUMN IF NOT EXISTS trashed_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS trashed_by  BIGINT,
    ADD COLUMN IF NOT EXISTS trash_batch VARCHAR(40);

ALTER TABLE document_folder
    ADD COLUMN IF NOT EXISTS trashed_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS trashed_by  BIGINT,
    ADD COLUMN IF NOT EXISTS trash_batch VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_document_trash_batch ON document (trash_batch) WHERE trash_batch IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_document_folder_trash_batch ON document_folder (trash_batch) WHERE trash_batch IS NOT NULL;

INSERT INTO document_type (id, code, name, category, owner_type, description, required,
                           requires_expiry_date, requires_review, requires_signature, active,
                           multiple_allowed, requires_back_side, allowed_mime_types, max_file_size_bytes)
SELECT 279001, 'GENERAL', 'Document général', 'OTHER', NULL,
       'Tout document conservé dans la GED : courrier numérisé, document administratif, PDF à archiver.',
       FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE,
       'application/pdf,image/jpeg,image/png,image/webp,image/tiff,image/gif,text/plain,text/csv,'
           || 'application/vnd.openxmlformats-officedocument.wordprocessingml.document,'
           || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,'
           || 'application/vnd.openxmlformats-officedocument.presentationml.presentation,'
           || 'application/vnd.oasis.opendocument.text,application/vnd.oasis.opendocument.spreadsheet,'
           || 'application/msword,application/vnd.ms-excel,application/vnd.ms-powerpoint',
       10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'GENERAL');
