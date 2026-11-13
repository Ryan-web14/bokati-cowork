# AccountingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 37 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-accounts"></a>
### GET /sni/api/v1/accounting/accounts

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.Account>`
- Modèles : [AccountingResponses.Account](#m-AccountingResponses-Account)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `accounts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `search` | requête | `String` |  |  |
| `classCode` | requête | `String` |  |  |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-accounting-accounts"></a>
### POST /sni/api/v1/accounting/accounts

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ACCOUNT:CREATE`
- Corps JSON (obligatoire) : `AccountingRequests.CreateAccount`
- Réponse : `AccountingResponses.Account`
- Modèles : [AccountingResponses.Account](#m-AccountingResponses-Account), [AccountingRequests.CreateAccount](#m-AccountingRequests-CreateAccount)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `createAccount`

<a id="r-get-sni-api-v1-accounting-accounts-proposal"></a>
### GET /sni/api/v1/accounting/accounts/proposal

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.AccountProposal`
- Modèles : [AccountingResponses.AccountProposal](#m-AccountingResponses-AccountProposal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `proposal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `parentNumber` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-accounts-number"></a>
### GET /sni/api/v1/accounting/accounts/{number}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.Account`
- Modèles : [AccountingResponses.Account](#m-AccountingResponses-Account)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `account`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-accounts-number"></a>
### PATCH /sni/api/v1/accounting/accounts/{number}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ACCOUNT:CREATE`
- Corps JSON (obligatoire) : `AccountingRequests.UpdateAccount`
- Réponse : `AccountingResponses.Account`
- Modèles : [AccountingResponses.Account](#m-AccountingResponses-Account), [AccountingRequests.UpdateAccount](#m-AccountingRequests-UpdateAccount)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `updateAccount`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-accounts-number-activate"></a>
### PATCH /sni/api/v1/accounting/accounts/{number}/activate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ACCOUNT:MANAGE`
- Réponse : `AccountingResponses.Account`
- Modèles : [AccountingResponses.Account](#m-AccountingResponses-Account)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-accounts-number-deactivate"></a>
### PATCH /sni/api/v1/accounting/accounts/{number}/deactivate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ACCOUNT:MANAGE`
- Corps JSON (obligatoire) : `AccountingRequests.Reason`
- Réponse : `AccountingResponses.Account`
- Modèles : [AccountingResponses.Account](#m-AccountingResponses-Account), [AccountingRequests.Reason](#m-AccountingRequests-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-accounts-number-history"></a>
### GET /sni/api/v1/accounting/accounts/{number}/history

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.AccountHistory>`
- Modèles : [AccountingResponses.AccountHistory](#m-AccountingResponses-AccountHistory)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `accountHistory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-accounts-number-third-parties"></a>
### GET /sni/api/v1/accounting/accounts/{number}/third-parties

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.ThirdParty>`
- Modèles : [AccountingResponses.ThirdParty](#m-AccountingResponses-ThirdParty)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `thirdParties`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-entries"></a>
### GET /sni/api/v1/accounting/entries

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.Entry>`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry), [EntryStatus](#m-EntryStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `entries`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `journalCode` | requête | `String` |  |  |
| `status` | requête | `EntryStatus` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-post-sni-api-v1-accounting-entries"></a>
### POST /sni/api/v1/accounting/entries

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:CREATE`
- Corps JSON (obligatoire) : `AccountingRequests.Entry`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry), [AccountingRequests.Entry](#m-AccountingRequests-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `create`

<a id="r-get-sni-api-v1-accounting-entries-pending"></a>
### GET /sni/api/v1/accounting/entries/pending

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.Entry>`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `pending`

<a id="r-delete-sni-api-v1-accounting-entries-reference"></a>
### DELETE /sni/api/v1/accounting/entries/{reference}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:CREATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-entries-reference"></a>
### GET /sni/api/v1/accounting/entries/{reference}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `entry`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-accounting-entries-reference"></a>
### PUT /sni/api/v1/accounting/entries/{reference}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:CREATE`
- Corps JSON (obligatoire) : `AccountingRequests.Entry`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry), [AccountingRequests.Entry](#m-AccountingRequests-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-entries-reference-regularize"></a>
### POST /sni/api/v1/accounting/entries/{reference}/regularize

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:BALANCE`
- Corps JSON (obligatoire) : `AccountingRequests.Regularize`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry), [AccountingRequests.Regularize](#m-AccountingRequests-Regularize)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `regularize`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-entries-reference-reject"></a>
### PATCH /sni/api/v1/accounting/entries/{reference}/reject

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:BALANCE`
- Corps JSON (obligatoire) : `AccountingRequests.Reason`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry), [AccountingRequests.Reason](#m-AccountingRequests-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-entries-reference-reverse"></a>
### PATCH /sni/api/v1/accounting/entries/{reference}/reverse

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:REVERSE`
- Corps JSON (obligatoire) : `AccountingRequests.Reverse`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry), [AccountingRequests.Reverse](#m-AccountingRequests-Reverse)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `reverse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-entries-reference-validate"></a>
### PATCH /sni/api/v1/accounting/entries/{reference}/validate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:VALIDATE`
- Réponse : `AccountingResponses.Entry`
- Modèles : [AccountingResponses.Entry](#m-AccountingResponses-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fiscal-years"></a>
### GET /sni/api/v1/accounting/fiscal-years

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.FiscalYear>`
- Modèles : [AccountingResponses.FiscalYear](#m-AccountingResponses-FiscalYear)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `fiscalYears`

<a id="r-post-sni-api-v1-accounting-fiscal-years"></a>
### POST /sni/api/v1/accounting/fiscal-years

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_PERIOD:CLOSE`
- Corps JSON (obligatoire) : `AccountingRequests.CreateFiscalYear`
- Réponse : `AccountingResponses.FiscalYear`
- Modèles : [AccountingResponses.FiscalYear](#m-AccountingResponses-FiscalYear), [AccountingRequests.CreateFiscalYear](#m-AccountingRequests-CreateFiscalYear)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `createFiscalYear`

<a id="r-get-sni-api-v1-accounting-journals"></a>
### GET /sni/api/v1/accounting/journals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.Journal>`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `journals`

<a id="r-post-sni-api-v1-accounting-journals"></a>
### POST /sni/api/v1/accounting/journals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_JOURNAL:CREATE`
- Corps JSON (obligatoire) : `AccountingRequests.CreateJournal`
- Réponse : `AccountingResponses.Journal`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal), [AccountingRequests.CreateJournal](#m-AccountingRequests-CreateJournal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `createJournal`

<a id="r-patch-sni-api-v1-accounting-journals-code"></a>
### PATCH /sni/api/v1/accounting/journals/{code}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_JOURNAL:MANAGE`
- Corps JSON (obligatoire) : `AccountingRequests.UpdateJournal`
- Réponse : `AccountingResponses.Journal`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal), [AccountingRequests.UpdateJournal](#m-AccountingRequests-UpdateJournal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `updateJournal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-lettering"></a>
### GET /sni/api/v1/accounting/lettering

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.Lettering>`
- Modèles : [AccountingResponses.Lettering](#m-AccountingResponses-Lettering)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `letterings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `accountNumber` | requête | `String` | oui |  |
| `thirdPartyCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-lettering"></a>
### POST /sni/api/v1/accounting/lettering

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_LETTERING:MANUAL`
- Corps JSON (obligatoire) : `AccountingRequests.Letter`
- Réponse : `AccountingResponses.Lettering`
- Modèles : [AccountingResponses.Lettering](#m-AccountingResponses-Lettering), [AccountingRequests.Letter](#m-AccountingRequests-Letter)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `letter`

<a id="r-get-sni-api-v1-accounting-lettering-open-lines"></a>
### GET /sni/api/v1/accounting/lettering/open-lines

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.OpenLine>`
- Modèles : [AccountingResponses.OpenLine](#m-AccountingResponses-OpenLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `openLines`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `accountNumber` | requête | `String` | oui |  |
| `thirdPartyCode` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-accounting-lettering-id-unletter"></a>
### PATCH /sni/api/v1/accounting/lettering/{id}/unletter

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_LETTERING:UNLETTER`
- Corps JSON (obligatoire) : `AccountingRequests.Reason`
- Réponse : `AccountingResponses.Lettering`
- Modèles : [AccountingResponses.Lettering](#m-AccountingResponses-Lettering), [AccountingRequests.Reason](#m-AccountingRequests-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `unletter`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-numbering-checks"></a>
### POST /sni/api/v1/accounting/numbering-checks

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.NumberingCheck>`
- Modèles : [AccountingResponses.NumberingCheck](#m-AccountingResponses-NumberingCheck)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `checkAll`

<a id="r-get-sni-api-v1-accounting-periods"></a>
### GET /sni/api/v1/accounting/periods

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingResponses.Period>`
- Modèles : [AccountingResponses.Period](#m-AccountingResponses-Period)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `periods`

<a id="r-patch-sni-api-v1-accounting-periods-code-close"></a>
### PATCH /sni/api/v1/accounting/periods/{code}/close

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_PERIOD:CLOSE`
- Réponse : `AccountingResponses.Period`
- Modèles : [AccountingResponses.Period](#m-AccountingResponses-Period)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `close`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-periods-code-lock"></a>
### PATCH /sni/api/v1/accounting/periods/{code}/lock

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_PERIOD:LOCK`
- Réponse : `AccountingResponses.Period`
- Modèles : [AccountingResponses.Period](#m-AccountingResponses-Period)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `lock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-periods-code-unlock"></a>
### PATCH /sni/api/v1/accounting/periods/{code}/unlock

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_PERIOD:LOCK`
- Réponse : `AccountingResponses.Period`
- Modèles : [AccountingResponses.Period](#m-AccountingResponses-Period)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `unlock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-reports-general-ledger"></a>
### GET /sni/api/v1/accounting/reports/general-ledger

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.GeneralLedger`
- Modèles : [AccountingResponses.GeneralLedger](#m-AccountingResponses-GeneralLedger)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `generalLedger`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `account` | requête | `String` |  |  |
| `thirdPartyCode` | requête | `String` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-reports-journal"></a>
### GET /sni/api/v1/accounting/reports/journal

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.JournalReport`
- Modèles : [AccountingResponses.JournalReport](#m-AccountingResponses-JournalReport)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `journalReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `journalCode` | requête | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-reports-numbering-continuity"></a>
### GET /sni/api/v1/accounting/reports/numbering-continuity

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.NumberingCheck`
- Modèles : [AccountingResponses.NumberingCheck](#m-AccountingResponses-NumberingCheck)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `continuity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `journalCode` | requête | `String` | oui |  |
| `fiscalYearCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-reports-trial-balance"></a>
### GET /sni/api/v1/accounting/reports/trial-balance

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `AccountingResponses.TrialBalance`
- Modèles : [AccountingResponses.TrialBalance](#m-AccountingResponses-TrialBalance)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingController.java`, méthode `trialBalance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `scope` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AccountingResponses-Account"></a>
#### AccountingResponses.Account

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `label` | texte |  |  |
| `parentNumber` | texte |  |  |
| `classCode` | texte |  |  |
| `nature` | enum : `BALANCE`, `MANAGEMENT`, `HAO`, `ANALYTIC`, `OFF_BALANCE` |  |  |
| `normalSide` | enum : `DEBIT`, `CREDIT` |  |  |
| `reconcilable` | booléen |  |  |
| `thirdPartyType` | enum : `CUSTOMER`, `SUPPLIER`, `EMPLOYEE`, `PARTNER`, `OTHER` |  |  |
| `treasuryType` | enum : `CASH`, `BANK`, `MOBILE_MONEY` |  |  |
| `analyticRequired` | booléen |  |  |
| `taxCode` | texte |  |  |
| `statementLineCode` | texte |  |  |
| `status` | enum : `PENDING_APPROVAL`, `ACTIVE`, `INACTIVE`, `MERGED` |  |  |
| `mergedInto` | texte |  |  |
| `hasPostings` | booléen |  |  |
| `childCount` | entier |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccountingRequests-CreateAccount"></a>
#### AccountingRequests.CreateAccount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `parentNumber` | texte |  |  |
| `number` | texte |  |  |
| `label` | texte | oui | longueur min `1` |
| `reconcilable` | booléen |  |  |
| `thirdPartyType` | enum : `CUSTOMER`, `SUPPLIER`, `EMPLOYEE`, `PARTNER`, `OTHER` |  |  |
| `treasuryType` | enum : `CASH`, `BANK`, `MOBILE_MONEY` |  |  |
| `analyticRequired` | booléen |  |  |
| `taxCode` | texte |  |  |
| `statementLineCode` | texte |  |  |
| `reason` | texte |  |  |

<a id="m-AccountingResponses-AccountProposal"></a>
#### AccountingResponses.AccountProposal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `parentNumber` | texte |  |  |
| `proposedNumber` | texte |  |  |
| `classCode` | texte |  |  |
| `nature` | enum : `BALANCE`, `MANAGEMENT`, `HAO`, `ANALYTIC`, `OFF_BALANCE` |  |  |
| `normalSide` | enum : `DEBIT`, `CREDIT` |  |  |
| `reconcilable` | booléen |  |  |
| `thirdPartyType` | enum : `CUSTOMER`, `SUPPLIER`, `EMPLOYEE`, `PARTNER`, `OTHER` |  |  |
| `treasuryType` | enum : `CASH`, `BANK`, `MOBILE_MONEY` |  |  |
| `analyticRequired` | booléen |  |  |
| `taxCode` | texte |  |  |
| `statementLineCode` | texte |  |  |
| `needsApproval` | booléen |  |  |

<a id="m-AccountingRequests-UpdateAccount"></a>
#### AccountingRequests.UpdateAccount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `reconcilable` | booléen |  |  |
| `thirdPartyType` | enum : `CUSTOMER`, `SUPPLIER`, `EMPLOYEE`, `PARTNER`, `OTHER` |  |  |
| `treasuryType` | enum : `CASH`, `BANK`, `MOBILE_MONEY` |  |  |
| `analyticRequired` | booléen |  |  |
| `taxCode` | texte |  |  |
| `statementLineCode` | texte |  |  |
| `reason` | texte |  |  |

<a id="m-AccountingRequests-Reason"></a>
#### AccountingRequests.Reason

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-AccountingResponses-AccountHistory"></a>
#### AccountingResponses.AccountHistory

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `field` | texte |  |  |
| `oldValue` | texte |  |  |
| `newValue` | texte |  |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccountingResponses-ThirdParty"></a>
#### AccountingResponses.ThirdParty

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `partyType` | enum : `CUSTOMER`, `SUPPLIER`, `EMPLOYEE`, `PARTNER`, `OTHER` |  |  |
| `partyCode` | texte |  |  |
| `label` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccountingResponses-Entry"></a>
#### AccountingResponses.Entry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entryNumber` | texte |  |  |
| `provisionalNumber` | texte |  |  |
| `journalCode` | texte |  |  |
| `fiscalYearCode` | texte |  |  |
| `periodCode` | texte |  |  |
| `entryDate` | date AAAA-MM-JJ |  |  |
| `originalDate` | date AAAA-MM-JJ |  |  |
| `documentDate` | date AAAA-MM-JJ |  |  |
| `label` | texte |  |  |
| `origin` | enum : `MANUAL`, `CASH`, `PLATFORM`, `IMPORT`, `CLOSING`, `OPENING`, `REVERSAL`, `REGULARIZATION` |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_BALANCE`, `VALIDATED`, `REVERSED`, `REJECTED` |  |  |
| `acceptedUnbalanced` | booléen |  |  |
| `unbalancedAmount` | nombre décimal |  |  |
| `pendingReason` | enum : `UNBALANCED`, `COUNTERPART_NOT_ALLOWED`, `MISSING_ACCOUNT`, `ANALYTIC_INCOMPLETE`, `INACTIVE_ACCOUNT` |  |  |
| `pendingDetail` | texte |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `reversalOf` | texte |  |  |
| `reversedByEntry` | texte |  |  |
| `reversalReason` | texte |  |  |
| `regularizationOf` | texte |  |  |
| `regularizedBy` | texte |  |  |
| `rejectedBy` | texte |  |  |
| `rejectionReason` | texte |  |  |
| `contentHash` | texte |  |  |
| `lines` | [AccountingResponses.EntryLine](#m-AccountingResponses-EntryLine)[] |  |  |
| `messages` | texte[] |  |  |

<a id="m-EntryStatus"></a>
#### EntryStatus

Énumération : `DRAFT`, `PENDING_BALANCE`, `VALIDATED`, `REVERSED`, `REJECTED`

<a id="m-AccountingRequests-Entry"></a>
#### AccountingRequests.Entry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `journalCode` | texte | oui | longueur min `1` |
| `entryDate` | date AAAA-MM-JJ | oui |  |
| `documentDate` | date AAAA-MM-JJ |  |  |
| `label` | texte | oui | longueur min `1` |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `lines` | [AccountingRequests.EntryLine](#m-AccountingRequests-EntryLine)[] | oui | éléments min `1` |
| `validate` | booléen |  |  |

<a id="m-AccountingRequests-Regularize"></a>
#### AccountingRequests.Regularize

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte | oui | longueur min `1` |
| `lines` | [AccountingRequests.EntryLine](#m-AccountingRequests-EntryLine)[] | oui | éléments min `1` |

<a id="m-AccountingRequests-Reverse"></a>
#### AccountingRequests.Reverse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |
| `date` | date AAAA-MM-JJ |  |  |

<a id="m-AccountingResponses-FiscalYear"></a>
#### AccountingResponses.FiscalYear

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `OPEN`, `CLOSING`, `CLOSED` |  |  |
| `periods` | [AccountingResponses.Period](#m-AccountingResponses-Period)[] |  |  |

<a id="m-AccountingRequests-CreateFiscalYear"></a>
#### AccountingRequests.CreateFiscalYear

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `startDate` | date AAAA-MM-JJ | oui |  |
| `endDate` | date AAAA-MM-JJ | oui |  |

<a id="m-AccountingResponses-Journal"></a>
#### AccountingResponses.Journal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `type` | enum : `SALES`, `PURCHASES`, `CASH`, `BANK`, `MOBILE_MONEY`, `WALLET`, `MISC`, `STOCK`, `OPENING`, `REVERSAL`, `OFF_BALANCE`, `ANALYTIC`, `PAYROLL` |  |  |
| `treasuryAccountNumber` | texte |  |  |
| `numberingReset` | enum : `YEAR`, `PERIOD` |  |  |
| `autoValidate` | booléen |  |  |
| `allowedAccounts` | texte[] |  |  |
| `cashRegisterCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-AccountingRequests-CreateJournal"></a>
#### AccountingRequests.CreateJournal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `label` | texte | oui | longueur min `1` |
| `type` | enum : `SALES`, `PURCHASES`, `CASH`, `BANK`, `MOBILE_MONEY`, `WALLET`, `MISC`, `STOCK`, `OPENING`, `REVERSAL`, `OFF_BALANCE`, `ANALYTIC`, `PAYROLL` | oui |  |
| `treasuryAccountNumber` | texte |  |  |
| `numberingReset` | enum : `YEAR`, `PERIOD` |  |  |
| `autoValidate` | booléen |  |  |
| `allowedAccounts` | texte[] |  |  |
| `cashRegisterCode` | texte |  |  |

<a id="m-AccountingRequests-UpdateJournal"></a>
#### AccountingRequests.UpdateJournal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `autoValidate` | booléen |  |  |
| `allowedAccounts` | texte[] |  |  |
| `active` | booléen |  |  |

<a id="m-AccountingResponses-Lettering"></a>
#### AccountingResponses.Lettering

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `accountNumber` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `kind` | enum : `AUTO_REFERENCE`, `AUTO_MATCH`, `MANUAL`, `CARRY_FORWARD` |  |  |
| `status` | enum : `PARTIAL`, `COMPLETE`, `UNLETTERED` |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `differenceEntryNumber` | texte |  |  |
| `letteredBy` | texte |  |  |
| `letteredAt` | date-heure ISO 8601 UTC |  |  |
| `unletteredBy` | texte |  |  |
| `unletteredAt` | date-heure ISO 8601 UTC |  |  |
| `unletteringReason` | texte |  |  |
| `lineIds` | entier int64 ⚠[] |  |  |

<a id="m-AccountingRequests-Letter"></a>
#### AccountingRequests.Letter

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte | oui | longueur min `1` |
| `thirdPartyCode` | texte |  |  |
| `lines` | [AccountingRequests.LetterLine](#m-AccountingRequests-LetterLine)[] | oui | éléments min `1` |
| `partial` | booléen |  |  |
| `withDifference` | booléen |  |  |

<a id="m-AccountingResponses-OpenLine"></a>
#### AccountingResponses.OpenLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineId` | entier int64 ⚠ |  |  |
| `entryNumber` | texte |  |  |
| `entryDate` | date AAAA-MM-JJ |  |  |
| `journalCode` | texte |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |
| `side` | enum : `DEBIT`, `CREDIT` |  |  |

<a id="m-AccountingResponses-NumberingCheck"></a>
#### AccountingResponses.NumberingCheck

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `journalCode` | texte |  |  |
| `fiscalYearCode` | texte |  |  |
| `checkedAt` | date-heure ISO 8601 UTC |  |  |
| `checkedBy` | texte |  |  |
| `firstNumber` | entier |  |  |
| `lastNumber` | entier |  |  |
| `entryCount` | entier |  |  |
| `counterValue` | entier |  |  |
| `gaps` | entier[] |  |  |
| `duplicates` | entier[] |  |  |
| `chainOk` | booléen |  |  |
| `ok` | booléen |  |  |
| `brokenAt` | texte |  |  |

<a id="m-AccountingResponses-Period"></a>
#### AccountingResponses.Period

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `OPEN`, `LOCKED`, `CLOSED` |  |  |
| `lockedAt` | date-heure ISO 8601 UTC |  |  |
| `lockedBy` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `closedBy` | texte |  |  |
| `pendingEntries` | entier int64 ⚠ |  |  |
| `draftEntries` | entier int64 ⚠ |  |  |

<a id="m-AccountingResponses-GeneralLedger"></a>
#### AccountingResponses.GeneralLedger

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `accounts` | [AccountingResponses.LedgerAccount](#m-AccountingResponses-LedgerAccount)[] |  |  |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `generatedBy` | texte |  |  |

<a id="m-AccountingResponses-JournalReport"></a>
#### AccountingResponses.JournalReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `journalCode` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `entries` | [AccountingResponses.Entry](#m-AccountingResponses-Entry)[] |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `generatedBy` | texte |  |  |

<a id="m-AccountingResponses-TrialBalance"></a>
#### AccountingResponses.TrialBalance

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `scope` | texte |  |  |
| `lines` | [AccountingResponses.TrialBalanceLine](#m-AccountingResponses-TrialBalanceLine)[] |  |  |
| `totals` | [AccountingResponses.TrialBalanceLine](#m-AccountingResponses-TrialBalanceLine) |  |  |
| `balanced` | booléen |  |  |
| `imbalance` | nombre décimal |  |  |
| `imbalanceEntries` | texte[] |  |  |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `generatedBy` | texte |  |  |

<a id="m-AccountingResponses-EntryLine"></a>
#### AccountingResponses.EntryLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `lineNo` | entier |  |  |
| `accountNumber` | texte |  |  |
| `accountLabel` | texte |  |  |
| `thirdPartyType` | enum : `CUSTOMER`, `SUPPLIER`, `EMPLOYEE`, `PARTNER`, `OTHER` |  |  |
| `thirdPartyCode` | texte |  |  |
| `label` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `foreignAmount` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `taxCode` | texte |  |  |
| `taxBase` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `reference` | texte |  |  |
| `letteringCode` | texte |  |  |
| `letteredAmount` | nombre décimal |  |  |

<a id="m-AccountingRequests-EntryLine"></a>
#### AccountingRequests.EntryLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte | oui | longueur min `1` |
| `thirdPartyCode` | texte |  |  |
| `label` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `foreignAmount` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `taxCode` | texte |  |  |
| `taxBase` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `reference` | texte |  |  |
| `analytic` | objet { clé : texte } |  |  |

<a id="m-AccountingRequests-LetterLine"></a>
#### AccountingRequests.LetterLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineId` | entier int64 ⚠ | oui |  |
| `amount` | nombre décimal |  |  |

<a id="m-AccountingResponses-LedgerAccount"></a>
#### AccountingResponses.LedgerAccount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `accountLabel` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `openingBalance` | nombre décimal |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |
| `closingBalance` | nombre décimal |  |  |
| `lines` | [AccountingResponses.LedgerLine](#m-AccountingResponses-LedgerLine)[] |  |  |

<a id="m-AccountingResponses-TrialBalanceLine"></a>
#### AccountingResponses.TrialBalanceLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `accountLabel` | texte |  |  |
| `classCode` | texte |  |  |
| `openingDebit` | nombre décimal |  |  |
| `openingCredit` | nombre décimal |  |  |
| `movementDebit` | nombre décimal |  |  |
| `movementCredit` | nombre décimal |  |  |
| `closingDebit` | nombre décimal |  |  |
| `closingCredit` | nombre décimal |  |  |

<a id="m-AccountingResponses-LedgerLine"></a>
#### AccountingResponses.LedgerLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `entryNumber` | texte |  |  |
| `journalCode` | texte |  |  |
| `label` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `reference` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `balance` | nombre décimal |  |  |
| `letteringCode` | texte |  |  |
