# AccountingPrintController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-deposit-slips-number-pdf"></a>
### GET /sni/api/v1/accounting/deposit-slips/{number}/pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Renvoie : `application/pdf`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingPrintController.java`, méthode `depositSlip`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-partners-code-statement-pdf"></a>
### GET /sni/api/v1/accounting/partners/{code}/statement/pdf

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Renvoie : `application/pdf`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingPrintController.java`, méthode `partnerStatement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-purchases-suppliers-code-statement-pdf"></a>
### GET /sni/api/v1/accounting/purchases/suppliers/{code}/statement/pdf

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Renvoie : `application/pdf`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/AccountingPrintController.java`, méthode `supplierStatement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
