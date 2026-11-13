-- Lot B0 (facturation) : la demande de signature d'un devis garde la revision signee ; une
-- revision du devis rend la demande caduque (statut CANCELLED).

ALTER TABLE billing_document_signature ADD COLUMN IF NOT EXISTS document_revision INTEGER;
