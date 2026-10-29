-- Tranche 1 du centre de gestion des entreprises · le rattachement d'un membre a une organisation.
--
-- Un membre peut etre rattache a plusieurs organisations, une fois chacune, comme proprietaire
-- (OWNER) ou employe (EMPLOYEE). member.customer_id ne change pas : il reste le payeur par defaut
-- du membre. Un lien termine reste en historique.

CREATE SEQUENCE IF NOT EXISTS customer_member_link_short_id_seq START WITH 1;

CREATE TABLE IF NOT EXISTS customer_member_link (
    id                BIGINT PRIMARY KEY,
    short_id          BIGINT       NOT NULL DEFAULT nextval('customer_member_link_short_id_seq'),
    customer_id       BIGINT       NOT NULL REFERENCES customer (id),
    member_id         BIGINT       NOT NULL REFERENCES member (id),
    role              VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    started_at        TIMESTAMPTZ  NOT NULL,
    ended_at          TIMESTAMPTZ,
    ended_reason      VARCHAR(500),
    invoice_link_mode VARCHAR(20),
    can_sign          BOOLEAN      NOT NULL DEFAULT FALSE,
    can_collect_mail  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by        VARCHAR(150) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ
);

ALTER SEQUENCE customer_member_link_short_id_seq OWNED BY customer_member_link.short_id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_member_link_short_id ON customer_member_link (short_id);
-- Un seul lien actif par couple organisation-membre.
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_member_link_active
    ON customer_member_link (customer_id, member_id) WHERE status = 'ACTIVE';
-- Un seul lien en mode AUTOMATIC par membre : une facture ne doit pas hesiter entre deux organisations.
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_member_link_automatic
    ON customer_member_link (member_id) WHERE status = 'ACTIVE' AND invoice_link_mode = 'AUTOMATIC';
CREATE INDEX IF NOT EXISTS idx_customer_member_link_member ON customer_member_link (member_id);

-- Le mode de rattachement des factures par defaut de l'organisation · le lien peut le remplacer.
ALTER TABLE customer ADD COLUMN IF NOT EXISTS default_invoice_link_mode VARCHAR(20);
UPDATE customer SET default_invoice_link_mode = 'ON_CHOICE'
WHERE type = 'COMPANY' AND default_invoice_link_mode IS NULL;

-- Reprise : chaque membre dont le payeur est une organisation en devient employe, a sa date
-- de creation. Les proprietaires se designent ensuite a la main.
INSERT INTO customer_member_link (id, customer_id, member_id, role, status, started_at, created_by, created_at)
SELECT (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT * 1000 + ROW_NUMBER() OVER (ORDER BY m.id),
       c.id, m.id, 'EMPLOYEE', 'ACTIVE', COALESCE(m.created_at, NOW()), 'MIGRATION', NOW()
FROM member m
JOIN customer c ON c.id = m.customer_id
WHERE c.type = 'COMPANY'
  AND NOT c.deleted
  AND NOT m.deleted
  AND NOT EXISTS (SELECT 1 FROM customer_member_link l
                  WHERE l.customer_id = c.id AND l.member_id = m.id AND l.status = 'ACTIVE');
