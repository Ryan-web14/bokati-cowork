# Lots B9a et B9b : venue, planning du jour, séries

> Partie du [plan booking](README.md). Faits le 19/11/2026 sur `feature/booking`. Décision D7. Migration V393 (prod V387).

## B9a. Rappels et venue

- Le courriel de rappel (lot B6, la veille et 2 h avant) porte deux liens à jeton (le jeton de la réservation, comme la page de confirmation) :
  - **Je viens** : `GET /public/bookings/{numéro}/attendance?token=…&answer=COMING` enregistre la venue (`attendanceAnswer: COMING`, `attendanceAnsweredAt`, événement `BOOKING_ATTENDANCE_CONFIRMED`) et affiche une page de remerciement ;
  - **J'annule** : `GET …&answer=CANCEL` affiche une page de confirmation ; le bouton envoie `POST /public/bookings/{numéro}/attendance/cancel` (jeton en formulaire), qui annule par la voie ordinaire, politique d'annulation de la ressource comprise (`attendanceAnswer: CANCELLED`). Une confirmation en deux temps évite qu'un aperçu automatique du lien n'annule la réservation.
  - Jeton faux : page « action impossible » ; réservation qui n'est plus confirmée ni en attente de paiement : même page, avec la raison.
- **Libération anticipée** : `bookings.early-release.minutes` (0 : coupée, par défaut). Une réservation confirmée sans arrivée ni démarrage tant de minutes après son début (au moins la fenêtre d'arrivée de 15 minutes) est marquée absente (frais de non-présentation de la politique, droit consommé) et son créneau rendu (`releasedEarlyAt`, événement `BOOKING_EARLY_RELEASED`). Un client qui a répondu « je viens » n'est pas libéré. Passe avec la tâche du cycle de vie (toutes les minutes).
- WhatsApp de rappel : le modèle reste à créer et faire approuver par Meta ; tant qu'il ne l'est pas, le rappel part par courriel seulement.

## B9b. Planning et séries

- **Planning du jour** : `GET /bookings/planning?date=AAAA-MM-JJ&resourceCode=` (aujourd'hui par défaut) → `{ date, bookings, arrived, late, toCollect, resources: [{ resourceCode, resourceName, bookings: [{ bookingNumber, contactName, contactPhone, startedAt, endedAt, status, statusLabel, checkedInAt, late, lateMinutes, paymentStatus, payOnSite, attendanceAnswer, quantity }] }] }`. Retard : réservation confirmée dont le début est passé sans arrivée. Réservations annulées, refusées, expirées et brouillons exclues.
- **Séries récurrentes** :
  - `GET /bookings/series/{recurrenceGroupNumber}` : les occurrences (liste allégée) ;
  - `PATCH /bookings/series/{n}/cancel?from=AAAA-MM-JJ` `{ reason }` : annule les occurrences à partir de la date (toutes les occurrences à venir sans date) ;
  - `PATCH /bookings/series/{n}/reschedule?from=` `{ newStartTime, newEndTime }` (nouvel horaire le même jour) ou `{ shiftDays }` (décalage), `acceptPriceChange` facultatif.
  - Chaque occurrence suit ses règles (politique d'annulation, capacité, prix, lot B2b) ; réponse `{ recurrenceGroupNumber, changed, skipped, changedBookings, refused: { numéro: raison } }` : une occurrence refusée n'arrête pas les autres ; les occurrences déjà closes sont comptées dans `skipped`.

## Tests

`BookingB9abTest` : liens du rappel (jeton contrôlé, venue notée, annulation par la voie ordinaire), planning (regroupement, retard, exclusions), annulation d'une série avec refus rapporté.
