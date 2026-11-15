-- Centre d'audit, lot A2 : evenements de securite (connexions, verrouillages, mots de passe,
-- deconnexions, refus d'acces, limitations), fin de verrouillage d'un compte, trace de la revocation
-- d'une session, et lecture du centre ouverte a tous les administrateurs (decision du 14/11/2026).

CREATE TABLE IF NOT EXISTS security_event
(
    id           BIGINT PRIMARY KEY,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    event_type   VARCHAR(40)  NOT NULL,
    user_id      BIGINT,
    user_code    VARCHAR(80),
    login        VARCHAR(250),
    ip_address   VARCHAR(80),
    user_agent   VARCHAR(500),
    request_id   VARCHAR(64),
    path         VARCHAR(300),
    details_json TEXT
);
CREATE INDEX IF NOT EXISTS ix_security_event_created ON security_event (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_security_event_type ON security_event (event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_security_event_user ON security_event (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_security_event_login ON security_event (lower(login), created_at DESC);

ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS revoked_by VARCHAR(250);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS revoked_reason VARCHAR(300);

WITH grants (role_name, permission_name) AS (
    VALUES ('ADMIN', 'SYSTEM_AUDIT')
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
