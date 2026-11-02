-- Reservation par l'equipe (administration, gerance, agents des operations) · creneaux du jour ou
-- deja passes, hors preavis de la politique, sauf pour les politiques qui l'interdisent.

-- La politique admet-elle qu'un membre de l'equipe passe outre son preavis · vrai par defaut.
ALTER TABLE resource_policy ADD COLUMN IF NOT EXISTS staff_override_allowed BOOLEAN NOT NULL DEFAULT TRUE;
-- La stricte et l'evenementielle gardent leur preavis, quel que soit celui qui reserve.
UPDATE resource_policy SET staff_override_allowed = FALSE
WHERE lower(name) LIKE '%strict%' OR lower(name) LIKE '%evenement%' OR lower(name) LIKE '%événement%';

-- Un creneau passe sans etre pris est ferme par le temps, pas desactive a la main · la difference
-- dit s'il peut encore servir a une reservation saisie apres coup par l'equipe.
ALTER TABLE resource_availability ADD COLUMN IF NOT EXISTS expired_at TIMESTAMPTZ;
UPDATE resource_availability SET expired_at = ended_at
WHERE expired_at IS NULL AND ended_at <= NOW() AND active = FALSE AND available = FALSE;

-- Qui, dans l'equipe, a reserve hors preavis · la confirmation et la cloture en tiennent compte.
ALTER TABLE booking ADD COLUMN IF NOT EXISTS policy_override_by VARCHAR(150);
