# ResourceAvailabilityController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-availabilities"></a>
### GET /sni/api/v1/resource-availabilities

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceAvailabilityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceAvailabilityResponse](#m-ResourceAvailabilityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=50, sort=startedAt,ASC |

<a id="r-post-sni-api-v1-resource-availabilities"></a>
### POST /sni/api/v1/resource-availabilities

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_AVAILABILITY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateResourceAvailabilityRequest`
- Réponse : `vide`
- Modèles : [CreateResourceAvailabilityRequest](#m-CreateResourceAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `create`

<a id="r-post-sni-api-v1-resource-availabilities-bulk"></a>
### POST /sni/api/v1/resource-availabilities/bulk

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `BulkCreateResourceAvailabilityRequest`
- Réponse : `BulkResourceOperationResponse`
- Modèles : [BulkResourceOperationResponse](#m-BulkResourceOperationResponse), [BulkCreateResourceAvailabilityRequest](#m-BulkCreateResourceAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `createBulk`

<a id="r-get-sni-api-v1-resource-availabilities-grouped"></a>
### GET /sni/api/v1/resource-availabilities/grouped

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceAvailabilityGroupResponse>`
- Modèles : [ResourceAvailabilityGroupResponse](#m-ResourceAvailabilityGroupResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `listGroupedByResource`

<a id="r-patch-sni-api-v1-resource-availabilities-release"></a>
### PATCH /sni/api/v1/resource-availabilities/release

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_AVAILABILITY_RELEASE`, voir le README racine)
- Corps JSON (obligatoire) : `ReleaseResourceAvailabilityRequest`
- Réponse : `vide`
- Modèles : [ReleaseResourceAvailabilityRequest](#m-ReleaseResourceAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `release`

<a id="r-get-sni-api-v1-resource-availabilities-remaining-slots"></a>
### GET /sni/api/v1/resource-availabilities/remaining-slots

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceAvailabilityWindowResponse>`
- Modèles : [ResourceAvailabilityWindowResponse](#m-ResourceAvailabilityWindowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `remaining`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` | oui |  |
| `startedAt` | requête | `LocalDateTime` |  |  |
| `endedAt` | requête | `LocalDateTime` |  |  |
| `durationMinutes` | requête | `Integer` |  |  |
| `quantity` | requête | `Integer` |  | 1 |

<a id="r-post-sni-api-v1-resource-availabilities-reserve"></a>
### POST /sni/api/v1/resource-availabilities/reserve

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_AVAILABILITY_RESERVE`, voir le README racine)
- Corps JSON (obligatoire) : `ReserveResourceAvailabilityRequest`
- Réponse : `vide`
- Modèles : [ReserveResourceAvailabilityRequest](#m-ReserveResourceAvailabilityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAvailabilityController.java`, méthode `reserve`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceAvailabilityResponse"></a>
#### ResourceAvailabilityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `resourceCode` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `slotDurationMinutes` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `available` | booléen |  |  |
| `active` | booléen |  |  |
| `subscribersOnly` | booléen |  |  |

<a id="m-CreateResourceAvailabilityRequest"></a>
#### CreateResourceAvailabilityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `capacity` | entier |  |  |
| `active` | booléen |  |  |
| `slotDurationMinutes` | entier |  |  |

<a id="m-BulkResourceOperationResponse"></a>
#### BulkResourceOperationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `skipped` | entier |  |  |
| `results` | [BulkResourceOperationResponse.Entry](#m-BulkResourceOperationResponse-Entry)[] |  |  |

<a id="m-BulkCreateResourceAvailabilityRequest"></a>
#### BulkCreateResourceAvailabilityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCodes` | texte[] | oui | éléments min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `capacity` | entier |  |  |
| `active` | booléen |  |  |
| `slotDurationMinutes` | entier |  |  |

<a id="m-ResourceAvailabilityGroupResponse"></a>
#### ResourceAvailabilityGroupResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `availabilities` | [ResourceAvailabilityResponse](#m-ResourceAvailabilityResponse)[] |  |  |

<a id="m-ReleaseResourceAvailabilityRequest"></a>
#### ReleaseResourceAvailabilityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier | oui |  |

<a id="m-ResourceAvailabilityWindowResponse"></a>
#### ResourceAvailabilityWindowResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `durationMinutes` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `slotCount` | entier |  |  |

<a id="m-ReserveResourceAvailabilityRequest"></a>
#### ReserveResourceAvailabilityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `quantity` | entier | oui |  |

<a id="m-BulkResourceOperationResponse-Entry"></a>
#### BulkResourceOperationResponse.Entry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `status` | texte |  |  |
| `reason` | texte |  |  |
| `detail` | entier |  |  |
