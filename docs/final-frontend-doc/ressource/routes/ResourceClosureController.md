# ResourceClosureController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-closures"></a>
### GET /sni/api/v1/resource-closures

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `PaginatedResponse<ResourceClosureResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourceClosureResponse](#m-ResourceClosureResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceClosureController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-resource-closures"></a>
### POST /sni/api/v1/resource-closures

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_CLOSURE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateResourceClosureRequest`
- Réponse : `vide`
- Modèles : [CreateResourceClosureRequest](#m-CreateResourceClosureRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceClosureController.java`, méthode `create`

<a id="r-delete-sni-api-v1-resource-closures-id"></a>
### DELETE /sni/api/v1/resource-closures/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceClosureController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-resource-closures-id"></a>
### GET /sni/api/v1/resource-closures/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceClosureResponse`
- Modèles : [ResourceClosureResponse](#m-ResourceClosureResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceClosureController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-resource-closures-id-active"></a>
### PATCH /sni/api/v1/resource-closures/{id}/active

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_CLOSURE_UPDATE_ACTIVE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateActiveRequest`
- Réponse : `vide`
- Modèles : [UpdateActiveRequest](#m-UpdateActiveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceClosureController.java`, méthode `updateActive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceClosureResponse"></a>
#### ResourceClosureResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `resourceCode` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `active` | booléen |  |  |

<a id="m-CreateResourceClosureRequest"></a>
#### CreateResourceClosureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `startedAt` | date-heure locale sans fuseau | oui |  |
| `endedAt` | date-heure locale sans fuseau | oui |  |
| `reason` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-UpdateActiveRequest"></a>
#### UpdateActiveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `active` | booléen | oui |  |
