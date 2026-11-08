# ResourceAmenitiesController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-amenities"></a>
### GET /sni/api/v1/resource-amenities

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<AmenityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [AmenityResponse](#m-AmenityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=name,ASC |

<a id="r-post-sni-api-v1-resource-amenities"></a>
### POST /sni/api/v1/resource-amenities

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_AMENITY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateAmenityRequest`
- Réponse : `vide`
- Modèles : [CreateAmenityRequest](#m-CreateAmenityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `create`

<a id="r-get-sni-api-v1-resource-amenities-search"></a>
### GET /sni/api/v1/resource-amenities/search

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<AmenityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [AmenityResponse](#m-AmenityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-amenities-search-basic"></a>
### GET /sni/api/v1/resource-amenities/search/basic

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<AmenityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [AmenityResponse](#m-AmenityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-resource-amenities-code"></a>
### DELETE /sni/api/v1/resource-amenities/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-amenities-code"></a>
### GET /sni/api/v1/resource-amenities/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `AmenityResponse`
- Modèles : [AmenityResponse](#m-AmenityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resource-amenities-code"></a>
### PUT /sni/api/v1/resource-amenities/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_AMENITY_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateAmenityRequest`
- Réponse : `vide`
- Modèles : [UpdateAmenityRequest](#m-UpdateAmenityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceAmenitiesController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-CreateAmenityRequest"></a>
#### CreateAmenityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-UpdateAmenityRequest"></a>
#### UpdateAmenityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
