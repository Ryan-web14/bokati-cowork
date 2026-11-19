-- Invitation a usage unique : a la prochaine connexion du compte vise, le serveur genere un lien
-- (30 minutes) qui ouvre une page de creation d'un utilisateur SUPER_ADMIN. Une fois utilise, le
-- lien et l'invitation sont clos definitivement.
CREATE TABLE IF NOT EXISTS super_admin_invitation (
    id               BIGSERIAL PRIMARY KEY,
    target_email     VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    token_hash       VARCHAR(64),
    issued_at        TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ,
    issue_count      INTEGER      NOT NULL DEFAULT 0,
    used_at          TIMESTAMPTZ,
    used_ip          VARCHAR(64),
    created_user_email VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_super_admin_invitation_status CHECK (status IN ('PENDING', 'ISSUED', 'USED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_super_admin_invitation_token ON super_admin_invitation (token_hash) WHERE token_hash IS NOT NULL;

INSERT INTO super_admin_invitation (target_email, status)
SELECT 'admin@sni-cg.com', 'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM super_admin_invitation WHERE lower(target_email) = 'admin@sni-cg.com');
