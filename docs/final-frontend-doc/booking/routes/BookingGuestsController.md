# BookingGuestsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 5 route(s).

## Routes

<a id="r-patch-sni-api-v1-bookings-bookingnumber-details"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/details

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingGuests.Details`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingGuests.Details](#m-BookingGuests-Details)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingGuestsController.java`, méthode `details`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-bookingnumber-extend"></a>
### POST /sni/api/v1/bookings/{bookingNumber}/extend

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingGuestsController.ExtendRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingGuestsController.ExtendRequest](#m-BookingGuestsController-ExtendRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingGuestsController.java`, méthode `extend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-bookingnumber-participants-invite"></a>
### POST /sni/api/v1/bookings/{bookingNumber}/participants/invite

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingGuestsController.java`, méthode `inviteAll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-participants-participantid-check-in"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/participants/{participantId}/check-in

- Accès : back-office : rôle d'administration et droit `BOOKING:CHECKIN`
- Réponse : `BookingParticipantResponse`
- Modèles : [BookingParticipantResponse](#m-BookingParticipantResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingGuestsController.java`, méthode `checkIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `participantId` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-post-sni-api-v1-bookings-bookingnumber-participants-participantid-invite"></a>
### POST /sni/api/v1/bookings/{bookingNumber}/participants/{participantId}/invite

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingGuestsController.java`, méthode `invite`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `participantId` | chemin | `Long` (code identifiant ou nombre) | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingResponse"></a>
#### BookingResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `resourceTypeCode` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | texte |  |  |
| `contactPhone` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED` |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `durationMinutes` | entier |  |  |
| `quantity` | entier |  |  |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `paymentStatus` | enum : `COVERED`, `PENDING`, `PAID`, `CANCELLED` |  |  |
| `paymentStatusLabel` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `passNumber` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `notes` | texte |  |  |
| `metadataJson` | texte |  |  |
| `checkInToken` | texte |  |  |
| `checkInQrValue` | texte |  |  |
| `virtualMeetingUrl` | texte |  |  |
| `confirmedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [BookingLineResponse](#m-BookingLineResponse)[] |  |  |
| `participants` | [BookingParticipantResponse](#m-BookingParticipantResponse)[] |  |  |
| `resourceCapacity` | entier |  |  |
| `wholeResourceBooking` | booléen |  |  |
| `seatsLabel` | texte |  |  |
| `batchNumber` | texte |  |  |
| `modificationCount` | entier |  |  |
| `lastModifiedAt` | date-heure ISO 8601 UTC |  |  |
| `lastModification` | texte |  |  |
| `createdBy` | texte |  |  |
| `channel` | enum : `PORTAL`, `TEAM`, `PHONE`, `DESK`, `EMAIL`, `WHATSAPP` |  |  |
| `holdNumber` | texte |  |  |
| `recurrenceGroupNumber` | texte |  |  |
| `paymentDueAt` | date-heure locale sans fuseau |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `checkedInAt` | date-heure ISO 8601 UTC |  |  |
| `checkedOutAt` | date-heure ISO 8601 UTC |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectedBy` | texte |  |  |
| `rejectedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectionReason` | texte |  |  |
| `rejectionReasonCode` | texte |  |  |
| `cancellationReason` | texte |  |  |
| `cancellationReasonCode` | texte |  |  |
| `invoiceNumber` | texte |  |  |
| `extrasAmount` | nombre décimal |  |  |
| `grandTotal` | nombre décimal |  |  |
| `payOnSite` | booléen |  |  |
| `originalAmount` | nombre décimal |  |  |
| `priceAdjustmentAmount` | nombre décimal |  |  |
| `priceAdjustmentRate` | nombre décimal |  |  |
| `priceAdjustmentReason` | texte |  |  |
| `priceAdjustmentReasonCode` | texte |  |  |
| `priceAdjustmentBy` | texte |  |  |
| `priceAdjustmentStatus` | texte |  |  |
| `priceAdjustmentApprovalNumber` | texte |  |  |
| `leadNumber` | texte |  |  |
| `callNote` | texte |  |  |
| `attendanceAnswer` | texte |  |  |
| `attendanceAnsweredAt` | date-heure ISO 8601 UTC |  |  |
| `releasedEarlyAt` | date-heure ISO 8601 UTC |  |  |
| `depositAmount` | nombre décimal |  |  |
| `depositPaidAt` | date-heure ISO 8601 UTC |  |  |
| `roomLayout` | texte |  |  |
| `specialRequests` | texte |  |  |
| `internalNotes` | texte |  |  |
| `tags` | texte[] |  |  |
| `companyCoveredMinutes` | entier |  |  |
| `monthlyStatement` | booléen |  |  |
| `csatScore` | entier |  |  |
| `csatComment` | texte |  |  |
| `csatAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingGuests-Details"></a>
#### BookingGuests.Details

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `roomLayout` | texte |  |  |
| `specialRequests` | texte |  |  |
| `internalNotes` | texte |  |  |
| `tags` | texte[] |  |  |

<a id="m-BookingGuestsController-ExtendRequest"></a>
#### BookingGuestsController.ExtendRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `minutes` | entier |  | min `1`, max `1440` |
| `acceptPriceChange` | booléen |  |  |

<a id="m-BookingParticipantResponse"></a>
#### BookingParticipantResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `memberCode` | texte |  |  |
| `name` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `role` | enum : `HOST`, `GUEST`, `ORGANIZER`, `MEMBER` |  |  |
| `status` | enum : `INVITED`, `CONFIRMED`, `DECLINED`, `REMOVED` |  |  |
| `invitedAt` | date-heure ISO 8601 UTC |  |  |
| `respondedAt` | date-heure ISO 8601 UTC |  |  |
| `checkedInAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingLineResponse"></a>
#### BookingLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `lineType` | enum : `RESOURCE`, `ENTITLEMENT`, `BILLABLE`, `SERVICE`, `AMENITY`, `PARTICIPANT`, `DEPOSIT`, `OVERRUN`, `NO_SHOW`, `DISCOUNT` |  |  |
| `description` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `unitPrice` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `billableNumber` | texte |  |  |
