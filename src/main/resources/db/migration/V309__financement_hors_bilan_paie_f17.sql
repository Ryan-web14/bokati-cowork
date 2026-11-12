-- Lot F17 : emprunts et tableaux d'amortissement, comptes courants d'associes, registre des
-- engagements hors bilan tenu en classe 90 (journal HB), import du journal de paie (journal PA).

INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT 2970002, '465', 'Associes, dividendes a payer', '46', '4', 'BALANCE', 'CREDIT', TRUE, 'PARTNER', NULL, FALSE, 'CLASSE_4', 'ACTIVE', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_account WHERE number = '465');

-- ------------------------------------------------------------------ emprunts

CREATE TABLE IF NOT EXISTS acc_loan
(
    id                 BIGINT PRIMARY KEY,
    code               VARCHAR(40)    NOT NULL UNIQUE,
    lender_type        VARCHAR(20)    NOT NULL,
    lender_code        VARCHAR(120),
    lender_name        VARCHAR(200)   NOT NULL,
    purpose            VARCHAR(500),
    principal          NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3)     NOT NULL DEFAULT 'XAF',
    disbursed_on       DATE           NOT NULL,
    first_due_date     DATE           NOT NULL,
    rate               NUMERIC(9, 6)  NOT NULL DEFAULT 0,
    rate_type          VARCHAR(10)    NOT NULL DEFAULT 'FIXED',
    day_count          VARCHAR(10)    NOT NULL DEFAULT '30_360',
    frequency_months   INTEGER        NOT NULL DEFAULT 1,
    term_count         INTEGER        NOT NULL,
    deferral_type      VARCHAR(10)    NOT NULL DEFAULT 'NONE',
    deferral_count     INTEGER        NOT NULL DEFAULT 0,
    method             VARCHAR(20)    NOT NULL DEFAULT 'ANNUITY',
    fees               NUMERIC(19, 4) NOT NULL DEFAULT 0,
    insurance          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    loan_account       VARCHAR(20)    NOT NULL DEFAULT '162',
    interest_account   VARCHAR(20)    NOT NULL DEFAULT '671',
    insurance_account  VARCHAR(20)    NOT NULL DEFAULT '625',
    fees_account       VARCHAR(20)    NOT NULL DEFAULT '631',
    accrued_account    VARCHAR(20)    NOT NULL DEFAULT '166',
    bank_account_code  VARCHAR(40)    NOT NULL,
    section_code       VARCHAR(60),
    budget_item_code   VARCHAR(60),
    posting_mode       VARCHAR(20)    NOT NULL DEFAULT 'AT_DATE',
    schedule_version   INTEGER        NOT NULL DEFAULT 1,
    status             VARCHAR(10)    NOT NULL DEFAULT 'DRAFT',
    disbursement_entry VARCHAR(60),
    created_by         VARCHAR(180)   NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    closed_at          TIMESTAMPTZ,
    CONSTRAINT ck_acc_loan_lender CHECK (lender_type IN ('BANK', 'PARTNER', 'OTHER')),
    CONSTRAINT ck_acc_loan_method CHECK (method IN ('ANNUITY', 'CONSTANT_CAPITAL', 'BULLET')),
    CONSTRAINT ck_acc_loan_deferral CHECK (deferral_type IN ('NONE', 'CAPITAL', 'TOTAL')),
    CONSTRAINT ck_acc_loan_day_count CHECK (day_count IN ('30_360', 'ACT_365')),
    CONSTRAINT ck_acc_loan_mode CHECK (posting_mode IN ('AT_DATE', 'AT_STATEMENT')),
    CONSTRAINT ck_acc_loan_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED')),
    CONSTRAINT ck_acc_loan_terms CHECK (term_count > 0 AND frequency_months > 0 AND deferral_count >= 0 AND deferral_count < term_count)
);

CREATE TABLE IF NOT EXISTS acc_loan_installment
(
    id              BIGINT PRIMARY KEY,
    loan_id         BIGINT         NOT NULL REFERENCES acc_loan (id),
    version         INTEGER        NOT NULL,
    number          INTEGER        NOT NULL,
    due_date        DATE           NOT NULL,
    opening_balance NUMERIC(19, 4) NOT NULL,
    interest        NUMERIC(19, 4) NOT NULL,
    principal       NUMERIC(19, 4) NOT NULL,
    insurance       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    payment         NUMERIC(19, 4) NOT NULL,
    closing_balance NUMERIC(19, 4) NOT NULL,
    status          VARCHAR(12)    NOT NULL DEFAULT 'PLANNED',
    entry_number    VARCHAR(60),
    posted_at       TIMESTAMPTZ,
    CONSTRAINT uq_acc_loan_installment UNIQUE (loan_id, version, number),
    CONSTRAINT ck_acc_loan_installment_status CHECK (status IN ('PLANNED', 'POSTED'))
);
CREATE INDEX IF NOT EXISTS ix_acc_loan_installment_due ON acc_loan_installment (status, due_date);

CREATE TABLE IF NOT EXISTS acc_loan_event
(
    id           BIGINT PRIMARY KEY,
    loan_id      BIGINT         NOT NULL REFERENCES acc_loan (id),
    kind         VARCHAR(20)    NOT NULL,
    event_date   DATE           NOT NULL,
    amount       NUMERIC(19, 4),
    rate         NUMERIC(9, 6),
    detail       VARCHAR(500),
    from_version INTEGER,
    to_version   INTEGER,
    entry_number VARCHAR(60),
    created_by   VARCHAR(180)   NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------------ comptes courants d'associes

CREATE TABLE IF NOT EXISTS acc_partner_account
(
    id               BIGINT PRIMARY KEY,
    code             VARCHAR(40)    NOT NULL UNIQUE,
    partner_name     VARCHAR(200)   NOT NULL,
    third_party_code VARCHAR(120)   NOT NULL UNIQUE,
    account_number   VARCHAR(20)    NOT NULL DEFAULT '462',
    blocked          BOOLEAN        NOT NULL DEFAULT FALSE,
    blocked_until    DATE,
    rate             NUMERIC(9, 6)  NOT NULL DEFAULT 0,
    admitted_rate    NUMERIC(9, 6),
    cap              NUMERIC(19, 4),
    opened_on        DATE           NOT NULL,
    status           VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    created_by       VARCHAR(180)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_partner_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS acc_partner_interest
(
    id             BIGINT PRIMARY KEY,
    partner_code   VARCHAR(40)    NOT NULL,
    from_date      DATE           NOT NULL,
    to_date        DATE           NOT NULL,
    average_balance NUMERIC(19, 4) NOT NULL,
    rate           NUMERIC(9, 6)  NOT NULL,
    interest       NUMERIC(19, 4) NOT NULL,
    non_deductible NUMERIC(19, 4) NOT NULL DEFAULT 0,
    entry_number   VARCHAR(60),
    created_by     VARCHAR(180)   NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_partner_interest UNIQUE (partner_code, to_date)
);

-- ------------------------------------------------------------------ hors bilan

CREATE TABLE IF NOT EXISTS acc_commitment_type
(
    code              VARCHAR(40) PRIMARY KEY,
    label             VARCHAR(200) NOT NULL,
    default_direction VARCHAR(10)  NOT NULL,
    lease             BOOLEAN      NOT NULL DEFAULT FALSE,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_acc_commitment_type_direction CHECK (default_direction IN ('GIVEN', 'RECEIVED'))
);

INSERT INTO acc_commitment_type (code, label, default_direction, lease)
VALUES ('CAUTION_DONNEE', 'Caution donnee', 'GIVEN', FALSE),
       ('CAUTION_RECUE', 'Caution recue', 'RECEIVED', FALSE),
       ('GARANTIE_BANCAIRE', 'Garantie bancaire', 'RECEIVED', FALSE),
       ('NANTISSEMENT', 'Nantissement', 'GIVEN', FALSE),
       ('HYPOTHEQUE', 'Hypotheque', 'GIVEN', FALSE),
       ('CREDIT_BAIL', 'Credit-bail ou location longue duree', 'GIVEN', TRUE),
       ('BAIL', 'Engagement de location (bail des locaux)', 'GIVEN', TRUE),
       ('AVAL', 'Aval', 'GIVEN', FALSE),
       ('ENGAGEMENT_ACHAT', 'Engagement d''achat', 'GIVEN', FALSE)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS acc_commitment
(
    id                BIGINT PRIMARY KEY,
    code              VARCHAR(40)    NOT NULL UNIQUE,
    type_code         VARCHAR(40)    NOT NULL REFERENCES acc_commitment_type (code),
    direction         VARCHAR(10)    NOT NULL,
    third_party_code  VARCHAR(120),
    third_party_name  VARCHAR(200)   NOT NULL,
    description       VARCHAR(500),
    amount            NUMERIC(19, 4) NOT NULL,
    monthly_amount    NUMERIC(19, 4),
    currency          VARCHAR(3)     NOT NULL DEFAULT 'XAF',
    start_date        DATE           NOT NULL,
    end_date          DATE,
    renewal_date      DATE,
    notice_date       DATE,
    linked_type       VARCHAR(40),
    linked_code       VARCHAR(120),
    document_refs     VARCHAR(1000),
    status            VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    booked_amount     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    last_entry_number VARCHAR(60),
    released_on       DATE,
    release_reference VARCHAR(200),
    created_by        VARCHAR(180)   NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_commitment_direction CHECK (direction IN ('GIVEN', 'RECEIVED')),
    CONSTRAINT ck_acc_commitment_status CHECK (status IN ('ACTIVE', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_acc_commitment_amount CHECK (amount >= 0)
);

-- ------------------------------------------------------------------ paie

CREATE TABLE IF NOT EXISTS acc_payroll_template
(
    code              VARCHAR(40) PRIMARY KEY,
    label             VARCHAR(200) NOT NULL,
    separator         VARCHAR(4)   NOT NULL DEFAULT ';',
    header_rows       INTEGER      NOT NULL DEFAULT 1,
    employee_column   VARCHAR(60)  NOT NULL,
    name_column       VARCHAR(60),
    rubric_column     VARCHAR(60)  NOT NULL,
    amount_column     VARCHAR(60)  NOT NULL,
    section_column    VARCHAR(60),
    decimal_separator VARCHAR(1)   NOT NULL DEFAULT ',',
    encoding          VARCHAR(20)  NOT NULL DEFAULT 'UTF-8',
    created_by        VARCHAR(180) NOT NULL DEFAULT 'SYSTEM'
);

INSERT INTO acc_payroll_template (code, label, employee_column, name_column, rubric_column, amount_column, section_column)
VALUES ('STANDARD', 'Journal de paie standard (Matricule;Nom;Rubrique;Montant;Section)', 'Matricule', 'Nom', 'Rubrique', 'Montant', 'Section')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS acc_payroll_rubric
(
    code            VARCHAR(60) PRIMARY KEY,
    label           VARCHAR(200) NOT NULL,
    kind            VARCHAR(20)  NOT NULL,
    account         VARCHAR(20),
    counter_account VARCHAR(20),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_acc_payroll_rubric_kind CHECK (kind IN ('GROSS', 'EMPLOYER_CHARGE', 'DEDUCTION', 'ADVANCE', 'NET', 'INFO'))
);

INSERT INTO acc_payroll_rubric (code, label, kind, account, counter_account)
VALUES ('SALAIRE_BASE', 'Salaire de base', 'GROSS', '661', NULL),
       ('PRIMES', 'Primes et gratifications', 'GROSS', '661', NULL),
       ('HEURES_SUP', 'Heures supplementaires', 'GROSS', '661', NULL),
       ('AVANTAGES', 'Avantages en nature et indemnites', 'GROSS', '661', NULL),
       ('CNSS_SALARIALE', 'CNSS, part salariale', 'DEDUCTION', '431', NULL),
       ('IRPP', 'Impot sur les traitements et salaires', 'DEDUCTION', '447', NULL),
       ('CNSS_PATRONALE', 'CNSS, part patronale', 'EMPLOYER_CHARGE', '664', '431'),
       ('AVANCES', 'Avances et acomptes retenus', 'ADVANCE', '421', NULL),
       ('NET_A_PAYER', 'Net a payer', 'NET', '422', NULL),
       ('BRUT', 'Salaire brut (total, ignore)', 'INFO', NULL, NULL)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS acc_payroll_employee
(
    matricule        VARCHAR(60) PRIMARY KEY,
    full_name        VARCHAR(200) NOT NULL,
    third_party_code VARCHAR(120) NOT NULL,
    section_code     VARCHAR(60),
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS acc_payroll_import
(
    id            BIGINT PRIMARY KEY,
    period_code   VARCHAR(7)     NOT NULL,
    template_code VARCHAR(40)    NOT NULL,
    file_name     VARCHAR(255),
    status        VARCHAR(10)    NOT NULL DEFAULT 'DRAFT',
    gross_total   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    employer_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    deduction_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    advance_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    net_total     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    paid_total    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    employee_count INTEGER       NOT NULL DEFAULT 0,
    errors_json   TEXT,
    warnings_json TEXT,
    entry_number  VARCHAR(60),
    imported_by   VARCHAR(180)   NOT NULL,
    imported_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    posted_by     VARCHAR(180),
    posted_at     TIMESTAMPTZ,
    CONSTRAINT ck_acc_payroll_import_status CHECK (status IN ('DRAFT', 'REPLACED', 'POSTED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_acc_payroll_import_live ON acc_payroll_import (period_code) WHERE status <> 'REPLACED';

CREATE TABLE IF NOT EXISTS acc_payroll_line
(
    id            BIGINT PRIMARY KEY,
    import_id     BIGINT         NOT NULL REFERENCES acc_payroll_import (id),
    line_no       INTEGER        NOT NULL,
    matricule     VARCHAR(60)    NOT NULL,
    employee_name VARCHAR(200),
    rubric_code   VARCHAR(60)    NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    section_code  VARCHAR(60)
);
CREATE INDEX IF NOT EXISTS ix_acc_payroll_line_import ON acc_payroll_line (import_id);

-- ------------------------------------------------------------------ droits

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('FINANCING_READ', 'Consulter emprunts, comptes courants et engagements hors bilan', 'FINANCING', 'READ'),
           ('FINANCING_MANAGE', 'Gerer emprunts, comptes courants et engagements hors bilan', 'FINANCING', 'MANAGE'),
           ('PAYROLL_IMPORT', 'Importer un journal de paie', 'PAYROLL', 'IMPORT'),
           ('PAYROLL_POST', 'Comptabiliser et payer un journal de paie', 'PAYROLL', 'POST'),
           ('PAYROLL_SETTINGS_MANAGE', 'Gerer les modeles, rubriques et salaries de la paie', 'PAYROLL_SETTINGS', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('FINANCING_READ'), ('FINANCING_MANAGE'), ('PAYROLL_IMPORT'), ('PAYROLL_POST'), ('PAYROLL_SETTINGS_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'FINANCING_READ'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
