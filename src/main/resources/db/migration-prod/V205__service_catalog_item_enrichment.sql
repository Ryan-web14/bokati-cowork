-- =====================================================================================
-- Enrichissement du catalogue de services
--
-- Toutes les colonnes sont facultatives et sans valeur imposee, a l'exception de
-- discount_policy qui prend NONE. Laissees vides, le comportement de la facturation reste
-- strictement celui d'aujourd'hui : c'est ce qui rend l'ajout sans effet sur l'existant.
--
-- Trois familles :
--   1. l'economie de la ligne, qui alimente le plancher de prix et le calcul de marge ;
--   2. le contenu de la prestation, repris sur le document remis au client ;
--   3. l'aide a la saisie et le classement.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1. Economie de la ligne
--
--    Le plancher se resout dans cet ordre : floor_price s'il est saisi, sinon derive de
--    cost_price et min_margin_rate, sinon cost_price seul. Un article sans cout ni
--    plancher n'est soumis a aucune limite, comme aujourd'hui.
-- -------------------------------------------------------------------------------------
ALTER TABLE service_catalog_item
    ADD COLUMN IF NOT EXISTS cost_price        NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS floor_price       NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS min_margin_rate   NUMERIC(9, 4),
    ADD COLUMN IF NOT EXISTS max_discount_rate NUMERIC(9, 4),
    ADD COLUMN IF NOT EXISTS discount_policy   VARCHAR(20) NOT NULL DEFAULT 'NONE';

-- Les montants restent positifs et les taux tiennent dans [0, 100]. Poses en NOT VALID :
-- la contrainte s'applique aux ecritures nouvelles sans faire echouer le deploiement sur
-- une ligne ancienne qui ne la respecterait pas.
ALTER TABLE service_catalog_item
    ADD CONSTRAINT ck_service_catalog_item_amounts_positive
        CHECK (
            (cost_price  IS NULL OR cost_price  >= 0)
            AND (floor_price IS NULL OR floor_price >= 0)
        ) NOT VALID;

ALTER TABLE service_catalog_item
    ADD CONSTRAINT ck_service_catalog_item_rates_bounded
        CHECK (
            (min_margin_rate   IS NULL OR (min_margin_rate   >= 0 AND min_margin_rate   < 100))
            AND (max_discount_rate IS NULL OR (max_discount_rate >= 0 AND max_discount_rate <= 100))
        ) NOT VALID;

-- min_margin_rate strictement inferieur a 100 : le plancher se calcule en taux de marque,
-- soit cost_price / (1 - taux). A 100 la division n'a pas de sens.
ALTER TABLE service_catalog_item
    ADD CONSTRAINT ck_service_catalog_item_discount_policy
        CHECK (discount_policy IN ('NONE', 'WARN', 'BLOCK')) NOT VALID;


-- -------------------------------------------------------------------------------------
-- 2. Contenu de la prestation
--
--    included_items porte la liste de ce que la prestation comprend, en JSON, rendue en
--    sous-lignes sur le document. Stockee en text comme les autres colonnes JSON du
--    schema (billing_address_json, metadata_json), pour rester homogene.
-- -------------------------------------------------------------------------------------
ALTER TABLE service_catalog_item
    ADD COLUMN IF NOT EXISTS detailed_description TEXT,
    ADD COLUMN IF NOT EXISTS included_items       TEXT,
    ADD COLUMN IF NOT EXISTS image_url            VARCHAR(500);


-- -------------------------------------------------------------------------------------
-- 3. Aide a la saisie et classement
-- -------------------------------------------------------------------------------------
ALTER TABLE service_catalog_item
    ADD COLUMN IF NOT EXISTS billing_mode       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS default_quantity   NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS min_quantity       NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS max_quantity       NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS taxable_by_default BOOLEAN,
    ADD COLUMN IF NOT EXISTS subcategory        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tags               TEXT,
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(120),
    ADD COLUMN IF NOT EXISTS valid_from         DATE,
    ADD COLUMN IF NOT EXISTS valid_until        DATE;

ALTER TABLE service_catalog_item
    ADD CONSTRAINT ck_service_catalog_item_billing_mode
        CHECK (billing_mode IS NULL
               OR billing_mode IN ('UNIT', 'HOURLY', 'DAILY', 'MONTHLY', 'FIXED')) NOT VALID;

-- Une fenetre de validite fermee doit se tenir dans le bon ordre.
ALTER TABLE service_catalog_item
    ADD CONSTRAINT ck_service_catalog_item_validity_window
        CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_until >= valid_from) NOT VALID;


-- -------------------------------------------------------------------------------------
-- 4. Index de recherche
--
--    subcategory et external_reference servent au filtrage du catalogue et a la
--    reconciliation avec un systeme tiers. Index partiels : la majorite des articles
--    n'ont ni l'une ni l'autre.
-- -------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_service_catalog_item_subcategory
    ON service_catalog_item (subcategory)
    WHERE subcategory IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_service_catalog_item_external_reference
    ON service_catalog_item (external_reference)
    WHERE external_reference IS NOT NULL;
