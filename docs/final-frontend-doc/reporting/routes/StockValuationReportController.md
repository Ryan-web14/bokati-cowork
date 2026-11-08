# StockValuationReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-stock-valuation"></a>
### GET /sni/api/v1/reporting/stock-valuation

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `StockValuationReportResponse`
- Modèles : [StockValuationReportResponse](#m-StockValuationReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/StockValuationReportController.java`, méthode `stockValuation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `inactiveDays` | requête | `int` |  | 30 |

<a id="r-get-sni-api-v1-reporting-stock-valuation-pdf"></a>
### GET /sni/api/v1/reporting/stock-valuation/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/StockValuationReportController.java`, méthode `stockValuationPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `inactiveDays` | requête | `int` |  | 30 |

<a id="r-get-sni-api-v1-reports-stock-valuation"></a>
### GET /sni/api/v1/reports/stock-valuation

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `StockValuationReportResponse`
- Modèles : [StockValuationReportResponse](#m-StockValuationReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/StockValuationReportController.java`, méthode `stockValuation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `inactiveDays` | requête | `int` |  | 30 |

<a id="r-get-sni-api-v1-reports-stock-valuation-pdf"></a>
### GET /sni/api/v1/reports/stock-valuation/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/StockValuationReportController.java`, méthode `stockValuationPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `inactiveDays` | requête | `int` |  | 30 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StockValuationReportResponse"></a>
#### StockValuationReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `totalValue` | nombre décimal |  |  |
| `totalItems` | entier int64 ⚠ |  |  |
| `totalLocations` | entier int64 ⚠ |  |  |
| `byCategory` | [StockValuationReportResponse.CategoryValuation](#m-StockValuationReportResponse-CategoryValuation)[] |  |  |
| `byLocation` | [StockValuationReportResponse.LocationValuation](#m-StockValuationReportResponse-LocationValuation)[] |  |  |
| `lowStockItems` | [StockValuationReportResponse.LowStockItem](#m-StockValuationReportResponse-LowStockItem)[] |  |  |
| `deadStockItems` | [StockValuationReportResponse.DeadStockItem](#m-StockValuationReportResponse-DeadStockItem)[] |  |  |

<a id="m-StockValuationReportResponse-CategoryValuation"></a>
#### StockValuationReportResponse.CategoryValuation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `categoryCode` | texte |  |  |
| `categoryName` | texte |  |  |
| `itemCount` | entier int64 ⚠ |  |  |
| `totalQuantity` | nombre décimal |  |  |
| `totalValue` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-StockValuationReportResponse-LocationValuation"></a>
#### StockValuationReportResponse.LocationValuation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `locationType` | texte |  |  |
| `itemCount` | entier int64 ⚠ |  |  |
| `totalQuantity` | nombre décimal |  |  |
| `totalValue` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-StockValuationReportResponse-LowStockItem"></a>
#### StockValuationReportResponse.LowStockItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `categoryName` | texte |  |  |
| `quantityAvailable` | nombre décimal |  |  |
| `reorderPoint` | nombre décimal |  |  |
| `reorderQuantity` | nombre décimal |  |  |

<a id="m-StockValuationReportResponse-DeadStockItem"></a>
#### StockValuationReportResponse.DeadStockItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `categoryName` | texte |  |  |
| `locationName` | texte |  |  |
| `quantityOnHand` | nombre décimal |  |  |
| `stockValue` | nombre décimal |  |  |
| `lastMovementDate` | date AAAA-MM-JJ |  |  |
| `daysSinceLastMovement` | entier int64 ⚠ |  |  |
