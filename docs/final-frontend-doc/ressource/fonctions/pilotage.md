# Pilotage : coûts, marges, occupation

> Fiche du module [ressource](../README.md), d'après le lot R5e du [plan ressources](../../../backend/ressources/README.md) (18/11/2026). Routes exactes et champs JSON : [routes.md](../routes.md).

## Coûts des ressources

- `GET|POST /resources/{code}/costs`, `DELETE /resource-costs/{id}` : `{ kind, basis, amount, label, validFrom, validUntil }`.
  - `kind` : `RENT`, `CLEANING`, `ENERGY`, `DEPRECIATION`, `SERVICES` ou `OTHER`.
  - `basis` : `MONTHLY`, `PER_BOOKING` ou `PER_HOUR` (heure réservée).
- Droits : lecture en `REPORT:VIEW`, saisie en `RESOURCE:UPDATE`.

## Marges

- `GET /resource-analytics/margins?from=&to=&resourceCode=` (mois en cours par défaut) : par ressource, réservations, minutes, chiffre d'affaires, coûts (par nature), marge et taux de marge, plus les totaux.
  - Un coût mensuel est réparti jour par jour (mois de longueur réelle) sur sa validité.
  - Un coût par réservation compte chaque réservation ; un coût horaire compte les heures réservées.
- `GET /resource-analytics/bookings/{numéro}/margin` : marge d'une réservation. Les coûts mensuels y comptent au prorata de ses minutes sur les minutes ouvertes du mois.

Chiffre d'affaires d'une réservation : son montant (remises déduites) plus ses suppléments (services, options, participants, dépassement, non-présentation), sans la caution. Réservations comptées : confirmées, en cours, terminées, non présentées.

## Historique des prix

- `GET /resources/{code}/price-history?from=&to=` (trois mois avant et après par défaut, 1 000 au plus) : pour chaque réservation, unité, prix unitaire, sous-total, total et règles appliquées, lues dans la trace du prix (lot R1).
- Les versions des règles (photo à chaque changement) sont à `GET /resource-pricing-rules/{id}/history` (lot R1).

## Occupation et chiffre d'affaires

`GET /resource-analytics/occupancy?from=&to=&groupBy=RESOURCE|TYPE|GROUP|SITE&siteCode=` (droit `REPORT:VIEW`). Pour chaque ligne : minutes ouvertes (créneaux actifs) et réservées, taux d'occupation, la même chose en heures pleines et en heures creuses, réservations et chiffre d'affaires. Le tri va du plus occupé au moins occupé.

- Une salle louée entière compte une place ; un espace partagé compte ses postes (quantité réservée sur capacité du créneau).
- Heures pleines : paramètre `app.resources.peak-hours` (`09:00-12:00,14:00-17:00` par défaut), du lundi au vendredi ; le reste est en heures creuses.
