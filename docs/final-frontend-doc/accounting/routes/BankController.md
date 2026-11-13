# BankController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 32 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-bank-accounts"></a>
### GET /sni/api/v1/accounting/bank-accounts

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankAccount>`
- Modèles : [BankAccount](#m-BankAccount)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `accounts`

<a id="r-post-sni-api-v1-accounting-bank-accounts"></a>
### POST /sni/api/v1/accounting/bank-accounts

- Accès : back-office : rôle d'administration et droit `BANK_ACCOUNT:MANAGE`
- Corps JSON (obligatoire) : `BankAccount`
- Réponse : `BankAccount`
- Modèles : [BankAccount](#m-BankAccount)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `saveAccount`

<a id="r-get-sni-api-v1-accounting-bank-accounts-code-matching"></a>
### GET /sni/api/v1/accounting/bank-accounts/{code}/matching

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `BankService.MatchingScreen`
- Modèles : [BankService.MatchingScreen](#m-BankService-MatchingScreen)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `screen`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-bank-accounts-code-matching"></a>
### POST /sni/api/v1/accounting/bank-accounts/{code}/matching

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Corps JSON (obligatoire) : `BankController.MatchRequest`
- Réponse : `BankMatching`
- Modèles : [BankMatching](#m-BankMatching), [BankController.MatchRequest](#m-BankController-MatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `match`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-bank-accounts-code-matching-auto"></a>
### POST /sni/api/v1/accounting/bank-accounts/{code}/matching/auto

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `auto`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-accounting-bank-accounts-code-matching-matching"></a>
### DELETE /sni/api/v1/accounting/bank-accounts/{code}/matching/{matching}

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Corps JSON (obligatoire) : `BankController.Reason`
- Réponse : `BankMatching`
- Modèles : [BankMatching](#m-BankMatching), [BankController.Reason](#m-BankController-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `unmatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `matching` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-bank-accounts-code-reconciliations"></a>
### GET /sni/api/v1/accounting/bank-accounts/{code}/reconciliations

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankReconciliation>`
- Modèles : [BankReconciliation](#m-BankReconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `reconciliations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-bank-accounts-code-reconciliations"></a>
### POST /sni/api/v1/accounting/bank-accounts/{code}/reconciliations

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Réponse : `BankService.Reconciliation`
- Modèles : [BankService.Reconciliation](#m-BankService-Reconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `prepare`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `date` | requête | `LocalDate` | oui |  |

<a id="r-post-sni-api-v1-accounting-bank-accounts-code-statement-lines-id-post"></a>
### POST /sni/api/v1/accounting/bank-accounts/{code}/statement-lines/{id}/post

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Corps JSON (facultatif) : `BankService.PostLine`
- Réponse : `BankService.Line`
- Modèles : [BankService.Line](#m-BankService-Line), [BankService.PostLine](#m-BankService-PostLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `post`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-bank-accounts-code-statements"></a>
### GET /sni/api/v1/accounting/bank-accounts/{code}/statements

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankStatement>`
- Modèles : [BankStatement](#m-BankStatement)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `statements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-bank-accounts-code-statements"></a>
### POST /sni/api/v1/accounting/bank-accounts/{code}/statements

- Accès : back-office : rôle d'administration et droit `BANK_STATEMENT:IMPORT`
- Envoi : `multipart/form-data`
- Réponse : `BankService.ImportResult`
- Modèles : [BankService.ImportResult](#m-BankService-ImportResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `importFile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `openingBalance` | requête | `BigDecimal` |  |  |
| `closingBalance` | requête | `BigDecimal` |  |  |
| `replace` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-bank-accounts-code-statements-manual"></a>
### POST /sni/api/v1/accounting/bank-accounts/{code}/statements/manual

- Accès : back-office : rôle d'administration et droit `BANK_STATEMENT:IMPORT`
- Envoi : `application/json`
- Corps JSON (obligatoire) : `BankService.ManualStatement`
- Réponse : `BankService.ImportResult`
- Modèles : [BankService.ImportResult](#m-BankService-ImportResult), [BankService.ManualStatement](#m-BankService-ManualStatement)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `importManual`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-bank-assignment-rules"></a>
### GET /sni/api/v1/accounting/bank-assignment-rules

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankAssignmentRule>`
- Modèles : [BankAssignmentRule](#m-BankAssignmentRule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `rules`

<a id="r-post-sni-api-v1-accounting-bank-assignment-rules"></a>
### POST /sni/api/v1/accounting/bank-assignment-rules

- Accès : back-office : rôle d'administration et droit `BANK_ACCOUNT:MANAGE`
- Corps JSON (obligatoire) : `BankAssignmentRule`
- Réponse : `BankAssignmentRule`
- Modèles : [BankAssignmentRule](#m-BankAssignmentRule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `saveRule`

<a id="r-get-sni-api-v1-accounting-bank-dashboard"></a>
### GET /sni/api/v1/accounting/bank-dashboard

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankService.Dashboard>`
- Modèles : [BankService.Dashboard](#m-BankService-Dashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `dashboard`

<a id="r-get-sni-api-v1-accounting-bank-import-templates"></a>
### GET /sni/api/v1/accounting/bank-import-templates

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankImportTemplate>`
- Modèles : [BankImportTemplate](#m-BankImportTemplate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `templates`

<a id="r-post-sni-api-v1-accounting-bank-import-templates"></a>
### POST /sni/api/v1/accounting/bank-import-templates

- Accès : back-office : rôle d'administration et droit `BANK_ACCOUNT:MANAGE`
- Corps JSON (obligatoire) : `BankImportTemplate`
- Réponse : `BankImportTemplate`
- Modèles : [BankImportTemplate](#m-BankImportTemplate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `saveTemplate`

<a id="r-post-sni-api-v1-accounting-bank-import-templates-code-preview"></a>
### POST /sni/api/v1/accounting/bank-import-templates/{code}/preview

- Accès : back-office : rôle d'administration et droit `BANK_ACCOUNT:MANAGE`
- Envoi : `multipart/form-data`
- Réponse : `List<BankService.ManualLine>`
- Modèles : [BankService.ManualLine](#m-BankService-ManualLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-accounting-cash-registers-registercode-balances"></a>
### GET /sni/api/v1/accounting/cash/registers/{registerCode}/balances

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<ExchangeRateService.CurrencyBalance>`
- Modèles : [ExchangeRateService.CurrencyBalance](#m-ExchangeRateService-CurrencyBalance)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `balances`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-cheques"></a>
### GET /sni/api/v1/accounting/cheques

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankCheque>`
- Modèles : [BankCheque](#m-BankCheque)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `cheques`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-accounting-cheques-transactionnumber-reject"></a>
### PATCH /sni/api/v1/accounting/cheques/{transactionNumber}/reject

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Réponse : `BankCheque`
- Modèles : [BankCheque](#m-BankCheque)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` | oui |  |
| `fees` | requête | `BigDecimal` |  |  |
| `bankAccountCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-accounting-deposit-slips"></a>
### GET /sni/api/v1/accounting/deposit-slips

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<BankDepositSlip>`
- Modèles : [BankDepositSlip](#m-BankDepositSlip)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `slips`

<a id="r-post-sni-api-v1-accounting-deposit-slips"></a>
### POST /sni/api/v1/accounting/deposit-slips

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Corps JSON (obligatoire) : `ChequeService.SlipInput`
- Réponse : `ChequeService.Slip`
- Modèles : [ChequeService.Slip](#m-ChequeService-Slip), [ChequeService.SlipInput](#m-ChequeService-SlipInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `prepareSlip`

<a id="r-patch-sni-api-v1-accounting-deposit-slips-number-credit"></a>
### PATCH /sni/api/v1/accounting/deposit-slips/{number}/credit

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Réponse : `ChequeService.Slip`
- Modèles : [ChequeService.Slip](#m-ChequeService-Slip)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `credit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `statementLineId` | requête | `Long` |  |  |
| `date` | requête | `LocalDate` |  |  |

<a id="r-patch-sni-api-v1-accounting-deposit-slips-number-deposit"></a>
### PATCH /sni/api/v1/accounting/deposit-slips/{number}/deposit

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:MANAGE`
- Réponse : `ChequeService.Slip`
- Modèles : [ChequeService.Slip](#m-ChequeService-Slip)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `deposit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-exchange-rates"></a>
### GET /sni/api/v1/accounting/exchange-rates

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashExchangeRate>`
- Modèles : [CashExchangeRate](#m-CashExchangeRate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `rates`

<a id="r-post-sni-api-v1-accounting-exchange-rates"></a>
### POST /sni/api/v1/accounting/exchange-rates

- Accès : back-office : rôle d'administration et droit `CASH_EXCHANGE_RATE:MANAGE`
- Réponse : `CashExchangeRate`
- Modèles : [CashExchangeRate](#m-CashExchangeRate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `setRate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currency` | requête | `String` | oui |  |
| `rate` | requête | `BigDecimal` | oui |  |
| `validOn` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-exchange-rates-board"></a>
### GET /sni/api/v1/accounting/exchange-rates/board

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<ExchangeRateService.CurrencyRate>`
- Modèles : [ExchangeRateService.CurrencyRate](#m-ExchangeRateService-CurrencyRate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `board`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-exchange-rates-convert"></a>
### GET /sni/api/v1/accounting/exchange-rates/convert

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `BigDecimal`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `convert`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amount` | requête | `BigDecimal` | oui |  |
| `from` | requête | `String` | oui |  |
| `to` | requête | `String` |  | XAF |
| `date` | requête | `LocalDate` |  |  |

<a id="r-post-sni-api-v1-accounting-exchange-rates-fetch"></a>
### POST /sni/api/v1/accounting/exchange-rates/fetch

- Accès : back-office : rôle d'administration et droit `CASH_EXCHANGE_RATE:MANAGE`
- Réponse : `List<CashExchangeRate>`
- Modèles : [CashExchangeRate](#m-CashExchangeRate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `fetch`

<a id="r-get-sni-api-v1-accounting-reconciliations-id-pdf"></a>
### GET /sni/api/v1/accounting/reconciliations/{id}/pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `reconciliationPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-accounting-reconciliations-id-validate"></a>
### PATCH /sni/api/v1/accounting/reconciliations/{id}/validate

- Accès : back-office : rôle d'administration et droit `BANK_RECONCILIATION:VALIDATE`
- Réponse : `BankService.Reconciliation`
- Modèles : [BankService.Reconciliation](#m-BankService-Reconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/BankController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BankAccount"></a>
#### BankAccount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `kind` | texte |  |  |
| `bankName` | texte |  |  |
| `branch` | texte |  |  |
| `label` | texte |  |  |
| `rib` | texte |  |  |
| `currency` | texte |  |  |
| `accountNumber` | texte |  |  |
| `journalCode` | texte |  |  |
| `importTemplateCode` | texte |  |  |
| `openingBalance` | nombre décimal |  |  |
| `openingDate` | date AAAA-MM-JJ |  |  |
| `active` | booléen |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BankService-MatchingScreen"></a>
#### BankService.MatchingScreen

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bankAccountCode` | texte |  |  |
| `statementLines` | [BankService.Line](#m-BankService-Line)[] |  |  |
| `bookLines` | [BankService.BookLine](#m-BankService-BookLine)[] |  |  |
| `proposals` | [BankService.Proposal](#m-BankService-Proposal)[] |  |  |
| `statementOpen` | nombre décimal |  |  |
| `bookOpen` | nombre décimal |  |  |

<a id="m-BankMatching"></a>
#### BankMatching

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `kind` | texte |  |  |
| `statementLineIds` | texte |  |  |
| `entryLineIds` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `locked` | booléen |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `unmatchedBy` | texte |  |  |
| `unmatchedAt` | date-heure ISO 8601 UTC |  |  |
| `unmatchedReason` | texte |  |  |

<a id="m-BankController-MatchRequest"></a>
#### BankController.MatchRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `statementLineIds` | entier int64 ⚠[] |  |  |
| `entryLineIds` | entier int64 ⚠[] |  |  |

<a id="m-BankController-Reason"></a>
#### BankController.Reason

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-BankReconciliation"></a>
#### BankReconciliation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `reconciliationDate` | date AAAA-MM-JJ |  |  |
| `statementBalance` | nombre décimal |  |  |
| `bookBalance` | nombre décimal |  |  |
| `outstanding` | texte |  |  |
| `detailJson` | texte |  |  |
| `difference` | nombre décimal |  |  |
| `status` | texte |  |  |
| `preparedBy` | texte |  |  |
| `preparedAt` | date-heure ISO 8601 UTC |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BankService-Reconciliation"></a>
#### BankService.Reconciliation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `statementBalance` | nombre décimal |  |  |
| `bookBalance` | nombre décimal |  |  |
| `bookOutstanding` | [BankService.BookLine](#m-BankService-BookLine)[] |  |  |
| `statementOutstanding` | [BankService.Line](#m-BankService-Line)[] |  |  |
| `reconciledBalance` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `status` | texte |  |  |
| `preparedBy` | texte |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `id` | entier int64 ⚠ |  |  |

<a id="m-BankService-Line"></a>
#### BankService.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `statementNumber` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `valueDate` | date AAAA-MM-JJ |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `matchingCode` | texte |  |  |
| `awaitingPayment` | texte |  |  |
| `postedEntry` | texte |  |  |

<a id="m-BankService-PostLine"></a>
#### BankService.PostLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleId` | entier int64 ⚠ |  |  |
| `accountNumber` | texte |  |  |
| `thirdParty` | texte |  |  |
| `label` | texte |  |  |
| `invoiceNumber` | texte |  |  |

<a id="m-BankStatement"></a>
#### BankStatement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `periodFrom` | date AAAA-MM-JJ |  |  |
| `periodTo` | date AAAA-MM-JJ |  |  |
| `openingBalance` | nombre décimal |  |  |
| `closingBalance` | nombre décimal |  |  |
| `fileName` | texte |  |  |
| `source` | texte |  |  |
| `missingBefore` | booléen |  |  |
| `duplicates` | entier |  |  |
| `status` | texte |  |  |
| `replacedBy` | texte |  |  |
| `importedBy` | texte |  |  |
| `importedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BankService-ImportResult"></a>
#### BankService.ImportResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `statement` | [BankStatement](#m-BankStatement) |  |  |
| `lines` | entier |  |  |
| `duplicates` | entier |  |  |
| `autoMatched` | entier |  |  |
| `warnings` | texte[] |  |  |

<a id="m-BankService-ManualStatement"></a>
#### BankService.ManualStatement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `periodFrom` | date AAAA-MM-JJ |  |  |
| `periodTo` | date AAAA-MM-JJ |  |  |
| `openingBalance` | nombre décimal |  |  |
| `closingBalance` | nombre décimal |  |  |
| `lines` | [BankService.ManualLine](#m-BankService-ManualLine)[] |  |  |

<a id="m-BankAssignmentRule"></a>
#### BankAssignmentRule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `label` | texte |  |  |
| `labelPattern` | texte |  |  |
| `direction` | texte |  |  |
| `minAmount` | nombre décimal |  |  |
| `maxAmount` | nombre décimal |  |  |
| `accountNumber` | texte |  |  |
| `thirdParty` | texte |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BankService-Dashboard"></a>
#### BankService.Dashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `accountNumber` | texte |  |  |
| `statementBalance` | nombre décimal |  |  |
| `bookBalance` | nombre décimal |  |  |
| `bookOutstanding` | entier |  |  |
| `statementOutstanding` | entier |  |  |
| `oldestOutstandingDays` | entier int64 ⚠ |  |  |
| `lastStatement` | date AAAA-MM-JJ |  |  |
| `lastReconciliation` | date AAAA-MM-JJ |  |  |

<a id="m-BankImportTemplate"></a>
#### BankImportTemplate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `format` | texte |  |  |
| `delimiter` | texte |  |  |
| `headerRows` | entier |  |  |
| `dateColumn` | texte |  |  |
| `valueDateColumn` | texte |  |  |
| `labelColumn` | texte |  |  |
| `referenceColumn` | texte |  |  |
| `amountColumn` | texte |  |  |
| `debitColumn` | texte |  |  |
| `creditColumn` | texte |  |  |
| `dateFormat` | texte |  |  |
| `decimalSeparator` | texte |  |  |
| `encoding` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BankService-ManualLine"></a>
#### BankService.ManualLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `valueDate` | date AAAA-MM-JJ |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-ExchangeRateService-CurrencyBalance"></a>
#### ExchangeRateService.CurrencyBalance

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `balance` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `baseEquivalent` | nombre décimal |  |  |

<a id="m-BankCheque"></a>
#### BankCheque

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `transactionNumber` | texte |  |  |
| `chequeNumber` | texte |  |  |
| `bankName` | texte |  |  |
| `drawer` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `registerCode` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `status` | texte |  |  |
| `slipNumber` | texte |  |  |
| `depositedAt` | date-heure ISO 8601 UTC |  |  |
| `clearedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectedReason` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BankDepositSlip"></a>
#### BankDepositSlip

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `registerCode` | texte |  |  |
| `cashAmount` | nombre décimal |  |  |
| `chequeTotal` | nombre décimal |  |  |
| `status` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `depositedBy` | texte |  |  |
| `depositedAt` | date-heure ISO 8601 UTC |  |  |
| `creditedAt` | date-heure ISO 8601 UTC |  |  |
| `creditEntry` | texte |  |  |

<a id="m-ChequeService-Slip"></a>
#### ChequeService.Slip

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `slip` | [BankDepositSlip](#m-BankDepositSlip) |  |  |
| `cheques` | [BankCheque](#m-BankCheque)[] |  |  |

<a id="m-ChequeService-SlipInput"></a>
#### ChequeService.SlipInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bankAccountCode` | texte |  |  |
| `registerCode` | texte |  |  |
| `cashAmount` | nombre décimal |  |  |
| `chequeTransactions` | texte[] |  |  |

<a id="m-CashExchangeRate"></a>
#### CashExchangeRate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `currency` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `validOn` | date AAAA-MM-JJ |  |  |
| `setBy` | texte |  |  |
| `source` | texte |  |  |
| `provider` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ExchangeRateService-CurrencyRate"></a>
#### ExchangeRateService.CurrencyRate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `validOn` | date AAAA-MM-JJ |  |  |
| `source` | texte |  |  |
| `provider` | texte |  |  |

<a id="m-BankService-BookLine"></a>
#### BankService.BookLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `entryNumber` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |
| `sourceCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `matchingCode` | texte |  |  |

<a id="m-BankService-Proposal"></a>
#### BankService.Proposal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `statementLineIds` | entier int64 ⚠[] |  |  |
| `entryLineIds` | entier int64 ⚠[] |  |  |
| `amount` | nombre décimal |  |  |
| `kind` | texte |  |  |
| `confidence` | entier |  |  |
