-- Portefeuille · centre de controle.
--
-- Deux principes, poses ici en contraintes plutot qu'en consignes : aucune action administrative
-- sans motif saisi, et un second visa sur les operations sensibles. Le premier est un NOT NULL ; le
-- second est un statut qui ne devient EXECUTED que sous la main d'un autre.

-- ---------------------------------------------------------------------------------------------
-- 1. Actions administratives, tracees
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS wallet_admin_action (
    id BIGINT PRIMARY KEY,
    action_number VARCHAR(100) NOT NULL UNIQUE,
    wallet_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    amount NUMERIC(19,4),
    currency VARCHAR(3),
    reference VARCHAR(180),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'EXECUTED',
    requested_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    approved_at TIMESTAMPTZ,
    rejected_by VARCHAR(120),
    rejected_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    executed_at TIMESTAMPTZ,
    result_reference VARCHAR(180),
    payload_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_admin_action_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_wallet_admin_action_reason CHECK (length(trim(reason)) >= 5),
    -- Le second visa ne peut pas etre celui qui a demande : c'est tout le sens de la separation.
    CONSTRAINT ck_wallet_admin_action_four_eyes CHECK (approved_by IS NULL OR approved_by <> requested_by)
);

CREATE INDEX IF NOT EXISTS idx_wallet_admin_action_wallet ON wallet_admin_action(wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_admin_action_status ON wallet_admin_action(status, created_at DESC);

-- Une action tracee ne se reecrit pas. Une erreur se corrige par une nouvelle action, qui dit
-- qu'elle corrige la precedente · c'est ainsi que la trace reste une trace.
CREATE OR REPLACE FUNCTION wallet_admin_action_guard()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Une action administrative de portefeuille ne se supprime pas'
            USING ERRCODE = '23514';
    END IF;
    -- Seuls les champs de decision et d'execution peuvent evoluer, jamais la demande elle-meme.
    IF NEW.wallet_id <> OLD.wallet_id
       OR NEW.action_type <> OLD.action_type
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.reason <> OLD.reason
       OR NEW.requested_by <> OLD.requested_by
       OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'La demande d une action administrative ne se modifie pas · seule sa decision evolue'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_wallet_admin_action_guard ON wallet_admin_action;
CREATE TRIGGER trg_wallet_admin_action_guard
    BEFORE UPDATE OR DELETE ON wallet_admin_action
    FOR EACH ROW
    EXECUTE FUNCTION wallet_admin_action_guard();

-- ---------------------------------------------------------------------------------------------
-- 2. Signalements
-- ---------------------------------------------------------------------------------------------
--
-- Le socle que la surveillance (lot D bis) enrichira de regles parametrables. Ici, les signalements
-- que le systeme sait deja produire sans regle : rafale de codes faux, appareil revoque, chaine
-- rompue, reactivation d'un portefeuille dormant, tentative au-dela du plafond.

CREATE TABLE IF NOT EXISTS wallet_risk_flag (
    id BIGINT PRIMARY KEY,
    flag_number VARCHAR(100) NOT NULL UNIQUE,
    wallet_id BIGINT NOT NULL,
    flag_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    details VARCHAR(1000),
    reference VARCHAR(180),
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    detected_by VARCHAR(120) NOT NULL DEFAULT 'SYSTEM',
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMPTZ,
    resolution VARCHAR(1000),
    CONSTRAINT fk_wallet_risk_flag_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_risk_flag_open ON wallet_risk_flag(status, severity, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_risk_flag_wallet ON wallet_risk_flag(wallet_id, detected_at DESC);

-- Un meme signal ne s'ouvre pas deux fois tant que le premier n'est pas traite · sinon une rafale
-- de codes faux produirait une rafale de signalements, et la revue s'y noierait.
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_risk_flag_open_once
    ON wallet_risk_flag(wallet_id, flag_type) WHERE status = 'OPEN';

-- ---------------------------------------------------------------------------------------------
-- 3. Rapprochement avec la tresorerie
-- ---------------------------------------------------------------------------------------------
--
-- La contrepartie directe du choix de ne pas etre emetteur de monnaie electronique : personne
-- n'impose de cantonner les fonds, donc c'est a l'etablissement de verifier qu'il peut honorer les
-- prestations deja payees. Un ecart persistant signifie que des avances clients ont finance autre
-- chose que ce pour quoi elles ont ete versees.

CREATE TABLE IF NOT EXISTS wallet_treasury_reconciliation (
    id BIGINT PRIMARY KEY,
    reconciliation_number VARCHAR(100) NOT NULL UNIQUE,
    reconciliation_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    total_wallet_balance NUMERIC(19,4) NOT NULL,
    total_held_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    wallet_count INTEGER NOT NULL DEFAULT 0,
    ledger_account_balance NUMERIC(19,4),
    available_cash NUMERIC(19,4),
    coverage_ratio NUMERIC(9,4),
    variance NUMERIC(19,4),
    variance_explained BOOLEAN NOT NULL DEFAULT FALSE,
    explained_by VARCHAR(500),
    status VARCHAR(40) NOT NULL DEFAULT 'BALANCED',
    prepared_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_wallet_treasury_date UNIQUE (reconciliation_date, currency)
);

-- ---------------------------------------------------------------------------------------------
-- 4. Derogation de plafond bornee dans le temps
-- ---------------------------------------------------------------------------------------------

ALTER TABLE wallet_account
    ADD COLUMN IF NOT EXISTS limit_policy_until TIMESTAMPTZ;

COMMENT ON COLUMN wallet_account.limit_policy_until IS
    'Fin de la derogation de plafond · au-dela, le portefeuille retombe sur son palier';

-- ---------------------------------------------------------------------------------------------
-- 5. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2350001, 'wallet_admin_action', 'Action administrative portefeuille', 'Sequence des actions administratives sur les portefeuilles', 'WAA', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_admin_action');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2350002, 'wallet_risk_flag', 'Signalement portefeuille', 'Sequence des signalements de surveillance', 'WRF', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_risk_flag');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2350003, 'wallet_treasury_reconciliation', 'Rapprochement tresorerie portefeuille', 'Sequence des rapprochements entre encours et tresorerie', 'WTS', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_treasury_reconciliation');
