-- Booking, lot B9e : entreprises (budget mensuel de l'entreprise et des collaborateurs, releve mensuel
-- groupe, forfait d'heures partage).

CREATE TABLE IF NOT EXISTS booking_company_terms
(
    id                BIGINT PRIMARY KEY,
    business_code     VARCHAR(300) NOT NULL,
    monthly_statement BOOLEAN      NOT NULL DEFAULT FALSE,
    monthly_budget    NUMERIC(19, 2),
    monthly_hours     INTEGER,
    updated_by        VARCHAR(180),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_booking_company_terms UNIQUE (business_code)
);

CREATE TABLE IF NOT EXISTS booking_member_budget
(
    id             BIGINT PRIMARY KEY,
    business_code  VARCHAR(300)   NOT NULL,
    member_id      VARCHAR(300)   NOT NULL,
    monthly_budget NUMERIC(19, 2) NOT NULL,
    updated_by     VARCHAR(180),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_booking_member_budget UNIQUE (business_code, member_id)
);

CREATE TABLE IF NOT EXISTS booking_company_statement
(
    business_code   VARCHAR(300) NOT NULL,
    statement_month VARCHAR(7)   NOT NULL,
    document_number VARCHAR(120),
    item_count      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_booking_company_statement PRIMARY KEY (business_code, statement_month)
);

ALTER TABLE booking ADD COLUMN IF NOT EXISTS company_covered_minutes INTEGER;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS monthly_statement BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS ix_booking_billing_company_start ON booking (billing_company_code, started_at) WHERE billing_company_code IS NOT NULL;
