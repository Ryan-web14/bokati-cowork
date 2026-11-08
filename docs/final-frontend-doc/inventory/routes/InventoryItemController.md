# InventoryItemController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 25 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-items"></a>
### GET /sni/api/v1/inventory/items

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<InventoryItemResponse>`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse), [InventoryItemType](#m-InventoryItemType)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `query` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `name` | requête | `String` |  |  |
| `categoryCode` | requête | `String` |  |  |
| `itemType` | requête | `InventoryItemType` |  |  |
| `active` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=name,ASC |

<a id="r-post-sni-api-v1-inventory-items"></a>
### POST /sni/api/v1/inventory/items

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemRequest`
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse), [InventoryItemRequest](#m-InventoryItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `create`

<a id="r-delete-sni-api-v1-inventory-items-barcodes-barcodeid"></a>
### DELETE /sni/api/v1/inventory/items/barcodes/{barcodeId}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `deleteBarcode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `barcodeId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-inventory-items-barcodes-barcodeid-primary"></a>
### PATCH /sni/api/v1/inventory/items/barcodes/{barcodeId}/primary

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_BARCODE_PRIMARY`, voir le README racine)
- Réponse : `InventoryBarcodeResponse`
- Modèles : [InventoryBarcodeResponse](#m-InventoryBarcodeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `markPrimaryBarcode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `barcodeId` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-inventory-items-packagings-packagingid"></a>
### DELETE /sni/api/v1/inventory/items/packagings/{packagingId}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `deletePackaging`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `packagingId` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-inventory-items-substitutes-substituteid"></a>
### DELETE /sni/api/v1/inventory/items/substitutes/{substituteId}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `deleteSubstitute`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `substituteId` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-inventory-items-translations-translationid"></a>
### DELETE /sni/api/v1/inventory/items/translations/{translationId}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `deleteTranslation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `translationId` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-inventory-items-itemcode"></a>
### DELETE /sni/api/v1/inventory/items/{itemCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:DELETE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_DELETE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode"></a>
### GET /sni/api/v1/inventory/items/{itemCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-items-itemcode"></a>
### PUT /sni/api/v1/inventory/items/{itemCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemRequest`
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse), [InventoryItemRequest](#m-InventoryItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-items-itemcode-activate"></a>
### PATCH /sni/api/v1/inventory/items/{itemCode}/activate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_ACTIVATE`, voir le README racine)
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode-barcodes"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/barcodes

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryBarcodeResponse>`
- Modèles : [InventoryBarcodeResponse](#m-InventoryBarcodeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `listBarcodes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-items-itemcode-barcodes"></a>
### POST /sni/api/v1/inventory/items/{itemCode}/barcodes

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_BARCODE_ADD`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryBarcodeRequest`
- Réponse : `InventoryBarcodeResponse`
- Modèles : [InventoryBarcodeResponse](#m-InventoryBarcodeResponse), [InventoryBarcodeRequest](#m-InventoryBarcodeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `addBarcode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-items-itemcode-deactivate"></a>
### PATCH /sni/api/v1/inventory/items/{itemCode}/deactivate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_DEACTIVATE`, voir le README racine)
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-items-itemcode-lifecycle"></a>
### PATCH /sni/api/v1/inventory/items/{itemCode}/lifecycle

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_LIFECYCLE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemLifecycleRequest`
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse), [InventoryItemLifecycleRequest](#m-InventoryItemLifecycleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `changeLifecycle`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode-packagings"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/packagings

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryPackagingResponse>`
- Modèles : [InventoryPackagingResponse](#m-InventoryPackagingResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `listPackagings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-items-itemcode-packagings"></a>
### POST /sni/api/v1/inventory/items/{itemCode}/packagings

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_PACKAGING_ADD`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryPackagingRequest`
- Réponse : `InventoryPackagingResponse`
- Modèles : [InventoryPackagingResponse](#m-InventoryPackagingResponse), [InventoryPackagingRequest](#m-InventoryPackagingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `addPackaging`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode-price-history"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/price-history

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<InventoryItemPriceHistoryResponse>`
- Modèles : [InventoryItemPriceHistoryResponse](#m-InventoryItemPriceHistoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `priceHistory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-patch-sni-api-v1-inventory-items-itemcode-revision"></a>
### PATCH /sni/api/v1/inventory/items/{itemCode}/revision

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_REVISION`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemRevisionRequest`
- Réponse : `InventoryItemResponse`
- Modèles : [InventoryItemResponse](#m-InventoryItemResponse), [InventoryItemRevisionRequest](#m-InventoryItemRevisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `changeRevision`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode-revision-history"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/revision-history

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryItemRevisionHistoryResponse>`
- Modèles : [InventoryItemRevisionHistoryResponse](#m-InventoryItemRevisionHistoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `revisionHistory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode-substitutes"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/substitutes

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryItemSubstituteResponse>`
- Modèles : [InventoryItemSubstituteResponse](#m-InventoryItemSubstituteResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `listSubstitutes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-items-itemcode-substitutes"></a>
### POST /sni/api/v1/inventory/items/{itemCode}/substitutes

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_SUBSTITUTE_ADD`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemSubstituteRequest`
- Réponse : `InventoryItemSubstituteResponse`
- Modèles : [InventoryItemSubstituteResponse](#m-InventoryItemSubstituteResponse), [InventoryItemSubstituteRequest](#m-InventoryItemSubstituteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `addSubstitute`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-items-itemcode-translations"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/translations

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryItemTranslationResponse>`
- Modèles : [InventoryItemTranslationResponse](#m-InventoryItemTranslationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `listTranslations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-items-itemcode-translations"></a>
### PUT /sni/api/v1/inventory/items/{itemCode}/translations

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_TRANSLATION_UPSERT`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemTranslationRequest`
- Réponse : `InventoryItemTranslationResponse`
- Modèles : [InventoryItemTranslationResponse](#m-InventoryItemTranslationResponse), [InventoryItemTranslationRequest](#m-InventoryItemTranslationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `upsertTranslation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-items-itemcode-valuation-method"></a>
### PATCH /sni/api/v1/inventory/items/{itemCode}/valuation-method

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_VALUATION_METHOD`, voir le README racine)
- Corps JSON (obligatoire) : `ItemValuationMethodRequest`
- Réponse : `Integer`
- Modèles : [ItemValuationMethodRequest](#m-ItemValuationMethodRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemController.java`, méthode `changeValuationMethod`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryItemResponse"></a>
#### InventoryItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `psku` | texte |  |  |
| `shortCode` | texte |  |  |
| `displayCode` | texte |  |  |
| `identificationCode` | texte |  |  |
| `specification` | texte |  |  |
| `categoryCode` | texte |  |  |
| `categoryName` | texte |  |  |
| `unitCode` | texte |  |  |
| `unitName` | texte |  |  |
| `itemType` | enum : `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART` |  |  |
| `trackingType` | enum : `NONE`, `QUANTITY`, `SERIAL`, `LOT`, `EXPIRY` |  |  |
| `defaultCost` | entier int64 ⚠ |  |  |
| `salePrice` | entier int64 ⚠ |  |  |
| `taxable` | booléen |  |  |
| `allowNegativeStock` | booléen |  |  |
| `requiresExpiryDate` | booléen |  |  |
| `requiresLotNumber` | booléen |  |  |
| `requiresSerialNumber` | booléen |  |  |
| `lifecycleStatus` | enum : `DRAFT`, `NEW`, `ACTIVE`, `PHASE_OUT`, `OBSOLETE`, `BLOCKED` |  |  |
| `active` | booléen |  |  |
| `revision` | texte |  |  |
| `weightKg` | nombre décimal |  |  |
| `volumeM3` | nombre décimal |  |  |
| `lengthMm` | entier |  |  |
| `widthMm` | entier |  |  |
| `heightMm` | entier |  |  |
| `stackable` | booléen |  |  |
| `templateCode` | texte |  |  |
| `variantSignature` | texte |  |  |

<a id="m-InventoryItemType"></a>
#### InventoryItemType

Énumération : `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART`

<a id="m-InventoryItemRequest"></a>
#### InventoryItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `psku` | texte |  |  |
| `identificationCode` | texte |  |  |
| `specification` | texte |  |  |
| `categoryCode` | texte |  |  |
| `unitCode` | texte |  |  |
| `itemType` | enum : `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART` | oui |  |
| `trackingType` | enum : `NONE`, `QUANTITY`, `SERIAL`, `LOT`, `EXPIRY` |  |  |
| `defaultCost` | entier int64 ⚠ |  |  |
| `salePrice` | entier int64 ⚠ |  |  |
| `taxable` | booléen |  |  |
| `allowNegativeStock` | booléen |  |  |
| `requiresExpiryDate` | booléen |  |  |
| `requiresLotNumber` | booléen |  |  |
| `requiresSerialNumber` | booléen |  |  |
| `lifecycleStatus` | enum : `DRAFT`, `NEW`, `ACTIVE`, `PHASE_OUT`, `OBSOLETE`, `BLOCKED` |  |  |
| `active` | booléen |  |  |
| `revision` | texte |  |  |
| `weightKg` | nombre décimal |  |  |
| `volumeM3` | nombre décimal |  |  |
| `lengthMm` | entier |  |  |
| `widthMm` | entier |  |  |
| `heightMm` | entier |  |  |
| `stackable` | booléen |  |  |

<a id="m-InventoryBarcodeResponse"></a>
#### InventoryBarcodeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `barcodeType` | enum : `EAN13`, `EAN8`, `UPC`, `CODE128`, `QR`, `INTERNAL`, `SUPPLIER` |  |  |
| `barcodeValue` | texte |  |  |
| `unitCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `primaryCode` | booléen |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-InventoryBarcodeRequest"></a>
#### InventoryBarcodeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `barcodeType` | enum : `EAN13`, `EAN8`, `UPC`, `CODE128`, `QR`, `INTERNAL`, `SUPPLIER` | oui |  |
| `barcodeValue` | texte | oui | longueur min `1` |
| `unitCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `primaryCode` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryItemLifecycleRequest"></a>
#### InventoryItemLifecycleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lifecycleStatus` | enum : `DRAFT`, `NEW`, `ACTIVE`, `PHASE_OUT`, `OBSOLETE`, `BLOCKED` | oui |  |
| `reason` | texte |  |  |

<a id="m-InventoryPackagingResponse"></a>
#### InventoryPackagingResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `packagingLevel` | enum : `EACH`, `INNER`, `CASE`, `PALLET` |  |  |
| `name` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `barcodeValue` | texte |  |  |
| `weightKg` | nombre décimal |  |  |
| `lengthMm` | entier |  |  |
| `widthMm` | entier |  |  |
| `heightMm` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryPackagingRequest"></a>
#### InventoryPackagingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `packagingLevel` | enum : `EACH`, `INNER`, `CASE`, `PALLET` | oui |  |
| `name` | texte |  |  |
| `quantity` | nombre décimal | oui |  |
| `barcodeValue` | texte |  |  |
| `weightKg` | nombre décimal |  |  |
| `lengthMm` | entier |  |  |
| `widthMm` | entier |  |  |
| `heightMm` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryItemPriceHistoryResponse"></a>
#### InventoryItemPriceHistoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `priceType` | enum : `DEFAULT_COST`, `SALE_PRICE` |  |  |
| `previousValue` | entier int64 ⚠ |  |  |
| `newValue` | entier int64 ⚠ |  |  |
| `delta` | entier int64 ⚠ |  |  |
| `changedBy` | texte |  |  |
| `reason` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-InventoryItemRevisionRequest"></a>
#### InventoryItemRevisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `revision` | texte | oui | longueur min `1` |
| `reason` | texte |  |  |
| `documentCode` | texte |  |  |
| `changedBy` | texte |  |  |

<a id="m-InventoryItemRevisionHistoryResponse"></a>
#### InventoryItemRevisionHistoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `previousRevision` | texte |  |  |
| `newRevision` | texte |  |  |
| `changedBy` | texte |  |  |
| `reason` | texte |  |  |
| `documentCode` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-InventoryItemSubstituteResponse"></a>
#### InventoryItemSubstituteResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `substituteItemCode` | texte |  |  |
| `substituteItemName` | texte |  |  |
| `priority` | entier |  |  |
| `conversionFactor` | nombre décimal |  |  |
| `bidirectional` | booléen |  |  |
| `inherited` | booléen |  |  |
| `notes` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryItemSubstituteRequest"></a>
#### InventoryItemSubstituteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `substituteItemCode` | texte | oui | longueur min `1` |
| `priority` | entier |  |  |
| `conversionFactor` | nombre décimal |  |  |
| `bidirectional` | booléen |  |  |
| `notes` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryItemTranslationResponse"></a>
#### InventoryItemTranslationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `languageCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |

<a id="m-InventoryItemTranslationRequest"></a>
#### InventoryItemTranslationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `languageCode` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |

<a id="m-ItemValuationMethodRequest"></a>
#### ItemValuationMethodRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `valuationMethod` | enum : `WEIGHTED_AVERAGE`, `FIFO` | oui |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
