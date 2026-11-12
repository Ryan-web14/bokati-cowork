-- GED lot 3 : texte des documents et recherche plein texte.
--
-- Une ligne par document : le texte de sa version courante, lu dans la couche texte d'un PDF,
-- reconnu par OCR sur un scan ou une image, ou tire d'un fichier bureautique. Le travailleur
-- d'extraction la (re)cree quand la version du document change.
--
-- La recherche passe par search_vector : configuration francaise, sans accents ni majuscules
-- (normalize_text, cree par V6), pour que « echeance » trouve « Échéance ».

CREATE TABLE IF NOT EXISTS document_text (
    document_id     BIGINT PRIMARY KEY REFERENCES document(id) ON DELETE CASCADE,
    version_number  INTEGER,
    status          VARCHAR(20) NOT NULL,
    method          VARCHAR(20),
    page_count      INTEGER,
    ocr_pages       INTEGER,
    content         TEXT,
    content_length  INTEGER NOT NULL DEFAULT 0,
    truncated       BOOLEAN NOT NULL DEFAULT FALSE,
    error           VARCHAR(500),
    attempts        INTEGER NOT NULL DEFAULT 1,
    extracted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_vector   TSVECTOR GENERATED ALWAYS AS (to_tsvector('french', normalize_text(content))) STORED
);

CREATE INDEX IF NOT EXISTS idx_document_text_search ON document_text USING gin (search_vector);
CREATE INDEX IF NOT EXISTS idx_document_text_status ON document_text (status);

-- Recherche sur le nom des fichiers, a cote du contenu
CREATE INDEX IF NOT EXISTS idx_document_title_trgm ON document USING gin (normalize_text(title) gin_trgm_ops);
