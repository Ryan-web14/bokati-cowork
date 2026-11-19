# Lot B4 : ajustement du prix à la réservation

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Décision D4. Migration V390 (prod V384). Demande 3.

## Tarif préférentiel

Déjà en place depuis le lot B10 : une règle tarifaire de la ressource peut viser des clients désignés (membres, clients, entreprises) ou un segment (`audience`) ; le moteur l'applique à ces clients seulement. Rien de plus dans ce lot.

## Ajustement manuel

- À la création (`CreateBookingRequest.priceAdjustment`) ou ensuite (`POST /bookings/{numéro}/price-adjustment`), même corps : `{ discountRate | newAmount, reasonCode, reasonText }`. Un seul de `discountRate` (0,01 à 100 %) ou `newAmount` (nouveau total). Seulement une baisse ; une hausse passe par l'unité ou la grille.
- **Motif obligatoire** : liste `BOOKING_PRICE_ADJUSTMENT_REASON` (client fidèle, geste commercial, compensation d'un incident, tarif négocié, erreur de grille, partenaire, autre avec précision), modifiable par `/reference-lists`.
- Réservé à l'équipe (le portail ne l'envoie pas ; sinon `403`).
- **Plafond** : le titulaire du droit `BOOKING:PRICE_ADJUST` (SUPER_ADMIN, ADMIN, MANAGER) ajuste directement jusqu'à `bookings.price-adjustment.max-rate` (20 %).
- **Au-delà, ou sans ce droit** : demande au moteur d'approbation, centre `BOOKING`, type `PRICE_ADJUSTMENT`, circuit `BOOKING_PRICE_ADJUSTMENT` (N+1, droit `BOOKING:PRICE_APPROVE`). `subjectType` `BOOKING`, `subjectRef` le numéro de réservation, `amount` le nouveau total, `detailsJson` : `bookingNumber`, `resourceCode`, `owner`, `contactName`, `startedAt`, `currency`, `amountBefore`, `amountAfter`, `discount`, `rate`, `reasonCode`, `reason`.
  - À la création, la réservation attend en `PENDING_APPROVAL` (créneau retenu) ; `approvalForPriceOnly` note qu'elle n'attend que pour son prix.
  - **Accord** : remise appliquée, puis la réservation qui n'attendait que pour son prix est approuvée et suit son cours (paiement ou confirmation).
  - **Refus** (ou renvoi) : prix calculé gardé, `priceAdjustmentStatus: REJECTED`, et la réservation suit son cours au prix calculé.
- Application : ligne `DISCOUNT` (code `MANUAL`), montant baissé si rien n'est facturé, sinon avoir sur la facture (même mécanique que le code promo), événement `BOOKING_PRICE_ADJUSTED`.

## Trace

- Réservation et réponse : `originalAmount`, `priceAdjustmentAmount`, `priceAdjustmentRate`, `priceAdjustmentReason`, `priceAdjustmentReasonCode`, `priceAdjustmentBy` (session), `priceAdjustmentStatus` (`APPLIED`, `PENDING`, `REJECTED`), `priceAdjustmentApprovalNumber`.
- Facture : le libellé de la ligne de réservation porte « Prix ajusté de X à Y XAF (motif) ». Le libellé commence désormais par « Réservation » (avant : « Booking »).
- Un seul ajustement par réservation (`409` si un ajustement est déjà appliqué ou en attente).

## Tests

`BookingB4Test` : plafond avec le droit, approbation sans le droit ou au-delà, refus d'une hausse, demande soumise puis accord (remise et approbation de la réservation), refus (prix gardé, réservation qui continue).
