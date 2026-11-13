# AnalyticController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 25 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-analytic-axes"></a>
### GET /sni/api/v1/accounting/analytic/axes

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticService.Axis>`
- Modèles : [AnalyticService.Axis](#m-AnalyticService-Axis)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `axes`

<a id="r-post-sni-api-v1-accounting-analytic-axes"></a>
### POST /sni/api/v1/accounting/analytic/axes

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AnalyticService.AxisInput`
- Réponse : `AnalyticService.Axis`
- Modèles : [AnalyticService.Axis](#m-AnalyticService-Axis), [AnalyticService.AxisInput](#m-AnalyticService-AxisInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `saveAxis`

<a id="r-get-sni-api-v1-accounting-analytic-axes-axis-sections"></a>
### GET /sni/api/v1/accounting/analytic/axes/{axis}/sections

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticService.Section>`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `tree`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `includeClosed` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-accounting-analytic-axes-axis-sections"></a>
### POST /sni/api/v1/accounting/analytic/axes/{axis}/sections

- Accès : back-office : rôle d'administration et droit `ANALYTIC_ITEM:CREATE`
- Corps JSON (obligatoire) : `AnalyticService.SectionInput`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section), [AnalyticService.SectionInput](#m-AnalyticService-SectionInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `create`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-analytic-axes-axis-sections-code"></a>
### PATCH /sni/api/v1/accounting/analytic/axes/{axis}/sections/{code}

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AnalyticService.SectionInput`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section), [AnalyticService.SectionInput](#m-AnalyticService-SectionInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-analytic-axes-axis-sections-code-approve"></a>
### PATCH /sni/api/v1/accounting/analytic/axes/{axis}/sections/{code}/approve

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-analytic-axes-axis-sections-code-close"></a>
### PATCH /sni/api/v1/accounting/analytic/axes/{axis}/sections/{code}/close

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `close`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-analytic-axes-axis-sections-code-merge"></a>
### PATCH /sni/api/v1/accounting/analytic/axes/{axis}/sections/{code}/merge

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `merge`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |
| `into` | requête | `String` | oui |  |
| `restate` | requête | `boolean` |  | false |

<a id="r-patch-sni-api-v1-accounting-analytic-axes-axis-sections-code-reject"></a>
### PATCH /sni/api/v1/accounting/analytic/axes/{axis}/sections/{code}/reject

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-analytic-axes-axis-sections-code-reopen"></a>
### PATCH /sni/api/v1/accounting/analytic/axes/{axis}/sections/{code}/reopen

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `AnalyticService.Section`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `reopen`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-analytic-distribution"></a>
### POST /sni/api/v1/accounting/analytic/distribution

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `List<AccAnalyticDistribution>`
- Modèles : [AccAnalyticDistribution](#m-AccAnalyticDistribution)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `distribute`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `periodCode` | requête | `String` | oui |  |
| `axisCode` | requête | `String` | oui |  |
| `keyCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-entries-reference-allocations"></a>
### GET /sni/api/v1/accounting/analytic/entries/{reference}/allocations

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticService.Allocation>`
- Modèles : [AnalyticService.Allocation](#m-AnalyticService-Allocation)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `allocations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-keys"></a>
### GET /sni/api/v1/accounting/analytic/keys

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticService.Key>`
- Modèles : [AnalyticService.Key](#m-AnalyticService-Key)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `keys`

<a id="r-post-sni-api-v1-accounting-analytic-keys"></a>
### POST /sni/api/v1/accounting/analytic/keys

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AnalyticService.KeyInput`
- Réponse : `AnalyticService.Key`
- Modèles : [AnalyticService.Key](#m-AnalyticService-Key), [AnalyticService.KeyInput](#m-AnalyticService-KeyInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `saveKey`

<a id="r-post-sni-api-v1-accounting-analytic-reclass"></a>
### POST /sni/api/v1/accounting/analytic/reclass

- Accès : back-office : rôle d'administration et droit `ANALYTIC:RECLASS`
- Corps JSON (obligatoire) : `AnalyticService.Reclass`
- Réponse : `AnalyticService.Allocation`
- Modèles : [AnalyticService.Allocation](#m-AnalyticService-Allocation), [AnalyticService.Reclass](#m-AnalyticService-Reclass)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `reclass`

<a id="r-get-sni-api-v1-accounting-analytic-reports-balance"></a>
### GET /sni/api/v1/accounting/analytic/reports/balance

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticReportService.BalanceLine>`
- Modèles : [AnalyticReportService.BalanceLine](#m-AnalyticReportService-BalanceLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `balance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | requête | `String` |  | ACTIVITE |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-reports-control"></a>
### GET /sni/api/v1/accounting/analytic/reports/control

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `Map<String, BigDecimal>`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `control`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | requête | `String` |  | ACTIVITE |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-reports-dashboard"></a>
### GET /sni/api/v1/accounting/analytic/reports/dashboard

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticReportService.DashboardRow>`
- Modèles : [AnalyticReportService.DashboardRow](#m-AnalyticReportService-DashboardRow)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-reports-items-item"></a>
### GET /sni/api/v1/accounting/analytic/reports/items/{item}

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `AnalyticReportService.ItemAnalysis`
- Modèles : [AnalyticReportService.ItemAnalysis](#m-AnalyticReportService-ItemAnalysis)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `item`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `item` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-reports-result"></a>
### GET /sni/api/v1/accounting/analytic/reports/result

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `AnalyticReportService.Result`
- Modèles : [AnalyticReportService.Result](#m-AnalyticReportService-Result)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `result`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `axis` | requête | `String` |  | ACTIVITE |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-rules"></a>
### GET /sni/api/v1/accounting/analytic/rules

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AccAnalyticRule>`
- Modèles : [AccAnalyticRule](#m-AccAnalyticRule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `rules`

<a id="r-post-sni-api-v1-accounting-analytic-rules"></a>
### POST /sni/api/v1/accounting/analytic/rules

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AnalyticService.RuleInput`
- Réponse : `AccAnalyticRule`
- Modèles : [AccAnalyticRule](#m-AccAnalyticRule), [AnalyticService.RuleInput](#m-AnalyticService-RuleInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `createRule`

<a id="r-delete-sni-api-v1-accounting-analytic-rules-id"></a>
### DELETE /sni/api/v1/accounting/analytic/rules/{id}

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `deleteRule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-accounting-analytic-rules-id"></a>
### PUT /sni/api/v1/accounting/analytic/rules/{id}

- Accès : back-office : rôle d'administration et droit `ANALYTIC_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AnalyticService.RuleInput`
- Réponse : `AccAnalyticRule`
- Modèles : [AccAnalyticRule](#m-AccAnalyticRule), [AnalyticService.RuleInput](#m-AnalyticService-RuleInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `updateRule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-analytic-sections-pending"></a>
### GET /sni/api/v1/accounting/analytic/sections/pending

- Accès : back-office : rôle d'administration et droit `ANALYTIC_REPORT:READ`
- Réponse : `List<AnalyticService.Section>`
- Modèles : [AnalyticService.Section](#m-AnalyticService-Section)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AnalyticController.java`, méthode `pending`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AnalyticService-Axis"></a>
#### AnalyticService.Axis

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `requiredOn` | texte |  |  |
| `defaultSection` | texte |  |  |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `sections` | entier int64 ⚠ |  |  |

<a id="m-AnalyticService-AxisInput"></a>
#### AnalyticService.AxisInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `requiredOn` | texte |  |  |
| `defaultSection` | texte |  |  |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-AnalyticService-Section"></a>
#### AnalyticService.Section

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `axisCode` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `parentCode` | texte |  |  |
| `family` | texte |  |  |
| `costBehavior` | texte |  |  |
| `auxiliary` | booléen |  |  |
| `responsible` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validTo` | date AAAA-MM-JJ |  |  |
| `status` | texte |  |  |
| `mergedInto` | texte |  |  |
| `color` | texte |  |  |
| `sortOrder` | entier |  |  |
| `createdBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `accounts` | texte[] |  |  |
| `children` | [AnalyticService.Section](#m-AnalyticService-Section)[] |  |  |

<a id="m-AnalyticService-SectionInput"></a>
#### AnalyticService.SectionInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `parentCode` | texte |  |  |
| `family` | texte |  |  |
| `costBehavior` | texte |  |  |
| `auxiliary` | booléen |  |  |
| `responsible` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validTo` | date AAAA-MM-JJ |  |  |
| `color` | texte |  |  |
| `sortOrder` | entier |  |  |
| `accounts` | texte[] |  |  |

<a id="m-AccAnalyticDistribution"></a>
#### AccAnalyticDistribution

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `periodCode` | texte |  |  |
| `axisCode` | texte |  |  |
| `fromSection` | texte |  |  |
| `toSection` | texte |  |  |
| `keyCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AnalyticService-Allocation"></a>
#### AnalyticService.Allocation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entryLineId` | entier int64 ⚠ |  |  |
| `axisCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `percentage` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `origin` | texte |  |  |

<a id="m-AnalyticService-Key"></a>
#### AnalyticService.Key

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `axisCode` | texte |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `lines` | [AnalyticService.KeyLine](#m-AnalyticService-KeyLine)[] |  |  |

<a id="m-AnalyticService-KeyInput"></a>
#### AnalyticService.KeyInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `axisCode` | texte |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `lines` | [AnalyticService.KeyLine](#m-AnalyticService-KeyLine)[] |  |  |

<a id="m-AnalyticService-Reclass"></a>
#### AnalyticService.Reclass

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entryLineId` | entier int64 ⚠ |  |  |
| `axisCode` | texte |  |  |
| `fromSection` | texte |  |  |
| `toSection` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `reason` | texte |  |  |

<a id="m-AnalyticReportService-BalanceLine"></a>
#### AnalyticReportService.BalanceLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `section` | texte |  |  |
| `label` | texte |  |  |
| `account` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `balance` | nombre décimal |  |  |

<a id="m-AnalyticReportService-DashboardRow"></a>
#### AnalyticReportService.DashboardRow

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `item` | texte |  |  |
| `label` | texte |  |  |
| `family` | texte |  |  |
| `period` | nombre décimal |  |  |
| `previousPeriod` | nombre décimal |  |  |
| `variancePercent` | nombre décimal |  |  |
| `averageThreeMonths` | nombre décimal |  |  |
| `drift` | nombre décimal |  |  |

<a id="m-AnalyticReportService-ItemAnalysis"></a>
#### AnalyticReportService.ItemAnalysis

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `item` | texte |  |  |
| `label` | texte |  |  |
| `family` | texte |  |  |
| `costBehavior` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `period` | nombre décimal |  |  |
| `yearToDate` | nombre décimal |  |  |
| `previousYearSamePeriod` | nombre décimal |  |  |
| `variancePercent` | nombre décimal |  |  |
| `projectionYearEnd` | nombre décimal |  |  |
| `monthly` | [AnalyticReportService.Month](#m-AnalyticReportService-Month)[] |  |  |
| `bySite` | [AnalyticReportService.Breakdown](#m-AnalyticReportService-Breakdown)[] |  |  |
| `byCentre` | [AnalyticReportService.Breakdown](#m-AnalyticReportService-Breakdown)[] |  |  |
| `bySupplier` | [AnalyticReportService.Breakdown](#m-AnalyticReportService-Breakdown)[] |  |  |
| `byJournal` | [AnalyticReportService.Breakdown](#m-AnalyticReportService-Breakdown)[] |  |  |
| `detail` | [AnalyticReportService.Detail](#m-AnalyticReportService-Detail)[] |  |  |
| `alerts` | texte[] |  |  |

<a id="m-AnalyticReportService-Result"></a>
#### AnalyticReportService.Result

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `axis` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `sections` | [AnalyticReportService.SectionResult](#m-AnalyticReportService-SectionResult)[] |  |  |
| `total` | [AnalyticReportService.SectionResult](#m-AnalyticReportService-SectionResult) |  |  |

<a id="m-AccAnalyticRule"></a>
#### AccAnalyticRule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `priority` | entier |  |  |
| `axisCode` | texte |  |  |
| `accountPrefix` | texte |  |  |
| `journalCode` | texte |  |  |
| `sourceType` | texte |  |  |
| `thirdParty` | texte |  |  |
| `sectionCode` | texte |  |  |
| `keyCode` | texte |  |  |
| `label` | texte |  |  |
| `active` | booléen |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AnalyticService-RuleInput"></a>
#### AnalyticService.RuleInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `priority` | entier |  |  |
| `axisCode` | texte |  |  |
| `accountPrefix` | texte |  |  |
| `journalCode` | texte |  |  |
| `sourceType` | texte |  |  |
| `thirdParty` | texte |  |  |
| `sectionCode` | texte |  |  |
| `keyCode` | texte |  |  |
| `label` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-AnalyticService-KeyLine"></a>
#### AnalyticService.KeyLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sectionCode` | texte |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-AnalyticReportService-Month"></a>
#### AnalyticReportService.Month

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `month` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-AnalyticReportService-Breakdown"></a>
#### AnalyticReportService.Breakdown

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `label` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-AnalyticReportService-Detail"></a>
#### AnalyticReportService.Detail

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `entryNumber` | texte |  |  |
| `journal` | texte |  |  |
| `label` | texte |  |  |
| `account` | texte |  |  |
| `thirdParty` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-AnalyticReportService-SectionResult"></a>
#### AnalyticReportService.SectionResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `section` | texte |  |  |
| `label` | texte |  |  |
| `products` | nombre décimal |  |  |
| `directCharges` | nombre décimal |  |  |
| `distributedIn` | nombre décimal |  |  |
| `distributedOut` | nombre décimal |  |  |
| `margin` | nombre décimal |  |  |
| `result` | nombre décimal |  |  |
