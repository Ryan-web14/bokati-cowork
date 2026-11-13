# BudgetController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 23 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-budget"></a>
### GET /sni/api/v1/accounting/budget

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<BudgetService.Budget>`
- Modèles : [BudgetService.Budget](#m-BudgetService-Budget)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `budgets`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fiscalYearCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-budget"></a>
### POST /sni/api/v1/accounting/budget

- Accès : back-office : rôle d'administration et droit `BUDGET:PREPARE`
- Corps JSON (obligatoire) : `BudgetService.CreateBudget`
- Réponse : `BudgetService.Budget`
- Modèles : [BudgetService.Budget](#m-BudgetService-Budget), [BudgetService.CreateBudget](#m-BudgetService-CreateBudget)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `create`

<a id="r-post-sni-api-v1-accounting-budget-check"></a>
### POST /sni/api/v1/accounting/budget/check

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Corps JSON (obligatoire) : `List<BudgetControl.Spend>`
- Réponse : `BudgetControl.Impact`
- Modèles : [BudgetControl.Impact](#m-BudgetControl-Impact), [BudgetControl.Spend](#m-BudgetControl-Spend)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `check`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-budget-forecasts"></a>
### GET /sni/api/v1/accounting/budget/forecasts

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<AccBudgetForecast>`
- Modèles : [AccBudgetForecast](#m-AccBudgetForecast)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `forecasts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-budget-forecasts"></a>
### POST /sni/api/v1/accounting/budget/forecasts

- Accès : back-office : rôle d'administration et droit `BUDGET_FORECAST:MANAGE`
- Corps JSON (obligatoire) : `BudgetService.ForecastInput`
- Réponse : `AccBudgetForecast`
- Modèles : [AccBudgetForecast](#m-AccBudgetForecast), [BudgetService.ForecastInput](#m-BudgetService-ForecastInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `createForecast`

<a id="r-put-sni-api-v1-accounting-budget-forecasts-number"></a>
### PUT /sni/api/v1/accounting/budget/forecasts/{number}

- Accès : back-office : rôle d'administration et droit `BUDGET_FORECAST:MANAGE`
- Corps JSON (obligatoire) : `BudgetService.ForecastInput`
- Réponse : `AccBudgetForecast`
- Modèles : [AccBudgetForecast](#m-AccBudgetForecast), [BudgetService.ForecastInput](#m-BudgetService-ForecastInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `updateForecast`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-budget-forecasts-number-status"></a>
### PATCH /sni/api/v1/accounting/budget/forecasts/{number}/status

- Accès : back-office : rôle d'administration et droit `BUDGET_FORECAST:MANAGE`
- Réponse : `AccBudgetForecast`
- Modèles : [AccBudgetForecast](#m-AccBudgetForecast)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `forecastStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `status` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-budget-items"></a>
### GET /sni/api/v1/accounting/budget/items

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<BudgetService.Item>`
- Modèles : [BudgetService.Item](#m-BudgetService-Item)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `items`

<a id="r-post-sni-api-v1-accounting-budget-items"></a>
### POST /sni/api/v1/accounting/budget/items

- Accès : back-office : rôle d'administration et droit `BUDGET:PREPARE`
- Corps JSON (obligatoire) : `BudgetService.ItemInput`
- Réponse : `BudgetService.Item`
- Modèles : [BudgetService.Item](#m-BudgetService-Item), [BudgetService.ItemInput](#m-BudgetService-ItemInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `saveItem`

<a id="r-patch-sni-api-v1-accounting-budget-items-code-decision"></a>
### PATCH /sni/api/v1/accounting/budget/items/{code}/{decision}

- Accès : back-office : rôle d'administration et droit `BUDGET:PREPARE`
- Réponse : `BudgetService.Item`
- Modèles : [BudgetService.Item](#m-BudgetService-Item)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `decideItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `decision` | chemin | `String` | oui |  |
| `into` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-accounting-budget-lines-lineid-freeze"></a>
### PATCH /sni/api/v1/accounting/budget/lines/{lineId}/freeze

- Accès : back-office : rôle d'administration et droit `BUDGET:PREPARE`
- Réponse : `BudgetService.Line`
- Modèles : [BudgetService.Line](#m-BudgetService-Line)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `freeze`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lineId` | chemin | `Long` | oui |  |
| `frozen` | requête | `boolean` |  | true |

<a id="r-get-sni-api-v1-accounting-budget-reports-commitments"></a>
### GET /sni/api/v1/accounting/budget/reports/commitments

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<BudgetReportService.Commitment>`
- Modèles : [BudgetReportService.Commitment](#m-BudgetReportService-Commitment)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `commitments`

<a id="r-get-sni-api-v1-accounting-budget-reports-follow-up"></a>
### GET /sni/api/v1/accounting/budget/reports/follow-up

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<BudgetReportService.FollowUp>`
- Modèles : [BudgetReportService.FollowUp](#m-BudgetReportService-FollowUp)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `followUp`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fiscalYearCode` | requête | `String` | oui |  |
| `month` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-accounting-budget-reports-forecasts"></a>
### GET /sni/api/v1/accounting/budget/reports/forecasts

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<BudgetReportService.ForecastRow>`
- Modèles : [BudgetReportService.ForecastRow](#m-BudgetReportService-ForecastRow)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `forecastFollowUp`

<a id="r-get-sni-api-v1-accounting-budget-reports-planned"></a>
### GET /sni/api/v1/accounting/budget/reports/planned

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<BudgetReportService.PlannedSplit>`
- Modèles : [BudgetReportService.PlannedSplit](#m-BudgetReportService-PlannedSplit)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `planned`

<a id="r-get-sni-api-v1-accounting-budget-reports-reserve"></a>
### GET /sni/api/v1/accounting/budget/reports/reserve

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `Map<String, BigDecimal>`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `reserve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fiscalYearCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-budget-transfers"></a>
### GET /sni/api/v1/accounting/budget/transfers

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<AccBudgetTransfer>`
- Modèles : [AccBudgetTransfer](#m-AccBudgetTransfer)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `transfers`

<a id="r-post-sni-api-v1-accounting-budget-transfers"></a>
### POST /sni/api/v1/accounting/budget/transfers

- Accès : back-office : rôle d'administration et droit `BUDGET:TRANSFER_REQUEST`
- Corps JSON (obligatoire) : `BudgetService.TransferInput`
- Réponse : `AccBudgetTransfer`
- Modèles : [AccBudgetTransfer](#m-AccBudgetTransfer), [BudgetService.TransferInput](#m-BudgetService-TransferInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `requestTransfer`

<a id="r-patch-sni-api-v1-accounting-budget-transfers-id-decision"></a>
### PATCH /sni/api/v1/accounting/budget/transfers/{id}/{decision}

- Accès : back-office : rôle d'administration et droit `BUDGET:TRANSFER_REQUEST`
- Réponse : `AccBudgetTransfer`
- Modèles : [AccBudgetTransfer](#m-AccBudgetTransfer)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `decideTransfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `decision` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-budget-unplanned-reasons"></a>
### GET /sni/api/v1/accounting/budget/unplanned-reasons

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `List<Map<String, Object>>`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `unplannedReasons`

<a id="r-get-sni-api-v1-accounting-budget-id"></a>
### GET /sni/api/v1/accounting/budget/{id}

- Accès : back-office : rôle d'administration et droit `BUDGET_REPORT:READ`
- Réponse : `BudgetService.Budget`
- Modèles : [BudgetService.Budget](#m-BudgetService-Budget)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `budget`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-budget-id-lines"></a>
### POST /sni/api/v1/accounting/budget/{id}/lines

- Accès : back-office : rôle d'administration et droit `BUDGET:PREPARE`
- Corps JSON (obligatoire) : `BudgetService.LineInput`
- Réponse : `BudgetService.Line`
- Modèles : [BudgetService.Line](#m-BudgetService-Line), [BudgetService.LineInput](#m-BudgetService-LineInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `line`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-accounting-budget-id-action"></a>
### PATCH /sni/api/v1/accounting/budget/{id}/{action}

- Accès : back-office : rôle d'administration et droit `BUDGET:PREPARE`
- Corps JSON (facultatif) : `Map<String, String>`
- Réponse : `BudgetService.Budget`
- Modèles : [BudgetService.Budget](#m-BudgetService-Budget)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BudgetController.java`, méthode `transition`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `action` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BudgetService-Budget"></a>
#### BudgetService.Budget

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `fiscalYearCode` | texte |  |  |
| `version` | entier |  |  |
| `label` | texte |  |  |
| `status` | texte |  |  |
| `reason` | texte |  |  |
| `submittedBy` | texte |  |  |
| `consolidatedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `totalCharges` | nombre décimal |  |  |
| `totalProducts` | nombre décimal |  |  |
| `lines` | [BudgetService.Line](#m-BudgetService-Line)[] |  |  |

<a id="m-BudgetService-CreateBudget"></a>
#### BudgetService.CreateBudget

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYearCode` | texte |  |  |
| `label` | texte |  |  |
| `reason` | texte |  |  |
| `basis` | texte |  |  |
| `evolutionPct` | nombre décimal |  |  |

<a id="m-BudgetControl-Impact"></a>
#### BudgetControl.Impact

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `budgetActive` | booléen |  |  |
| `lines` | [BudgetControl.LineImpact](#m-BudgetControl-LineImpact)[] |  |  |
| `blocked` | booléen |  |  |
| `unbudgeted` | texte[] |  |  |
| `messages` | texte[] |  |  |

<a id="m-BudgetControl-Spend"></a>
#### BudgetControl.Spend

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `natureCode` | texte |  |  |
| `accountNumber` | texte |  |  |
| `sectionCode` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-AccBudgetForecast"></a>
#### AccBudgetForecast

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `itemCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `subject` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `plannedMonth` | texte |  |  |
| `recurrence` | texte |  |  |
| `occurrences` | entier |  |  |
| `supplier` | texte |  |  |
| `responsible` | texte |  |  |
| `priority` | texte |  |  |
| `status` | texte |  |  |
| `realizedAmount` | nombre décimal |  |  |
| `remindedAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BudgetService-ForecastInput"></a>
#### BudgetService.ForecastInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `subject` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `plannedMonth` | texte |  |  |
| `recurrence` | texte |  |  |
| `occurrences` | entier |  |  |
| `supplier` | texte |  |  |
| `responsible` | texte |  |  |
| `priority` | texte |  |  |

<a id="m-BudgetService-Item"></a>
#### BudgetService.Item

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `parentCode` | texte |  |  |
| `itemType` | texte |  |  |
| `responsible` | texte |  |  |
| `controlLevel` | texte |  |  |
| `tolerancePct` | nombre décimal |  |  |
| `controlAtParent` | booléen |  |  |
| `status` | texte |  |  |
| `mergedInto` | texte |  |  |
| `accounts` | texte[] |  |  |
| `expenseItems` | texte[] |  |  |
| `natures` | texte[] |  |  |

<a id="m-BudgetService-ItemInput"></a>
#### BudgetService.ItemInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `parentCode` | texte |  |  |
| `itemType` | texte |  |  |
| `responsible` | texte |  |  |
| `controlLevel` | texte |  |  |
| `tolerancePct` | nombre décimal |  |  |
| `controlAtParent` | booléen |  |  |
| `accounts` | texte[] |  |  |
| `expenseItems` | texte[] |  |  |
| `natures` | texte[] |  |  |

<a id="m-BudgetService-Line"></a>
#### BudgetService.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `itemLabel` | texte |  |  |
| `sectionCode` | texte |  |  |
| `annualAmount` | nombre décimal |  |  |
| `months` | nombre décimal[] |  |  |
| `controlLevel` | texte |  |  |
| `tolerancePct` | nombre décimal |  |  |
| `responsible` | texte |  |  |
| `frozen` | booléen |  |  |
| `committed` | nombre décimal |  |  |
| `realized` | nombre décimal |  |  |
| `available` | nombre décimal |  |  |
| `consumptionPct` | nombre décimal |  |  |

<a id="m-BudgetReportService-Commitment"></a>
#### BudgetReportService.Commitment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sourceType` | texte |  |  |
| `reference` | texte |  |  |
| `itemCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `open` | nombre décimal |  |  |
| `forecastNumber` | texte |  |  |
| `unplanned` | booléen |  |  |
| `unplannedCode` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `ageDays` | entier int64 ⚠ |  |  |

<a id="m-BudgetReportService-FollowUp"></a>
#### BudgetReportService.FollowUp

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `line` | [BudgetService.Line](#m-BudgetService-Line) |  |  |
| `months` | [BudgetReportService.Month](#m-BudgetReportService-Month)[] |  |  |
| `projection` | nombre décimal |  |  |
| `projectedVariance` | nombre décimal |  |  |

<a id="m-BudgetReportService-ForecastRow"></a>
#### BudgetReportService.ForecastRow

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `forecast` | [AccBudgetForecast](#m-AccBudgetForecast) |  |  |
| `overdue` | booléen |  |  |
| `gap` | nombre décimal |  |  |

<a id="m-BudgetReportService-PlannedSplit"></a>
#### BudgetReportService.PlannedSplit

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `planned` | nombre décimal |  |  |
| `unplanned` | nombre décimal |  |  |
| `unplannedPct` | nombre décimal |  |  |
| `reasons` | objet { clé : nombre décimal } |  |  |

<a id="m-AccBudgetTransfer"></a>
#### AccBudgetTransfer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `fromLineId` | entier int64 ⚠ |  |  |
| `toLineId` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |
| `reason` | texte |  |  |
| `status` | texte |  |  |
| `requestedBy` | texte |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BudgetService-TransferInput"></a>
#### BudgetService.TransferInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fromLineId` | entier int64 ⚠ |  |  |
| `toLineId` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |
| `reason` | texte |  |  |

<a id="m-BudgetService-LineInput"></a>
#### BudgetService.LineInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `annualAmount` | nombre décimal |  |  |
| `profile` | texte |  |  |
| `months` | nombre décimal[] |  |  |
| `controlLevel` | texte |  |  |
| `tolerancePct` | nombre décimal |  |  |
| `responsible` | texte |  |  |

<a id="m-BudgetControl-LineImpact"></a>
#### BudgetControl.LineImpact

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineId` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `itemLabel` | texte |  |  |
| `sectionCode` | texte |  |  |
| `controlLevel` | texte |  |  |
| `budgeted` | nombre décimal |  |  |
| `committed` | nombre décimal |  |  |
| `realized` | nombre décimal |  |  |
| `available` | nombre décimal |  |  |
| `requested` | nombre décimal |  |  |
| `availableAfter` | nombre décimal |  |  |
| `status` | enum : `GREEN`, `ORANGE`, `RED` |  |  |
| `openForecasts` | texte[] |  |  |

<a id="m-BudgetReportService-Month"></a>
#### BudgetReportService.Month

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `month` | texte |  |  |
| `budgeted` | nombre décimal |  |  |
| `realizedCumulative` | nombre décimal |  |  |
| `budgetedCumulative` | nombre décimal |  |  |
