-- Portefeuille · socle de confiance.
--
-- Le socle existant est bon : trois soldes distincts, verrou optimiste, cle d'idempotence par
-- ecriture, retenues. Mais il manque tout ce qui permet d'ouvrir le portefeuille a l'initiative du
-- client, et c'est precisement ce qui est demande ensuite.
--
-- Quatre manques, chacun bloquant pour la suite.
--
-- 1. Aucun secret. Le mot « PIN » n'apparait nulle part dans le code. Ouvrir le transfert entre
--    abonnes sans secret reviendrait a laisser un compte vole vider un solde sans obstacle.
--
-- 2. Aucun plafond. Un portefeuille peut recevoir et depenser sans limite, quel que soit le niveau
--    de verification de son titulaire.
--
-- 3. Une ecriture n'a pas d'identifiant propre. Elle se designe par sa cle d'idempotence, qui est
--    une cle technique et non une reference que l'on peut donner a un client au telephone.
--
-- 4. Le journal est immuable par convention, pas par construction. Rien n'empeche une mise a jour,
--    et rien ne permettrait de prouver qu'il n'y en a pas eu.

-- ---------------------------------------------------------------------------------------------
-- 1. Le secret, et la politique qui decide quand il est exige
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS wallet_security_policy (
    id BIGINT PRIMARY KEY,
    scope VARCHAR(40) NOT NULL DEFAULT 'GLOBAL',
    scope_code VARCHAR(120),
    pin_requirement VARCHAR(40) NOT NULL DEFAULT 'OPTIONAL',
    pin_required_operations TEXT NOT NULL DEFAULT 'TRANSFER',
    pin_length INTEGER NOT NULL DEFAULT 4,
    pin_expiry_days INTEGER,
    max_failed_attempts INTEGER NOT NULL DEFAULT 5,
    lockout_minutes INTEGER NOT NULL DEFAULT 15,
    lockout_escalation BOOLEAN NOT NULL DEFAULT TRUE,
    otp_threshold_amount NUMERIC(19,4),
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_wallet_security_scope CHECK (
        (scope = 'GLOBAL' AND scope_code IS NULL)
        OR (scope <> 'GLOBAL' AND scope_code IS NOT NULL)
    ),
    CONSTRAINT ck_wallet_security_pin_length CHECK (pin_length BETWEEN 4 AND 6)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_security_scope
    ON wallet_security_policy(scope, COALESCE(scope_code, ''));

-- TRANSFER y figure toujours et ne peut en etre retire. La regle ne tient pas par la discipline de
-- celui qui configure : un transfert est la seule operation qui fait sortir de l'argent vers
-- quelqu'un d'autre, sans facture en face et sans annulation possible.
COMMENT ON COLUMN wallet_security_policy.pin_required_operations IS
    'Operations exigeant le code quel que soit pin_requirement · TRANSFER y est impose';

INSERT INTO wallet_security_policy (id, scope, scope_code, pin_requirement, pin_required_operations)
SELECT 2320001, 'GLOBAL', NULL, 'OPTIONAL', 'TRANSFER'
WHERE NOT EXISTS (SELECT 1 FROM wallet_security_policy WHERE scope = 'GLOBAL');

CREATE TABLE IF NOT EXISTS wallet_credential (
    id BIGINT PRIMARY KEY,
    wallet_id BIGINT NOT NULL UNIQUE,
    pin_hash VARCHAR(255) NOT NULL,
    pin_algorithm VARCHAR(60) NOT NULL,
    pin_set_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    pin_expires_at TIMESTAMPTZ,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    lockout_count INTEGER NOT NULL DEFAULT 0,
    must_change_pin BOOLEAN NOT NULL DEFAULT FALSE,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_credential_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
    CONSTRAINT ck_wallet_credential_attempts CHECK (failed_attempts >= 0 AND lockout_count >= 0)
);

-- L'empreinte, jamais le code. Un administrateur ne peut pas le lire, seulement le reinitialiser ·
-- et l'algorithme est stocke pour pouvoir en changer sans invalider les codes existants.
COMMENT ON COLUMN wallet_credential.pin_hash IS
    'Empreinte lente et salee · le code n existe nulle part en clair, pas meme dans un journal';

-- ---------------------------------------------------------------------------------------------
-- 2. Confirmation liee a l'operation exacte
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS wallet_transaction_confirmation (
    id BIGINT PRIMARY KEY,
    confirmation_code VARCHAR(100) NOT NULL UNIQUE,
    wallet_id BIGINT NOT NULL,
    operation_type VARCHAR(60) NOT NULL,
    amount NUMERIC(19,4),
    currency VARCHAR(3),
    counterparty_label VARCHAR(255),
    challenge_type VARCHAR(40) NOT NULL DEFAULT 'PIN',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    payload_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    confirmed_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    ip_address VARCHAR(60),
    device_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_confirmation_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
    CONSTRAINT ck_wallet_confirmation_attempts CHECK (attempts >= 0 AND max_attempts > 0)
);

CREATE INDEX IF NOT EXISTS idx_wallet_confirmation_wallet
    ON wallet_transaction_confirmation(wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_wallet_confirmation_expiry
    ON wallet_transaction_confirmation(status, expires_at);

-- Sans empreinte, un attaquant ferait confirmer un transfert de mille et executerait un transfert
-- de cent mille. L'empreinte lie la confirmation a l'operation exacte, et rien d'autre.
COMMENT ON COLUMN wallet_transaction_confirmation.payload_hash IS
    'Empreinte de l operation · elle ne peut pas changer entre la demande et la confirmation';

-- ---------------------------------------------------------------------------------------------
-- 3. Plafonds adosses au niveau de verification
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS wallet_limit_policy (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    kyc_level INTEGER NOT NULL DEFAULT 1,
    max_balance NUMERIC(19,4),
    max_single_topup NUMERIC(19,4),
    max_daily_topup NUMERIC(19,4),
    max_monthly_topup NUMERIC(19,4),
    max_single_transfer NUMERIC(19,4),
    max_daily_transfer NUMERIC(19,4),
    max_monthly_transfer NUMERIC(19,4),
    max_daily_operations INTEGER,
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_wallet_limit_kyc CHECK (kyc_level >= 1)
);

CREATE INDEX IF NOT EXISTS idx_wallet_limit_kyc ON wallet_limit_policy(kyc_level, active);

-- Un niveau de verification plus eleve leve les plafonds · c'est ce qui donne au client une raison
-- de completer son dossier, plutot qu'un mur sans explication.
INSERT INTO wallet_limit_policy
(id, code, name, kyc_level, max_balance, max_single_topup, max_daily_topup, max_monthly_topup,
 max_single_transfer, max_daily_transfer, max_monthly_transfer, max_daily_operations, currency)
SELECT 2320101, 'WLP-NIVEAU-1', 'Verification minimale', 1,
       200000, 100000, 200000, 500000, 50000, 100000, 300000, 10, 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM wallet_limit_policy WHERE code = 'WLP-NIVEAU-1');

INSERT INTO wallet_limit_policy
(id, code, name, kyc_level, max_balance, max_single_topup, max_daily_topup, max_monthly_topup,
 max_single_transfer, max_daily_transfer, max_monthly_transfer, max_daily_operations, currency)
SELECT 2320102, 'WLP-NIVEAU-2', 'Piece d identite verifiee', 2,
       2000000, 500000, 1000000, 3000000, 300000, 500000, 2000000, 30, 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM wallet_limit_policy WHERE code = 'WLP-NIVEAU-2');

INSERT INTO wallet_limit_policy
(id, code, name, kyc_level, max_balance, max_single_topup, max_daily_topup, max_monthly_topup,
 max_single_transfer, max_daily_transfer, max_monthly_transfer, max_daily_operations, currency)
SELECT 2320103, 'WLP-NIVEAU-3', 'Dossier complet', 3,
       NULL, 2000000, 5000000, NULL, 1000000, 2000000, NULL, 100, 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM wallet_limit_policy WHERE code = 'WLP-NIVEAU-3');

-- ---------------------------------------------------------------------------------------------
-- 4. Identite et chainage des ecritures
-- ---------------------------------------------------------------------------------------------

ALTER TABLE wallet_ledger_entry
    ADD COLUMN IF NOT EXISTS transaction_uuid UUID,
    ADD COLUMN IF NOT EXISTS transaction_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS current_hash VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_ledger_transaction_uuid
    ON wallet_ledger_entry (transaction_uuid) WHERE transaction_uuid IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_ledger_transaction_number
    ON wallet_ledger_entry (transaction_number) WHERE transaction_number IS NOT NULL;

COMMENT ON COLUMN wallet_ledger_entry.transaction_uuid IS
    'Identifiant technique, version 7 · horodate donc croissant, un index ne se fragmente pas';
COMMENT ON COLUMN wallet_ledger_entry.transaction_number IS
    'Reference lisible, prononcable au telephone · ne sert jamais de clef etrangere';
COMMENT ON COLUMN wallet_ledger_entry.current_hash IS
    'Empreinte de l ecriture et de celle qui la precede · une modification rompt la chaine';

-- Le journal etait immuable par convention. Il l'est desormais par construction : une ecriture
-- passee ne se modifie plus et ne se supprime plus, une correction se fait par contre-passation.
CREATE OR REPLACE FUNCTION wallet_ledger_is_append_only()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Une ecriture de portefeuille ne se supprime pas · contre-passez-la'
            USING ERRCODE = '23514';
    END IF;
    RAISE EXCEPTION 'Une ecriture de portefeuille ne se modifie pas · contre-passez-la'
        USING ERRCODE = '23514';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_wallet_ledger_append_only ON wallet_ledger_entry;

CREATE TRIGGER trg_wallet_ledger_append_only
    BEFORE UPDATE OR DELETE ON wallet_ledger_entry
    FOR EACH ROW
    EXECUTE FUNCTION wallet_ledger_is_append_only();

ALTER TABLE wallet_account
    ADD COLUMN IF NOT EXISTS limit_policy_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS dormant_since TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS frozen_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS locked_by_owner_at TIMESTAMPTZ;

-- ---------------------------------------------------------------------------------------------
-- 5. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2320201, 'wallet_transaction', 'Ecriture de portefeuille', 'Sequence lisible des ecritures de portefeuille', 'WTX', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 7, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_transaction');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2320202, 'wallet_confirmation', 'Confirmation de portefeuille', 'Sequence des demandes de confirmation', 'WCF', null, '{PREFIX}-{YYYY}{MM}{DD}-{SEQ}', 6, 1, 1, 'DAILY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_confirmation');
