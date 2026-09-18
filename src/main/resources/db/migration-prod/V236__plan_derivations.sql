-- Abonnements derives · un prix et des avantages propres, sans multiplier les plans.
--
-- Accorder a un client un prix ou des avantages differents n'avait qu'une issue : creer un plan
-- dedie. Multiplie par chaque negociation, cela produit un catalogue a mille plans ou plus
-- personne ne sait quel est le tarif de reference. Plutot qu'une table d'exceptions posee a cote
-- du modele, on reutilise la version de plan qui existe deja, en creant une version privee
-- rattachee a un seul abonnement. La facturation, les droits et le calcul de periode lisent une
-- version de plan · une version privee se comporte exactement comme une version publique. Ce qui
-- change tient en une ligne : les versions de portee SUBSCRIPTION sont exclues du catalogue.

-- ---------------------------------------------------------------------------------------------
-- 1. La version de plan sait a qui elle appartient
-- ---------------------------------------------------------------------------------------------

ALTER TABLE subscription_plan_version
    ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'CATALOGUE',
    ADD COLUMN IF NOT EXISTS owner_subscription_id BIGINT,
    ADD COLUMN IF NOT EXISTS derived_from_version_id BIGINT,
    ADD COLUMN IF NOT EXISTS floor_price NUMERIC(19,4);

ALTER TABLE subscription_plan_version
    DROP CONSTRAINT IF EXISTS ck_plan_version_scope;
ALTER TABLE subscription_plan_version
    ADD CONSTRAINT ck_plan_version_scope CHECK (
        (scope = 'CATALOGUE' AND owner_subscription_id IS NULL)
        OR (scope = 'SUBSCRIPTION' AND owner_subscription_id IS NOT NULL AND derived_from_version_id IS NOT NULL)
    );

ALTER TABLE subscription_plan_version
    DROP CONSTRAINT IF EXISTS fk_plan_version_owner_subscription;
ALTER TABLE subscription_plan_version
    ADD CONSTRAINT fk_plan_version_owner_subscription FOREIGN KEY (owner_subscription_id) REFERENCES subscription(id);

ALTER TABLE subscription_plan_version
    DROP CONSTRAINT IF EXISTS fk_plan_version_derived_from;
ALTER TABLE subscription_plan_version
    ADD CONSTRAINT fk_plan_version_derived_from FOREIGN KEY (derived_from_version_id) REFERENCES subscription_plan_version(id);

CREATE INDEX IF NOT EXISTS idx_plan_version_scope ON subscription_plan_version(plan_id, scope, status);
CREATE INDEX IF NOT EXISTS idx_plan_version_owner ON subscription_plan_version(owner_subscription_id) WHERE owner_subscription_id IS NOT NULL;

COMMENT ON COLUMN subscription_plan_version.floor_price IS
    'Prix plancher d une version de catalogue · aucune derivation n est acceptee en dessous, meme approuvee';

-- ---------------------------------------------------------------------------------------------
-- 2. La derivation · versionnee, datee, motivee, approuvee au-dela d un seuil
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS plan_derivation (
    id BIGINT PRIMARY KEY,
    derivation_code VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    source_plan_version_id BIGINT NOT NULL,
    derived_plan_version_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    reason VARCHAR(40) NOT NULL,
    reason_details VARCHAR(1000),
    effective_from DATE,
    effective_to DATE,
    renewal_behaviour VARCHAR(30) NOT NULL DEFAULT 'KEEP',
    revert_after_periods INTEGER,
    periods_applied INTEGER NOT NULL DEFAULT 0,
    promotions_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    total_impact_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_percent NUMERIC(9,4),
    currency VARCHAR(3),
    requested_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    approved_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    applied_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    supersedes_derivation_id BIGINT,
    batch_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_plan_derivation_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_plan_derivation_source FOREIGN KEY (source_plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT fk_plan_derivation_derived FOREIGN KEY (derived_plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT fk_plan_derivation_supersedes FOREIGN KEY (supersedes_derivation_id) REFERENCES plan_derivation(id),
    CONSTRAINT ck_plan_derivation_revert CHECK (renewal_behaviour <> 'REVERT_AFTER_PERIODS' OR revert_after_periods >= 1),
    -- Le second visa ne peut pas etre celui qui a demande.
    CONSTRAINT ck_plan_derivation_four_eyes CHECK (approved_by IS NULL OR approved_by <> requested_by)
);

CREATE INDEX IF NOT EXISTS idx_plan_derivation_subscription ON plan_derivation(subscription_id, status);
CREATE INDEX IF NOT EXISTS idx_plan_derivation_status ON plan_derivation(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_plan_derivation_batch ON plan_derivation(batch_code) WHERE batch_code IS NOT NULL;

-- Une seule derivation active par abonnement · la suivante remplace la precedente, et le dit.
CREATE UNIQUE INDEX IF NOT EXISTS uk_plan_derivation_active
    ON plan_derivation(subscription_id) WHERE status = 'ACTIVE';

COMMENT ON COLUMN plan_derivation.total_impact_amount IS
    'Ecart chiffre sur une periode par rapport au catalogue · positif si concession, negatif si majoration';

-- ---------------------------------------------------------------------------------------------
-- 3. Le delta · ce qui a ete concede, et combien
-- ---------------------------------------------------------------------------------------------
--
-- Sans lui, on sait qu un client a un traitement particulier, mais personne ne peut dire ce qui a
-- ete concede ni combien cela coute. Avec lui, on produit la liste des concessions par commercial,
-- par periode, par montant.

CREATE TABLE IF NOT EXISTS plan_derivation_delta (
    id BIGINT PRIMARY KEY,
    derivation_id BIGINT NOT NULL,
    delta_type VARCHAR(30) NOT NULL,
    target_code VARCHAR(120),
    catalogue_value VARCHAR(255),
    derived_value VARCHAR(255),
    impact_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_plan_derivation_delta_derivation FOREIGN KEY (derivation_id) REFERENCES plan_derivation(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_plan_derivation_delta ON plan_derivation_delta(derivation_id);

-- ---------------------------------------------------------------------------------------------
-- 4. Regles d approbation · au-dela d un rabais, un visa
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS plan_derivation_approval_rule (
    id BIGINT PRIMARY KEY,
    rule_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    max_discount_percent_without_approval NUMERIC(9,4) NOT NULL DEFAULT 10,
    max_impact_without_approval NUMERIC(19,4),
    max_discount_percent_allowed NUMERIC(9,4),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON COLUMN plan_derivation_approval_rule.max_discount_percent_allowed IS
    'Au-dela, refuse meme avec un visa · nul si seul le prix plancher borne';

INSERT INTO plan_derivation_approval_rule (id, rule_code, name, max_discount_percent_without_approval, max_impact_without_approval, max_discount_percent_allowed)
SELECT 2400001, 'PDR-DEFAULT', 'Regle par defaut', 10, 50000, 50
WHERE NOT EXISTS (SELECT 1 FROM plan_derivation_approval_rule WHERE rule_code = 'PDR-DEFAULT');

-- ---------------------------------------------------------------------------------------------
-- 5. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2400101, 'plan_derivation', 'Derivation de plan', 'Sequence des abonnements derives', 'DRV', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'plan_derivation');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2400102, 'plan_derivation_batch', 'Lot de derivations', 'Sequence des creations en lot', 'DRB', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 4, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'plan_derivation_batch');
