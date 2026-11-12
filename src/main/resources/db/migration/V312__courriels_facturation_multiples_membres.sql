-- Renumerotee V283_2 -> V312 : les bases de developpement avaient deja applique V284 et suivantes,
-- Flyway refusait cette migration plus ancienne (out-of-order desactive). Contenu inchange, rejouable sans risque.
-- Courriel de facturation d'un membre (reglages, table user_settings) : une ou plusieurs adresses
-- separees par des virgules, comme pour les clients et les entreprises (V279_1).

ALTER TABLE user_settings ALTER COLUMN billing_email TYPE VARCHAR(500);
