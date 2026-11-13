-- Lot F14 : exports pour l'expert-comptable (Sage 100, Sage X3, Odoo, CSV) avec correspondance des
-- comptes et journaux par format, lots d'export traces, archive annuelle, outils de reprise
-- (import d'ecritures et de soldes d'ouverture, comparaison avec une balance externe) ; droits.

CREATE TABLE IF NOT EXISTS acc_export_mapping
(
    id         BIGINT PRIMARY KEY,
    format     VARCHAR(20)  NOT NULL,
    kind       VARCHAR(10)  NOT NULL,
    source     VARCHAR(40)  NOT NULL,
    target     VARCHAR(80)  NOT NULL,
    created_by VARCHAR(180),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_acc_export_mapping UNIQUE (format, kind, source),
    CONSTRAINT ck_acc_export_mapping_format CHECK (format IN ('SAGE100', 'SAGEX3', 'ODOO', 'CSV')),
    CONSTRAINT ck_acc_export_mapping_kind CHECK (kind IN ('ACCOUNT', 'JOURNAL'))
);

CREATE TABLE IF NOT EXISTS acc_export_batch
(
    id            BIGINT PRIMARY KEY,
    number        VARCHAR(40)    NOT NULL UNIQUE,
    format        VARCHAR(20)    NOT NULL,
    mode          VARCHAR(10)    NOT NULL,
    period_from   DATE           NOT NULL,
    period_to     DATE           NOT NULL,
    journal_code  VARCHAR(30),
    entry_count   INTEGER        NOT NULL DEFAULT 0,
    line_count    INTEGER        NOT NULL DEFAULT 0,
    total_debit   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_credit  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    excluded      INTEGER        NOT NULL DEFAULT 0,
    file_name     VARCHAR(200),
    content_hash  VARCHAR(64),
    created_by    VARCHAR(180),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_acc_export_batch_mode CHECK (mode IN ('ENTRIES', 'BALANCE'))
);
CREATE INDEX IF NOT EXISTS ix_acc_export_batch_period ON acc_export_batch (format, period_to);

CREATE TABLE IF NOT EXISTS acc_archive
(
    id               BIGINT PRIMARY KEY,
    number           VARCHAR(40)   NOT NULL UNIQUE,
    fiscal_year_code VARCHAR(10)   NOT NULL,
    file_name        VARCHAR(200)  NOT NULL,
    size_bytes       BIGINT        NOT NULL,
    content_hash     VARCHAR(64)   NOT NULL,
    manifest         TEXT          NOT NULL,
    created_by       VARCHAR(180),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('ACCOUNTING_EXPORT_RUN', 'Exporter les ecritures et la balance (Sage 100, Sage X3, Odoo, CSV)', 'ACCOUNTING_EXPORT', 'RUN'),
           ('ACCOUNTING_EXPORT_MAPPING', 'Gerer la correspondance des comptes et journaux par format d''export', 'ACCOUNTING_EXPORT', 'MAPPING'),
           ('ACCOUNTING_IMPORT_RUN', 'Importer des ecritures et des soldes de reprise', 'ACCOUNTING_IMPORT', 'RUN'),
           ('ACCOUNTING_ARCHIVE_RUN', 'Produire l''archive annuelle de la comptabilite', 'ACCOUNTING_ARCHIVE', 'RUN')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('ACCOUNTING_EXPORT_RUN'), ('ACCOUNTING_EXPORT_MAPPING'), ('ACCOUNTING_IMPORT_RUN'), ('ACCOUNTING_ARCHIVE_RUN')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('ACCOUNTING_EXPORT_RUN'), ('ACCOUNTING_EXPORT_MAPPING'), ('ACCOUNTING_IMPORT_RUN')) AS perms(p)
    UNION ALL
    SELECT 'AUDITOR', 'ACCOUNTING_EXPORT_RUN'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
