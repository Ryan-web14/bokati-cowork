-- Reservation le jour meme.
--
-- Le code deleguait deja entierement la question a la politique de la ressource : la creation
-- d'une reservation la consulte par BookingResourceGuard.validatePolicy, et la liste des creneaux
-- disponibles par ResourceAvailabilityServiceImpl.respectsBookingNotice. Une seule source de
-- verite, aucune regle cachee sur la date.
--
-- Ce qui interdisait le jour meme etait donc la donnee, pas la logique. La politique semee sous
-- le nom « Politique Standard », appliquee par defaut a la majorite des espaces, exigeait 1440
-- minutes de preavis. Vingt-quatre heures de preavis excluent mecaniquement toute la journee en
-- cours : aucune reservation ne passait, et aucun creneau du jour ne s'affichait, puisque les deux
-- controles lisent la meme valeur.
--
-- Les politiques dont le long preavis est la raison d'etre ne sont pas touchees · « Stricte » et
-- « Evenementielle » sont choisies pour cela.
--
-- Chaque mise a jour est gardee par la valeur semee d'origine : une politique deja ajustee par un
-- administrateur garde son reglage.

UPDATE resource_policy
SET min_booking_notice_minutes = 60,
    description = 'Politique de reservation standard pour la majorite des espaces : duree de 30 min a 4h, preavis de 1h, annulation possible sous 12h avant la reservation',
    updated_at = NOW()
WHERE code = 'RPL-00001'
  AND min_booking_notice_minutes = 1440;

UPDATE resource_policy
SET min_booking_notice_minutes = 120,
    description = 'Politique demi-journee : duree de 3h a 5h, preavis de 2h, annulation possible 24h avant la reservation',
    updated_at = NOW()
WHERE code = 'RPL-00004'
  AND min_booking_notice_minutes = 1440;

UPDATE resource_policy
SET min_booking_notice_minutes = 240,
    description = 'Politique journee complete : duree de 7h a 10h, preavis de 4h, annulation possible 48h avant la reservation',
    updated_at = NOW()
WHERE code = 'RPL-00005'
  AND min_booking_notice_minutes = 2880;
