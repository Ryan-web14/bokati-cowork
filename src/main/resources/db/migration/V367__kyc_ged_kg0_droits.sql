-- Centre KYC et GED, lot KG0 : droit KYC_MANAGE pour les ecritures sur les dossiers KYC (creation,
-- soumission, assignation, risque, notes, exigences, depot par l'equipe, regles de controle croise),
-- qui n'exigeaient jusqu'ici que KYC_READ. Donne aux roles qui decident deja (KYC_APPROVE), et aux
-- administrateurs.

WITH missing (name, display_name, module, action) AS (
    VALUES ('KYC_MANAGE', 'Gerer les dossiers KYC', 'KYC', 'MANAGE')
), numbered AS (
    SELECT m.*, row_number() OVER () AS rn FROM missing m
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = m.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rn, name, display_name, module, action, TRUE, TRUE
FROM numbered;

INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT DISTINCT r.id, p.id, 'SYSTEM', TRUE
FROM role r
JOIN permission p ON p.name = 'KYC_MANAGE'
WHERE (r.name IN ('ADMIN', 'SUPER_ADMIN')
       OR EXISTS (SELECT 1 FROM role_permission rp JOIN permission ap ON ap.id = rp.permission_id
                  WHERE rp.role_id = r.id AND ap.name = 'KYC_APPROVE' AND rp.is_active))
  AND NOT EXISTS (SELECT 1 FROM role_permission x WHERE x.role_id = r.id AND x.permission_id = p.id);
