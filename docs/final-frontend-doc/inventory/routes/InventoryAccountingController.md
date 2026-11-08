# InventoryAccountingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-accounting-adjustment-approval-rules"></a>
### GET /sni/api/v1/inventory/accounting/adjustment-approval-rules

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryAdjustmentApprovalRuleResponse>`
- Modèles : [InventoryAdjustmentApprovalRuleResponse](#m-InventoryAdjustmentApprovalRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `listRules`

<a id="r-post-sni-api-v1-inventory-accounting-adjustment-approval-rules"></a>
### POST /sni/api/v1/inventory/accounting/adjustment-approval-rules

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ADJUSTMENT_RULE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryAdjustmentApprovalRuleRequest`
- Réponse : `InventoryAdjustmentApprovalRuleResponse`
- Modèles : [InventoryAdjustmentApprovalRuleResponse](#m-InventoryAdjustmentApprovalRuleResponse), [InventoryAdjustmentApprovalRuleRequest](#m-InventoryAdjustmentApprovalRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `createRule`

<a id="r-get-sni-api-v1-inventory-accounting-adjustment-reasons"></a>
### GET /sni/api/v1/inventory/accounting/adjustment-reasons

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<AdjustmentReasonResponse>`
- Modèles : [AdjustmentReasonResponse](#m-AdjustmentReasonResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `listReasons`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-inventory-accounting-adjustment-reasons"></a>
### POST /sni/api/v1/inventory/accounting/adjustment-reasons

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ADJUSTMENT_REASON_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `AdjustmentReasonRequest`
- Réponse : `AdjustmentReasonResponse`
- Modèles : [AdjustmentReasonResponse](#m-AdjustmentReasonResponse), [AdjustmentReasonRequest](#m-AdjustmentReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `createReason`

<a id="r-get-sni-api-v1-inventory-accounting-journal"></a>
### GET /sni/api/v1/inventory/accounting/journal

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<StockJournalEntryResponse>`
- Modèles : [StockJournalEntryResponse](#m-StockJournalEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `journal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `LocalDate` |  |  |
| `toDate` | requête | `LocalDate` |  |  |
| `periodCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-accounting-journal-csv"></a>
### GET /sni/api/v1/inventory/accounting/journal.csv

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `journalCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `LocalDate` |  |  |
| `toDate` | requête | `LocalDate` |  |  |
| `periodCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-accounting-periods"></a>
### GET /sni/api/v1/inventory/accounting/periods

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryPeriodResponse>`
- Modèles : [InventoryPeriodResponse](#m-InventoryPeriodResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `listPeriods`

<a id="r-post-sni-api-v1-inventory-accounting-periods"></a>
### POST /sni/api/v1/inventory/accounting/periods

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_PERIOD_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryPeriodRequest`
- Réponse : `InventoryPeriodResponse`
- Modèles : [InventoryPeriodResponse](#m-InventoryPeriodResponse), [InventoryPeriodRequest](#m-InventoryPeriodRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `createPeriod`

<a id="r-get-sni-api-v1-inventory-accounting-periods-periodcode"></a>
### GET /sni/api/v1/inventory/accounting/periods/{periodCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryPeriodResponse`
- Modèles : [InventoryPeriodResponse](#m-InventoryPeriodResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `getPeriod`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `periodCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-accounting-periods-periodcode-close"></a>
### PATCH /sni/api/v1/inventory/accounting/periods/{periodCode}/close

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_PERIOD_CLOSE`, voir le README racine)
- Réponse : `InventoryPeriodResponse`
- Modèles : [InventoryPeriodResponse](#m-InventoryPeriodResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `closePeriod`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `periodCode` | chemin | `String` | oui |  |
| `closedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-accounting-periods-periodcode-reopen"></a>
### PATCH /sni/api/v1/inventory/accounting/periods/{periodCode}/reopen

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_PERIOD_REOPEN`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryPeriodReopenRequest`
- Réponse : `InventoryPeriodResponse`
- Modèles : [InventoryPeriodResponse](#m-InventoryPeriodResponse), [InventoryPeriodReopenRequest](#m-InventoryPeriodReopenRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `reopenPeriod`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `periodCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-accounting-snapshots"></a>
### GET /sni/api/v1/inventory/accounting/snapshots

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<StockValuationSnapshotResponse>`
- Modèles : [StockValuationSnapshotResponse](#m-StockValuationSnapshotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `snapshotAt`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `snapshotDate` | requête | `LocalDate` | oui |  |

<a id="r-post-sni-api-v1-inventory-accounting-snapshots"></a>
### POST /sni/api/v1/inventory/accounting/snapshots

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_SNAPSHOT_TAKE`, voir le README racine)
- Réponse : `List<StockValuationSnapshotResponse>`
- Modèles : [StockValuationSnapshotResponse](#m-StockValuationSnapshotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryAccountingController.java`, méthode `takeSnapshot`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `snapshotDate` | requête | `LocalDate` | oui |  |
| `periodCode` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryAdjustmentApprovalRuleResponse"></a>
#### InventoryAdjustmentApprovalRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `approvalLevel` | enum : `NONE`, `MANAGER`, `DIRECTOR`, `EXECUTIVE` |  |  |
| `minAmount` | entier int64 ⚠ |  |  |
| `maxAmount` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryAdjustmentApprovalRuleRequest"></a>
#### InventoryAdjustmentApprovalRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `approvalLevel` | enum : `NONE`, `MANAGER`, `DIRECTOR`, `EXECUTIVE` | oui |  |
| `minAmount` | entier int64 ⚠ | oui |  |
| `maxAmount` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |

<a id="m-AdjustmentReasonResponse"></a>
#### AdjustmentReasonResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte |  |  |
| `label` | texte |  |  |
| `counterpartAccount` | texte |  |  |
| `negativeOnly` | booléen |  |  |
| `positiveOnly` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-AdjustmentReasonRequest"></a>
#### AdjustmentReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte | oui | longueur min `1` |
| `label` | texte | oui | longueur min `1` |
| `counterpartAccount` | texte | oui | longueur min `1` |
| `negativeOnly` | booléen |  |  |
| `positiveOnly` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-StockJournalEntryResponse"></a>
#### StockJournalEntryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `stockAccount` | texte |  |  |
| `counterpartAccount` | texte |  |  |
| `direction` | enum : `DEBIT`, `CREDIT` |  |  |
| `amount` | entier int64 ⚠ |  |  |
| `accountingDate` | date AAAA-MM-JJ |  |  |
| `periodCode` | texte |  |  |
| `label` | texte |  |  |

<a id="m-InventoryPeriodResponse"></a>
#### InventoryPeriodResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `periodCode` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `OPEN`, `CLOSING`, `CLOSED` |  |  |
| `closedBy` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `reopenedBy` | texte |  |  |
| `reopenedAt` | date-heure ISO 8601 UTC |  |  |
| `reopenReason` | texte |  |  |
| `closingStockValue` | entier int64 ⚠ |  |  |

<a id="m-InventoryPeriodRequest"></a>
#### InventoryPeriodRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `periodCode` | texte |  |  |
| `startDate` | date AAAA-MM-JJ | oui |  |
| `endDate` | date AAAA-MM-JJ | oui |  |

<a id="m-InventoryPeriodReopenRequest"></a>
#### InventoryPeriodReopenRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |
| `reopenedBy` | texte |  |  |

<a id="m-StockValuationSnapshotResponse"></a>
#### StockValuationSnapshotResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `snapshotDate` | date AAAA-MM-JJ |  |  |
| `periodCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `quantityOnHand` | nombre décimal |  |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `totalValue` | entier int64 ⚠ |  |  |
| `valuationMethod` | enum : `WEIGHTED_AVERAGE`, `FIFO` |  |  |
| `oldestLayerAgeDays` | entier |  |  |
