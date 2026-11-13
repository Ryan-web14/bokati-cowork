-- Lot B12 (facturation) : auteur de la piece et visa.
-- Fiche collaborateur : fonction imprimee sous le nom, signature enregistree pour le visa, et
-- superieur direct (N+1) dont se servent les centres d'approbation (lot B15).

CREATE TABLE IF NOT EXISTS user_staff_profile
(
    id                     BIGINT PRIMARY KEY,
    user_id                BIGINT       NOT NULL UNIQUE REFERENCES users (id),
    job_title              VARCHAR(160),
    signature_image_base64 TEXT,
    signature_updated_at   TIMESTAMPTZ,
    manager_user_id        BIGINT REFERENCES users (id),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_staff_profile_manager CHECK (manager_user_id IS NULL OR manager_user_id <> user_id)
);

CREATE INDEX IF NOT EXISTS ix_user_staff_profile_manager ON user_staff_profile (manager_user_id);

-- Auteur de la piece (figee a la creation) et auteur de la version courante, repris du jeton.
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS created_by           VARCHAR(180);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS created_by_name      VARCHAR(255);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS created_by_title     VARCHAR(160);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS version_author       VARCHAR(180);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS version_author_name  VARCHAR(255);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS version_author_title VARCHAR(160);
