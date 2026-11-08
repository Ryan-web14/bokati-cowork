-- GED, lot 2 : le plan de classement automatique.
--
-- Les dossiers du plan (Membres, Clients, Entreprises, un dossier par proprietaire et ses
-- sous-dossiers KYC, Contrats, Domiciliation...) sont de vrais dossiers, crees a la demande par le
-- serveur. Leur cle systeme les retrouve sans ambiguite, empeche d'en creer deux quand deux depots
-- arrivent en meme temps, et les protege : ni renommes, ni deplaces, ni supprimes.

ALTER TABLE document_folder ADD COLUMN IF NOT EXISTS system_key VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uk_document_folder_system_key
    ON document_folder (system_key) WHERE system_key IS NOT NULL;

-- Les documents encore sans dossier, que le classement reprend par lots.
CREATE INDEX IF NOT EXISTS idx_document_unfiled
    ON document (id) WHERE folder_id IS NULL AND deleted = FALSE;
