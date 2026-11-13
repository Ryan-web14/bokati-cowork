# ClosingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 32 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-accounts-number-balance-justification"></a>
### GET /sni/api/v1/accounting/accounts/{number}/balance-justification

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `BalanceJustificationService.Justification`
- Modèles : [BalanceJustificationService.Justification](#m-BalanceJustificationService-Justification)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `justification`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `thirdParty` | requête | `String` |  |  |
| `date` | requête | `LocalDate` |  |  |

<a id="r-post-sni-api-v1-accounting-accounts-number-balance-justification"></a>
### POST /sni/api/v1/accounting/accounts/{number}/balance-justification

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ACCOUNT:CREATE`
- Corps JSON (facultatif) : `ClosingController.PrepareRequest`
- Réponse : `BalanceJustificationService.SavedJustification`
- Modèles : [BalanceJustificationService.SavedJustification](#m-BalanceJustificationService-SavedJustification), [ClosingController.PrepareRequest](#m-ClosingController-PrepareRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `prepareJustification`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-accounts-number-collective"></a>
### GET /sni/api/v1/accounting/accounts/{number}/collective

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `BalanceJustificationService.Collective`
- Modèles : [BalanceJustificationService.Collective](#m-BalanceJustificationService-Collective)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `collective`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `date` | requête | `LocalDate` |  |  |

<a id="r-patch-sni-api-v1-accounting-balance-justifications-id-review"></a>
### PATCH /sni/api/v1/accounting/balance-justifications/{id}/review

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `BalanceJustificationService.SavedJustification`
- Modèles : [BalanceJustificationService.SavedJustification](#m-BalanceJustificationService-SavedJustification)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `reviewJustification`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-closing-editions-number-pdf"></a>
### GET /sni/api/v1/accounting/closing/editions/{number}/pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `editionPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-closing-editions-number-verify"></a>
### GET /sni/api/v1/accounting/closing/editions/{number}/verify

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `verify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-closing-year"></a>
### GET /sni/api/v1/accounting/closing/{year}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `ClosingService.Checklist`
- Modèles : [ClosingService.Checklist](#m-ClosingService-Checklist)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `checklist`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-closing-year-close"></a>
### POST /sni/api/v1/accounting/closing/{year}/close

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (facultatif) : `ClosingService.CloseRequest`
- Réponse : `ClosingService.CloseResult`
- Modèles : [ClosingService.CloseResult](#m-ClosingService-CloseResult), [ClosingService.CloseRequest](#m-ClosingService-CloseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `close`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-closing-year-editions"></a>
### GET /sni/api/v1/accounting/closing/{year}/editions

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<ClosingService.Edition>`
- Modèles : [ClosingService.Edition](#m-ClosingService-Edition)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `editions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-closing-year-editions"></a>
### POST /sni/api/v1/accounting/closing/{year}/editions

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ClosingService.EditionRequest`
- Réponse : `ClosingService.Edition`
- Modèles : [ClosingService.Edition](#m-ClosingService-Edition), [ClosingService.EditionRequest](#m-ClosingService-EditionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `finalizeEdition`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-closing-year-inventory-balance"></a>
### GET /sni/api/v1/accounting/closing/{year}/inventory-balance

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `inventoryBalance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-closing-year-justification-file"></a>
### GET /sni/api/v1/accounting/closing/{year}/justification-file

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `justificationFile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-closing-year-notes"></a>
### GET /sni/api/v1/accounting/closing/{year}/notes

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<ClosingService.NoteView>`
- Modèles : [ClosingService.NoteView](#m-ClosingService-NoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `notes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-accounting-closing-year-notes-note"></a>
### PUT /sni/api/v1/accounting/closing/{year}/notes/{note}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ClosingService.NoteRequest`
- Réponse : `ClosingService.NoteView`
- Modèles : [ClosingService.NoteView](#m-ClosingService-NoteView), [ClosingService.NoteRequest](#m-ClosingService-NoteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `saveNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `note` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-closing-year-notes-note-validate"></a>
### POST /sni/api/v1/accounting/closing/{year}/notes/{note}/validate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `ClosingService.NoteView`
- Modèles : [ClosingService.NoteView](#m-ClosingService-NoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `validateNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `note` | chemin | `String` | oui |  |
| `validate` | requête | `boolean` |  | true |

<a id="r-get-sni-api-v1-accounting-closing-year-opening-preview"></a>
### GET /sni/api/v1/accounting/closing/{year}/opening-preview

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `ClosingService.OpeningPreview`
- Modèles : [ClosingService.OpeningPreview](#m-ClosingService-OpeningPreview)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `openingPreview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-closing-year-steps-step"></a>
### POST /sni/api/v1/accounting/closing/{year}/steps/{step}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (facultatif) : `ClosingController.TickRequest`
- Réponse : `ClosingService.Checklist`
- Modèles : [ClosingService.Checklist](#m-ClosingService-Checklist), [ClosingController.TickRequest](#m-ClosingController-TickRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `tick`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `step` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-closing-year-steps-step-waive"></a>
### POST /sni/api/v1/accounting/closing/{year}/steps/{step}/waive

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ClosingController.WaiveRequest`
- Réponse : `ClosingService.Checklist`
- Modèles : [ClosingService.Checklist](#m-ClosingService-Checklist), [ClosingController.WaiveRequest](#m-ClosingController-WaiveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `waive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `step` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-confirmations"></a>
### GET /sni/api/v1/accounting/confirmations

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccConfirmationRequest>`
- Modèles : [AccConfirmationRequest](#m-AccConfirmationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `confirmations`

<a id="r-post-sni-api-v1-accounting-confirmations"></a>
### POST /sni/api/v1/accounting/confirmations

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `BalanceJustificationService.SelectionRequest`
- Réponse : `List<AccConfirmationRequest>`
- Modèles : [AccConfirmationRequest](#m-AccConfirmationRequest), [BalanceJustificationService.SelectionRequest](#m-BalanceJustificationService-SelectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `selectConfirmations`

<a id="r-get-sni-api-v1-accounting-confirmations-number-letter"></a>
### GET /sni/api/v1/accounting/confirmations/{number}/letter

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `confirmationLetter`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `format` | requête | `String` |  | pdf |

<a id="r-post-sni-api-v1-accounting-confirmations-number-reconcile"></a>
### POST /sni/api/v1/accounting/confirmations/{number}/reconcile

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ClosingController.ReconcileRequest`
- Réponse : `AccConfirmationRequest`
- Modèles : [AccConfirmationRequest](#m-AccConfirmationRequest), [ClosingController.ReconcileRequest](#m-ClosingController-ReconcileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `reconcileConfirmation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-confirmations-number-reply"></a>
### POST /sni/api/v1/accounting/confirmations/{number}/reply

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `BalanceJustificationService.ReplyRequest`
- Réponse : `AccConfirmationRequest`
- Modèles : [AccConfirmationRequest](#m-AccConfirmationRequest), [BalanceJustificationService.ReplyRequest](#m-BalanceJustificationService-ReplyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `replyConfirmation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-confirmations-number-send"></a>
### POST /sni/api/v1/accounting/confirmations/{number}/send

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `BalanceJustificationService.SendResult`
- Modèles : [BalanceJustificationService.SendResult](#m-BalanceJustificationService-SendResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `sendConfirmation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `reminder` | requête | `boolean` |  | false |
| `channel` | requête | `String` |  | EMAIL |
| `recipient` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-accounting-entries-reference-cash-flow-code"></a>
### PATCH /sni/api/v1/accounting/entries/{reference}/cash-flow-code

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_ENTRY:CREATE`
- Corps JSON (obligatoire) : `ClosingController.FlowCodeRequest`
- Réponse : `Map<String, Object>`
- Modèles : [ClosingController.FlowCodeRequest](#m-ClosingController-FlowCodeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `entryFlowCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-posting-rules-code-cash-flow-code"></a>
### PATCH /sni/api/v1/accounting/posting-rules/{code}/cash-flow-code

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Corps JSON (obligatoire) : `ClosingController.FlowCodeRequest`
- Réponse : `Map<String, Object>`
- Modèles : [ClosingController.FlowCodeRequest](#m-ClosingController-FlowCodeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `ruleFlowCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-statements-balance-sheet"></a>
### GET /sni/api/v1/accounting/statements/balance-sheet

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `balanceSheet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-statements-cash-flow"></a>
### GET /sni/api/v1/accounting/statements/cash-flow

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `cashFlow`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | requête | `String` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-statements-income-statement"></a>
### GET /sni/api/v1/accounting/statements/income-statement

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `incomeStatement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-delete-sni-api-v1-accounting-statements-mappings-id"></a>
### DELETE /sni/api/v1/accounting/statements/mappings/{id}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `deleteMapping`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-statements-template-mappings"></a>
### GET /sni/api/v1/accounting/statements/{template}/mappings

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FinancialStatementService.MappingView>`
- Modèles : [FinancialStatementService.MappingView](#m-FinancialStatementService-MappingView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `mappings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `template` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-statements-template-mappings"></a>
### POST /sni/api/v1/accounting/statements/{template}/mappings

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FinancialStatementService.MappingRequest`
- Réponse : `FinancialStatementService.MappingView`
- Modèles : [FinancialStatementService.MappingView](#m-FinancialStatementService-MappingView), [FinancialStatementService.MappingRequest](#m-FinancialStatementService-MappingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ClosingController.java`, méthode `addMapping`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `template` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BalanceJustificationService-Justification"></a>
#### BalanceJustificationService.Justification

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `items` | [BalanceJustificationService.OpenItem](#m-BalanceJustificationService-OpenItem)[] |  |  |
| `totalOpen` | nombre décimal |  |  |
| `bookBalance` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |

<a id="m-BalanceJustificationService-SavedJustification"></a>
#### BalanceJustificationService.SavedJustification

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `accountNumber` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `totalOpen` | nombre décimal |  |  |
| `bookBalance` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `comment` | texte |  |  |
| `preparedBy` | texte |  |  |
| `preparedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewedBy` | texte |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ClosingController-PrepareRequest"></a>
#### ClosingController.PrepareRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `thirdPartyCode` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `comment` | texte |  |  |

<a id="m-BalanceJustificationService-Collective"></a>
#### BalanceJustificationService.Collective

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `prefix` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `balance` | nombre décimal |  |  |
| `partiesTotal` | nombre décimal |  |  |
| `withoutThirdParty` | nombre décimal |  |  |
| `parties` | [BalanceJustificationService.PartyBalance](#m-BalanceJustificationService-PartyBalance)[] |  |  |
| `abnormal` | [BalanceJustificationService.PartyBalance](#m-BalanceJustificationService-PartyBalance)[] |  |  |
| `ok` | booléen |  |  |

<a id="m-ClosingService-Checklist"></a>
#### ClosingService.Checklist

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYear` | texte |  |  |
| `status` | texte |  |  |
| `start` | date AAAA-MM-JJ |  |  |
| `end` | date AAAA-MM-JJ |  |  |
| `steps` | [ClosingService.StepView](#m-ClosingService-StepView)[] |  |  |
| `ready` | booléen |  |  |
| `blocking` | texte[] |  |  |
| `netResult` | nombre décimal |  |  |
| `openingEntryNumber` | texte |  |  |

<a id="m-ClosingService-CloseResult"></a>
#### ClosingService.CloseResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYear` | texte |  |  |
| `netResult` | nombre décimal |  |  |
| `openingEntryNumber` | texte |  |  |
| `offBalanceEntryNumber` | texte |  |  |
| `lines` | entier |  |  |
| `carriedLines` | entier |  |  |
| `nextFiscalYear` | texte |  |  |

<a id="m-ClosingService-CloseRequest"></a>
#### ClosingService.CloseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `derogation` | texte |  |  |

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

<a id="m-ClosingService-EditionRequest"></a>
#### ClosingService.EditionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `report` | texte |  |  |
| `derogation` | texte |  |  |

<a id="m-ClosingService-NoteView"></a>
#### ClosingService.NoteView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `title` | texte |  |  |
| `text` | booléen |  |  |
| `source` | texte |  |  |
| `notApplicable` | booléen |  |  |
| `body` | texte |  |  |
| `status` | texte |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `reusedFromPreviousYear` | booléen |  |  |
| `accounts` | [ClosingService.NoteAccount](#m-ClosingService-NoteAccount)[] |  |  |
| `total` | nombre décimal |  |  |
| `previousTotal` | nombre décimal |  |  |

<a id="m-ClosingService-NoteRequest"></a>
#### ClosingService.NoteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `body` | texte |  |  |
| `notApplicable` | booléen |  |  |

<a id="m-ClosingService-OpeningPreview"></a>
#### ClosingService.OpeningPreview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYear` | texte |  |  |
| `nextFiscalYear` | texte |  |  |
| `entryDate` | date AAAA-MM-JJ |  |  |
| `journal` | texte |  |  |
| `netResult` | nombre décimal |  |  |
| `lines` | [ClosingService.OpeningLine](#m-ClosingService-OpeningLine)[] |  |  |
| `offBalanceLines` | [ClosingService.OpeningLine](#m-ClosingService-OpeningLine)[] |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |

<a id="m-ClosingController-TickRequest"></a>
#### ClosingController.TickRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `done` | booléen |  |  |
| `comment` | texte |  |  |

<a id="m-ClosingController-WaiveRequest"></a>
#### ClosingController.WaiveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-AccConfirmationRequest"></a>
#### AccConfirmationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `accountNumber` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `balanceDate` | date AAAA-MM-JJ |  |  |
| `balance` | nombre décimal |  |  |
| `openItems` | texte |  |  |
| `status` | texte |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `remindedAt` | date-heure ISO 8601 UTC |  |  |
| `replyChannel` | texte |  |  |
| `repliedBalance` | nombre décimal |  |  |
| `replyDetail` | texte |  |  |
| `repliedAt` | date-heure ISO 8601 UTC |  |  |
| `difference` | nombre décimal |  |  |
| `reconciledBy` | texte |  |  |
| `reconciledAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `sentChannel` | texte |  |  |
| `recipient` | texte |  |  |

<a id="m-BalanceJustificationService-SelectionRequest"></a>
#### BalanceJustificationService.SelectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `top` | entier |  |  |
| `threshold` | nombre décimal |  |  |
| `thirdParties` | texte[] |  |  |

<a id="m-ClosingController-ReconcileRequest"></a>
#### ClosingController.ReconcileRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `comment` | texte |  |  |

<a id="m-BalanceJustificationService-ReplyRequest"></a>
#### BalanceJustificationService.ReplyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `channel` | texte |  |  |
| `confirmedBalance` | nombre décimal |  |  |
| `detail` | texte |  |  |

<a id="m-BalanceJustificationService-SendResult"></a>
#### BalanceJustificationService.SendResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `confirmation` | [AccConfirmationRequest](#m-AccConfirmationRequest) |  |  |
| `channel` | texte |  |  |
| `recipient` | texte |  |  |

<a id="m-ClosingController-FlowCodeRequest"></a>
#### ClosingController.FlowCodeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |

<a id="m-FinancialStatementService-MappingView"></a>
#### FinancialStatementService.MappingView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `lineCode` | texte |  |  |
| `accountPrefix` | texte |  |  |
| `balanceSide` | texte |  |  |
| `valueColumn` | texte |  |  |

<a id="m-FinancialStatementService-MappingRequest"></a>
#### FinancialStatementService.MappingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineCode` | texte |  |  |
| `accountPrefix` | texte |  |  |
| `balanceSide` | texte |  |  |
| `valueColumn` | texte |  |  |

<a id="m-BalanceJustificationService-OpenItem"></a>
#### BalanceJustificationService.OpenItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineId` | entier int64 ⚠ |  |  |
| `entryNumber` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `reference` | texte |  |  |
| `label` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |

<a id="m-BalanceJustificationService-PartyBalance"></a>
#### BalanceJustificationService.PartyBalance

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `thirdPartyCode` | texte |  |  |
| `balance` | nombre décimal |  |  |
| `abnormal` | booléen |  |  |
| `justificationGap` | nombre décimal |  |  |

<a id="m-ClosingService-StepView"></a>
#### ClosingService.StepView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `automatic` | booléen |  |  |
| `mandatory` | booléen |  |  |
| `status` | texte |  |  |
| `detail` | texte |  |  |
| `checkedBy` | texte |  |  |
| `checkedAt` | date-heure ISO 8601 UTC |  |  |
| `comment` | texte |  |  |

<a id="m-ClosingService-NoteAccount"></a>
#### ClosingService.NoteAccount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `label` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `previous` | nombre décimal |  |  |

<a id="m-ClosingService-OpeningLine"></a>
#### ClosingService.OpeningLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `label` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `reference` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `currency` | texte |  |  |
| `foreignAmount` | nombre décimal |  |  |
| `sourceLineId` | entier int64 ⚠ |  |  |
