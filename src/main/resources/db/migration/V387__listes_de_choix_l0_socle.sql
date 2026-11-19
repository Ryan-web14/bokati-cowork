-- Listes de choix, lot L0 : socle commun des listes de l'administrateur.
-- Les valeurs de reference de la caisse deviennent les valeurs du socle (table renommee, donnees
-- gardees, routes de la caisse inchangees) ; chaque liste a sa definition (module, libelle, mode
-- strict ou souple) ; droit SYSTEM:REFERENCE_LISTS pour les modifier.

DO $$
BEGIN
    IF to_regclass('public.cash_reference_value') IS NOT NULL AND to_regclass('public.reference_value') IS NULL THEN
        ALTER TABLE cash_reference_value RENAME TO reference_value;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_cash_reference_value') THEN
        ALTER TABLE reference_value RENAME CONSTRAINT uq_cash_reference_value TO uq_reference_value;
    END IF;
END $$;

ALTER TABLE reference_value ADD COLUMN IF NOT EXISTS auto_added BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS reference_list
(
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(60)  NOT NULL,
    module      VARCHAR(40)  NOT NULL,
    label       VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    strict      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reference_list_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_reference_list_module ON reference_list (module);

-- Definitions des listes de la caisse.
WITH lists (code, module, label, description) AS (
    VALUES ('VARIANCE_REASON', 'PAYMENT', 'Motifs d''écart de caisse', 'Écart constaté au comptage d''une session.'),
           ('ADJUSTMENT_REASON', 'PAYMENT', 'Motifs de régularisation', 'Régularisation d''un mouvement de caisse.'),
           ('REVERSAL_REASON', 'PAYMENT', 'Motifs de contre-passation', 'Annulation d''un mouvement de caisse.'),
           ('SUSPENSION_REASON', 'PAYMENT', 'Motifs de suspension', 'Suspension d''une caisse ou d''une session.'),
           ('SESSION_CANCEL_REASON', 'PAYMENT', 'Motifs d''annulation de session', 'Session de caisse ouverte à tort.'),
           ('ABSENCE_REASON', 'PAYMENT', 'Motifs d''absence', 'Absence d''un caissier.'),
           ('RESPONSIBILITY_TRANSFER_REASON', 'PAYMENT', 'Motifs de passation', 'Passation de la responsabilité d''une caisse.'),
           ('OUT_OF_HOURS_REASON', 'PAYMENT', 'Motifs d''ouverture hors horaires', 'Ouverture d''une session hors des horaires de la caisse.'),
           ('EXTERNAL_DOCUMENT_TYPE', 'PAYMENT', 'Types de pièce justificative', 'Pièce externe jointe à un mouvement.'),
           ('BANK', 'PAYMENT', 'Banques', 'Banques proposées pour les remises et les chèques.')
), all_lists AS (
    SELECT code, module, label, description FROM lists
    UNION
    -- Une liste de la caisse ajoutee depuis, sans definition : rattachee a la caisse.
    SELECT DISTINCT v.list_code, 'PAYMENT', v.list_code, NULL FROM reference_value v
    WHERE NOT EXISTS (SELECT 1 FROM lists l WHERE l.code = v.list_code)
), numbered AS (
    SELECT a.*, ROW_NUMBER() OVER (ORDER BY a.code) AS rank
    FROM all_lists a
    WHERE NOT EXISTS (SELECT 1 FROM reference_list r WHERE r.code = a.code)
)
INSERT INTO reference_list (id, code, module, label, description, strict)
SELECT COALESCE((SELECT MAX(id) FROM reference_list), 3870000) + rank, code, module, label, description, TRUE
FROM numbered;

-- Droit de modification des listes.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('SYSTEM_REFERENCE_LISTS', 'Gerer les listes de choix (valeurs, ordre, mode)', 'SYSTEM', 'REFERENCE_LISTS')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH grants (role_name, permission_name) AS (
    VALUES ('SUPER_ADMIN', 'SYSTEM_REFERENCE_LISTS'), ('ADMIN', 'SYSTEM_REFERENCE_LISTS')
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
