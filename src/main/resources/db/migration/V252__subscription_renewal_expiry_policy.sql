-- Un delai dedie pour les reconductions automatiques en echec.
--
-- Ce delai empruntait grace_period_days, qui sert deja a decider quand une echeance impayee entre
-- en tolerance. Les deux notions n ont rien a voir · tolerer une facture impayee et fermer un
-- abonnement dont la reconduction a echoue ne se reglent pas au meme rythme, et les regler ensemble
-- obligeait a choisir un chiffre qui convenait mal aux deux.

ALTER TABLE subscription_policy
    ADD COLUMN IF NOT EXISTS renewal_expiry_days INT NOT NULL DEFAULT 7;

-- Une echeance impayee entre en tolerance des le lendemain · un jour, pas une semaine.
UPDATE subscription_policy SET grace_period_days = 1 WHERE grace_period_days <> 1;
