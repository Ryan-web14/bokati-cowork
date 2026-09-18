-- Une salle se loue entiere · la quantite n'y est qu'indicative.
--
-- Le modele traitait la quantite d'une reservation comme un nombre de places consommees, ce qui est
-- juste pour un open space et faux pour une salle. Trois consequences, toutes visibles.
--
-- 1. Reserver une salle de reunion de douze places pour six personnes etait refuse : le controle
--    comparait la quantite au nombre de reservations simultanees admises, qui vaut un pour une
--    salle. Six est superieur a un, donc refus.
--
-- 2. Le prix etait multiplie par la quantite. Une salle a vingt mille francs facturait cent vingt
--    mille pour six participants, alors qu'on loue la piece et non le siege.
--
-- 3. La reservation ne decrementait la disponibilite que de la quantite demandee. Six places prises
--    sur douze en laissaient six, donc quelqu'un d'autre pouvait reserver la meme salle au meme
--    creneau et s'y presenter.
--
-- Un type de ressource declare desormais si sa location occupe la ressource entiere. Dans ce cas la
-- quantite designe le nombre de participants, elle ne sert qu'a verifier qu'ils tiennent dans la
-- piece, et le creneau est pris en totalite.

ALTER TABLE resource_type
    ADD COLUMN IF NOT EXISTS whole_resource_booking BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN resource_type.whole_resource_booking IS
    'La location occupe la ressource entiere · la quantite designe alors des participants, pas des places consommees';

-- Les espaces qui se louent en entier. Un open space, un parking et un casier n'y figurent pas :
-- on y prend des places, une a la fois, et la quantite y garde tout son sens.
UPDATE resource_type
SET whole_resource_booking = TRUE,
    updated_at = NOW()
WHERE code IN (
    'RTY-00001',  -- Salle de reunion
    'RTY-00002',  -- Bureau privatif
    'RTY-00004',  -- Salle de formation
    'RTY-00005',  -- Cabine telephonique
    'RTY-00006'   -- Espace evenementiel
)
  AND whole_resource_booking = FALSE;

-- Une ressource louee en entier n'admet qu'une reservation a la fois. La valeur etait peut-etre
-- deja posee a la main ; on ne l'ecrase que lorsqu'elle est absente.
UPDATE resource_type
SET bookable_slots = 1,
    updated_at = NOW()
WHERE whole_resource_booking = TRUE
  AND bookable_slots IS NULL;
