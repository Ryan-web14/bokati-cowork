-- Les creneaux du soir, reserves aux membres abonnes.
--
-- Pose a la creation pour tout creneau qui commence a 18:00 ou plus tard. Le portail ne les
-- montre ni ne les laisse reserver a un membre sans abonnement actif ; l accueil peut les
-- reserver pour n importe qui. Les creneaux existants restent ouverts a tous.
ALTER TABLE resource_availability ADD COLUMN IF NOT EXISTS subscribers_only BOOLEAN NOT NULL DEFAULT FALSE;
