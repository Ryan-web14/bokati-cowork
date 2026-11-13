-- Lot B5 (facturation) : la proforma n'est plus une piece fiscale (ni scellement, ni ecriture, ni
-- creance). Elle se transforme en devis accepte ; un paiement recu sur elle devient un acompte du
-- devis (decision D2). Lien de transformation garde sur la proforma.

ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS converted_to_document_number VARCHAR(60);
