-- Portefeuille · les usages.
--
-- Le socle de confiance (V232) rendait le portefeuille sur : code secret, plafonds, journal chaine.
-- Ce qui suit lui donne des raisons d'exister pour son titulaire. Chaque usage a sa table parce que
-- chacun a son cycle de vie propre, et qu'un transfert n'est pas une demande de paiement qui ne
-- serait pas un appareil : les fondre en une table « operation » obligerait a lire le type avant
-- de savoir quelles colonnes ont un sens.

-- ---------------------------------------------------------------------------------------------
-- 1. Transfert entre abonnes
-- ---------------------------------------------------------------------------------------------
--
-- Deux ecritures, une par portefeuille, mais un seul fait : le transfert. C'est lui qui porte le
-- statut, la confirmation, la raison d'un echec. Les ecritures ne sont que ses traces au grand
-- livre, et chacune y pointe pour qu'on retrouve le fait depuis la trace.

CREATE TABLE IF NOT EXISTS wallet_transfer (
    id BIGINT PRIMARY KEY,
    transfer_number VARCHAR(100) NOT NULL UNIQUE,
    transfer_uuid UUID NOT NULL UNIQUE,
    source_wallet_id BIGINT NOT NULL,
    target_wallet_id BIGINT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    fee_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    message VARCHAR(255),
    confirmation_code VARCHAR(100),
    payment_request_number VARCHAR(100),
    debit_entry_number VARCHAR(100),
    credit_entry_number VARCHAR(100),
    fee_entry_number VARCHAR(100),
    initiated_by VARCHAR(120),
    ip_address VARCHAR(60),
    device_id VARCHAR(120),
    failure_reason VARCHAR(255),
    expires_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_transfer_source FOREIGN KEY (source_wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT fk_wallet_transfer_target FOREIGN KEY (target_wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_wallet_transfer_amount CHECK (amount > 0 AND fee_amount >= 0),
    -- Un portefeuille ne se transfere pas a lui-meme : ce ne serait ni un transfert ni une erreur
    -- rattrapable, juste deux ecritures qui s'annulent et une ligne de plus dans le journal.
    CONSTRAINT ck_wallet_transfer_distinct CHECK (source_wallet_id <> target_wallet_id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_transfer_source ON wallet_transfer(source_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_transfer_target ON wallet_transfer(target_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_transfer_status ON wallet_transfer(status, expires_at);

-- ---------------------------------------------------------------------------------------------
-- 2. Beneficiaires enregistres
-- ---------------------------------------------------------------------------------------------
--
-- Un alias, un portefeuille cible, et le compte des envois deja faits. Ce dernier point n'est pas
-- decoratif : un premier envoi vers un inconnu et un dixieme envoi vers un habitue ne meritent pas
-- la meme vigilance, et la surveillance (lot D bis) lira cette colonne.

CREATE TABLE IF NOT EXISTS wallet_beneficiary (
    id BIGINT PRIMARY KEY,
    owner_wallet_id BIGINT NOT NULL,
    beneficiary_wallet_id BIGINT NOT NULL,
    alias VARCHAR(120) NOT NULL,
    transfer_count INTEGER NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_beneficiary_owner FOREIGN KEY (owner_wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_beneficiary_target FOREIGN KEY (beneficiary_wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
    CONSTRAINT uk_wallet_beneficiary UNIQUE (owner_wallet_id, beneficiary_wallet_id),
    CONSTRAINT ck_wallet_beneficiary_distinct CHECK (owner_wallet_id <> beneficiary_wallet_id)
);

-- ---------------------------------------------------------------------------------------------
-- 3. Demandes de paiement
-- ---------------------------------------------------------------------------------------------
--
-- Une demande n'est pas un transfert inverse : elle ne deplace rien. Elle attend que le payeur la
-- regle par un transfert ordinaire, avec son code et ses plafonds · aucune demande ne peut
-- contourner ce que le transfert exige.

CREATE TABLE IF NOT EXISTS wallet_payment_request (
    id BIGINT PRIMARY KEY,
    request_number VARCHAR(100) NOT NULL UNIQUE,
    requester_wallet_id BIGINT NOT NULL,
    payer_wallet_id BIGINT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    transfer_number VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_request_requester FOREIGN KEY (requester_wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT fk_wallet_request_payer FOREIGN KEY (payer_wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_wallet_request_amount CHECK (amount > 0),
    CONSTRAINT ck_wallet_request_distinct CHECK (requester_wallet_id <> payer_wallet_id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_request_payer ON wallet_payment_request(payer_wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_wallet_request_requester ON wallet_payment_request(requester_wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_wallet_request_expiry ON wallet_payment_request(status, expires_at);

-- ---------------------------------------------------------------------------------------------
-- 4. Journal des appareils
-- ---------------------------------------------------------------------------------------------
--
-- Chaque appareil qui a demande une confirmation est note. Un appareil jamais vu qui tente un
-- transfert important est le signal le plus simple et le plus fiable dont on dispose · il ne
-- demande aucune intelligence, seulement d'avoir tenu la liste.

CREATE TABLE IF NOT EXISTS wallet_device (
    id BIGINT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    device_id VARCHAR(120) NOT NULL,
    label VARCHAR(120),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_ip_address VARCHAR(60),
    use_count INTEGER NOT NULL DEFAULT 1,
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT fk_wallet_device_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
    CONSTRAINT uk_wallet_device UNIQUE (wallet_id, device_id)
);

-- ---------------------------------------------------------------------------------------------
-- 5. Preferences du titulaire · alertes et notifications
-- ---------------------------------------------------------------------------------------------

ALTER TABLE wallet_account
    ADD COLUMN IF NOT EXISTS low_balance_threshold NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS low_balance_alerted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS notify_on_credit BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notify_on_debit BOOLEAN NOT NULL DEFAULT TRUE;

-- L'alerte de solde bas ne se repete pas a chaque debit sous le seuil : elle part une fois, et se
-- rearme quand le solde repasse au-dessus. Sans cette date, un titulaire a 500 F recevrait un
-- courriel par cafe.
COMMENT ON COLUMN wallet_account.low_balance_alerted_at IS
    'Date de la derniere alerte de solde bas · nulle tant que le solde est au-dessus du seuil';

-- ---------------------------------------------------------------------------------------------
-- 6. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2340001, 'wallet_transfer', 'Transfert de portefeuille', 'Sequence des transferts entre abonnes', 'WTR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_transfer');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2340002, 'wallet_payment_request', 'Demande de paiement', 'Sequence des demandes de paiement entre abonnes', 'WPR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'wallet_payment_request');
