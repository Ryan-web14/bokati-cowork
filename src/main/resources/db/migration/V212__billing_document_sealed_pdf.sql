-- =====================================================================================
-- PDF fige et empreinte
--
-- Le PDF etait reconstruit a chaque telechargement et rien n'etait conserve. Deux
-- consequences :
--
--   1. Impossible de prouver qu'un fichier presente est bien celui qui a ete emis.
--   2. Deux generations successives ne produisent meme pas le meme fichier · PDFBox
--      inscrit une date de creation dans les metadonnees, et la liste des paiements
--      imprimee sur le document evolue. Toute comparaison d'empreinte etait donc vaine.
--
-- A la validation, le PDF est desormais fige une fois, stocke, et son SHA-256 conserve.
-- C'est cette version qui est servie ensuite.
-- =====================================================================================

ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS pdf_storage_provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pdf_storage_path     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS pdf_sha256           VARCHAR(64),
    ADD COLUMN IF NOT EXISTS pdf_sealed_at        TIMESTAMP WITH TIME ZONE;


-- -------------------------------------------------------------------------------------
-- Les quatre colonnes vont ensemble · une empreinte sans fichier, ou l'inverse, signale
-- une ecriture partielle et rendrait la comparaison trompeuse.
--
-- Pose en NOT VALID : la contrainte s'applique aux ecritures nouvelles sans faire echouer
-- le deploiement sur une ligne ancienne.
-- -------------------------------------------------------------------------------------
ALTER TABLE billing_document
    ADD CONSTRAINT ck_billing_document_sealed_pdf_complete
        CHECK (
            (pdf_storage_provider IS NULL AND pdf_storage_path IS NULL
             AND pdf_sha256 IS NULL AND pdf_sealed_at IS NULL)
            OR (pdf_storage_provider IS NOT NULL AND pdf_storage_path IS NOT NULL
                AND pdf_sha256 IS NOT NULL AND pdf_sealed_at IS NOT NULL)
        ) NOT VALID;


-- -------------------------------------------------------------------------------------
-- Recherche par empreinte · c'est la question que pose la page de verification quand on
-- lui depose un fichier : « d'ou vient celui-ci ? ». Index partiel, la colonne restant
-- vide sur tout document non scelle.
-- -------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_billing_document_pdf_sha256
    ON billing_document (pdf_sha256)
    WHERE pdf_sha256 IS NOT NULL;
