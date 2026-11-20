-- Rappel Flyway joue apres chaque migration (au demarrage) : le role SUPER_ADMIN porte tous les
-- droits actifs du catalogue. Le serveur le laissait deja tout faire, mais le frontend affiche les
-- modules d'apres la liste des droits du jeton, qui ne contenait que les droits attaches au role ;
-- les droits ajoutes sans l'attribuer au SUPER_ADMIN masquaient des modules. Rejouable sans effet.
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM role r
JOIN permission p ON p.is_active = TRUE
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

UPDATE role_permission rp
SET is_active = TRUE
FROM role r, permission p
WHERE rp.role_id = r.id AND rp.permission_id = p.id
  AND r.name = 'SUPER_ADMIN' AND p.is_active = TRUE AND rp.is_active IS DISTINCT FROM TRUE;
