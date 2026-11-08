# FinancialReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 18 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-aging"></a>
### GET /sni/api/v1/reporting/aging

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `reporting.dto.response.BillingAgingReportResponse`
- Modèles : [reporting.dto.response.BillingAgingReportResponse](#m-reporting-dto-response-BillingAgingReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `aging`

<a id="r-get-sni-api-v1-reporting-cash-flow"></a>
### GET /sni/api/v1/reporting/cash-flow

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `CashFlowReportResponse`
- Modèles : [CashFlowReportResponse](#m-CashFlowReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `cashFlow`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-cash-registers"></a>
### GET /sni/api/v1/reporting/cash-registers

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `CashRegisterReportResponse`
- Modèles : [CashRegisterReportResponse](#m-CashRegisterReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `cashRegisters`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-dashboard"></a>
### GET /sni/api/v1/reporting/dashboard

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `FinancialDashboardResponse`
- Modèles : [FinancialDashboardResponse](#m-FinancialDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-dashboard-pdf"></a>
### GET /sni/api/v1/reporting/dashboard/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `dashboardPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-finance-aging"></a>
### GET /sni/api/v1/reporting/finance/aging

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `reporting.dto.response.BillingAgingReportResponse`
- Modèles : [reporting.dto.response.BillingAgingReportResponse](#m-reporting-dto-response-BillingAgingReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `aging`

<a id="r-get-sni-api-v1-reporting-finance-cash-flow"></a>
### GET /sni/api/v1/reporting/finance/cash-flow

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `CashFlowReportResponse`
- Modèles : [CashFlowReportResponse](#m-CashFlowReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `cashFlow`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-finance-cash-registers"></a>
### GET /sni/api/v1/reporting/finance/cash-registers

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `CashRegisterReportResponse`
- Modèles : [CashRegisterReportResponse](#m-CashRegisterReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `cashRegisters`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-finance-dashboard"></a>
### GET /sni/api/v1/reporting/finance/dashboard

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `FinancialDashboardResponse`
- Modèles : [FinancialDashboardResponse](#m-FinancialDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-finance-dashboard-pdf"></a>
### GET /sni/api/v1/reporting/finance/dashboard/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `dashboardPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-finance-payments"></a>
### GET /sni/api/v1/reporting/finance/payments

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `PaymentSourceReportResponse`
- Modèles : [PaymentSourceReportResponse](#m-PaymentSourceReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `payments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-payments"></a>
### GET /sni/api/v1/reporting/payments

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `PaymentSourceReportResponse`
- Modèles : [PaymentSourceReportResponse](#m-PaymentSourceReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `payments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-finance-aging"></a>
### GET /sni/api/v1/reports/finance/aging

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `reporting.dto.response.BillingAgingReportResponse`
- Modèles : [reporting.dto.response.BillingAgingReportResponse](#m-reporting-dto-response-BillingAgingReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `aging`

<a id="r-get-sni-api-v1-reports-finance-cash-flow"></a>
### GET /sni/api/v1/reports/finance/cash-flow

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `CashFlowReportResponse`
- Modèles : [CashFlowReportResponse](#m-CashFlowReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `cashFlow`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-finance-cash-registers"></a>
### GET /sni/api/v1/reports/finance/cash-registers

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `CashRegisterReportResponse`
- Modèles : [CashRegisterReportResponse](#m-CashRegisterReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `cashRegisters`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-finance-dashboard"></a>
### GET /sni/api/v1/reports/finance/dashboard

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `FinancialDashboardResponse`
- Modèles : [FinancialDashboardResponse](#m-FinancialDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-finance-dashboard-pdf"></a>
### GET /sni/api/v1/reports/finance/dashboard/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `dashboardPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-finance-payments"></a>
### GET /sni/api/v1/reports/finance/payments

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `PaymentSourceReportResponse`
- Modèles : [PaymentSourceReportResponse](#m-PaymentSourceReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/FinancialReportController.java`, méthode `payments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-reporting-dto-response-BillingAgingReportResponse"></a>
#### reporting.dto.response.BillingAgingReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `totalOutstanding` | nombre décimal |  |  |
| `totalInvoices` | entier int64 ⚠ |  |  |
| `current` | [reporting.dto.response.BillingAgingReportResponse.AgingBucket](#m-reporting-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `days1to30` | [reporting.dto.response.BillingAgingReportResponse.AgingBucket](#m-reporting-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `days31to60` | [reporting.dto.response.BillingAgingReportResponse.AgingBucket](#m-reporting-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `days61to90` | [reporting.dto.response.BillingAgingReportResponse.AgingBucket](#m-reporting-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `over90` | [reporting.dto.response.BillingAgingReportResponse.AgingBucket](#m-reporting-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |

<a id="m-CashFlowReportResponse"></a>
#### CashFlowReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `totalInflows` | nombre décimal |  |  |
| `totalOutflows` | nombre décimal |  |  |
| `netCashFlow` | nombre décimal |  |  |
| `byMovementType` | [CashFlowReportResponse.CashFlowByType](#m-CashFlowReportResponse-CashFlowByType)[] |  |  |
| `dailySeries` | [CashFlowReportResponse.DailyCashFlowEntry](#m-CashFlowReportResponse-DailyCashFlowEntry)[] |  |  |

<a id="m-CashRegisterReportResponse"></a>
#### CashRegisterReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `totalCashIn` | nombre décimal |  |  |
| `totalCashOut` | nombre décimal |  |  |
| `netFlow` | nombre décimal |  |  |
| `registers` | [CashRegisterReportResponse.RegisterSummary](#m-CashRegisterReportResponse-RegisterSummary)[] |  |  |

<a id="m-FinancialDashboardResponse"></a>
#### FinancialDashboardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `invoiceCount` | entier int64 ⚠ |  |  |
| `totalInvoiced` | nombre décimal |  |  |
| `totalPaid` | nombre décimal |  |  |
| `totalOutstanding` | nombre décimal |  |  |
| `overdueCount` | entier int64 ⚠ |  |  |
| `overdueAmount` | nombre décimal |  |  |
| `collectionRate` | nombre décimal |  |  |
| `totalVat` | nombre décimal |  |  |
| `totalAdditionalCent` | nombre décimal |  |  |
| `totalDiscounts` | nombre décimal |  |  |
| `cancelledInvoiceCount` | entier int64 ⚠ |  |  |
| `revenueBySource` | [FinancialDashboardResponse.RevenueBySource](#m-FinancialDashboardResponse-RevenueBySource)[] |  |  |
| `payments` | [FinancialDashboardResponse.PaymentSummary](#m-FinancialDashboardResponse-PaymentSummary) |  |  |

<a id="m-PaymentSourceReportResponse"></a>
#### PaymentSourceReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `totalReceived` | nombre décimal |  |  |
| `totalTransactions` | entier int64 ⚠ |  |  |
| `byMethod` | [PaymentSourceReportResponse.MethodBreakdown](#m-PaymentSourceReportResponse-MethodBreakdown)[] |  |  |
| `dailySeries` | [PaymentSourceReportResponse.DailyPaymentEntry](#m-PaymentSourceReportResponse-DailyPaymentEntry)[] |  |  |

<a id="m-reporting-dto-response-BillingAgingReportResponse-AgingBucket"></a>
#### reporting.dto.response.BillingAgingReportResponse.AgingBucket

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-CashFlowReportResponse-CashFlowByType"></a>
#### CashFlowReportResponse.CashFlowByType

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementType` | texte |  |  |
| `direction` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `totalAmount` | nombre décimal |  |  |

<a id="m-CashFlowReportResponse-DailyCashFlowEntry"></a>
#### CashFlowReportResponse.DailyCashFlowEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `inflows` | nombre décimal |  |  |
| `outflows` | nombre décimal |  |  |
| `net` | nombre décimal |  |  |

<a id="m-CashRegisterReportResponse-RegisterSummary"></a>
#### CashRegisterReportResponse.RegisterSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `registerName` | texte |  |  |
| `sessionsCount` | entier int64 ⚠ |  |  |
| `openingAmount` | nombre décimal |  |  |
| `cashIn` | nombre décimal |  |  |
| `cashOut` | nombre décimal |  |  |
| `netFlow` | nombre décimal |  |  |
| `totalVariance` | nombre décimal |  |  |
| `movementsCount` | entier int64 ⚠ |  |  |

<a id="m-FinancialDashboardResponse-RevenueBySource"></a>
#### FinancialDashboardResponse.RevenueBySource

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sourceType` | texte |  |  |
| `invoiceCount` | entier int64 ⚠ |  |  |
| `totalInvoiced` | nombre décimal |  |  |
| `totalPaid` | nombre décimal |  |  |
| `outstandingAmount` | nombre décimal |  |  |

<a id="m-FinancialDashboardResponse-PaymentSummary"></a>
#### FinancialDashboardResponse.PaymentSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionCount` | entier int64 ⚠ |  |  |
| `totalReceived` | nombre décimal |  |  |
| `cashAmount` | nombre décimal |  |  |
| `walletAmount` | nombre décimal |  |  |
| `mobileMoneyAmount` | nombre décimal |  |  |
| `bankTransferAmount` | nombre décimal |  |  |
| `cardAmount` | nombre décimal |  |  |
| `chequeAmount` | nombre décimal |  |  |
| `failedCount` | entier int64 ⚠ |  |  |
| `pendingCount` | entier int64 ⚠ |  |  |

<a id="m-PaymentSourceReportResponse-MethodBreakdown"></a>
#### PaymentSourceReportResponse.MethodBreakdown

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `method` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-PaymentSourceReportResponse-DailyPaymentEntry"></a>
#### PaymentSourceReportResponse.DailyPaymentEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `cash` | nombre décimal |  |  |
| `wallet` | nombre décimal |  |  |
| `mobileMoney` | nombre décimal |  |  |
| `bankTransfer` | nombre décimal |  |  |
| `card` | nombre décimal |  |  |
| `cheque` | nombre décimal |  |  |
| `total` | nombre décimal |  |  |
