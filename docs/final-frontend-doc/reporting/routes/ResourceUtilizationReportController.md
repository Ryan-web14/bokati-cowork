# ResourceUtilizationReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-resource-utilization"></a>
### GET /sni/api/v1/reporting/resource-utilization

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ResourceUtilizationReportResponse`
- Modèles : [ResourceUtilizationReportResponse](#m-ResourceUtilizationReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ResourceUtilizationReportController.java`, méthode `resourceUtilization`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-resource-utilization-pdf"></a>
### GET /sni/api/v1/reporting/resource-utilization/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ResourceUtilizationReportController.java`, méthode `resourceUtilizationPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-resource-utilization"></a>
### GET /sni/api/v1/reports/resource-utilization

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ResourceUtilizationReportResponse`
- Modèles : [ResourceUtilizationReportResponse](#m-ResourceUtilizationReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ResourceUtilizationReportController.java`, méthode `resourceUtilization`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-resource-utilization-pdf"></a>
### GET /sni/api/v1/reports/resource-utilization/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/ResourceUtilizationReportController.java`, méthode `resourceUtilizationPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceUtilizationReportResponse"></a>
#### ResourceUtilizationReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `periodDays` | entier |  |  |
| `overallOccupancyRate` | nombre décimal |  |  |
| `resources` | [ResourceUtilizationReportResponse.ResourceUtilization](#m-ResourceUtilizationReportResponse-ResourceUtilization)[] |  |  |
| `hourlyHeatmap` | [ResourceUtilizationReportResponse.HourlyHeatmap](#m-ResourceUtilizationReportResponse-HourlyHeatmap)[] |  |  |
| `underutilized` | [ResourceUtilizationReportResponse.UnderutilizedResource](#m-ResourceUtilizationReportResponse-UnderutilizedResource)[] |  |  |

<a id="m-ResourceUtilizationReportResponse-ResourceUtilization"></a>
#### ResourceUtilizationReportResponse.ResourceUtilization

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `resourceType` | texte |  |  |
| `totalBookings` | entier int64 ⚠ |  |  |
| `morningBookings` | entier int64 ⚠ |  |  |
| `afternoonBookings` | entier int64 ⚠ |  |  |
| `eveningBookings` | entier int64 ⚠ |  |  |
| `bookedMinutes` | entier int64 ⚠ |  |  |
| `availableMinutes` | entier int64 ⚠ |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `totalRevenue` | nombre décimal |  |  |
| `revenuePerHour` | nombre décimal |  |  |

<a id="m-ResourceUtilizationReportResponse-HourlyHeatmap"></a>
#### ResourceUtilizationReportResponse.HourlyHeatmap

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `bookingCount` | entier int64 ⚠ |  |  |
| `percentage` | nombre décimal |  |  |

<a id="m-ResourceUtilizationReportResponse-UnderutilizedResource"></a>
#### ResourceUtilizationReportResponse.UnderutilizedResource

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `resourceType` | texte |  |  |
| `bookedMinutes` | entier int64 ⚠ |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `totalRevenue` | nombre décimal |  |  |
