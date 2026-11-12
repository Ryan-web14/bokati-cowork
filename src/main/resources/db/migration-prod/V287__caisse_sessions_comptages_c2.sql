-- Plan finance, lot C2 : sessions et comptages.
--
-- Parametres de fonctionnement par caisse, etats de session complets, comptages par coupure,
-- revues d'ecart et leurs decisions, passations, absences des caissiers, scelles, delegations.
--
-- Les caisses existantes gardent leur comportement actuel (attendu visible, comptage sans
-- billetage, report de fin de mois sans comptage) pour ne pas casser l'interface en place : le
-- nouveau comportement s'active caisse par caisse. Les nouvelles caisses suivent le plan.

ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS blind_close BOOLEAN;
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS count_lines_required BOOLEAN;
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS count_tolerance NUMERIC(19, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS count_tolerance_percent NUMERIC(7, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS double_count_threshold NUMERIC(19, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS max_session_hours INTEGER;
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS month_end_policy VARCHAR(20);

UPDATE cash_register SET blind_close = FALSE WHERE blind_close IS NULL;
UPDATE cash_register SET count_lines_required = FALSE WHERE count_lines_required IS NULL;
UPDATE cash_register SET count_tolerance = 0 WHERE count_tolerance IS NULL;
UPDATE cash_register SET double_count_threshold = 10000 WHERE double_count_threshold IS NULL;
UPDATE cash_register SET max_session_hours = 24 WHERE max_session_hours IS NULL;
UPDATE cash_register SET month_end_policy = 'CARRY_OVER' WHERE month_end_policy IS NULL;

ALTER TABLE cash_register ALTER COLUMN blind_close SET DEFAULT TRUE;
ALTER TABLE cash_register ALTER COLUMN count_lines_required SET DEFAULT TRUE;
ALTER TABLE cash_register ALTER COLUMN month_end_policy SET DEFAULT 'REQUIRE_COUNT';
ALTER TABLE cash_register DROP CONSTRAINT IF EXISTS ck_cash_register_month_end;
ALTER TABLE cash_register ADD CONSTRAINT ck_cash_register_month_end
    CHECK (month_end_policy IS NULL OR month_end_policy IN ('REQUIRE_COUNT', 'CARRY_OVER'));

ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS expected_opening_amount NUMERIC(19, 4);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS opening_variance NUMERIC(19, 4);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS opening_variance_reason VARCHAR(500);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ;
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS pause_seconds BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS suspended_by VARCHAR(180);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(500);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS status_before_suspension VARCHAR(30);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS kept_float NUMERIC(19, 4);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS amount_to_remit NUMERIC(19, 4);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS closing_count_number VARCHAR(100);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS variance_decision VARCHAR(30);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS variance_decided_by VARCHAR(180);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS forced_close BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS handover_number VARCHAR(100);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS max_duration_alerted_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS cash_denomination
(
    id          BIGINT PRIMARY KEY,
    currency    VARCHAR(3)    NOT NULL,
    value       NUMERIC(19, 4) NOT NULL,
    kind        VARCHAR(10)   NOT NULL,
    label       VARCHAR(100)  NOT NULL,
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    active_from DATE          NOT NULL DEFAULT CURRENT_DATE,
    active_to   DATE,
    CONSTRAINT ck_cash_denomination_kind CHECK (kind IN ('BILL', 'COIN')),
    CONSTRAINT uq_cash_denomination UNIQUE (currency, value, kind)
);

WITH seed (value, kind, label, sort_order) AS (
    VALUES (10000, 'BILL', 'Billet de 10 000', 10), (5000, 'BILL', 'Billet de 5 000', 20),
           (2000, 'BILL', 'Billet de 2 000', 30), (1000, 'BILL', 'Billet de 1 000', 40),
           (500, 'BILL', 'Billet de 500', 50), (500, 'COIN', 'Piece de 500', 60),
           (100, 'COIN', 'Piece de 100', 70), (50, 'COIN', 'Piece de 50', 80),
           (25, 'COIN', 'Piece de 25', 90), (10, 'COIN', 'Piece de 10', 100),
           (5, 'COIN', 'Piece de 5', 110), (2, 'COIN', 'Piece de 2', 120), (1, 'COIN', 'Piece de 1', 130)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.sort_order) AS rank
    FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM cash_denomination d WHERE d.currency = 'XAF' AND d.value = s.value AND d.kind = s.kind)
)
INSERT INTO cash_denomination (id, currency, value, kind, label, sort_order, active_from)
SELECT COALESCE((SELECT MAX(id) FROM cash_denomination), 2930000) + rank, 'XAF', value, kind, label, sort_order, DATE '2000-01-01'
FROM numbered;

CREATE TABLE IF NOT EXISTS cash_count
(
    id                BIGINT PRIMARY KEY,
    count_number      VARCHAR(100)  NOT NULL UNIQUE,
    register_code     VARCHAR(100)  NOT NULL,
    session_number    VARCHAR(100),
    moment            VARCHAR(30)   NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    total             NUMERIC(19, 4) NOT NULL,
    damaged_total     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    with_lines        BOOLEAN       NOT NULL,
    expected_amount   NUMERIC(19, 4),
    variance          NUMERIC(19, 4),
    counted_by        VARCHAR(180)  NOT NULL,
    second_counter    VARCHAR(180),
    witness           VARCHAR(180),
    status            VARCHAR(20)   NOT NULL,
    superseded_by     VARCHAR(100),
    reference_number  VARCHAR(100),
    note              VARCHAR(1000),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_count_status CHECK (status IN ('VALID', 'SUPERSEDED'))
);
CREATE INDEX IF NOT EXISTS idx_cash_count_session ON cash_count (session_number, created_at);
CREATE INDEX IF NOT EXISTS idx_cash_count_register ON cash_count (register_code, created_at);

CREATE TABLE IF NOT EXISTS cash_count_line
(
    id                BIGINT PRIMARY KEY,
    count_id          BIGINT        NOT NULL REFERENCES cash_count (id),
    value             NUMERIC(19, 4) NOT NULL,
    kind              VARCHAR(10)   NOT NULL,
    quantity          INTEGER       NOT NULL,
    damaged_quantity  INTEGER       NOT NULL DEFAULT 0,
    amount            NUMERIC(19, 4) NOT NULL,
    CONSTRAINT ck_cash_count_line_quantities CHECK (quantity >= 0 AND damaged_quantity >= 0)
);
CREATE INDEX IF NOT EXISTS idx_cash_count_line_count ON cash_count_line (count_id);

CREATE TABLE IF NOT EXISTS cash_variance_review
(
    id               BIGINT PRIMARY KEY,
    review_number    VARCHAR(100)  NOT NULL UNIQUE,
    register_code    VARCHAR(100)  NOT NULL,
    session_number   VARCHAR(100),
    kind             VARCHAR(30)   NOT NULL,
    amount           NUMERIC(19, 4) NOT NULL,
    count_number     VARCHAR(100),
    cashier          VARCHAR(180),
    reason           VARCHAR(1000),
    status           VARCHAR(20)   NOT NULL,
    decision         VARCHAR(30),
    decided_by       VARCHAR(180),
    decided_at       TIMESTAMPTZ,
    decision_note    VARCHAR(1000),
    contest_deadline TIMESTAMPTZ,
    contested_at     TIMESTAMPTZ,
    contest_reason   VARCHAR(1000),
    review_due_at    TIMESTAMPTZ,
    entry_number     VARCHAR(100),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_variance_kind CHECK (kind IN ('OPENING', 'CLOSING', 'SPOT_CHECK', 'HANDOVER', 'FORCED_CLOSE', 'UNSEAL', 'TRANSFER', 'RESPONSIBILITY_TRANSFER')),
    CONSTRAINT ck_cash_variance_status CHECK (status IN ('PENDING', 'DECIDED', 'TOLERATED')),
    CONSTRAINT ck_cash_variance_decision CHECK (decision IS NULL OR decision IN ('EXPENSE', 'INCOME', 'CASHIER', 'INVESTIGATION', 'TOLERATED'))
);
CREATE INDEX IF NOT EXISTS idx_cash_variance_review_status ON cash_variance_review (status, created_at);
CREATE INDEX IF NOT EXISTS idx_cash_variance_review_session ON cash_variance_review (session_number);

CREATE TABLE IF NOT EXISTS cash_handover
(
    id                 BIGINT PRIMARY KEY,
    handover_number    VARCHAR(100)  NOT NULL UNIQUE,
    register_code      VARCHAR(100)  NOT NULL,
    from_session       VARCHAR(100)  NOT NULL,
    to_session         VARCHAR(100),
    outgoing           VARCHAR(180)  NOT NULL,
    incoming           VARCHAR(180)  NOT NULL,
    outgoing_count     VARCHAR(100)  NOT NULL,
    incoming_count     VARCHAR(100),
    outgoing_amount    NUMERIC(19, 4) NOT NULL,
    incoming_amount    NUMERIC(19, 4),
    status             VARCHAR(20)   NOT NULL,
    dispute_reason     VARCHAR(1000),
    outgoing_signed_at TIMESTAMPTZ,
    incoming_signed_at TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_handover_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DISPUTED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS cash_staff_absence
(
    id                     BIGINT PRIMARY KEY,
    absence_number         VARCHAR(100)  NOT NULL UNIQUE,
    user_email             VARCHAR(180)  NOT NULL,
    kind_code              VARCHAR(60)   NOT NULL,
    reason                 VARCHAR(500),
    starts_on              DATE          NOT NULL,
    ends_on                DATE,
    replacement_user       VARCHAR(180),
    register_codes         VARCHAR(2000),
    no_replacement_policy  VARCHAR(30),
    status                 VARCHAR(20)   NOT NULL,
    unplanned              BOOLEAN       NOT NULL DEFAULT FALSE,
    declared_by            VARCHAR(180)  NOT NULL,
    approved_by            VARCHAR(180),
    approved_at            TIMESTAMPTZ,
    forced                 BOOLEAN       NOT NULL DEFAULT FALSE,
    forced_note            VARCHAR(1000),
    reminder_sent_at       TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_absence_status CHECK (status IN ('PLANNED', 'APPROVED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT ck_cash_absence_period CHECK (ends_on IS NULL OR ends_on >= starts_on),
    CONSTRAINT ck_cash_absence_policy CHECK (no_replacement_policy IS NULL OR no_replacement_policy IN ('REMIT_TO_PARENT', 'SEAL', 'SIMPLE_SUSPENSION'))
);
CREATE INDEX IF NOT EXISTS idx_cash_absence_user ON cash_staff_absence (lower(user_email), starts_on);

CREATE TABLE IF NOT EXISTS cash_seal
(
    id               BIGINT PRIMARY KEY,
    seal_number      VARCHAR(100)  NOT NULL UNIQUE,
    register_code    VARCHAR(100)  NOT NULL,
    envelope_number  VARCHAR(100)  NOT NULL,
    amount           NUMERIC(19, 4) NOT NULL,
    count_number     VARCHAR(100)  NOT NULL,
    sealed_by        VARCHAR(180)  NOT NULL,
    witness          VARCHAR(180)  NOT NULL,
    deposited_in     VARCHAR(255),
    sealed_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    unsealed_at      TIMESTAMPTZ,
    unsealed_by      VARCHAR(180),
    unseal_witness   VARCHAR(180),
    unseal_count     VARCHAR(100),
    unseal_amount    NUMERIC(19, 4),
    absence_number   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS cash_delegation
(
    id           BIGINT PRIMARY KEY,
    delegator    VARCHAR(180)  NOT NULL,
    delegate     VARCHAR(180)  NOT NULL,
    permissions  VARCHAR(255)  NOT NULL,
    starts_on    DATE          NOT NULL,
    ends_on      DATE          NOT NULL,
    note         VARCHAR(500),
    created_by   VARCHAR(180)  NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT ck_cash_delegation_period CHECK (ends_on >= starts_on)
);

WITH seq (id, code, name, description, prefix) AS (
    VALUES (2930001, 'cash_count', 'Comptage de caisse', 'PV de billetage et comptages', 'PVB'),
           (2930002, 'cash_variance_review', 'Revue d''ecart de caisse', 'PV de revue d''ecart', 'PRE'),
           (2930003, 'cash_handover', 'Passation de caisse', 'PV de passation', 'PVP'),
           (2930004, 'cash_staff_absence', 'Absence de caissier', 'Absences declarees des caissiers', 'ABS'),
           (2930005, 'cash_seal', 'Scelles de caisse', 'PV de mise sous scelles', 'PVS'),
           (2930006, 'cash_forced_close', 'Cloture d''office', 'PV de cloture d''office', 'PCO'),
           (2930007, 'cash_spot_check', 'Comptage inopine', 'PV de comptage inopine', 'PCI')
)
INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT s.id, s.code, s.name, s.description, s.prefix, null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
FROM seq s
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition d WHERE lower(d.code) = s.code);

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('CASH_SESSION_PAUSE', 'Mettre sa session de caisse en pause', 'CASH_SESSION', 'PAUSE'),
           ('CASH_SESSION_SUSPEND', 'Suspendre une session de caisse', 'CASH_SESSION', 'SUSPEND'),
           ('CASH_SESSION_FORCE_CLOSE', 'Clore une session de caisse d''office', 'CASH_SESSION', 'FORCE_CLOSE'),
           ('CASH_SESSION_SPOT_CHECK', 'Faire un comptage inopine', 'CASH_SESSION', 'SPOT_CHECK'),
           ('CASH_STAFF_DECLARE_ABSENCE', 'Declarer une absence de caisse', 'CASH_STAFF', 'DECLARE_ABSENCE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH by_equivalent (permission_name, equivalent) AS (
    VALUES ('CASH_SESSION_PAUSE', 'CASH_SESSION_OPEN'),
           ('CASH_STAFF_DECLARE_ABSENCE', 'CASH_SESSION_OPEN'),
           ('CASH_SESSION_SUSPEND', 'CASH_VARIANCE_DECIDE'),
           ('CASH_SESSION_FORCE_CLOSE', 'CASH_VARIANCE_DECIDE'),
           ('CASH_SESSION_SPOT_CHECK', 'CASH_VARIANCE_DECIDE')
), holders AS (
    SELECT DISTINCT e.permission_name, rp.role_id
    FROM by_equivalent e
    JOIN permission equivalent ON equivalent.name = e.equivalent
    JOIN role_permission rp ON rp.permission_id = equivalent.id AND rp.is_active = TRUE
    UNION
    SELECT DISTINCT n.name, r.id
    FROM (VALUES ('CASH_SESSION_PAUSE'), ('CASH_SESSION_SUSPEND'), ('CASH_SESSION_FORCE_CLOSE'),
                 ('CASH_SESSION_SPOT_CHECK'), ('CASH_STAFF_DECLARE_ABSENCE')) AS n(name)
    JOIN role r ON r.name IN ('SUPER_ADMIN', 'ADMIN')
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT h.role_id, p.id, 'SYSTEM', TRUE
FROM holders h
JOIN permission p ON p.name = h.permission_name
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = h.role_id AND rp.permission_id = p.id
);
