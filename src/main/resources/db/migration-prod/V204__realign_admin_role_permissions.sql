-- =====================================================================================
-- Realignement des droits SUPER_ADMIN / ADMIN
--
-- V148 puis V150 avaient deja pose un filet de securite ("assign ALL active permissions to
-- SUPER_ADMIN and ADMIN"), mais il laisse deux trous qui reapparaissent en production :
--
--   1. Les deux scripts n'inserent que si la ligne n'existe pas (NOT EXISTS). Une ligne
--      role_permission presente mais desactivee (is_active = false) n'est donc jamais
--      reparee : UserPrincipal.resolveAuthorities filtre sur is_active, la permission
--      n'arrive pas dans les autorites, et l'administrateur recoit un 403 sur une action
--      qu'il est cense pouvoir faire.
--   2. C'est un instantane. Toute permission creee apres le passage de V150 n'est accordee
--      a personne, y compris ADMIN.
--
-- Ce script rejoue les deux corrections et repare en plus les lignes desactivees.
-- Entierement idempotent : rejouable sans effet de bord.
--
-- SUPER_ADMIN est traite ici par coherence du referentiel uniquement : il court-circuite
-- deja le controle de permission dans AdminApiAuthorizationManager.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1. Les roles eux-memes doivent etre actifs · resolveAuthorities ignore un role inactif,
--    ce qui prive l'utilisateur de TOUTES ses permissions d'un coup.
-- -------------------------------------------------------------------------------------
UPDATE role
SET is_active = true
WHERE name IN ('SUPER_ADMIN', 'ADMIN')
  AND is_active = false;


-- -------------------------------------------------------------------------------------
-- 2. Reactiver les rattachements desactives · c'est le trou laisse par V148 / V150.
-- -------------------------------------------------------------------------------------
UPDATE role_permission rp
SET is_active = true
FROM role r
WHERE rp.role_id = r.id
  AND r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND rp.is_active = false;


-- -------------------------------------------------------------------------------------
-- 3. Accorder toute permission active pas encore rattachee · couvre les permissions
--    creees apres le passage de V150.
-- -------------------------------------------------------------------------------------
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
CROSS JOIN permission p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND p.is_active = true
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- -------------------------------------------------------------------------------------
-- 4. Signaler les permissions desactivees dans le catalogue.
--
--    On ne les reactive volontairement PAS : desactiver une entree de permission est la
--    facon prevue de retirer un droit du referentiel, et le rallumer automatiquement
--    reintroduirait un droit retire expres. En revanche, une permission consultee par
--    AdminApiAuthorizationManager mais inactive refuse l'acces a tout le monde sans que
--    rien ne l'explique : on l'affiche dans le log de deploiement.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    inactive_permissions TEXT;
BEGIN
    SELECT string_agg(name, ', ' ORDER BY name) INTO inactive_permissions
    FROM permission
    WHERE is_active = false;

    IF inactive_permissions IS NOT NULL THEN
        RAISE WARNING 'V204 : permission(s) desactivee(s) dans le catalogue, refusees a tous les roles : %', inactive_permissions;
    END IF;
END $$;


-- -------------------------------------------------------------------------------------
-- 5. Verifier que les permissions attendues par le resolveur de chemins existent bien.
--
--    Une permission resolue mais absente du referentiel equivaut a un refus definitif,
--    quelle que soit la configuration des roles - c'est exactement le defaut corrige sur
--    BILLING:DELETE. Ce controle sert de garde-fou au prochain ajout de regle.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    missing TEXT;
BEGIN
    SELECT string_agg(expected.name, ', ' ORDER BY expected.name) INTO missing
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
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = expected.name);

    IF missing IS NOT NULL THEN
        RAISE WARNING 'V204 : permission(s) attendue(s) par AdminApiAuthorizationManager absente(s) du referentiel · tout appel les exigeant sera refuse : %', missing;
    END IF;
END $$;
