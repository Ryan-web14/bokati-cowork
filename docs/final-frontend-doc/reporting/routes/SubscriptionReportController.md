# SubscriptionReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-subscriptions"></a>
### GET /sni/api/v1/reporting/subscriptions

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `SubscriptionReportResponse`
- Modèles : [SubscriptionReportResponse](#m-SubscriptionReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/SubscriptionReportController.java`, méthode `subscriptionReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `months` | requête | `int` |  | 12 |
| `renewalDays` | requête | `int` |  | 30 |

<a id="r-get-sni-api-v1-reporting-subscriptions-pdf"></a>
### GET /sni/api/v1/reporting/subscriptions/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/SubscriptionReportController.java`, méthode `subscriptionReportPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `months` | requête | `int` |  | 12 |
| `renewalDays` | requête | `int` |  | 30 |

<a id="r-get-sni-api-v1-reports-subscriptions"></a>
### GET /sni/api/v1/reports/subscriptions

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `SubscriptionReportResponse`
- Modèles : [SubscriptionReportResponse](#m-SubscriptionReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/SubscriptionReportController.java`, méthode `subscriptionReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `months` | requête | `int` |  | 12 |
| `renewalDays` | requête | `int` |  | 30 |

<a id="r-get-sni-api-v1-reports-subscriptions-pdf"></a>
### GET /sni/api/v1/reports/subscriptions/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/SubscriptionReportController.java`, méthode `subscriptionReportPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `months` | requête | `int` |  | 12 |
| `renewalDays` | requête | `int` |  | 30 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionReportResponse"></a>
#### SubscriptionReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `totalActive` | entier int64 ⚠ |  |  |
| `currentMrr` | nombre décimal |  |  |
| `mrrTrend` | [SubscriptionReportResponse.MrrDataPoint](#m-SubscriptionReportResponse-MrrDataPoint)[] |  |  |
| `activeByPlan` | [SubscriptionReportResponse.PlanDistribution](#m-SubscriptionReportResponse-PlanDistribution)[] |  |  |
| `churnTrend` | [SubscriptionReportResponse.ChurnDataPoint](#m-SubscriptionReportResponse-ChurnDataPoint)[] |  |  |
| `upcomingRenewals` | [SubscriptionReportResponse.UpcomingRenewal](#m-SubscriptionReportResponse-UpcomingRenewal)[] |  |  |
| `revenueByPlan` | [SubscriptionReportResponse.PlanRevenue](#m-SubscriptionReportResponse-PlanRevenue)[] |  |  |

<a id="m-SubscriptionReportResponse-MrrDataPoint"></a>
#### SubscriptionReportResponse.MrrDataPoint

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `month` | date AAAA-MM-JJ |  |  |
| `mrr` | nombre décimal |  |  |
| `activeCount` | entier int64 ⚠ |  |  |

<a id="m-SubscriptionReportResponse-PlanDistribution"></a>
#### SubscriptionReportResponse.PlanDistribution

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planName` | texte |  |  |
| `subscriptionCount` | entier int64 ⚠ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-SubscriptionReportResponse-ChurnDataPoint"></a>
#### SubscriptionReportResponse.ChurnDataPoint

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `month` | date AAAA-MM-JJ |  |  |
| `cancelledCount` | entier int64 ⚠ |  |  |
| `totalCount` | entier int64 ⚠ |  |  |
| `churnRate` | nombre décimal |  |  |

<a id="m-SubscriptionReportResponse-UpcomingRenewal"></a>
#### SubscriptionReportResponse.UpcomingRenewal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `subscriberCode` | texte |  |  |
| `subscriberType` | texte |  |  |
| `planName` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `daysUntilRenewal` | entier int64 ⚠ |  |  |

<a id="m-SubscriptionReportResponse-PlanRevenue"></a>
#### SubscriptionReportResponse.PlanRevenue

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planName` | texte |  |  |
| `subscriptionCount` | entier int64 ⚠ |  |  |
| `totalRevenue` | nombre décimal |  |  |
| `percentage` | nombre décimal |  |  |
