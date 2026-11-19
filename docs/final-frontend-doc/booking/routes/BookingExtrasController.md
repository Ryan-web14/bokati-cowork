# BookingExtrasController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 6 route(s).

## Routes

<a id="r-post-sni-api-v1-bookings-number-amenities"></a>
### POST /sni/api/v1/bookings/{number}/amenities

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingExtrasController.ItemRequest`
- Réponse : `BookingExtras.LineView`
- Modèles : [BookingExtras.LineView](#m-BookingExtras-LineView), [BookingExtrasController.ItemRequest](#m-BookingExtrasController-ItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingExtrasController.java`, méthode `addAmenity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-number-extra-participants"></a>
### POST /sni/api/v1/bookings/{number}/extra-participants

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingExtrasController.ParticipantsRequest`
- Réponse : `BookingExtras.LineView`
- Modèles : [BookingExtras.LineView](#m-BookingExtras-LineView), [BookingExtrasController.ParticipantsRequest](#m-BookingExtrasController-ParticipantsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingExtrasController.java`, méthode `addParticipants`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-number-lines"></a>
### GET /sni/api/v1/bookings/{number}/lines

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingExtras.LineView>`
- Modèles : [BookingExtras.LineView](#m-BookingExtras-LineView)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingExtrasController.java`, méthode `lines`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-number-promo-code"></a>
### POST /sni/api/v1/bookings/{number}/promo-code

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingExtrasController.PromoRequest`
- Réponse : `BookingExtras.LineView`
- Modèles : [BookingExtras.LineView](#m-BookingExtras-LineView), [BookingExtrasController.PromoRequest](#m-BookingExtrasController-PromoRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingExtrasController.java`, méthode `promo`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-number-services"></a>
### POST /sni/api/v1/bookings/{number}/services

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingExtrasController.ItemRequest`
- Réponse : `BookingExtras.LineView`
- Modèles : [BookingExtras.LineView](#m-BookingExtras-LineView), [BookingExtrasController.ItemRequest](#m-BookingExtrasController-ItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingExtrasController.java`, méthode `addService`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resource-bundles-code-book"></a>
### POST /sni/api/v1/resource-bundles/{code}/book

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingExtrasController.BundleBookingRequest`
- Réponse : `BookingBatchResponse`
- Modèles : [BookingBatchResponse](#m-BookingBatchResponse), [BookingExtrasController.BundleBookingRequest](#m-BookingExtrasController-BundleBookingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingExtrasController.java`, méthode `book`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingExtras-LineView"></a>
#### BookingExtras.LineView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `description` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `billableNumber` | texte |  |  |

<a id="m-BookingExtrasController-ItemRequest"></a>
#### BookingExtrasController.ItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `quantity` | entier |  |  |

<a id="m-BookingExtrasController-ParticipantsRequest"></a>
#### BookingExtrasController.ParticipantsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `count` | entier |  |  |
| `waive` | booléen |  |  |

<a id="m-BookingExtrasController-PromoRequest"></a>
#### BookingExtrasController.PromoRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |

<a id="m-BookingBatchResponse"></a>
#### BookingBatchResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batchNumber` | texte |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `billingCompanyCode` | texte |  |  |
| `bookingCount` | entier |  |  |
| `memberCount` | entier |  |  |
| `notes` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `invoices` | [BookingBatchResponse.BatchInvoice](#m-BookingBatchResponse-BatchInvoice)[] |  |  |
| `uninvoicedBookingNumbers` | texte[] |  |  |
| `bookings` | [BookingResponse](#m-BookingResponse)[] |  |  |

<a id="m-BookingExtrasController-BundleBookingRequest"></a>
#### BookingExtrasController.BundleBookingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberIds` | texte[] |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `billingCompanyCode` | texte |  |  |
| `confirmImmediately` | booléen |  |  |
| `sendEmail` | booléen |  |  |
| `notes` | texte |  |  |
| `idempotencyKey` | texte |  |  |

<a id="m-BookingBatchResponse-BatchInvoice"></a>
#### BookingBatchResponse.BatchInvoice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `REFUNDED`, `APPLIED`, `WRITTEN_OFF` |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `bookingNumbers` | texte[] |  |  |

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
