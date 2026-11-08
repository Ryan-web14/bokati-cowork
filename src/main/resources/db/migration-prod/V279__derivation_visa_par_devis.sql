-- Un devis accepte vaut visa de sa derivation, meme quand l'admin qui l'accepte l'a prepare
-- (decision du 2026-11-08). La regle des quatre yeux reste entiere pour les derivations posees
-- directement sur un abonnement : seule une approbation de source QUOTE y echappe.

ALTER TABLE plan_derivation ADD COLUMN IF NOT EXISTS approval_source VARCHAR(20);

ALTER TABLE plan_derivation DROP CONSTRAINT IF EXISTS ck_plan_derivation_four_eyes;
ALTER TABLE plan_derivation ADD CONSTRAINT ck_plan_derivation_four_eyes
    CHECK (approved_by IS NULL OR approved_by <> requested_by OR approval_source = 'QUOTE');
