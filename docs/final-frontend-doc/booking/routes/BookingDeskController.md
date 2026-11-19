# BookingDeskController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings-planning"></a>
### GET /sni/api/v1/bookings/planning

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingDeskService.Planning`
- Modèles : [BookingDeskService.Planning](#m-BookingDeskService-Planning)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingDeskController.java`, méthode `planning`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |
| `resourceCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-bookings-series-recurrencegroupnumber"></a>
### GET /sni/api/v1/bookings/series/{recurrenceGroupNumber}

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingResponse>`
- Modèles : [BookingResponse](#m-BookingResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingDeskController.java`, méthode `series`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recurrenceGroupNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-series-recurrencegroupnumber-cancel"></a>
### PATCH /sni/api/v1/bookings/series/{recurrenceGroupNumber}/cancel

- Accès : back-office : rôle d'administration et droit `BOOKING:CANCEL`
- Corps JSON (facultatif) : `BookingDeskController.SeriesCancelRequest`
- Réponse : `BookingDeskService.SeriesOutcome`
- Modèles : [BookingDeskService.SeriesOutcome](#m-BookingDeskService-SeriesOutcome), [BookingDeskController.SeriesCancelRequest](#m-BookingDeskController-SeriesCancelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingDeskController.java`, méthode `cancelSeries`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recurrenceGroupNumber` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |

<a id="r-patch-sni-api-v1-bookings-series-recurrencegroupnumber-reschedule"></a>
### PATCH /sni/api/v1/bookings/series/{recurrenceGroupNumber}/reschedule

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingDeskController.SeriesRescheduleRequest`
- Réponse : `BookingDeskService.SeriesOutcome`
- Modèles : [BookingDeskService.SeriesOutcome](#m-BookingDeskService-SeriesOutcome), [BookingDeskController.SeriesRescheduleRequest](#m-BookingDeskController-SeriesRescheduleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingDeskController.java`, méthode `rescheduleSeries`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recurrenceGroupNumber` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingDeskService-Planning"></a>
#### BookingDeskService.Planning

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `bookings` | entier |  |  |
| `arrived` | entier |  |  |
| `late` | entier |  |  |
| `toCollect` | entier |  |  |
| `resources` | [BookingDeskService.PlanningResource](#m-BookingDeskService-PlanningResource)[] |  |  |

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

<a id="m-BookingDeskService-SeriesOutcome"></a>
#### BookingDeskService.SeriesOutcome

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `recurrenceGroupNumber` | texte |  |  |
| `changed` | entier |  |  |
| `skipped` | entier |  |  |
| `changedBookings` | texte[] |  |  |
| `refused` | objet { clé : texte } |  |  |

<a id="m-BookingDeskController-SeriesCancelRequest"></a>
#### BookingDeskController.SeriesCancelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-BookingDeskController-SeriesRescheduleRequest"></a>
#### BookingDeskController.SeriesRescheduleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `newStartTime` | [LocalTime](#m-LocalTime) |  |  |
| `newEndTime` | [LocalTime](#m-LocalTime) |  |  |
| `shiftDays` | entier |  |  |
| `acceptPriceChange` | booléen |  |  |

<a id="m-BookingDeskService-PlanningResource"></a>
#### BookingDeskService.PlanningResource

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `bookings` | [BookingDeskService.PlanningItem](#m-BookingDeskService-PlanningItem)[] |  |  |

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

<a id="m-LocalTime"></a>
#### LocalTime

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `minute` | entier |  |  |
| `second` | entier |  |  |
| `nano` | entier |  |  |

<a id="m-BookingDeskService-PlanningItem"></a>
#### BookingDeskService.PlanningItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `contactName` | texte |  |  |
| `contactPhone` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `status` | enum : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED` |  |  |
| `statusLabel` | texte |  |  |
| `checkedInAt` | date-heure ISO 8601 UTC |  |  |
| `late` | booléen |  |  |
| `lateMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `paymentStatus` | enum : `COVERED`, `PENDING`, `PAID`, `CANCELLED` |  |  |
| `payOnSite` | booléen |  |  |
| `attendanceAnswer` | texte |  |  |
| `quantity` | entier |  |  |
