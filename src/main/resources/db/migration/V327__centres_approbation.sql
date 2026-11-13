-- Lot B15 (facturation et caisse) : centres d'approbation sur un modele commun.
-- Un centre (BILLING, CASH...) est une valeur, pas une table : un nouveau centre se branche sans
-- nouvelle table. Meme hierarchie (user_staff_profile.manager_user_id, lot B12), memes circuits,
-- meme moteur, meme historique ; un niveau superieur peut valider en avance (decision D11).

CREATE TABLE IF NOT EXISTS approval_circuit
(
    id           BIGINT PRIMARY KEY,
    code         VARCHAR(60)  NOT NULL UNIQUE,
    center       VARCHAR(30)  NOT NULL,
    request_type VARCHAR(60)  NOT NULL,
    label        VARCHAR(160) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_approval_circuit_type UNIQUE (center, request_type)
);

CREATE TABLE IF NOT EXISTS approval_circuit_level
(
    id              BIGINT PRIMARY KEY,
    circuit_id      BIGINT         NOT NULL REFERENCES approval_circuit (id) ON DELETE CASCADE,
    level_order     INTEGER        NOT NULL,
    label           VARCHAR(120)   NOT NULL,
    min_amount      NUMERIC(19, 4),
    approver_rule   VARCHAR(20)    NOT NULL,
    hierarchy_depth INTEGER,
    permission      VARCHAR(80),
    user_login      VARCHAR(180),
    CONSTRAINT ux_approval_circuit_level UNIQUE (circuit_id, level_order),
    CONSTRAINT ck_approval_circuit_level_rule CHECK (approver_rule IN ('MANAGER', 'PERMISSION', 'USER'))
);

CREATE TABLE IF NOT EXISTS approval_request
(
    id              BIGINT PRIMARY KEY,
    number          VARCHAR(60)    NOT NULL UNIQUE,
    center          VARCHAR(30)    NOT NULL,
    request_type    VARCHAR(60)    NOT NULL,
    subject_type    VARCHAR(40)    NOT NULL,
    subject_ref     VARCHAR(80)    NOT NULL,
    title           VARCHAR(500)   NOT NULL,
    amount          NUMERIC(19, 4),
    currency        VARCHAR(3),
    requester       VARCHAR(180)   NOT NULL,
    requester_name  VARCHAR(255),
    status          VARCHAR(20)    NOT NULL,
    required_levels INTEGER        NOT NULL,
    current_level   INTEGER        NOT NULL,
    level_since     TIMESTAMPTZ,
    reminded_at     TIMESTAMPTZ,
    escalated_at    TIMESTAMPTZ,
    details_json    TEXT,
    last_comment    VARCHAR(1000),
    submitted_at    TIMESTAMPTZ    NOT NULL,
    decided_at      TIMESTAMPTZ,
    decided_by      VARCHAR(180),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_approval_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'RETURNED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS ix_approval_request_subject ON approval_request (center, request_type, subject_ref);
CREATE INDEX IF NOT EXISTS ix_approval_request_status ON approval_request (center, status);
CREATE INDEX IF NOT EXISTS ix_approval_request_requester ON approval_request (lower(requester));

CREATE TABLE IF NOT EXISTS approval_step
(
    id                     BIGINT PRIMARY KEY,
    request_id             BIGINT        NOT NULL REFERENCES approval_request (id) ON DELETE CASCADE,
    level_order            INTEGER       NOT NULL,
    label                  VARCHAR(120)  NOT NULL,
    approver_rule          VARCHAR(20)   NOT NULL,
    expected_approver      VARCHAR(180),
    expected_approver_name VARCHAR(255),
    permission             VARCHAR(80),
    status                 VARCHAR(30)   NOT NULL,
    decided_by             VARCHAR(180),
    decided_by_name        VARCHAR(255),
    delegated_from         VARCHAR(180),
    decided_at             TIMESTAMPTZ,
    comment                VARCHAR(1000),
    CONSTRAINT ux_approval_step_level UNIQUE (request_id, level_order),
    CONSTRAINT ck_approval_step_status CHECK (status IN ('WAITING', 'PENDING', 'APPROVED', 'APPROVED_BY_HIGHER', 'REJECTED', 'RETURNED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS ix_approval_step_expected ON approval_step (lower(expected_approver));

CREATE TABLE IF NOT EXISTS approval_event
(
    id             BIGINT PRIMARY KEY,
    request_id     BIGINT         NOT NULL REFERENCES approval_request (id) ON DELETE CASCADE,
    level_order    INTEGER,
    actor          VARCHAR(180)   NOT NULL,
    actor_name     VARCHAR(255),
    event          VARCHAR(30)    NOT NULL,
    comment        VARCHAR(1000),
    amount_before  NUMERIC(19, 4),
    amount_after   NUMERIC(19, 4),
    delegated_from VARCHAR(180),
    occurred_at    TIMESTAMPTZ    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_approval_event_request ON approval_event (request_id);
CREATE INDEX IF NOT EXISTS ix_approval_event_actor ON approval_event (lower(actor));

CREATE TABLE IF NOT EXISTS approval_delegation
(
    id           BIGINT PRIMARY KEY,
    delegator    VARCHAR(180) NOT NULL,
    delegate     VARCHAR(180) NOT NULL,
    center       VARCHAR(30),
    level_order  INTEGER,
    starts_on    DATE         NOT NULL,
    ends_on      DATE         NOT NULL,
    reason       VARCHAR(500),
    created_by   VARCHAR(180) NOT NULL,
    cancelled_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Reprise des delegations de la caisse (par niveau) dans le modele commun.
INSERT INTO approval_delegation (id, delegator, delegate, center, level_order, starts_on, ends_on, created_by, cancelled_at, created_at)
SELECT d.id, d.delegator, d.delegate, 'CASH', d.level, d.starts_on, d.ends_on, d.created_by, d.cancelled_at, d.created_at
FROM fund_approval_delegation d
WHERE NOT EXISTS (SELECT 1 FROM approval_delegation a WHERE a.id = d.id);

-- Reprise des demandes de fonds en cours de validation : meme niveau courant, niveaux deja
-- franchis approuves. Les identifiants derivent de ceux de la demande (hors de la plage du
-- generateur), pour une reprise rejouable.
INSERT INTO approval_request (id, number, center, request_type, subject_type, subject_ref, title, amount, currency, requester,
                              status, required_levels, current_level, level_since, submitted_at, created_at, updated_at)
SELECT f.id, 'APR-' || f.number, 'CASH', 'FUND_REQUEST', 'FUND_REQUEST', f.number, LEFT(COALESCE(f.subject, f.number), 500),
       COALESCE(f.approved_amount, f.total_amount), 'XAF', f.requester, 'PENDING', f.required_levels, GREATEST(f.current_level, 1),
       COALESCE(f.level_since, f.submitted_at, now()), COALESCE(f.submitted_at, now()), now(), now()
FROM fund_request f
WHERE f.status = 'IN_APPROVAL' AND f.required_levels > 0
  AND NOT EXISTS (SELECT 1 FROM approval_request a WHERE a.id = f.id);

INSERT INTO approval_step (id, request_id, level_order, label, approver_rule, permission, status)
SELECT f.id * 10 + l.level, f.id, l.level,
       CASE WHEN f.budget_approval IS NOT NULL AND l.level = f.required_levels THEN 'Validation budgétaire'
            WHEN l.level = 1 THEN 'Chef de service' WHEN l.level = 2 THEN 'Responsable financier' ELSE 'Direction' END,
       'PERMISSION',
       CASE WHEN f.budget_approval = 'OVERRIDE' AND l.level = f.required_levels THEN 'BUDGET:OVERRIDE'
            WHEN f.budget_approval IS NOT NULL AND l.level = f.required_levels THEN 'BUDGET:APPROVE_UNPLANNED'
            ELSE 'FUND_REQUEST:APPROVE_L' || LEAST(l.level, 3) END,
       CASE WHEN l.level < f.current_level THEN 'APPROVED' WHEN l.level = GREATEST(f.current_level, 1) THEN 'PENDING' ELSE 'WAITING' END
FROM fund_request f
CROSS JOIN LATERAL generate_series(1, f.required_levels) AS l(level)
WHERE f.status = 'IN_APPROVAL' AND f.required_levels > 0
  AND EXISTS (SELECT 1 FROM approval_request a WHERE a.id = f.id)
  AND NOT EXISTS (SELECT 1 FROM approval_step s WHERE s.request_id = f.id);

INSERT INTO approval_event (id, request_id, level_order, actor, event, comment, occurred_at)
SELECT f.id * 10, f.id, 0, f.requester, 'SUBMITTED', 'Reprise de la demande de fonds en cours de validation', COALESCE(f.submitted_at, now())
FROM fund_request f
WHERE EXISTS (SELECT 1 FROM approval_request a WHERE a.id = f.id)
  AND NOT EXISTS (SELECT 1 FROM approval_event e WHERE e.id = f.id * 10);

-- Circuits de la facturation (la caisse calcule les siens a partir de ses parametres).
INSERT INTO approval_circuit (id, code, center, request_type, label)
VALUES (3270101, 'BILLING_INVOICE_CHANGE', 'BILLING', 'INVOICE_CHANGE', 'Modification d''une facture avant paiement'),
       (3270102, 'BILLING_DISCOUNT_OVERRIDE', 'BILLING', 'DISCOUNT_OVERRIDE', 'Dérogation de prix ou de remise')
ON CONFLICT (code) DO NOTHING;

INSERT INTO approval_circuit_level (id, circuit_id, level_order, label, min_amount, approver_rule, hierarchy_depth, permission)
VALUES (3270111, 3270101, 1, 'Responsable (N+1)', NULL, 'MANAGER', 1, 'BILLING:APPROVE'),
       (3270112, 3270101, 2, 'Direction (N+2)', 500000, 'MANAGER', 2, 'BILLING:APPROVE_L2'),
       (3270121, 3270102, 1, 'Responsable (N+1)', NULL, 'MANAGER', 1, 'BILLING:APPROVE')
ON CONFLICT (id) DO NOTHING;

-- Numerotation.
INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 3270001, 'approval_request', 'Demande d''approbation', 'Numero des demandes d''approbation', 'APR', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1,
       'MONTHLY', TRUE, FALSE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE lower(s.code) = 'approval_request');

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('APPROVAL_MANAGE', 'Gerer la hierarchie et les circuits d''approbation, voir tout l''historique', 'APPROVAL', 'MANAGE'),
           ('BILLING_APPROVE', 'Approuver les demandes de la facturation (responsable)', 'BILLING', 'APPROVE'),
           ('BILLING_APPROVE_L2', 'Approuver les demandes de la facturation (direction)', 'BILLING', 'APPROVE_L2')
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
    CROSS JOIN (VALUES ('APPROVAL_MANAGE'), ('BILLING_APPROVE'), ('BILLING_APPROVE_L2')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('BILLING_APPROVE'), ('BILLING_APPROVE_L2')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'BILLING_APPROVE'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
