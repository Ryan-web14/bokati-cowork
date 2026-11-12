-- Lot F2 du plan finance : moteur d'imputation.
--
--   * acc_posting_rule : schemas d'ecriture par type d'evenement, versionnes, avec date d'effet ;
--   * acc_posting_event : registre d'idempotence des evenements comptables (type + reference),
--     file d'attente du moteur et ecran des ecritures en echec ;
--   * droits ACCOUNTING_POSTING_*.

CREATE TABLE IF NOT EXISTS acc_posting_rule
(
    id             BIGINT PRIMARY KEY,
    code           VARCHAR(80)   NOT NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    event_type     VARCHAR(80)   NOT NULL,
    label          VARCHAR(255)  NOT NULL,
    description    VARCHAR(2000),
    family         VARCHAR(40)   NOT NULL,
    priority       INTEGER       NOT NULL DEFAULT 100,
    conditions     TEXT,
    journal        VARCHAR(80),
    entry_label    VARCHAR(500),
    lines          TEXT,
    no_entry       BOOLEAN       NOT NULL DEFAULT FALSE,
    sample_payload TEXT,
    effective_from DATE          NOT NULL DEFAULT DATE '2026-01-01',
    status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    activated_by   VARCHAR(180),
    activated_at   TIMESTAMPTZ,
    created_by     VARCHAR(180)  NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_posting_rule_version UNIQUE (code, version),
    CONSTRAINT ck_acc_posting_rule_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT ck_acc_posting_rule_content CHECK (no_entry OR (journal IS NOT NULL AND lines IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_acc_posting_rule_event ON acc_posting_rule (event_type, status);

CREATE TABLE IF NOT EXISTS acc_posting_event
(
    id                 BIGINT PRIMARY KEY,
    event_type         VARCHAR(80)   NOT NULL,
    reference          VARCHAR(150)  NOT NULL,
    family             VARCHAR(40),
    event_date         DATE          NOT NULL,
    payload            TEXT          NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    replay             BOOLEAN       NOT NULL DEFAULT FALSE,
    failure_reason     VARCHAR(2000),
    rule_code          VARCHAR(80),
    rule_version       INTEGER,
    entry_number       VARCHAR(40),
    provisional_number VARCHAR(40),
    shifted_note       VARCHAR(500),
    attempts           INTEGER       NOT NULL DEFAULT 0,
    last_attempt_at    TIMESTAMPTZ,
    posted_at          TIMESTAMPTZ,
    ignored_by         VARCHAR(180),
    ignored_reason     VARCHAR(1000),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_posting_event UNIQUE (event_type, reference),
    CONSTRAINT ck_acc_posting_event_status CHECK (status IN ('PENDING', 'POSTED', 'UNBALANCED', 'FAILED', 'IGNORED'))
);

CREATE INDEX IF NOT EXISTS idx_acc_posting_event_status ON acc_posting_event (status, event_date);

-- Une piece regeneree laisse sa trace dans le suivi des attentes.
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS posting_event_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_acc_entry_posting_event ON acc_entry (posting_event_id);

-- ------------------------------------------------------------------ permissions

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('ACCOUNTING_POSTING_READ', 'Consulter les schemas et les evenements comptables', 'ACCOUNTING_POSTING', 'READ'),
           ('ACCOUNTING_POSTING_MANAGE', 'Modifier et activer les schemas d''ecriture', 'ACCOUNTING_POSTING', 'MANAGE'),
           ('ACCOUNTING_POSTING_REPLAY', 'Regenerer, ignorer et rejouer les evenements comptables', 'ACCOUNTING_POSTING', 'REPLAY')
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
    CROSS JOIN (VALUES ('ACCOUNTING_POSTING_READ'), ('ACCOUNTING_POSTING_MANAGE'), ('ACCOUNTING_POSTING_REPLAY')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('ACCOUNTING_POSTING_READ'), ('ACCOUNTING_POSTING_REPLAY')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
