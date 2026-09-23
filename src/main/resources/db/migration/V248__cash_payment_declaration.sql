-- Annoncer un paiement en especes, puis le confirmer quand l argent est la.
--
-- Le mobile money et le portefeuille aboutissent seuls. Les especes arrivent avec la personne ·
-- entre l annonce faite depuis l espace client et les billets comptes a la caisse, il n y a rien
-- d encaisse. Cette attente n etait representee nulle part.
--
-- Annoncer n eteint rien : la facture reste due, la reservation reste en attente de paiement.

CREATE TABLE IF NOT EXISTS cash_payment_declaration (
    id                  BIGINT PRIMARY KEY,
    declaration_number  VARCHAR(60)  NOT NULL UNIQUE,
    document_number     VARCHAR(100) NOT NULL,
    booking_number      VARCHAR(100),
    customer_type       VARCHAR(60)  NOT NULL,
    customer_code       VARCHAR(120) NOT NULL,
    customer_name       VARCHAR(250),
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    note                VARCHAR(500),
    declared_at         TIMESTAMPTZ  NOT NULL,
    declared_by         VARCHAR(150),
    expires_at          TIMESTAMPTZ  NOT NULL,
    confirmed_at        TIMESTAMPTZ,
    confirmed_by        VARCHAR(150),
    confirmed_amount    NUMERIC(19,4),
    cash_session_number VARCHAR(100),
    transaction_number  VARCHAR(100),
    closed_at           TIMESTAMPTZ,
    closed_by           VARCHAR(150),
    close_reason        VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

-- La file de la caisse · les annonces en attente, les plus anciennes d abord.
CREATE INDEX IF NOT EXISTS idx_cash_declaration_status_declared
    ON cash_payment_declaration (status, declared_at);

-- Ce que le worker d expiration parcourt.
CREATE INDEX IF NOT EXISTS idx_cash_declaration_expiry
    ON cash_payment_declaration (status, expires_at);

-- Les annonces d un client, et celle en cours sur une facture donnee.
CREATE INDEX IF NOT EXISTS idx_cash_declaration_customer
    ON cash_payment_declaration (customer_type, customer_code, declared_at DESC);
CREATE INDEX IF NOT EXISTS idx_cash_declaration_document
    ON cash_payment_declaration (document_number, status);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2480001, 'cash_payment_declaration', 'Annonce de paiement en especes', 'Sequence des annonces de reglement en especes', 'ESP', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'cash_payment_declaration');
