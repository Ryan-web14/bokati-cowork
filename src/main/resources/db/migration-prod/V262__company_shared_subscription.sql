-- Tranche 4 du centre de gestion des entreprises · l'abonnement d'une organisation partage entre ses
-- collaborateurs.
--
-- L'organisation detient l'abonnement et ses droits (entitlement_grant a son nom). Un collaborateur
-- consomme par sa place (subscription_seat). Chaque droit se repartit selon un mode :
--   POOL           pot commun, chaque place y puise jusqu'a epuisement (plafond facultatif) ;
--   QUOTA          part nominative fixe par place ;
--   QUOTA_AND_POOL sa part d'abord, puis le pot commun (ce qui n'est pas distribue en parts).
-- Sans regle pour un droit, il est en POOL.

-- Nombre de places au plus · vide = sans limite.
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS max_seats INTEGER;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS ck_subscription_max_seats;
ALTER TABLE subscription ADD CONSTRAINT ck_subscription_max_seats CHECK (max_seats IS NULL OR max_seats >= 0);

-- Numero court de la place · l'identifiant genere depasse ce que lit JavaScript.
CREATE SEQUENCE IF NOT EXISTS subscription_seat_short_id_seq START WITH 1;
ALTER TABLE subscription_seat ADD COLUMN IF NOT EXISTS short_id BIGINT;
ALTER SEQUENCE subscription_seat_short_id_seq OWNED BY subscription_seat.short_id;
UPDATE subscription_seat SET short_id = nextval('subscription_seat_short_id_seq') WHERE short_id IS NULL;
ALTER TABLE subscription_seat ALTER COLUMN short_id SET DEFAULT nextval('subscription_seat_short_id_seq');
ALTER TABLE subscription_seat ALTER COLUMN short_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_seat_short_id ON subscription_seat (short_id);
ALTER TABLE subscription_seat ADD COLUMN IF NOT EXISTS removed_reason VARCHAR(500);
ALTER TABLE subscription_seat ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(150);

-- La regle de repartition d'un droit de l'abonnement.
CREATE TABLE IF NOT EXISTS subscription_entitlement_sharing (
    id               BIGINT PRIMARY KEY,
    subscription_id  BIGINT        NOT NULL REFERENCES subscription (id),
    entitlement_code VARCHAR(100)  NOT NULL,
    sharing_mode     VARCHAR(30)   NOT NULL,
    -- Part de chaque place quand la place n'en precise pas · QUOTA et QUOTA_AND_POOL.
    default_quota    NUMERIC(19, 4),
    -- Plafond de consommation par place sur la periode · facultatif.
    seat_cap         NUMERIC(19, 4),
    updated_by       VARCHAR(150),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    CONSTRAINT uk_subscription_entitlement_sharing UNIQUE (subscription_id, entitlement_code),
    CONSTRAINT ck_subscription_entitlement_sharing_mode CHECK (sharing_mode IN ('POOL', 'QUOTA', 'QUOTA_AND_POOL')),
    CONSTRAINT ck_subscription_entitlement_sharing_amounts
        CHECK ((default_quota IS NULL OR default_quota >= 0) AND (seat_cap IS NULL OR seat_cap >= 0))
);

-- Ce qu'une place recoit en propre sur un droit · remplace la regle pour cette place.
CREATE TABLE IF NOT EXISTS subscription_seat_allowance (
    id               BIGINT PRIMARY KEY,
    seat_id          BIGINT        NOT NULL REFERENCES subscription_seat (id),
    entitlement_code VARCHAR(100)  NOT NULL,
    quota            NUMERIC(19, 4),
    cap              NUMERIC(19, 4),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    CONSTRAINT uk_subscription_seat_allowance UNIQUE (seat_id, entitlement_code),
    CONSTRAINT ck_subscription_seat_allowance_amounts CHECK ((quota IS NULL OR quota >= 0) AND (cap IS NULL OR cap >= 0))
);

-- Ce que chaque place a pris, reservation par reservation · la part nominative d'abord, le pot ensuite.
CREATE TABLE IF NOT EXISTS subscription_seat_usage (
    id               BIGINT PRIMARY KEY,
    seat_id          BIGINT        NOT NULL REFERENCES subscription_seat (id),
    subscription_id  BIGINT        NOT NULL REFERENCES subscription (id),
    member_code      VARCHAR(120)  NOT NULL,
    entitlement_code VARCHAR(100)  NOT NULL,
    period_start     DATE          NOT NULL,
    reference_type   VARCHAR(80)   NOT NULL,
    reference_id     VARCHAR(120)  NOT NULL,
    quantity         NUMERIC(19, 4) NOT NULL,
    from_quota       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    from_pool        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status           VARCHAR(20)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    CONSTRAINT uk_subscription_seat_usage_reference UNIQUE (seat_id, entitlement_code, reference_type, reference_id),
    CONSTRAINT ck_subscription_seat_usage_status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED')),
    CONSTRAINT ck_subscription_seat_usage_amounts CHECK (quantity >= 0 AND from_quota >= 0 AND from_pool >= 0)
);
CREATE INDEX IF NOT EXISTS idx_subscription_seat_usage_period
    ON subscription_seat_usage (subscription_id, entitlement_code, period_start);

-- Une reservation prise sur le droit d'une organisation · le titulaire du droit et la place.
ALTER TABLE booking ADD COLUMN IF NOT EXISTS entitlement_owner_type VARCHAR(60);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS entitlement_owner_code VARCHAR(120);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS subscription_seat_id BIGINT;

-- L'usage garde le collaborateur qui a consomme le droit de l'organisation.
ALTER TABLE usage_record ADD COLUMN IF NOT EXISTS beneficiary_type VARCHAR(60);
ALTER TABLE usage_record ADD COLUMN IF NOT EXISTS beneficiary_code VARCHAR(120);
