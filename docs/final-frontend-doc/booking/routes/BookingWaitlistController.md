# BookingWaitlistController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings-waitlist"></a>
### GET /sni/api/v1/bookings/waitlist

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingWaitlistEntryResponse>`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse), [BookingWaitlistStatus](#m-BookingWaitlistStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingWaitlistController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `BookingWaitlistStatus` |  |  |

<a id="r-post-sni-api-v1-bookings-waitlist"></a>
### POST /sni/api/v1/bookings/waitlist

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `JoinBookingWaitlistRequest`
- Réponse : `BookingWaitlistEntryResponse`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse), [JoinBookingWaitlistRequest](#m-JoinBookingWaitlistRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingWaitlistController.java`, méthode `join`

<a id="r-post-sni-api-v1-bookings-waitlist-expire-offers"></a>
### POST /sni/api/v1/bookings/waitlist/expire-offers

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingWaitlistController.java`, méthode `expireOffers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-post-sni-api-v1-bookings-waitlist-promote-available"></a>
### POST /sni/api/v1/bookings/waitlist/promote-available

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingWaitlistController.java`, méthode `promoteAvailable`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-patch-sni-api-v1-bookings-waitlist-id-cancel"></a>
### PATCH /sni/api/v1/bookings/waitlist/{id}/cancel

- Accès : back-office : rôle d'administration et droit `BOOKING:CANCEL`
- Réponse : `BookingWaitlistEntryResponse`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingWaitlistController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingWaitlistEntryResponse"></a>
#### BookingWaitlistEntryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | texte |  |  |
| `contactPhone` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `quantity` | entier |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` |  |  |
| `status` | enum : `WAITING`, `OFFERED`, `BOOKED`, `CANCELLED`, `EXPIRED` |  |  |
| `offeredAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `alternatives` | [BookingSuggestionResponse](#m-BookingSuggestionResponse)[] |  |  |

<a id="m-BookingWaitlistStatus"></a>
#### BookingWaitlistStatus

Énumération : `WAITING`, `OFFERED`, `BOOKED`, `CANCELLED`, `EXPIRED`

<a id="m-JoinBookingWaitlistRequest"></a>
#### JoinBookingWaitlistRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `identityLookup` | [BookingIdentityLookup](#m-BookingIdentityLookup) |  |  |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier |  |  |
| `paymentMode` | enum : `DIRECT`, `WALLET`, `SUBSCRIPTION`, `PASS` | oui |  |

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

<a id="m-BookingIdentityLookup"></a>
#### BookingIdentityLookup

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `businessCode` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `walkIn` | booléen |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | texte |  |  |
| `contactPhone` | texte |  |  |
