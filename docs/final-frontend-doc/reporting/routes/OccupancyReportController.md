# OccupancyReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-occupancy"></a>
### GET /sni/api/v1/reporting/occupancy

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `OccupancyReportResponse`
- Modèles : [OccupancyReportResponse](#m-OccupancyReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/OccupancyReportController.java`, méthode `occupancy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reporting-occupancy-pdf"></a>
### GET /sni/api/v1/reporting/occupancy/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/OccupancyReportController.java`, méthode `occupancyPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-occupancy"></a>
### GET /sni/api/v1/reports/occupancy

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `OccupancyReportResponse`
- Modèles : [OccupancyReportResponse](#m-OccupancyReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/OccupancyReportController.java`, méthode `occupancy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reports-occupancy-pdf"></a>
### GET /sni/api/v1/reports/occupancy/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/OccupancyReportController.java`, méthode `occupancyPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `String` |  |  |
| `to` | requête | `String` |  |  |
| `fromDate` | requête | `String` |  |  |
| `toDate` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-OccupancyReportResponse"></a>
#### OccupancyReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `periodDays` | entier |  |  |
| `totalBookings` | entier int64 ⚠ |  |  |
| `totalBookedMinutes` | entier int64 ⚠ |  |  |
| `overallOccupancyRate` | nombre décimal |  |  |
| `byResourceType` | [OccupancyReportResponse.ResourceTypeOccupancy](#m-OccupancyReportResponse-ResourceTypeOccupancy)[] |  |  |
| `byResource` | [OccupancyReportResponse.ResourceOccupancy](#m-OccupancyReportResponse-ResourceOccupancy)[] |  |  |

<a id="m-OccupancyReportResponse-ResourceTypeOccupancy"></a>
#### OccupancyReportResponse.ResourceTypeOccupancy

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `typeCode` | texte |  |  |
| `typeName` | texte |  |  |
| `resourceCount` | entier int64 ⚠ |  |  |
| `totalBookings` | entier int64 ⚠ |  |  |
| `totalBookedMinutes` | entier int64 ⚠ |  |  |
| `totalRevenue` | nombre décimal |  |  |
| `occupancyRate` | nombre décimal |  |  |

<a id="m-OccupancyReportResponse-ResourceOccupancy"></a>
#### OccupancyReportResponse.ResourceOccupancy

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `typeCode` | texte |  |  |
| `typeName` | texte |  |  |
| `bookingCount` | entier int64 ⚠ |  |  |
| `bookedMinutes` | entier int64 ⚠ |  |  |
| `availableMinutes` | entier int64 ⚠ |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `revenue` | nombre décimal |  |  |
