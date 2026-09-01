-- =====================================================================================
-- Duree de creneau portee par la ressource
--
-- Le moteur de disponibilite raisonnait sur une constante de 30 minutes, dupliquee dans
-- ResourceAvailabilityServiceImpl et BookingResourceGuard. La duree devient une propriete de
-- la RESSOURCE, et non de chaque disponibilite.
--
-- Pourquoi la ressource et non la disponibilite · le moteur compose une fenetre reservable a
-- partir de plusieurs creneaux contigus (requiredSlots = duree / duree_de_creneau). Des
-- creneaux de durees differentes sur une meme ressource fausseraient ce decompte, et
-- silencieusement : on renverrait des fenetres de la mauvaise longueur, pas une erreur.
-- resource_availability.slot_duration_minutes reste rempli, comme copie figee de la valeur au
-- moment de la creation.
--
-- Rien ne change pour l'existant : le defaut reprend la valeur en vigueur, 30.
-- =====================================================================================

ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS slot_duration_minutes INTEGER NOT NULL DEFAULT 30;


-- -------------------------------------------------------------------------------------
-- Seuls les diviseurs de 60 sont admis.
--
-- Une duree qui ne divise pas 60 casse l'alignement des creneaux sur l'heure, que
-- BookingResourceGuard verifie deja (startedAt.getMinute() % duree == 0) : une duree de 7 ou
-- de 45 minutes rendrait la moitie des heures de la journee inaccessibles a la reservation,
-- sans que rien ne l'explique.
--
-- Pose en NOT VALID · la contrainte s'applique aux ecritures nouvelles sans faire echouer le
-- deploiement sur une ligne ancienne. Toutes valent 30 aujourd'hui, mais la garantie tient
-- sans avoir a le supposer.
-- -------------------------------------------------------------------------------------
ALTER TABLE resource
    ADD CONSTRAINT ck_resource_slot_duration_divides_60
        CHECK (slot_duration_minutes > 0 AND 60 % slot_duration_minutes = 0) NOT VALID;


-- -------------------------------------------------------------------------------------
-- Signale un parc heterogene · une ressource dont les creneaux futurs n'ont pas tous la meme
-- duree produirait des fenetres de longueur fausse. Le cas ne devrait pas exister avant cette
-- migration, tout valant 30 ; on le verifie plutot que de le supposer.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    heterogenes INTEGER;
BEGIN
    SELECT COUNT(*) INTO heterogenes FROM (
        SELECT resource_id
        FROM resource_availability
        WHERE ended_at > NOW()
        GROUP BY resource_id
        HAVING COUNT(DISTINCT slot_duration_minutes) > 1) d;

    IF heterogenes > 0 THEN
        RAISE WARNING 'V210 : % ressource(s) ont des creneaux futurs de durees differentes · les fenetres calculees pour elles seront fausses tant que ces creneaux coexistent.', heterogenes;
    END IF;
END $$;
