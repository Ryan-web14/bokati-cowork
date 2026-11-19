# Lot B6 : notifications et liste d'attente

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Écarts 5 (rappels), 6, Bn-15, Bn-16, Bn-17 ; Bn-25 pour la liste d'attente. Migration V392 (prod V386).

## Rappels automatiques (écarts 5, Bn-16)

- Nouvelle tâche `BookingReminderWorker` (toutes les 5 minutes, `bokati.booking.reminders.worker-delay-ms`) : prévoit les rappels des réservations confirmées à venir qui ont un courriel de contact, puis envoie ceux dont l'heure est venue. Avant, la file ne partait que sur `POST /bookings/notifications/dispatch-due`.
- Délais : `bookings.reminders.offsets-minutes` (`1440,120` : la veille et 2 h avant) ; `bookings.reminders.enabled` (vrai). Un rappel n'est jamais prévu deux fois à la même heure ; un délai déjà passé est sauté.
- Un rappel dont la réservation n'est plus confirmée ni en cours (annulée, expirée, absente) est annulé au lieu d'être envoyé.
- Texte en français (« Rappel : votre réservation … commence le … ») ; préférences du membre respectées comme avant.
- `POST /bookings/{numéro}/notifications/reminder` : `400 RAPPEL_SANS_COURRIEL` sans courriel de contact (avant : `200` avec 0) ; `409` si la réservation n'est pas confirmée.
- WhatsApp : avec le lot B9a (« je viens » ou « j'annule »), modèle à faire approuver par Meta.

## Réservations en attente (écart Bn-17)

À la création d'une réservation en attente de paiement ou d'approbation, le courriel part au contact (sauf `sendEmail: false`) : « Réservation enregistrée, paiement attendu » avec le montant et **l'échéance de paiement** (« À régler avant le … : sans paiement, le créneau sera libéré »), ou « Demande de réservation reçue ». Avant, aucun courriel avant la confirmation.

## Liste d'attente refaite (écarts 6, Bn-15)

- Seuls les créneaux à venir sont proposés ; une inscription dont le créneau est passé expire.
- Une place est proposée quand le contrôle de disponibilité complet la donne (capacité, fermetures, politique), et non plus « aucune réservation sur la plage ».
- **Une seule offre à la fois par créneau** : la plus ancienne inscription, pendant `bokati.booking.waitlist.offer-ttl-minutes` (30). Déclinée ou expirée, la place passe à la suivante au passage suivant (5 minutes) ou tout de suite après un refus.
- **Courriel à l'offre** au contact (`notifiedAt`).
- **Acceptation** : `POST /bookings/waitlist/{id}/accept` (équipe) ou `POST /client/bookings/waitlist/{id}/accept` (portail) crée la réservation pour le titulaire de l'inscription (clé d'idempotence `WAITLIST-{id}`, mêmes règles qu'une réservation) ; l'inscription passe `BOOKED` avec `bookingNumber`. Offre échue : `409 WAITLIST_OFFER_EXPIRED` ; pas d'offre ouverte : `409 WAITLIST_OFFER_NOT_OPEN`.
- **Refus** : `POST .../{id}/decline`.
- Liste de l'équipe `GET /bookings/waitlist?status=&resourceCode=&ownerType=&ownerCode=&page=&size=` : **paginée** (`{ data, pageable }`, 100 au plus), tous états sans `status` (avant : liste des 100 premières `WAITING`). Alternatives calculées seulement pour les inscriptions en attente.
- Type `BUSINESS_ENTITY` pris en compte dans les alternatives (avant : `BUSINESS`, jamais vrai).
- **Portail** : `POST /client/bookings/waitlist` `{ resourceCode, startedAt, endedAt, quantity, paymentMode }`, `GET /client/bookings/waitlist?status=` (mes inscriptions), `POST .../{id}/accept`, `POST .../{id}/decline`, `DELETE .../{id}`. L'inscription d'un autre est introuvable.

## Tests

`BookingB6Test` : offre à la plus ancienne inscription seulement avec courriel, créneau plein non proposé et inscription passée expirée, acceptation qui crée la réservation une fois, rappels prévus la veille et 2 h avant puis annulés pour une réservation annulée.
