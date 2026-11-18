# Lot R5a : calendrier et règles de réservation

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`. Migration V376 (prod V370).

## Battement entre réservations

Politique : `bufferMinutes`. Pour une ressource louée entière (salle), une réservation est refusée si un créneau du battement, avant ou après, est occupé (ménage, installation). La recherche de disponibilités ne propose plus ces fenêtres. L'équipe (dérogation de la politique) peut passer outre.

## Avance maximale

Politique : `maxAdvanceDays`. Réservation et recherche refusées au-delà (vide : l'horizon des créneaux, 90 jours).

## Blocages typés et fermetures répétées

Fermeture (`POST /resource-closures`) :

| Champ | Valeurs |
| --- | --- |
| `kind` | `CLOSURE` (défaut), `MAINTENANCE`, `CLEANING`, `PRIVATE_EVENT`, `OTHER` |
| `recurrence` | `NONE` (défaut), `WEEKLY`, `YEARLY` |
| `recurrenceUntil` | dernière date, vide = sans fin |

- Une fermeture hebdomadaire dure au plus une semaine, une annuelle au plus un an.
- Les occurrences à venir (400 jours) rendent leurs créneaux indisponibles ; les réservations touchées suivent la règle du lot R2 (refus, ou `cancelBookings=true`).
- À l'ouverture des créneaux, une fermeture répétée ne refuse pas la journée : les créneaux qu'elle couvre sont créés indisponibles. Une fermeture unique refuse toujours la plage, comme avant.
- Réponse : `kind`, `recurrence`, `recurrenceUntil` en plus.

## Jours fériés et horaires exceptionnels

- Jours fériés nationaux du Congo calculés, sans saisie : 1er janvier, lundi de Pâques, 1er mai, Ascension, lundi de Pentecôte, 10 juin, 15 août, 1er novembre, 28 novembre, 25 décembre. Paramètre `app.resources.holidays.national` (vrai par défaut).
- Horaires des jours fériés : paramètre `app.resources.holidays.hours`. `NORMAL` (défaut) garde la semaine type ; `CLOSED` ferme ; des plages `HH:mm-HH:mm,HH:mm-HH:mm` les remplacent. **À choisir par l'exploitant.**
- Jours particuliers, `/resource-calendar-days` :
  - `GET ?from=&to=&resourceCode=` : fériés nationaux (`national: true`, sans identifiant) et jours saisis, généraux et de la ressource.
  - `POST` `{ date, resourceCode?, kind, label?, openingHours? }`, où `kind` vaut `HOLIDAY` (férié ajouté), `SPECIAL_HOURS` (plages obligatoires) ou `CLOSED`. Rend `201`.
  - `DELETE /{id}`.
- Ordre d'application pour une date : jour de la ressource, jour général, férié national, semaine type. Un jour `SPECIAL_HOURS` général neutralise un férié national.
- Pris en compte par l'ouverture quotidienne des créneaux, la création manuelle (dans les horaires du jour) et le calendrier : `dayLabel` sur chaque jour des calendriers public et équipe ; le calendrier de l'équipe rend aussi `blocks` (fermetures et occurrences de la plage, avec leur nature).

## Prix

Règle tarifaire : `holidayMode` = `ONLY` (jours fériés seulement, par exemple une majoration) ou `EXCLUDED` (hors jours fériés), vide = tous les jours. Le moteur de prix lit les fériés nationaux et ceux ajoutés par l'équipe.

## Tests

`ResourceR5aTest` : fériés calculés (Pâques 2026), répétition hebdomadaire bornée, fermeture unique, horaires exceptionnels et jour fermé, règle des jours fériés, battement d'une salle.
