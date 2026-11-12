-- Lot F9 du plan finance : comptabilite analytique et analyse par poste de depense.
--
--   * axes et sections en arbre (les postes de depense sont les sections de l'axe POSTE) ;
--   * correspondance section et comptes generaux, regles automatiques, cles de repartition ;
--   * ventilation de chaque ligne de charge ou de produit, reclassements traces ;
--   * repartition secondaire des centres auxiliaires ;
--   * droits ANALYTIC_*.

CREATE TABLE IF NOT EXISTS acc_analytic_axis
(
    id              BIGINT PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,
    label           VARCHAR(120) NOT NULL,
    required_on     VARCHAR(10)  NOT NULL DEFAULT 'NONE',
    default_section VARCHAR(60),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_axis_required CHECK (required_on IN ('NONE', 'CHARGE', 'PRODUCT', 'BOTH'))
);

CREATE TABLE IF NOT EXISTS acc_analytic_section
(
    id             BIGINT PRIMARY KEY,
    axis_code      VARCHAR(30)  NOT NULL REFERENCES acc_analytic_axis (code),
    code           VARCHAR(60)  NOT NULL,
    label          VARCHAR(200) NOT NULL,
    parent_code    VARCHAR(60),
    family         VARCHAR(80),
    cost_behavior  VARCHAR(10),
    auxiliary      BOOLEAN      NOT NULL DEFAULT FALSE,
    responsible    VARCHAR(180),
    valid_from     DATE,
    valid_to       DATE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    merged_into    VARCHAR(60),
    color          VARCHAR(20),
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_by     VARCHAR(180) NOT NULL,
    approved_by    VARCHAR(180),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_analytic_section UNIQUE (axis_code, code),
    CONSTRAINT ck_acc_section_status CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'CLOSED', 'MERGED', 'REJECTED')),
    CONSTRAINT ck_acc_section_behavior CHECK (cost_behavior IS NULL OR cost_behavior IN ('FIXED', 'VARIABLE'))
);

CREATE TABLE IF NOT EXISTS acc_analytic_section_account
(
    id             BIGINT PRIMARY KEY,
    axis_code      VARCHAR(30) NOT NULL,
    section_code   VARCHAR(60) NOT NULL,
    account_prefix VARCHAR(20) NOT NULL,
    CONSTRAINT uq_acc_section_account UNIQUE (axis_code, account_prefix)
);

CREATE TABLE IF NOT EXISTS acc_allocation_key
(
    id             BIGINT PRIMARY KEY,
    code           VARCHAR(40)  NOT NULL,
    label          VARCHAR(200) NOT NULL,
    axis_code      VARCHAR(30)  NOT NULL,
    effective_from DATE         NOT NULL,
    created_by     VARCHAR(180) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_allocation_key UNIQUE (code, effective_from)
);

CREATE TABLE IF NOT EXISTS acc_allocation_key_line
(
    id           BIGINT PRIMARY KEY,
    key_id       BIGINT         NOT NULL REFERENCES acc_allocation_key (id),
    section_code VARCHAR(60)    NOT NULL,
    percentage   NUMERIC(9, 4)  NOT NULL,
    CONSTRAINT ck_acc_key_line_pct CHECK (percentage > 0 AND percentage <= 100)
);

CREATE TABLE IF NOT EXISTS acc_analytic_rule
(
    id             BIGINT PRIMARY KEY,
    priority       INTEGER      NOT NULL DEFAULT 100,
    axis_code      VARCHAR(30)  NOT NULL,
    account_prefix VARCHAR(20),
    journal_code   VARCHAR(30),
    source_type    VARCHAR(80),
    third_party    VARCHAR(120),
    section_code   VARCHAR(60),
    key_code       VARCHAR(40),
    label          VARCHAR(200),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by     VARCHAR(180) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_rule_target CHECK (section_code IS NOT NULL OR key_code IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS acc_analytic_allocation
(
    id            BIGINT PRIMARY KEY,
    entry_line_id BIGINT         NOT NULL REFERENCES acc_entry_line (id),
    entry_id      BIGINT         NOT NULL,
    axis_code     VARCHAR(30)    NOT NULL,
    section_code  VARCHAR(60)    NOT NULL,
    percentage    NUMERIC(9, 4)  NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    origin        VARCHAR(20)    NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_acc_allocation_line ON acc_analytic_allocation (entry_line_id);
CREATE INDEX IF NOT EXISTS idx_acc_allocation_section ON acc_analytic_allocation (axis_code, section_code);

CREATE TABLE IF NOT EXISTS acc_analytic_reclass
(
    id            BIGINT PRIMARY KEY,
    entry_line_id BIGINT         NOT NULL,
    axis_code     VARCHAR(30)    NOT NULL,
    from_section  VARCHAR(60)    NOT NULL,
    to_section    VARCHAR(60)    NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    reason        VARCHAR(1000)  NOT NULL,
    entry_number  VARCHAR(40),
    created_by    VARCHAR(180)   NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS acc_analytic_distribution
(
    id           BIGINT PRIMARY KEY,
    period_code  VARCHAR(10)    NOT NULL,
    axis_code    VARCHAR(30)    NOT NULL,
    from_section VARCHAR(60)    NOT NULL,
    to_section   VARCHAR(60)    NOT NULL,
    key_code     VARCHAR(40)    NOT NULL,
    amount       NUMERIC(19, 4) NOT NULL,
    created_by   VARCHAR(180)   NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_acc_distribution_period ON acc_analytic_distribution (period_code, axis_code);

-- ------------------------------------------------------------------ axes et sections initiaux

INSERT INTO acc_analytic_axis (id, code, label, required_on, default_section, sort_order)
VALUES (2967001, 'SITE', 'Site', 'NONE', NULL, 1),
       (2967002, 'ACTIVITE', 'Activite', 'BOTH', 'STRUCTURE', 2),
       (2967003, 'CENTRE', 'Centre de cout', 'NONE', NULL, 3),
       (2967004, 'POSTE', 'Poste de depense', 'CHARGE', 'NON_CLASSE', 4),
       (2967005, 'PROJET', 'Projet ou evenement', 'NONE', NULL, 5),
       (2967006, 'RESSOURCE', 'Ressource', 'NONE', NULL, 6),
       (2967007, 'CLIENT', 'Organisation cliente', 'NONE', NULL, 7)
ON CONFLICT (code) DO NOTHING;

WITH seed (axis_code, code, label, parent_code, family, cost_behavior, auxiliary) AS (
    VALUES ('ACTIVITE', 'STRUCTURE', 'Structure (non reparti)', NULL, NULL, NULL, TRUE),
           ('ACTIVITE', 'BUREAUX', 'Bureaux prives', NULL, NULL, NULL, FALSE),
           ('ACTIVITE', 'OPEN_SPACE', 'Open space', NULL, NULL, NULL, FALSE),
           ('ACTIVITE', 'SALLES', 'Salles de reunion', NULL, NULL, NULL, FALSE),
           ('ACTIVITE', 'DOMICILIATION', 'Domiciliation', NULL, NULL, NULL, FALSE),
           ('ACTIVITE', 'EVENEMENTS', 'Evenements', NULL, NULL, NULL, FALSE),
           ('ACTIVITE', 'MARKETPLACE', 'Marketplace', NULL, NULL, NULL, FALSE),
           ('ACTIVITE', 'SERVICES', 'Services annexes', NULL, NULL, NULL, FALSE),
           ('CENTRE', 'ACCUEIL', 'Accueil', NULL, NULL, NULL, FALSE),
           ('CENTRE', 'ENTRETIEN', 'Entretien', NULL, NULL, NULL, TRUE),
           ('CENTRE', 'INFORMATIQUE', 'Informatique', NULL, NULL, NULL, TRUE),
           ('CENTRE', 'DIRECTION', 'Direction', NULL, NULL, NULL, TRUE),
           ('CENTRE', 'COMMERCIAL', 'Commercial', NULL, NULL, NULL, FALSE),
           ('POSTE', 'NON_CLASSE', 'A classer', NULL, 'Divers', 'VARIABLE', FALSE),
           ('POSTE', 'LOYER', 'Loyer', NULL, 'Locaux', 'FIXED', FALSE),
           ('POSTE', 'CHARGES_LOCATIVES', 'Charges locatives', NULL, 'Locaux', 'FIXED', FALSE),
           ('POSTE', 'ELECTRICITE', 'Electricite', NULL, 'Locaux', 'VARIABLE', FALSE),
           ('POSTE', 'EAU', 'Eau', NULL, 'Locaux', 'VARIABLE', FALSE),
           ('POSTE', 'NETTOYAGE', 'Entretien et nettoyage', NULL, 'Locaux', 'FIXED', FALSE),
           ('POSTE', 'REPARATIONS', 'Reparations', NULL, 'Locaux', 'VARIABLE', FALSE),
           ('POSTE', 'SECURITE', 'Securite', NULL, 'Locaux', 'FIXED', FALSE),
           ('POSTE', 'TELECOMS', 'Internet et telecoms', NULL, 'Equipement et technique', 'FIXED', FALSE),
           ('POSTE', 'LOGICIELS', 'Logiciels et abonnements', NULL, 'Equipement et technique', 'FIXED', FALSE),
           ('POSTE', 'MATERIEL', 'Petit materiel et mobilier', NULL, 'Equipement et technique', 'VARIABLE', FALSE),
           ('POSTE', 'CAFE', 'Cafe et boissons', NULL, 'Accueil et vie du lieu', 'VARIABLE', FALSE),
           ('POSTE', 'FOURNITURES', 'Fournitures de bureau', NULL, 'Accueil et vie du lieu', 'VARIABLE', FALSE),
           ('POSTE', 'EVENEMENTS_MEMBRES', 'Evenements membres', NULL, 'Accueil et vie du lieu', 'VARIABLE', FALSE),
           ('POSTE', 'SALAIRES', 'Salaires et charges sociales', NULL, 'Personnel', 'FIXED', FALSE),
           ('POSTE', 'DEPLACEMENTS', 'Deplacements et missions', NULL, 'Personnel', 'VARIABLE', FALSE),
           ('POSTE', 'PUBLICITE', 'Publicite et communication', NULL, 'Commercial', 'VARIABLE', FALSE),
           ('POSTE', 'HONORAIRES', 'Honoraires', NULL, 'Administratif', 'FIXED', FALSE),
           ('POSTE', 'ASSURANCES', 'Assurances', NULL, 'Administratif', 'FIXED', FALSE),
           ('POSTE', 'FRAIS_BANCAIRES', 'Frais bancaires et mobile money', NULL, 'Administratif', 'VARIABLE', FALSE),
           ('POSTE', 'IMPOTS', 'Impots et taxes', NULL, 'Administratif', 'FIXED', FALSE),
           ('POSTE', 'INTERETS', 'Interets et agios', NULL, 'Financier', 'FIXED', FALSE)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.axis_code, s.code) AS rank
    FROM seed s WHERE NOT EXISTS (SELECT 1 FROM acc_analytic_section a WHERE a.axis_code = s.axis_code AND a.code = s.code)
)
INSERT INTO acc_analytic_section (id, axis_code, code, label, parent_code, family, cost_behavior, auxiliary, sort_order, created_by)
SELECT 2967100 + rank, axis_code, code, label, parent_code, family, cost_behavior, auxiliary, rank, 'SYSTEM' FROM numbered;

-- Postes de depense rattaches aux comptes generaux (proposition par le compte).
WITH seed (section_code, account_prefix) AS (
    VALUES ('LOYER', '622'), ('ELECTRICITE', '6052'), ('EAU', '6051'), ('NETTOYAGE', '624'), ('REPARATIONS', '624'),
           ('TELECOMS', '628'), ('LOGICIELS', '634'), ('MATERIEL', '6056'), ('CAFE', '605'), ('FOURNITURES', '6055'),
           ('SALAIRES', '66'), ('DEPLACEMENTS', '618'), ('PUBLICITE', '627'), ('HONORAIRES', '632'), ('ASSURANCES', '625'),
           ('FRAIS_BANCAIRES', '631'), ('IMPOTS', '64'), ('INTERETS', '67')
), dedup AS (
    SELECT DISTINCT ON (account_prefix) section_code, account_prefix FROM seed ORDER BY account_prefix, section_code
), numbered AS (
    SELECT d.*, ROW_NUMBER() OVER (ORDER BY d.account_prefix) AS rank
    FROM dedup d WHERE NOT EXISTS (SELECT 1 FROM acc_analytic_section_account a WHERE a.axis_code = 'POSTE' AND a.account_prefix = d.account_prefix)
)
INSERT INTO acc_analytic_section_account (id, axis_code, section_code, account_prefix)
SELECT 2967300 + rank, 'POSTE', section_code, account_prefix FROM numbered;

-- Natures de caisse : leur poste de depense par defaut.
UPDATE cash_operation_nature SET default_section = CASE code
    WHEN 'ACHAT_FOURNITURES_BUREAU' THEN 'FOURNITURES'
    WHEN 'ACHAT_PETIT_MATERIEL' THEN 'MATERIEL'
    WHEN 'ACHAT_CONSOMMABLES_ESPACES' THEN 'CAFE'
    WHEN 'TRANSPORT_COURSE' THEN 'DEPLACEMENTS'
    WHEN 'FRAIS_MISSION' THEN 'DEPLACEMENTS'
    WHEN 'AFFRANCHISSEMENT' THEN 'FOURNITURES'
    WHEN 'TELECOMMUNICATIONS' THEN 'TELECOMS'
    WHEN 'PETITES_REPARATIONS' THEN 'REPARATIONS'
    ELSE default_section END
WHERE default_section IS NULL;

-- ------------------------------------------------------------------ droits

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('ANALYTIC_SETTINGS_MANAGE', 'Parametrer l''analytique (axes, sections, regles, cles)', 'ANALYTIC_SETTINGS', 'MANAGE'),
           ('ANALYTIC_ITEM_CREATE', 'Proposer ou creer des postes analytiques', 'ANALYTIC_ITEM', 'CREATE'),
           ('ANALYTIC_RECLASS', 'Reclasser une imputation analytique', 'ANALYTIC', 'RECLASS'),
           ('ANALYTIC_REPORT_READ', 'Consulter les etats analytiques et l''analyse des depenses', 'ANALYTIC_REPORT', 'READ')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('ANALYTIC_SETTINGS_MANAGE'), ('ANALYTIC_ITEM_CREATE'), ('ANALYTIC_RECLASS'), ('ANALYTIC_REPORT_READ')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('ANALYTIC_ITEM_CREATE'), ('ANALYTIC_RECLASS'), ('ANALYTIC_REPORT_READ')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', p FROM (VALUES ('ANALYTIC_ITEM_CREATE'), ('ANALYTIC_REPORT_READ')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Ventilation demandee pour une ligne (saisie ou fait metier) : {"AXE": "SECTION" ou "KEY:CLE"}.
ALTER TABLE acc_entry_line ADD COLUMN IF NOT EXISTS analytic_json TEXT;

-- Schemas de caisse et de justification : le poste de depense vient de la nature d'operation.
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT r.id + 10000, r.code, 2, r.event_type, r.label, r.description, r.family, r.priority, r.conditions, r.journal, r.entry_label,
       replace(r.lines, '"thirdParty":"{?natureThirdParty}"', '"thirdParty":"{?natureThirdParty}","analytic":{"POSTE":"{?natureSection}"}'),
       r.no_entry, r.sample_payload, r.effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM acc_posting_rule r
WHERE r.code IN ('CASH_OUT', 'CASH_IN', 'EXPENSE_JUSTIFIED') AND r.version = 1
  AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = 2);

UPDATE acc_posting_rule SET status = 'RETIRED'
WHERE code IN ('CASH_OUT', 'CASH_IN', 'EXPENSE_JUSTIFIED') AND version = 1
  AND EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = acc_posting_rule.code AND o.version = 2);
