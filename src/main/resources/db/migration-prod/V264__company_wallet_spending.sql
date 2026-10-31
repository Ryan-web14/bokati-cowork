-- Tranche 5 du centre de gestion des entreprises · le portefeuille de l'organisation et les depenses
-- que ses collaborateurs y font.
--
-- Le portefeuille lui-meme est un wallet_account au nom de l'organisation (owner_type CUSTOMER).
-- Le gerant autorise un collaborateur a y depenser, operation par operation, sous des plafonds. Une
-- depense au-dela du seuil de validation attend le gerant : ses fonds sont retenus, et sans reponse
-- sous 48 heures elle est annulee.

-- Ce qu'un collaborateur peut depenser sur le portefeuille de son organisation.
CREATE SEQUENCE IF NOT EXISTS wallet_spending_authorization_short_id_seq START WITH 1;
CREATE TABLE IF NOT EXISTS wallet_spending_authorization (
    id                  BIGINT PRIMARY KEY,
    short_id            BIGINT        NOT NULL DEFAULT nextval('wallet_spending_authorization_short_id_seq'),
    wallet_id           BIGINT        NOT NULL REFERENCES wallet_account (id),
    customer_code       VARCHAR(120)  NOT NULL,
    member_code         VARCHAR(120)  NOT NULL,
    -- Liste separee par des virgules : BOOKING, SUBSCRIPTION, PASS, INVOICE_PAYMENT, MARKETPLACE.
    allowed_operations  VARCHAR(200)  NOT NULL,
    max_per_transaction NUMERIC(19, 4),
    max_per_day         NUMERIC(19, 4),
    max_per_month       NUMERIC(19, 4),
    -- Au-dela, la depense attend la validation du gerant · vide, jamais.
    approval_above      NUMERIC(19, 4),
    valid_from          TIMESTAMPTZ,
    valid_until         TIMESTAMPTZ,
    status              VARCHAR(20)   NOT NULL,
    status_reason       VARCHAR(500),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT ck_wallet_spending_authorization_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT ck_wallet_spending_authorization_amounts CHECK (
        (max_per_transaction IS NULL OR max_per_transaction >= 0)
        AND (max_per_day IS NULL OR max_per_day >= 0)
        AND (max_per_month IS NULL OR max_per_month >= 0)
        AND (approval_above IS NULL OR approval_above >= 0))
);
ALTER SEQUENCE wallet_spending_authorization_short_id_seq OWNED BY wallet_spending_authorization.short_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_spending_authorization_short_id ON wallet_spending_authorization (short_id);
-- Une seule autorisation en vigueur par collaborateur et par portefeuille.
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_spending_authorization_open
    ON wallet_spending_authorization (wallet_id, lower(member_code)) WHERE status <> 'REVOKED';
CREATE INDEX IF NOT EXISTS idx_wallet_spending_authorization_member
    ON wallet_spending_authorization (lower(member_code), status);

-- Chaque depense sur le portefeuille de l'organisation · qui l'a faite, pour quoi, et ou elle en est.
CREATE SEQUENCE IF NOT EXISTS wallet_company_spend_short_id_seq START WITH 1;
CREATE TABLE IF NOT EXISTS wallet_company_spend (
    id                 BIGINT PRIMARY KEY,
    short_id           BIGINT        NOT NULL DEFAULT nextval('wallet_company_spend_short_id_seq'),
    wallet_id          BIGINT        NOT NULL REFERENCES wallet_account (id),
    authorization_id   BIGINT        REFERENCES wallet_spending_authorization (id),
    customer_code      VARCHAR(120)  NOT NULL,
    spent_by_member    VARCHAR(120)  NOT NULL,
    operation          VARCHAR(30)   NOT NULL,
    amount             NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3)    NOT NULL,
    document_number    VARCHAR(120)  NOT NULL,
    status             VARCHAR(30)   NOT NULL,
    hold_number        VARCHAR(100),
    intent_number      VARCHAR(100),
    transaction_number VARCHAR(100),
    expires_at         TIMESTAMPTZ,
    decided_by         VARCHAR(150),
    decided_at         TIMESTAMPTZ,
    decision_reason    VARCHAR(500),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ,
    CONSTRAINT ck_wallet_company_spend_status
        CHECK (status IN ('PENDING_APPROVAL', 'EXECUTED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_wallet_company_spend_operation
        CHECK (operation IN ('BOOKING', 'SUBSCRIPTION', 'PASS', 'INVOICE_PAYMENT', 'MARKETPLACE')),
    CONSTRAINT ck_wallet_company_spend_amount CHECK (amount > 0)
);
ALTER SEQUENCE wallet_company_spend_short_id_seq OWNED BY wallet_company_spend.short_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_company_spend_short_id ON wallet_company_spend (short_id);
-- Une seule demande en attente par facture.
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_company_spend_pending_document
    ON wallet_company_spend (lower(document_number)) WHERE status = 'PENDING_APPROVAL';
CREATE INDEX IF NOT EXISTS idx_wallet_company_spend_member
    ON wallet_company_spend (wallet_id, lower(spent_by_member), status, created_at);
CREATE INDEX IF NOT EXISTS idx_wallet_company_spend_pending
    ON wallet_company_spend (status, expires_at) WHERE status = 'PENDING_APPROVAL';
CREATE INDEX IF NOT EXISTS idx_wallet_company_spend_transaction
    ON wallet_company_spend (transaction_number) WHERE transaction_number IS NOT NULL;
