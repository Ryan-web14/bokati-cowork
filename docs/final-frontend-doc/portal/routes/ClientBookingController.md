# ClientBookingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-client-bookings"></a>
### GET /sni/api/v1/client/bookings

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<ClientBookingSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ClientBookingSummaryResponse](#m-ClientBookingSummaryResponse), [BookingStatus](#m-BookingStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `listBookings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `BookingStatus` |  |  |
| `from` | requête | `LocalDateTime` |  |  |
| `to` | requête | `LocalDateTime` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=startedAt,DESC |

<a id="r-post-sni-api-v1-client-bookings"></a>
### POST /sni/api/v1/client/bookings

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientCreateBookingRequest`
- Réponse : `ClientBookingResponse`
- Modèles : [ClientBookingResponse](#m-ClientBookingResponse), [ClientCreateBookingRequest](#m-ClientCreateBookingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `createBooking`

<a id="r-get-sni-api-v1-client-bookings-alternatives"></a>
### GET /sni/api/v1/client/bookings/alternatives

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<BookingSuggestionResponse>`
- Modèles : [BookingSuggestionResponse](#m-BookingSuggestionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `alternatives`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` | oui |  |
| `startedAt` | requête | `LocalDateTime` | oui |  |
| `endedAt` | requête | `LocalDateTime` | oui |  |
| `quantity` | requête | `Integer` |  |  |

<a id="r-post-sni-api-v1-client-bookings-availability"></a>
### POST /sni/api/v1/client/bookings/availability

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientCheckAvailabilityRequest`
- Réponse : `BookingAvailabilityResponse`
- Modèles : [BookingAvailabilityResponse](#m-BookingAvailabilityResponse), [ClientCheckAvailabilityRequest](#m-ClientCheckAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `checkAvailability`

<a id="r-post-sni-api-v1-client-bookings-create"></a>
### POST /sni/api/v1/client/bookings/create

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientCreateBookingRequest`
- Réponse : `ClientBookingResponse`
- Modèles : [ClientBookingResponse](#m-ClientBookingResponse), [ClientCreateBookingRequest](#m-ClientCreateBookingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `createBooking`

<a id="r-get-sni-api-v1-client-bookings-last-minute"></a>
### GET /sni/api/v1/client/bookings/last-minute

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientPortalBookingService.LastMinuteOffer>`
- Modèles : [ClientPortalBookingService.LastMinuteOffer](#m-ClientPortalBookingService-LastMinuteOffer)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `lastMinute`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `durationMinutes` | requête | `Integer` |  |  |

<a id="r-get-sni-api-v1-client-bookings-pay-on-site"></a>
### GET /sni/api/v1/client/bookings/pay-on-site

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingPayOnSite.Access`
- Modèles : [BookingPayOnSite.Access](#m-BookingPayOnSite-Access)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `payOnSite`

<a id="r-delete-sni-api-v1-client-bookings-bookingnumber"></a>
### DELETE /sni/api/v1/client/bookings/{bookingNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `ClientCancelBookingRequest`
- Réponse : `ClientBookingResponse`
- Modèles : [ClientBookingResponse](#m-ClientBookingResponse), [ClientCancelBookingRequest](#m-ClientCancelBookingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `cancelBooking`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-bookings-bookingnumber"></a>
### GET /sni/api/v1/client/bookings/{bookingNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientBookingResponse`
- Modèles : [ClientBookingResponse](#m-ClientBookingResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `getBooking`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-bookings-bookingnumber-extend"></a>
### POST /sni/api/v1/client/bookings/{bookingNumber}/extend

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientBookingController.ClientExtendRequest`
- Réponse : `ClientBookingResponse`
- Modèles : [ClientBookingResponse](#m-ClientBookingResponse), [ClientBookingController.ClientExtendRequest](#m-ClientBookingController-ClientExtendRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `extend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-bookings-bookingnumber-pay-cash"></a>
### POST /sni/api/v1/client/bookings/{bookingNumber}/pay/cash

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `DeclareCashPaymentRequest`
- Réponse : `CashPaymentDeclarationResponse`
- Modèles : [CashPaymentDeclarationResponse](#m-CashPaymentDeclarationResponse), [DeclareCashPaymentRequest](#m-DeclareCashPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `declareCashPayment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-bookings-bookingnumber-reschedule"></a>
### POST /sni/api/v1/client/bookings/{bookingNumber}/reschedule

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientBookingController.ClientRescheduleRequest`
- Réponse : `ClientBookingResponse`
- Modèles : [ClientBookingResponse](#m-ClientBookingResponse), [ClientBookingController.ClientRescheduleRequest](#m-ClientBookingController-ClientRescheduleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `reschedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-bookings-bookingnumber-reschedule-preview"></a>
### GET /sni/api/v1/client/bookings/{bookingNumber}/reschedule/preview

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingChangePreviewResponse`
- Modèles : [BookingChangePreviewResponse](#m-BookingChangePreviewResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingController.java`, méthode `previewReschedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `newStartedAt` | requête | `LocalDateTime` | oui |  |
| `newEndedAt` | requête | `LocalDateTime` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientBookingSummaryResponse"></a>
#### ClientBookingSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `resourceTypeCode` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED` |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `durationMinutes` | entier |  |  |
| `quantity` | entier |  |  |
| `seatsLabel` | texte |  |  |
| `resourceCapacity` | entier |  |  |
| `wholeResourceBooking` | booléen |  |  |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `paymentStatus` | enum : `COVERED`, `PENDING`, `PAID`, `CANCELLED` |  |  |
| `paymentStatusLabel` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `confirmedAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingStatus"></a>
#### BookingStatus

Énumération : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED`

<a id="m-ClientBookingResponse"></a>
#### ClientBookingResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `resourceName` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED` |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `durationMinutes` | entier |  |  |
| `quantity` | entier |  |  |
| `seatsLabel` | texte |  |  |
| `resourceCapacity` | entier |  |  |
| `wholeResourceBooking` | booléen |  |  |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `paymentStatus` | enum : `COVERED`, `PENDING`, `PAID`, `CANCELLED` |  |  |
| `paymentStatusLabel` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `passNumber` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `notes` | texte |  |  |
| `checkInToken` | texte |  |  |
| `checkInQrValue` | texte |  |  |
| `virtualMeetingUrl` | texte |  |  |
| `confirmedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `participants` | [BookingParticipantResponse](#m-BookingParticipantResponse)[] |  |  |

<a id="m-ClientCreateBookingRequest"></a>
#### ClientCreateBookingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier |  | min `1` |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` | oui |  |
| `notes` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `participants` | [BookingParticipantRequest](#m-BookingParticipantRequest)[] |  |  |
| `billingCompanyCode` | texte |  |  |
| `payOnSite` | booléen |  |  |

<a id="m-BookingSuggestionResponse"></a>
#### BookingSuggestionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `remainingCapacity` | entier |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `reason` | texte |  |  |

<a id="m-BookingAvailabilityResponse"></a>
#### BookingAvailabilityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `durationMinutes` | entier |  |  |
| `quantity` | entier |  |  |
| `available` | booléen |  |  |
| `remainingCapacity` | entier |  |  |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `unitPrice` | nombre décimal |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `message` | texte |  |  |
| `occupiedRanges` | [BookingAvailabilityResponse.OccupiedRange](#m-BookingAvailabilityResponse-OccupiedRange)[] |  |  |

<a id="m-ClientCheckAvailabilityRequest"></a>
#### ClientCheckAvailabilityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier |  | min `1` |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `billingCompanyCode` | texte |  |  |

<a id="m-ClientPortalBookingService-LastMinuteOffer"></a>
#### ClientPortalBookingService.LastMinuteOffer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `price` | nombre décimal |  |  |
| `discountedPrice` | nombre décimal |  |  |
| `discountRate` | entier |  |  |
| `currency` | texte |  |  |

<a id="m-BookingPayOnSite-Access"></a>
#### BookingPayOnSite.Access

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `allowed` | booléen |  |  |
| `source` | enum : `CLIENT_OPTION`, `GLOBAL_SETTING` |  |  |
| `reason` | texte |  |  |

<a id="m-ClientCancelBookingRequest"></a>
#### ClientCancelBookingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-ClientBookingController-ClientExtendRequest"></a>
#### ClientBookingController.ClientExtendRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `minutes` | entier |  | min `1`, max `1440` |
| `acceptPriceChange` | booléen |  |  |

<a id="m-CashPaymentDeclarationResponse"></a>
#### CashPaymentDeclarationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `declarationNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `bookingNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `AWAITING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, `EXPIRED` |  |  |
| `note` | texte |  |  |
| `declaredAt` | date-heure ISO 8601 UTC |  |  |
| `declaredBy` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `confirmedAt` | date-heure ISO 8601 UTC |  |  |
| `confirmedBy` | texte |  |  |
| `confirmedAmount` | nombre décimal |  |  |
| `cashSessionNumber` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `closedBy` | texte |  |  |
| `closeReason` | texte |  |  |
| `confirmationNote` | texte |  |  |

<a id="m-DeclareCashPaymentRequest"></a>
#### DeclareCashPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal |  |  |
| `note` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-ClientBookingController-ClientRescheduleRequest"></a>
#### ClientBookingController.ClientRescheduleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `newStartedAt` | date-heure locale sans fuseau | oui |  |
| `newEndedAt` | date-heure locale sans fuseau | oui |  |
| `acceptPriceChange` | booléen |  |  |

<a id="m-BookingChangePreviewResponse"></a>
#### BookingChangePreviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `currentAmount` | nombre décimal |  |  |
| `newAmount` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `change` | texte |  |  |
| `currency` | texte |  |  |
| `invoiced` | booléen |  |  |
| `modificationCount` | entier |  |  |
| `lastModifiedAt` | date-heure ISO 8601 UTC |  |  |
| `lastModification` | texte |  |  |

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

<a id="m-BookingParticipantRequest"></a>
#### BookingParticipantRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCode` | texte |  |  |
| `name` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `role` | enum : `HOST`, `GUEST`, `ORGANIZER`, `MEMBER` |  |  |

<a id="m-BookingAvailabilityResponse-OccupiedRange"></a>
#### BookingAvailabilityResponse.OccupiedRange

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `reason` | texte |  |  |
| `bookingNumbers` | texte[] |  |  |
