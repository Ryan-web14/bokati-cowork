-- Envoi de messages WhatsApp par Infobip : modeles de message (crees chez Infobip, approuves par
-- Meta) et journal des envois. Aucun point d'entree public : les statuts se relisent chez Infobip.

CREATE TABLE IF NOT EXISTS whatsapp_template
(
    id           BIGINT PRIMARY KEY,
    purpose      VARCHAR(60)   NOT NULL,
    name         VARCHAR(120)  NOT NULL,
    language     VARCHAR(10)   NOT NULL DEFAULT 'fr',
    category     VARCHAR(20)   NOT NULL DEFAULT 'UTILITY',
    body_text    VARCHAR(1024) NOT NULL,
    examples     TEXT,
    placeholders INTEGER       NOT NULL DEFAULT 0,
    status       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    status_detail VARCHAR(500),
    created_by   VARCHAR(180),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    synced_at    TIMESTAMPTZ,
    CONSTRAINT uk_whatsapp_template UNIQUE (name, language)
);
CREATE INDEX IF NOT EXISTS ix_whatsapp_template_purpose ON whatsapp_template (purpose, status);

CREATE TABLE IF NOT EXISTS whatsapp_message
(
    id            BIGINT PRIMARY KEY,
    message_id    VARCHAR(60)  NOT NULL UNIQUE,
    to_phone      VARCHAR(30)  NOT NULL,
    kind          VARCHAR(10)  NOT NULL,
    purpose       VARCHAR(60),
    template_name VARCHAR(120),
    content       TEXT,
    reference     VARCHAR(120),
    status_group  VARCHAR(40),
    status_name   VARCHAR(80),
    error         VARCHAR(1000),
    created_by    VARCHAR(180),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_whatsapp_message_kind CHECK (kind IN ('TEXT', 'TEMPLATE'))
);
CREATE INDEX IF NOT EXISTS ix_whatsapp_message_reference ON whatsapp_message (reference);

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('WHATSAPP_MANAGE', 'Gerer les modeles WhatsApp et consulter les envois', 'WHATSAPP', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM role r JOIN permission p ON p.name = 'WHATSAPP_MANAGE'
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
