-- Immobilisations (lot F18, complement demande par l'exploitant) : taux annuel par categorie et
-- profils d'amortissement par marque ou modele (un Mac ne se deprecie pas comme un autre
-- ordinateur). Ordre d'application : valeurs saisies sur la fiche, puis profil de marque, puis
-- categorie.

ALTER TABLE fa_category ADD COLUMN IF NOT EXISTS annual_rate NUMERIC(7, 4);

-- Materiel informatique : degressif SYSCOHADA au taux lineaire de 15 % (6 ans et 8 mois),
-- coefficient 2,5 au-dela de 6 ans, soit 37,5 % la premiere annee.
UPDATE fa_category SET method = 'DECLINING', annual_rate = 15, duration_months = 80, coefficient = NULL
WHERE code = 'MATERIEL_INFORMATIQUE';

CREATE TABLE IF NOT EXISTS fa_depreciation_profile
(
    id              BIGINT PRIMARY KEY,
    label           VARCHAR(200)  NOT NULL,
    category_code   VARCHAR(40) REFERENCES fa_category (code),
    brand           VARCHAR(120)  NOT NULL,
    model_pattern   VARCHAR(120),
    method          VARCHAR(20),
    annual_rate     NUMERIC(7, 4),
    duration_months INTEGER,
    coefficient     NUMERIC(7, 4),
    residual_rate   NUMERIC(7, 4),
    priority        INTEGER       NOT NULL DEFAULT 0,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    notes           VARCHAR(1000),
    created_by      VARCHAR(180)  NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      VARCHAR(180),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT ck_fa_depreciation_profile_method CHECK (method IS NULL OR method IN ('LINEAR', 'DECLINING', 'UNITS', 'NONE')),
    CONSTRAINT ck_fa_depreciation_profile_rate CHECK (annual_rate IS NULL OR (annual_rate > 0 AND annual_rate <= 100)),
    CONSTRAINT ck_fa_depreciation_profile_residual CHECK (residual_rate IS NULL OR (residual_rate >= 0 AND residual_rate < 100))
);

CREATE INDEX IF NOT EXISTS ix_fa_depreciation_profile_brand ON fa_depreciation_profile (UPPER(brand));
