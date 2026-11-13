-- Complements du lot F12 : canal et destinataire de l'envoi des confirmations de solde (courriel,
-- WhatsApp ou impression, sans acces exterieur), codes de flux de tresorerie portes par les schemas
-- d'imputation et par les pieces.

ALTER TABLE acc_confirmation_request ADD COLUMN IF NOT EXISTS sent_channel VARCHAR(12);
ALTER TABLE acc_confirmation_request ADD COLUMN IF NOT EXISTS recipient VARCHAR(180);

ALTER TABLE acc_posting_rule ADD COLUMN IF NOT EXISTS cash_flow_code VARCHAR(4);
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS cash_flow_code VARCHAR(4);
