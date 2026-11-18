-- Facturation : montants en devises etrangeres (USD, EUR) figes a l'emission, a titre indicatif.

ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS currency_equivalents_json TEXT;
