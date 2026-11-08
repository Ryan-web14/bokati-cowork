# InventoryCategoryController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-categories"></a>
### GET /sni/api/v1/inventory/categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryCategoryResponse>`
- Modèles : [InventoryCategoryResponse](#m-InventoryCategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryCategoryController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-inventory-categories"></a>
### POST /sni/api/v1/inventory/categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_CATEGORY_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryCategoryRequest`
- Réponse : `InventoryCategoryResponse`
- Modèles : [InventoryCategoryResponse](#m-InventoryCategoryResponse), [InventoryCategoryRequest](#m-InventoryCategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryCategoryController.java`, méthode `create`

<a id="r-get-sni-api-v1-inventory-categories-code"></a>
### GET /sni/api/v1/inventory/categories/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryCategoryResponse`
- Modèles : [InventoryCategoryResponse](#m-InventoryCategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryCategoryController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-categories-code"></a>
### PUT /sni/api/v1/inventory/categories/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_CATEGORY_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryCategoryRequest`
- Réponse : `InventoryCategoryResponse`
- Modèles : [InventoryCategoryResponse](#m-InventoryCategoryResponse), [InventoryCategoryRequest](#m-InventoryCategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryCategoryController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-categories-code-activate"></a>
### PATCH /sni/api/v1/inventory/categories/{code}/activate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_CATEGORY_ACTIVATE`, voir le README racine)
- Réponse : `InventoryCategoryResponse`
- Modèles : [InventoryCategoryResponse](#m-InventoryCategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryCategoryController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-categories-code-deactivate"></a>
### PATCH /sni/api/v1/inventory/categories/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_CATEGORY_DEACTIVATE`, voir le README racine)
- Réponse : `InventoryCategoryResponse`
- Modèles : [InventoryCategoryResponse](#m-InventoryCategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryCategoryController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryCategoryResponse"></a>
#### InventoryCategoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `parentCategoryCode` | texte |  |  |
| `parentCategoryName` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryCategoryRequest"></a>
#### InventoryCategoryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `parentCategoryCode` | texte |  |  |
| `active` | booléen |  |  |
