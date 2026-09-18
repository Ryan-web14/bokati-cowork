-- Parrainage.
--
-- Trois tables, et une regle qui commande la conception : un parrainage se qualifie, il ne se
-- declare pas. Recompenser a l'inscription reviendrait a payer pour des comptes crees et jamais
-- utilises, ce qui est exactement ce qu'un programme de parrainage attire quand il paie trop tot.
-- D'ou qualification_rule, qui dit ce qui declenche reellement la recompense.
--
-- Les recompenses reutilisent promotion_reward plutot que d'inventer un second vocabulaire : un
-- parrainage qui offre dix pour cent, un mois, ou un credit de portefeuille n'a aucune raison de
-- s'exprimer autrement qu'une campagne qui offre la meme chose. Le programme designe donc deux
-- promotions porteuses, celle du parrain et celle du filleul.

-- ---------------------------------------------------------------------------------------------
-- 1. Le programme
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS referral_program (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    referrer_promotion_id BIGINT,
    referee_promotion_id BIGINT,
    qualification_rule VARCHAR(60) NOT NULL DEFAULT 'FIRST_PAYMENT',
    qualification_delay_days INTEGER,
    max_referrals_per_referrer INTEGER,
    currency VARCHAR(3),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_referral_program_referrer_promotion
        FOREIGN KEY (referrer_promotion_id) REFERENCES promotion(id) ON DELETE SET NULL,
    CONSTRAINT fk_referral_program_referee_promotion
        FOREIGN KEY (referee_promotion_id) REFERENCES promotion(id) ON DELETE SET NULL,
    CONSTRAINT ck_referral_program_validity CHECK (
        valid_from IS NULL OR valid_until IS NULL OR valid_until >= valid_from
    ),
    -- Un programme qui ne recompense personne n'est pas un programme.
    CONSTRAINT ck_referral_program_rewards CHECK (
        referrer_promotion_id IS NOT NULL OR referee_promotion_id IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_referral_program_status
    ON referral_program(status, valid_from, valid_until);

COMMENT ON COLUMN referral_program.qualification_rule IS
    'Ce qui declenche la recompense · SIGNUP, FIRST_PAYMENT, TENURE_REACHED';
COMMENT ON COLUMN referral_program.max_referrals_per_referrer IS
    'Plafond par parrain · sans lui, un programme genereux devient une source de revenus a lui seul';

-- ---------------------------------------------------------------------------------------------
-- 2. Le lien de parrainage
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS referral_link (
    id BIGINT PRIMARY KEY,
    program_id BIGINT NOT NULL,
    referrer_type VARCHAR(60) NOT NULL,
    referrer_code VARCHAR(120) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    click_count INTEGER NOT NULL DEFAULT 0,
    signup_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_referral_link_program
        FOREIGN KEY (program_id) REFERENCES referral_program(id) ON DELETE CASCADE,
    CONSTRAINT ck_referral_link_counts CHECK (click_count >= 0 AND signup_count >= 0)
);

-- Un parrain n'a qu'un lien par programme. Lui en donner plusieurs eclaterait ses statistiques
-- sans rien lui apporter, et rendrait le plafond par parrain contournable.
CREATE UNIQUE INDEX IF NOT EXISTS uk_referral_link_referrer
    ON referral_link(program_id, referrer_type, referrer_code);

CREATE INDEX IF NOT EXISTS idx_referral_link_referrer
    ON referral_link(referrer_type, referrer_code);

-- ---------------------------------------------------------------------------------------------
-- 3. Le parrainage lui-meme
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS referral (
    id BIGINT PRIMARY KEY,
    referral_number VARCHAR(100) NOT NULL UNIQUE,
    program_id BIGINT NOT NULL,
    link_id BIGINT,
    referrer_type VARCHAR(60) NOT NULL,
    referrer_code VARCHAR(120) NOT NULL,
    referee_type VARCHAR(60) NOT NULL,
    referee_code VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    qualified_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason VARCHAR(255),
    referrer_reward_granted BOOLEAN NOT NULL DEFAULT FALSE,
    referrer_reward_amount NUMERIC(19,4),
    referrer_rewarded_at TIMESTAMPTZ,
    referee_reward_granted BOOLEAN NOT NULL DEFAULT FALSE,
    referee_reward_amount NUMERIC(19,4),
    referee_rewarded_at TIMESTAMPTZ,
    currency VARCHAR(3),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_referral_program
        FOREIGN KEY (program_id) REFERENCES referral_program(id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_link
        FOREIGN KEY (link_id) REFERENCES referral_link(id) ON DELETE SET NULL,
    -- On ne se parraine pas soi-meme. C'est la premiere chose que quelqu'un essaie.
    CONSTRAINT ck_referral_not_self CHECK (
        NOT (lower(referrer_type) = lower(referee_type) AND lower(referrer_code) = lower(referee_code))
    )
);

-- Un filleul n'est parraine qu'une fois par programme, quel que soit le parrain. Sans cela, deux
-- parrains reclameraient la meme recompense pour la meme personne.
CREATE UNIQUE INDEX IF NOT EXISTS uk_referral_referee
    ON referral(program_id, referee_type, referee_code);

CREATE INDEX IF NOT EXISTS idx_referral_referrer
    ON referral(referrer_type, referrer_code, status);
CREATE INDEX IF NOT EXISTS idx_referral_status
    ON referral(status, registered_at);

COMMENT ON TABLE referral IS
    'Un parrainage se qualifie, il ne se declare pas · recompenser a l inscription revient a payer pour des comptes jamais utilises';

-- ---------------------------------------------------------------------------------------------
-- 4. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2280001, 'referral', 'Parrainage', 'Sequence des parrainages', 'PAR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'referral');
