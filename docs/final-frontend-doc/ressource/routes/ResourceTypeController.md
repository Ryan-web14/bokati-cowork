# ResourceTypeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-types"></a>
### GET /sni/api/v1/resource-types

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceTypeResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceTypeResponse](#m-ResourceTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-resource-types"></a>
### POST /sni/api/v1/resource-types

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_TYPE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateResourceTypeRequest`
- Réponse : `vide`
- Modèles : [CreateResourceTypeRequest](#m-CreateResourceTypeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `create`

<a id="r-get-sni-api-v1-resource-types-search"></a>
### GET /sni/api/v1/resource-types/search

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceTypeResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceTypeResponse](#m-ResourceTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-types-search-basic"></a>
### GET /sni/api/v1/resource-types/search/basic

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceTypeResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceTypeResponse](#m-ResourceTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-resource-types-code"></a>
### DELETE /sni/api/v1/resource-types/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-types-code"></a>
### GET /sni/api/v1/resource-types/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceTypeResponse`
- Modèles : [ResourceTypeResponse](#m-ResourceTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resource-types-code"></a>
### PUT /sni/api/v1/resource-types/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_TYPE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateResourceTypeRequest`
- Réponse : `vide`
- Modèles : [UpdateResourceTypeRequest](#m-UpdateResourceTypeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceTypeController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceTypeResponse"></a>
#### ResourceTypeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `bookableSlots` | entier |  |  |

<a id="m-CreateResourceTypeRequest"></a>
#### CreateResourceTypeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `bookableSlots` | entier |  |  |

<a id="m-UpdateResourceTypeRequest"></a>
#### UpdateResourceTypeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `bookableSlots` | entier |  |  |
