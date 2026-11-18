-- Ressources, lot R2 : etage de la ressource (champ accepte mais perdu jusque-la).
ALTER TABLE resource ADD COLUMN IF NOT EXISTS floor_label VARCHAR(50);
