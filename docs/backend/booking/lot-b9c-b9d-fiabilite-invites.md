# Lots B9c et B9d : fiabilité et acompte, invités et demandes

> Partie du [plan booking](README.md). Faits le 19/11/2026 sur `feature/booking`. Décision D7. Migrations V394 et V395 (prod V388 et V389).

## B9c. Fiabilité et acompte

- **Score de fiabilité** sur 100, calculé sur `bookings.reliability.window-days` (180) jours : moins `bookings.reliability.no-show-penalty` (20) par absence, moins `bookings.reliability.late-cancel-penalty` (10) par annulation tardive (moins de `bookings.reliability.late-cancel-hours`, 24 h, avant le début). `GET /bookings/pay-on-site/reliability/{ownerType}/{ownerCode}` → `{ score, bookings, noShows, lateCancellations, windowDays, threshold, guaranteeRequired, guarantee, depositRate }`.
- Sous `bookings.reliability.threshold` (60 ; 0 : jamais), une garantie est imposée selon `bookings.reliability.mode` :
  - `DEPOSIT` (défaut) : une réservation en paiement direct demande un **acompte** de `bookings.deposit.rate` (30 %) du total (`depositAmount`) ;
  - `PREPAY` : pas de rendez-vous réglé sur place au portail (`409 PREPAYMENT_REQUIRED`) ; paiement complet avant confirmation.
- **Acompte demandé par l'équipe** : `CreateBookingRequest.depositRate` (1 à 100 %, paiement direct).
- **Confirmation à l'acompte** : quand un paiement partiel atteint l'acompte (chaîne de paiement), la réservation est confirmée (`depositPaidAt`), le reste se règle sur place : statut de règlement « En attente de paiement » jusqu'au solde. Le paiement complet la marque réglée comme avant.

## B9d. Invités et demandes

- **Invitations** : `POST /bookings/{numéro}/participants/{id}/invite` (ou `POST /bookings/{numéro}/participants/invite` pour tous ceux qui n'ont pas répondu) envoie un courriel avec deux liens à jeton propre à l'invité : « Je participe » et « Je ne participe pas » (`GET /public/bookings/invitations/{jeton}?answer=ACCEPT|DECLINE`, page de remerciement). Le participant passe `CONFIRMED` ou `DECLINED` (`respondedAt`). Événements `BOOKING_PARTICIPANT_INVITED`, `BOOKING_PARTICIPANT_ANSWERED`.
- **Pointage des invités** : `PATCH /bookings/{numéro}/participants/{id}/check-in` (`checkedInAt`), réservation confirmée ou en cours. Événement `BOOKING_PARTICIPANT_CHECKED_IN`.
- **Détails** : `PATCH /bookings/{numéro}/details` `{ roomLayout, specialRequests, internalNotes, tags }`, seuls les champs donnés changent :
  - `roomLayout` : liste souple `BOOKING_ROOM_LAYOUT` (table de conseil, en U, salle de classe, théâtre, cabaret) ;
  - `specialRequests` (1 000 caractères) ;
  - `internalNotes` (2 000) : notes de l'équipe, jamais montrées au portail ;
  - `tags` : jusqu'à 10 étiquettes, liste souple `BOOKING_TAG` (client important, événement, à rappeler, première visite).
- **Modification autonome au portail** : `POST /client/bookings/{numéro}/reschedule` `{ newStartedAt, newEndedAt, acceptPriceChange }` et l'aperçu `GET /client/bookings/{numéro}/reschedule/preview` : le membre déplace sa réservation dans la politique de la ressource (préavis, durée, capacité) ; une hausse de prix demande `acceptPriceChange` (lot B2b).

## Tests

`BookingB9cTest` (score, acompte, prépaiement imposé, arrondi) ; `BookingB9dTest` (invitation et réponse, pointage, détails par listes souples).
