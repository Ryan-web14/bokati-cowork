# CashStatisticsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-statistics-cashiers-cashiercode"></a>
### GET /sni/api/v1/cash-registers/statistics/cashiers/{cashierCode}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashierStatisticsResponse`
- Modèles : [CashierStatisticsResponse](#m-CashierStatisticsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStatisticsController.java`, méthode `cashierStatistics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `cashierCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-statistics-registers-registercode"></a>
### GET /sni/api/v1/cash-registers/statistics/registers/{registerCode}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashRegisterStatisticsResponse`
- Modèles : [CashRegisterStatisticsResponse](#m-CashRegisterStatisticsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStatisticsController.java`, méthode `registerStatistics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-cash-registers-statistics-sessions-sessionnumber"></a>
### GET /sni/api/v1/cash-registers/statistics/sessions/{sessionNumber}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashSessionStatisticsResponse`
- Modèles : [CashSessionStatisticsResponse](#m-CashSessionStatisticsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStatisticsController.java`, méthode `sessionStatistics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashierStatisticsResponse"></a>
#### CashierStatisticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `cashierCode` | texte |  |  |
| `sessionCount` | entier int64 ⚠ |  |  |
| `closedSessionCount` | entier int64 ⚠ |  |  |
| `reviewSessionCount` | entier int64 ⚠ |  |  |
| `reviewFrequencyPercentage` | nombre décimal |  |  |
| `averageVarianceAmount` | nombre décimal |  |  |
| `varianceStandardDeviation` | nombre décimal |  |  |
| `varianceTrend` | texte |  |  |
| `averageSessionDurationMinutes` | nombre décimal |  |  |
| `riskScore` | nombre décimal |  |  |

<a id="m-CashRegisterStatisticsResponse"></a>
#### CashRegisterStatisticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `registerName` | texte |  |  |
| `sessionCount` | entier int64 ⚠ |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `averageSessionDurationMinutes` | nombre décimal |  |  |
| `averageVarianceAmount` | nombre décimal |  |  |
| `peakHours` | [CashRegisterStatisticsResponse.HourlyVolume](#m-CashRegisterStatisticsResponse-HourlyVolume)[] |  |  |
| `periodOverPeriodChangePercentage` | nombre décimal |  |  |

<a id="m-CashSessionStatisticsResponse"></a>
#### CashSessionStatisticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `openedBy` | texte |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `durationMinutes` | entier int64 ⚠ |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `transactionsPerHour` | nombre décimal |  |  |
| `totalInflow` | nombre décimal |  |  |
| `totalOutflow` | nombre décimal |  |  |
| `inOutRatio` | nombre décimal |  |  |
| `idleMinutes` | entier int64 ⚠ |  |  |
| `averageMovementAmount` | nombre décimal |  |  |
| `varianceAmount` | nombre décimal |  |  |
| `variancePercentage` | nombre décimal |  |  |

<a id="m-CashRegisterStatisticsResponse-HourlyVolume"></a>
#### CashRegisterStatisticsResponse.HourlyVolume

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hourOfDay` | entier |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `totalAmount` | nombre décimal |  |  |
