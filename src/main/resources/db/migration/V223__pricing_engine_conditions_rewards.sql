-- Moteur de tarification · conditions, recompenses et tracabilite des remises.
--
-- Le module promotion existait sans effet. On pouvait creer une promotion, l'activer, enregistrer
-- qu'un abonne l'avait utilisee, mais aucun montant n'etait jamais calcule ni applique nulle part.
-- promotion et coupon_redemption n'etaient references par aucun autre module, et
-- billing_document_discount.discount_code etait un texte libre sans clef vers eux.
--
-- Trois manques de schema l'expliquaient.
--
-- 1. Une promotion ne portait qu'un type de remise et une valeur. « Dix pour cent sur le premier
--    mois, pour un nouveau membre, sur un plan mensuel » n'etait donc pas exprimable : il aurait
--    fallu coder chaque idee commerciale. Les conditions et les recompenses deviennent des lignes.
--
-- 2. Rien ne disait comment deux promotions se combinent, ni ce qu'une campagne a le droit de
--    couter. Priorite, cumul, exclusivite, plafond et budget sont ajoutes a la promotion.
--
-- 3. Une remise appliquee ne laissait aucune trace exploitable. On ne pouvait donc repondre ni a
--    « combien nous ont coute les promotions ce trimestre », ni a « quelle campagne a produit
--    cette remise ».

-- ---------------------------------------------------------------------------------------------
-- 1. La promotion apprend a se combiner, et a couter une somme bornee
-- ---------------------------------------------------------------------------------------------

ALTER TABLE promotion
    ADD COLUMN IF NOT EXISTS promotion_type VARCHAR(40) NOT NULL DEFAULT 'AUTOMATIC',
    ADD COLUMN IF NOT EXISTS stackable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS exclusive BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS max_redemptions_per_subscriber INTEGER,
    ADD COLUMN IF NOT EXISTS max_discount_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS budget_amount NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS consumed_budget_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_discount_granted NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS counterpart_account VARCHAR(60),
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Le budget consomme ne peut pas depasser le budget accorde. C'est la garantie qu'une campagne
-- epuisee cesse de s'appliquer, y compris sous forte concurrence : la contrainte tient meme si
-- deux paniers simultanes passent le controle applicatif.
ALTER TABLE promotion
    DROP CONSTRAINT IF EXISTS ck_promotion_budget;
ALTER TABLE promotion
    ADD CONSTRAINT ck_promotion_budget CHECK (
        consumed_budget_amount >= 0
        AND (budget_amount IS NULL OR consumed_budget_amount <= budget_amount)
    );

ALTER TABLE promotion
    DROP CONSTRAINT IF EXISTS ck_promotion_max_discount;
ALTER TABLE promotion
    ADD CONSTRAINT ck_promotion_max_discount CHECK (
        max_discount_amount IS NULL OR max_discount_amount > 0
    );

COMMENT ON COLUMN promotion.stackable IS
    'La promotion accepte-t-elle de s ajouter a une autre deja retenue';
COMMENT ON COLUMN promotion.exclusive IS
    'Si elle s applique, l evaluation des suivantes s arrete';
COMMENT ON COLUMN promotion.priority IS
    'Ordre croissant d evaluation · a egalite, la plus avantageuse pour le client l emporte';
COMMENT ON COLUMN promotion.max_discount_amount IS
    'Plafond absolu, meme pour un pourcentage · 80 % sur un abonnement annuel n est presque jamais l intention';

-- ---------------------------------------------------------------------------------------------
-- 2. Conditions · ce qui doit etre vrai pour que la promotion s applique
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS promotion_condition (
    id BIGINT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    condition_type VARCHAR(60) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    value VARCHAR(255),
    value_list TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_promotion_condition_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_promotion_condition_promotion
    ON promotion_condition(promotion_id);

COMMENT ON TABLE promotion_condition IS
    'Conditions d application, en donnees et non en code · toutes doivent etre vraies';
COMMENT ON COLUMN promotion_condition.value_list IS
    'Valeurs separees par des virgules, pour les operateurs IN, NOT_IN et BETWEEN';

-- ---------------------------------------------------------------------------------------------
-- 3. Recompenses · ce que la promotion accorde, et sur quoi
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS promotion_reward (
    id BIGINT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    reward_type VARCHAR(60) NOT NULL,
    value NUMERIC(19,4),
    target_scope VARCHAR(40) NOT NULL DEFAULT 'WHOLE_ORDER',
    target_code VARCHAR(120),
    max_amount NUMERIC(19,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_promotion_reward_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE,
    CONSTRAINT ck_promotion_reward_value CHECK (value IS NULL OR value >= 0),
    CONSTRAINT ck_promotion_reward_max CHECK (max_amount IS NULL OR max_amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_promotion_reward_promotion
    ON promotion_reward(promotion_id);

-- target_scope couvre plusieurs modules a dessein : un plan, un pass, un service, une ressource
-- reservable ou un article vendu au comptoir. Le moteur ne connait pas le module d origine, il
-- manipule un objet tarifable designe par une portee et un code. C est ce qui evite d ecrire
-- trois moteurs de promotion.
COMMENT ON COLUMN promotion_reward.target_scope IS
    'WHOLE_ORDER, LINE, PLAN, ADDON, PASS, ENTITLEMENT, SERVICE, RESOURCE, INVENTORY_ITEM, CATEGORY';

-- ---------------------------------------------------------------------------------------------
-- 4. Tracabilite · une remise calculee doit pouvoir etre rattachee a son origine
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS applied_discount (
    id BIGINT PRIMARY KEY,
    discount_number VARCHAR(100) NOT NULL UNIQUE,
    source_type VARCHAR(40) NOT NULL,
    source_code VARCHAR(120) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    document_code VARCHAR(120) NOT NULL,
    line_reference VARCHAR(120),
    subscriber_type VARCHAR(60),
    subscriber_code VARCHAR(120),
    original_amount NUMERIC(19,4) NOT NULL,
    discount_amount NUMERIC(19,4) NOT NULL,
    final_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(255),
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    applied_by VARCHAR(120),
    reversed_at TIMESTAMPTZ,
    reversal_reason VARCHAR(255),
    reversed_by VARCHAR(120),
    CONSTRAINT ck_applied_discount_amounts CHECK (
        original_amount >= 0
        AND discount_amount >= 0
        AND final_amount >= 0
        AND discount_amount <= original_amount
    )
);

CREATE INDEX IF NOT EXISTS idx_applied_discount_source
    ON applied_discount(source_type, source_code);
CREATE INDEX IF NOT EXISTS idx_applied_discount_document
    ON applied_discount(document_type, document_code);
CREATE INDEX IF NOT EXISTS idx_applied_discount_subscriber
    ON applied_discount(subscriber_type, subscriber_code, applied_at);

-- Aucune remise ne descend le total sous zero : le reliquat doit devenir un credit explicite, pas
-- une facture negative. La contrainte le rend impossible plutot que deconseille.
COMMENT ON TABLE applied_discount IS
    'Trace exploitable de chaque remise accordee · cout des campagnes, taux d utilisation, marge apres remise';

-- ---------------------------------------------------------------------------------------------
-- 5. La remise de facture retrouve son origine
-- ---------------------------------------------------------------------------------------------

-- billing_document_discount fonctionne et n est pas remplace : on lui ajoute une clef vers la
-- source plutot que de creer une table concurrente. discount_code restait un texte libre, donc
-- une remise ne pouvait pas etre rattachee a la campagne qui l avait produite.
ALTER TABLE billing_document_discount
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_code VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_billing_discount_source
    ON billing_document_discount(source_type, source_code);

-- ---------------------------------------------------------------------------------------------
-- 6. Sequence des remises accordees
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2230001, 'applied_discount', 'Remise accordee', 'Sequence des remises reellement accordees', 'DSC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'applied_discount');
