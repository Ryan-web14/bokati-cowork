# SupplierSuggestionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 12 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-procurement-supplier-categories"></a>
### GET /sni/api/v1/inventory/procurement/supplier-categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<SupplierSuggestionDtos.RefCategoryView>`
- Modèles : [SupplierSuggestionDtos.RefCategoryView](#m-SupplierSuggestionDtos-RefCategoryView)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `categories`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `active` | requête | `Boolean` |  |  |
| `parentCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-inventory-procurement-supplier-categories"></a>
### POST /sni/api/v1/inventory/procurement/supplier-categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Corps JSON (obligatoire) : `SupplierSuggestionDtos.RefCategoryRequest`
- Réponse : `SupplierSuggestionDtos.RefCategoryView`
- Modèles : [SupplierSuggestionDtos.RefCategoryView](#m-SupplierSuggestionDtos-RefCategoryView), [SupplierSuggestionDtos.RefCategoryRequest](#m-SupplierSuggestionDtos-RefCategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `createCategory`

<a id="r-delete-sni-api-v1-inventory-procurement-supplier-categories-code"></a>
### DELETE /sni/api/v1/inventory/procurement/supplier-categories/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `deleteCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-procurement-supplier-categories-code"></a>
### GET /sni/api/v1/inventory/procurement/supplier-categories/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `SupplierSuggestionDtos.RefCategoryView`
- Modèles : [SupplierSuggestionDtos.RefCategoryView](#m-SupplierSuggestionDtos-RefCategoryView)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `category`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-procurement-supplier-categories-code"></a>
### PUT /sni/api/v1/inventory/procurement/supplier-categories/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Corps JSON (obligatoire) : `SupplierSuggestionDtos.RefCategoryRequest`
- Réponse : `SupplierSuggestionDtos.RefCategoryView`
- Modèles : [SupplierSuggestionDtos.RefCategoryView](#m-SupplierSuggestionDtos-RefCategoryView), [SupplierSuggestionDtos.RefCategoryRequest](#m-SupplierSuggestionDtos-RefCategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `updateCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-procurement-supplier-categories-code-activate"></a>
### PATCH /sni/api/v1/inventory/procurement/supplier-categories/{code}/activate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `SupplierSuggestionDtos.RefCategoryView`
- Modèles : [SupplierSuggestionDtos.RefCategoryView](#m-SupplierSuggestionDtos-RefCategoryView)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `activateCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-procurement-supplier-categories-code-deactivate"></a>
### PATCH /sni/api/v1/inventory/procurement/supplier-categories/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `SupplierSuggestionDtos.RefCategoryView`
- Modèles : [SupplierSuggestionDtos.RefCategoryView](#m-SupplierSuggestionDtos-RefCategoryView)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `deactivateCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-procurement-supplier-suggestions"></a>
### GET /sni/api/v1/inventory/procurement/supplier-suggestions

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `SupplierSuggestionDtos.SuggestionResult`
- Modèles : [SupplierSuggestionDtos.SuggestionResult](#m-SupplierSuggestionDtos-SuggestionResult)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `suggest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `natureCode` | requête | `String` |  |  |
| `inventoryCategoryCode` | requête | `String` |  |  |
| `supplierCategoryCode` | requête | `String` |  |  |
| `designation` | requête | `String` |  |  |
| `itemCode` | requête | `String` |  |  |
| `amount` | requête | `BigDecimal` |  |  |
| `limit` | requête | `Integer` |  |  |

<a id="r-get-sni-api-v1-inventory-procurement-suppliers-suppliercode-categories"></a>
### GET /sni/api/v1/inventory/procurement/suppliers/{supplierCode}/categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<SupplierSuggestionDtos.SupplierCategoryView>`
- Modèles : [SupplierSuggestionDtos.SupplierCategoryView](#m-SupplierSuggestionDtos-SupplierCategoryView)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `supplierCategories`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `supplierCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-procurement-suppliers-suppliercode-categories"></a>
### POST /sni/api/v1/inventory/procurement/suppliers/{supplierCode}/categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Corps JSON (obligatoire) : `SupplierSuggestionDtos.CategoryRef`
- Réponse : `List<SupplierSuggestionDtos.SupplierCategoryView>`
- Modèles : [SupplierSuggestionDtos.SupplierCategoryView](#m-SupplierSuggestionDtos-SupplierCategoryView), [SupplierSuggestionDtos.CategoryRef](#m-SupplierSuggestionDtos-CategoryRef)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `addSupplierCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `supplierCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-procurement-suppliers-suppliercode-categories"></a>
### PUT /sni/api/v1/inventory/procurement/suppliers/{supplierCode}/categories

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Corps JSON (obligatoire) : `SupplierSuggestionDtos.SupplierCategoriesRequest`
- Réponse : `List<SupplierSuggestionDtos.SupplierCategoryView>`
- Modèles : [SupplierSuggestionDtos.SupplierCategoryView](#m-SupplierSuggestionDtos-SupplierCategoryView), [SupplierSuggestionDtos.SupplierCategoriesRequest](#m-SupplierSuggestionDtos-SupplierCategoriesRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `replaceSupplierCategories`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `supplierCode` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-inventory-procurement-suppliers-suppliercode-categories-kind-code"></a>
### DELETE /sni/api/v1/inventory/procurement/suppliers/{supplierCode}/categories/{kind}/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- Réponse : `List<SupplierSuggestionDtos.SupplierCategoryView>`
- Modèles : [SupplierSuggestionDtos.SupplierCategoryView](#m-SupplierSuggestionDtos-SupplierCategoryView), [SupplierCategoryKind](#m-SupplierCategoryKind)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/suggestion/controller/SupplierSuggestionController.java`, méthode `removeSupplierCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `supplierCode` | chemin | `String` | oui |  |
| `kind` | chemin | `SupplierCategoryKind` | oui |  |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SupplierSuggestionDtos-RefCategoryView"></a>
#### SupplierSuggestionDtos.RefCategoryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `parentCode` | texte |  |  |
| `parentLabel` | texte |  |  |
| `active` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `natures` | [SupplierSuggestionDtos.TargetView](#m-SupplierSuggestionDtos-TargetView)[] |  |  |
| `inventoryCategories` | [SupplierSuggestionDtos.TargetView](#m-SupplierSuggestionDtos-TargetView)[] |  |  |
| `supplierCount` | entier int64 ⚠ |  |  |
| `childCount` | entier int64 ⚠ |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SupplierSuggestionDtos-RefCategoryRequest"></a>
#### SupplierSuggestionDtos.RefCategoryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `parentCode` | texte |  |  |
| `active` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `natureCodes` | texte[] |  |  |
| `inventoryCategoryCodes` | texte[] |  |  |

<a id="m-SupplierSuggestionDtos-SuggestionResult"></a>
#### SupplierSuggestionDtos.SuggestionResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `natureCode` | texte |  |  |
| `natureLabel` | texte |  |  |
| `natureGuessed` | booléen |  |  |
| `inventoryCategoryCode` | texte |  |  |
| `supplierCategoryCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `limit` | entier |  |  |
| `suggestions` | [SupplierSuggestionDtos.Suggestion](#m-SupplierSuggestionDtos-Suggestion)[] |  |  |

<a id="m-SupplierSuggestionDtos-SupplierCategoryView"></a>
#### SupplierSuggestionDtos.SupplierCategoryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | enum : `NATURE`, `INVENTORY_CATEGORY`, `SUPPLIER_CATEGORY` |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-SupplierSuggestionDtos-CategoryRef"></a>
#### SupplierSuggestionDtos.CategoryRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | enum : `NATURE`, `INVENTORY_CATEGORY`, `SUPPLIER_CATEGORY` | oui |  |
| `code` | texte | oui | longueur min `1` |

<a id="m-SupplierSuggestionDtos-SupplierCategoriesRequest"></a>
#### SupplierSuggestionDtos.SupplierCategoriesRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `categories` | [SupplierSuggestionDtos.CategoryRef](#m-SupplierSuggestionDtos-CategoryRef)[] | oui |  |

<a id="m-SupplierCategoryKind"></a>
#### SupplierCategoryKind

Énumération : `NATURE`, `INVENTORY_CATEGORY`, `SUPPLIER_CATEGORY`

<a id="m-SupplierSuggestionDtos-TargetView"></a>
#### SupplierSuggestionDtos.TargetView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |

<a id="m-SupplierSuggestionDtos-Suggestion"></a>
#### SupplierSuggestionDtos.Suggestion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rank` | entier |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `phone` | texte |  |  |
| `email` | texte |  |  |
| `score` | entier |  |  |
| `reasons` | texte[] |  |  |
| `declared` | booléen |  |  |
| `historyCount` | entier int64 ⚠ |  |  |
| `contractUnitPrice` | entier int64 ⚠ |  |  |
| `leadTimeDays` | entier |  |  |
| `categories` | [SupplierSuggestionDtos.SupplierCategoryView](#m-SupplierSuggestionDtos-SupplierCategoryView)[] |  |  |
