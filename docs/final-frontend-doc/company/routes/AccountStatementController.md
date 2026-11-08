# AccountStatementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-companies-customerid-statement"></a>
### GET /sni/api/v1/companies/{customerId}/statement

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `StatementDtos.Statement`
- Modèles : [StatementDtos.Statement](#m-StatementDtos-Statement)
- Code : `src/main/java/com/sni/bokaticowork/features/company/statement/controller/AccountStatementController.java`, méthode `company`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-companies-customerid-statement-csv"></a>
### GET /sni/api/v1/companies/{customerId}/statement.csv

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/company/statement/controller/AccountStatementController.java`, méthode `companyCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-companies-customerid-statement-pdf"></a>
### GET /sni/api/v1/companies/{customerId}/statement.pdf

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/company/statement/controller/AccountStatementController.java`, méthode `companyPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-members-memberid-statement"></a>
### GET /sni/api/v1/members/{memberId}/statement

- Accès : back-office : rôle d'administration et droit `CLIENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `StatementDtos.Statement`
- Modèles : [StatementDtos.Statement](#m-StatementDtos-Statement)
- Code : `src/main/java/com/sni/bokaticowork/features/company/statement/controller/AccountStatementController.java`, méthode `member`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-members-memberid-statement-csv"></a>
### GET /sni/api/v1/members/{memberId}/statement.csv

- Accès : back-office : rôle d'administration et droit `CLIENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/company/statement/controller/AccountStatementController.java`, méthode `memberCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-members-memberid-statement-pdf"></a>
### GET /sni/api/v1/members/{memberId}/statement.pdf

- Accès : back-office : rôle d'administration et droit `CLIENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/company/statement/controller/AccountStatementController.java`, méthode `memberPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StatementDtos-Statement"></a>
#### StatementDtos.Statement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subject` | enum : `COMPANY`, `MEMBER` |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `billingEmail` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `asOf` | date AAAA-MM-JJ |  |  |
| `creditLimit` | nombre décimal |  |  |
| `paymentTermsDays` | entier |  |  |
| `blocked` | booléen |  |  |
| `blockedReason` | texte |  |  |
| `totals` | [StatementDtos.Totals](#m-StatementDtos-Totals)[] |  |  |
| `accounts` | [StatementDtos.AccountBreakdown](#m-StatementDtos-AccountBreakdown)[] |  |  |
| `lines` | [StatementDtos.Line](#m-StatementDtos-Line)[] |  |  |

<a id="m-StatementDtos-Totals"></a>
#### StatementDtos.Totals

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `invoiced` | nombre décimal |  |  |
| `paid` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `overdue` | nombre décimal |  |  |
| `creditAvailable` | nombre décimal |  |  |
| `netBalanceDue` | nombre décimal |  |  |
| `openInvoices` | entier |  |  |
| `overdueInvoices` | entier |  |  |
| `aging` | [StatementDtos.Aging](#m-StatementDtos-Aging) |  |  |

<a id="m-StatementDtos-AccountBreakdown"></a>
#### StatementDtos.AccountBreakdown

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountType` | enum : `COMPANY`, `MEMBER`, `PERSONAL` |  |  |
| `accountCode` | texte |  |  |
| `accountName` | texte |  |  |
| `currency` | texte |  |  |
| `invoiced` | nombre décimal |  |  |
| `paid` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `overdue` | nombre décimal |  |  |
| `documentCount` | entier |  |  |

<a id="m-StatementDtos-Line"></a>
#### StatementDtos.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | texte |  |  |
| `status` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `currency` | texte |  |  |
| `accountType` | enum : `COMPANY`, `MEMBER`, `PERSONAL` |  |  |
| `accountCode` | texte |  |  |
| `accountName` | texte |  |  |
| `companyLinkSource` | texte |  |  |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `impact` | nombre décimal |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |
| `inPeriod` | booléen |  |  |

<a id="m-StatementDtos-Aging"></a>
#### StatementDtos.Aging

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notDue` | nombre décimal |  |  |
| `days1To30` | nombre décimal |  |  |
| `days31To60` | nombre décimal |  |  |
| `days61To90` | nombre décimal |  |  |
| `over90` | nombre décimal |  |  |
