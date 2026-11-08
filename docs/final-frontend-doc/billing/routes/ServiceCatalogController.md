# ServiceCatalogController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-catalog"></a>
### GET /sni/api/v1/billing/catalog

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<ServiceCatalogItemResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ServiceCatalogItemResponse](#m-ServiceCatalogItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |
| `category` | requête | `String` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=displayOrder,ASC |

<a id="r-post-sni-api-v1-billing-catalog"></a>
### POST /sni/api/v1/billing/catalog

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateServiceCatalogItemRequest`
- Réponse : `ServiceCatalogItemResponse`
- Modèles : [ServiceCatalogItemResponse](#m-ServiceCatalogItemResponse), [CreateServiceCatalogItemRequest](#m-CreateServiceCatalogItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `create`

<a id="r-get-sni-api-v1-billing-catalog-lookup"></a>
### GET /sni/api/v1/billing/catalog/lookup

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogLookupItemResponse>`
- Modèles : [CatalogLookupItemResponse](#m-CatalogLookupItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `lookup`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `sources` | requête | `List<String>` |  |  |
| `category` | requête | `String` |  |  |

<a id="r-delete-sni-api-v1-billing-catalog-itemcode"></a>
### DELETE /sni/api/v1/billing/catalog/{itemCode}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-catalog-itemcode"></a>
### PUT /sni/api/v1/billing/catalog/{itemCode}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `UpdateServiceCatalogItemRequest`
- Réponse : `ServiceCatalogItemResponse`
- Modèles : [ServiceCatalogItemResponse](#m-ServiceCatalogItemResponse), [UpdateServiceCatalogItemRequest](#m-UpdateServiceCatalogItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-itemcode-activate"></a>
### PATCH /sni/api/v1/billing/catalog/{itemCode}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `ServiceCatalogItemResponse`
- Modèles : [ServiceCatalogItemResponse](#m-ServiceCatalogItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-catalog-itemcode-deactivate"></a>
### PATCH /sni/api/v1/billing/catalog/{itemCode}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `ServiceCatalogItemResponse`
- Modèles : [ServiceCatalogItemResponse](#m-ServiceCatalogItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ServiceCatalogItemResponse"></a>
#### ServiceCatalogItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `category` | texte |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `taxRuleCode` | texte |  |  |
| `active` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `costPrice` | nombre décimal |  |  |
| `floorPrice` | nombre décimal |  |  |
| `minMarginRate` | nombre décimal |  |  |
| `maxDiscountRate` | nombre décimal |  |  |
| `discountPolicy` | texte |  |  |
| `effectiveFloorPrice` | nombre décimal |  |  |
| `floorPriceOrigin` | texte |  |  |
| `detailedDescription` | texte |  |  |
| `includedItems` | texte[] |  |  |
| `imageUrl` | texte |  |  |
| `billingMode` | texte |  |  |
| `defaultQuantity` | nombre décimal |  |  |
| `minQuantity` | nombre décimal |  |  |
| `maxQuantity` | nombre décimal |  |  |
| `taxableByDefault` | booléen |  |  |
| `subcategory` | texte |  |  |
| `tags` | texte[] |  |  |
| `externalReference` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `currentlyValid` | booléen |  |  |

<a id="m-CreateServiceCatalogItemRequest"></a>
#### CreateServiceCatalogItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `category` | texte |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal | oui |  |
| `currency` | texte |  |  |
| `taxRuleCode` | texte |  |  |
| `displayOrder` | entier |  |  |
| `costPrice` | nombre décimal |  |  |
| `floorPrice` | nombre décimal |  |  |
| `minMarginRate` | nombre décimal |  | min `0`, max `100` |
| `maxDiscountRate` | nombre décimal |  | min `0`, max `100` |
| `discountPolicy` | texte |  |  |
| `detailedDescription` | texte |  |  |
| `includedItems` | texte[] |  |  |
| `imageUrl` | texte |  |  |
| `billingMode` | texte |  |  |
| `defaultQuantity` | nombre décimal |  |  |
| `minQuantity` | nombre décimal |  |  |
| `maxQuantity` | nombre décimal |  |  |
| `taxableByDefault` | booléen |  |  |
| `subcategory` | texte |  |  |
| `tags` | texte[] |  |  |
| `externalReference` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |

<a id="m-CatalogLookupItemResponse"></a>
#### CatalogLookupItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `category` | texte |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `taxRuleCode` | texte |  |  |
| `meta` | objet { clé : objet libre } |  |  |

<a id="m-UpdateServiceCatalogItemRequest"></a>
#### UpdateServiceCatalogItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `category` | texte |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `taxRuleCode` | texte |  |  |
| `displayOrder` | entier |  |  |
| `costPrice` | nombre décimal |  |  |
| `floorPrice` | nombre décimal |  |  |
| `minMarginRate` | nombre décimal |  | min `0`, max `100` |
| `maxDiscountRate` | nombre décimal |  | min `0`, max `100` |
| `discountPolicy` | texte |  |  |
| `detailedDescription` | texte |  |  |
| `includedItems` | texte[] |  |  |
| `imageUrl` | texte |  |  |
| `billingMode` | texte |  |  |
| `defaultQuantity` | nombre décimal |  |  |
| `minQuantity` | nombre décimal |  |  |
| `maxQuantity` | nombre décimal |  |  |
| `taxableByDefault` | booléen |  |  |
| `subcategory` | texte |  |  |
| `tags` | texte[] |  |  |
| `externalReference` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `clearFields` | texte[] |  |  |
