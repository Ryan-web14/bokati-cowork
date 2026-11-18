# Lot B2b : reports, écart de prix, annulation, options

> Demande du 18/11/2026, après B2. Migration V386 (prod V380).

## Reports et changements de ressource

- **Aperçu avant de valider** :
  - `GET /bookings/{numéro}/reschedule/preview?newStartedAt=&newEndedAt=` ;
  - `GET /bookings/{numéro}/change-resource/preview?newResourceCode=`.

  Ils rendent `{ bookingNumber, currentAmount, newAmount, difference, change, currency, invoiced, modificationCount, lastModifiedAt, lastModification }`. `change` vaut `INCREASE` (plus cher), `DECREASE` (moins cher) ou `SAME`. Les remises acquises, l'unité imposée et la couverture partielle sont prises en compte.
- **Plus cher** : le report ou le changement est refusé (409 `PRICE_INCREASE_CONFIRMATION_REQUIRED`, message avec l'ancien prix, le nouveau et l'écart) tant que `acceptPriceChange: true` n'est pas envoyé dans `PATCH .../reschedule` ou `.../change-resource`. Une fois confirmé, si la réservation est facturée, un complément est émis et la réservation n'est plus marquée réglée (`paidAt` vidé) : le complément est à encaisser.
- **Moins cher** : passe sans confirmation ; un avoir suit si la réservation est facturée.
- Réservation payée par abonnement ou pass, sans couverture partielle : pas de confirmation.

## Marquage et information

- La réservation porte `modificationCount`, `lastModifiedAt` et `lastModification` (« Report au … · +5000 XAF (pièce FAC-…) »), rendus dans sa réponse. L'écran affiche un badge « Modifiée N fois » et la dernière modification.
- **Client** : averti comme avant à chaque report ou changement (courriel, sauf `sendEmail: false`), avec l'historique et l'événement.
- **Équipe (admin)** : à chaque report ou changement, une alerte en direct `BOOKING_MODIFIED` dans le back-office (module `BOOKING`, sévérité `WARNING` si un complément est à encaisser, `INFO` sinon), avec le numéro, le nombre de modifications, l'écart et la pièce. L'agent qui a créé la réservation reçoit aussi une notification dans l'appli (`/admin/notifications/unread`), sauf s'il est l'auteur de la modification.

## Annulation

La politique d'annulation de la ressource (délai, grille de remboursement) s'applique à tous, équipe comprise. Une réservation encore en attente (paiement ou approbation) reste annulable sans condition.

## Options à la création

`CreateBookingRequest.options` : `[{ kind: "SERVICE" | "AMENITY", code, quantity }]`. Les services du catalogue (café, traiteur, impression, matériel) et les équipements optionnels de la ressource sont ajoutés dès la création, sur la même facture que la réservation. Un autre `kind` est refusé (400).

## Canal

`CreateBookingRequest.channel` (PHONE, DESK, EMAIL, WHATSAPP) : précisé par l'équipe, par exemple pour une réservation prise au téléphone. Ignoré pour un client du portail.

## Tests

`BookingB2bTest` : hausse refusée sans confirmation puis acceptée, baisse sans confirmation, modification marquée avec complément à encaisser.
