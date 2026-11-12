-- Lots F5 et F6 du plan finance : etat de besoin, demande de fonds multi-lignes, validation a
-- plusieurs niveaux, decaissement distinct, avances a justifier et justification ligne par ligne.

CREATE TABLE IF NOT EXISTS fund_need_statement
(
    id             BIGINT PRIMARY KEY,
    number         VARCHAR(40)    NOT NULL UNIQUE,
    requester      VARCHAR(180)   NOT NULL,
    department     VARCHAR(120),
    location_code  VARCHAR(100),
    cost_center    VARCHAR(60),
    subject        VARCHAR(500)   NOT NULL,
    urgency        VARCHAR(20)    NOT NULL DEFAULT 'NORMAL',
    wanted_date    DATE,
    status         VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    total_amount   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    submitted_at   TIMESTAMPTZ,
    decided_by     VARCHAR(180),
    decided_at     TIMESTAMPTZ,
    decision_note  VARCHAR(1000),
    attachments    TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fund_need_urgency CHECK (urgency IN ('NORMAL', 'URGENT', 'CRITICAL')),
    CONSTRAINT ck_fund_need_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'VALIDATED', 'REJECTED', 'PARTIALLY_TRANSFORMED', 'TRANSFORMED'))
);

CREATE TABLE IF NOT EXISTS fund_need_line
(
    id                         BIGINT PRIMARY KEY,
    statement_id               BIGINT         NOT NULL REFERENCES fund_need_statement (id),
    line_no                    INTEGER        NOT NULL,
    designation                VARCHAR(500)   NOT NULL,
    quantity                   NUMERIC(19, 4) NOT NULL DEFAULT 1,
    unit                       VARCHAR(30),
    unit_price                 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_amount               NUMERIC(19, 4) NOT NULL DEFAULT 0,
    nature_code                VARCHAR(80),
    supplier                   VARCHAR(255),
    transformed_request_number VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS fund_request
(
    id                       BIGINT PRIMARY KEY,
    number                   VARCHAR(40)    NOT NULL UNIQUE,
    request_type             VARCHAR(30)    NOT NULL,
    status                   VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    requester                VARCHAR(180)   NOT NULL,
    requester_department     VARCHAR(120),
    requester_phone          VARCHAR(40),
    beneficiary_type         VARCHAR(20)    NOT NULL DEFAULT 'SELF',
    beneficiary_code         VARCHAR(180),
    beneficiary_name         VARCHAR(255),
    beneficiary_id_number    VARCHAR(80),
    beneficiary_phone        VARCHAR(40),
    register_code            VARCHAR(100),
    subject                  VARCHAR(500)   NOT NULL,
    total_amount             NUMERIC(19, 4) NOT NULL DEFAULT 0,
    approved_amount          NUMERIC(19, 4),
    disbursed_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    justified_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    returned_amount          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    complement_amount        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    delivery_mode            VARCHAR(20)    NOT NULL DEFAULT 'CASH',
    wanted_date              DATE,
    mission_destination      VARCHAR(255),
    mission_start            DATE,
    mission_end              DATE,
    unplanned                BOOLEAN        NOT NULL DEFAULT FALSE,
    unplanned_reason         VARCHAR(1000),
    need_statement_number    VARCHAR(40),
    transfer_order_number    VARCHAR(60),
    required_levels          INTEGER        NOT NULL DEFAULT 1,
    current_level            INTEGER        NOT NULL DEFAULT 0,
    last_comment             VARCHAR(1000),
    submitted_at             TIMESTAMPTZ,
    level_since              TIMESTAMPTZ,
    approver_reminded_at     TIMESTAMPTZ,
    approved_at              TIMESTAMPTZ,
    expires_at               TIMESTAMPTZ,
    disbursed_at             TIMESTAMPTZ,
    justification_due_date   DATE,
    justification_reminders  INTEGER        NOT NULL DEFAULT 0,
    justification_reminded_at TIMESTAMPTZ,
    settled_at               TIMESTAMPTZ,
    cancelled_at             TIMESTAMPTZ,
    legacy_request_number    VARCHAR(60),
    attachments              TEXT,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fund_request_type CHECK (request_type IN ('DIRECT_EXPENSE', 'ADVANCE', 'EXPENSE_REFUND', 'SUPPLIER_PAYMENT', 'STAFF_ADVANCE',
                                                           'CASH_SUPPLY', 'FUND_REMITTANCE')),
    CONSTRAINT ck_fund_request_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'IN_APPROVAL', 'APPROVED', 'PARTIALLY_DISBURSED', 'DISBURSED',
                                                       'TO_JUSTIFY', 'PARTIALLY_JUSTIFIED', 'JUSTIFIED', 'SETTLED', 'REJECTED',
                                                       'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_fund_request_beneficiary CHECK (beneficiary_type IN ('SELF', 'EMPLOYEE', 'THIRD_PARTY')),
    CONSTRAINT ck_fund_request_delivery CHECK (delivery_mode IN ('CASH', 'MOBILE_MONEY', 'TRANSFER'))
);

CREATE INDEX IF NOT EXISTS idx_fund_request_status ON fund_request (status);
CREATE INDEX IF NOT EXISTS idx_fund_request_requester ON fund_request (requester);
CREATE INDEX IF NOT EXISTS idx_fund_request_beneficiary ON fund_request (beneficiary_code);

CREATE TABLE IF NOT EXISTS fund_request_line
(
    id          BIGINT PRIMARY KEY,
    request_id  BIGINT         NOT NULL REFERENCES fund_request (id),
    line_no     INTEGER        NOT NULL,
    nature_code VARCHAR(80),
    designation VARCHAR(500)   NOT NULL,
    amount      NUMERIC(19, 4) NOT NULL,
    supplier    VARCHAR(255),
    section     VARCHAR(60),
    CONSTRAINT ck_fund_request_line_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS fund_request_approval
(
    id             BIGINT PRIMARY KEY,
    request_id     BIGINT         NOT NULL REFERENCES fund_request (id),
    level          INTEGER        NOT NULL,
    approver       VARCHAR(180)   NOT NULL,
    decision       VARCHAR(20)    NOT NULL,
    amount_before  NUMERIC(19, 4),
    amount_after   NUMERIC(19, 4),
    comment        VARCHAR(1000),
    delegated_from VARCHAR(180),
    decided_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fund_approval_decision CHECK (decision IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'RETURNED', 'REDUCED', 'CANCELLED', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS fund_approval_delegation
(
    id           BIGINT PRIMARY KEY,
    delegator    VARCHAR(180) NOT NULL,
    delegate     VARCHAR(180) NOT NULL,
    level        INTEGER      NOT NULL,
    starts_on    DATE         NOT NULL,
    ends_on      DATE         NOT NULL,
    created_by   VARCHAR(180) NOT NULL,
    cancelled_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_fund_delegation_dates CHECK (ends_on >= starts_on)
);

CREATE TABLE IF NOT EXISTS fund_disbursement
(
    id              BIGINT PRIMARY KEY,
    request_id      BIGINT         NOT NULL REFERENCES fund_request (id),
    kind            VARCHAR(20)    NOT NULL,
    movement_number VARCHAR(60)    NOT NULL,
    session_number  VARCHAR(60),
    amount          NUMERIC(19, 4) NOT NULL,
    signed_by_name  VARCHAR(255),
    signature       TEXT,
    created_by      VARCHAR(180)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fund_disbursement_kind CHECK (kind IN ('DISBURSEMENT', 'COMPLEMENT', 'RETURN'))
);

CREATE TABLE IF NOT EXISTS expense_justification_line
(
    id                 BIGINT PRIMARY KEY,
    number             VARCHAR(40)    NOT NULL UNIQUE,
    request_id         BIGINT         NOT NULL REFERENCES fund_request (id),
    line_no            INTEGER        NOT NULL,
    expense_date       DATE           NOT NULL,
    nature_code        VARCHAR(80)    NOT NULL,
    supplier           VARCHAR(255),
    designation        VARCHAR(500)   NOT NULL,
    amount             NUMERIC(19, 4) NOT NULL,
    vat_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    section            VARCHAR(60),
    document_reference VARCHAR(500),
    honour_statement   BOOLEAN        NOT NULL DEFAULT FALSE,
    request_line_no    INTEGER,
    status             VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    reject_reason      VARCHAR(1000),
    entered_by         VARCHAR(180)   NOT NULL,
    validated_by       VARCHAR(180),
    validated_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_expense_line_status CHECK (status IN ('PENDING', 'VALIDATED', 'REJECTED')),
    CONSTRAINT ck_expense_line_amount CHECK (amount > 0 AND vat_amount >= 0 AND vat_amount < amount)
);

CREATE INDEX IF NOT EXISTS idx_expense_line_request ON expense_justification_line (request_id);

-- Retour du reliquat d'une avance au guichet.
INSERT INTO cash_operation_nature (id, code, label, family, direction, movement_type, account_number, document_required,
                                   justification_days, max_per_operation, prior_request_required, double_visa_threshold,
                                   keywords, sort_order, system_value)
SELECT COALESCE((SELECT MAX(id) FROM cash_operation_nature), 2940000) + 1, 'RETOUR_AVANCE', 'Retour du reliquat d''une avance', 'AVANCES',
       'IN', 'CASH_IN', '421', FALSE, NULL, NULL, TRUE, NULL, 'reliquat avance retour', 90, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cash_operation_nature WHERE code = 'RETOUR_AVANCE');

-- Numerotation.
INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT v.id, v.code, v.name, v.description, v.prefix, NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', TRUE, FALSE, now(), now()
FROM (VALUES (2965001, 'fund_need_statement', 'Etat de besoin', 'Numero des etats de besoin', 'EDB'),
             (2965002, 'fund_request', 'Demande de fonds', 'Numero des demandes de fonds', 'DDF'),
             (2965003, 'expense_justification', 'Justification de depense', 'Numero des lignes de justification', 'JUS'))
     AS v(id, code, name, description, prefix)
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = v.code);

-- Journal des avances et de leurs justifications.
INSERT INTO acc_journal (id, code, label, type, auto_validate, created_by)
SELECT 2965101, 'AV', 'Avances et justifications', 'MISC', TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_journal WHERE code = 'AV');

-- Schema : ligne de justification validee (catalogue, section 9).
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
VALUES (2965201, 'EXPENSE_JUSTIFIED', 1, 'EXPENSE_JUSTIFIED', 'Ligne de justification validee',
        'D charge de la nature (HT) ; D TVA recuperable ; C 421 du beneficiaire (lettre par numero de demande).', 'FUNDING', 10, NULL,
        '{journalCode}', 'Justification {lineNumber} de {requestNumber}',
        '[{"side":"DEBIT","account":"{natureAccount}","amount":"netAmount","label":"{designation}","thirdParty":"{?natureThirdParty}"},{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA"},{"side":"CREDIT","account":"421","amount":"amount","thirdParty":"{beneficiaryCode}","reference":"{requestNumber}"}]',
        FALSE, '{"lineNumber":"JUS-1","requestNumber":"DDF-1","journalCode":"AV","natureAccount":"6181","vatAccount":"4452","amount":11800,"netAmount":10000,"vatAmount":1800,"designation":"Hotel","beneficiaryCode":"agent@elleaose.com"}',
        DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM')
ON CONFLICT (code, version) DO NOTHING;

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('FUND_REQUEST_CREATE', 'Creer des etats de besoin et des demandes de fonds', 'FUND_REQUEST', 'CREATE'),
           ('FUND_REQUEST_READ', 'Consulter toutes les demandes de fonds', 'FUND_REQUEST', 'READ'),
           ('FUND_REQUEST_APPROVE_L1', 'Valider les demandes de fonds (chef de service)', 'FUND_REQUEST', 'APPROVE_L1'),
           ('FUND_REQUEST_APPROVE_L2', 'Valider les demandes de fonds (responsable financier)', 'FUND_REQUEST', 'APPROVE_L2'),
           ('FUND_REQUEST_APPROVE_L3', 'Valider les demandes de fonds (direction)', 'FUND_REQUEST', 'APPROVE_L3'),
           ('FUND_REQUEST_DISBURSE', 'Decaisser les demandes de fonds approuvees', 'FUND_REQUEST', 'DISBURSE'),
           ('FUND_JUSTIFICATION_VALIDATE', 'Valider les justifications d''avances', 'FUND_JUSTIFICATION', 'VALIDATE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('FUND_REQUEST_CREATE'), ('FUND_REQUEST_READ'), ('FUND_REQUEST_APPROVE_L1'), ('FUND_REQUEST_APPROVE_L2'),
                       ('FUND_REQUEST_APPROVE_L3'), ('FUND_REQUEST_DISBURSE'), ('FUND_JUSTIFICATION_VALIDATE')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('FUND_REQUEST_CREATE'), ('FUND_REQUEST_READ'), ('FUND_REQUEST_APPROVE_L2'),
                                     ('FUND_REQUEST_DISBURSE'), ('FUND_JUSTIFICATION_VALIDATE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', p FROM (VALUES ('FUND_REQUEST_CREATE'), ('FUND_REQUEST_READ'), ('FUND_REQUEST_APPROVE_L1')) AS perms(p)
    UNION ALL
    SELECT 'CASHIER', p FROM (VALUES ('FUND_REQUEST_CREATE'), ('FUND_REQUEST_DISBURSE')) AS perms(p)
    UNION ALL
    SELECT 'STAFF', 'FUND_REQUEST_CREATE'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
