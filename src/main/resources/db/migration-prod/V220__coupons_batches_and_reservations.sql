-- Coupons · le code saisi par le client, distinct de la campagne qui le porte.
--
-- Le module n'avait que coupon_redemption, qui enregistre qu'un abonne a utilise une promotion.
-- C'est un journal, pas un code : rien ne permettait de generer mille codes uniques pour un
-- partenariat, de distinguer un code public partage sur les reseaux d'un code nominatif a usage
-- unique, ni de suivre le taux d'utilisation d'un lot.
--
-- Trois tables, et une regle qui commande tout le reste : un coupon n'est consomme qu'au paiement,
-- jamais a la saisie. Un code a usage unique consomme des la saisie serait perdu si le panier est
-- abandonne, et le client n'aurait plus rien a montrer. Reservation temporaire, puis capture ou
-- liberation · la meme mecanique que les retenues de stock et de portefeuille, qui existent deja.

-- ---------------------------------------------------------------------------------------------
-- 1. Lot de generation
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS coupon_batch (
    id BIGINT PRIMARY KEY,
    batch_code VARCHAR(100) NOT NULL UNIQUE,
    promotion_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    requested_quantity INTEGER NOT NULL,
    generated_quantity INTEGER NOT NULL DEFAULT 0,
    redeemed_quantity INTEGER NOT NULL DEFAULT 0,
    code_prefix VARCHAR(40),
    code_length INTEGER NOT NULL DEFAULT 10,
    coupon_kind VARCHAR(40) NOT NULL,
    max_redemptions_per_coupon INTEGER,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    channel VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_coupon_batch_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE,
    CONSTRAINT ck_coupon_batch_quantities CHECK (
        requested_quantity > 0
        AND generated_quantity >= 0
        AND redeemed_quantity >= 0
        AND generated_quantity <= requested_quantity
    )
);

CREATE INDEX IF NOT EXISTS idx_coupon_batch_promotion ON coupon_batch(promotion_id);

COMMENT ON TABLE coupon_batch IS
    'Campagne de codes · mille codes imprimes sur des flyers se suivent par leur lot, pas un a un';

-- ---------------------------------------------------------------------------------------------
-- 2. Le coupon
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    promotion_id BIGINT NOT NULL,
    batch_id BIGINT,
    coupon_kind VARCHAR(40) NOT NULL,
    assigned_to_type VARCHAR(60),
    assigned_to_code VARCHAR(120),
    max_redemptions INTEGER,
    redemption_count INTEGER NOT NULL DEFAULT 0,
    reserved_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    issued_by VARCHAR(120),
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(255),
    revoked_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_coupon_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_batch
        FOREIGN KEY (batch_id) REFERENCES coupon_batch(id) ON DELETE SET NULL,
    CONSTRAINT ck_coupon_counts CHECK (
        redemption_count >= 0
        AND reserved_count >= 0
        AND (max_redemptions IS NULL OR redemption_count <= max_redemptions)
    ),
    -- Un coupon nominatif designe un titulaire ou aucun, jamais un type sans code.
    CONSTRAINT ck_coupon_assignment CHECK (
        (assigned_to_type IS NULL AND assigned_to_code IS NULL)
        OR (assigned_to_type IS NOT NULL AND assigned_to_code IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_coupon_promotion ON coupon(promotion_id);
CREATE INDEX IF NOT EXISTS idx_coupon_batch ON coupon(batch_id);
CREATE INDEX IF NOT EXISTS idx_coupon_assignee ON coupon(assigned_to_type, assigned_to_code, status);
CREATE INDEX IF NOT EXISTS idx_coupon_status_validity ON coupon(status, valid_from, valid_until);

COMMENT ON COLUMN coupon.coupon_kind IS
    'SINGLE_USE, MULTI_USE, UNIQUE_PER_SUBSCRIBER · un code public et un code nominatif a usage unique sont deux objets differents dans la vie reelle';
COMMENT ON COLUMN coupon.reserved_count IS
    'Reservations en cours · un code retenu par un panier n en est pas encore consomme';

-- ---------------------------------------------------------------------------------------------
-- 3. Reservation
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS coupon_reservation (
    id BIGINT PRIMARY KEY,
    reservation_number VARCHAR(100) NOT NULL UNIQUE,
    coupon_id BIGINT NOT NULL,
    cart_reference VARCHAR(120) NOT NULL,
    subscriber_type VARCHAR(60),
    subscriber_code VARCHAR(120),
    discount_amount NUMERIC(19,4),
    currency VARCHAR(3),
    status VARCHAR(40) NOT NULL DEFAULT 'RESERVED',
    expires_at TIMESTAMPTZ NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reserved_by VARCHAR(120),
    captured_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    release_reason VARCHAR(255),
    CONSTRAINT fk_coupon_reservation_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupon(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coupon_reservation_coupon ON coupon_reservation(coupon_id, status);
CREATE INDEX IF NOT EXISTS idx_coupon_reservation_cart ON coupon_reservation(cart_reference, status);
CREATE INDEX IF NOT EXISTS idx_coupon_reservation_expiry ON coupon_reservation(status, expires_at);

-- Un panier ne retient un code qu'une fois. Sans cela un rafraichissement de page empilerait les
-- reservations et epuiserait un code a usages multiples sans qu aucune vente n ait lieu.
CREATE UNIQUE INDEX IF NOT EXISTS uk_coupon_reservation_active
    ON coupon_reservation (coupon_id, cart_reference)
    WHERE status = 'RESERVED';

COMMENT ON TABLE coupon_reservation IS
    'Retenue temporaire d un code pendant qu un panier se construit · capturee au paiement, liberee sinon';

-- ---------------------------------------------------------------------------------------------
-- 4. Le journal d utilisation retrouve le code employe
-- ---------------------------------------------------------------------------------------------

-- coupon_redemption disait quelle promotion un abonne avait utilisee, jamais par quel code. Sur
-- une campagne a mille codes, cela rendait le taux d utilisation par lot incalculable.
ALTER TABLE coupon_redemption
    ADD COLUMN IF NOT EXISTS coupon_id BIGINT;

ALTER TABLE coupon_redemption
    DROP CONSTRAINT IF EXISTS fk_coupon_redemption_coupon;
ALTER TABLE coupon_redemption
    ADD CONSTRAINT fk_coupon_redemption_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupon(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_coupon_redemption_coupon ON coupon_redemption(coupon_id);

-- ---------------------------------------------------------------------------------------------
-- 5. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2240001, 'coupon_batch', 'Lot de coupons', 'Sequence des lots de generation de coupons', 'CPB', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'coupon_batch');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2240002, 'coupon_reservation', 'Reservation de coupon', 'Sequence des retenues temporaires de coupons', 'CPR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'coupon_reservation');
