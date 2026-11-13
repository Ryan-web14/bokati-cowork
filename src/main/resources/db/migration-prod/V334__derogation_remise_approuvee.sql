-- Lot C3 (facturation, complements) : une remise ou un prix hors limites passe par le centre
-- d'approbation de la facturation (circuit BILLING_DISCOUNT_OVERRIDE, N+1) avant l'emission.
-- L'empreinte des lignes lie l'accord a l'offre approuvee : toute modification le fait tomber.
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS discount_approval_number VARCHAR(40);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS discount_approval_status VARCHAR(20);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS discount_approval_fingerprint VARCHAR(64);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS discount_approval_reason TEXT;
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS discount_approval_comment VARCHAR(1000);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS discount_approved_by VARCHAR(180);
