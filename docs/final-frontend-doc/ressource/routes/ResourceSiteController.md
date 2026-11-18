# ResourceSiteController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 10 route(s).

## Routes

<a id="r-delete-sni-api-v1-resource-floors-code"></a>
### DELETE /sni/api/v1/resource-floors/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `deleteFloor`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resource-floors-code"></a>
### PATCH /sni/api/v1/resource-floors/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceLocationRequests.Floor`
- Réponse : `ResourceLocationResponses.Floor`
- Modèles : [ResourceLocationResponses.Floor](#m-ResourceLocationResponses-Floor), [ResourceLocationRequests.Floor](#m-ResourceLocationRequests-Floor)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `updateFloor`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-floors-code-plan"></a>
### GET /sni/api/v1/resource-floors/{code}/plan

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceLocationResponses.FloorPlan`
- Modèles : [ResourceLocationResponses.FloorPlan](#m-ResourceLocationResponses-FloorPlan)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `plan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `at` | requête | `LocalDateTime` |  |  |

<a id="r-get-sni-api-v1-resource-sites"></a>
### GET /sni/api/v1/resource-sites

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceLocationResponses.Site>`
- Modèles : [ResourceLocationResponses.Site](#m-ResourceLocationResponses-Site)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `list`

<a id="r-post-sni-api-v1-resource-sites"></a>
### POST /sni/api/v1/resource-sites

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceLocationRequests.Site`
- Réponse : `ResourceLocationResponses.Site`
- Modèles : [ResourceLocationResponses.Site](#m-ResourceLocationResponses-Site), [ResourceLocationRequests.Site](#m-ResourceLocationRequests-Site)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `create`

<a id="r-delete-sni-api-v1-resource-sites-code"></a>
### DELETE /sni/api/v1/resource-sites/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-sites-code"></a>
### GET /sni/api/v1/resource-sites/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceLocationResponses.Site`
- Modèles : [ResourceLocationResponses.Site](#m-ResourceLocationResponses-Site)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resource-sites-code"></a>
### PATCH /sni/api/v1/resource-sites/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceLocationRequests.Site`
- Réponse : `ResourceLocationResponses.Site`
- Modèles : [ResourceLocationResponses.Site](#m-ResourceLocationResponses-Site), [ResourceLocationRequests.Site](#m-ResourceLocationRequests-Site)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-sites-code-floors"></a>
### GET /sni/api/v1/resource-sites/{code}/floors

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceLocationResponses.Floor>`
- Modèles : [ResourceLocationResponses.Floor](#m-ResourceLocationResponses-Floor)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `floors`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resource-sites-code-floors"></a>
### POST /sni/api/v1/resource-sites/{code}/floors

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceLocationRequests.Floor`
- Réponse : `ResourceLocationResponses.Floor`
- Modèles : [ResourceLocationResponses.Floor](#m-ResourceLocationResponses-Floor), [ResourceLocationRequests.Floor](#m-ResourceLocationRequests-Floor)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceSiteController.java`, méthode `createFloor`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceLocationResponses-Floor"></a>
#### ResourceLocationResponses.Floor

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `code` | texte |  |  |
| `siteCode` | texte |  |  |
| `name` | texte |  |  |
| `level` | entier |  |  |
| `planDocumentCode` | texte |  |  |
| `active` | booléen |  |  |
| `resourceCount` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-ResourceLocationRequests-Floor"></a>
#### ResourceLocationRequests.Floor

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `level` | entier |  |  |
| `planDocumentCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-ResourceLocationResponses-FloorPlan"></a>
#### ResourceLocationResponses.FloorPlan

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `floorCode` | texte |  |  |
| `floorName` | texte |  |  |
| `level` | entier |  |  |
| `siteCode` | texte |  |  |
| `siteName` | texte |  |  |
| `planDocumentCode` | texte |  |  |
| `at` | date-heure locale sans fuseau |  |  |
| `resources` | [ResourceLocationResponses.PlacedResource](#m-ResourceLocationResponses-PlacedResource)[] |  |  |

<a id="m-ResourceLocationResponses-Site"></a>
#### ResourceLocationResponses.Site

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `address` | texte |  |  |
| `city` | texte |  |  |
| `phone` | texte |  |  |
| `timeZone` | texte |  |  |
| `openingHoursNote` | texte |  |  |
| `active` | booléen |  |  |
| `floorCount` | entier int64 (texte au-delà de 2^53) |  |  |
| `resourceCount` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-ResourceLocationRequests-Site"></a>
#### ResourceLocationRequests.Site

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `address` | texte |  |  |
| `city` | texte |  |  |
| `phone` | texte |  |  |
| `timeZone` | texte |  |  |
| `openingHoursNote` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-ResourceLocationResponses-PlacedResource"></a>
#### ResourceLocationResponses.PlacedResource

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `typeCode` | texte |  |  |
| `capacity` | entier |  |  |
| `planX` | nombre décimal |  |  |
| `planY` | nombre décimal |  |  |
| `state` | texte |  |  |
| `remainingCapacity` | entier |  |  |
| `freeAt` | date-heure locale sans fuseau |  |  |
