# ActivityReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-activity"></a>
### GET /sni/api/v1/reporting/activity

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ActivityReportResponse`
- Modèles : [ActivityReportResponse](#m-ActivityReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java`, méthode `activityReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-reporting-activity-csv"></a>
### GET /sni/api/v1/reporting/activity/csv

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java`, méthode `activityReportCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-reporting-activity-pdf"></a>
### GET /sni/api/v1/reporting/activity/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java`, méthode `activityReportPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-reports-activity"></a>
### GET /sni/api/v1/reports/activity

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ActivityReportResponse`
- Modèles : [ActivityReportResponse](#m-ActivityReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java`, méthode `activityReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-reports-activity-csv"></a>
### GET /sni/api/v1/reports/activity/csv

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java`, méthode `activityReportCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-reports-activity-pdf"></a>
### GET /sni/api/v1/reports/activity/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java`, méthode `activityReportPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ActivityReportResponse"></a>
#### ActivityReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `bookings` | [ActivityReportResponse.BookingSection](#m-ActivityReportResponse-BookingSection) |  |  |
| `invoices` | [ActivityReportResponse.InvoiceSection](#m-ActivityReportResponse-InvoiceSection) |  |  |
| `payments` | [ActivityReportResponse.PaymentSection](#m-ActivityReportResponse-PaymentSection) |  |  |
| `subscriptions` | [ActivityReportResponse.SubscriptionSection](#m-ActivityReportResponse-SubscriptionSection) |  |  |
| `stock` | [ActivityReportResponse.StockSection](#m-ActivityReportResponse-StockSection) |  |  |
| `contracts` | [ActivityReportResponse.ContractSection](#m-ActivityReportResponse-ContractSection) |  |  |
| `members` | [ActivityReportResponse.MemberSection](#m-ActivityReportResponse-MemberSection) |  |  |
| `support` | [ActivityReportResponse.SupportSection](#m-ActivityReportResponse-SupportSection) |  |  |

<a id="m-ActivityReportResponse-BookingSection"></a>
#### ActivityReportResponse.BookingSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `totalCount` | entier int64 ⚠ |  |  |
| `confirmedCount` | entier int64 ⚠ |  |  |
| `completedCount` | entier int64 ⚠ |  |  |
| `cancelledCount` | entier int64 ⚠ |  |  |
| `totalRevenue` | nombre décimal |  |  |
| `totalBookedMinutes` | entier int64 ⚠ |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-InvoiceSection"></a>
#### ActivityReportResponse.InvoiceSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `totalCount` | entier int64 ⚠ |  |  |
| `totalInvoiced` | nombre décimal |  |  |
| `totalPaid` | nombre décimal |  |  |
| `totalOutstanding` | nombre décimal |  |  |
| `overdueCount` | entier int64 ⚠ |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-PaymentSection"></a>
#### ActivityReportResponse.PaymentSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionCount` | entier int64 ⚠ |  |  |
| `totalReceived` | nombre décimal |  |  |
| `cashAmount` | nombre décimal |  |  |
| `walletAmount` | nombre décimal |  |  |
| `mobileMoneyAmount` | nombre décimal |  |  |
| `bankTransferAmount` | nombre décimal |  |  |
| `cardAmount` | nombre décimal |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-SubscriptionSection"></a>
#### ActivityReportResponse.SubscriptionSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `activeCount` | entier int64 ⚠ |  |  |
| `newCount` | entier int64 ⚠ |  |  |
| `cancelledCount` | entier int64 ⚠ |  |  |
| `renewedCount` | entier int64 ⚠ |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-StockSection"></a>
#### ActivityReportResponse.StockSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `inboundMovements` | entier int64 ⚠ |  |  |
| `outboundMovements` | entier int64 ⚠ |  |  |
| `inboundValue` | nombre décimal |  |  |
| `outboundValue` | nombre décimal |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-ContractSection"></a>
#### ActivityReportResponse.ContractSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signedCount` | entier int64 ⚠ |  |  |
| `expiredCount` | entier int64 ⚠ |  |  |
| `activeCount` | entier int64 ⚠ |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-MemberSection"></a>
#### ActivityReportResponse.MemberSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `totalActive` | entier int64 ⚠ |  |  |
| `newCount` | entier int64 ⚠ |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-SupportSection"></a>
#### ActivityReportResponse.SupportSection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `openedCount` | entier int64 ⚠ |  |  |
| `resolvedCount` | entier int64 ⚠ |  |  |
| `closedCount` | entier int64 ⚠ |  |  |
| `highPriorityCount` | entier int64 ⚠ |  |  |
| `variation` | [ActivityReportResponse.Variation](#m-ActivityReportResponse-Variation) |  |  |

<a id="m-ActivityReportResponse-Variation"></a>
#### ActivityReportResponse.Variation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `previousValue` | nombre décimal |  |  |
| `currentValue` | nombre décimal |  |  |
| `changePercent` | nombre décimal |  |  |
