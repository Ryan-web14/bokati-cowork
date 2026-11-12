-- Lot F20 : produits et charges constates d'avance, rattachement a la periode. Periode de service
-- sur les lignes de facture et de charge, echeanciers de reconnaissance et leurs evenements,
-- consommation des pass a credits, traitements de fin de periode par ajustement du solde,
-- factures a etablir et non parvenues avec extourne ; droits.

-- ------------------------------------------------------------------ periodes de service

ALTER TABLE billing_document_line ADD COLUMN IF NOT EXISTS service_start DATE;
ALTER TABLE billing_document_line ADD COLUMN IF NOT EXISTS service_end DATE;
ALTER TABLE billing_document_line ADD COLUMN IF NOT EXISTS recognition_method VARCHAR(20);
ALTER TABLE billing_document_line ADD COLUMN IF NOT EXISTS recognition_source VARCHAR(30);
ALTER TABLE billing_document_line ADD COLUMN IF NOT EXISTS original_line_id BIGINT;

ALTER TABLE acc_supplier_invoice_line ADD COLUMN IF NOT EXISTS service_start DATE;
ALTER TABLE acc_supplier_invoice_line ADD COLUMN IF NOT EXISTS service_end DATE;
ALTER TABLE acc_supplier_invoice_line ADD COLUMN IF NOT EXISTS recognition_method VARCHAR(20);

ALTER TABLE expense_justification_line ADD COLUMN IF NOT EXISTS service_start DATE;
ALTER TABLE expense_justification_line ADD COLUMN IF NOT EXISTS service_end DATE;
ALTER TABLE expense_justification_line ADD COLUMN IF NOT EXISTS recognition_method VARCHAR(20);

ALTER TABLE cash_operation_nature ADD COLUMN IF NOT EXISTS periodic BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cash_operation_nature ADD COLUMN IF NOT EXISTS default_duration_months INTEGER;
ALTER TABLE cash_operation_nature ADD COLUMN IF NOT EXISTS default_method VARCHAR(20);

-- ------------------------------------------------------------------ echeanciers

CREATE TABLE IF NOT EXISTS acc_deferral_schedule
(
    id              BIGINT PRIMARY KEY,
    company_code    VARCHAR(40)    NOT NULL DEFAULT 'MAIN',
    kind            VARCHAR(10)    NOT NULL,
    source_type     VARCHAR(30)    NOT NULL,
    source_line_id  BIGINT         NOT NULL,
    document_number VARCHAR(60),
    third_party     VARCHAR(120),
    label           VARCHAR(300),
    account_number  VARCHAR(20)    NOT NULL,
    section_code    VARCHAR(40),
    activity        VARCHAR(40),
    amount          NUMERIC(19, 2) NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    method          VARCHAR(20)    NOT NULL,
    total_units     NUMERIC(19, 4),
    recognition_source VARCHAR(30),
    version         INTEGER        NOT NULL DEFAULT 1,
    status          VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    ended_on        DATE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_deferral_schedule_source UNIQUE (kind, source_type, source_line_id),
    CONSTRAINT ck_acc_deferral_schedule_kind CHECK (kind IN ('REVENUE', 'EXPENSE')),
    CONSTRAINT ck_acc_deferral_schedule_method CHECK (method IN ('DAILY', 'MONTHLY', 'USAGE', 'EVENT_DATE')),
    CONSTRAINT ck_acc_deferral_schedule_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS ix_acc_deferral_schedule_document ON acc_deferral_schedule (document_number);

CREATE TABLE IF NOT EXISTS acc_deferral_schedule_event
(
    id           BIGINT PRIMARY KEY,
    schedule_id  BIGINT        NOT NULL REFERENCES acc_deferral_schedule (id),
    type         VARCHAR(20)   NOT NULL,
    event_date   DATE          NOT NULL,
    before_value VARCHAR(200),
    after_value  VARCHAR(200),
    reason       VARCHAR(500),
    created_by   VARCHAR(180)  NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_deferral_schedule_event_type CHECK (type IN ('FREEZE', 'SHIFT', 'TERMINATION', 'CANCELLATION', 'EXPIRY', 'RESCHEDULE', 'PERIOD_EDIT'))
);
CREATE INDEX IF NOT EXISTS ix_acc_deferral_schedule_event_schedule ON acc_deferral_schedule_event (schedule_id);

CREATE TABLE IF NOT EXISTS acc_usage_consumption
(
    id          BIGINT PRIMARY KEY,
    schedule_id BIGINT         NOT NULL REFERENCES acc_deferral_schedule (id),
    usage_date  DATE           NOT NULL,
    units       NUMERIC(19, 4) NOT NULL,
    source_ref  VARCHAR(120),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_acc_usage_consumption_schedule ON acc_usage_consumption (schedule_id);

CREATE TABLE IF NOT EXISTS acc_deferral_run
(
    id             BIGINT PRIMARY KEY,
    period_code    VARCHAR(10)    NOT NULL,
    kind           VARCHAR(10)    NOT NULL,
    status         VARCHAR(10)    NOT NULL DEFAULT 'POSTED',
    entry_number   VARCHAR(60),
    total_need     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_previous NUMERIC(19, 2) NOT NULL DEFAULT 0,
    difference     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_by     VARCHAR(180)   NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_acc_deferral_run_period ON acc_deferral_run (kind, period_code);

CREATE TABLE IF NOT EXISTS acc_deferral_run_line
(
    id          BIGINT PRIMARY KEY,
    run_id      BIGINT         NOT NULL REFERENCES acc_deferral_run (id),
    schedule_id BIGINT         NOT NULL REFERENCES acc_deferral_schedule (id),
    need        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    previous    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    difference  NUMERIC(19, 2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_acc_deferral_run_line_schedule ON acc_deferral_run_line (schedule_id);

-- ------------------------------------------------------------------ factures a etablir et non parvenues

CREATE TABLE IF NOT EXISTS acc_accrual
(
    id                    BIGINT PRIMARY KEY,
    kind                  VARCHAR(20)    NOT NULL,
    source_type           VARCHAR(30)    NOT NULL DEFAULT 'MANUAL',
    reference             VARCHAR(120)   NOT NULL,
    third_party           VARCHAR(120),
    label                 VARCHAR(300),
    account_number        VARCHAR(20)    NOT NULL,
    section_code          VARCHAR(40),
    period_code           VARCHAR(10)    NOT NULL,
    estimated_amount      NUMERIC(19, 2) NOT NULL,
    vat_amount            NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status                VARCHAR(20)    NOT NULL DEFAULT 'PROPOSED',
    entry_number          VARCHAR(60),
    reversal_entry_number VARCHAR(60),
    actual_reference      VARCHAR(120),
    actual_amount         NUMERIC(19, 2),
    gap                   NUMERIC(19, 2),
    created_by            VARCHAR(180)   NOT NULL,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_accrual_kind CHECK (kind IN ('ACCRUED_REVENUE', 'ACCRUED_EXPENSE')),
    CONSTRAINT ck_acc_accrual_status CHECK (status IN ('PROPOSED', 'POSTED', 'REVERSED', 'MATCHED', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS ix_acc_accrual_period ON acc_accrual (period_code);

-- ------------------------------------------------------------------ droits

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('ACCOUNTING_DEFERRAL_READ', 'Consulter les echeanciers de constatations d''avance et leurs etats', 'ACCOUNTING_DEFERRAL', 'READ'),
           ('ACCOUNTING_DEFERRAL_RUN', 'Lancer et comptabiliser le traitement de fin de periode des constatations d''avance', 'ACCOUNTING_DEFERRAL', 'RUN'),
           ('ACCOUNTING_DEFERRAL_EDIT_PERIOD', 'Corriger la periode de service d''une ligne, avec motif', 'ACCOUNTING_DEFERRAL', 'EDIT_PERIOD'),
           ('ACCOUNTING_ACCRUAL_MANAGE', 'Valider ou ajuster les factures a etablir et non parvenues', 'ACCOUNTING_ACCRUAL', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('ACCOUNTING_DEFERRAL_READ'), ('ACCOUNTING_DEFERRAL_RUN'), ('ACCOUNTING_DEFERRAL_EDIT_PERIOD'),
                       ('ACCOUNTING_ACCRUAL_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'ACCOUNTING_DEFERRAL_READ'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
