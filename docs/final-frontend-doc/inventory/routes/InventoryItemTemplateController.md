# InventoryItemTemplateController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-item-templates"></a>
### GET /sni/api/v1/inventory/item-templates

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryItemTemplateResponse>`
- Modèles : [InventoryItemTemplateResponse](#m-InventoryItemTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemTemplateController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-inventory-item-templates"></a>
### POST /sni/api/v1/inventory/item-templates

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_TEMPLATE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemTemplateRequest`
- Réponse : `InventoryItemTemplateResponse`
- Modèles : [InventoryItemTemplateResponse](#m-InventoryItemTemplateResponse), [InventoryItemTemplateRequest](#m-InventoryItemTemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemTemplateController.java`, méthode `create`

<a id="r-get-sni-api-v1-inventory-item-templates-templatecode"></a>
### GET /sni/api/v1/inventory/item-templates/{templateCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryItemTemplateResponse`
- Modèles : [InventoryItemTemplateResponse](#m-InventoryItemTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemTemplateController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `templateCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-item-templates-templatecode"></a>
### PUT /sni/api/v1/inventory/item-templates/{templateCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_TEMPLATE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryItemTemplateRequest`
- Réponse : `InventoryItemTemplateResponse`
- Modèles : [InventoryItemTemplateResponse](#m-InventoryItemTemplateResponse), [InventoryItemTemplateRequest](#m-InventoryItemTemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemTemplateController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `templateCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-item-templates-templatecode-variants"></a>
### POST /sni/api/v1/inventory/item-templates/{templateCode}/variants

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ITEM_TEMPLATE_GENERATE`, voir le README racine)
- Corps JSON (facultatif) : `InventoryVariantGenerationRequest`
- Réponse : `InventoryVariantGenerationResponse`
- Modèles : [InventoryVariantGenerationResponse](#m-InventoryVariantGenerationResponse), [InventoryVariantGenerationRequest](#m-InventoryVariantGenerationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryItemTemplateController.java`, méthode `generate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `templateCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryItemTemplateResponse"></a>
#### InventoryItemTemplateResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `categoryCode` | texte |  |  |
| `categoryName` | texte |  |  |
| `unitCode` | texte |  |  |
| `itemType` | enum : `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART` |  |  |
| `trackingType` | enum : `NONE`, `QUANTITY`, `SERIAL`, `LOT`, `EXPIRY` |  |  |
| `defaultCost` | entier int64 ⚠ |  |  |
| `salePrice` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |
| `possibleCombinations` | entier |  |  |
| `generatedVariants` | entier int64 ⚠ |  |  |
| `axes` | [InventoryItemTemplateResponse.Axis](#m-InventoryItemTemplateResponse-Axis)[] |  |  |

<a id="m-InventoryItemTemplateRequest"></a>
#### InventoryItemTemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `categoryCode` | texte |  |  |
| `unitCode` | texte |  |  |
| `itemType` | enum : `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART` | oui |  |
| `trackingType` | enum : `NONE`, `QUANTITY`, `SERIAL`, `LOT`, `EXPIRY` |  |  |
| `defaultCost` | entier int64 ⚠ |  |  |
| `salePrice` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |
| `axes` | [InventoryItemTemplateRequest.Axis](#m-InventoryItemTemplateRequest-Axis)[] | oui | éléments min `1` |

<a id="m-InventoryVariantGenerationResponse"></a>
#### InventoryVariantGenerationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `dryRun` | booléen |  |  |
| `requestedCombinations` | entier |  |  |
| `createdCount` | entier |  |  |
| `skippedCount` | entier |  |  |
| `variants` | [InventoryVariantGenerationResponse.Variant](#m-InventoryVariantGenerationResponse-Variant)[] |  |  |

<a id="m-InventoryVariantGenerationRequest"></a>
#### InventoryVariantGenerationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `selectedValues` | objet { clé : texte[] } |  |  |
| `dryRun` | booléen |  |  |

<a id="m-InventoryItemTemplateResponse-Axis"></a>
#### InventoryItemTemplateResponse.Axis

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `axisCode` | texte |  |  |
| `name` | texte |  |  |
| `position` | entier |  |  |
| `values` | [InventoryItemTemplateResponse.Value](#m-InventoryItemTemplateResponse-Value)[] |  |  |

<a id="m-InventoryItemTemplateRequest-Axis"></a>
#### InventoryItemTemplateRequest.Axis

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `axisCode` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `position` | entier |  |  |
| `values` | [InventoryItemTemplateRequest.Value](#m-InventoryItemTemplateRequest-Value)[] | oui | éléments min `1` |

<a id="m-InventoryVariantGenerationResponse-Variant"></a>
#### InventoryVariantGenerationResponse.Variant

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `name` | texte |  |  |
| `variantSignature` | texte |  |  |
| `alreadyExisted` | booléen |  |  |

<a id="m-InventoryItemTemplateResponse-Value"></a>
#### InventoryItemTemplateResponse.Value

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `valueCode` | texte |  |  |
| `label` | texte |  |  |
| `position` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryItemTemplateRequest-Value"></a>
#### InventoryItemTemplateRequest.Value

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `valueCode` | texte | oui | longueur min `1` |
| `label` | texte | oui | longueur min `1` |
| `position` | entier |  |  |
| `active` | booléen |  |  |
