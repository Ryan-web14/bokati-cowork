# InventoryAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-admin-dashboard"></a>
### GET /sni/api/v1/inventory/admin/dashboard

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryDashboardResponse`
- Modèles : [InventoryDashboardResponse](#m-InventoryDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `dashboard`

<a id="r-post-sni-api-v1-inventory-admin-labels"></a>
### POST /sni/api/v1/inventory/admin/labels

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Corps JSON (obligatoire) : `InventoryLabelBatchRequest`
- Réponse : `List<InventoryLabelResponse>`
- Modèles : [InventoryLabelResponse](#m-InventoryLabelResponse), [InventoryLabelBatchRequest](#m-InventoryLabelBatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `labels`

<a id="r-get-sni-api-v1-inventory-admin-labels-type-code"></a>
### GET /sni/api/v1/inventory/admin/labels/{type}/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryLabelResponse`
- Modèles : [InventoryLabelResponse](#m-InventoryLabelResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `label`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-admin-reconciliation"></a>
### GET /sni/api/v1/inventory/admin/reconciliation

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `StockReconciliationReportResponse`
- Modèles : [StockReconciliationReportResponse](#m-StockReconciliationReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `reconciliation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-admin-reconciliation-csv"></a>
### GET /sni/api/v1/inventory/admin/reconciliation.csv

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `reconciliationCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-admin-reports-anomalies"></a>
### GET /sni/api/v1/inventory/admin/reports/anomalies

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryAnomalyReportResponse`
- Modèles : [InventoryAnomalyReportResponse](#m-InventoryAnomalyReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `anomalyReport`

<a id="r-get-sni-api-v1-inventory-admin-reports-movements"></a>
### GET /sni/api/v1/inventory/admin/reports/movements

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryMovementReportResponse`
- Modèles : [InventoryMovementReportResponse](#m-InventoryMovementReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `movementReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-admin-reports-movements-csv"></a>
### GET /sni/api/v1/inventory/admin/reports/movements.csv

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `movementReportCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-admin-reports-movements-pdf"></a>
### GET /sni/api/v1/inventory/admin/reports/movements.pdf

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `movementReportPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-admin-reports-overrides"></a>
### GET /sni/api/v1/inventory/admin/reports/overrides

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryOverrideReportResponse`
- Modèles : [InventoryOverrideReportResponse](#m-InventoryOverrideReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `overrideReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `referenceType` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-admin-reports-overrides-csv"></a>
### GET /sni/api/v1/inventory/admin/reports/overrides.csv

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/admin/controller/InventoryAdminController.java`, méthode `overrideReportCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `referenceType` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryDashboardResponse"></a>
#### InventoryDashboardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `openAlerts` | entier int64 ⚠ |  |  |
| `lowStockAlerts` | entier int64 ⚠ |  |  |
| `outOfStockAlerts` | entier int64 ⚠ |  |  |
| `negativeStockAlerts` | entier int64 ⚠ |  |  |
| `expirySoonAlerts` | entier int64 ⚠ |  |  |
| `expiringLots30Days` | entier int64 ⚠ |  |  |
| `outOfStockLevels` | entier int64 ⚠ |  |  |
| `openPurchaseOrders` | entier int64 ⚠ |  |  |
| `totalStockValue` | entier int64 ⚠ |  |  |

<a id="m-InventoryLabelResponse"></a>
#### InventoryLabelResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `labelType` | texte |  |  |
| `code` | texte |  |  |
| `displayText` | texte |  |  |
| `barcodeValue` | texte |  |  |
| `qrValue` | texte |  |  |

<a id="m-InventoryLabelBatchRequest"></a>
#### InventoryLabelBatchRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `items` | [InventoryLabelBatchRequest.Item](#m-InventoryLabelBatchRequest-Item)[] | oui | éléments min `1` |

<a id="m-StockReconciliationReportResponse"></a>
#### StockReconciliationReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `itemCodeFilter` | texte |  |  |
| `locationCodeFilter` | texte |  |  |
| `pairsChecked` | entier int64 ⚠ |  |  |
| `divergenceCount` | entier int64 ⚠ |  |  |
| `totalAbsoluteDifference` | nombre décimal |  |  |
| `consistent` | booléen |  |  |
| `divergences` | [StockReconciliationLineResponse](#m-StockReconciliationLineResponse)[] |  |  |

<a id="m-InventoryAnomalyReportResponse"></a>
#### InventoryAnomalyReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `negativeStockLevels` | entier int64 ⚠ |  |  |
| `negativeStockAlerts` | entier int64 ⚠ |  |  |
| `movementsWithNegativeOverride` | entier int64 ⚠ |  |  |
| `suspiciousAdjustments` | entier int64 ⚠ |  |  |
| `largeInventoryCountVariances` | entier int64 ⚠ |  |  |

<a id="m-InventoryMovementReportResponse"></a>
#### InventoryMovementReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reportTitle` | texte |  |  |
| `itemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `fromDate` | texte |  |  |
| `toDate` | texte |  |  |
| `generatedAt` | texte |  |  |
| `summary` | [InventoryMovementReportResponse.Summary](#m-InventoryMovementReportResponse-Summary) |  |  |
| `lines` | [InventoryMovementReportResponse.Line](#m-InventoryMovementReportResponse-Line)[] |  |  |
| `items` | [InventoryMovementReportResponse.ItemLine](#m-InventoryMovementReportResponse-ItemLine)[] |  |  |
| `locations` | [InventoryMovementReportResponse.LocationLine](#m-InventoryMovementReportResponse-LocationLine)[] |  |  |
| `recentMovements` | [InventoryMovementReportResponse.MovementDetail](#m-InventoryMovementReportResponse-MovementDetail)[] |  |  |

<a id="m-InventoryOverrideReportResponse"></a>
#### InventoryOverrideReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date-heure ISO 8601 UTC |  |  |
| `toDate` | date-heure ISO 8601 UTC |  |  |
| `referenceTypeFilter` | texte |  |  |
| `totalOverrides` | entier int64 ⚠ |  |  |
| `manualOverrides` | entier int64 ⚠ |  |  |
| `breakdown` | [InventoryOverrideReportResponse.Breakdown](#m-InventoryOverrideReportResponse-Breakdown)[] |  |  |
| `lines` | [InventoryOverrideReportResponse.Line](#m-InventoryOverrideReportResponse-Line)[] |  |  |

<a id="m-InventoryLabelBatchRequest-Item"></a>
#### InventoryLabelBatchRequest.Item

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |

<a id="m-StockReconciliationLineResponse"></a>
#### StockReconciliationLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `recordedQuantity` | nombre décimal |  |  |
| `expectedQuantity` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `lastMovementAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-InventoryMovementReportResponse-Summary"></a>
#### InventoryMovementReportResponse.Summary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCount` | entier int64 ⚠ |  |  |
| `totalInQuantity` | nombre décimal |  |  |
| `totalOutQuantity` | nombre décimal |  |  |
| `netQuantity` | nombre décimal |  |  |
| `totalInValue` | entier int64 ⚠ |  |  |
| `totalOutValue` | entier int64 ⚠ |  |  |
| `netValue` | entier int64 ⚠ |  |  |
| `totalMovementValue` | entier int64 ⚠ |  |  |
| `firstMovementAt` | texte |  |  |
| `lastMovementAt` | texte |  |  |

<a id="m-InventoryMovementReportResponse-Line"></a>
#### InventoryMovementReportResponse.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementType` | enum : `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT` |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `totalQuantity` | nombre décimal |  |  |
| `totalValue` | entier int64 ⚠ |  |  |
| `signedQuantity` | nombre décimal |  |  |
| `signedValue` | entier int64 ⚠ |  |  |

<a id="m-InventoryMovementReportResponse-ItemLine"></a>
#### InventoryMovementReportResponse.ItemLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `totalInQuantity` | nombre décimal |  |  |
| `totalOutQuantity` | nombre décimal |  |  |
| `netQuantity` | nombre décimal |  |  |
| `totalValue` | entier int64 ⚠ |  |  |

<a id="m-InventoryMovementReportResponse-LocationLine"></a>
#### InventoryMovementReportResponse.LocationLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `totalInQuantity` | nombre décimal |  |  |
| `totalOutQuantity` | nombre décimal |  |  |
| `netQuantity` | nombre décimal |  |  |
| `totalValue` | entier int64 ⚠ |  |  |

<a id="m-InventoryMovementReportResponse-MovementDetail"></a>
#### InventoryMovementReportResponse.MovementDetail

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCode` | texte |  |  |
| `movementType` | enum : `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT` |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationFromCode` | texte |  |  |
| `locationToCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `totalCost` | entier int64 ⚠ |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | texte |  |  |

<a id="m-InventoryOverrideReportResponse-Breakdown"></a>
#### InventoryOverrideReportResponse.Breakdown

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `referenceType` | texte |  |  |
| `performedBy` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |

<a id="m-InventoryOverrideReportResponse-Line"></a>
#### InventoryOverrideReportResponse.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCode` | texte |  |  |
| `movementType` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `reasonCode` | texte |  |  |
| `reason` | texte |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | date-heure ISO 8601 UTC |  |  |
