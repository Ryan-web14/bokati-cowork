-- Courriel de facturation : une ou plusieurs adresses separees par des virgules (entreprises).
-- Les colonnes qui portent ce champ, sa copie sur la facture et le destinataire journalise
-- passent a 500 caracteres. Elargir un VARCHAR ne reecrit pas la table.

ALTER TABLE customer ALTER COLUMN billing_email TYPE VARCHAR(500);
ALTER TABLE billing_document ALTER COLUMN customer_email TYPE VARCHAR(500);
ALTER TABLE email_delivery_log ALTER COLUMN recipient_email TYPE VARCHAR(500);
