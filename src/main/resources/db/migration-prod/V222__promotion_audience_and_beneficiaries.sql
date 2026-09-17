-- Ciblage nominatif · designer des personnes, pas seulement des populations.
--
-- Les conditions decrivent des populations : les nouveaux, les mensuels, ceux d'un segment. Il
-- manquait la possibilite de designer des personnes precises, une par une. Sans elle, le geste
-- commercial le plus courant, trente pour cent au client mecontent, obligeait a inventer un segment
-- artificiel qui resterait ensuite dans le systeme sans que personne ne sache pourquoi.
--
-- Trois usages, tous courants : le geste commercial, la liste d'invites d'un partenariat, et la
-- relance des abonnes partis.
--
-- Une regle commande la conception : une promotion nominative n'apparait qu'a ses beneficiaires.
-- Elle ne doit fuiter ni dans le catalogue public, ni dans une reponse consultee par un autre
-- client. C'est pourquoi la requete de visibilite est portee par le depot et non laissee a
-- l'interface.

-- ---------------------------------------------------------------------------------------------
-- 1. A qui la promotion s'adresse
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS promotion_audience (
    id BIGINT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    audience_type VARCHAR(40) NOT NULL,
    segment_code VARCHAR(120),
    plan_code VARCHAR(120),
    business_entity_code VARCHAR(120),
    cohort_rule TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_promotion_audience_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_promotion_audience_promotion
    ON promotion_audience(promotion_id);

COMMENT ON COLUMN promotion_audience.cohort_rule IS
    'Regle de cohorte, par exemple inscrits entre deux dates · en donnees, jamais en code';

-- ---------------------------------------------------------------------------------------------
-- 2. Les beneficiaires nommes
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS promotion_beneficiary (
    id BIGINT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    subscriber_type VARCHAR(60) NOT NULL,
    subscriber_code VARCHAR(120) NOT NULL,
    added_by VARCHAR(120),
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    added_reason VARCHAR(255),
    notified_at TIMESTAMPTZ,
    notification_channel VARCHAR(60),
    redeemed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by VARCHAR(120),
    revoked_reason VARCHAR(255),
    CONSTRAINT fk_promotion_beneficiary_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE
);

-- Une personne ne figure qu'une fois sur la liste d'une promotion. Un double import ne doit pas
-- lui donner deux droits, ni rendre le taux d'utilisation faux.
CREATE UNIQUE INDEX IF NOT EXISTS uk_promotion_beneficiary
    ON promotion_beneficiary(promotion_id, subscriber_type, subscriber_code);

CREATE INDEX IF NOT EXISTS idx_promotion_beneficiary_subscriber
    ON promotion_beneficiary(subscriber_type, subscriber_code, revoked_at);

COMMENT ON COLUMN promotion_beneficiary.notified_at IS
    'Qui a ete prevenu, quand et par quel canal · faute de quoi un client reclamera une offre qu il n a jamais recue';
COMMENT ON COLUMN promotion_beneficiary.revoked_at IS
    'Retrait avant utilisation · seule facon d annuler un geste accorde par erreur, la ligne restant pour la trace';

-- ---------------------------------------------------------------------------------------------
-- 3. Sequence
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2260001, 'promotion_beneficiary_import', 'Import de beneficiaires', 'Sequence des imports de listes de beneficiaires', 'IMB', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'promotion_beneficiary_import');
