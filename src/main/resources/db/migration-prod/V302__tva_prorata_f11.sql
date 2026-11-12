-- Complement du lot F11 : prorata de deduction de la TVA. La TVA recuperable du mois n'est
-- deductible qu'a hauteur du prorata ; la part non deductible passe en charge a la liquidation.

ALTER TABLE acc_vat_return ADD COLUMN IF NOT EXISTS deduction_prorata NUMERIC(7, 4) NOT NULL DEFAULT 100;
ALTER TABLE acc_vat_return ADD COLUMN IF NOT EXISTS deductible_gross NUMERIC(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE acc_vat_return ADD COLUMN IF NOT EXISTS non_deductible NUMERIC(19, 4) NOT NULL DEFAULT 0;
