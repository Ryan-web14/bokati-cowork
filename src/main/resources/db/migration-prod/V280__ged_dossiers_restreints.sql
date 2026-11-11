-- GED lot 4 : dossiers a acces restreint.
--
-- Un dossier restreint, et tout ce qu'il contient, n'est visible que des personnes et des roles qui
-- y ont un droit (table document_permission, cible FOLDER). Pour les autres, il n'existe pas. Les
-- dossiers non restreints suivent les droits generaux (DOCUMENT:READ, UPLOAD, WRITE), comme avant.
--
-- Les droits d'un dossier sont des niveaux cumulatifs : READ (voir), DOWNLOAD (ouvrir, telecharger,
-- lire le texte), EDIT (deposer, renommer, deplacer, copier), DELETE (mettre a la corbeille),
-- ADMIN (gerer l'acces). Un role se designe par son id dans grantee_id, un utilisateur aussi.

ALTER TABLE document_folder ADD COLUMN IF NOT EXISTS restricted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE document_folder ADD COLUMN IF NOT EXISTS restricted_at TIMESTAMPTZ;
ALTER TABLE document_folder ADD COLUMN IF NOT EXISTS restricted_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_document_folder_restricted ON document_folder (id) WHERE restricted;
CREATE INDEX IF NOT EXISTS idx_document_permission_target ON document_permission (target_type, target_id);
