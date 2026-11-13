# CatalogReferenceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 20 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-catalog-categories"></a>
### GET /sni/api/v1/billing/catalog/categories

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogReferenceDtos.CategoryResponse>`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `categories`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-catalog-categories"></a>
### POST /sni/api/v1/billing/catalog/categories

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.CategoryRequest`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse), [CatalogReferenceDtos.CategoryRequest](#m-CatalogReferenceDtos-CategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `createCategory`

<a id="r-delete-sni-api-v1-billing-catalog-categories-code"></a>
### DELETE /sni/api/v1/billing/catalog/categories/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `deleteCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-catalog-categories-code"></a>
### PUT /sni/api/v1/billing/catalog/categories/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.CategoryUpdateRequest`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse), [CatalogReferenceDtos.CategoryUpdateRequest](#m-CatalogReferenceDtos-CategoryUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `updateCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-categories-code-activate"></a>
### PATCH /sni/api/v1/billing/catalog/categories/{code}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `activateCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-categories-code-deactivate"></a>
### PATCH /sni/api/v1/billing/catalog/categories/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `deactivateCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-catalog-categories-code-subcategories"></a>
### GET /sni/api/v1/billing/catalog/categories/{code}/subcategories

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogReferenceDtos.CategoryResponse>`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `subcategories`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-catalog-categories-code-subcategories"></a>
### POST /sni/api/v1/billing/catalog/categories/{code}/subcategories

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.CategoryRequest`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse), [CatalogReferenceDtos.CategoryRequest](#m-CatalogReferenceDtos-CategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `createSubcategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-billing-catalog-categories-code-subcategories-subcode"></a>
### DELETE /sni/api/v1/billing/catalog/categories/{code}/subcategories/{subCode}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `deleteSubcategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `subCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-catalog-categories-code-subcategories-subcode"></a>
### PUT /sni/api/v1/billing/catalog/categories/{code}/subcategories/{subCode}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.CategoryUpdateRequest`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse), [CatalogReferenceDtos.CategoryUpdateRequest](#m-CatalogReferenceDtos-CategoryUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `updateSubcategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `subCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-categories-code-subcategories-subcode-activate"></a>
### PATCH /sni/api/v1/billing/catalog/categories/{code}/subcategories/{subCode}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `activateSubcategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `subCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-categories-code-subcategories-subcode-deactivate"></a>
### PATCH /sni/api/v1/billing/catalog/categories/{code}/subcategories/{subCode}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `CatalogReferenceDtos.CategoryResponse`
- Modèles : [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `deactivateSubcategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `subCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-catalog-references"></a>
### GET /sni/api/v1/billing/catalog/references

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `CatalogReferenceDtos.ReferencesResponse`
- Modèles : [CatalogReferenceDtos.ReferencesResponse](#m-CatalogReferenceDtos-ReferencesResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `references`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-catalog-references-merge"></a>
### POST /sni/api/v1/billing/catalog/references/merge

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.MergeRequest`
- Réponse : `Map<String, Long>`
- Modèles : [CatalogReferenceDtos.MergeRequest](#m-CatalogReferenceDtos-MergeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `merge`

<a id="r-get-sni-api-v1-billing-catalog-units"></a>
### GET /sni/api/v1/billing/catalog/units

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogReferenceDtos.UnitResponse>`
- Modèles : [CatalogReferenceDtos.UnitResponse](#m-CatalogReferenceDtos-UnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `units`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-catalog-units"></a>
### POST /sni/api/v1/billing/catalog/units

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.UnitRequest`
- Réponse : `CatalogReferenceDtos.UnitResponse`
- Modèles : [CatalogReferenceDtos.UnitResponse](#m-CatalogReferenceDtos-UnitResponse), [CatalogReferenceDtos.UnitRequest](#m-CatalogReferenceDtos-UnitRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `createUnit`

<a id="r-delete-sni-api-v1-billing-catalog-units-code"></a>
### DELETE /sni/api/v1/billing/catalog/units/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `deleteUnit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-catalog-units-code"></a>
### PUT /sni/api/v1/billing/catalog/units/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.UnitUpdateRequest`
- Réponse : `CatalogReferenceDtos.UnitResponse`
- Modèles : [CatalogReferenceDtos.UnitResponse](#m-CatalogReferenceDtos-UnitResponse), [CatalogReferenceDtos.UnitUpdateRequest](#m-CatalogReferenceDtos-UnitUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `updateUnit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-units-code-activate"></a>
### PATCH /sni/api/v1/billing/catalog/units/{code}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `CatalogReferenceDtos.UnitResponse`
- Modèles : [CatalogReferenceDtos.UnitResponse](#m-CatalogReferenceDtos-UnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `activateUnit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-units-code-deactivate"></a>
### PATCH /sni/api/v1/billing/catalog/units/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Réponse : `CatalogReferenceDtos.UnitResponse`
- Modèles : [CatalogReferenceDtos.UnitResponse](#m-CatalogReferenceDtos-UnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CatalogReferenceController.java`, méthode `deactivateUnit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CatalogReferenceDtos-CategoryResponse"></a>
#### CatalogReferenceDtos.CategoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `parentCode` | texte |  |  |
| `description` | texte |  |  |
| `revenueAccount` | texte |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |
| `createdFromItem` | booléen |  |  |
| `itemCount` | entier int64 ⚠ |  |  |
| `subcategories` | [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)[] |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CatalogReferenceDtos-CategoryRequest"></a>
#### CatalogReferenceDtos.CategoryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  | longueur min `0`, longueur max `100` |
| `label` | texte | oui | longueur min `0`, longueur max `150` |
| `description` | texte |  |  |
| `revenueAccount` | texte |  | longueur min `0`, longueur max `20` |
| `displayOrder` | entier |  |  |

<a id="m-CatalogReferenceDtos-CategoryUpdateRequest"></a>
#### CatalogReferenceDtos.CategoryUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  | longueur min `0`, longueur max `150` |
| `description` | texte |  |  |
| `revenueAccount` | texte |  | longueur min `0`, longueur max `20` |
| `displayOrder` | entier |  |  |

<a id="m-CatalogReferenceDtos-ReferencesResponse"></a>
#### CatalogReferenceDtos.ReferencesResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `categories` | [CatalogReferenceDtos.CategoryResponse](#m-CatalogReferenceDtos-CategoryResponse)[] |  |  |
| `units` | [CatalogReferenceDtos.UnitResponse](#m-CatalogReferenceDtos-UnitResponse)[] |  |  |
| `taxRules` | [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse)[] |  |  |
| `strict` | booléen |  |  |

<a id="m-CatalogReferenceDtos-MergeRequest"></a>
#### CatalogReferenceDtos.MergeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `list` | texte | oui | longueur min `1` |
| `parentCode` | texte |  |  |
| `sourceCode` | texte | oui | longueur min `1` |
| `targetCode` | texte | oui | longueur min `1` |

<a id="m-CatalogReferenceDtos-UnitResponse"></a>
#### CatalogReferenceDtos.UnitResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `pluralLabel` | texte |  |  |
| `symbol` | texte |  |  |
| `quantityScale` | entier |  |  |
| `inventoryUnitCode` | texte |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |
| `createdFromItem` | booléen |  |  |
| `itemCount` | entier int64 ⚠ |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CatalogReferenceDtos-UnitRequest"></a>
#### CatalogReferenceDtos.UnitRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  | longueur min `0`, longueur max `50` |
| `label` | texte | oui | longueur min `0`, longueur max `100` |
| `pluralLabel` | texte |  | longueur min `0`, longueur max `100` |
| `symbol` | texte |  | longueur min `0`, longueur max `20` |
| `quantityScale` | entier |  | min `0`, max `4` |
| `inventoryUnitCode` | texte |  | longueur min `0`, longueur max `50` |
| `displayOrder` | entier |  |  |

<a id="m-CatalogReferenceDtos-UnitUpdateRequest"></a>
#### CatalogReferenceDtos.UnitUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  | longueur min `0`, longueur max `100` |
| `pluralLabel` | texte |  | longueur min `0`, longueur max `100` |
| `symbol` | texte |  | longueur min `0`, longueur max `20` |
| `quantityScale` | entier |  | min `0`, max `4` |
| `inventoryUnitCode` | texte |  | longueur min `0`, longueur max `50` |
| `displayOrder` | entier |  |  |

<a id="m-CatalogReferenceDtos-TaxRuleResponse"></a>
#### CatalogReferenceDtos.TaxRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `baseCode` | texte |  |  |
| `name` | texte |  |  |
| `taxType` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `appliesOn` | texte |  |  |
| `treatment` | texte |  |  |
| `labelOnDocument` | texte |  |  |
| `exemptionReason` | texte |  |  |
| `countryCode` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `active` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `currentlyValid` | booléen |  |  |
| `selectableOnItem` | booléen |  |  |
