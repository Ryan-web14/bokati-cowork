-- Complements des lots F18 a F20 : lien entre un bien en location-acquisition et son emprunt
-- (note 3B), reference de la source d'un echeancier de constatation d'avance (numero de pass pour
-- la reconnaissance a l'usage).

ALTER TABLE fixed_asset ADD COLUMN IF NOT EXISTS loan_code VARCHAR(40);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_loan ON fixed_asset (loan_code);

ALTER TABLE acc_deferral_schedule ADD COLUMN IF NOT EXISTS source_ref VARCHAR(120);
