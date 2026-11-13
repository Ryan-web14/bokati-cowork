# BillingDashboardController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-dashboard"></a>
### GET /sni/api/v1/billing/dashboard

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `BillingDashboardService.Dashboard`
- Modèles : [BillingDashboardService.Dashboard](#m-BillingDashboardService-Dashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDashboardController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `currency` | requête | `String` |  |  |
| `weeks` | requête | `int` |  | 8 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BillingDashboardService-Dashboard"></a>
#### BillingDashboardService.Dashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `currency` | texte |  |  |
| `quotes` | [BillingDashboardService.Quotes](#m-BillingDashboardService-Quotes) |  |  |
| `invoicing` | [BillingDashboardService.Invoicing](#m-BillingDashboardService-Invoicing) |  |  |
| `forecast` | [BillingDashboardService.WeekForecast](#m-BillingDashboardService-WeekForecast)[] |  |  |
| `deals` | [BillingDashboardService.DealStage](#m-BillingDashboardService-DealStage)[] |  |  |
| `topCustomers` | [BillingDashboardService.TopCustomer](#m-BillingDashboardService-TopCustomer)[] |  |  |

<a id="m-BillingDashboardService-Quotes"></a>
#### BillingDashboardService.Quotes

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `issued` | entier int64 ⚠ |  |  |
| `converted` | entier int64 ⚠ |  |  |
| `conversionRate` | nombre décimal |  |  |
| `issuedAmount` | nombre décimal |  |  |
| `convertedAmount` | nombre décimal |  |  |
| `averageDaysToInvoice` | nombre décimal |  |  |

<a id="m-BillingDashboardService-Invoicing"></a>
#### BillingDashboardService.Invoicing

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoices` | entier int64 ⚠ |  |  |
| `invoiced` | nombre décimal |  |  |
| `collected` | nombre décimal |  |  |
| `outstanding` | nombre décimal |  |  |
| `overdue` | nombre décimal |  |  |
| `averageDaysToPay` | nombre décimal |  |  |

<a id="m-BillingDashboardService-WeekForecast"></a>
#### BillingDashboardService.WeekForecast

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `weekStart` | date AAAA-MM-JJ |  |  |
| `expected` | nombre décimal |  |  |
| `documents` | entier int64 ⚠ |  |  |

<a id="m-BillingDashboardService-DealStage"></a>
#### BillingDashboardService.DealStage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stage` | texte |  |  |
| `deals` | entier int64 ⚠ |  |  |
| `expectedAmount` | nombre décimal |  |  |
| `weightedAmount` | nombre décimal |  |  |

<a id="m-BillingDashboardService-TopCustomer"></a>
#### BillingDashboardService.TopCustomer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `invoiced` | nombre décimal |  |  |
| `outstanding` | nombre décimal |  |  |
