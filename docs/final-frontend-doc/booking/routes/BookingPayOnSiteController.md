# BookingPayOnSiteController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings-pay-on-site-clients"></a>
### GET /sni/api/v1/bookings/pay-on-site/clients

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingPayOnSite.Grant>`
- Modèles : [BookingPayOnSite.Grant](#m-BookingPayOnSite-Grant)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingPayOnSiteController.java`, méthode `grants`

<a id="r-get-sni-api-v1-bookings-pay-on-site-clients-ownertype-ownercode"></a>
### GET /sni/api/v1/bookings/pay-on-site/clients/{ownerType}/{ownerCode}

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingPayOnSite.Access`
- Modèles : [BookingPayOnSite.Access](#m-BookingPayOnSite-Access), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingPayOnSiteController.java`, méthode `access`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | chemin | `SubscriberType` | oui |  |
| `ownerCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-bookings-pay-on-site-clients-ownertype-ownercode"></a>
### PUT /sni/api/v1/bookings/pay-on-site/clients/{ownerType}/{ownerCode}

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingPayOnSiteController.GrantRequest`
- Réponse : `BookingPayOnSite.Grant`
- Modèles : [BookingPayOnSite.Grant](#m-BookingPayOnSite-Grant), [BookingPayOnSiteController.GrantRequest](#m-BookingPayOnSiteController-GrantRequest), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingPayOnSiteController.java`, méthode `set`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | chemin | `SubscriberType` | oui |  |
| `ownerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-pay-on-site-due"></a>
### GET /sni/api/v1/bookings/pay-on-site/due

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingResponse>`
- Modèles : [BookingResponse](#m-BookingResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingPayOnSiteController.java`, méthode `due`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-bookings-pay-on-site-reliability-ownertype-ownercode"></a>
### GET /sni/api/v1/bookings/pay-on-site/reliability/{ownerType}/{ownerCode}

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingReliability.Score`
- Modèles : [BookingReliability.Score](#m-BookingReliability-Score), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingPayOnSiteController.java`, méthode `reliability`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | chemin | `SubscriberType` | oui |  |
| `ownerCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingPayOnSite-Grant"></a>
#### BookingPayOnSite.Grant

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `enabled` | booléen |  |  |
| `reason` | texte |  |  |
| `revokedAutomatically` | booléen |  |  |
| `updatedBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingPayOnSite-Access"></a>
#### BookingPayOnSite.Access

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `allowed` | booléen |  |  |
| `source` | enum : `CLIENT_OPTION`, `GLOBAL_SETTING` |  |  |
| `reason` | texte |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-BookingPayOnSiteController-GrantRequest"></a>
#### BookingPayOnSiteController.GrantRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `enabled` | booléen | oui |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

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

<a id="m-BookingReliability-Score"></a>
#### BookingReliability.Score

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `score` | entier |  |  |
| `bookings` | entier int64 (texte au-delà de 2^53) |  |  |
| `noShows` | entier int64 (texte au-delà de 2^53) |  |  |
| `lateCancellations` | entier int64 (texte au-delà de 2^53) |  |  |
| `windowDays` | entier |  |  |
| `threshold` | entier |  |  |
| `guaranteeRequired` | booléen |  |  |
| `guarantee` | texte |  |  |
| `depositRate` | nombre décimal |  |  |

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
| `invitedAt` | date-heure ISO 8601 UTC |  |  |
| `respondedAt` | date-heure ISO 8601 UTC |  |  |
| `checkedInAt` | date-heure ISO 8601 UTC |  |  |
