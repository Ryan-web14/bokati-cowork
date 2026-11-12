-- Courriel de facturation d'un membre (reglages, table user_settings) : une ou plusieurs adresses
-- separees par des virgules, comme pour les clients et les entreprises (V279_1).

ALTER TABLE user_settings ALTER COLUMN billing_email TYPE VARCHAR(500);
