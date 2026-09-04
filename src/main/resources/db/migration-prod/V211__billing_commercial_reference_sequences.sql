-- =====================================================================================
-- Sequences des references commerciales d'un document de facturation
--
-- Le bon de commande, l'affaire et la reference client etaient remplis, faute de valeur
-- fournie, en prefixant le numero de document : « BC-INV-MAN-20260904-00000018 »,
-- « PRJ-INV-... », « REF-INV-... ». Trois champs qui ne portaient donc aucune information :
-- ils redisaient le numero de la facture, deja imprime deux fois sur la page, et n'etaient
-- ni uniques ni distincts entre eux.
--
-- Chacun recoit sa propre serie. Le lien avec la facture n'a pas a etre porte par le numero :
-- il est deja dans la ligne du document, qui range la reference a cote de son propre numero.
--
-- Les trois formes different volontairement, pour qu'un numero lu seul se rattache sans
-- ambiguite a son champ :
--   bon de commande    BC-2026-000137     serie annuelle
--   affaire            AFF-2026-00042     serie annuelle, plus courte
--   reference client   REF-202609-000318  serie mensuelle
--
-- La serie est distincte de 'purchase_order', qui numerote les bons de commande emis vers un
-- fournisseur : le bon de commande porte ici est celui du client vers nous. Deux objets differents
-- n'ont pas a partager un compteur. Le nom en base porte donc la mention « client », la colonne
-- name etant unique.
--
-- L'identifiant est calcule a partir du maximum existant et non fige : sequence_definition
-- n'a pas de sequence propre. Les trois inserts sont separes pour que chacun voie la ligne
-- posee par le precedent, sinon les trois retomberaient sur le meme identifiant.
-- =====================================================================================

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step,
 reset_policy, enabled, system_managed, created_at, updated_at)
SELECT COALESCE((SELECT MAX(id) FROM sequence_definition), 0) + 1,
       'billing_purchase_order',
       'Bon de commande client',
       'Serie des bons de commande rattaches a un document de facturation',
       'BC', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_purchase_order');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step,
 reset_policy, enabled, system_managed, created_at, updated_at)
SELECT COALESCE((SELECT MAX(id) FROM sequence_definition), 0) + 1,
       'billing_project',
       'Affaire de facturation',
       'Serie des affaires rattachees a un document de facturation',
       'AFF', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_project');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step,
 reset_policy, enabled, system_managed, created_at, updated_at)
SELECT COALESCE((SELECT MAX(id) FROM sequence_definition), 0) + 1,
       'billing_customer_reference',
       'Reference client de facturation',
       'Serie des references client rattachees a un document de facturation',
       'REF', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_customer_reference');


-- -------------------------------------------------------------------------------------
-- Les anciennes valeurs derivees du numero de document sont effacees. Elles n'ont jamais
-- rien designe : les conserver ferait cohabiter sur une meme colonne deux formes qui ne se
-- lisent pas de la meme facon. Seules celles de la forme exacte « <PREFIXE>-<numero du
-- document> » sont visees, une reference reellement transmise par un client est intacte.
-- -------------------------------------------------------------------------------------
UPDATE billing_document
SET po_number = null
WHERE po_number = 'BC-' || document_number;

UPDATE billing_document
SET project_code = null
WHERE project_code = 'PRJ-' || document_number;

UPDATE billing_document
SET customer_reference = null
WHERE customer_reference = 'REF-' || document_number;


DO $$
DECLARE
    manquantes int;
BEGIN
    SELECT count(*) INTO manquantes
    FROM (VALUES ('billing_purchase_order'), ('billing_project'), ('billing_customer_reference')) AS attendu(code)
    WHERE NOT EXISTS (SELECT 1 FROM sequence_definition s WHERE s.code = attendu.code);

    IF manquantes > 0 THEN
        RAISE WARNING 'V211 : % sequence(s) de reference commerciale absente(s) · les documents seront emis sans ces references.', manquantes;
    END IF;
END $$;
