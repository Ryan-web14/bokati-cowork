-- Lots F10 et C5 du plan finance : banque, cheques, mobile money, rapprochement ; devises de caisse.

CREATE TABLE IF NOT EXISTS bank_import_template
(
    id                BIGINT PRIMARY KEY,
    code              VARCHAR(40)  NOT NULL UNIQUE,
    label             VARCHAR(200) NOT NULL,
    format            VARCHAR(10)  NOT NULL DEFAULT 'CSV',
    delimiter         VARCHAR(3)   NOT NULL DEFAULT ';',
    header_rows       INTEGER      NOT NULL DEFAULT 1,
    date_column       VARCHAR(80)  NOT NULL,
    value_date_column VARCHAR(80),
    label_column      VARCHAR(80)  NOT NULL,
    reference_column  VARCHAR(80),
    amount_column     VARCHAR(80),
    debit_column      VARCHAR(80),
    credit_column     VARCHAR(80),
    date_format       VARCHAR(30)  NOT NULL DEFAULT 'dd/MM/yyyy',
    decimal_separator VARCHAR(1)   NOT NULL DEFAULT ',',
    encoding          VARCHAR(20)  NOT NULL DEFAULT 'UTF-8',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_bank_template_format CHECK (format IN ('CSV')),
    CONSTRAINT ck_bank_template_amount CHECK (amount_column IS NOT NULL OR (debit_column IS NOT NULL AND credit_column IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS bank_account
(
    id                   BIGINT PRIMARY KEY,
    code                 VARCHAR(40)    NOT NULL UNIQUE,
    kind                 VARCHAR(20)    NOT NULL DEFAULT 'BANK',
    bank_name            VARCHAR(120),
    branch               VARCHAR(120),
    label                VARCHAR(200)   NOT NULL,
    rib                  VARCHAR(80),
    currency             VARCHAR(3)     NOT NULL DEFAULT 'XAF',
    account_number       VARCHAR(20)    NOT NULL UNIQUE,
    journal_code         VARCHAR(30)    NOT NULL,
    import_template_code VARCHAR(40),
    opening_balance      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    opening_date         DATE,
    active               BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by           VARCHAR(180)   NOT NULL,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_bank_account_kind CHECK (kind IN ('BANK', 'MOBILE_MONEY', 'CARD'))
);

CREATE TABLE IF NOT EXISTS bank_statement
(
    id                BIGINT PRIMARY KEY,
    number            VARCHAR(40)    NOT NULL UNIQUE,
    bank_account_code VARCHAR(40)    NOT NULL,
    period_from       DATE           NOT NULL,
    period_to         DATE           NOT NULL,
    opening_balance   NUMERIC(19, 4) NOT NULL,
    closing_balance   NUMERIC(19, 4) NOT NULL,
    file_name         VARCHAR(255),
    source            VARCHAR(10)    NOT NULL DEFAULT 'FILE',
    missing_before    BOOLEAN        NOT NULL DEFAULT FALSE,
    duplicates        INTEGER        NOT NULL DEFAULT 0,
    status            VARCHAR(20)    NOT NULL DEFAULT 'IMPORTED',
    replaced_by       VARCHAR(40),
    imported_by       VARCHAR(180)   NOT NULL,
    imported_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_bank_statement_status CHECK (status IN ('IMPORTED', 'REPLACED')),
    CONSTRAINT ck_bank_statement_source CHECK (source IN ('FILE', 'MANUAL'))
);

CREATE TABLE IF NOT EXISTS bank_statement_line
(
    id                BIGINT PRIMARY KEY,
    statement_id      BIGINT         NOT NULL REFERENCES bank_statement (id),
    bank_account_code VARCHAR(40)    NOT NULL,
    line_no           INTEGER        NOT NULL,
    operation_date    DATE           NOT NULL,
    value_date        DATE,
    label             VARCHAR(500)   NOT NULL,
    reference         VARCHAR(120),
    amount            NUMERIC(19, 4) NOT NULL,
    fingerprint       VARCHAR(64)    NOT NULL,
    matching_code     VARCHAR(20),
    matched_at        TIMESTAMPTZ,
    matched_by        VARCHAR(180),
    awaiting_payment  VARCHAR(60),
    posted_entry      VARCHAR(40),
    CONSTRAINT ck_bank_statement_line_amount CHECK (amount <> 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bank_statement_line_fp ON bank_statement_line (bank_account_code, fingerprint);
CREATE INDEX IF NOT EXISTS idx_bank_statement_line_open ON bank_statement_line (bank_account_code, matching_code);

CREATE TABLE IF NOT EXISTS bank_matching
(
    id                 BIGINT PRIMARY KEY,
    code               VARCHAR(20)    NOT NULL,
    bank_account_code  VARCHAR(40)    NOT NULL,
    kind               VARCHAR(20)    NOT NULL,
    statement_line_ids TEXT           NOT NULL,
    entry_line_ids     TEXT           NOT NULL,
    amount             NUMERIC(19, 4) NOT NULL,
    locked             BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by         VARCHAR(180)   NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    unmatched_by       VARCHAR(180),
    unmatched_at       TIMESTAMPTZ,
    unmatched_reason   VARCHAR(500),
    CONSTRAINT uq_bank_matching UNIQUE (bank_account_code, code),
    CONSTRAINT ck_bank_matching_kind CHECK (kind IN ('AUTO_REFERENCE', 'AUTO_AMOUNT', 'AUTO_COMBINATION', 'MANUAL', 'POSTED'))
);

CREATE TABLE IF NOT EXISTS bank_assignment_rule
(
    id             BIGINT PRIMARY KEY,
    label          VARCHAR(200)   NOT NULL,
    label_pattern  VARCHAR(200)   NOT NULL,
    direction      VARCHAR(5),
    min_amount     NUMERIC(19, 4),
    max_amount     NUMERIC(19, 4),
    account_number VARCHAR(20)    NOT NULL,
    third_party    VARCHAR(120),
    priority       INTEGER        NOT NULL DEFAULT 100,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by     VARCHAR(180)   NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_bank_rule_direction CHECK (direction IS NULL OR direction IN ('IN', 'OUT'))
);

CREATE TABLE IF NOT EXISTS bank_reconciliation
(
    id                BIGINT PRIMARY KEY,
    number            VARCHAR(40),
    bank_account_code VARCHAR(40)    NOT NULL,
    reconciliation_date DATE         NOT NULL,
    statement_balance NUMERIC(19, 4) NOT NULL,
    book_balance      NUMERIC(19, 4) NOT NULL,
    outstanding       TEXT           NOT NULL,
    difference        NUMERIC(19, 4) NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    prepared_by       VARCHAR(180)   NOT NULL,
    prepared_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    validated_by      VARCHAR(180),
    validated_at      TIMESTAMPTZ,
    CONSTRAINT ck_bank_reconciliation_status CHECK (status IN ('DRAFT', 'VALIDATED'))
);

CREATE TABLE IF NOT EXISTS bank_cheque
(
    id                 BIGINT PRIMARY KEY,
    transaction_number VARCHAR(60)    NOT NULL UNIQUE,
    cheque_number      VARCHAR(80),
    bank_name          VARCHAR(120),
    drawer             VARCHAR(255),
    amount             NUMERIC(19, 4) NOT NULL,
    register_code      VARCHAR(100),
    session_number     VARCHAR(60),
    status             VARCHAR(20)    NOT NULL DEFAULT 'RECEIVED',
    slip_number        VARCHAR(40),
    deposited_at       TIMESTAMPTZ,
    cleared_at         TIMESTAMPTZ,
    rejected_at        TIMESTAMPTZ,
    rejected_reason    VARCHAR(500),
    received_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_bank_cheque_status CHECK (status IN ('RECEIVED', 'DEPOSITED', 'CLEARED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS bank_deposit_slip
(
    id                BIGINT PRIMARY KEY,
    number            VARCHAR(40)    NOT NULL UNIQUE,
    bank_account_code VARCHAR(40)    NOT NULL,
    register_code     VARCHAR(100),
    cash_amount       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    cheque_total      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PREPARED',
    created_by        VARCHAR(180)   NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deposited_by      VARCHAR(180),
    deposited_at      TIMESTAMPTZ,
    credited_at       TIMESTAMPTZ,
    credit_entry      VARCHAR(40),
    CONSTRAINT ck_bank_slip_status CHECK (status IN ('PREPARED', 'DEPOSITED', 'CREDITED'))
);

ALTER TABLE acc_entry_line ADD COLUMN IF NOT EXISTS matching_code VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_acc_entry_line_matching ON acc_entry_line (account_number, matching_code);

-- Le pointage bancaire d'une ligne validee est permis (seule colonne modifiable apres validation).
CREATE OR REPLACE FUNCTION acc_entry_line_protect() RETURNS TRIGGER AS $$
DECLARE
    entry_status VARCHAR;
    entry_ref    BIGINT;
BEGIN
    entry_ref := CASE WHEN TG_OP = 'DELETE' THEN OLD.entry_id ELSE NEW.entry_id END;
    SELECT status INTO entry_status FROM acc_entry WHERE id = entry_ref;
    IF entry_status IN ('VALIDATED', 'REVERSED') THEN
        IF TG_OP = 'UPDATE'
            AND NEW.entry_id = OLD.entry_id AND NEW.line_no = OLD.line_no AND NEW.account_number = OLD.account_number
            AND NEW.debit = OLD.debit AND NEW.credit = OLD.credit
            AND NEW.third_party_code IS NOT DISTINCT FROM OLD.third_party_code
            AND NEW.label IS NOT DISTINCT FROM OLD.label
            AND NEW.reference IS NOT DISTINCT FROM OLD.reference THEN
            RETURN NEW;
        END IF;
        RAISE EXCEPTION 'Les lignes d''une piece validee ne se modifient pas';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Comptes de tresorerie bancaires et mobile money (lot F4).
INSERT INTO bank_account (id, code, kind, label, account_number, journal_code, created_by)
VALUES (2969001, 'BANQUE-PRINCIPALE', 'BANK', 'Banque principale', '5211', 'BQ-5211', 'SYSTEM'),
       (2969002, 'PAWAPAY', 'MOBILE_MONEY', 'Mobile money PawaPay', '5212', 'MM-5212', 'SYSTEM'),
       (2969003, 'MM-MARCHANDS', 'MOBILE_MONEY', 'Mobile money numeros marchands', '5213', 'MM-5213', 'SYSTEM')
ON CONFLICT (code) DO NOTHING;

-- Regles d'affectation de depart.
INSERT INTO bank_assignment_rule (id, label, label_pattern, direction, account_number, priority, created_by)
VALUES (2969101, 'Frais et commissions bancaires', 'FRAIS|COMMISSION|COTISATION|TENUE DE COMPTE|AGIOS', 'OUT', '631', 10, 'SYSTEM'),
       (2969102, 'Interets crediteurs', 'INTERET', 'IN', '77', 20, 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- C5 : taux de change du jour des caisses.
CREATE TABLE IF NOT EXISTS cash_exchange_rate
(
    id         BIGINT PRIMARY KEY,
    currency   VARCHAR(3)     NOT NULL,
    rate       NUMERIC(19, 6) NOT NULL,
    valid_on   DATE           NOT NULL,
    set_by     VARCHAR(180)   NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_cash_exchange_rate UNIQUE (currency, valid_on),
    CONSTRAINT ck_cash_exchange_rate CHECK (rate > 0 AND currency <> 'XAF')
);

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('BANK_ACCOUNT_MANAGE', 'Gerer les comptes bancaires et les modeles d''import', 'BANK_ACCOUNT', 'MANAGE'),
           ('BANK_STATEMENT_IMPORT', 'Importer un releve', 'BANK_STATEMENT', 'IMPORT'),
           ('BANK_RECONCILIATION_MANAGE', 'Pointer, comptabiliser une ligne de releve, preparer le rapprochement', 'BANK_RECONCILIATION', 'MANAGE'),
           ('BANK_RECONCILIATION_VALIDATE', 'Valider un etat de rapprochement', 'BANK_RECONCILIATION', 'VALIDATE'),
           ('BANK_RECONCILIATION_UNLOCK', 'Depointer dans un rapprochement valide', 'BANK_RECONCILIATION', 'UNLOCK'),
           ('CASH_EXCHANGE_RATE_MANAGE', 'Saisir le taux de change du jour', 'CASH_EXCHANGE_RATE', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('BANK_ACCOUNT_MANAGE'), ('BANK_STATEMENT_IMPORT'), ('BANK_RECONCILIATION_MANAGE'), ('BANK_RECONCILIATION_VALIDATE'),
                       ('BANK_RECONCILIATION_UNLOCK'), ('CASH_EXCHANGE_RATE_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('BANK_ACCOUNT_MANAGE'), ('BANK_STATEMENT_IMPORT'), ('BANK_RECONCILIATION_MANAGE'),
                                     ('BANK_RECONCILIATION_VALIDATE'), ('CASH_EXCHANGE_RATE_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'CASH_EXCHANGE_RATE_MANAGE'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
