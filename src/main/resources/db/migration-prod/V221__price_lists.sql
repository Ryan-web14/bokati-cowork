-- Grilles tarifaires · un tarif negocie n'est pas une remise.
--
-- La distinction n'est pas cosmetique. Une remise se lit sur la facture comme une faveur ponctuelle
-- et se cumule avec d'autres ; un tarif negocie est un terme de contrat, il remplace le prix
-- catalogue et ne s'affiche pas comme une reduction exceptionnelle. Afficher « 30 % de remise » a
-- un partenaire institutionnel qui a signe un tarif au poste, c'est lui suggerer chaque mois qu'il
-- pourrait obtenir mieux.
--
-- Quatre situations reelles dans un espace de coworking, toutes couvertes par ce modele : un
-- partenaire dont les membres paient un tarif negocie, une entreprise qui a signe un volume et
-- obtient un prix au poste, un tarif etudiant ou association, et un degressif au-dela de cinq
-- postes.

-- ---------------------------------------------------------------------------------------------
-- 1. La grille
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS price_list (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    currency VARCHAR(3) NOT NULL,
    audience_type VARCHAR(40) NOT NULL DEFAULT 'ALL',
    audience_code VARCHAR(120),
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    priority INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Une grille destinee a quelqu'un en particulier designe ce quelqu'un. Une grille pour tous
    -- ne designe personne. L'entre-deux, un type sans code, ne veut rien dire.
    CONSTRAINT ck_price_list_audience CHECK (
        (audience_type = 'ALL' AND audience_code IS NULL)
        OR (audience_type <> 'ALL' AND audience_code IS NOT NULL)
    ),
    CONSTRAINT ck_price_list_validity CHECK (
        valid_from IS NULL OR valid_until IS NULL OR valid_until >= valid_from
    )
);

CREATE INDEX IF NOT EXISTS idx_price_list_audience
    ON price_list(audience_type, audience_code, active);
CREATE INDEX IF NOT EXISTS idx_price_list_validity
    ON price_list(active, valid_from, valid_until, priority);

COMMENT ON COLUMN price_list.priority IS
    'En cas de chevauchement, la plus prioritaire gagne · une seule grille s applique a une ligne';

-- ---------------------------------------------------------------------------------------------
-- 2. Les lignes de la grille
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS price_list_entry (
    id BIGINT PRIMARY KEY,
    price_list_id BIGINT NOT NULL,
    target_scope VARCHAR(40) NOT NULL,
    target_code VARCHAR(120),
    price_mode VARCHAR(40) NOT NULL,
    value NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3),
    min_quantity INTEGER NOT NULL DEFAULT 1,
    billing_cycle VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_price_list_entry_list
        FOREIGN KEY (price_list_id) REFERENCES price_list(id) ON DELETE CASCADE,
    CONSTRAINT ck_price_list_entry_value CHECK (value >= 0),
    CONSTRAINT ck_price_list_entry_quantity CHECK (min_quantity >= 1)
);

CREATE INDEX IF NOT EXISTS idx_price_list_entry_list
    ON price_list_entry(price_list_id);
CREATE INDEX IF NOT EXISTS idx_price_list_entry_target
    ON price_list_entry(target_scope, target_code, min_quantity);

-- Le palier de quantite est ce qui rend le degressif possible sans table supplementaire : trois
-- lignes sur le meme objet, a partir de un, de cinq et de dix postes. La ligne retenue est celle
-- dont le palier est le plus eleve parmi ceux que la quantite atteint.
COMMENT ON COLUMN price_list_entry.min_quantity IS
    'Palier · la ligne retenue est celle au palier le plus eleve que la quantite atteint';
COMMENT ON COLUMN price_list_entry.price_mode IS
    'FIXED_PRICE remplace le prix, PERCENTAGE_OFF_LIST et FIXED_AMOUNT_OFF_LIST le derivent du catalogue';

-- ---------------------------------------------------------------------------------------------
-- 3. Sequence
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2250001, 'price_list', 'Grille tarifaire', 'Sequence des grilles tarifaires', 'GRT', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'price_list');
