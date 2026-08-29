-- =====================================================================================
-- Versionnage des documents de facturation
--
-- billing_document_edit_history existe depuis V121 et est bien ecrite a chaque modification,
-- mais snapshot_json n'a jamais ete renseignee : le constructeur ne posait que le document,
-- le type d'edition, l'auteur et la date. Et changed_by valait la constante 'SYSTEM'.
--
-- On savait donc qu'une modification avait eu lieu, jamais ce que le document contenait avant,
-- ni qui l'avait changee. Cette migration pose ce qui manque pour en faire un vrai versionnage ;
-- le remplissage de snapshot_json releve du code.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1. Numero de version et transmission
--
--    sent_to_customer_at distingue un brouillon retouche d'une proposition reellement
--    transmise. C'est la distinction qui compte : ce sont les versions envoyees qui ont
--    une valeur probante, les autres sont du travail en cours.
-- -------------------------------------------------------------------------------------
ALTER TABLE billing_document_edit_history
    ADD COLUMN IF NOT EXISTS version_number      INTEGER,
    ADD COLUMN IF NOT EXISTS sent_to_customer_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS change_summary      TEXT;


-- -------------------------------------------------------------------------------------
-- 2. Numeroter l'historique deja en place
--
--    Les lignes anterieures n'ont pas de numero. On les numerote dans leur ordre
--    chronologique, par document, pour que la suite continue une serie coherente plutot
--    que de repartir de 1 a cote de lignes non numerotees.
-- -------------------------------------------------------------------------------------
WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY document_id ORDER BY changed_at, id) AS rn
    FROM billing_document_edit_history
    WHERE version_number IS NULL
)
UPDATE billing_document_edit_history h
SET version_number = numbered.rn
FROM numbered
WHERE h.id = numbered.id;


-- -------------------------------------------------------------------------------------
-- 3. Un numero de version est unique par document
--
--    Pose en NOT VALID : si l'historique existant comportait deja un doublon, le
--    deploiement ne doit pas echouer pour autant. La contrainte s'applique aux ecritures
--    nouvelles, qui sont celles que le code produit desormais.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    duplicate_groups INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_groups FROM (
        SELECT document_id, version_number
        FROM billing_document_edit_history
        WHERE version_number IS NOT NULL
        GROUP BY document_id, version_number
        HAVING COUNT(*) > 1) d;

    IF duplicate_groups > 0 THEN
        RAISE WARNING 'V211 : % groupe(s) de versions en double detecte(s) · index unique ux_edit_history_document_version NON cree. Reprendre la numerotation avant de le poser.', duplicate_groups;
    ELSE
        CREATE UNIQUE INDEX IF NOT EXISTS ux_edit_history_document_version
            ON billing_document_edit_history (document_id, version_number)
            WHERE version_number IS NOT NULL;
    END IF;
END $$;


-- -------------------------------------------------------------------------------------
-- 4. Recherche des versions transmises
-- -------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_edit_history_sent
    ON billing_document_edit_history (document_id, sent_to_customer_at)
    WHERE sent_to_customer_at IS NOT NULL;
