-- D ou vient l encaissement · le client depuis son espace, un agent au guichet, ou une automatisation.
-- Sans cette colonne, un reglement fait depuis l espace client n etait annonce a personne : la caisse
-- le decouvrait en consultant la facture, parfois des jours plus tard.

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS channel VARCHAR(30);

-- Les transactions anterieures gardent NULL · on ne sait pas d ou elles viennent, et l inventer
-- fausserait toute lecture ulterieure.
CREATE INDEX IF NOT EXISTS idx_payment_transaction_channel
    ON payment_transaction (channel, status);
