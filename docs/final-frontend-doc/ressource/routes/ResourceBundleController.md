# ResourceBundleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-bundles"></a>
### GET /sni/api/v1/resource-bundles

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceBundles.Response>`
- Modèles : [ResourceBundles.Response](#m-ResourceBundles-Response)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceBundleController.java`, méthode `list`

<a id="r-post-sni-api-v1-resource-bundles"></a>
### POST /sni/api/v1/resource-bundles

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceBundles.Request`
- Réponse : `ResourceBundles.Response`
- Modèles : [ResourceBundles.Response](#m-ResourceBundles-Response), [ResourceBundles.Request](#m-ResourceBundles-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceBundleController.java`, méthode `create`

<a id="r-delete-sni-api-v1-resource-bundles-code"></a>
### DELETE /sni/api/v1/resource-bundles/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceBundleController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-bundles-code"></a>
### GET /sni/api/v1/resource-bundles/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceBundles.Response`
- Modèles : [ResourceBundles.Response](#m-ResourceBundles-Response)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceBundleController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-resource-bundles-code"></a>
### PATCH /sni/api/v1/resource-bundles/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceBundles.Request`
- Réponse : `ResourceBundles.Response`
- Modèles : [ResourceBundles.Response](#m-ResourceBundles-Response), [ResourceBundles.Request](#m-ResourceBundles-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceBundleController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceBundles-Response"></a>
#### ResourceBundles.Response

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `discountPercent` | nombre décimal |  |  |
| `active` | booléen |  |  |
| `items` | [ResourceBundles.ItemResponse](#m-ResourceBundles-ItemResponse)[] |  |  |

<a id="m-ResourceBundles-Request"></a>
#### ResourceBundles.Request

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `discountPercent` | nombre décimal |  |  |
| `active` | booléen |  |  |
| `items` | [ResourceBundles.Item](#m-ResourceBundles-Item)[] |  |  |

<a id="m-ResourceBundles-ItemResponse"></a>
#### ResourceBundles.ItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `quantity` | entier |  |  |

<a id="m-ResourceBundles-Item"></a>
#### ResourceBundles.Item

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `quantity` | entier |  |  |
