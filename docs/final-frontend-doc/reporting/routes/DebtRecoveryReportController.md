# DebtRecoveryReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-debt-recovery"></a>
### GET /sni/api/v1/reporting/debt-recovery

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `DebtRecoveryReportResponse`
- Modèles : [DebtRecoveryReportResponse](#m-DebtRecoveryReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/DebtRecoveryReportController.java`, méthode `debtRecovery`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sortBy` | requête | `String` |  | age |

<a id="r-get-sni-api-v1-reporting-debt-recovery-csv"></a>
### GET /sni/api/v1/reporting/debt-recovery/csv

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/DebtRecoveryReportController.java`, méthode `debtRecoveryCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sortBy` | requête | `String` |  | age |

<a id="r-get-sni-api-v1-reporting-debt-recovery-pdf"></a>
### GET /sni/api/v1/reporting/debt-recovery/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/DebtRecoveryReportController.java`, méthode `debtRecoveryPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sortBy` | requête | `String` |  | age |

<a id="r-get-sni-api-v1-reports-debt-recovery"></a>
### GET /sni/api/v1/reports/debt-recovery

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `DebtRecoveryReportResponse`
- Modèles : [DebtRecoveryReportResponse](#m-DebtRecoveryReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/DebtRecoveryReportController.java`, méthode `debtRecovery`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sortBy` | requête | `String` |  | age |

<a id="r-get-sni-api-v1-reports-debt-recovery-csv"></a>
### GET /sni/api/v1/reports/debt-recovery/csv

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/DebtRecoveryReportController.java`, méthode `debtRecoveryCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sortBy` | requête | `String` |  | age |

<a id="r-get-sni-api-v1-reports-debt-recovery-pdf"></a>
### GET /sni/api/v1/reports/debt-recovery/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/DebtRecoveryReportController.java`, méthode `debtRecoveryPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sortBy` | requête | `String` |  | age |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DebtRecoveryReportResponse"></a>
#### DebtRecoveryReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `totalOutstanding` | nombre décimal |  |  |
| `totalInvoices` | entier int64 ⚠ |  |  |
| `aging` | [DebtRecoveryReportResponse.AgingSummary](#m-DebtRecoveryReportResponse-AgingSummary) |  |  |
| `unpaidInvoices` | [DebtRecoveryReportResponse.UnpaidInvoice](#m-DebtRecoveryReportResponse-UnpaidInvoice)[] |  |  |

<a id="m-DebtRecoveryReportResponse-AgingSummary"></a>
#### DebtRecoveryReportResponse.AgingSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `current` | [DebtRecoveryReportResponse.AgingBucket](#m-DebtRecoveryReportResponse-AgingBucket) |  |  |
| `days1to30` | [DebtRecoveryReportResponse.AgingBucket](#m-DebtRecoveryReportResponse-AgingBucket) |  |  |
| `days31to60` | [DebtRecoveryReportResponse.AgingBucket](#m-DebtRecoveryReportResponse-AgingBucket) |  |  |
| `days61to90` | [DebtRecoveryReportResponse.AgingBucket](#m-DebtRecoveryReportResponse-AgingBucket) |  |  |
| `days90plus` | [DebtRecoveryReportResponse.AgingBucket](#m-DebtRecoveryReportResponse-AgingBucket) |  |  |

<a id="m-DebtRecoveryReportResponse-UnpaidInvoice"></a>
#### DebtRecoveryReportResponse.UnpaidInvoice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `customerName` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerType` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `agingDays` | entier int64 ⚠ |  |  |
| `agingBucket` | texte |  |  |

<a id="m-DebtRecoveryReportResponse-AgingBucket"></a>
#### DebtRecoveryReportResponse.AgingBucket

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `invoiceCount` | entier int64 ⚠ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `averageAmount` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |
