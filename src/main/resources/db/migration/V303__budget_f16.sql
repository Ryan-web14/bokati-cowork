-- Lot F16 du plan finance : gestion budgetaire (analytique-et-budget section 3 a 5).

CREATE TABLE IF NOT EXISTS acc_budget_item
(
    id                BIGINT PRIMARY KEY,
    code              VARCHAR(60)   NOT NULL UNIQUE,
    label             VARCHAR(200)  NOT NULL,
    parent_code       VARCHAR(60),
    item_type         VARCHAR(20)   NOT NULL DEFAULT 'CHARGE',
    responsible       VARCHAR(180),
    control_level     VARCHAR(20)   NOT NULL DEFAULT 'ALERT',
    tolerance_pct     NUMERIC(7, 2) NOT NULL DEFAULT 0,
    control_at_parent BOOLEAN       NOT NULL DEFAULT FALSE,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    merged_into       VARCHAR(60),
    created_by        VARCHAR(180)  NOT NULL,
    approved_by       VARCHAR(180),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_budget_item_type CHECK (item_type IN ('CHARGE', 'PRODUCT', 'INVESTMENT', 'TREASURY')),
    CONSTRAINT ck_budget_item_control CHECK (control_level IN ('BLOCKING', 'ALERT', 'INFO')),
    CONSTRAINT ck_budget_item_status CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'CLOSED', 'MERGED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS acc_budget_item_mapping
(
    id        BIGINT PRIMARY KEY,
    item_code VARCHAR(60) NOT NULL,
    kind      VARCHAR(20) NOT NULL,
    value     VARCHAR(80) NOT NULL,
    CONSTRAINT uq_budget_item_mapping UNIQUE (kind, value),
    CONSTRAINT ck_budget_item_mapping_kind CHECK (kind IN ('ACCOUNT', 'EXPENSE_ITEM', 'NATURE'))
);

CREATE TABLE IF NOT EXISTS acc_budget
(
    id               BIGINT PRIMARY KEY,
    fiscal_year_code VARCHAR(10)  NOT NULL,
    version          INTEGER      NOT NULL,
    label            VARCHAR(200) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    reason           VARCHAR(1000),
    submitted_by     VARCHAR(180),
    consolidated_by  VARCHAR(180),
    approved_by      VARCHAR(180),
    approved_at      TIMESTAMPTZ,
    created_by       VARCHAR(180) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_budget_version UNIQUE (fiscal_year_code, version),
    CONSTRAINT ck_budget_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'CONSOLIDATED', 'ACTIVE', 'ARCHIVED', 'REJECTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_budget_active ON acc_budget (fiscal_year_code) WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS acc_budget_line
(
    id            BIGINT PRIMARY KEY,
    budget_id     BIGINT         NOT NULL REFERENCES acc_budget (id),
    item_code     VARCHAR(60)    NOT NULL,
    section_code  VARCHAR(60),
    annual_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    control_level VARCHAR(20),
    tolerance_pct NUMERIC(7, 2),
    responsible   VARCHAR(180),
    frozen        BOOLEAN        NOT NULL DEFAULT FALSE,
    alerted_pct   INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT uq_budget_line UNIQUE (budget_id, item_code, section_code)
);

CREATE TABLE IF NOT EXISTS acc_budget_line_month
(
    id      BIGINT PRIMARY KEY,
    line_id BIGINT         NOT NULL REFERENCES acc_budget_line (id),
    month   INTEGER        NOT NULL,
    amount  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    CONSTRAINT uq_budget_line_month UNIQUE (line_id, month),
    CONSTRAINT ck_budget_line_month CHECK (month BETWEEN 1 AND 12)
);

CREATE TABLE IF NOT EXISTS acc_budget_forecast
(
    id              BIGINT PRIMARY KEY,
    number          VARCHAR(40)    NOT NULL UNIQUE,
    item_code       VARCHAR(60)    NOT NULL,
    section_code    VARCHAR(60),
    subject         VARCHAR(500)   NOT NULL,
    amount          NUMERIC(19, 4) NOT NULL,
    planned_month   VARCHAR(7)     NOT NULL,
    recurrence      VARCHAR(20)    NOT NULL DEFAULT 'NONE',
    occurrences     INTEGER        NOT NULL DEFAULT 1,
    supplier        VARCHAR(255),
    responsible     VARCHAR(180),
    priority        VARCHAR(20)    NOT NULL DEFAULT 'IMPORTANT',
    status          VARCHAR(20)    NOT NULL DEFAULT 'PLANNED',
    realized_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reminded_at     TIMESTAMPTZ,
    created_by      VARCHAR(180)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_budget_forecast_recurrence CHECK (recurrence IN ('NONE', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT ck_budget_forecast_priority CHECK (priority IN ('ESSENTIAL', 'IMPORTANT', 'DESIRABLE')),
    CONSTRAINT ck_budget_forecast_status CHECK (status IN ('PLANNED', 'COMMITTED', 'REALIZED', 'POSTPONED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS acc_unplanned_reason
(
    code       VARCHAR(40) PRIMARY KEY,
    label      VARCHAR(200) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO acc_unplanned_reason (code, label, sort_order)
VALUES ('URGENCE', 'Urgence', 1), ('PANNE', 'Panne', 2), ('OPPORTUNITE', 'Opportunite', 3),
       ('OBLIGATION', 'Obligation reglementaire', 4), ('OUBLI', 'Oubli de prevision', 5), ('AUTRE', 'Autre', 6)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS acc_budget_commitment
(
    id              BIGINT PRIMARY KEY,
    line_id         BIGINT         NOT NULL REFERENCES acc_budget_line (id),
    source_type     VARCHAR(30)    NOT NULL,
    reference       VARCHAR(60)    NOT NULL,
    amount          NUMERIC(19, 4) NOT NULL,
    settled_amount  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    forecast_number VARCHAR(40),
    unplanned       BOOLEAN        NOT NULL DEFAULT FALSE,
    unplanned_code  VARCHAR(40),
    unplanned_text  VARCHAR(1000),
    created_by      VARCHAR(180)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    closed_at       TIMESTAMPTZ,
    CONSTRAINT ck_budget_commitment_status CHECK (status IN ('OPEN', 'SETTLED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_budget_commitment_line ON acc_budget_commitment (line_id, status);
CREATE INDEX IF NOT EXISTS idx_budget_commitment_reference ON acc_budget_commitment (source_type, reference);

CREATE TABLE IF NOT EXISTS acc_budget_transfer
(
    id           BIGINT PRIMARY KEY,
    from_line_id BIGINT         NOT NULL REFERENCES acc_budget_line (id),
    to_line_id   BIGINT         NOT NULL REFERENCES acc_budget_line (id),
    amount       NUMERIC(19, 4) NOT NULL,
    reason       VARCHAR(1000)  NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'REQUESTED',
    requested_by VARCHAR(180)   NOT NULL,
    decided_by   VARCHAR(180),
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_budget_transfer_status CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_budget_transfer_amount CHECK (amount > 0 AND from_line_id <> to_line_id)
);

CREATE TABLE IF NOT EXISTS acc_budget_override
(
    id         BIGINT PRIMARY KEY,
    line_id    BIGINT         NOT NULL,
    reference  VARCHAR(60)    NOT NULL,
    overrun    NUMERIC(19, 4) NOT NULL,
    reason     VARCHAR(1000)  NOT NULL,
    granted_by VARCHAR(180)   NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Demande de fonds : validation budgetaire supplementaire, prevision ou depense non prevue.
ALTER TABLE fund_request ADD COLUMN IF NOT EXISTS budget_approval VARCHAR(20);
ALTER TABLE fund_request ADD COLUMN IF NOT EXISTS forecast_number VARCHAR(40);
ALTER TABLE fund_request ADD COLUMN IF NOT EXISTS unplanned_code VARCHAR(40);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2968001, 'budget_forecast', 'Prevision de depense', 'Numero des previsions budgetaires', 'PRV', NULL, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'budget_forecast');

-- Nomenclature budgetaire de depart, reliee aux postes de depense.
WITH seed (code, label, parent_code, item_type, control_level) AS (
    VALUES ('FONCTIONNEMENT', 'Fonctionnement', NULL, 'CHARGE', 'ALERT'),
           ('LOCAUX', 'Locaux', 'FONCTIONNEMENT', 'CHARGE', 'ALERT'),
           ('ENERGIE', 'Energie et eau', 'LOCAUX', 'CHARGE', 'ALERT'),
           ('ACCUEIL', 'Accueil et vie du lieu', 'FONCTIONNEMENT', 'CHARGE', 'ALERT'),
           ('TECHNIQUE', 'Equipement et technique', 'FONCTIONNEMENT', 'CHARGE', 'ALERT'),
           ('ADMINISTRATIF', 'Administratif', 'FONCTIONNEMENT', 'CHARGE', 'ALERT'),
           ('PERSONNEL', 'Personnel', NULL, 'CHARGE', 'ALERT'),
           ('MISSIONS', 'Missions et deplacements', 'PERSONNEL', 'CHARGE', 'BLOCKING'),
           ('COMMERCIAL', 'Commercial', NULL, 'CHARGE', 'ALERT'),
           ('INVESTISSEMENT', 'Investissement', NULL, 'INVESTMENT', 'BLOCKING'),
           ('RECETTES', 'Chiffre d''affaires', NULL, 'PRODUCT', 'INFO')
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.code) AS rank FROM seed s WHERE NOT EXISTS (SELECT 1 FROM acc_budget_item i WHERE i.code = s.code)
)
INSERT INTO acc_budget_item (id, code, label, parent_code, item_type, control_level, created_by)
SELECT 2968100 + rank, code, label, parent_code, item_type, control_level, 'SYSTEM' FROM numbered;

WITH seed (item_code, kind, value) AS (
    VALUES ('LOCAUX', 'EXPENSE_ITEM', 'LOYER'), ('LOCAUX', 'EXPENSE_ITEM', 'CHARGES_LOCATIVES'), ('LOCAUX', 'EXPENSE_ITEM', 'NETTOYAGE'),
           ('LOCAUX', 'EXPENSE_ITEM', 'REPARATIONS'), ('LOCAUX', 'EXPENSE_ITEM', 'SECURITE'),
           ('ENERGIE', 'EXPENSE_ITEM', 'ELECTRICITE'), ('ENERGIE', 'EXPENSE_ITEM', 'EAU'),
           ('ACCUEIL', 'EXPENSE_ITEM', 'CAFE'), ('ACCUEIL', 'EXPENSE_ITEM', 'FOURNITURES'), ('ACCUEIL', 'EXPENSE_ITEM', 'EVENEMENTS_MEMBRES'),
           ('TECHNIQUE', 'EXPENSE_ITEM', 'TELECOMS'), ('TECHNIQUE', 'EXPENSE_ITEM', 'LOGICIELS'), ('TECHNIQUE', 'EXPENSE_ITEM', 'MATERIEL'),
           ('ADMINISTRATIF', 'EXPENSE_ITEM', 'HONORAIRES'), ('ADMINISTRATIF', 'EXPENSE_ITEM', 'ASSURANCES'),
           ('ADMINISTRATIF', 'EXPENSE_ITEM', 'FRAIS_BANCAIRES'), ('ADMINISTRATIF', 'EXPENSE_ITEM', 'IMPOTS'),
           ('PERSONNEL', 'EXPENSE_ITEM', 'SALAIRES'), ('MISSIONS', 'EXPENSE_ITEM', 'DEPLACEMENTS'),
           ('COMMERCIAL', 'EXPENSE_ITEM', 'PUBLICITE'), ('INVESTISSEMENT', 'ACCOUNT', '2'), ('RECETTES', 'ACCOUNT', '7')
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.kind, s.value) AS rank FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM acc_budget_item_mapping m WHERE m.kind = s.kind AND m.value = s.value)
)
INSERT INTO acc_budget_item_mapping (id, item_code, kind, value) SELECT 2968200 + rank, item_code, kind, value FROM numbered;

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('BUDGET_ITEM_MANAGE', 'Gerer la nomenclature budgetaire', 'BUDGET_ITEM', 'MANAGE'),
           ('BUDGET_FORECAST_MANAGE', 'Gerer les previsions de depenses', 'BUDGET_FORECAST', 'MANAGE'),
           ('BUDGET_PREPARE', 'Preparer les lignes de budget', 'BUDGET', 'PREPARE'),
           ('BUDGET_CONSOLIDATE', 'Consolider le budget', 'BUDGET', 'CONSOLIDATE'),
           ('BUDGET_APPROVE', 'Approuver le budget', 'BUDGET', 'APPROVE'),
           ('BUDGET_TRANSFER_REQUEST', 'Demander un virement de credits', 'BUDGET', 'TRANSFER_REQUEST'),
           ('BUDGET_TRANSFER_APPROVE', 'Approuver un virement de credits', 'BUDGET', 'TRANSFER_APPROVE'),
           ('BUDGET_OVERRIDE', 'Deroger au blocage budgetaire', 'BUDGET', 'OVERRIDE'),
           ('BUDGET_APPROVE_UNPLANNED', 'Valider une depense non prevue', 'BUDGET', 'APPROVE_UNPLANNED'),
           ('BUDGET_REPORT_READ', 'Consulter le suivi budgetaire', 'BUDGET_REPORT', 'READ')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('BUDGET_ITEM_MANAGE'), ('BUDGET_FORECAST_MANAGE'), ('BUDGET_PREPARE'), ('BUDGET_CONSOLIDATE'), ('BUDGET_APPROVE'),
                       ('BUDGET_TRANSFER_REQUEST'), ('BUDGET_TRANSFER_APPROVE'), ('BUDGET_OVERRIDE'), ('BUDGET_APPROVE_UNPLANNED'),
                       ('BUDGET_REPORT_READ')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('BUDGET_ITEM_MANAGE'), ('BUDGET_FORECAST_MANAGE'), ('BUDGET_PREPARE'), ('BUDGET_CONSOLIDATE'),
                                     ('BUDGET_TRANSFER_REQUEST'), ('BUDGET_TRANSFER_APPROVE'), ('BUDGET_OVERRIDE'), ('BUDGET_REPORT_READ')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', p FROM (VALUES ('BUDGET_FORECAST_MANAGE'), ('BUDGET_PREPARE'), ('BUDGET_TRANSFER_REQUEST'), ('BUDGET_REPORT_READ')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
