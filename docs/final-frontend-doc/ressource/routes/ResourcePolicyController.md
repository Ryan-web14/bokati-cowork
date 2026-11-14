# ResourcePolicyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-policies"></a>
### GET /sni/api/v1/resource-policies

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourcePolicyResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourcePolicyResponse](#m-ResourcePolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-resource-policies"></a>
### POST /sni/api/v1/resource-policies

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_POLICY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateResourcePolicyRequest`
- Réponse : `vide`
- Modèles : [CreateResourcePolicyRequest](#m-CreateResourcePolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `create`

<a id="r-get-sni-api-v1-resource-policies-search"></a>
### GET /sni/api/v1/resource-policies/search

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourcePolicyResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourcePolicyResponse](#m-ResourcePolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-policies-search-basic"></a>
### GET /sni/api/v1/resource-policies/search/basic

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourcePolicyResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourcePolicyResponse](#m-ResourcePolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-resource-policies-code"></a>
### DELETE /sni/api/v1/resource-policies/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-policies-code"></a>
### GET /sni/api/v1/resource-policies/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourcePolicyResponse`
- Modèles : [ResourcePolicyResponse](#m-ResourcePolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resource-policies-code"></a>
### PUT /sni/api/v1/resource-policies/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_POLICY_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateResourcePolicyRequest`
- Réponse : `vide`
- Modèles : [UpdateResourcePolicyRequest](#m-UpdateResourcePolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resource-policies-code-allow-cancellation"></a>
### PATCH /sni/api/v1/resource-policies/{code}/allow-cancellation

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_POLICY_UPDATE_CANCELLATION`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `updateCancellationPolicy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `allowed` | requête | `Boolean` | oui |  |

<a id="r-patch-sni-api-v1-resource-policies-code-status"></a>
### PATCH /sni/api/v1/resource-policies/{code}/status

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_POLICY_UPDATE_STATUS`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePolicyController.java`, méthode `updateStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `active` | requête | `Boolean` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourcePolicyResponse"></a>
#### ResourcePolicyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `minBookingDurationMinutes` | entier |  |  |
| `maxBookingDurationMinutes` | entier |  |  |
| `minBookingNoticeMinutes` | entier |  |  |
| `cancellationNoticeMinutes` | entier |  |  |
| `allowCancellation` | booléen |  |  |
| `staffOverrideAllowed` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-CreateResourcePolicyRequest"></a>
#### CreateResourcePolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `minBookingDurationMinutes` | entier | oui |  |
| `maxBookingDurationMinutes` | entier | oui |  |
| `minBookingNoticeMinutes` | entier | oui |  |
| `cancellationNoticeMinutes` | entier |  |  |
| `allowCancellation` | booléen |  |  |
| `staffOverrideAllowed` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-UpdateResourcePolicyRequest"></a>
#### UpdateResourcePolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `minBookingDurationMinutes` | entier |  |  |
| `maxBookingDurationMinutes` | entier |  |  |
| `minBookingNoticeMinutes` | entier |  |  |
| `cancellationNoticeMinutes` | entier |  |  |
| `allowCancellation` | booléen |  |  |
| `staffOverrideAllowed` | booléen |  |  |
| `active` | booléen |  |  |
