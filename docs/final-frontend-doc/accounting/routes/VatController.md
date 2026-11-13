# VatController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-vat-returns"></a>
### GET /sni/api/v1/accounting/vat-returns

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:READ`
- Réponse : `List<AccVatReturn>`
- Modèles : [AccVatReturn](#m-AccVatReturn)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `list`

<a id="r-post-sni-api-v1-accounting-vat-returns"></a>
### POST /sni/api/v1/accounting/vat-returns

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:PREPARE`
- Réponse : `AccVatReturn`
- Modèles : [AccVatReturn](#m-AccVatReturn)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `prepare`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-accounting-vat-returns-id"></a>
### DELETE /sni/api/v1/accounting/vat-returns/{id}

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:PREPARE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-vat-returns-id"></a>
### GET /sni/api/v1/accounting/vat-returns/{id}

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:READ`
- Réponse : `AccVatReturn`
- Modèles : [AccVatReturn](#m-AccVatReturn)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-vat-returns-id-detail"></a>
### GET /sni/api/v1/accounting/vat-returns/{id}/detail

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:READ`
- Réponse : `VatService.Detail`
- Modèles : [VatService.Detail](#m-VatService-Detail)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `detail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-vat-returns-id-export"></a>
### GET /sni/api/v1/accounting/vat-returns/{id}/export

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `export`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-vat-returns-id-liquidate"></a>
### POST /sni/api/v1/accounting/vat-returns/{id}/liquidate

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:VALIDATE`
- Réponse : `AccVatReturn`
- Modèles : [AccVatReturn](#m-AccVatReturn)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `liquidate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-vat-returns-id-pdf"></a>
### GET /sni/api/v1/accounting/vat-returns/{id}/pdf

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `pdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-accounting-vat-returns-id-validate"></a>
### PATCH /sni/api/v1/accounting/vat-returns/{id}/validate

- Accès : back-office : rôle d'administration et droit `VAT_RETURN:VALIDATE`
- Réponse : `AccVatReturn`
- Modèles : [AccVatReturn](#m-AccVatReturn)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/VatController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AccVatReturn"></a>
#### AccVatReturn

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `periodCode` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `regime` | texte |  |  |
| `collected` | nombre décimal |  |  |
| `deductible` | nombre décimal |  |  |
| `deductionProrata` | nombre décimal |  |  |
| `deductibleGross` | nombre décimal |  |  |
| `nonDeductible` | nombre décimal |  |  |
| `creditBroughtForward` | nombre décimal |  |  |
| `dueAmount` | nombre décimal |  |  |
| `creditCarriedForward` | nombre décimal |  |  |
| `revenue` | nombre décimal |  |  |
| `invoiceTaxTotal` | nombre décimal |  |  |
| `controlDifference` | nombre décimal |  |  |
| `pendingBalance` | nombre décimal |  |  |
| `detailJson` | texte |  |  |
| `status` | texte |  |  |
| `voucherNumber` | texte |  |  |
| `liquidationEntryNumber` | texte |  |  |
| `preparedBy` | texte |  |  |
| `preparedAt` | date-heure ISO 8601 UTC |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `liquidatedBy` | texte |  |  |
| `liquidatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-VatService-Detail"></a>
#### VatService.Detail

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `collected` | [VatService.AccountAmount](#m-VatService-AccountAmount)[] |  |  |
| `deductible` | [VatService.AccountAmount](#m-VatService-AccountAmount)[] |  |  |
| `byCode` | [VatService.CodeAmount](#m-VatService-CodeAmount)[] |  |  |
| `documents` | [VatService.DocumentTax](#m-VatService-DocumentTax)[] |  |  |
| `sealedTaxTotal` | nombre décimal |  |  |

<a id="m-VatService-AccountAmount"></a>
#### VatService.AccountAmount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `account` | texte |  |  |
| `label` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-VatService-CodeAmount"></a>
#### VatService.CodeAmount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `taxCode` | texte |  |  |
| `base` | nombre décimal |  |  |
| `tax` | nombre décimal |  |  |

<a id="m-VatService-DocumentTax"></a>
#### VatService.DocumentTax

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `customerName` | texte |  |  |
| `currency` | texte |  |  |
| `taxable` | nombre décimal |  |  |
| `vat` | nombre décimal |  |  |
| `extraTax` | nombre décimal |  |  |
| `taxXaf` | nombre décimal |  |  |
| `accountingTax` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `event` | texte |  |  |
| `sealed` | booléen |  |  |
| `fiscalNumber` | texte |  |  |
