# Lot B9g : suivi

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Décision D7. Migration V397 (prod V391).

## Tableau de bord

`GET /bookings/dashboard?from=&to=` (mois en cours par défaut ; dates de début des réservations) → `{ from, to, bookings, confirmed, completed, cancelled, lateCancellations, noShows, expired, noShowRate, averageLeadHours, revenue, channels: [{ channel, bookings, revenue }], topResources: [{ resourceCode, resourceName, bookings, revenue }] }`.

- `lateCancellations` : annulées moins de `bookings.reliability.late-cancel-hours` (24) heures avant le début.
- `noShowRate` : absences sur les réservations terminées ou absentes, en %.
- `averageLeadHours` : délai moyen entre la prise de la réservation et son début.
- `revenue` : montants des réservations confirmées, en cours, terminées ou absentes (hors suppléments).

## Avis après utilisation

- La note (1 à 5) arrive par le lien du courriel de clôture (lot B0, jeton) ; la page de remerciement propose maintenant un **commentaire** (`POST /public/bookings/csat/{numéro}/comment`, jeton en formulaire).
- La réservation porte `csatScore`, `csatComment`, `csatAt`.
- Note à `bookings.reviews.alert-threshold` (2) ou moins : alerte en direct `BOOKING_LOW_REVIEW` (module `BOOKING`, `WARNING`) avec la note, la ressource et le commentaire.
- Qualité par ressource : `GET /bookings/reviews?from=&to=&resourceCode=` (trois derniers mois par défaut) → `[{ resourceCode, resourceName, reviews, averageScore, lowScores }]`, la plus mal notée d'abord.

## Agenda personnel synchronisé

Coupé par défaut (`bookings.calendar-feed.enabled`). Activé :

- Portail : `GET /client/bookings/calendar-feed` (lien actuel, `204` sans lien), `POST` (crée ou renouvelle le lien : l'ancien cesse de marcher), `DELETE` (révoque). Réponse `{ memberId, url, createdAt }`.
- Le lien `GET /public/bookings/calendar/{jeton}.ics` rend les réservations du membre (du mois passé à venir, non annulées) au format iCalendar, à coller dans Google Agenda, Outlook ou le téléphone.

## Rapport de réservations par client

`GET /bookings/client-report.pdf?ownerType=&ownerCode=&from=&to=` (mois en cours par défaut) : PDF imprimable de toutes les réservations du client sur la période (numéro, date, horaire, espace, statut, durée, montant, règlement) avec les totaux (nombre, durée, montant, absences).

## Tests

`BookingB9gTest` : avis bas qui alerte, commentaire contrôlé par le jeton, agenda coupé par défaut puis calendrier ICS d'un membre.
