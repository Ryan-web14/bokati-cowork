# Lot B9f : remplissage

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Décision D7. Pas de migration.

## Offres de dernière minute

- Réglages : `bookings.last-minute.discount-rate` (remise en %, 0 : coupée, par défaut) et `bookings.last-minute.hours` (24).
- Une réservation **du portail** qui commence dans la fenêtre reçoit la remise d'elle-même, avant la facture (ligne de remise « Offre de dernière minute (-N %) »).
- Portail : `GET /client/bookings/last-minute?durationMinutes=60` → un créneau libre par ressource visible dans la fenêtre : `{ resourceCode, resourceName, startedAt, endedAt, price, discountedPrice, discountRate, currency }`, le plus proche d'abord. Vide si l'offre est coupée.
- Avis WhatsApp aux membres volontaires : modèle à créer et faire approuver par Meta, non branché.

## Alternatives

Portail : `GET /client/bookings/alternatives?resourceCode=&startedAt=&endedAt=&quantity=` → les fenêtres libres de même durée les plus proches (même moteur que `POST /bookings/suggestions` de l'équipe). À proposer quand la vérification de disponibilité répond `available: false`.

## Prolongation en un clic

- Équipe : `POST /bookings/{numéro}/extend` `{ minutes, acceptPriceChange }` ; portail : `POST /client/bookings/{numéro}/extend` (mêmes champs).
- Réservation confirmée **ou en cours** ; 1 à 1 440 minutes. Seule la plage ajoutée est contrôlée (ouverture, alignement, capacité) : une réservation en cours se prolonge malgré son début passé.
- Prix recalculé (lot B2b) : plus cher, `409 PRICE_INCREASE_CONFIRMATION_REQUIRED` sans `acceptPriceChange: true` ; facturée, un complément est à encaisser. Droit d'abonnement repris sur la nouvelle durée. Événement `BOOKING_EXTENDED`.
- Renouvellement des locations au mois : passe par les abonnements (hors réservation).

## Tests

`BookingB9fTest` : remise de dernière minute au portail seulement et dans la fenêtre, coupée par défaut ; prolongation bornée et passée au service.
