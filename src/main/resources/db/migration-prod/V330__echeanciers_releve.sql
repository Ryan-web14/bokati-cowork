-- Lot B9 (facturation) : un echeancier suit tout ce qui est paye sur sa facture, dans l'ordre des
-- echeances (ecart E12). Ce qui etait deja paye a sa creation n'y compte pas.

ALTER TABLE payment_schedule ADD COLUMN IF NOT EXISTS paid_before NUMERIC(19, 4);

UPDATE payment_schedule s
SET paid_before = GREATEST(0, d.paid_amount - s.paid_amount)
FROM billing_document d
WHERE d.document_number = s.billing_document_number AND s.paid_before IS NULL;
