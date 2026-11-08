# InventoryReorderRuleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-reorder-rules"></a>
### GET /sni/api/v1/inventory/reorder-rules

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryReorderRuleResponse>`
- Modèles : [InventoryReorderRuleResponse](#m-InventoryReorderRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryReorderRuleController.java`, méthode `list`

<a id="r-post-sni-api-v1-inventory-reorder-rules"></a>
### POST /sni/api/v1/inventory/reorder-rules

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_REORDER_RULE_UPSERT`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryReorderRuleRequest`
- Réponse : `InventoryReorderRuleResponse`
- Modèles : [InventoryReorderRuleResponse](#m-InventoryReorderRuleResponse), [InventoryReorderRuleRequest](#m-InventoryReorderRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryReorderRuleController.java`, méthode `createOrUpdate`

<a id="r-get-sni-api-v1-inventory-reorder-rules-suggestions"></a>
### GET /sni/api/v1/inventory/reorder-rules/suggestions

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<ReorderSuggestionResponse>`
- Modèles : [ReorderSuggestionResponse](#m-ReorderSuggestionResponse), [ReorderSuggestionSeverity](#m-ReorderSuggestionSeverity)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryReorderRuleController.java`, méthode `suggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `severity` | requête | `ReorderSuggestionSeverity` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryReorderRuleResponse"></a>
#### InventoryReorderRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `minQuantity` | nombre décimal |  |  |
| `maxQuantity` | nombre décimal |  |  |
| `reorderQuantity` | nombre décimal |  |  |
| `preferredSupplierCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryReorderRuleRequest"></a>
#### InventoryReorderRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `locationCode` | texte |  |  |
| `minQuantity` | nombre décimal |  |  |
| `maxQuantity` | nombre décimal |  |  |
| `reorderQuantity` | nombre décimal |  |  |
| `preferredSupplierCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-ReorderSuggestionResponse"></a>
#### ReorderSuggestionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `itemType` | enum : `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART` |  |  |
| `categoryCode` | texte |  |  |
| `categoryName` | texte |  |  |
| `unitCode` | texte |  |  |
| `unitName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `ruleScope` | texte |  |  |
| `currentQuantity` | nombre décimal |  |  |
| `quantityOnHand` | nombre décimal |  |  |
| `quantityReserved` | nombre décimal |  |  |
| `minQuantity` | nombre décimal |  |  |
| `maxQuantity` | nombre décimal |  |  |
| `reorderQuantity` | nombre décimal |  |  |
| `targetQuantity` | nombre décimal |  |  |
| `shortageQuantity` | nombre décimal |  |  |
| `estimatedUnitCost` | entier int64 ⚠ |  |  |
| `estimatedOrderCost` | entier int64 ⚠ |  |  |
| `preferredSupplierCode` | texte |  |  |
| `severity` | enum : `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |  |  |
| `reasonCode` | enum : `NO_STOCK_LEVEL`, `OUT_OF_STOCK`, `BELOW_MINIMUM`, `AT_MINIMUM`, `BELOW_TARGET_MAX` |  |  |
| `reason` | texte |  |  |
| `priorityScore` | entier |  |  |
| `consumptionRateLast30Days` | nombre décimal |  |  |
| `consumptionRatePrev30Days` | nombre décimal |  |  |
| `consumptionTrend` | enum : `RISING`, `STABLE`, `FALLING` |  |  |
| `consumptionTrendPercent` | entier |  |  |
| `daysOfStockRemaining` | entier |  |  |
| `quantityOnOrder` | nombre décimal |  |  |
| `recommendedOrderByDate` | date-heure ISO 8601 UTC |  |  |
| `bestSupplierCode` | texte |  |  |
| `bestSupplierPrice` | entier int64 ⚠ |  |  |
| `bestSupplierLeadTimeDays` | entier |  |  |

<a id="m-ReorderSuggestionSeverity"></a>
#### ReorderSuggestionSeverity

Énumération : `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
