-- Verification de l'adresse · un fait date, pas une deduction.
--
-- L'espace client deduisait « email verifie » de « compte non verrouille », ce qui etait vrai pour
-- tout le monde des l'inscription. Et l'activation par un administrateur rendait le membre ACTIVE
-- sans lui donner le role MEMBER ni marquer l'adresse : le jeton sortait sans role, et l'espace
-- client repondait 403 a quelqu'un que l'on venait d'activer.
--
-- Deux colonnes : quand l'adresse a ete verifiee, et par quoi · le code saisi par le titulaire, ou
-- l'administrateur qui, en activant le compte sans attendre le code, s'en porte garant.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS email_verified_by VARCHAR(120);

COMMENT ON COLUMN users.email_verified_by IS
    'OTT_VERIFICATION si le titulaire a saisi son code · sinon l identifiant de l administrateur qui a active le compte';

-- Ceux qui ont saisi un code : verifies a la date de ce code.
UPDATE users u
SET email_verified_at = ott.used_at,
    email_verified_by = 'OTT_VERIFICATION'
FROM (
    SELECT user_id, MIN(created_at) AS used_at
    FROM one_time_token
    WHERE is_used = TRUE
    GROUP BY user_id
) ott
WHERE u.id = ott.user_id
  AND u.email_verified_at IS NULL;

-- Ceux qu'un administrateur a actives sans code : verifies par lui, a la date d'activation.
UPDATE users u
SET email_verified_at = COALESCE(m.portal_activated_at, now()),
    email_verified_by = 'ADMIN_ACTIVATION'
FROM member m
WHERE m.user_id = u.id
  AND m.member_status = 'ACTIVE'
  AND m.deleted = FALSE
  AND u.email_verified_at IS NULL;

-- Le personnel n'a pas de code a saisir · son adresse est celle qu'on lui a donnee.
UPDATE users u
SET email_verified_at = COALESCE(u.created_at, now()),
    email_verified_by = 'STAFF_PROVISIONING'
FROM role_user ru
JOIN role r ON r.id = ru.role_id
WHERE ru.user_id = u.id
  AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'STAFF')
  AND u.email_verified_at IS NULL;
