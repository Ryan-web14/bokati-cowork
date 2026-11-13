# ManagementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 12 route(s).

## Routes

<a id="r-patch-sni-api-v1-accounting-accounts-number-non-deductible"></a>
### PATCH /sni/api/v1/accounting/accounts/{number}/non-deductible

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ACCOUNT:CREATE`
- Corps JSON (obligatoire) : `ManagementController.RateRequest`
- Réponse : `Map<String, Object>`
- Modèles : [ManagementController.RateRequest](#m-ManagementController-RateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `nonDeductible`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-liasse-year"></a>
### GET /sni/api/v1/accounting/liasse/{year}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `liasse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `format` | requête | `String` |  | json |

<a id="r-post-sni-api-v1-accounting-liasse-year-finalize"></a>
### POST /sni/api/v1/accounting/liasse/{year}/finalize

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `ClosingService.Edition`
- Modèles : [ClosingService.Edition](#m-ClosingService-Edition)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `finalizeLiasse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-management-cash-forecast"></a>
### GET /sni/api/v1/accounting/management/cash-forecast

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `forecast`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |
| `horizon` | requête | `String` |  | WEEKS |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-management-dashboard"></a>
### GET /sni/api/v1/accounting/management/dashboard

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `ManagementReportService.Dashboard`
- Modèles : [ManagementReportService.Dashboard](#m-ManagementReportService-Dashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-management-ratios"></a>
### GET /sni/api/v1/accounting/management/ratios

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<ManagementReportService.Ratio>`
- Modèles : [ManagementReportService.Ratio](#m-ManagementReportService-Ratio)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `ratios`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-management-sig"></a>
### GET /sni/api/v1/accounting/management/sig

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `sig`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | requête | `String` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-delete-sni-api-v1-accounting-tax-result-adjustments-id"></a>
### DELETE /sni/api/v1/accounting/tax-result/adjustments/{id}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `deleteAdjustment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-tax-result-year"></a>
### GET /sni/api/v1/accounting/tax-result/{year}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `taxResult`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `format` | requête | `String` |  | json |

<a id="r-post-sni-api-v1-accounting-tax-result-year-adjustments"></a>
### POST /sni/api/v1/accounting/tax-result/{year}/adjustments

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `TaxResultService.AdjustmentRequest`
- Réponse : `TaxResultService.Adjustment`
- Modèles : [TaxResultService.Adjustment](#m-TaxResultService-Adjustment), [TaxResultService.AdjustmentRequest](#m-TaxResultService-AdjustmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `addAdjustment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-tax-result-year-post"></a>
### POST /sni/api/v1/accounting/tax-result/{year}/post

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `TaxResultService.TaxResult`
- Modèles : [TaxResultService.TaxResult](#m-TaxResultService-TaxResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `postTax`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-tax-result-year-save"></a>
### POST /sni/api/v1/accounting/tax-result/{year}/save

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `TaxResultService.TaxResult`
- Modèles : [TaxResultService.TaxResult](#m-TaxResultService-TaxResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ManagementController.java`, méthode `saveTaxResult`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ManagementController-RateRequest"></a>
#### ManagementController.RateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rate` | nombre décimal |  |  |

<a id="m-ClosingService-Edition"></a>
#### ClosingService.Edition

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `report` | texte |  |  |
| `fiscalYear` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `templateVersion` | entier |  |  |
| `contentHash` | texte |  |  |
| `derogation` | texte |  |  |
| `generatedBy` | texte |  |  |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ManagementReportService-Dashboard"></a>
#### ManagementReportService.Dashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `revenueMonth` | nombre décimal |  |  |
| `revenueYearToDate` | nombre décimal |  |  |
| `revenuePreviousYearToDate` | nombre décimal |  |  |
| `addedValue` | nombre décimal |  |  |
| `ebitda` | nombre décimal |  |  |
| `netResult` | nombre décimal |  |  |
| `treasury` | nombre décimal |  |  |
| `receivables` | nombre décimal |  |  |
| `overdueReceivables` | nombre décimal |  |  |
| `dso` | entier |  |  |
| `staffAdvances` | nombre décimal |  |  |
| `expensesByNature` | [ManagementReportService.Amount](#m-ManagementReportService-Amount)[] |  |  |

<a id="m-ManagementReportService-Ratio"></a>
#### ManagementReportService.Ratio

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `value` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `formula` | texte |  |  |

<a id="m-TaxResultService-Adjustment"></a>
#### TaxResultService.Adjustment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `accountNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `source` | texte |  |  |
| `comment` | texte |  |  |

<a id="m-TaxResultService-AdjustmentRequest"></a>
#### TaxResultService.AdjustmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `accountNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `comment` | texte |  |  |

<a id="m-TaxResultService-TaxResult"></a>
#### TaxResultService.TaxResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYear` | texte |  |  |
| `accountingResult` | nombre décimal |  |  |
| `reintegrations` | [TaxResultService.Adjustment](#m-TaxResultService-Adjustment)[] |  |  |
| `deductions` | [TaxResultService.Adjustment](#m-TaxResultService-Adjustment)[] |  |  |
| `totalReintegrations` | nombre décimal |  |  |
| `totalDeductions` | nombre décimal |  |  |
| `resultBeforeLosses` | nombre décimal |  |  |
| `lossesAvailable` | nombre décimal |  |  |
| `lossesUsed` | nombre décimal |  |  |
| `taxResult` | nombre décimal |  |  |
| `lossCarriedForward` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `tax` | nombre décimal |  |  |
| `turnover` | nombre décimal |  |  |
| `minimumRate` | nombre décimal |  |  |
| `minimumTax` | nombre décimal |  |  |
| `taxDue` | nombre décimal |  |  |
| `taxBooked` | nombre décimal |  |  |
| `entryNumber` | texte |  |  |
| `status` | texte |  |  |

<a id="m-ManagementReportService-Amount"></a>
#### ManagementReportService.Amount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `value` | nombre décimal |  |  |
