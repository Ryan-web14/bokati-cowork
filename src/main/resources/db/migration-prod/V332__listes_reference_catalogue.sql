-- Lot C0 (facturation, complements) : listes de reference du catalogue. Familles et sous-familles,
-- unites, regles de taxe gerables depuis l'ecran. Les colonnes texte des articles restent : la
-- reprise met dans les listes toutes les valeurs deja saisies, avec la valeur comme code, pour
-- qu'aucun article ne devienne invalide.

CREATE TABLE IF NOT EXISTS billing_catalog_category
(
    id                BIGINT PRIMARY KEY,
    code              VARCHAR(100) NOT NULL,
    label             VARCHAR(150) NOT NULL,
    parent_code       VARCHAR(100),
    description       TEXT,
    revenue_account   VARCHAR(20),
    display_order     INTEGER      NOT NULL DEFAULT 0,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_from_item BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_billing_catalog_category_code
    ON billing_catalog_category (COALESCE(parent_code, ''), lower(code));
CREATE INDEX IF NOT EXISTS ix_billing_catalog_category_parent ON billing_catalog_category (parent_code);

CREATE TABLE IF NOT EXISTS billing_catalog_unit
(
    id                  BIGINT PRIMARY KEY,
    code                VARCHAR(50)  NOT NULL,
    label               VARCHAR(100) NOT NULL,
    plural_label        VARCHAR(100),
    symbol              VARCHAR(20),
    quantity_scale      INTEGER      NOT NULL DEFAULT 2,
    inventory_unit_code VARCHAR(50),
    display_order       INTEGER      NOT NULL DEFAULT 0,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_from_item   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_catalog_unit_scale CHECK (quantity_scale BETWEEN 0 AND 4)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_billing_catalog_unit_code ON billing_catalog_unit (lower(code));

-- Regles de taxe : code stable (base_code) que les articles referencent, versions datees.
ALTER TABLE billing_tax_rule ADD COLUMN IF NOT EXISTS base_code VARCHAR(120);
ALTER TABLE billing_tax_rule ADD COLUMN IF NOT EXISTS label_on_document VARCHAR(255);
ALTER TABLE billing_tax_rule ADD COLUMN IF NOT EXISTS exemption_reason VARCHAR(500);
ALTER TABLE billing_tax_rule ADD COLUMN IF NOT EXISTS treatment VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE billing_tax_rule ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;
UPDATE billing_tax_rule SET base_code = code WHERE base_code IS NULL;
ALTER TABLE billing_tax_rule ALTER COLUMN base_code SET NOT NULL;
ALTER TABLE billing_tax_rule DROP CONSTRAINT IF EXISTS ck_billing_tax_rule_treatment;
ALTER TABLE billing_tax_rule ADD CONSTRAINT ck_billing_tax_rule_treatment
    CHECK (treatment IN ('STANDARD', 'REDUCED', 'EXEMPT', 'OUT_OF_SCOPE'));
CREATE INDEX IF NOT EXISTS ix_billing_tax_rule_base_code ON billing_tax_rule (base_code, valid_from);

UPDATE billing_tax_rule SET label_on_document = 'TVA 18 %', display_order = 1
WHERE code = 'TVA_CG_18' AND label_on_document IS NULL;
UPDATE billing_tax_rule SET label_on_document = 'Centimes additionnels 5 % de la TVA', display_order = 90
WHERE code = 'CAC_CG_ON_VAT_5' AND label_on_document IS NULL;

INSERT INTO billing_tax_rule (id, code, base_code, name, country_code, tax_type, rate, applies_on, active, valid_from, valid_until,
                              label_on_document, exemption_reason, treatment, display_order)
SELECT 3380001, 'TVA_CG_EXONERE', 'TVA_CG_EXONERE', 'Exonéré de TVA', 'CG', 'VAT', 0, 'TAXABLE_AMOUNT', TRUE, CURRENT_DATE, NULL,
       'Exonéré de TVA', 'Opération exonérée de TVA', 'EXEMPT', 2
WHERE NOT EXISTS (SELECT 1 FROM billing_tax_rule WHERE code = 'TVA_CG_EXONERE');

INSERT INTO billing_tax_rule (id, code, base_code, name, country_code, tax_type, rate, applies_on, active, valid_from, valid_until,
                              label_on_document, exemption_reason, treatment, display_order)
SELECT 3380002, 'TVA_CG_HORS_CHAMP', 'TVA_CG_HORS_CHAMP', 'Hors champ de la TVA', 'CG', 'VAT', 0, 'TAXABLE_AMOUNT', TRUE, CURRENT_DATE, NULL,
       'Hors champ de la TVA', 'Opération hors du champ d''application de la TVA', 'OUT_OF_SCOPE', 3
WHERE NOT EXISTS (SELECT 1 FROM billing_tax_rule WHERE code = 'TVA_CG_HORS_CHAMP');

-- Reprise des familles saisies sur les articles.
INSERT INTO billing_catalog_category (id, code, label, parent_code, display_order)
SELECT 3381000000 + ROW_NUMBER() OVER (ORDER BY v.value), v.value, v.value, NULL, 0
FROM (SELECT DISTINCT TRIM(category) AS value FROM service_catalog_item WHERE category IS NOT NULL AND TRIM(category) <> '') v
WHERE NOT EXISTS (SELECT 1 FROM billing_catalog_category c WHERE c.parent_code IS NULL AND lower(c.code) = lower(v.value));

-- Famille d'accueil des sous-familles saisies sans famille.
INSERT INTO billing_catalog_category (id, code, label, parent_code, display_order)
SELECT 3381999999, 'AUTRES', 'Autres', NULL, 999
WHERE NOT EXISTS (SELECT 1 FROM billing_catalog_category c WHERE c.parent_code IS NULL AND lower(c.code) = 'autres');

-- Reprise des sous-familles, sous leur famille (ou sous « Autres »).
INSERT INTO billing_catalog_category (id, code, label, parent_code, display_order)
SELECT 3382000000 + ROW_NUMBER() OVER (ORDER BY v.parent, v.value), v.value, v.value, v.parent, 0
FROM (SELECT DISTINCT COALESCE(NULLIF(TRIM(i.category), ''),
                               (SELECT c.code FROM billing_catalog_category c WHERE c.parent_code IS NULL AND lower(c.code) = 'autres' LIMIT 1)) AS parent,
                      TRIM(i.subcategory) AS value
      FROM service_catalog_item i
      WHERE i.subcategory IS NOT NULL AND TRIM(i.subcategory) <> '') v
WHERE NOT EXISTS (SELECT 1 FROM billing_catalog_category c WHERE lower(c.parent_code) = lower(v.parent) AND lower(c.code) = lower(v.value));

-- Unites de base.
INSERT INTO billing_catalog_unit (id, code, label, plural_label, symbol, quantity_scale, display_order)
SELECT b.id, b.code, b.label, b.plural_label, b.symbol, b.scale, b.ord
FROM (VALUES (3383000001, 'heure', 'Heure', 'heures', 'h', 2, 1),
             (3383000002, 'demi-journee', 'Demi-journée', 'demi-journées', NULL, 0, 2),
             (3383000003, 'jour', 'Jour', 'jours', 'j', 0, 3),
             (3383000004, 'semaine', 'Semaine', 'semaines', NULL, 0, 4),
             (3383000005, 'mois', 'Mois', 'mois', NULL, 0, 5),
             (3383000006, 'an', 'An', 'ans', NULL, 0, 6),
             (3383000007, 'personne', 'Personne', 'personnes', 'pers.', 0, 7),
             (3383000008, 'poste', 'Poste', 'postes', NULL, 0, 8),
             (3383000009, 'forfait', 'Forfait', 'forfaits', NULL, 0, 9),
             (3383000010, 'unite', 'Unité', 'unités', 'u', 2, 10),
             (3383000011, 'page', 'Page', 'pages', 'p.', 0, 11)) AS b(id, code, label, plural_label, symbol, scale, ord)
WHERE NOT EXISTS (SELECT 1 FROM billing_catalog_unit u WHERE lower(u.code) = lower(b.code));

-- Reprise des unites saisies sur les articles et sur les lignes de pieces.
INSERT INTO billing_catalog_unit (id, code, label, display_order)
SELECT 3384000000 + ROW_NUMBER() OVER (ORDER BY v.value), v.value, v.value, 100
FROM (SELECT DISTINCT ON (lower(x.value)) x.value
      FROM (SELECT TRIM(unit) AS value FROM service_catalog_item WHERE unit IS NOT NULL AND TRIM(unit) <> ''
            UNION
            SELECT TRIM(unit) FROM billing_document_line WHERE unit IS NOT NULL AND TRIM(unit) <> '') x
      ORDER BY lower(x.value), x.value) v
WHERE NOT EXISTS (SELECT 1 FROM billing_catalog_unit u WHERE lower(u.code) = lower(v.value));

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('BILLING_CATALOG_REFERENCES', 'Gerer les familles, sous-familles et unites du catalogue', 'BILLING', 'CATALOG_REFERENCES'),
           ('BILLING_TAX_RULES', 'Gerer les regles de taxe de la facturation', 'BILLING', 'TAX_RULES')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('BILLING_CATALOG_REFERENCES'), ('BILLING_TAX_RULES')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'BILLING_CATALOG_REFERENCES'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
