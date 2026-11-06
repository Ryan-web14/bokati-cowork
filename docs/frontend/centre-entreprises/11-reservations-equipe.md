# Réservation par l'équipe et créneaux du jour même

Préfixe des routes : `/sni/api/v1`.

## Dérogation au préavis

Un membre de l'équipe (`ROLE_ADMIN`, `ROLE_SUPER_ADMIN`, `ROLE_MANAGER`, `ROLE_OPERATIONS_AGENT`) qui
réserve depuis l'administration (pas depuis le portail client) n'est pas tenu au préavis de la
ressource :

- il peut réserver dans l'heure qui court ou une minute avant le début ;
- il peut prendre des créneaux déjà passés et restés libres, pour saisir après coup la réservation
  d'un client qui n'a pas pu la faire en ligne, jusqu'à 31 jours en arrière
  (`bokati.booking.staff.max-backdate-days`) ;
- la recherche de créneaux (`/resource-availabilities/remaining-slots`, `/bookings/check-availability`)
  les lui propose ; un créneau désactivé à la main ne l'est jamais, seul un créneau fermé par le
  temps sans avoir été pris ;
- les durées minimale et maximale de la politique s'appliquent toujours, ainsi que les plafonds de
  nombre de réservations ; l'approbation n'est pas demandée ;
- les politiques qui l'interdisent (`staffOverrideAllowed = false` : la stricte et l'événementielle)
  gardent leur préavis pour tout le monde.

Selon l'heure de début, une réservation de l'équipe suit l'un de ces chemins :

| Plage | Effet à la création |
| --- | --- |
| Entièrement passée | confirmée et close (`COMPLETED`) aussitôt, facture émise : il ne reste que le paiement |
| Commencée, pas finie | confirmée et en cours (`IN_PROGRESS`) |
| Pas encore commencée | chemin habituel (paiement attendu, confirmation), sans préavis |

`policyOverrideBy` sur la réservation nomme le membre de l'équipe qui a dérogé ; la confirmation,
même déclenchée plus tard par le paiement, respecte la dérogation.

## Créneaux du jour même

La recherche de créneaux ne juge plus le préavis sur le début de la plage demandée, mais créneau
par créneau : demander la journée en cours renvoie les créneaux encore réservables de
l'après-midi, au lieu d'une liste vide dès que le matin est passé.

## Vérification de disponibilité

`POST /bookings/check-availability` juge comme la réservation qui suivra. Pour un membre de l'équipe,
un créneau hors préavis ou déjà passé répond `available: true`, et non plus une erreur « l'heure
de début est dans le passé » :

| Cas | `available` | `message` |
| --- | --- | --- |
| Membre de l'équipe, créneau hors préavis ou passé | `true` | `Disponible · réservation de l'équipe hors préavis` |
| Cas habituel | `true` | `Disponible` |
| Politique stricte ou événementielle, ou réservation depuis le portail | selon le préavis | le motif du refus |
