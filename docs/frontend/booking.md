# Guide Frontend/API - Booking

Base API: `/sni/api/v1`

Ce document decrit l'integration frontend du module booking. Il couvre la disponibilite, les reservations simples et recurrentes, les holds, les workflows d'approbation, le check-in/check-out, les participants, les policies, les quotas, les notifications, les metrics, les repair jobs et les integrations resource/subscription/payment.

## Regles importantes

### Champs generes par le backend

Ne jamais afficher ces champs en saisie libre dans les formulaires de creation. Ils sont generes et retournes par l'API.

- `bookingNumber`
- `holdNumber`
- `recurrenceGroupNumber`
- `policyNumber`
- `overrideNumber`
- `eventNumber`
- `notificationNumber`
- `billableNumber`

### Champs resolus automatiquement

Le frontend ne doit pas envoyer ces champs dans `CreateBookingRequest`, `CreateBookingHoldRequest` ou `BookingAvailabilityRequest`:

- `ownerType`
- `ownerCode`
- `subscriptionNumber`
- `passNumber`
- `entitlementCode`

Le frontend envoie uniquement un selecteur d'identite:

- `memberId`
- `customerId`
- `businessCode`
- `email`
- `phone`
- `walkIn`
- `contactName`
- `contactEmail`
- `contactPhone`

Le backend resout ensuite:

- le proprietaire reel (`ownerType`, `ownerCode`)
- le contact de reservation
- la subscription active si `paymentMode = SUBSCRIPTION`
- le pass utilisable si `paymentMode = PASS`
- l'entitlement compatible avec la ressource

### Selection identite

Envoyer au maximum un seul identifiant fort parmi:

- `memberId`
- `customerId`
- `businessCode`

Si aucun identifiant fort n'est envoye, le backend tente une resolution par `email` puis `phone`.

Si aucune personne existante n'est trouvee et que `walkIn = true` ou qu'un contact est fourni, le backend cree un owner temporaire avec un code genere. Ce scenario est adapte aux visiteurs de passage.

### Dates

Les champs `LocalDateTime` utilisent un format sans timezone:

```json
"2026-04-20T09:00:00"
```

Contraintes appliquees:

- `endedAt` doit etre apres `startedAt`
- les horaires doivent etre alignes sur des slots de 30 minutes
- les secondes et nanosecondes doivent etre a zero
- la politique de la ressource peut imposer une duree min/max et un delai minimum de reservation

Les champs `Instant` utilisent un format ISO avec timezone:

```json
"2026-04-20T09:00:00Z"
```

## Concepts frontend

### Payment modes

`BookingPaymentMode`

- `DIRECT`: reservation payante directe. Le backend cree un billable item `BOOKING` en statut `PENDING` lors de la confirmation.
- `SUBSCRIPTION`: reservation consommee sur une subscription active. Le frontend n'envoie pas `subscriptionNumber`.
- `PASS`: reservation consommee sur un pass utilisable. Le frontend n'envoie pas `passNumber`.

Un visiteur temporaire peut reserver en `DIRECT`. Il ne peut pas reserver en `SUBSCRIPTION` ou `PASS`.

### Statuts

`BookingStatus`

- `DRAFT`
- `PENDING_APPROVAL`
- `REJECTED`
- `CONFIRMED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`
- `NO_SHOW`
- `EXPIRED`

Transitions principales:

- creation sans approbation + `confirmImmediately=true`: `DRAFT -> CONFIRMED`
- creation sans approbation + `confirmImmediately=false`: `DRAFT`
- creation avec policy d'approbation: `PENDING_APPROVAL`
- approbation: `PENDING_APPROVAL -> DRAFT -> CONFIRMED`
- rejet: `PENDING_APPROVAL -> REJECTED`
- demarrage: `CONFIRMED -> IN_PROGRESS`
- check-in: `CONFIRMED -> IN_PROGRESS` ou reste `IN_PROGRESS`
- check-out: reste `IN_PROGRESS`
- completion: `CONFIRMED|IN_PROGRESS -> COMPLETED`
- annulation: `DRAFT|PENDING_APPROVAL|CONFIRMED|IN_PROGRESS|REJECTED -> CANCELLED`, sauf booking deja `COMPLETED` ou `CANCELLED`
- no-show: `CONFIRMED|IN_PROGRESS -> NO_SHOW`

### Holds

Un hold reserve temporairement la capacite d'une ressource.

Statuts `BookingHoldStatus`:

- `ACTIVE`
- `CONFIRMED`
- `RELEASED`
- `EXPIRED`

Parcours recommande:

1. `POST /bookings/check-availability`
2. `POST /bookings/holds`
3. `POST /bookings` avec `holdNumber`
4. Le backend passe le hold en `CONFIRMED` si le booking correspond exactement au hold.

Si l'utilisateur abandonne:

- `PATCH /bookings/holds/{holdNumber}/release`

Un worker doit appeler:

- `POST /bookings/holds/expire-due`

### Resource prerequisites

Une ressource doit etre reservable:

- `bookingEnabled = true`
- `active = true`
- `status = ACTIVE`
- `capacity` suffisante si renseignee
- une politique resource peut imposer duree min/max, delai minimum, annulation
- une pricing rule active permet de calculer le prix; sinon prix `0 XAF` par defaut avec unite `HOUR`

### Subscription/pass integration

Pour `SUBSCRIPTION`, le backend cherche:

- une subscription active pour `ownerType + ownerCode`
- un entitlement grant utilisable pour le type/groupe de ressource

Pour `PASS`, le backend cherche:

- un entitlement grant pass utilisable pour le type/groupe de ressource

Au moment de la confirmation:

- le backend reserve l'entitlement

Au moment de la completion ou no-show:

- le backend consomme l'entitlement
- un `UsageRecord` est cree avec `referenceType = BOOKING` et `referenceCode = bookingNumber`

Au moment de l'annulation:

- le backend libere l'entitlement si la reservation etait confirmee ou en cours

## Types TypeScript recommandes

```ts
export type BookingPaymentMode = "DIRECT" | "SUBSCRIPTION" | "PASS";

export type BookingStatus =
  | "DRAFT"
  | "PENDING_APPROVAL"
  | "REJECTED"
  | "CONFIRMED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW"
  | "EXPIRED";

export type BookingHoldStatus = "ACTIVE" | "CONFIRMED" | "RELEASED" | "EXPIRED";

export type BookingParticipantRole = "HOST" | "GUEST" | "ORGANIZER";
export type BookingParticipantStatus = "INVITED" | "CONFIRMED" | "DECLINED" | "REMOVED";

export type BookingRecurrenceFrequency = "DAILY" | "WEEKLY" | "MONTHLY";

export type SubscriberType = "MEMBER" | "CUSTOMER" | "BUSINESS_ENTITY";
```

## Format pagination

`GET /bookings` retourne un `PaginatedResponse<BookingResponse>`.

```json
{
  "data": [],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalPages": 0,
    "totalElements": 0,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false,
    "sort": {}
  }
}
```

Parametres standards:

- `page`
- `size`
- `sort`

Exemple:

`GET /sni/api/v1/bookings?page=0&size=20&sort=startedAt,desc`

## Endpoints

### Verifier la disponibilite

`POST /sni/api/v1/bookings/check-availability`

Utiliser cet endpoint avant d'afficher le bouton de confirmation.

Request:

```json
{
  "resourceCode": "ROOM-001",
  "memberId": "MEM-00001",
  "paymentMode": "SUBSCRIPTION",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1
}
```

Request visiteur:

```json
{
  "resourceCode": "ROOM-001",
  "walkIn": true,
  "contactName": "Awa Demo",
  "contactEmail": "awa.demo@example.com",
  "paymentMode": "DIRECT",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1
}
```

Response:

```json
{
  "resourceCode": "ROOM-001",
  "resourceName": "Salle reunion 1",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "durationMinutes": 120,
  "quantity": 1,
  "available": true,
  "remainingCapacity": 1,
  "bookingUnit": "HOUR",
  "unitPrice": 5000,
  "estimatedAmount": 10000,
  "currency": "XAF",
  "message": "Available"
}
```

Si `available=false`, afficher `message` et proposer `POST /bookings/suggestions`.

### Suggestions alternatives

`POST /sni/api/v1/bookings/suggestions`

Utiliser le meme body que `check-availability`.

Response:

```json
[
  {
    "resourceCode": "ROOM-001",
    "resourceName": "Salle reunion 1",
    "startedAt": "2026-04-20T11:00:00",
    "endedAt": "2026-04-20T13:00:00",
    "remainingCapacity": 1,
    "estimatedAmount": 10000,
    "reason": "nearby_available_slot"
  }
]
```

Note: l'implementation actuelle propose des creneaux proches sur la meme ressource, dans une fenetre `-4h/+4h`, tries par montant estime.

### Creer un hold

`POST /sni/api/v1/bookings/holds`

Request:

```json
{
  "resourceCode": "ROOM-001",
  "memberId": "MEM-00001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1,
  "ttlMinutes": 10,
  "idempotencyKey": "hold-room-001-20260420-0900-mem-00001"
}
```

Response:

```json
{
  "holdNumber": "HLD-2026-000001",
  "resourceCode": "ROOM-001",
  "ownerType": "MEMBER",
  "ownerCode": "MEM-00001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1,
  "status": "ACTIVE",
  "expiresAt": "2026-04-16T10:10:00Z"
}
```

Frontend:

- stocker `holdNumber`
- afficher un countdown base sur `expiresAt`
- envoyer `holdNumber` dans `POST /bookings`
- liberer le hold si l'utilisateur annule

### Liberer un hold

`PATCH /sni/api/v1/bookings/holds/{holdNumber}/release`

Response: `BookingHoldResponse`

### Expirer les holds dus

`POST /sni/api/v1/bookings/holds/expire-due?limit=100`

Response:

```json
3
```

Ce endpoint est destine a un worker/admin, pas a l'utilisateur final.

### Creer un booking

`POST /sni/api/v1/bookings`

Request directe:

```json
{
  "resourceCode": "ROOM-001",
  "walkIn": true,
  "contactName": "Awa Demo",
  "contactEmail": "awa.demo@example.com",
  "contactPhone": "+237600000000",
  "idempotencyKey": "booking-room-001-20260420-0900-awa-demo",
  "holdNumber": "HLD-2026-000001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1,
  "paymentMode": "DIRECT",
  "confirmImmediately": true,
  "sendEmail": true,
  "notes": "Besoin d'un projecteur",
  "metadataJson": "{\"source\":\"frontdesk\"}",
  "participants": [
    {
      "name": "Invite 1",
      "email": "invite1@example.com",
      "role": "GUEST"
    }
  ]
}
```

Request subscription:

```json
{
  "resourceCode": "ROOM-001",
  "memberId": "MEM-00001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1,
  "paymentMode": "SUBSCRIPTION",
  "confirmImmediately": true,
  "sendEmail": true
}
```

Request pass:

```json
{
  "resourceCode": "ROOM-001",
  "customerId": "CUS-00001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1,
  "paymentMode": "PASS",
  "confirmImmediately": true
}
```

Response:

```json
{
  "bookingNumber": "BKG-2026-000001",
  "resourceCode": "ROOM-001",
  "resourceName": "Salle reunion 1",
  "resourceTypeCode": "MEETING_ROOM",
  "resourceGroupCode": "ROOMS",
  "ownerType": "MEMBER",
  "ownerCode": "MEM-00001",
  "contactName": "Awa Demo",
  "contactEmail": "awa.demo@example.com",
  "contactPhone": "+237600000000",
  "status": "CONFIRMED",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "durationMinutes": 120,
  "quantity": 1,
  "bookingUnit": "HOUR",
  "paymentMode": "SUBSCRIPTION",
  "subscriptionNumber": "SUB-MEM-COW-MON-202604-00000001",
  "passNumber": null,
  "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
  "unitPrice": 5000,
  "subtotalAmount": 10000,
  "totalAmount": 10000,
  "currency": "XAF",
  "notes": null,
  "metadataJson": null,
  "confirmedAt": "2026-04-16T10:00:00Z",
  "completedAt": null,
  "cancelledAt": null,
  "createdAt": "2026-04-16T10:00:00Z",
  "lines": [
    {
      "id": 1,
      "lineType": "RESOURCE",
      "description": "Booking resource - Salle reunion 1",
      "quantity": 2,
      "unit": "HOUR",
      "unitPrice": 5000,
      "amount": 10000,
      "currency": "XAF",
      "entitlementCode": null,
      "billableNumber": null
    },
    {
      "id": 2,
      "lineType": "ENTITLEMENT",
      "description": "Entitlement consumption - ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
      "quantity": 2,
      "unit": "HOUR",
      "unitPrice": 0,
      "amount": 0,
      "currency": "XAF",
      "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
      "billableNumber": null
    }
  ],
  "participants": []
}
```

### Creer des bookings recurrents

`POST /sni/api/v1/bookings/recurring`

Request:

```json
{
  "booking": {
    "resourceCode": "ROOM-001",
    "memberId": "MEM-00001",
    "startedAt": "2026-04-20T09:00:00",
    "endedAt": "2026-04-20T11:00:00",
    "quantity": 1,
    "paymentMode": "SUBSCRIPTION",
    "confirmImmediately": true
  },
  "frequency": "WEEKLY",
  "intervalValue": 1,
  "occurrences": 4
}
```

Response: `BookingResponse[]`

Contraintes:

- `occurrences`: min `2`, max `60`
- `intervalValue`: min `1`
- les occurrences sont creees une par une
- si une occurrence echoue, le frontend doit afficher l'erreur retournee; l'API est transactionnelle

### Recuperer un booking

`GET /sni/api/v1/bookings/{bookingNumber}`

Response: `BookingResponse`

### Lister les bookings

`GET /sni/api/v1/bookings`

Filtres:

- `query`
- `ownerType`
- `ownerCode`
- `resourceCode`
- `status`
- `startedFrom`
- `startedTo`
- `page`
- `size`
- `sort`

Exemple:

`GET /sni/api/v1/bookings?query=awa&status=CONFIRMED&page=0&size=20`

La recherche utilise SQL natif avec trigram/similarity sur:

- `bookingNumber`
- `ownerCode`
- `contactName`
- `contactEmail`
- `resourceCode`
- `resourceName`
- `resourceTypeCode`
- `resourceGroupCode`

### Recherche rapide

`GET /sni/api/v1/bookings/search?query=awa`

Response: `BookingResponse[]`

Retourne jusqu'a 20 resultats via la fonction SQL `search_booking`.

### Confirmer

`PATCH /sni/api/v1/bookings/{bookingNumber}/confirm`

Request optionnelle:

```json
{
  "changedBy": "admin@example.com",
  "reason": "Client confirme au desk",
  "sendEmail": true
}
```

Effets:

- valide encore la disponibilite
- reserve la ressource
- confirme un hold si `holdNumber` est lie
- reserve l'entitlement si `SUBSCRIPTION` ou `PASS`
- cree un billable item si `DIRECT`
- envoie l'email si demande

### Approuver

`PATCH /sni/api/v1/bookings/{bookingNumber}/approve`

Request optionnelle:

```json
{
  "actor": "manager@example.com",
  "reason": "Approuve pour client premium",
  "sendEmail": true
}
```

Effets:

- valide que le booking est `PENDING_APPROVAL`
- passe par `DRAFT`
- confirme automatiquement le booking

### Rejeter

`PATCH /sni/api/v1/bookings/{bookingNumber}/reject`

Request optionnelle:

```json
{
  "actor": "manager@example.com",
  "reason": "Quota depasse",
  "sendEmail": true
}
```

Response: `BookingResponse` avec `status = REJECTED`.

### Demarrer

`PATCH /sni/api/v1/bookings/{bookingNumber}/start`

Request optionnelle: `BookingStatusChangeRequest`

Status attendu: `CONFIRMED`.

Response: `BookingResponse` avec `status = IN_PROGRESS`.

### Completer

`PATCH /sni/api/v1/bookings/{bookingNumber}/complete`

Request optionnelle: `BookingStatusChangeRequest`

Status attendu: `CONFIRMED` ou `IN_PROGRESS`.

Effets:

- consomme l'entitlement si `SUBSCRIPTION` ou `PASS`
- cree un `UsageRecord`
- marque le booking `COMPLETED`
- envoie l'email si demande

### Annuler

`PATCH /sni/api/v1/bookings/{bookingNumber}/cancel`

Request optionnelle:

```json
{
  "changedBy": "admin@example.com",
  "reason": "Client indisponible",
  "sendEmail": true
}
```

Effets:

- verifie la policy d'annulation de la ressource
- libere la ressource si le booking etait `CONFIRMED` ou `IN_PROGRESS`
- libere l'entitlement si necessaire
- marque le booking `CANCELLED`

### No-show

`PATCH /sni/api/v1/bookings/{bookingNumber}/no-show`

Request optionnelle: `BookingStatusChangeRequest`

Status attendu: `CONFIRMED` ou `IN_PROGRESS`.

Effets:

- consomme l'entitlement si `SUBSCRIPTION` ou `PASS`
- marque le booking `NO_SHOW`
- participe aux quotas de no-show par audience

### Check-in

`PATCH /sni/api/v1/bookings/{bookingNumber}/check-in`

Request optionnelle:

```json
{
  "actor": "frontdesk@example.com",
  "note": "Client arrive",
  "sendEmail": false
}
```

Status attendu: `CONFIRMED` ou `IN_PROGRESS`.

Response: `BookingResponse`.

### Check-out

`PATCH /sni/api/v1/bookings/{bookingNumber}/check-out`

Request optionnelle:

```json
{
  "actor": "frontdesk@example.com",
  "note": "Salle rendue propre",
  "sendEmail": false
}
```

Status attendu: `IN_PROGRESS`.

Response: `BookingResponse`.

### Ajouter un participant

`POST /sni/api/v1/bookings/{bookingNumber}/participants`

Request:

```json
{
  "memberCode": "MEM-00002",
  "name": "Invite 1",
  "email": "invite1@example.com",
  "phone": "061234678",
  "role": "GUEST"
}
```

Response:

```json
{
  "id": 1,
  "memberCode": "MEM-00002",
  "name": "Invite 1",
  "email": "invite1@example.com",
  "phone": "061234678",
  "role": "GUEST",
  "status": "INVITED"
}
```

### Retirer un participant

`DELETE /sni/api/v1/bookings/{bookingNumber}/participants/{participantId}`

Response: `BookingParticipantResponse` avec `status = REMOVED`.

### Historique de statuts

`GET /sni/api/v1/bookings/{bookingNumber}/history`

Response:

```json
[
  {
    "fromStatus": "DRAFT",
    "toStatus": "CONFIRMED",
    "changedBy": "admin@example.com",
    "reason": "Booking confirmed",
    "changedAt": "2026-04-16T10:00:00Z"
  }
]
```

### Evenements booking

`GET /sni/api/v1/bookings/{bookingNumber}/events`

Response:

```json
[
  {
    "eventNumber": "BEV-2026-000001",
    "bookingNumber": "BKG-2026-000001",
    "eventType": "BOOKING_CONFIRMED",
    "title": "Booking confirmed",
    "description": "Booking was confirmed",
    "payloadJson": null,
    "createdAt": "2026-04-16T10:00:00Z"
  }
]
```

`BookingEventType`:

- `BOOKING_CREATED`
- `BOOKING_HOLD_CREATED`
- `BOOKING_HOLD_EXPIRED`
- `BOOKING_CONFIRMED`
- `BOOKING_APPROVED`
- `BOOKING_REJECTED`
- `BOOKING_STARTED`
- `BOOKING_CHECKED_IN`
- `BOOKING_CHECKED_OUT`
- `BOOKING_COMPLETED`
- `BOOKING_CANCELLED`
- `BOOKING_NO_SHOW`
- `RESOURCE_RESERVED`
- `RESOURCE_RELEASED`
- `ENTITLEMENT_RESERVED`
- `ENTITLEMENT_CONSUMED`
- `ENTITLEMENT_RELEASED`
- `USAGE_RECORDED`
- `EMAIL_QUEUED`
- `EMAIL_SENT`
- `EMAIL_FAILED`
- `REPAIR_APPLIED`
- `QUOTA_OVERRIDE_APPLIED`

## Policies et quotas

### Creer une policy par audience

`POST /sni/api/v1/bookings/policies`

Request:

```json
{
  "audienceType": "MEMBER",
  "resourceTypeCode": "MEETING_ROOM",
  "resourceGroupCode": "ROOMS",
  "approvalRequired": false,
  "maxActiveBookings": 3,
  "maxBookingsPerDay": 2,
  "maxBookingsPerWeek": 5,
  "maxBookingsPerMonth": 20,
  "maxNoShowsPerMonth": 2,
  "minBookingNoticeMinutes": 60,
  "maxBookingDurationMinutes": 240,
  "active": true
}
```

Response: `BookingAudiencePolicyResponse`

Regles d'application:

- la policy est recherchee par `audienceType`, puis type/groupe de ressource
- `approvalRequired=true` cree les bookings en `PENDING_APPROVAL`
- les limites peuvent etre augmentees par un quota override
- `maxNoShowsPerMonth` bloque les owners qui depassent le nombre de no-show

### Creer un quota override

`POST /sni/api/v1/bookings/quota-overrides`

Request:

```json
{
  "memberId": "MEM-00001",
  "resourceCode": "ROOM-001",
  "extraActiveBookings": 1,
  "extraBookingsPerDay": 1,
  "extraBookingsPerWeek": 2,
  "extraBookingsPerMonth": 5,
  "reason": "Exception commerciale",
  "approvedBy": "manager@example.com",
  "validFrom": "2026-04-16T10:00:00Z",
  "validUntil": "2026-05-16T10:00:00Z"
}
```

Response:

```json
{
  "overrideNumber": "BKO-2026-000001",
  "ownerType": "MEMBER",
  "ownerCode": "MEM-00001",
  "resourceCode": "ROOM-001",
  "extraActiveBookings": 1,
  "extraBookingsPerDay": 1,
  "extraBookingsPerWeek": 2,
  "extraBookingsPerMonth": 5,
  "reason": "Exception commerciale",
  "approvedBy": "manager@example.com",
  "validFrom": "2026-04-16T10:00:00Z",
  "validUntil": "2026-05-16T10:00:00Z",
  "active": true
}
```

Ce endpoint doit etre reserve a un backoffice/admin.

## Notifications

### Queue reminder

`POST /sni/api/v1/bookings/{bookingNumber}/notifications/reminder?minutesBefore=30`

Response:

```json
1
```

Retourne `0` si le booking n'a pas d'email contact.

### Dispatcher les notifications dues

`POST /sni/api/v1/bookings/notifications/dispatch-due?limit=100`

Response:

```json
10
```

Ce endpoint est destine a un worker. Il envoie les notifications `PENDING` dont `scheduledAt <= now`.

## Metrics

`GET /sni/api/v1/bookings/metrics/overview`

Response:

```json
{
  "draftBookings": 1,
  "pendingApprovalBookings": 2,
  "confirmedBookings": 10,
  "completedBookings": 25,
  "cancelledBookings": 3,
  "noShowBookings": 1,
  "activeHolds": 2,
  "confirmedAmount": 75000,
  "completedAmount": 225000
}
```

Utilisation frontend:

- dashboard operations
- alertes sur holds actifs
- suivi no-show
- suivi revenu direct via confirmed/completed amount

## Repair jobs

`POST /sni/api/v1/bookings/repair?limit=100`

Response:

```json
{
  "inspected": 25,
  "repaired": 0,
  "failed": 0
}
```

Ce endpoint inspecte les bookings candidats et ajoute un event `REPAIR_APPLIED`. Il est destine a un worker/admin.

## Recommandations d'ecrans frontend

### Planning resource

Objectif: voir et creer des reservations par ressource.

Donnees:

- ressource selectionnee depuis le module resource
- disponibilites via `check-availability`
- suggestions via `suggestions`
- bookings via `GET /bookings?resourceCode=...&startedFrom=...&startedTo=...`

Actions:

- creer hold
- creer booking
- confirmer
- annuler
- check-in/check-out

### Backoffice booking list

Filtres:

- recherche `query`
- status
- ressource
- owner
- periode

Actions rapides:

- consulter detail
- approuver/rejeter si `PENDING_APPROVAL`
- confirmer si `DRAFT`
- start/check-in/check-out/complete
- cancel/no-show

### Detail booking

Onglets recommandes:

- Resume
- Pricing/lignes
- Participants
- Timeline statuts
- Events
- Notifications
- Resource
- Subscription/pass

Afficher clairement:

- `bookingNumber`
- owner resolu
- contact
- resource
- periode
- statut
- `paymentMode`
- `subscriptionNumber` ou `passNumber` si applicable
- `entitlementCode`
- montants

### Frontdesk walk-in

Formulaire minimal:

- resource
- date debut/fin
- contactName
- contactEmail ou contactPhone
- `walkIn=true`
- `paymentMode=DIRECT`

Ne pas demander:

- ownerCode
- customerId obligatoire
- subscriptionNumber
- passNumber
- entitlementCode

### Admin policies

Ecran reserve admin:

- creation policy par audience
- creation quota override par owner
- suivi no-show

## Gestion d'erreurs attendues

Les erreurs backend sont retournees via le format global de l'application.

Cas frequents a gerer:

- ressource inexistante
- ressource non reservable
- creneau non aligne sur 30 minutes
- duree inferieure/superieure a la policy resource
- delai minimum de reservation non respecte
- capacite insuffisante
- hold expire ou ne correspondant pas au booking
- aucune subscription active
- aucun entitlement subscription utilisable
- aucun pass utilisable
- quota audience depasse
- no-show quota depasse
- statut invalide pour une action

Exemple:

```json
{
  "errorCode": "CONFLICT",
  "status": 409,
  "message": "booking policy: daily booking quota exceeded",
  "timestamp": "2026-04-16 10:00:00",
  "path": "/sni/api/v1/bookings"
}
```

## Workers et automatisations

Workers a configurer cote backend/ops:

- expiration holds: `POST /bookings/holds/expire-due?limit=100`
- dispatch notifications: `POST /bookings/notifications/dispatch-due?limit=100`
- repair booking: `POST /bookings/repair?limit=100`

Frequences recommandees:

- holds: toutes les 1 a 5 minutes
- notifications: toutes les 1 a 5 minutes
- repair: toutes les heures ou une fois par jour selon volume

## Tarification : paliers de duree et unite imposee

Ajoute le 2026-09-01. La journee normale va de 08h00 a 18h00, soit dix heures.

### Ce que la duree declenche

| Duree | Unite retenue | Montant |
|---|---|---|
| moins de 5h | `HOUR` | duree x tarif horaire |
| **5h pile** | `HALF_DAY` | 1 x forfait demi-journee |
| de 5h01 a 9h59 | `HOUR` | duree x tarif horaire |
| **10h et au-dela** | `DAY` | 1 x forfait journalier |

Sur une grille a 10 000 l'heure, 40 000 la demi-journee et 70 000 la journee :

```
  2h00  ->  20 000   HOUR
  4h00  ->  40 000   HOUR
  5h00  ->  40 000   HALF_DAY
  5h30  ->  55 000   HOUR
  6h00  ->  60 000   HOUR
  9h30  ->  95 000   HOUR
 10h00  ->  70 000   DAY
```

**Le forfait ne vaut qu'a sa duree exacte.** 5h30 repasse a l'heure : si le forfait couvrait
toute la tranche, une reservation de 5h30 serait facturee 5h et le systeme rendrait une demi-heure
sans que personne ne l'ait decide.

Deux consequences a assumer dans l'interface, car elles surprennent :

- **5h coute moins cher que 4h30** (40 000 contre 45 000). C'est le propre d'un forfait : il est
  remise. Un simulateur qui affiche le montant en direct rend la chose lisible ; un formulaire muet
  la fait decouvrir sur la facture.
- **9h30 coute plus cher que 10h** (95 000 contre 70 000). Suggerer d'arrondir a la journee est un
  bon geste commercial, et evite une reclamation.

Ce qui change par rapport a l'existant : le seuil journalier etait a **8h**, il passe a **10h** ;
et tout ce qui depassait 5h basculait en demi-journee, donc **6h etait facturee 5h**.

### Imposer l'unite · administration uniquement

`bookingUnit` est accepte sur `POST /bookings` et `POST /bookings/check-availability`. Il impose
l'unite de facturation la ou la duree en designerait une autre.

```json
{
  "resourceCode": "RES-SAL-202609-00000002",
  "startedAt": "2026-09-25T08:00",
  "endedAt": "2026-09-25T14:00",
  "quantity": 1,
  "paymentMode": "DIRECT",
  "bookingUnit": "HALF_DAY"
}
```

Valeurs : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`. Vide, l'unite se deduit de la duree.

**Le prix n'est jamais saisi.** Il est lu dans la grille tarifaire de la ressource pour cette
unite. Un montant facture reste donc rattache a une regle, et explicable apres coup. Pour un geste
hors grille, ce n'est pas ce mecanisme : passez par une remise sur la facture.

Sur les memes 6h, qui partiraient automatiquement a 60 000 :

| `bookingUnit` | Montant |
|---|---|
| absent | 60 000 · 6 x horaire |
| `HALF_DAY` | 40 000 · 1 forfait |
| `DAY` | 70 000 · 1 forfait |
| `HOUR` | 60 000 · inchange |

**Un forfait impose reste un forfait**, quelle que soit la duree : `HALF_DAY` donne 40 000 sur 4h,
sur 6h comme sur 10h. Sans cela, l'arrondi au superieur en compterait deux sur 6h et imposer le
forfait couterait plus cher que de ne rien imposer. L'heure fait exception, elle reste
proportionnelle : un forfait couvre une plage, une heure se compte.

`quantity` multiplie ensuite le tout : 2 places en demi-journee imposee font 2 forfaits.

### Refus quand la grille manque

Si la ressource n'a pas de regle tarifaire active pour l'unite demandee, la demande est refusee
plutot que repliee en silence sur une autre unite :

```
La ressource RES-SAL-202609-00000001 n'a aucune grille tarifaire active en HALF_DAY ·
creez-la avant d'imposer cette unite, ou laissez le calcul automatique.
```

Sur `POST /bookings` c'est une erreur `400`. Sur `POST /bookings/check-availability`, le devis
revient avec `available: false` et ce message : le point d'entree existe pour renseigner, pas pour
echouer. Message a afficher tel quel, il nomme la ressource et l'unite.

Pour eviter le refus, l'interface peut n'ouvrir le selecteur qu'aux unites reellement tarifees,
lisibles par `GET /resource-pricing-rules?resourceCode=...`.

### Le portail client n'a pas ce champ

`POST /client/bookings` et `POST /client/bookings/availability` ne portent aucun `bookingUnit` :
le tarif s'y deduit toujours de la duree. Envoyer le champ sur une session client authentifiee
donne un `400 Propriete inconnue dans le corps de la requete : bookingUnit`, le rejet des
proprietes inconnues etant global. C'est voulu — un client ne choisit pas son propre tarif.

Une reservation client passe donc forcement par le tableau des paliers ci-dessus, et le montant
affiche avant confirmation est celui qui sera facture.

## Checklist integration frontend

- Ne pas envoyer `ownerCode`, `subscriptionNumber`, `passNumber`, `entitlementCode` dans les payloads de creation.
- Toujours verifier la disponibilite avant creation.
- Utiliser un `idempotencyKey` stable pour hold et booking depuis le frontend.
- Utiliser un hold pour les parcours multi-etapes.
- Afficher suggestions si `available=false`.
- Desactiver les actions incompatibles avec le statut courant.
- Afficher les messages d'erreur backend tels quels pour les conflits de policy/quota.
- Refresh du detail apres chaque transition de statut.
- Prevoir un acces admin pour policies, quota overrides, workers et metrics.
- Afficher le montant estime en direct : les paliers de duree produisent des sauts que le
  formulaire seul ne laisse pas deviner.
- `bookingUnit` est reserve a l'administration · ne pas l'exposer dans le parcours client.
