-- Les routes des pass plans et l'ecriture des passes exigent des permissions qui n'avaient jamais ete
-- creees : personne ne pouvait les ouvrir, pas meme un SUPER_ADMIN, dont le passe-droit s'arrete au
-- controle des URL.
--
-- Elles vont aux roles qui ont deja le droit equivalent sur les abonnements, et aux administrateurs.

WITH new_permission (name, display_name, action) AS (
    VALUES ('PASS_PLAN_READ', 'Consulter les pass plans', 'READ'),
           ('PASS_PLAN_WRITE', 'Gerer les pass plans', 'WRITE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, 'PASS_PLAN', action, TRUE, TRUE
FROM numbered;

INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + 1, 'PASS_WRITE', 'Gerer les passes', 'PASS', 'WRITE', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE name = 'PASS_WRITE');

WITH grants (permission_name, equivalent) AS (
    VALUES ('PASS_PLAN_READ', 'SUBSCRIPTION_READ'),
           ('PASS_PLAN_WRITE', 'SUBSCRIPTION_UPDATE'),
           ('PASS_WRITE', 'SUBSCRIPTION_UPDATE')
), holders AS (
    SELECT DISTINCT g.permission_name, r.id AS role_id
    FROM grants g
    JOIN role r ON r.name IN ('SUPER_ADMIN', 'ADMIN')
    UNION
    SELECT DISTINCT g.permission_name, rp.role_id
    FROM grants g
    JOIN permission equivalent ON equivalent.name = g.equivalent
    JOIN role_permission rp ON rp.permission_id = equivalent.id AND rp.is_active = TRUE
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT h.role_id, p.id, 'SYSTEM', TRUE
FROM holders h
JOIN permission p ON p.name = h.permission_name
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = h.role_id AND rp.permission_id = p.id
);
