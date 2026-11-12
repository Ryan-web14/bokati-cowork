-- Suggestion de fournisseurs (complement du lot F13). La fiche fournisseur de reference reste celle
-- de l'inventaire (inventory_supplier) ; elle gagne :
--   * un referentiel de categories de fournisseurs (arborescent), chaque categorie reliee a des
--     natures d'operation et a des categories de stock (pont entre la categorie du besoin et les
--     fournisseurs) ;
--   * le rattachement d'un fournisseur a une ou plusieurs categories (nature, categorie de stock
--     ou categorie fournisseur).
-- Les lignes d'etat de besoin, de demande de fonds et de justification gagnent supplier_code
-- (facultatif, le texte libre supplier reste) ; le fournisseur comptable peut etre relie a sa
-- fiche d'inventaire.

CREATE TABLE IF NOT EXISTS inventory_supplier_category_ref
(
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(80)  NOT NULL,
    label       VARCHAR(180) NOT NULL,
    description VARCHAR(1000),
    parent_id   BIGINT REFERENCES inventory_supplier_category_ref (id),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_by  VARCHAR(180) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by  VARCHAR(180),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_supplier_category_ref_code UNIQUE (code),
    CONSTRAINT ck_inventory_supplier_category_ref_parent CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX IF NOT EXISTS idx_inventory_supplier_category_ref_parent ON inventory_supplier_category_ref (parent_id);

-- Natures d'operation et categories de stock reliees a une categorie fournisseur.
CREATE TABLE IF NOT EXISTS inventory_supplier_category_ref_target
(
    id          BIGINT PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES inventory_supplier_category_ref (id) ON DELETE CASCADE,
    target_kind VARCHAR(30)  NOT NULL,
    target_code VARCHAR(80)  NOT NULL,
    created_by  VARCHAR(180) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_supplier_category_ref_target UNIQUE (category_id, target_kind, target_code),
    CONSTRAINT ck_inventory_supplier_category_ref_target_kind CHECK (target_kind IN ('NATURE', 'INVENTORY_CATEGORY'))
);
CREATE INDEX IF NOT EXISTS idx_inventory_supplier_category_ref_target ON inventory_supplier_category_ref_target (target_kind, target_code);

-- Categories d'un fournisseur : nature d'operation, categorie de stock ou categorie fournisseur.
CREATE TABLE IF NOT EXISTS inventory_supplier_category
(
    id            BIGINT PRIMARY KEY,
    supplier_id   BIGINT       NOT NULL REFERENCES inventory_supplier (id) ON DELETE CASCADE,
    category_kind VARCHAR(30)  NOT NULL,
    category_code VARCHAR(80)  NOT NULL,
    created_by    VARCHAR(180) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_supplier_category UNIQUE (supplier_id, category_kind, category_code),
    CONSTRAINT ck_inventory_supplier_category_kind CHECK (category_kind IN ('NATURE', 'INVENTORY_CATEGORY', 'SUPPLIER_CATEGORY'))
);
CREATE INDEX IF NOT EXISTS idx_inventory_supplier_category_code ON inventory_supplier_category (category_kind, category_code);

-- Lignes reliees a une fiche fournisseur (facultatif).
ALTER TABLE fund_need_line ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(100);
ALTER TABLE fund_request_line ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(100);
ALTER TABLE expense_justification_line ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_fund_need_line_nature_supplier ON fund_need_line (nature_code, supplier_code);
CREATE INDEX IF NOT EXISTS idx_fund_request_line_nature_supplier ON fund_request_line (nature_code, supplier_code);
CREATE INDEX IF NOT EXISTS idx_expense_line_nature_supplier ON expense_justification_line (nature_code, supplier_code);

-- Fournisseur comptable relie a sa fiche d'inventaire (au plus un par fiche).
ALTER TABLE acc_supplier ADD COLUMN IF NOT EXISTS inventory_supplier_id BIGINT REFERENCES inventory_supplier (id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_acc_supplier_inventory_supplier ON acc_supplier (inventory_supplier_id)
    WHERE inventory_supplier_id IS NOT NULL;

-- Categories de depart, pensees pour un espace de coworking.
WITH seed (rank, code, label, description, sort_order) AS (
    VALUES (1, 'FOURNITURES_BUREAU', 'Fournitures de bureau', 'Papeterie, ramettes, cartouches, petites fournitures', 10),
           (2, 'INFORMATIQUE_TELEPHONIE', 'Informatique et telephonie', 'Materiel informatique, reseau, internet, credit de communication', 20),
           (3, 'ENTRETIEN_NETTOYAGE', 'Entretien et nettoyage', 'Produits d''entretien, societes de nettoyage, hygiene', 30),
           (4, 'RESTAURATION_TRAITEUR', 'Restauration, cafe et traiteur', 'Cafe, the, eau, collations, traiteur des evenements', 40),
           (5, 'TRAVAUX_MAINTENANCE', 'Travaux et maintenance', 'Petites reparations, plomberie, electricite, climatisation', 50),
           (6, 'MOBILIER_AMENAGEMENT', 'Mobilier et amenagement', 'Bureaux, sieges, rangements, decoration, petit materiel', 60),
           (7, 'ENERGIE_EAU', 'Energie et eau', 'Electricite, eau, carburant du groupe electrogene', 70),
           (8, 'PRESTATIONS_SERVICES', 'Prestations de services', 'Conseil, honoraires, sous-traitance, assurances', 80),
           (9, 'TRANSPORT_LOGISTIQUE', 'Transport et logistique', 'Courses, livraisons, coursiers, envoi de courrier', 90),
           (10, 'COMMUNICATION_IMPRESSION', 'Communication et impression', 'Impression, signaletique, publicite, evenementiel', 100),
           (11, 'SECURITE_GARDIENNAGE', 'Securite et gardiennage', 'Gardiennage, controle d''acces, videosurveillance, extincteurs', 110)
)
INSERT INTO inventory_supplier_category_ref (id, code, label, description, sort_order, created_by)
SELECT 3060000 + rank, code, label, description, sort_order, 'SYSTEM'
FROM seed
WHERE NOT EXISTS (SELECT 1 FROM inventory_supplier_category_ref r WHERE r.code = seed.code);

-- Pont vers les natures d'operation existantes (une nature absente est ignoree).
WITH seed (rank, category_code, nature_code) AS (
    VALUES (1, 'FOURNITURES_BUREAU', 'ACHAT_FOURNITURES_BUREAU'),
           (2, 'INFORMATIQUE_TELEPHONIE', 'TELECOMMUNICATIONS'),
           (3, 'INFORMATIQUE_TELEPHONIE', 'ACHAT_PETIT_MATERIEL'),
           (4, 'ENTRETIEN_NETTOYAGE', 'ACHAT_PRODUITS_ENTRETIEN'),
           (5, 'RESTAURATION_TRAITEUR', 'ACHAT_CONSOMMABLES_ESPACES'),
           (6, 'TRAVAUX_MAINTENANCE', 'PETITES_REPARATIONS'),
           (7, 'MOBILIER_AMENAGEMENT', 'ACHAT_PETIT_MATERIEL'),
           (8, 'TRANSPORT_LOGISTIQUE', 'TRANSPORT_COURSE'),
           (9, 'TRANSPORT_LOGISTIQUE', 'AFFRANCHISSEMENT')
)
INSERT INTO inventory_supplier_category_ref_target (id, category_id, target_kind, target_code, created_by)
SELECT 3061000 + seed.rank, ref.id, 'NATURE', seed.nature_code, 'SYSTEM'
FROM seed
         JOIN inventory_supplier_category_ref ref ON ref.code = seed.category_code
         JOIN cash_operation_nature nature ON nature.code = seed.nature_code
WHERE NOT EXISTS (SELECT 1
                  FROM inventory_supplier_category_ref_target t
                  WHERE t.category_id = ref.id AND t.target_kind = 'NATURE' AND t.target_code = seed.nature_code);
