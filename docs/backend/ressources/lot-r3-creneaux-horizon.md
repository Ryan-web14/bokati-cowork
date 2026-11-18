# Lot R3 : créneaux et horizon

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`.

## Horizon glissant (D5)

- `app.resources.booking-horizon-days` (90 par défaut) : la tâche d'ouverture (`MonthlyAvailabilityGenerator`) ouvre les créneaux de maintenant jusqu'à l'horizon, chaque jour à 03:00 (`bokati.resource.workers.monthly-availability-cron`, défaut passé de `0 0 3 1 * *` à `0 0 3 * * *`), plus au déploiement. Avant : le mois courant seulement, d'où trois jours de réservation possibles le 28.
- Un jour qui a déjà des créneaux (ouverts par la tâche ou par l'équipe) est sauté, les autres jours de la ressource sont ouverts. Avant : toute ressource ayant un créneau dans le mois était sautée en entier.
- Création manuelle : jusqu'à l'horizon (avant : un mois).

## Création selon les horaires

La création manuelle tient dans les horaires d'ouverture de la ressource ce jour-là (les siens, sinon les horaires globaux, soirées des abonnés comprises). Avant : 08:00-22:00 en dur. Dérogation de l'équipe : `outsideOpeningHours = true`.

## Gestion des créneaux

- `PATCH /resource-availabilities/{id}/active?active=` : désactiver (refusé si le créneau est réservé) ou réactiver un créneau à venir.
- `DELETE /resource-availabilities/{id}` : supprimer un créneau à venir sans réservation.
- `DELETE /resource-availabilities?resourceCode=&from=&to=` : supprimer les créneaux à venir sans réservation d'une ressource ; les créneaux réservés restent ; réponse `{ deleted, kept }`. Débloque le changement de durée de créneau.

## Lectures sans écriture

Les lectures (listes, vue groupée, fenêtres libres, réservation et libération internes) n'expirent plus les créneaux passés ; elles les excluent déjà. La tâche horaire (`SharedSlotCapacityWorker`) les clôt (`expirePastSlots`, par lots de 200).

## Opérations manuelles (D6)

`POST /resource-availabilities/reserve` et `PATCH /release` : motif obligatoire (`reason`), réservation concernée facultative (`bookingNumber`), journalisés. La libération manuelle ne rend jamais une place occupée par une réservation active : une salle louée entière occupée est refusée avec les numéros des réservations ; un poste partagé ne dépasse pas la capacité laissée par les réservations. La réservation et la libération internes (réservations, options) sont inchangées.

## Capacité de création

`capacity` n'est plus contrôlé à la création : la capacité d'un créneau suit toujours celle de la ressource (champ gardé pour ne pas casser les écrans qui l'envoient).

## Tests

`ResourceR3Test` (créneau réservé non supprimé, suppression groupée qui garde les réservés, motif obligatoire, salle occupée non libérée) ; `ResourceAvailabilityServiceImplTest` adapté (horaires d'ouverture, horizon de 90 jours, expiration par la tâche) ; `MonthlyAvailabilityGeneratorTest` adapté (horizon).
