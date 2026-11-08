# BookingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 37 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings"></a>
### GET /sni/api/v1/bookings

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `PaginatedResponse<BookingResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BookingResponse](#m-BookingResponse), [SubscriberType](#m-SubscriberType), [BookingStatus](#m-BookingStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` |  |  |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `resourceCode` | requête | `String` |  |  |
| `status` | requête | `BookingStatus` |  |  |
| `startedFrom` | requête | `LocalDateTime` |  |  |
| `startedTo` | requête | `LocalDateTime` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=startedAt,DESC |

<a id="r-post-sni-api-v1-bookings"></a>
### POST /sni/api/v1/bookings

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `CreateBookingRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [CreateBookingRequest](#m-CreateBookingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `create`

<a id="r-get-sni-api-v1-bookings-cancellation-policy"></a>
### GET /sni/api/v1/bookings/cancellation-policy

- Accès : back-office : rôle d'administration et droit `BOOKING:CANCEL`
- Réponse : `List<CancellationPolicyResponse>`
- Modèles : [CancellationPolicyResponse](#m-CancellationPolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `cancellationPolicy`

<a id="r-post-sni-api-v1-bookings-check-availability"></a>
### POST /sni/api/v1/bookings/check-availability

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingAvailabilityRequest`
- Réponse : `BookingAvailabilityResponse`
- Modèles : [BookingAvailabilityResponse](#m-BookingAvailabilityResponse), [BookingAvailabilityRequest](#m-BookingAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `checkAvailability`

<a id="r-patch-sni-api-v1-bookings-check-in-qr-checkintoken"></a>
### PATCH /sni/api/v1/bookings/check-in/qr/{checkInToken}

- Accès : back-office : rôle d'administration et droit `BOOKING:CHECKIN`
- Corps JSON (facultatif) : `BookingCheckRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingCheckRequest](#m-BookingCheckRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `checkInByToken`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `checkInToken` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-holds"></a>
### POST /sni/api/v1/bookings/holds

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `CreateBookingHoldRequest`
- Réponse : `BookingHoldResponse`
- Modèles : [BookingHoldResponse](#m-BookingHoldResponse), [CreateBookingHoldRequest](#m-CreateBookingHoldRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `createHold`

<a id="r-post-sni-api-v1-bookings-holds-expire-due"></a>
### POST /sni/api/v1/bookings/holds/expire-due

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `expireHolds`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-patch-sni-api-v1-bookings-holds-holdnumber-release"></a>
### PATCH /sni/api/v1/bookings/holds/{holdNumber}/release

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Réponse : `BookingHoldResponse`
- Modèles : [BookingHoldResponse](#m-BookingHoldResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `releaseHold`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `holdNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-metrics-overview"></a>
### GET /sni/api/v1/bookings/metrics/overview

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingMetricsOverviewResponse`
- Modèles : [BookingMetricsOverviewResponse](#m-BookingMetricsOverviewResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `metrics`

<a id="r-post-sni-api-v1-bookings-notifications-dispatch-due"></a>
### POST /sni/api/v1/bookings/notifications/dispatch-due

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `dispatchDueNotifications`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-get-sni-api-v1-bookings-policies"></a>
### GET /sni/api/v1/bookings/policies

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingAudiencePolicyResponse>`
- Modèles : [BookingAudiencePolicyResponse](#m-BookingAudiencePolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `listPolicies`

<a id="r-post-sni-api-v1-bookings-policies"></a>
### POST /sni/api/v1/bookings/policies

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `CreateBookingAudiencePolicyRequest`
- Réponse : `BookingAudiencePolicyResponse`
- Modèles : [BookingAudiencePolicyResponse](#m-BookingAudiencePolicyResponse), [CreateBookingAudiencePolicyRequest](#m-CreateBookingAudiencePolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `createPolicy`

<a id="r-get-sni-api-v1-bookings-quota-overrides"></a>
### GET /sni/api/v1/bookings/quota-overrides

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingQuotaOverrideResponse>`
- Modèles : [BookingQuotaOverrideResponse](#m-BookingQuotaOverrideResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `listQuotaOverrides`

<a id="r-post-sni-api-v1-bookings-quota-overrides"></a>
### POST /sni/api/v1/bookings/quota-overrides

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `CreateBookingQuotaOverrideRequest`
- Réponse : `BookingQuotaOverrideResponse`
- Modèles : [BookingQuotaOverrideResponse](#m-BookingQuotaOverrideResponse), [CreateBookingQuotaOverrideRequest](#m-CreateBookingQuotaOverrideRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `createQuotaOverride`

<a id="r-post-sni-api-v1-bookings-recurring"></a>
### POST /sni/api/v1/bookings/recurring

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `CreateRecurringBookingRequest`
- Réponse : `List<BookingResponse>`
- Modèles : [BookingResponse](#m-BookingResponse), [CreateRecurringBookingRequest](#m-CreateRecurringBookingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `createRecurring`

<a id="r-post-sni-api-v1-bookings-repair"></a>
### POST /sni/api/v1/bookings/repair

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `BookingRepairResponse`
- Modèles : [BookingRepairResponse](#m-BookingRepairResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `repair`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-get-sni-api-v1-bookings-search"></a>
### GET /sni/api/v1/bookings/search

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingResponse>`
- Modèles : [BookingResponse](#m-BookingResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-suggestions"></a>
### POST /sni/api/v1/bookings/suggestions

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingAvailabilityRequest`
- Réponse : `List<BookingSuggestionResponse>`
- Modèles : [BookingSuggestionResponse](#m-BookingSuggestionResponse), [BookingAvailabilityRequest](#m-BookingAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `suggestions`

<a id="r-get-sni-api-v1-bookings-bookingnumber"></a>
### GET /sni/api/v1/bookings/{bookingNumber}

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-approve"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/approve

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (facultatif) : `BookingApprovalRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingApprovalRequest](#m-BookingApprovalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-cancel"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/cancel

- Accès : back-office : rôle d'administration et droit `BOOKING:CANCEL`
- Corps JSON (facultatif) : `BookingStatusChangeRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingStatusChangeRequest](#m-BookingStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-change-resource"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/change-resource

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingChangeResourceRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingChangeResourceRequest](#m-BookingChangeResourceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `changeResource`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-check-in"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/check-in

- Accès : back-office : rôle d'administration et droit `BOOKING:CHECKIN`
- Corps JSON (facultatif) : `BookingCheckRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingCheckRequest](#m-BookingCheckRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `checkIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-check-out"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/check-out

- Accès : back-office : rôle d'administration et droit `BOOKING:CHECKIN`
- Corps JSON (facultatif) : `BookingCheckRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingCheckRequest](#m-BookingCheckRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `checkOut`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-complete"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/complete

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (facultatif) : `BookingStatusChangeRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingStatusChangeRequest](#m-BookingStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `complete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-confirm"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/confirm

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (facultatif) : `BookingStatusChangeRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingStatusChangeRequest](#m-BookingStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `confirm`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-early-check-in"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/early-check-in

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (facultatif) : `BookingCheckRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingCheckRequest](#m-BookingCheckRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `earlyCheckIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-bookingnumber-events"></a>
### GET /sni/api/v1/bookings/{bookingNumber}/events

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingEventResponse>`
- Modèles : [BookingEventResponse](#m-BookingEventResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `events`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-bookingnumber-history"></a>
### GET /sni/api/v1/bookings/{bookingNumber}/history

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingStatusHistoryResponse>`
- Modèles : [BookingStatusHistoryResponse](#m-BookingStatusHistoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-no-show"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/no-show

- Accès : back-office : rôle d'administration et droit `BOOKING:CANCEL`
- Corps JSON (facultatif) : `BookingStatusChangeRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingStatusChangeRequest](#m-BookingStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `noShow`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-bookingnumber-notifications-reminder"></a>
### POST /sni/api/v1/bookings/{bookingNumber}/notifications/reminder

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `queueReminder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `minutesBefore` | requête | `int` |  | 30 |

<a id="r-post-sni-api-v1-bookings-bookingnumber-participants"></a>
### POST /sni/api/v1/bookings/{bookingNumber}/participants

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingParticipantRequest`
- Réponse : `BookingParticipantResponse`
- Modèles : [BookingParticipantResponse](#m-BookingParticipantResponse), [BookingParticipantRequest](#m-BookingParticipantRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `addParticipant`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-bookings-bookingnumber-participants-participantid"></a>
### DELETE /sni/api/v1/bookings/{bookingNumber}/participants/{participantId}

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Réponse : `BookingParticipantResponse`
- Modèles : [BookingParticipantResponse](#m-BookingParticipantResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `removeParticipant`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `participantId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-reject"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/reject

- Accès : back-office : rôle d'administration et droit `BOOKING:CANCEL`
- Corps JSON (facultatif) : `BookingApprovalRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingApprovalRequest](#m-BookingApprovalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-reschedule"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/reschedule

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingRescheduleRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingRescheduleRequest](#m-BookingRescheduleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `reschedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-start"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/start

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (facultatif) : `BookingStatusChangeRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingStatusChangeRequest](#m-BookingStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `start`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-bookings-bookingnumber-transfer"></a>
### PATCH /sni/api/v1/bookings/{bookingNumber}/transfer

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingTransferRequest`
- Réponse : `BookingResponse`
- Modèles : [BookingResponse](#m-BookingResponse), [BookingTransferRequest](#m-BookingTransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java`, méthode `transfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |

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

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-BookingStatus"></a>
#### BookingStatus

Énumération : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED`

<a id="m-CreateBookingRequest"></a>
#### CreateBookingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `businessCode` | texte |  |  |
| `email` | string (email) |  |  |
| `phone` | texte |  |  |
| `walkIn` | booléen |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | string (email) |  |  |
| `contactPhone` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `holdNumber` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier |  | min `1` |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` | oui |  |
| `confirmImmediately` | booléen |  |  |
| `sendEmail` | booléen |  |  |
| `notes` | texte |  |  |
| `metadataJson` | texte |  |  |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `participants` | [BookingParticipantRequest](#m-BookingParticipantRequest)[] |  |  |
| `billingCompanyCode` | texte |  |  |

<a id="m-CancellationPolicyResponse"></a>
#### CancellationPolicyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `hoursBeforeStart` | entier |  |  |
| `refundPercentage` | nombre décimal |  |  |
| `ruleOrder` | entier |  |  |
| `active` | booléen |  |  |

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

<a id="m-BookingAvailabilityRequest"></a>
#### BookingAvailabilityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `businessCode` | texte |  |  |
| `email` | string (email) |  |  |
| `phone` | texte |  |  |
| `walkIn` | booléen |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | string (email) |  |  |
| `contactPhone` | texte |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier |  | min `1` |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `billingCompanyCode` | texte |  |  |

<a id="m-BookingCheckRequest"></a>
#### BookingCheckRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `actor` | texte |  |  |
| `note` | texte |  |  |
| `sendEmail` | booléen |  |  |

<a id="m-BookingHoldResponse"></a>
#### BookingHoldResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `holdNumber` | texte |  |  |
| `resourceCode` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `quantity` | entier |  |  |
| `status` | enum : `ACTIVE`, `CONFIRMED`, `RELEASED`, `EXPIRED` |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateBookingHoldRequest"></a>
#### CreateBookingHoldRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `businessCode` | texte |  |  |
| `email` | string (email) |  |  |
| `phone` | texte |  |  |
| `walkIn` | booléen |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | string (email) |  |  |
| `contactPhone` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier |  | min `1` |
| `ttlMinutes` | entier |  |  |
| `idempotencyKey` | texte |  |  |

<a id="m-BookingMetricsOverviewResponse"></a>
#### BookingMetricsOverviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `draftBookings` | entier int64 ⚠ |  |  |
| `pendingApprovalBookings` | entier int64 ⚠ |  |  |
| `confirmedBookings` | entier int64 ⚠ |  |  |
| `completedBookings` | entier int64 ⚠ |  |  |
| `cancelledBookings` | entier int64 ⚠ |  |  |
| `noShowBookings` | entier int64 ⚠ |  |  |
| `activeHolds` | entier int64 ⚠ |  |  |
| `confirmedAmount` | nombre décimal |  |  |
| `completedAmount` | nombre décimal |  |  |

<a id="m-BookingAudiencePolicyResponse"></a>
#### BookingAudiencePolicyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `policyNumber` | texte |  |  |
| `audienceType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `resourceTypeCode` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `approvalRequired` | booléen |  |  |
| `maxActiveBookings` | entier |  |  |
| `maxBookingsPerDay` | entier |  |  |
| `maxBookingsPerWeek` | entier |  |  |
| `maxBookingsPerMonth` | entier |  |  |
| `maxNoShowsPerMonth` | entier |  |  |
| `minBookingNoticeMinutes` | entier |  |  |
| `maxBookingDurationMinutes` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-CreateBookingAudiencePolicyRequest"></a>
#### CreateBookingAudiencePolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `audienceType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `resourceTypeCode` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `approvalRequired` | booléen |  |  |
| `maxActiveBookings` | entier |  |  |
| `maxBookingsPerDay` | entier |  |  |
| `maxBookingsPerWeek` | entier |  |  |
| `maxBookingsPerMonth` | entier |  |  |
| `maxNoShowsPerMonth` | entier |  |  |
| `minBookingNoticeMinutes` | entier |  |  |
| `maxBookingDurationMinutes` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-BookingQuotaOverrideResponse"></a>
#### BookingQuotaOverrideResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `overrideNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `resourceCode` | texte |  |  |
| `extraActiveBookings` | entier |  |  |
| `extraBookingsPerDay` | entier |  |  |
| `extraBookingsPerWeek` | entier |  |  |
| `extraBookingsPerMonth` | entier |  |  |
| `reason` | texte |  |  |
| `approvedBy` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `active` | booléen |  |  |

<a id="m-CreateBookingQuotaOverrideRequest"></a>
#### CreateBookingQuotaOverrideRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `businessCode` | texte |  |  |
| `email` | string (email) |  |  |
| `phone` | texte |  |  |
| `resourceCode` | texte |  |  |
| `extraActiveBookings` | entier |  |  |
| `extraBookingsPerDay` | entier |  |  |
| `extraBookingsPerWeek` | entier |  |  |
| `extraBookingsPerMonth` | entier |  |  |
| `reason` | texte |  |  |
| `approvedBy` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateRecurringBookingRequest"></a>
#### CreateRecurringBookingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `booking` | [CreateBookingRequest](#m-CreateBookingRequest) | oui |  |
| `frequency` | enum : `DAILY`, `WEEKLY`, `MONTHLY` | oui |  |
| `intervalValue` | entier |  | min `1` |
| `occurrences` | entier |  | min `2`, max `60` |
| `endDate` | date AAAA-MM-JJ |  |  |
| `excludedDates` | date AAAA-MM-JJ[] |  |  |

<a id="m-BookingRepairResponse"></a>
#### BookingRepairResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `inspected` | entier |  |  |
| `repaired` | entier |  |  |
| `failed` | entier |  |  |

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

<a id="m-BookingApprovalRequest"></a>
#### BookingApprovalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `actor` | texte |  |  |
| `reason` | texte |  |  |
| `sendEmail` | booléen |  |  |

<a id="m-BookingStatusChangeRequest"></a>
#### BookingStatusChangeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `changedBy` | texte |  |  |
| `reason` | texte |  |  |
| `sendEmail` | booléen |  |  |

<a id="m-BookingChangeResourceRequest"></a>
#### BookingChangeResourceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `newResourceCode` | texte | oui | longueur min `1` |
| `actor` | texte |  |  |
| `note` | texte |  |  |
| `sendEmail` | booléen |  |  |

<a id="m-BookingEventResponse"></a>
#### BookingEventResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventNumber` | texte |  |  |
| `bookingNumber` | texte |  |  |
| `eventType` | enum : `BOOKING_CREATED`, `BOOKING_HOLD_CREATED`, `BOOKING_HOLD_EXPIRED`, `BOOKING_CONFIRMED`, `BOOKING_APPROVED`, `BOOKING_REJECTED`, `BOOKING_STARTED`, `BOOKING_CHECKED_IN`, `BOOKING_CHECKED_OUT`, `BOOKING_COMPLETED`, `BOOKING_CANCELLED`, `BOOKING_NO_SHOW`, `RESOURCE_RESERVED`, `RESOURCE_RELEASED`, `ENTITLEMENT_RESERVED`, `ENTITLEMENT_CONSUMED`, `ENTITLEMENT_RELEASED`, `USAGE_RECORDED`, `EMAIL_QUEUED`, `EMAIL_SENT`, `EMAIL_FAILED`, `REPAIR_APPLIED`, `QUOTA_OVERRIDE_APPLIED`, `BOOKING_RESCHEDULED`, `BOOKING_TRANSFERRED`, `BOOKING_RESOURCE_CHANGED` |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `payloadJson` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingStatusHistoryResponse"></a>
#### BookingStatusHistoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fromStatus` | enum : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED` |  |  |
| `toStatus` | enum : `DRAFT`, `PENDING_APPROVAL`, `PENDING_PAYMENT`, `REJECTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`, `EXPIRED` |  |  |
| `changedBy` | texte |  |  |
| `reason` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingParticipantResponse"></a>
#### BookingParticipantResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberCode` | texte |  |  |
| `name` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `role` | enum : `HOST`, `GUEST`, `ORGANIZER`, `MEMBER` |  |  |
| `status` | enum : `INVITED`, `CONFIRMED`, `DECLINED`, `REMOVED` |  |  |

<a id="m-BookingParticipantRequest"></a>
#### BookingParticipantRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCode` | texte |  |  |
| `name` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `role` | enum : `HOST`, `GUEST`, `ORGANIZER`, `MEMBER` |  |  |

<a id="m-BookingRescheduleRequest"></a>
#### BookingRescheduleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `newStartedAt` | date-heure locale sans fuseau | oui |  |
| `newEndedAt` | date-heure locale sans fuseau | oui |  |
| `actor` | texte |  |  |
| `note` | texte |  |  |
| `sendEmail` | booléen |  |  |

<a id="m-BookingTransferRequest"></a>
#### BookingTransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `newOwnerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `newOwnerCode` | texte | oui | longueur min `1` |
| `newContactName` | texte |  |  |
| `newContactEmail` | texte |  |  |
| `newContactPhone` | texte |  |  |
| `actor` | texte |  |  |
| `note` | texte |  |  |
| `sendEmail` | booléen |  |  |

<a id="m-BookingLineResponse"></a>
#### BookingLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `lineType` | enum : `RESOURCE`, `ENTITLEMENT`, `BILLABLE` |  |  |
| `description` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |  |  |
| `unitPrice` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `billableNumber` | texte |  |  |
