# AnalyticsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [analytics](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-analytics-comparison"></a>
### GET /sni/api/v1/analytics/comparison

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `AnalyticsComparisonResponse`
- Modèles : [AnalyticsComparisonResponse](#m-AnalyticsComparisonResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `comparison`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` | oui |  |
| `toDate` | requête | `String` |  |  |
| `compareWithPrevious` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-analytics-live"></a>
### GET /sni/api/v1/analytics/live

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `AnalyticsLiveResponse`
- Modèles : [AnalyticsLiveResponse](#m-AnalyticsLiveResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `live`

<a id="r-get-sni-api-v1-analytics-overview"></a>
### GET /sni/api/v1/analytics/overview

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `AnalyticsOverviewResponse`
- Modèles : [AnalyticsOverviewResponse](#m-AnalyticsOverviewResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `overview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-analytics-top-owners"></a>
### GET /sni/api/v1/analytics/top-owners

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `List<TopOwnerResponse>`
- Modèles : [TopOwnerResponse](#m-TopOwnerResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `topOwners`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `limit` | requête | `int` |  | 10 |

<a id="r-get-sni-api-v1-analytics-top-resources"></a>
### GET /sni/api/v1/analytics/top-resources

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `List<TopResourceResponse>`
- Modèles : [TopResourceResponse](#m-TopResourceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `topResources`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `limit` | requête | `int` |  | 10 |

<a id="r-get-sni-api-v1-analytics-trends-bookings"></a>
### GET /sni/api/v1/analytics/trends/bookings

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `List<BookingTrendResponse>`
- Modèles : [BookingTrendResponse](#m-BookingTrendResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `bookingTrend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |
| `groupBy` | requête | `String` |  | day |

<a id="r-get-sni-api-v1-reports-attendance-summary"></a>
### GET /sni/api/v1/reports/attendance/summary

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `AnalyticsOverviewResponse.BookingMetrics`
- Modèles : [AnalyticsOverviewResponse.BookingMetrics](#m-AnalyticsOverviewResponse-BookingMetrics)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `attendance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-financial-summary"></a>
### GET /sni/api/v1/reports/financial/summary

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `AnalyticsOverviewResponse.FinancialMetrics`
- Modèles : [AnalyticsOverviewResponse.FinancialMetrics](#m-AnalyticsOverviewResponse-FinancialMetrics)
- Code : `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java`, méthode `financial`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AnalyticsComparisonResponse"></a>
#### AnalyticsComparisonResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `current` | [AnalyticsOverviewResponse](#m-AnalyticsOverviewResponse) |  |  |
| `previous` | [AnalyticsOverviewResponse](#m-AnalyticsOverviewResponse) |  |  |
| `revenue` | [AnalyticsComparisonResponse.Variation](#m-AnalyticsComparisonResponse-Variation) |  |  |
| `paid` | [AnalyticsComparisonResponse.Variation](#m-AnalyticsComparisonResponse-Variation) |  |  |
| `bookings` | [AnalyticsComparisonResponse.Variation](#m-AnalyticsComparisonResponse-Variation) |  |  |
| `activeSubscriptions` | [AnalyticsComparisonResponse.Variation](#m-AnalyticsComparisonResponse-Variation) |  |  |

<a id="m-AnalyticsLiveResponse"></a>
#### AnalyticsLiveResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `activeBookings` | entier int64 ⚠ |  |  |
| `checkedInBookings` | entier int64 ⚠ |  |  |
| `occupiedResources` | entier int64 ⚠ |  |  |
| `totalBookableResources` | entier int64 ⚠ |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `availableResources` | entier int64 ⚠ |  |  |
| `pendingApproval` | entier int64 ⚠ |  |  |
| `upcomingNextHour` | entier int64 ⚠ |  |  |
| `activeHolds` | entier int64 ⚠ |  |  |
| `bookingsToday` | entier int64 ⚠ |  |  |
| `revenueToday` | nombre décimal |  |  |
| `avgBookingAmount` | nombre décimal |  |  |
| `bookedMinutesToday` | entier int64 ⚠ |  |  |
| `cancelledToday` | entier int64 ⚠ |  |  |
| `noShowToday` | entier int64 ⚠ |  |  |
| `checkInsToday` | entier int64 ⚠ |  |  |
| `newMembersToday` | entier int64 ⚠ |  |  |

<a id="m-AnalyticsOverviewResponse"></a>
#### AnalyticsOverviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `financial` | [AnalyticsOverviewResponse.FinancialMetrics](#m-AnalyticsOverviewResponse-FinancialMetrics) |  |  |
| `payment` | [AnalyticsOverviewResponse.PaymentMetrics](#m-AnalyticsOverviewResponse-PaymentMetrics) |  |  |
| `wallet` | [AnalyticsOverviewResponse.WalletMetrics](#m-AnalyticsOverviewResponse-WalletMetrics) |  |  |
| `booking` | [AnalyticsOverviewResponse.BookingMetrics](#m-AnalyticsOverviewResponse-BookingMetrics) |  |  |
| `subscription` | [AnalyticsOverviewResponse.SubscriptionMetrics](#m-AnalyticsOverviewResponse-SubscriptionMetrics) |  |  |
| `customer` | [AnalyticsOverviewResponse.CustomerMetrics](#m-AnalyticsOverviewResponse-CustomerMetrics) |  |  |
| `inventory` | [AnalyticsOverviewResponse.InventoryMetrics](#m-AnalyticsOverviewResponse-InventoryMetrics) |  |  |

<a id="m-TopOwnerResponse"></a>
#### TopOwnerResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `ownerName` | texte |  |  |
| `bookingCount` | entier int64 ⚠ |  |  |
| `confirmedCount` | entier int64 ⚠ |  |  |
| `completedCount` | entier int64 ⚠ |  |  |
| `cancelledCount` | entier int64 ⚠ |  |  |
| `revenue` | nombre décimal |  |  |
| `bookedMinutes` | entier int64 ⚠ |  |  |

<a id="m-TopResourceResponse"></a>
#### TopResourceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `resourceTypeCode` | texte |  |  |
| `resourceTypeName` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `resourceGroupName` | texte |  |  |
| `bookingCount` | entier int64 ⚠ |  |  |
| `confirmedCount` | entier int64 ⚠ |  |  |
| `completedCount` | entier int64 ⚠ |  |  |
| `cancelledCount` | entier int64 ⚠ |  |  |
| `revenue` | nombre décimal |  |  |
| `bookedMinutes` | entier int64 ⚠ |  |  |

<a id="m-BookingTrendResponse"></a>
#### BookingTrendResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `bookingCount` | entier int64 ⚠ |  |  |
| `confirmedCount` | entier int64 ⚠ |  |  |
| `cancelledCount` | entier int64 ⚠ |  |  |
| `revenue` | nombre décimal |  |  |

<a id="m-AnalyticsOverviewResponse-BookingMetrics"></a>
#### AnalyticsOverviewResponse.BookingMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingCount` | entier int64 ⚠ |  |  |
| `confirmedBookings` | entier int64 ⚠ |  |  |
| `completedBookings` | entier int64 ⚠ |  |  |
| `cancelledBookings` | entier int64 ⚠ |  |  |
| `noShowBookings` | entier int64 ⚠ |  |  |
| `bookingRevenue` | nombre décimal |  |  |
| `bookedMinutes` | entier int64 ⚠ |  |  |
| `participantCount` | entier int64 ⚠ |  |  |

<a id="m-AnalyticsOverviewResponse-FinancialMetrics"></a>
#### AnalyticsOverviewResponse.FinancialMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoiceCount` | entier int64 ⚠ |  |  |
| `invoicedAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `outstandingAmount` | nombre décimal |  |  |
| `overdueInvoiceCount` | entier int64 ⚠ |  |  |
| `quotationCount` | entier int64 ⚠ |  |  |
| `quotedAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `additionalCentAmount` | nombre décimal |  |  |

<a id="m-AnalyticsComparisonResponse-Variation"></a>
#### AnalyticsComparisonResponse.Variation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `current` | nombre décimal |  |  |
| `previous` | nombre décimal |  |  |
| `percentChange` | nombre décimal |  |  |

<a id="m-AnalyticsOverviewResponse-PaymentMetrics"></a>
#### AnalyticsOverviewResponse.PaymentMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionCount` | entier int64 ⚠ |  |  |
| `succeededAmount` | nombre décimal |  |  |
| `cashAmount` | nombre décimal |  |  |
| `walletAmount` | nombre décimal |  |  |
| `mobileMoneyAmount` | nombre décimal |  |  |
| `bankTransferAmount` | nombre décimal |  |  |
| `cardAmount` | nombre décimal |  |  |
| `failedTransactionCount` | entier int64 ⚠ |  |  |
| `pendingTransactionCount` | entier int64 ⚠ |  |  |

<a id="m-AnalyticsOverviewResponse-WalletMetrics"></a>
#### AnalyticsOverviewResponse.WalletMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletCount` | entier int64 ⚠ |  |  |
| `availableBalance` | nombre décimal |  |  |
| `heldBalance` | nombre décimal |  |  |
| `credits` | nombre décimal |  |  |
| `debits` | nombre décimal |  |  |

<a id="m-AnalyticsOverviewResponse-SubscriptionMetrics"></a>
#### AnalyticsOverviewResponse.SubscriptionMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `activeSubscriptions` | entier int64 ⚠ |  |  |
| `pendingSubscriptions` | entier int64 ⚠ |  |  |
| `suspendedSubscriptions` | entier int64 ⚠ |  |  |
| `activePasses` | entier int64 ⚠ |  |  |
| `monthlyRecurringRevenue` | nombre décimal |  |  |
| `billableUsageQuantity` | entier int64 ⚠ |  |  |

<a id="m-AnalyticsOverviewResponse-CustomerMetrics"></a>
#### AnalyticsOverviewResponse.CustomerMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerCount` | entier int64 ⚠ |  |  |
| `memberCount` | entier int64 ⚠ |  |  |
| `activeMembers` | entier int64 ⚠ |  |  |
| `guestBookingCount` | entier int64 ⚠ |  |  |

<a id="m-AnalyticsOverviewResponse-InventoryMetrics"></a>
#### AnalyticsOverviewResponse.InventoryMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCount` | entier int64 ⚠ |  |  |
| `stockValue` | nombre décimal |  |  |
| `lowStockAlerts` | entier int64 ⚠ |  |  |
| `openPurchaseOrders` | entier int64 ⚠ |  |  |
