-- Lot B2 (facturation) : negociation et revision des devis. Chaque version garde son motif et
-- son origine (proposition du client, contre-proposition, modification interne).

ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS version_reason VARCHAR(1000);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS version_origin VARCHAR(30);
