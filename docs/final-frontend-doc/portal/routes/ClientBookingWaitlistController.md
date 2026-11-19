# ClientBookingWaitlistController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-client-bookings-waitlist"></a>
### GET /sni/api/v1/client/bookings/waitlist

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<BookingWaitlistEntryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse), [BookingWaitlistStatus](#m-BookingWaitlistStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingWaitlistController.java`, méthode `mine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `BookingWaitlistStatus` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-post-sni-api-v1-client-bookings-waitlist"></a>
### POST /sni/api/v1/client/bookings/waitlist

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientBookingWaitlistController.JoinRequest`
- Réponse : `BookingWaitlistEntryResponse`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse), [ClientBookingWaitlistController.JoinRequest](#m-ClientBookingWaitlistController-JoinRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingWaitlistController.java`, méthode `join`

<a id="r-delete-sni-api-v1-client-bookings-waitlist-id"></a>
### DELETE /sni/api/v1/client/bookings/waitlist/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingWaitlistEntryResponse`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingWaitlistController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-post-sni-api-v1-client-bookings-waitlist-id-accept"></a>
### POST /sni/api/v1/client/bookings/waitlist/{id}/accept

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingWaitlistEntryResponse`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingWaitlistController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-post-sni-api-v1-client-bookings-waitlist-id-decline"></a>
### POST /sni/api/v1/client/bookings/waitlist/{id}/decline

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingWaitlistEntryResponse`
- Modèles : [BookingWaitlistEntryResponse](#m-BookingWaitlistEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingWaitlistController.java`, méthode `decline`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingWaitlistEntryResponse"></a>
#### BookingWaitlistEntryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
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
| `bookingNumber` | texte |  |  |
| `notifiedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingWaitlistStatus"></a>
#### BookingWaitlistStatus

Énumération : `WAITING`, `OFFERED`, `BOOKED`, `CANCELLED`, `EXPIRED`

<a id="m-ClientBookingWaitlistController-JoinRequest"></a>
#### ClientBookingWaitlistController.JoinRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
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
