-- Lot KG7 : motifs normalises de refus et de correction, refus definitif, relance de renouvellement.

CREATE TABLE IF NOT EXISTS kyc_reason (
    id            BIGINT PRIMARY KEY,
    code          VARCHAR(60)  NOT NULL UNIQUE,
    label         VARCHAR(255) NOT NULL,
    -- REJECTION (refus definitif), CORRECTION (demande de correction) ou BOTH
    kind          VARCHAR(20)  NOT NULL DEFAULT 'BOTH',
    requires_text BOOLEAN      NOT NULL DEFAULT FALSE,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    system_reason BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

WITH seed (code, label, kind, requires_text, sort_order) AS (
    VALUES
    ('PIECE_ILLISIBLE', 'Pièce illisible ou floue', 'CORRECTION', FALSE, 10),
    ('PIECE_INCOMPLETE', 'Pièce incomplète (recto ou verso manquant, page coupée)', 'CORRECTION', FALSE, 20),
    ('PIECE_EXPIREE', 'Pièce expirée', 'BOTH', FALSE, 30),
    ('MAUVAIS_TYPE', 'Ce n''est pas la pièce demandée', 'CORRECTION', FALSE, 40),
    ('INFOS_INCOHERENTES', 'Informations incohérentes avec la fiche', 'BOTH', FALSE, 50),
    ('JUSTIFICATIF_ANCIEN', 'Justificatif de domicile de plus de 3 mois', 'CORRECTION', FALSE, 60),
    ('DOCUMENT_FALSIFIE', 'Document falsifié ou altéré', 'REJECTION', FALSE, 70),
    ('IDENTITE_NON_CONFIRMEE', 'Identité non confirmée', 'REJECTION', FALSE, 80),
    ('DOUBLON', 'Pièce déjà utilisée par un autre titulaire', 'REJECTION', FALSE, 90),
    ('HORS_POLITIQUE', 'Profil hors de la politique d''acceptation', 'REJECTION', FALSE, 100),
    ('AUTRE', 'Autre, préciser', 'BOTH', TRUE, 999)
), numbered AS (
    SELECT seed.*, ROW_NUMBER() OVER () AS rn FROM seed
    WHERE NOT EXISTS (SELECT 1 FROM kyc_reason r WHERE r.code = seed.code)
)
INSERT INTO kyc_reason (id, code, label, kind, requires_text, active, sort_order, system_reason)
SELECT COALESCE((SELECT MAX(id) FROM kyc_reason), 0) + rn, code, label, kind, requires_text, TRUE, sort_order, TRUE
FROM numbered;

-- Motif de la derniere decision du dossier (refus definitif ou demande de correction).
ALTER TABLE kyc_case ADD COLUMN IF NOT EXISTS decision_reason_code VARCHAR(60);

-- Derniere relance de renouvellement groupee (pour ne pas relancer deux fois la meme semaine).
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS renewal_reminded_at TIMESTAMPTZ;
