# ResourceGroupController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-groups"></a>
### GET /sni/api/v1/resource-groups

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceGroupResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceGroupResponse](#m-ResourceGroupResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `list`

<a id="r-post-sni-api-v1-resource-groups"></a>
### POST /sni/api/v1/resource-groups

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_GROUP_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateResourceGroupRequest`
- Réponse : `vide`
- Modèles : [CreateResourceGroupRequest](#m-CreateResourceGroupRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `create`

<a id="r-get-sni-api-v1-resource-groups-search"></a>
### GET /sni/api/v1/resource-groups/search

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceGroupResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceGroupResponse](#m-ResourceGroupResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-groups-search-basic"></a>
### GET /sni/api/v1/resource-groups/search/basic

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceGroupResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceGroupResponse](#m-ResourceGroupResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-resource-groups-code"></a>
### DELETE /sni/api/v1/resource-groups/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-groups-code"></a>
### GET /sni/api/v1/resource-groups/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceGroupResponse`
- Modèles : [ResourceGroupResponse](#m-ResourceGroupResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resource-groups-code"></a>
### PUT /sni/api/v1/resource-groups/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_GROUP_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateResourceGroupRequest`
- Réponse : `vide`
- Modèles : [UpdateResourceGroupRequest](#m-UpdateResourceGroupRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceGroupController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceGroupResponse"></a>
#### ResourceGroupResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `portalVisible` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-CreateResourceGroupRequest"></a>
#### CreateResourceGroupRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `portalVisible` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-UpdateResourceGroupRequest"></a>
#### UpdateResourceGroupRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `portalVisible` | booléen |  |  |
| `active` | booléen |  |  |
