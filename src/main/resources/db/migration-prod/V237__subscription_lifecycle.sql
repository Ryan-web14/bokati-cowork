-- Lot H · cycle de vie des abonnements
-- Engagement, resiliation avec preavis et liste de sortie, devis, prelevement automatique,
-- gel encadre, prorata explicite. Les seuils sont des donnees : une politique, une ligne.

-- ---------------------------------------------------------------------------------------------
-- 1. La politique · ce qui vaut pour tous, modifiable sans livrer
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_policy (
    id                              BIGINT PRIMARY KEY,
    policy_code                     VARCHAR(40)  NOT NULL UNIQUE,
    name                            VARCHAR(160) NOT NULL,
    -- Le meme prorata pour l'entree, la sortie et le changement de plan · sinon les trois divergent
    proration_policy                VARCHAR(20)  NOT NULL DEFAULT 'DAILY',
    default_notice_days             INTEGER      NOT NULL DEFAULT 30,
    early_termination_formula       VARCHAR(30)  NOT NULL DEFAULT 'PERCENT_OF_REMAINING',
    early_termination_percent       NUMERIC(9,4) NOT NULL DEFAULT 50,
    early_termination_fixed_fee     NUMERIC(19,4),
    freeze_max_per_year             INTEGER      NOT NULL DEFAULT 2,
    freeze_max_days                 INTEGER      NOT NULL DEFAULT 60,
    freeze_notice_days              INTEGER      NOT NULL DEFAULT 0,
    -- Part du prix de la periode facturee pendant le gel · 0 = rien, 100 = tout
    freeze_fee_percent              NUMERIC(9,4) NOT NULL DEFAULT 0,
    grace_period_days               INTEGER      NOT NULL DEFAULT 7,
    suspension_after_grace_days     INTEGER      NOT NULL DEFAULT 14,
    quote_validity_days             INTEGER      NOT NULL DEFAULT 30,
    active                          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_subscription_policy_proration CHECK (proration_policy IN ('DAILY', 'MONTH_STARTED', 'NONE')),
    CONSTRAINT ck_subscription_policy_formula CHECK (early_termination_formula IN ('NONE', 'FIXED_FEE', 'PERCENT_OF_REMAINING', 'REMAINING_PERIODS')),
    CONSTRAINT ck_subscription_policy_percents CHECK (
        early_termination_percent BETWEEN 0 AND 100 AND freeze_fee_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_subscription_policy_days CHECK (
        default_notice_days >= 0 AND freeze_max_per_year >= 0 AND freeze_max_days >= 0 AND freeze_notice_days >= 0
        AND grace_period_days >= 0 AND suspension_after_grace_days >= 0 AND quote_validity_days > 0)
);

INSERT INTO subscription_policy (id, policy_code, name)
SELECT 2410001, 'DEFAULT', 'Politique par défaut'
WHERE NOT EXISTS (SELECT 1 FROM subscription_policy WHERE policy_code = 'DEFAULT');

-- ---------------------------------------------------------------------------------------------
-- 2. L'engagement · ce que l'abonne a promis, et ce que coute d'y renoncer
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_commitment (
    id                              BIGINT PRIMARY KEY,
    subscription_id                 BIGINT       NOT NULL UNIQUE,
    commitment_months               INTEGER      NOT NULL,
    commitment_start                DATE         NOT NULL,
    commitment_end                  DATE         NOT NULL,
    early_termination_formula       VARCHAR(30)  NOT NULL,
    early_termination_percent       NUMERIC(9,4),
    early_termination_fixed_fee     NUMERIC(19,4),
    auto_renew_commitment           BOOLEAN      NOT NULL DEFAULT FALSE,
    source                          VARCHAR(20)  NOT NULL DEFAULT 'PLAN',
    created_by                      VARCHAR(120),
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_commitment_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_subscription_commitment_months CHECK (commitment_months > 0),
    CONSTRAINT ck_subscription_commitment_dates CHECK (commitment_end >= commitment_start),
    CONSTRAINT ck_subscription_commitment_formula CHECK (early_termination_formula IN ('NONE', 'FIXED_FEE', 'PERCENT_OF_REMAINING', 'REMAINING_PERIODS')),
    CONSTRAINT ck_subscription_commitment_source CHECK (source IN ('PLAN', 'QUOTE', 'MANUAL'))
);

-- ---------------------------------------------------------------------------------------------
-- 3. La resiliation · un preavis qui court, une liste de sortie qui se coche
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_termination (
    id                              BIGINT PRIMARY KEY,
    termination_code                VARCHAR(40)  NOT NULL UNIQUE,
    subscription_id                 BIGINT       NOT NULL,
    requested_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    requested_by                    VARCHAR(120) NOT NULL,
    channel                         VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    notice_period_days              INTEGER      NOT NULL,
    effective_date                  DATE         NOT NULL,
    reason                          TEXT,
    reason_category                 VARCHAR(40)  NOT NULL,
    early_termination               BOOLEAN      NOT NULL DEFAULT FALSE,
    remaining_commitment_months     INTEGER      NOT NULL DEFAULT 0,
    fee_amount                      NUMERIC(19,4) NOT NULL DEFAULT 0,
    fee_waived                      BOOLEAN      NOT NULL DEFAULT FALSE,
    fee_waived_by                   VARCHAR(120),
    fee_waived_reason               TEXT,
    fee_billable_number             VARCHAR(100),
    bridging_billable_number        VARCHAR(100),
    status                          VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    accepted_by                     VARCHAR(120),
    accepted_at                     TIMESTAMPTZ,
    retracted_by                    VARCHAR(120),
    retracted_at                    TIMESTAMPTZ,
    completed_at                    TIMESTAMPTZ,
    notes                           TEXT,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_termination_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_subscription_termination_status CHECK (status IN ('REQUESTED', 'ACCEPTED', 'RETRACTED', 'COMPLETED')),
    CONSTRAINT ck_subscription_termination_channel CHECK (channel IN ('STAFF', 'PORTAL', 'LETTER', 'EMAIL')),
    CONSTRAINT ck_subscription_termination_category CHECK (reason_category IN (
        'RELOCATION', 'COST', 'SERVICE_QUALITY', 'BUSINESS_CLOSURE', 'NO_LONGER_NEEDED', 'COMPETITOR', 'NON_PAYMENT', 'OTHER')),
    CONSTRAINT ck_subscription_termination_fee CHECK (fee_amount >= 0),
    CONSTRAINT ck_subscription_termination_waiver CHECK (fee_waived = FALSE OR fee_waived_by IS NOT NULL)
);

-- Un seul preavis en cours par abonnement
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_termination_open
    ON subscription_termination(subscription_id) WHERE status IN ('REQUESTED', 'ACCEPTED');
CREATE INDEX IF NOT EXISTS idx_subscription_termination_effective
    ON subscription_termination(status, effective_date);

CREATE TABLE IF NOT EXISTS subscription_exit_item (
    id                              BIGINT PRIMARY KEY,
    termination_id                  BIGINT       NOT NULL,
    item_code                       VARCHAR(40)  NOT NULL,
    label                           VARCHAR(200) NOT NULL,
    mandatory                       BOOLEAN      NOT NULL DEFAULT TRUE,
    status                          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    detail                          TEXT,
    done_by                         VARCHAR(120),
    done_at                         TIMESTAMPTZ,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_exit_item_termination FOREIGN KEY (termination_id) REFERENCES subscription_termination(id),
    CONSTRAINT ck_subscription_exit_item_status CHECK (status IN ('PENDING', 'DONE', 'WAIVED')),
    CONSTRAINT uk_subscription_exit_item UNIQUE (termination_id, item_code)
);

-- ---------------------------------------------------------------------------------------------
-- 4. Le devis · un abonnement promis, pas encore souscrit
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_quote (
    id                              BIGINT PRIMARY KEY,
    quote_number                    VARCHAR(40)  NOT NULL UNIQUE,
    subscriber_type                 VARCHAR(40)  NOT NULL,
    subscriber_code                 VARCHAR(120) NOT NULL,
    subscriber_name                 VARCHAR(200),
    subscriber_email                VARCHAR(200),
    plan_version_id                 BIGINT       NOT NULL,
    billing_cycle                   VARCHAR(40)  NOT NULL,
    currency                        VARCHAR(3)   NOT NULL,
    catalogue_price                 NUMERIC(19,4) NOT NULL,
    quoted_price                    NUMERIC(19,4) NOT NULL,
    setup_fee                       NUMERIC(19,4) NOT NULL DEFAULT 0,
    commitment_months               INTEGER      NOT NULL DEFAULT 0,
    trial_days                      INTEGER      NOT NULL DEFAULT 0,
    start_date                      DATE,
    services_json                   JSONB,
    notes                           TEXT,
    status                          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    valid_until                     DATE         NOT NULL,
    prepared_by                     VARCHAR(120) NOT NULL,
    sent_at                         TIMESTAMPTZ,
    accepted_at                     TIMESTAMPTZ,
    accepted_by                     VARCHAR(120),
    rejected_at                     TIMESTAMPTZ,
    rejection_reason                TEXT,
    converted_subscription_number   VARCHAR(100),
    derivation_code                 VARCHAR(40),
    proposal_number                 VARCHAR(60),
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_quote_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT ck_subscription_quote_status CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CONVERTED')),
    CONSTRAINT ck_subscription_quote_amounts CHECK (catalogue_price >= 0 AND quoted_price >= 0 AND setup_fee >= 0),
    CONSTRAINT ck_subscription_quote_converted CHECK (status <> 'CONVERTED' OR converted_subscription_number IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS idx_subscription_quote_subscriber ON subscription_quote(subscriber_type, subscriber_code, status);
CREATE INDEX IF NOT EXISTS idx_subscription_quote_validity ON subscription_quote(status, valid_until);

-- ---------------------------------------------------------------------------------------------
-- 5. Le prelevement · un mandat donne, une tentative par echeance
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_debit_mandate (
    id                              BIGINT PRIMARY KEY,
    mandate_code                    VARCHAR(40)  NOT NULL UNIQUE,
    subscription_id                 BIGINT       NOT NULL,
    wallet_number                   VARCHAR(60)  NOT NULL,
    status                          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    consent_given_at                TIMESTAMPTZ  NOT NULL,
    consent_given_by                VARCHAR(120) NOT NULL,
    consent_channel                 VARCHAR(20)  NOT NULL,
    consent_reference               VARCHAR(120),
    max_amount_per_debit            NUMERIC(19,4),
    consecutive_failures            INTEGER      NOT NULL DEFAULT 0,
    last_debit_at                   TIMESTAMPTZ,
    revoked_at                      TIMESTAMPTZ,
    revoked_by                      VARCHAR(120),
    revocation_reason               TEXT,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_debit_mandate_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_subscription_debit_mandate_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT ck_subscription_debit_mandate_channel CHECK (consent_channel IN ('PORTAL', 'SIGNED_FORM', 'EMAIL', 'STAFF')),
    CONSTRAINT ck_subscription_debit_mandate_max CHECK (max_amount_per_debit IS NULL OR max_amount_per_debit > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_debit_mandate_active
    ON subscription_debit_mandate(subscription_id) WHERE status IN ('ACTIVE', 'SUSPENDED');

CREATE TABLE IF NOT EXISTS subscription_debit_attempt (
    id                              BIGINT PRIMARY KEY,
    mandate_id                      BIGINT       NOT NULL,
    subscription_id                 BIGINT       NOT NULL,
    invoice_number                  VARCHAR(100) NOT NULL,
    amount                          NUMERIC(19,4) NOT NULL,
    currency                        VARCHAR(3)   NOT NULL,
    status                          VARCHAR(30)  NOT NULL,
    message                         TEXT,
    transaction_number              VARCHAR(100),
    executed_at                     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_debit_attempt_mandate FOREIGN KEY (mandate_id) REFERENCES subscription_debit_mandate(id),
    CONSTRAINT ck_subscription_debit_attempt_status CHECK (status IN ('SUCCEEDED', 'INSUFFICIENT_FUNDS', 'OVER_LIMIT', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_subscription_debit_attempt_subscription ON subscription_debit_attempt(subscription_id, executed_at DESC);

-- ---------------------------------------------------------------------------------------------
-- 6. Le gel · chaque gel est compte, borne, et facture selon la politique
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_freeze (
    id                              BIGINT PRIMARY KEY,
    subscription_id                 BIGINT       NOT NULL,
    started_on                      DATE         NOT NULL,
    planned_until                   DATE         NOT NULL,
    resumed_on                      DATE,
    days_planned                    INTEGER      NOT NULL,
    days_effective                  INTEGER,
    fee_amount                      NUMERIC(19,4) NOT NULL DEFAULT 0,
    fee_billable_number             VARCHAR(100),
    reason                          TEXT,
    requested_by                    VARCHAR(120) NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_freeze_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_subscription_freeze_dates CHECK (planned_until >= started_on),
    CONSTRAINT ck_subscription_freeze_days CHECK (days_planned > 0)
);
CREATE INDEX IF NOT EXISTS idx_subscription_freeze_subscription ON subscription_freeze(subscription_id, started_on DESC);

-- ---------------------------------------------------------------------------------------------
-- 7. Changement de plan · la politique de prorata appliquee est memorisee avec le changement
-- ---------------------------------------------------------------------------------------------
ALTER TABLE subscription_change_request ADD COLUMN IF NOT EXISTS proration_policy VARCHAR(20);
ALTER TABLE subscription_change_request ADD COLUMN IF NOT EXISTS proration_billable_number VARCHAR(100);
-- Le montant est toujours positif (contrainte existante) · ce drapeau dit s'il est du par le client ou lui est rendu
ALTER TABLE subscription_change_request ADD COLUMN IF NOT EXISTS proration_credit BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------------------------------------------------------------------------------------------
-- 8. Sequences
-- ---------------------------------------------------------------------------------------------
INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2410101, 'subscription_termination', 'Resiliation d abonnement', 'Sequence des preavis de resiliation', 'TRM', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'subscription_termination');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2410102, 'subscription_quote', 'Devis d abonnement', 'Sequence des devis avant souscription', 'QTE', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'subscription_quote');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2410103, 'subscription_debit_mandate', 'Mandat de prelevement', 'Sequence des mandats de prelevement sur portefeuille', 'MDT', null, '{PREFIX}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'subscription_debit_mandate');
