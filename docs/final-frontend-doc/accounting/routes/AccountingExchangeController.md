# AccountingExchangeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-archives-year"></a>
### GET /sni/api/v1/accounting/archives/{year}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccArchive>`
- Modèles : [AccArchive](#m-AccArchive)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `archives`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-archives-year"></a>
### POST /sni/api/v1/accounting/archives/{year}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `archive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-exports"></a>
### GET /sni/api/v1/accounting/exports

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccExportBatch>`
- Modèles : [AccExportBatch](#m-AccExportBatch)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `batches`

<a id="r-post-sni-api-v1-accounting-exports"></a>
### POST /sni/api/v1/accounting/exports

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AccountingExportService.ExportRequest`
- Réponse : `fichier (octets)`
- Modèles : [AccountingExportService.ExportRequest](#m-AccountingExportService-ExportRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `export`

<a id="r-get-sni-api-v1-accounting-exports-mappings"></a>
### GET /sni/api/v1/accounting/exports/mappings

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccExportMapping>`
- Modèles : [AccExportMapping](#m-AccExportMapping)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `mappings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `format` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-exports-mappings"></a>
### POST /sni/api/v1/accounting/exports/mappings

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AccountingExportService.MappingRequest`
- Réponse : `AccExportMapping`
- Modèles : [AccExportMapping](#m-AccExportMapping), [AccountingExportService.MappingRequest](#m-AccountingExportService-MappingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `saveMapping`

<a id="r-delete-sni-api-v1-accounting-exports-mappings-id"></a>
### DELETE /sni/api/v1/accounting/exports/mappings/{id}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `deleteMapping`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-imports-compare"></a>
### POST /sni/api/v1/accounting/imports/compare

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Envoi : `multipart/form-data`
- Réponse : `AccountingImportService.Comparison`
- Modèles : [AccountingImportService.Comparison](#m-AccountingImportService-Comparison)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `compare`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `date` | requête | `LocalDate` |  |  |
| `format` | requête | `String` |  | CSV |

<a id="r-post-sni-api-v1-accounting-imports-entries"></a>
### POST /sni/api/v1/accounting/imports/entries

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Envoi : `multipart/form-data`
- Réponse : `AccountingImportService.ImportResult`
- Modèles : [AccountingImportService.ImportResult](#m-AccountingImportService-ImportResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `importEntries`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `validate` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-accounting-imports-opening-year"></a>
### POST /sni/api/v1/accounting/imports/opening/{year}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Envoi : `multipart/form-data`
- Réponse : `AccountingImportService.ImportResult`
- Modèles : [AccountingImportService.ImportResult](#m-AccountingImportService-ImportResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingExchangeController.java`, méthode `importOpening`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AccArchive"></a>
#### AccArchive

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `fiscalYearCode` | texte |  |  |
| `fileName` | texte |  |  |
| `sizeBytes` | entier int64 ⚠ |  |  |
| `contentHash` | texte |  |  |
| `manifest` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccExportBatch"></a>
#### AccExportBatch

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `format` | texte |  |  |
| `mode` | texte |  |  |
| `periodFrom` | date AAAA-MM-JJ |  |  |
| `periodTo` | date AAAA-MM-JJ |  |  |
| `journalCode` | texte |  |  |
| `entryCount` | entier |  |  |
| `lineCount` | entier |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |
| `excluded` | entier |  |  |
| `fileName` | texte |  |  |
| `contentHash` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccountingExportService-ExportRequest"></a>
#### AccountingExportService.ExportRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `format` | texte |  |  |
| `mode` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `journal` | texte |  |  |

<a id="m-AccExportMapping"></a>
#### AccExportMapping

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `format` | texte |  |  |
| `kind` | texte |  |  |
| `source` | texte |  |  |
| `target` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccountingExportService-MappingRequest"></a>
#### AccountingExportService.MappingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `format` | texte |  |  |
| `kind` | texte |  |  |
| `source` | texte |  |  |
| `target` | texte |  |  |

<a id="m-AccountingImportService-Comparison"></a>
#### AccountingImportService.Comparison

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `format` | texte |  |  |
| `oursTotal` | nombre décimal |  |  |
| `theirsTotal` | nombre décimal |  |  |
| `gaps` | [AccountingImportService.Gap](#m-AccountingImportService-Gap)[] |  |  |
| `accounts` | entier |  |  |
| `identical` | booléen |  |  |

<a id="m-AccountingImportService-ImportResult"></a>
#### AccountingImportService.ImportResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entries` | entier |  |  |
| `lines` | entier |  |  |
| `validated` | entier |  |  |
| `created` | texte[] |  |  |
| `errors` | texte[] |  |  |

<a id="m-AccountingImportService-Gap"></a>
#### AccountingImportService.Gap

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `account` | texte |  |  |
| `ours` | nombre décimal |  |  |
| `theirs` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
