-- Operationnalisation des pass.
--
-- Le modele etait bon, c'est le parcours d'usage qui manquait : le moment ou quelqu'un se presente
-- et ou le pass doit etre valide, decompte, ou refuse. transferable et shareable etaient deux
-- booleens sans processus derriere, COMPANY_SHARED_PASS n'avait pas de modele de beneficiaires, et
-- le geste qui decompte au moment ou quelqu'un arrive n'existait nulle part.
--
-- Cinq tables, et un principe qui les tient : il n'y a qu'une seule facon de valider un pass. Le
-- guichet, la reservation en ligne et la borne appellent le meme service. Dupliquer cette regle
-- entre trois modules produirait trois comportements divergents, et c'est toujours celui qu'on n'a
-- pas teste qui laisse passer.

-- ---------------------------------------------------------------------------------------------
-- 1. L'usage · ce qui s'est reellement passe
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pass_usage (
    id BIGINT PRIMARY KEY,
    usage_number VARCHAR(100) NOT NULL UNIQUE,
    pass_id BIGINT NOT NULL,
    used_by_type VARCHAR(60),
    used_by_code VARCHAR(120),
    usage_type VARCHAR(40) NOT NULL,
    location_code VARCHAR(120),
    resource_code VARCHAR(120),
    entitlement_code VARCHAR(120),
    quantity NUMERIC(19,4) NOT NULL DEFAULT 1,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    status VARCHAR(40) NOT NULL DEFAULT 'COMPLETED',
    validated_by VARCHAR(120),
    validation_channel VARCHAR(40),
    reference_type VARCHAR(60),
    reference_code VARCHAR(120),
    reversed_at TIMESTAMPTZ,
    reversal_reason VARCHAR(255),
    reversed_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pass_usage_pass
        FOREIGN KEY (pass_id) REFERENCES subscription_pass(id) ON DELETE CASCADE,
    CONSTRAINT ck_pass_usage_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_pass_usage_pass ON pass_usage(pass_id, status);
CREATE INDEX IF NOT EXISTS idx_pass_usage_user ON pass_usage(used_by_type, used_by_code, created_at);
CREATE INDEX IF NOT EXISTS idx_pass_usage_day ON pass_usage(pass_id, started_at);

-- Un meme geste ne se compte qu'une fois. Un double scan a la borne, ou un client qui reappuie
-- parce que rien ne s'est affiche, ne doit pas consommer deux journees.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_usage_reference
    ON pass_usage (pass_id, reference_type, reference_code)
    WHERE reference_type IS NOT NULL AND reference_code IS NOT NULL AND reversed_at IS NULL;

COMMENT ON COLUMN pass_usage.used_by_code IS
    'Celui qui s en sert · pas forcement le titulaire, un pass partage servant a plusieurs';

-- ---------------------------------------------------------------------------------------------
-- 2. Regles de validite fines
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pass_validity_rule (
    id BIGINT PRIMARY KEY,
    pass_id BIGINT,
    pass_version_id BIGINT,
    rule_type VARCHAR(40) NOT NULL,
    value VARCHAR(255),
    value_list TEXT,
    allow_rule BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pass_validity_rule_pass
        FOREIGN KEY (pass_id) REFERENCES subscription_pass(id) ON DELETE CASCADE,
    CONSTRAINT fk_pass_validity_rule_version
        FOREIGN KEY (pass_version_id) REFERENCES pass_plan_version(id) ON DELETE CASCADE,
    -- Une regle porte sur un pass ou sur un plan, jamais sur les deux ni sur rien.
    CONSTRAINT ck_pass_validity_rule_scope CHECK (
        (pass_id IS NOT NULL AND pass_version_id IS NULL)
        OR (pass_id IS NULL AND pass_version_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_pass_validity_rule_pass ON pass_validity_rule(pass_id);
CREATE INDEX IF NOT EXISTS idx_pass_validity_rule_version ON pass_validity_rule(pass_version_id);

COMMENT ON COLUMN pass_validity_rule.allow_rule IS
    'Vrai la regle autorise, faux elle interdit · un jour ferie s exprime par une interdiction';

-- ---------------------------------------------------------------------------------------------
-- 3. Beneficiaires · ce qui donne un sens a COMPANY_SHARED_PASS
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pass_beneficiary (
    id BIGINT PRIMARY KEY,
    pass_id BIGINT NOT NULL,
    beneficiary_type VARCHAR(60) NOT NULL,
    beneficiary_code VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL DEFAULT 'AUTHORISED_USER',
    max_uses_for_beneficiary INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    added_by VARCHAR(120),
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    revoked_by VARCHAR(120),
    revoked_reason VARCHAR(255),
    CONSTRAINT fk_pass_beneficiary_pass
        FOREIGN KEY (pass_id) REFERENCES subscription_pass(id) ON DELETE CASCADE,
    CONSTRAINT ck_pass_beneficiary_counts CHECK (
        used_count >= 0
        AND (max_uses_for_beneficiary IS NULL OR max_uses_for_beneficiary > 0)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_beneficiary
    ON pass_beneficiary(pass_id, beneficiary_type, beneficiary_code);

CREATE INDEX IF NOT EXISTS idx_pass_beneficiary_person
    ON pass_beneficiary(beneficiary_type, beneficiary_code, revoked_at);

-- Une entreprise achete vingt journees, designe cinq collaborateurs, et plafonne chacun a six. Le
-- quota individuel vit dans le quota global : la somme des quotas peut le depasser, c'est le
-- premier arrive qui consomme.
COMMENT ON COLUMN pass_beneficiary.max_uses_for_beneficiary IS
    'Quota individuel dans le quota global · nul si le beneficiaire n est pas plafonne a titre propre';

-- ---------------------------------------------------------------------------------------------
-- 4. Transfert de titulaire
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pass_transfer (
    id BIGINT PRIMARY KEY,
    transfer_number VARCHAR(100) NOT NULL UNIQUE,
    pass_id BIGINT NOT NULL,
    from_owner_type VARCHAR(60) NOT NULL,
    from_owner_code VARCHAR(120) NOT NULL,
    to_owner_type VARCHAR(60) NOT NULL,
    to_owner_code VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_ACCEPTANCE',
    reason VARCHAR(255),
    transfer_fee NUMERIC(19,4),
    currency VARCHAR(3),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    requested_by VARCHAR(120),
    accepted_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason VARCHAR(255),
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT fk_pass_transfer_pass
        FOREIGN KEY (pass_id) REFERENCES subscription_pass(id) ON DELETE CASCADE,
    CONSTRAINT ck_pass_transfer_not_self CHECK (
        NOT (lower(from_owner_type) = lower(to_owner_type) AND lower(from_owner_code) = lower(to_owner_code))
    )
);

CREATE INDEX IF NOT EXISTS idx_pass_transfer_pass ON pass_transfer(pass_id, status);
CREATE INDEX IF NOT EXISTS idx_pass_transfer_recipient ON pass_transfer(to_owner_type, to_owner_code, status);

-- Un pass n'a qu'un transfert en cours. Deux demandes simultanees vers deux destinataires
-- differents donneraient le pass a celui qui accepte le second.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_transfer_pending
    ON pass_transfer (pass_id)
    WHERE status = 'PENDING_ACCEPTANCE';

-- ---------------------------------------------------------------------------------------------
-- 5. Materialisation
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pass_credential (
    id BIGINT PRIMARY KEY,
    pass_id BIGINT NOT NULL,
    credential_type VARCHAR(40) NOT NULL,
    value VARCHAR(255) NOT NULL,
    rotating BOOLEAN NOT NULL DEFAULT FALSE,
    valid_until TIMESTAMPTZ,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    issued_by VARCHAR(120),
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(255),
    CONSTRAINT fk_pass_credential_pass
        FOREIGN KEY (pass_id) REFERENCES subscription_pass(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pass_credential_pass ON pass_credential(pass_id, revoked_at);

-- La valeur d'un support actif est unique : c'est par elle qu'on retrouve le pass a la borne, et
-- deux pass partageant un code rendraient la lecture ambigue.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_credential_value
    ON pass_credential (value)
    WHERE revoked_at IS NULL;

COMMENT ON COLUMN pass_credential.rotating IS
    'Un code tournant limite la copie d ecran · le support change, le pass reste';

-- ---------------------------------------------------------------------------------------------
-- 6. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2290001, 'pass_usage', 'Usage de pass', 'Sequence des utilisations de pass', 'USG', null, '{PREFIX}-{YYYY}{MM}{DD}-{SEQ}', 6, 1, 1, 'DAILY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'pass_usage');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2290002, 'pass_transfer', 'Transfert de pass', 'Sequence des transferts de pass', 'PTR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'pass_transfer');
