# ResourceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 23 route(s).

## Routes

<a id="r-get-sni-api-v1-resources"></a>
### GET /sni/api/v1/resources

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `summary` | requête | `boolean` |  | false |
| `typeCode` | requête | `String` |  |  |
| `groupCode` | requête | `String` |  |  |
| `policyCode` | requête | `String` |  |  |
| `siteCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-resources"></a>
### POST /sni/api/v1/resources

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `ResourceRequest`
- Réponse : `ResourceResponse`
- Modèles : [ResourceResponse](#m-ResourceResponse), [ResourceRequest](#m-ResourceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `create`

<a id="r-post-sni-api-v1-resources-amenities-link"></a>
### POST /sni/api/v1/resources/amenities/link

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_LINK_AMENITY`, voir le README racine)
- Corps JSON (obligatoire) : `LinkAmenityToResourceRequest`
- Réponse : `vide`
- Modèles : [LinkAmenityToResourceRequest](#m-LinkAmenityToResourceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `linkAmenity`

<a id="r-post-sni-api-v1-resources-find"></a>
### POST /sni/api/v1/resources/find

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Corps JSON (obligatoire) : `ResourceLocationRequests.Find`
- Réponse : `ResourceLocationResponses.FindResult`
- Modèles : [ResourceLocationResponses.FindResult](#m-ResourceLocationResponses-FindResult), [ResourceLocationRequests.Find](#m-ResourceLocationRequests-Find)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `find`

<a id="r-post-sni-api-v1-resources-search"></a>
### POST /sni/api/v1/resources/search

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Corps JSON (facultatif) : `ResourceSearchCriteria`
- Réponse : `PaginatedResponse<ResourceSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceSummaryResponse](#m-ResourceSummaryResponse), [ResourceSearchCriteria](#m-ResourceSearchCriteria)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-resources-search-basic"></a>
### GET /sni/api/v1/resources/search/basic

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceSummaryResponse](#m-ResourceSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-delete-sni-api-v1-resources-code"></a>
### DELETE /sni/api/v1/resources/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `cancelBookings` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-resources-code"></a>
### GET /sni/api/v1/resources/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceResponse`
- Modèles : [ResourceResponse](#m-ResourceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resources-code"></a>
### PUT /sni/api/v1/resources/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateResourceRequest`
- Réponse : `vide`
- Modèles : [UpdateResourceRequest](#m-UpdateResourceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resources-code-active"></a>
### PATCH /sni/api/v1/resources/{code}/active

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_UPDATE_ACTIVE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateActiveRequest`
- Réponse : `vide`
- Modèles : [UpdateActiveRequest](#m-UpdateActiveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `updateActive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resources-code-amenities"></a>
### GET /sni/api/v1/resources/{code}/amenities

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<AmenityResponse>`
- Modèles : [AmenityResponse](#m-AmenityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `listAmenities`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-resources-code-amenities-amenitycode"></a>
### DELETE /sni/api/v1/resources/{code}/amenities/{amenityCode}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `unlinkAmenity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `amenityCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resources-code-booking-enabled"></a>
### PATCH /sni/api/v1/resources/{code}/booking-enabled

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_UPDATE_BOOKING_ENABLED`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateBookingEnabledRequest`
- Réponse : `vide`
- Modèles : [UpdateBookingEnabledRequest](#m-UpdateBookingEnabledRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `updateBookingEnabled`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resources-code-calendar-ics"></a>
### GET /sni/api/v1/resources/{code}/calendar.ics

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `calendar`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resources-code-classification"></a>
### PATCH /sni/api/v1/resources/{code}/classification

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_CHANGE_CLASSIFICATION`, voir le README racine)
- Corps JSON (obligatoire) : `ChangeTypeRequest`
- Réponse : `vide`
- Modèles : [ChangeTypeRequest](#m-ChangeTypeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `changeClassification`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resources-code-display-order"></a>
### PATCH /sni/api/v1/resources/{code}/display-order

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_UPDATE_DISPLAY_ORDER`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateDisplayOrderRequest`
- Réponse : `vide`
- Modèles : [UpdateDisplayOrderRequest](#m-UpdateDisplayOrderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `updateDisplayOrder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resources-code-ical-token"></a>
### POST /sni/api/v1/resources/{code}/ical-token

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `icalToken`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resources-code-impacted-bookings"></a>
### GET /sni/api/v1/resources/{code}/impacted-bookings

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceBookingImpact.ImpactedBooking>`
- Modèles : [ResourceBookingImpact.ImpactedBooking](#m-ResourceBookingImpact-ImpactedBooking)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `impactedBookings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDateTime` |  |  |
| `to` | requête | `LocalDateTime` |  |  |

<a id="r-put-sni-api-v1-resources-code-placement"></a>
### PUT /sni/api/v1/resources/{code}/placement

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceLocationRequests.Placement`
- Réponse : `ResourceResponse`
- Modèles : [ResourceResponse](#m-ResourceResponse), [ResourceLocationRequests.Placement](#m-ResourceLocationRequests-Placement)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `place`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resources-code-portal-visible"></a>
### PATCH /sni/api/v1/resources/{code}/portal-visible

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_UPDATE_PORTAL_VISIBLE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdatePortalVisibleRequest`
- Réponse : `vide`
- Modèles : [UpdatePortalVisibleRequest](#m-UpdatePortalVisibleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `updatePortalVisible`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resources-code-sales-settings"></a>
### PUT /sni/api/v1/resources/{code}/sales-settings

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceController.SalesSettings`
- Réponse : `ResourceResponse`
- Modèles : [ResourceResponse](#m-ResourceResponse), [ResourceController.SalesSettings](#m-ResourceController-SalesSettings)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `salesSettings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resources-code-status"></a>
### PATCH /sni/api/v1/resources/{code}/status

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_UPDATE_STATUS`, voir le README racine)
- Réponse : `vide`
- Modèles : [ResourceStatus](#m-ResourceStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `updateStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `status` | requête | `ResourceStatus` | oui |  |
| `cancelBookings` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-resources-code-usage-settings"></a>
### PUT /sni/api/v1/resources/{code}/usage-settings

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceController.UsageSettings`
- Réponse : `ResourceResponse`
- Modèles : [ResourceResponse](#m-ResourceResponse), [ResourceController.UsageSettings](#m-ResourceController-UsageSettings)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `usageSettings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceResponse"></a>
#### ResourceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `typeCode` | texte |  |  |
| `groupCode` | texte |  |  |
| `policyCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `capacity` | entier |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `floorLabel` | texte |  |  |
| `siteCode` | texte |  |  |
| `floorCode` | texte |  |  |
| `planX` | nombre décimal |  |  |
| `planY` | nombre décimal |  |  |
| `includedParticipants` | entier |  |  |
| `extraParticipantPrice` | entier |  |  |
| `depositAmount` | entier |  |  |
| `usageRules` | texte |  |  |
| `endChecklist` | texte[] |  |  |
| `status` | texte |  |  |
| `bookingEnabled` | booléen |  |  |
| `portalVisible` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |
| `slotDurationMinutes` | entier |  |  |

<a id="m-ResourceRequest"></a>
#### ResourceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `typeId` | texte | oui |  |
| `groupId` | texte |  |  |
| `policyId` | texte |  |  |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `capacity` | entier |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `floorLabel` | texte |  |  |
| `status` | texte |  |  |
| `bookingEnabled` | booléen |  |  |
| `portalVisible` | booléen |  |  |

<a id="m-LinkAmenityToResourceRequest"></a>
#### LinkAmenityToResourceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `amenityCode` | texte | oui | longueur min `1` |
| `quantity` | entier |  |  |
| `optional` | booléen |  |  |
| `extraPrice` | entier |  |  |

<a id="m-ResourceLocationResponses-FindResult"></a>
#### ResourceLocationResponses.FindResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `matches` | [ResourceLocationResponses.Option](#m-ResourceLocationResponses-Option)[] |  |  |
| `alternatives` | [ResourceLocationResponses.Option](#m-ResourceLocationResponses-Option)[] |  |  |

<a id="m-ResourceLocationRequests-Find"></a>
#### ResourceLocationRequests.Find

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `participants` | entier |  |  |
| `amenityCodes` | texte[] |  |  |
| `siteCode` | texte |  |  |
| `typeCode` | texte |  |  |
| `maxBudget` | nombre décimal |  |  |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |

<a id="m-ResourceSummaryResponse"></a>
#### ResourceSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `typeCode` | texte |  |  |
| `name` | texte |  |  |
| `status` | texte |  |  |
| `bookingEnabled` | booléen |  |  |
| `portalVisible` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-ResourceSearchCriteria"></a>
#### ResourceSearchCriteria

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `typeId` | code identifiant (texte) |  |  |
| `groupId` | code identifiant (texte) |  |  |
| `policyId` | code identifiant (texte) |  |  |
| `typeCode` | texte |  |  |
| `groupCode` | texte |  |  |
| `policyCode` | texte |  |  |
| `name` | texte |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `siteCode` | texte |  |  |
| `floorCode` | texte |  |  |
| `status` | enum : `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `OUT_OF_SERVICE`, `ARCHIVED` |  |  |
| `portalVisible` | booléen |  |  |
| `bookingEnabled` | booléen |  |  |
| `active` | booléen |  |  |
| `minCapacity` | entier |  |  |

<a id="m-UpdateResourceRequest"></a>
#### UpdateResourceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `capacity` | entier |  |  |
| `zone` | texte |  |  |
| `floorLabel` | texte |  |  |
| `locationLabel` | texte |  |  |
| `bookingEnabled` | booléen |  |  |
| `portalVisible` | booléen |  |  |

<a id="m-UpdateActiveRequest"></a>
#### UpdateActiveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `active` | booléen | oui |  |
| `cancelBookings` | booléen |  |  |

<a id="m-AmenityResponse"></a>
#### AmenityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `quantity` | entier |  |  |
| `optional` | booléen |  |  |
| `extraPrice` | entier |  |  |

<a id="m-UpdateBookingEnabledRequest"></a>
#### UpdateBookingEnabledRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingEnabled` | booléen | oui |  |

<a id="m-ChangeTypeRequest"></a>
#### ChangeTypeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `typeId` | texte | oui |  |
| `groupId` | texte |  |  |
| `policyId` | texte |  |  |

<a id="m-UpdateDisplayOrderRequest"></a>
#### UpdateDisplayOrderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `displayOrder` | entier | oui |  |

<a id="m-ResourceBookingImpact-ImpactedBooking"></a>
#### ResourceBookingImpact.ImpactedBooking

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `status` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `contactName` | texte |  |  |
| `quantity` | entier |  |  |

<a id="m-ResourceLocationRequests-Placement"></a>
#### ResourceLocationRequests.Placement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `siteCode` | texte |  |  |
| `floorCode` | texte |  |  |
| `planX` | nombre décimal |  |  |
| `planY` | nombre décimal |  |  |

<a id="m-UpdatePortalVisibleRequest"></a>
#### UpdatePortalVisibleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `visible` | booléen | oui |  |

<a id="m-ResourceController-SalesSettings"></a>
#### ResourceController.SalesSettings

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `includedParticipants` | entier |  |  |
| `extraParticipantPrice` | entier |  |  |
| `depositAmount` | entier |  |  |

<a id="m-ResourceStatus"></a>
#### ResourceStatus

Énumération : `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `OUT_OF_SERVICE`, `ARCHIVED`

<a id="m-ResourceController-UsageSettings"></a>
#### ResourceController.UsageSettings

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `usageRules` | texte |  |  |
| `endChecklist` | texte[] |  |  |

<a id="m-ResourceLocationResponses-Option"></a>
#### ResourceLocationResponses.Option

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `name` | texte |  |  |
| `typeCode` | texte |  |  |
| `siteCode` | texte |  |  |
| `floorCode` | texte |  |  |
| `capacity` | entier |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `price` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `notes` | texte[] |  |  |
