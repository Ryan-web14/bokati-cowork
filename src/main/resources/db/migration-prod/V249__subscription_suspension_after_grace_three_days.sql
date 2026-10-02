-- La suspension vient trois jours apres l entree en tolerance, pas deux semaines.
--
-- Avec une tolerance d un jour, quatorze jours de suspension laissaient deux semaines de droits
-- ouverts sur une echeance impayee · le rythme des deux reglages n allait plus ensemble.

UPDATE subscription_policy SET suspension_after_grace_days = 3 WHERE suspension_after_grace_days <> 3;
