-- Lot F19 : provisions et creances douteuses. Etat comptable de chaque facture (normal, litigieuse,
-- douteuse, perdue, recuperee), file de revue des classements, depreciations par facture,
-- decisions de passage en perte, registre des risques et provisions pour risques et charges,
-- revue de cloture, indemnites de fin de carriere ; comptes et droits.

-- ------------------------------------------------------------------ comptes

WITH seed (id, number, label, parent_number, nature, normal_side, reconcilable, analytic_required) AS (
    VALUES (3150001, '6515', 'Pertes sur autres debiteurs', '651', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3150002, '6591', 'Dotations aux provisions pour risques a court terme', '659', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3150003, '6593', 'Dotations aux depreciations des stocks', '659', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3150004, '7591', 'Reprises de provisions pour risques a court terme', '759', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3150005, '7593', 'Reprises de depreciations des stocks', '759', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3150006, '197', 'Provisions pour restructuration', '19', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3150007, '6911', 'Dotations aux provisions pour risques et charges', '691', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3150008, '7911', 'Reprises de provisions pour risques et charges', '791', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3150009, '3911', 'Depreciations des stocks de marchandises', '39', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3150010, '697', 'Dotations aux provisions et depreciations financieres', '69', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3150011, '797', 'Reprises de provisions et depreciations financieres', '79', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3150012, '4991', 'Provisions pour risques a court terme d''exploitation', '499', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3150013, '4918', 'Depreciations des autres creances (personnel, cessions, debiteurs divers)', '491', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3150014, '297', 'Depreciations des depots et cautionnements verses', '29', 'BALANCE', 'CREDIT', FALSE, FALSE)
)
INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT s.id, s.number, s.label, s.parent_number, substr(s.number, 1, 1), s.nature, s.normal_side, s.reconcilable, NULL, NULL,
       s.analytic_required, 'CLASSE_' || substr(s.number, 1, 1), 'ACTIVE', 'SYSTEM'
FROM seed s
WHERE NOT EXISTS (SELECT 1 FROM acc_account a WHERE a.number = s.number);

-- ------------------------------------------------------------------ creances

CREATE TABLE IF NOT EXISTS acc_receivable_status
(
    id                BIGINT PRIMARY KEY,
    document_number   VARCHAR(60)   NOT NULL UNIQUE,
    customer_code     VARCHAR(80),
    customer_type     VARCHAR(40),
    status            VARCHAR(30)   NOT NULL DEFAULT 'NORMAL',
    account_number    VARCHAR(20)   NOT NULL DEFAULT '411',
    since             DATE          NOT NULL,
    reason            VARCHAR(500),
    trigger_code      VARCHAR(40),
    next_review_at    DATE,
    reclass_entry     VARCHAR(60),
    voucher_number    VARCHAR(60),
    amount_ttc        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    impairment        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    individual_rate   NUMERIC(7, 4),
    rate_reason       VARCHAR(500),
    deductible        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(180)  NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_receivable_status_status CHECK (status IN ('NORMAL', 'DISPUTED', 'DOUBTFUL', 'RECOVERED', 'WRITTEN_OFF', 'RECOVERED_AFTER_LOSS'))
);
CREATE INDEX IF NOT EXISTS ix_acc_receivable_status_customer ON acc_receivable_status (customer_code);

CREATE TABLE IF NOT EXISTS acc_receivable_proposal
(
    id                 BIGINT PRIMARY KEY,
    document_number    VARCHAR(60)    NOT NULL,
    customer_code      VARCHAR(80),
    trigger_code       VARCHAR(40)    NOT NULL,
    target_status      VARCHAR(30)    NOT NULL DEFAULT 'DOUBTFUL',
    days_overdue       INTEGER        NOT NULL DEFAULT 0,
    balance_due        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    guarantees         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    suggested          NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status             VARCHAR(20)    NOT NULL DEFAULT 'PROPOSED',
    postponed_until    DATE,
    decided_by         VARCHAR(180),
    decided_at         TIMESTAMPTZ,
    reason             VARCHAR(500),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_receivable_proposal_status CHECK (status IN ('PROPOSED', 'ACCEPTED', 'POSTPONED', 'REJECTED'))
);
CREATE INDEX IF NOT EXISTS ix_acc_receivable_proposal_document ON acc_receivable_proposal (document_number, status);

CREATE TABLE IF NOT EXISTS acc_receivable_impairment
(
    id                BIGINT PRIMARY KEY,
    document_number   VARCHAR(60)    NOT NULL,
    period_code       VARCHAR(10)    NOT NULL,
    base_ht           NUMERIC(19, 2) NOT NULL DEFAULT 0,
    guarantees        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    method            VARCHAR(20)    NOT NULL,
    rate              NUMERIC(7, 4)  NOT NULL DEFAULT 0,
    amount            NUMERIC(19, 2) NOT NULL DEFAULT 0,
    previous_amount   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    difference        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    deductible        BOOLEAN        NOT NULL DEFAULT TRUE,
    override_reason   VARCHAR(500),
    kind              VARCHAR(20)    NOT NULL DEFAULT 'ADJUSTMENT',
    entry_number      VARCHAR(60),
    created_by        VARCHAR(180)   NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_receivable_impairment_kind CHECK (kind IN ('ADJUSTMENT', 'PAYMENT_RELEASE', 'WRITE_OFF_RELEASE', 'BACK_TO_NORMAL'))
);
CREATE INDEX IF NOT EXISTS ix_acc_receivable_impairment_document ON acc_receivable_impairment (document_number);

CREATE TABLE IF NOT EXISTS acc_receivable_writeoff
(
    id                BIGINT PRIMARY KEY,
    voucher_number    VARCHAR(60) UNIQUE,
    customer_code     VARCHAR(80),
    document_numbers  VARCHAR(2000) NOT NULL,
    amount_ht         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    amount_vat        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    amount_ttc        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    impairment_released NUMERIC(19, 2) NOT NULL DEFAULT 0,
    vat_recovered     BOOLEAN        NOT NULL DEFAULT TRUE,
    reason            VARCHAR(60)    NOT NULL,
    comment           VARCHAR(1000),
    justification_refs VARCHAR(1000),
    origin            VARCHAR(20)    NOT NULL DEFAULT 'DECISION',
    status            VARCHAR(20)    NOT NULL DEFAULT 'PREPARED',
    approval_level    VARCHAR(20),
    prepared_by       VARCHAR(180)   NOT NULL,
    approved_by       VARCHAR(180),
    approved_at       TIMESTAMPTZ,
    entry_number      VARCHAR(60),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_receivable_writeoff_status CHECK (status IN ('PREPARED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_acc_receivable_writeoff_origin CHECK (origin IN ('DECISION', 'BILLING_SYNC'))
);

-- ------------------------------------------------------------------ risques et provisions

CREATE TABLE IF NOT EXISTS acc_risk
(
    id                BIGINT PRIMARY KEY,
    code              VARCHAR(40)    NOT NULL UNIQUE,
    nature            VARCHAR(40)    NOT NULL,
    title             VARCHAR(300)   NOT NULL,
    third_party       VARCHAR(120),
    contract_ref      VARCHAR(120),
    origin_ref        VARCHAR(120),
    description       VARCHAR(4000),
    counsel           VARCHAR(200),
    jurisdiction      VARCHAR(200),
    next_deadline     DATE,
    probability       VARCHAR(20)    NOT NULL DEFAULT 'POSSIBLE',
    estimated_amount  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    estimation_method VARCHAR(500),
    estimated_at      DATE,
    outflow_date      DATE,
    term              VARCHAR(10)    NOT NULL DEFAULT 'LONG',
    provision_account VARCHAR(20),
    provision_amount  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    deductible        BOOLEAN        NOT NULL DEFAULT TRUE,
    section_code      VARCHAR(40),
    documents         VARCHAR(2000),
    status            VARCHAR(10)    NOT NULL DEFAULT 'OPEN',
    created_by        VARCHAR(180)   NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_risk_probability CHECK (probability IN ('PROBABLE', 'POSSIBLE', 'REMOTE')),
    CONSTRAINT ck_acc_risk_term CHECK (term IN ('SHORT', 'LONG')),
    CONSTRAINT ck_acc_risk_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS acc_risk_valuation
(
    id            BIGINT PRIMARY KEY,
    risk_id       BIGINT         NOT NULL REFERENCES acc_risk (id),
    valuation_date DATE          NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    probability   VARCHAR(20)    NOT NULL,
    reason        VARCHAR(1000),
    document_ref  VARCHAR(300),
    created_by    VARCHAR(180)   NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_acc_risk_valuation_risk ON acc_risk_valuation (risk_id);

CREATE TABLE IF NOT EXISTS acc_risk_provision_move
(
    id            BIGINT PRIMARY KEY,
    risk_id       BIGINT         NOT NULL REFERENCES acc_risk (id),
    kind          VARCHAR(20)    NOT NULL,
    move_date     DATE           NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    from_account  VARCHAR(20),
    to_account    VARCHAR(20),
    reason        VARCHAR(1000),
    entry_number  VARCHAR(60),
    created_by    VARCHAR(180)   NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_risk_provision_move_kind CHECK (kind IN ('ALLOWANCE', 'USED_RELEASE', 'UNUSED_RELEASE', 'RECLASS'))
);
CREATE INDEX IF NOT EXISTS ix_acc_risk_provision_move_risk ON acc_risk_provision_move (risk_id);

CREATE TABLE IF NOT EXISTS acc_provision_review
(
    id             BIGINT PRIMARY KEY,
    fiscal_year    VARCHAR(10)   NOT NULL,
    review_date    DATE          NOT NULL,
    voucher_number VARCHAR(60),
    summary        VARCHAR(4000),
    status         VARCHAR(20)   NOT NULL DEFAULT 'PREPARED',
    reviewed_by    VARCHAR(180)  NOT NULL,
    validated_by   VARCHAR(180),
    validated_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_provision_review_status CHECK (status IN ('PREPARED', 'VALIDATED'))
);

CREATE TABLE IF NOT EXISTS acc_retirement_obligation
(
    id            BIGINT PRIMARY KEY,
    fiscal_year   VARCHAR(10)    NOT NULL,
    computed_at   DATE           NOT NULL,
    method        VARCHAR(40)    NOT NULL,
    assumptions   VARCHAR(4000),
    headcount     INTEGER        NOT NULL DEFAULT 0,
    amount        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    previous      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    detail        TEXT,
    entry_number  VARCHAR(60),
    created_by    VARCHAR(180)   NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------------ droits

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('RECEIVABLE_RISK_READ', 'Consulter la balance agee avec risque et les etats des creances', 'RECEIVABLE_RISK', 'READ'),
           ('RECEIVABLE_RISK_CLASSIFY', 'Classer une creance en douteux ou litigieux, valider les propositions', 'RECEIVABLE_RISK', 'CLASSIFY'),
           ('RECEIVABLE_RISK_IMPAIR', 'Comptabiliser l''ajustement des depreciations de creances', 'RECEIVABLE_RISK', 'IMPAIR'),
           ('RECEIVABLE_RISK_WRITE_OFF_PREPARE', 'Preparer une decision de passage en perte', 'RECEIVABLE_RISK', 'WRITE_OFF_PREPARE'),
           ('RECEIVABLE_RISK_WRITE_OFF_APPROVE', 'Valider une decision de passage en perte jusqu''au seuil', 'RECEIVABLE_RISK', 'WRITE_OFF_APPROVE'),
           ('RECEIVABLE_RISK_WRITE_OFF_DIRECTION', 'Valider une decision de passage en perte au-dela du seuil', 'RECEIVABLE_RISK', 'WRITE_OFF_DIRECTION'),
           ('RISK_PROVISION_MANAGE', 'Tenir le registre des risques, estimer, constituer et reprendre les provisions', 'RISK_PROVISION', 'MANAGE'),
           ('RISK_PROVISION_REVIEW', 'Valider la revue de cloture des provisions', 'RISK_PROVISION', 'REVIEW'),
           ('CUSTOMER_CREDIT_OVERRIDE', 'Deroger au blocage d''un client a creance douteuse', 'CUSTOMER_CREDIT', 'OVERRIDE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('RECEIVABLE_RISK_READ'), ('RECEIVABLE_RISK_CLASSIFY'), ('RECEIVABLE_RISK_IMPAIR'),
                       ('RECEIVABLE_RISK_WRITE_OFF_PREPARE'), ('RECEIVABLE_RISK_WRITE_OFF_APPROVE'), ('RISK_PROVISION_MANAGE'),
                       ('RISK_PROVISION_REVIEW'), ('CUSTOMER_CREDIT_OVERRIDE')) AS perms(p)
    UNION ALL
    SELECT r, 'RECEIVABLE_RISK_WRITE_OFF_DIRECTION' FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    UNION ALL
    SELECT 'MANAGER', p FROM (VALUES ('RECEIVABLE_RISK_READ'), ('CUSTOMER_CREDIT_OVERRIDE')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
