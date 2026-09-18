-- Alignement de la creation d'un pass sur celle d'un abonnement.
--
-- Deux manques de modele empechaient de corriger le comportement.
--
-- 1. pass_plan_version ne portait ni transferable ni shareable. L'achat d'un pass depuis un plan
--    forcait donc les deux a faux sur le pass produit, sans jamais pouvoir consulter la volonte du
--    plan. Tout travail sur le transfert et le partage d'un pass restait sans effet sur un pass
--    vendu, puisque l'attribut etait ecrase a l'achat.
--
-- 2. subscription_pass ne portait pas de cle d'idempotence, la ou reservation et paiement en ont
--    une. Un double envoi de la demande d'achat produisait deux pass et deux factures.
--
-- Les valeurs par defaut reprennent le comportement actuel, donc la migration ne change rien a
-- elle seule : un plan existant reste non transferable et non partageable tant que personne ne l'a
-- decide.

ALTER TABLE pass_plan_version
    ADD COLUMN IF NOT EXISTS transferable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS shareable BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN pass_plan_version.transferable IS
    'Le pass vendu depuis ce plan peut-il changer de titulaire';
COMMENT ON COLUMN pass_plan_version.shareable IS
    'Le pass vendu depuis ce plan peut-il etre utilise par plusieurs beneficiaires';

ALTER TABLE subscription_pass
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_pass_idempotency_key
    ON subscription_pass (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON COLUMN subscription_pass.idempotency_key IS
    'Cle fournie par l appelant · un second envoi de la meme demande rend le pass deja cree';
