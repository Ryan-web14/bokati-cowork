-- =====================================================================================
-- Garde-fous de remise · tracage du depassement et permission de contournement
--
-- Le controle lui-meme vit dans le code (BillingDiscountGuard). Cette migration pose ce
-- qui doit exister en base pour qu'un depassement soit possible et surtout tracable :
-- un motif obligatoire, son auteur, sa date, et le droit qui autorise l'operation.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1. Tracage du depassement sur le document
--
--    Un depassement silencieux n'a aucune valeur de controle : c'est le motif ecrit qui
--    fait la difference entre une derogation assumee et un contournement.
-- -------------------------------------------------------------------------------------
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS discount_override_reason TEXT,
    ADD COLUMN IF NOT EXISTS discount_override_by     VARCHAR(180),
    ADD COLUMN IF NOT EXISTS discount_override_at     TIMESTAMP WITH TIME ZONE;

-- Les trois vont ensemble : un motif sans auteur, ou l'inverse, signale une ecriture
-- partielle. Pose en NOT VALID pour ne pas faire echouer le deploiement sur l'existant.
ALTER TABLE billing_document
    ADD CONSTRAINT ck_billing_document_discount_override_complete
        CHECK (
            (discount_override_reason IS NULL AND discount_override_by IS NULL AND discount_override_at IS NULL)
            OR (discount_override_reason IS NOT NULL AND discount_override_by IS NOT NULL AND discount_override_at IS NOT NULL)
        ) NOT VALID;


-- -------------------------------------------------------------------------------------
-- 2. La permission de contournement
--
--    Une permission du referentiel, pas un role code en dur dans le service. Un role en
--    dur cree un droit invisible de l'ecran des roles : impossible a accorder, impossible
--    a retirer, impossible a auditer. C'est exactement le defaut qui a produit les refus
--    sur BILLING:DELETE, permission que le resolveur exigeait sans qu'elle existe.
--
--    L'identifiant se calcule a partir du maximum existant · la table permission n'a pas
--    de sequence, c'est la convention suivie depuis V87.
-- -------------------------------------------------------------------------------------
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + 1,
       'BILLING_DISCOUNT_OVERRIDE',
       'Depasser les limites de remise',
       'BILLING',
       'DISCOUNT_OVERRIDE',
       TRUE,
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE name = 'BILLING_DISCOUNT_OVERRIDE');


-- -------------------------------------------------------------------------------------
-- 3. Rattachement aux roles d'administration
--
--    Sans ce rattachement la permission existe mais personne ne l'exerce, et le blocage
--    devient definitif au lieu d'etre une derogation.
-- -------------------------------------------------------------------------------------
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
CROSS JOIN permission p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND p.name = 'BILLING_DISCOUNT_OVERRIDE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Meme si la ligne existait deja mais desactivee · c'est le trou que V208 avait comble
-- pour l'ensemble du referentiel, et qu'il ne faut pas rouvrir ici.
UPDATE role_permission rp
SET is_active = true
FROM role r, permission p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND p.name = 'BILLING_DISCOUNT_OVERRIDE'
  AND rp.is_active = false;
