# ServiceCatalogController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 14 route(s).

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

<a id="r-get-sni-api-v1-billing-catalog-export"></a>
### GET /sni/api/v1/billing/catalog/export

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `exportCsv`

<a id="r-post-sni-api-v1-billing-catalog-import"></a>
### POST /sni/api/v1/billing/catalog/import

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Envoi : `text/csv, text/plain`
- Corps JSON (obligatoire) : `String`
- Réponse : `CatalogPriceService.ImportResult`
- Modèles : [CatalogPriceService.ImportResult](#m-CatalogPriceService-ImportResult)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `importCsv`

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

<a id="r-get-sni-api-v1-billing-catalog-margins"></a>
### GET /sni/api/v1/billing/catalog/margins

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogMarginResponse>`
- Modèles : [CatalogMarginResponse](#m-CatalogMarginResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `margins`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `search` | requête | `String` |  |  |
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

<a id="r-put-sni-api-v1-billing-catalog-itemcode-links"></a>
### PUT /sni/api/v1/billing/catalog/{itemCode}/links

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `CatalogLinksRequest`
- Réponse : `CatalogMarginResponse`
- Modèles : [CatalogMarginResponse](#m-CatalogMarginResponse), [CatalogLinksRequest](#m-CatalogLinksRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `links`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-catalog-itemcode-price"></a>
### GET /sni/api/v1/billing/catalog/{itemCode}/price

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `CatalogPriceResponse`
- Modèles : [CatalogPriceResponse](#m-CatalogPriceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `price`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |
| `segment` | requête | `String` |  |  |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-billing-catalog-itemcode-prices"></a>
### GET /sni/api/v1/billing/catalog/{itemCode}/prices

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogPriceResponse>`
- Modèles : [CatalogPriceResponse](#m-CatalogPriceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `priceHistory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-catalog-itemcode-prices"></a>
### POST /sni/api/v1/billing/catalog/{itemCode}/prices

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CatalogPriceRequest`
- Réponse : `CatalogPriceResponse`
- Modèles : [CatalogPriceResponse](#m-CatalogPriceResponse), [CatalogPriceRequest](#m-CatalogPriceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ServiceCatalogController.java`, méthode `setPrice`

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

<a id="m-CatalogPriceService-ImportResult"></a>
#### CatalogPriceService.ImportResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `created` | entier |  |  |
| `updated` | entier |  |  |
| `errors` | texte[] |  |  |

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

<a id="m-CatalogMarginResponse"></a>
#### CatalogMarginResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `name` | texte |  |  |
| `category` | texte |  |  |
| `subcategory` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `costPrice` | nombre décimal |  |  |
| `margin` | nombre décimal |  |  |
| `marginRate` | nombre décimal |  |  |
| `floorPrice` | nombre décimal |  |  |
| `maxDiscountRate` | nombre décimal |  |  |
| `inventoryItemCode` | texte |  |  |
| `revenueAccount` | texte |  |  |
| `active` | booléen |  |  |

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

<a id="m-CatalogLinksRequest"></a>
#### CatalogLinksRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `inventoryItemCode` | texte |  | longueur min `0`, longueur max `80` |
| `revenueAccount` | texte |  | longueur min `0`, longueur max `20` |

<a id="m-CatalogPriceResponse"></a>
#### CatalogPriceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `segment` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `source` | texte |  |  |

<a id="m-CatalogPriceRequest"></a>
#### CatalogPriceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `segment` | texte |  | motif `ALL|MEMBER|COMPANY|VISITOR` |
| `unitPrice` | nombre décimal | oui |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |
