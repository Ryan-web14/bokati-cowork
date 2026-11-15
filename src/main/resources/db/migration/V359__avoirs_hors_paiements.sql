-- Avoirs comptes a part des paiements : un avoir impute sur sa propre facture reduit ce qui reste du
-- sans etre compte comme un reglement. Reste du = total - avoirs - paiements. Une facture payee puis
-- remboursee en partie par avoir (annulation d'une reservation selon la politique) est remboursee,
-- et non plus « partiellement payee ». Les factures existantes partent de zero : leur etat ne change pas.
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS credited_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;
