# StockPlanningController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-stock-planning-picking"></a>
### GET /sni/api/v1/inventory/stock/planning/picking

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `StockPickingSuggestionResponse`
- Modèles : [StockPickingSuggestionResponse](#m-StockPickingSuggestionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockPlanningController.java`, méthode `picking`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` | oui |  |
| `quantity` | requête | `BigDecimal` | oui |  |

<a id="r-post-sni-api-v1-inventory-stock-planning-unit-conversions"></a>
### POST /sni/api/v1/inventory/stock/planning/unit-conversions

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Corps JSON (obligatoire) : `InventoryUnitConversionRequest`
- Réponse : `InventoryUnitConversionResponse`
- Modèles : [InventoryUnitConversionResponse](#m-InventoryUnitConversionResponse), [InventoryUnitConversionRequest](#m-InventoryUnitConversionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockPlanningController.java`, méthode `createConversion`

<a id="r-get-sni-api-v1-inventory-stock-planning-unit-conversions-itemcode"></a>
### GET /sni/api/v1/inventory/stock/planning/unit-conversions/{itemCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryUnitConversionResponse>`
- Modèles : [InventoryUnitConversionResponse](#m-InventoryUnitConversionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockPlanningController.java`, méthode `conversions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-stock-planning-unit-conversions-itemcode-convert"></a>
### GET /sni/api/v1/inventory/stock/planning/unit-conversions/{itemCode}/convert

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `BigDecimal`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockPlanningController.java`, méthode `convert`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |
| `fromUnitCode` | requête | `String` | oui |  |
| `toUnitCode` | requête | `String` | oui |  |
| `quantity` | requête | `BigDecimal` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StockPickingSuggestionResponse"></a>
#### StockPickingSuggestionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `requestedQuantity` | nombre décimal |  |  |
| `suggestedQuantity` | nombre décimal |  |  |
| `fullyCovered` | booléen |  |  |
| `lines` | [StockPickingSuggestionResponse.Line](#m-StockPickingSuggestionResponse-Line)[] |  |  |

<a id="m-InventoryUnitConversionResponse"></a>
#### InventoryUnitConversionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `fromUnitCode` | texte |  |  |
| `toUnitCode` | texte |  |  |
| `factor` | nombre décimal |  |  |

<a id="m-InventoryUnitConversionRequest"></a>
#### InventoryUnitConversionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `fromUnitCode` | texte | oui | longueur min `1` |
| `toUnitCode` | texte | oui | longueur min `1` |
| `factor` | nombre décimal | oui |  |

<a id="m-StockPickingSuggestionResponse-Line"></a>
#### StockPickingSuggestionResponse.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `availableQuantity` | nombre décimal |  |  |
| `pickQuantity` | nombre décimal |  |  |
