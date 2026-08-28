-- =====================================================================================
-- Diagnostic RBAC · pourquoi un compte ADMIN recoit-il un 403 ?
--
-- Lecture seule : aucun UPDATE, aucun INSERT. A executer sur la base concernee
-- (prod : heroku pg:psql --app <app>).
--
-- Remplacer l'adresse ci-dessous par le compte qui recoit le refus.
-- =====================================================================================

\set target_email 'admin@sni-cg.com'


-- -------------------------------------------------------------------------------------
-- 1. Le compte, ses roles, et l'etat actif de chacun.
--    Un role inactif prive l'utilisateur de TOUTES ses permissions d'un coup.
-- -------------------------------------------------------------------------------------
SELECT u.email,
       u.is_active          AS compte_actif,
       r.name               AS role,
       r.is_active          AS role_actif,
       ur.is_active         AS rattachement_actif
FROM users u
         JOIN user_role ur ON ur.user_id = u.id
         JOIN role r ON r.id = ur.role_id
WHERE u.email = :'target_email'
ORDER BY r.name;


-- -------------------------------------------------------------------------------------
-- 2. Etat des permissions exigees par les routes qui repondent 403.
--
--      /admin/notifications/*            -> SYSTEM_SETTINGS  (repli /admin/ du resolveur)
--      PATCH /billing/documents/*/issue  -> BILLING_UPDATE
--      GET   /kyc/cases/dashboard        -> KYC_READ
--
--    Trois colonnes a lire, dans cet ordre : si permission_active est false, le droit est
--    refuse a tout le monde et V204 ne le rallume pas (c'est volontaire). Si rattachement
--    est NULL, la permission n'est pas accordee au role. Si rattachement_actif est false,
--    c'est exactement le trou que V204 repare.
-- -------------------------------------------------------------------------------------
SELECT p.name                AS permission,
       p.is_active           AS permission_active,
       r.name                AS role,
       rp.is_active          AS rattachement_actif,
       CASE
           WHEN rp.id IS NULL          THEN 'NON ACCORDEE au role'
           WHEN p.is_active IS FALSE   THEN 'PERMISSION DESACTIVEE dans le catalogue'
           WHEN rp.is_active IS FALSE  THEN 'RATTACHEMENT DESACTIVE (corrige par V204)'
           ELSE 'OK'
           END               AS verdict
FROM permission p
         CROSS JOIN role r
         LEFT JOIN role_permission rp ON rp.permission_id = p.id AND rp.role_id = r.id
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')
  AND p.name IN ('SYSTEM_SETTINGS', 'BILLING_UPDATE', 'KYC_READ',
                 'BILLING_READ', 'BILLING_CANCEL', 'CASH_READ', 'ADMIN_ACCESS')
ORDER BY p.name, r.name;


-- -------------------------------------------------------------------------------------
-- 3. Vue d'ensemble : combien de rattachements sont desactives par role.
--    Un nombre non nul confirme le diagnostic sans avoir a lister le detail.
-- -------------------------------------------------------------------------------------
SELECT r.name                                              AS role,
       COUNT(*) FILTER (WHERE rp.is_active)                AS actifs,
       COUNT(*) FILTER (WHERE NOT rp.is_active)            AS desactives,
       (SELECT COUNT(*) FROM permission WHERE is_active)   AS permissions_actives_au_catalogue
FROM role r
         LEFT JOIN role_permission rp ON rp.role_id = r.id
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')
GROUP BY r.name
ORDER BY r.name;


-- -------------------------------------------------------------------------------------
-- 4. Permissions attendues par AdminApiAuthorizationManager mais absentes du catalogue.
--    Une permission resolue par le code mais inexistante en base equivaut a un refus
--    definitif, quelle que soit la configuration des roles.
-- -------------------------------------------------------------------------------------
SELECT expected.name AS permission_absente_du_referentiel
FROM (VALUES
          ('ADMIN_ACCESS'),
          ('BILLING_CANCEL'), ('BILLING_CREATE'), ('BILLING_READ'), ('BILLING_SEND'), ('BILLING_UPDATE'),
          ('BOOKING_CANCEL'), ('BOOKING_CHECKIN'), ('BOOKING_CREATE'), ('BOOKING_READ'), ('BOOKING_UPDATE'),
          ('CASH_ADJUST'), ('CASH_CLOSE_SESSION'), ('CASH_OPEN_SESSION'), ('CASH_READ'),
          ('CLIENT_CREATE'), ('CLIENT_DELETE'), ('CLIENT_READ'), ('CLIENT_UPDATE'),
          ('CRM_CONVERT'), ('CRM_READ'), ('CRM_WRITE'),
          ('DOCUMENT_READ'), ('DOCUMENT_REVIEW'), ('DOCUMENT_UPLOAD'),
          ('INVENTORY_APPROVE'), ('INVENTORY_CREATE'), ('INVENTORY_DELETE'), ('INVENTORY_READ'), ('INVENTORY_UPDATE'),
          ('KYC_APPROVE'), ('KYC_READ'), ('KYC_REJECT'),
          ('PAYMENT_PROCESS'), ('PAYMENT_READ'), ('PAYMENT_REFUND'),
          ('REPORT_EXPORT'), ('REPORT_VIEW'),
          ('RESOURCE_CREATE'), ('RESOURCE_DELETE'), ('RESOURCE_GALLERY'), ('RESOURCE_PRICE'), ('RESOURCE_READ'), ('RESOURCE_UPDATE'),
          ('SUBSCRIPTION_ACTIVATE'), ('SUBSCRIPTION_CANCEL'), ('SUBSCRIPTION_CREATE'), ('SUBSCRIPTION_READ'), ('SUBSCRIPTION_UPDATE'),
          ('SUPPORT_ASSIGN'), ('SUPPORT_METRICS'), ('SUPPORT_READ'), ('SUPPORT_WRITE'),
          ('SYSTEM_AUDIT'), ('SYSTEM_PERMISSIONS'), ('SYSTEM_ROLES'), ('SYSTEM_SETTINGS'), ('SYSTEM_USERS'),
          ('TASK_ASSIGN'), ('TASK_READ'), ('TASK_WRITE'),
          ('VISITOR_CHECKIN'), ('VISITOR_READ'), ('VISITOR_WRITE')
      ) AS expected(name)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = expected.name)
ORDER BY expected.name;


-- -------------------------------------------------------------------------------------
-- 5. Migrations reellement appliquees · confirme si V204 est passee.
-- -------------------------------------------------------------------------------------
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 12;
