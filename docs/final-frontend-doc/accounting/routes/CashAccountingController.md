# CashAccountingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 6 route(s).

## Routes

<a id="r-post-sni-api-v1-accounting-cash-feed"></a>
### POST /sni/api/v1/accounting/cash/feed

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Réponse : `PostingDtos.BatchResult`
- Modèles : [PostingDtos.BatchResult](#m-PostingDtos-BatchResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/CashAccountingController.java`, méthode `feed`

<a id="r-get-sni-api-v1-accounting-cash-reconciliation"></a>
### GET /sni/api/v1/accounting/cash/reconciliation

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<CashAccountingService.RegisterReconciliation>`
- Modèles : [CashAccountingService.RegisterReconciliation](#m-CashAccountingService-RegisterReconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/CashAccountingController.java`, méthode `reconciliation`

<a id="r-get-sni-api-v1-accounting-cash-registers-registercode-journals"></a>
### GET /sni/api/v1/accounting/cash/registers/{registerCode}/journals

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Réponse : `List<AccountingResponses.Journal>`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/CashAccountingController.java`, méthode `journals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-cash-registers-registercode-journals"></a>
### POST /sni/api/v1/accounting/cash/registers/{registerCode}/journals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_JOURNAL:CREATE`
- Corps JSON (obligatoire) : `CashAccountingController.NewJournal`
- Réponse : `AccountingResponses.Journal`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal), [CashAccountingController.NewJournal](#m-CashAccountingController-NewJournal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/CashAccountingController.java`, méthode `createJournal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-accounting-cash-registers-registercode-journals-journalcode"></a>
### DELETE /sni/api/v1/accounting/cash/registers/{registerCode}/journals/{journalCode}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_JOURNAL:MANAGE`
- Réponse : `List<AccountingResponses.Journal>`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/CashAccountingController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |
| `journalCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-accounting-cash-registers-registercode-journals-journalcode"></a>
### PUT /sni/api/v1/accounting/cash/registers/{registerCode}/journals/{journalCode}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_JOURNAL:MANAGE`
- Réponse : `List<AccountingResponses.Journal>`
- Modèles : [AccountingResponses.Journal](#m-AccountingResponses-Journal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/CashAccountingController.java`, méthode `authorize`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |
| `journalCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PostingDtos-BatchResult"></a>
#### PostingDtos.BatchResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `errors` | texte[] |  |  |

<a id="m-CashAccountingService-RegisterReconciliation"></a>
#### CashAccountingService.RegisterReconciliation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `registerName` | texte |  |  |
| `treasuryAccount` | texte |  |  |
| `journalCode` | texte |  |  |
| `accountingBalance` | nombre décimal |  |  |
| `operationalBalance` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `operationalSource` | texte |  |  |
| `movementsNotPosted` | entier int64 ⚠ |  |  |

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

<a id="m-CashAccountingController-NewJournal"></a>
#### CashAccountingController.NewJournal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `type` | enum : `SALES`, `PURCHASES`, `CASH`, `BANK`, `MOBILE_MONEY`, `WALLET`, `MISC`, `STOCK`, `OPENING`, `REVERSAL`, `OFF_BALANCE`, `ANALYTIC`, `PAYROLL` |  |  |
