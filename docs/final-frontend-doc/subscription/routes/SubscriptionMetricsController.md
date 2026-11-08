# SubscriptionMetricsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-subscription-metrics-kpis"></a>
### GET /sni/api/v1/subscription-metrics/kpis

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `SubscriptionKpiService.Kpis`
- Modèles : [SubscriptionKpiService.Kpis](#m-SubscriptionKpiService-Kpis)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/metrics/controller/SubscriptionMetricsController.java`, méthode `kpis`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-subscription-metrics-overview"></a>
### GET /sni/api/v1/subscription-metrics/overview

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `SubscriptionMetricsOverviewResponse`
- Modèles : [SubscriptionMetricsOverviewResponse](#m-SubscriptionMetricsOverviewResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/metrics/controller/SubscriptionMetricsController.java`, méthode `overview`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionKpiService-Kpis"></a>
#### SubscriptionKpiService.Kpis

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `activeSubscriptions` | entier int64 ⚠ |  |  |
| `mrr` | nombre décimal |  |  |
| `arr` | nombre décimal |  |  |
| `mrrByCycle` | objet { clé : nombre décimal } |  |  |
| `activeAtStart` | entier int64 ⚠ |  |  |
| `newInPeriod` | entier int64 ⚠ |  |  |
| `churnedInPeriod` | entier int64 ⚠ |  |  |
| `churnRate` | nombre décimal |  |  |
| `monthlyChurnRate` | nombre décimal |  |  |
| `newMrr` | nombre décimal |  |  |
| `churnedMrr` | nombre décimal |  |  |
| `netMrrMovement` | nombre décimal |  |  |
| `arpu` | nombre décimal |  |  |
| `customerLifetimeValue` | nombre décimal |  |  |
| `averageLifetimeMonths` | nombre décimal |  |  |
| `seatsOccupied` | entier int64 ⚠ |  |  |
| `workspaceCapacity` | entier int64 ⚠ |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `revenuePerSeat` | nombre décimal |  |  |
| `inGracePeriod` | entier int64 ⚠ |  |  |
| `pendingTermination` | entier int64 ⚠ |  |  |
| `pendingDocuments` | entier int64 ⚠ |  |  |

<a id="m-SubscriptionMetricsOverviewResponse"></a>
#### SubscriptionMetricsOverviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `activeSubscriptions` | entier int64 ⚠ |  |  |
| `pendingSubscriptions` | entier int64 ⚠ |  |  |
| `suspendedSubscriptions` | entier int64 ⚠ |  |  |
| `cancelledSubscriptions` | entier int64 ⚠ |  |  |
| `activePasses` | entier int64 ⚠ |  |  |
| `activeAddons` | entier int64 ⚠ |  |  |
| `activePromotions` | entier int64 ⚠ |  |  |
| `pendingBillableItems` | entier int64 ⚠ |  |  |
| `pendingBillableAmount` | nombre décimal |  |  |
| `activeSubscriptionMrr` | nombre décimal |  |  |
| `recordedUsageQuantity` | nombre décimal |  |  |
| `billableUsageQuantity` | nombre décimal |  |  |
| `overageCharges` | entier int64 ⚠ |  |  |
| `overageAmount` | nombre décimal |  |  |
| `rolloverRecords` | entier int64 ⚠ |  |  |
| `rolloverQuantity` | nombre décimal |  |  |
