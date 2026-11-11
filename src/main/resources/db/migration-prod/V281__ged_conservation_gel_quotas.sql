-- GED lot 5 : conservation, destruction definitive, gel juridique et quotas.

-- Destruction definitive : le fichier quitte le stockage, la ligne reste comme trace (qui, quand,
-- pourquoi). Un document detruit n'apparait plus nulle part, pas meme dans la corbeille.
ALTER TABLE document ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ;
ALTER TABLE document ADD COLUMN IF NOT EXISTS purged_by BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS purge_reason VARCHAR(300);
ALTER TABLE document_folder ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ;

-- Conservation : une regle « a detruire » marque le document ; un administrateur confirme la
-- destruction ou le garde (il sort alors des regles de conservation).
ALTER TABLE document ADD COLUMN IF NOT EXISTS destruction_due_at TIMESTAMPTZ;
ALTER TABLE document ADD COLUMN IF NOT EXISTS destruction_policy_code VARCHAR(100);
ALTER TABLE document ADD COLUMN IF NOT EXISTS retention_exempt BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_document_destruction_due ON document (destruction_due_at)
    WHERE destruction_due_at IS NOT NULL AND purged_at IS NULL;

-- Une regle de conservation peut viser un dossier de la GED (et tout ce qu'il contient)
ALTER TABLE document_retention_policy ADD COLUMN IF NOT EXISTS folder_id BIGINT;

-- Quota d'espace d'un dossier, sous-dossiers compris (octets ; vide : pas de limite)
ALTER TABLE document_folder ADD COLUMN IF NOT EXISTS quota_bytes BIGINT;

-- Gel juridique : tant qu'il court, rien de ce qu'il couvre ne se supprime, ne se remplace ni ne se detruit
CREATE TABLE IF NOT EXISTS document_legal_hold (
    id              BIGINT PRIMARY KEY,
    code            VARCHAR(40) NOT NULL UNIQUE,
    target_type     VARCHAR(20) NOT NULL,
    target_id       BIGINT NOT NULL,
    reason          TEXT NOT NULL,
    reference       VARCHAR(120),
    placed_by       BIGINT,
    placed_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_by     BIGINT,
    released_at     TIMESTAMPTZ,
    release_reason  TEXT,
    CONSTRAINT ck_document_legal_hold_target CHECK (target_type IN ('FOLDER', 'DOCUMENT'))
);
CREATE INDEX IF NOT EXISTS idx_document_legal_hold_active ON document_legal_hold (target_type, target_id)
    WHERE released_at IS NULL;
