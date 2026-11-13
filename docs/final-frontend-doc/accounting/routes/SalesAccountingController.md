# SalesAccountingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 5 route(s).

## Routes

<a id="r-post-sni-api-v1-accounting-sales-auto-letter"></a>
### POST /sni/api/v1/accounting/sales/auto-letter

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_LETTERING:MANUAL`
- Réponse : `PostingDtos.BatchResult`
- Modèles : [PostingDtos.BatchResult](#m-PostingDtos-BatchResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/SalesAccountingController.java`, méthode `autoLetter`

<a id="r-get-sni-api-v1-accounting-sales-checks"></a>
### GET /sni/api/v1/accounting/sales/checks

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `SalesAccountingService.Checks`
- Modèles : [SalesAccountingService.Checks](#m-SalesAccountingService-Checks)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/SalesAccountingController.java`, méthode `checks`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-post-sni-api-v1-accounting-sales-feed"></a>
### POST /sni/api/v1/accounting/sales/feed

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Réponse : `PostingDtos.BatchResult`
- Modèles : [PostingDtos.BatchResult](#m-PostingDtos-BatchResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/SalesAccountingController.java`, méthode `feed`

<a id="r-get-sni-api-v1-accounting-sales-lettering-proposals"></a>
### GET /sni/api/v1/accounting/sales/lettering-proposals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccountingLetteringService.Proposal>`
- Modèles : [AccountingLetteringService.Proposal](#m-AccountingLetteringService-Proposal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/SalesAccountingController.java`, méthode `proposals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `accountNumber` | requête | `String` |  | 411 |
| `limit` | requête | `int` |  | 100 |

<a id="r-get-sni-api-v1-accounting-sales-status-reference"></a>
### GET /sni/api/v1/accounting/sales/status/{reference}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<PostingDtos.Event>`
- Modèles : [PostingDtos.Event](#m-PostingDtos-Event)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/SalesAccountingController.java`, méthode `status`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

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

<a id="m-SalesAccountingService-Checks"></a>
#### SalesAccountingService.Checks

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletTotal` | nombre décimal |  |  |
| `account4191Balance` | nombre décimal |  |  |
| `walletDifference` | nombre décimal |  |  |
| `invoicesNotPosted` | entier int64 ⚠ |  |  |
| `paymentsNotPosted` | entier int64 ⚠ |  |  |
| `walletEntriesNotPosted` | entier int64 ⚠ |  |  |
| `invoiceGaps` | [SalesAccountingService.InvoiceGap](#m-SalesAccountingService-InvoiceGap)[] |  |  |

<a id="m-AccountingLetteringService-Proposal"></a>
#### AccountingLetteringService.Proposal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `debit` | [AccountingResponses.OpenLine](#m-AccountingResponses-OpenLine) |  |  |
| `credit` | [AccountingResponses.OpenLine](#m-AccountingResponses-OpenLine) |  |  |

<a id="m-PostingDtos-Event"></a>
#### PostingDtos.Event

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `eventType` | texte |  |  |
| `reference` | texte |  |  |
| `family` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `payload` | texte |  |  |
| `status` | enum : `PENDING`, `POSTED`, `UNBALANCED`, `FAILED`, `IGNORED` |  |  |
| `replay` | booléen |  |  |
| `failureReason` | texte |  |  |
| `ruleCode` | texte |  |  |
| `ruleVersion` | entier |  |  |
| `entryNumber` | texte |  |  |
| `provisionalNumber` | texte |  |  |
| `shiftedNote` | texte |  |  |
| `attempts` | entier |  |  |
| `lastAttemptAt` | date-heure ISO 8601 UTC |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |
| `ignoredBy` | texte |  |  |
| `ignoredReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SalesAccountingService-InvoiceGap"></a>
#### SalesAccountingService.InvoiceGap

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `customerCode` | texte |  |  |
| `balanceDue` | nombre décimal |  |  |
| `accountingOpen` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |

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
