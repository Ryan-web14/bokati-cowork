-- Droits mal decomptes par les reservations · diagnostic avant et apres correction
--
-- Contexte : jusqu'au correctif, la quantite retiree a un droit etait calculee dans l'unite de la
-- RESERVATION et debitee telle quelle sur un solde libelle dans l'unite de la DEFINITION. Une
-- reservation de 5h facturee en demi-journee retirait 1 d'un droit compte en heures ; une journee
-- de 10h en retirait 1 aussi.
--
-- Ces requetes ne modifient rien. A executer en production avant tout rattrapage.

-- ---------------------------------------------------------------------------------------------
-- 1. AVANT DEPLOIEMENT · droits qu'une reservation ne saura plus convertir
--
-- Le correctif refuse desormais les unites qu'aucune duree ne produit. Si cette requete renvoie
-- des lignes, les reservations sur ces droits passeront en 400 apres deploiement : corrigez
-- l'unite de la definition, ou rattachez la ressource a un autre droit, AVANT de deployer.
-- Zero ligne = aucun risque de regression.
-- ---------------------------------------------------------------------------------------------
SELECT d.code,
       d.name,
       d.unit,
       rt.code AS type_ressource,
       rg.code AS groupe_ressource,
       (SELECT count(*) FROM entitlement_grant g
         WHERE g.entitlement_definition_id = d.id AND g.status = 'ACTIVE') AS droits_actifs
FROM entitlement_definition d
LEFT JOIN resource_type rt ON rt.id = d.resource_type_id
LEFT JOIN resource_group rg ON rg.id = d.resource_group_id
WHERE d.active = true
  AND d.deleted = false
  AND d.unit NOT IN ('HOUR', 'DAY', 'BOOKING', 'VISIT')
ORDER BY droits_actifs DESC, d.code;

-- ---------------------------------------------------------------------------------------------
-- 2. RESERVATIONS DEJA MAL DECOMPTEES · ampleur de l'ecart
--
-- Compare ce qui a ete retire au droit (ligne ENTITLEMENT) a ce que la nouvelle regle retirerait.
-- Un ecart negatif signifie que le membre a consomme MOINS que son temps reel : le forfait a ete
-- sous-decompte, a l'avantage du membre et au detriment de l'espace.
-- ---------------------------------------------------------------------------------------------
WITH attendu AS (
    SELECT b.booking_number,
           b.started_at,
           b.status,
           b.owner_type,
           b.owner_code,
           b.duration_minutes,
           b.quantity AS places,
           b.booking_unit,
           d.code AS droit,
           d.unit AS unite_droit,
           l.quantity AS debite,
           CASE d.unit
               WHEN 'HOUR' THEN ROUND(b.duration_minutes / 60.0, 4) * b.quantity
               WHEN 'DAY'  THEN CEIL(b.duration_minutes / 600.0) * b.quantity
               WHEN 'BOOKING' THEN 1 * b.quantity
               WHEN 'VISIT'   THEN 1 * b.quantity
           END AS attendu
    FROM booking b
    JOIN booking_line l ON l.booking_id = b.id AND l.line_type = 'ENTITLEMENT'
    JOIN entitlement_definition d ON upper(d.code) = upper(b.entitlement_code)
    WHERE b.entitlement_code IS NOT NULL
      AND d.unit IN ('HOUR', 'DAY', 'BOOKING', 'VISIT')
)
SELECT booking_number,
       started_at::date AS date_reservation,
       status,
       owner_code,
       duration_minutes AS minutes,
       booking_unit AS unite_facturee,
       unite_droit,
       debite,
       attendu,
       debite - attendu AS ecart
FROM attendu
WHERE debite IS DISTINCT FROM attendu
ORDER BY started_at DESC;

-- ---------------------------------------------------------------------------------------------
-- 3. SYNTHESE PAR MEMBRE · combien chacun doit encore a son forfait
--
-- Base d'un eventuel rattrapage. Le rattrapage lui-meme est une decision de gestion : reprendre
-- des heures sur un forfait deja consomme peut mettre un membre en depassement retroactif.
-- ---------------------------------------------------------------------------------------------
WITH attendu AS (
    SELECT b.owner_type,
           b.owner_code,
           d.code AS droit,
           d.unit AS unite_droit,
           l.quantity AS debite,
           CASE d.unit
               WHEN 'HOUR' THEN ROUND(b.duration_minutes / 60.0, 4) * b.quantity
               WHEN 'DAY'  THEN CEIL(b.duration_minutes / 600.0) * b.quantity
               WHEN 'BOOKING' THEN 1 * b.quantity
               WHEN 'VISIT'   THEN 1 * b.quantity
           END AS attendu
    FROM booking b
    JOIN booking_line l ON l.booking_id = b.id AND l.line_type = 'ENTITLEMENT'
    JOIN entitlement_definition d ON upper(d.code) = upper(b.entitlement_code)
    WHERE b.entitlement_code IS NOT NULL
      AND b.status IN ('CONFIRMED', 'COMPLETED', 'CHECKED_IN')
      AND d.unit IN ('HOUR', 'DAY', 'BOOKING', 'VISIT')
)
SELECT owner_type,
       owner_code,
       droit,
       unite_droit,
       count(*) AS reservations,
       sum(debite) AS total_debite,
       sum(attendu) AS total_attendu,
       sum(attendu - debite) AS manque_au_decompte
FROM attendu
GROUP BY owner_type, owner_code, droit, unite_droit
HAVING sum(attendu - debite) <> 0
ORDER BY manque_au_decompte DESC;
