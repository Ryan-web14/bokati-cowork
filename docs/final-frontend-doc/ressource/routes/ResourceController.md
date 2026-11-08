# ResourceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 16 route(s).

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
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-resources"></a>
### POST /sni/api/v1/resources

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `ResourceRequest`
- Réponse : `vide`
- Modèles : [ResourceRequest](#m-ResourceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `create`

<a id="r-post-sni-api-v1-resources-amenities-link"></a>
### POST /sni/api/v1/resources/amenities/link

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_LINK_AMENITY`, voir le README racine)
- Corps JSON (obligatoire) : `LinkAmenityToResourceRequest`
- Réponse : `vide`
- Modèles : [LinkAmenityToResourceRequest](#m-LinkAmenityToResourceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `linkAmenity`

<a id="r-post-sni-api-v1-resources-search"></a>
### POST /sni/api/v1/resources/search

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
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
- Réponse : `List<ResourceSummaryResponse>`
- Modèles : [ResourceSummaryResponse](#m-ResourceSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-resources-code"></a>
### DELETE /sni/api/v1/resources/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

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

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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
| `typeId` | entier int64 ⚠ |  |  |
| `groupId` | entier int64 ⚠ |  |  |
| `policyId` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
| `zone` | texte |  |  |
| `locationLabel` | texte |  |  |
| `status` | enum : `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `OUT_OF_SERVICE`, `ARCHIVED` |  |  |
| `portalVisible` | booléen |  |  |
| `bookingEnabled` | booléen |  |  |
| `active` | booléen |  |  |
| `minCapacity` | entier |  |  |

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
| `status` | texte |  |  |
| `bookingEnabled` | booléen |  |  |
| `portalVisible` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |
| `slotDurationMinutes` | entier |  |  |

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

<a id="m-UpdatePortalVisibleRequest"></a>
#### UpdatePortalVisibleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `visible` | booléen | oui |  |

<a id="m-ResourceStatus"></a>
#### ResourceStatus

Énumération : `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `OUT_OF_SERVICE`, `ARCHIVED`
