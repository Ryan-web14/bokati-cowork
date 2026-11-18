# BookingOperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings-number-end-report"></a>
### GET /sni/api/v1/bookings/{number}/end-report

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingOperations.EndReportResponse>`
- Modèles : [BookingOperations.EndReportResponse](#m-BookingOperations-EndReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingOperationsController.java`, méthode `endReports`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-number-end-report"></a>
### POST /sni/api/v1/bookings/{number}/end-report

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Corps JSON (obligatoire) : `BookingOperations.EndReportRequest`
- Réponse : `BookingOperations.EndReportResponse`
- Modèles : [BookingOperations.EndReportResponse](#m-BookingOperations-EndReportResponse), [BookingOperations.EndReportRequest](#m-BookingOperations-EndReportRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingOperationsController.java`, méthode `endReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-number-usage-rules"></a>
### GET /sni/api/v1/bookings/{number}/usage-rules

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingOperations.UsageRulesView`
- Modèles : [BookingOperations.UsageRulesView](#m-BookingOperations-UsageRulesView)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingOperationsController.java`, méthode `usageRules`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-bookings-number-usage-rules-accept"></a>
### POST /sni/api/v1/bookings/{number}/usage-rules/accept

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `BookingOperations.UsageRulesView`
- Modèles : [BookingOperations.UsageRulesView](#m-BookingOperations-UsageRulesView)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingOperationsController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingOperations-EndReportResponse"></a>
#### BookingOperations.EndReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `bookingNumber` | texte |  |  |
| `condition` | texte |  |  |
| `checklist` | [BookingOperations.ChecklistAnswer](#m-BookingOperations-ChecklistAnswer)[] |  |  |
| `photoDocumentCodes` | texte[] |  |  |
| `notes` | texte |  |  |
| `reportedByType` | texte |  |  |
| `reportedBy` | texte |  |  |
| `incidentCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BookingOperations-EndReportRequest"></a>
#### BookingOperations.EndReportRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `condition` | texte |  |  |
| `checklist` | [BookingOperations.ChecklistAnswer](#m-BookingOperations-ChecklistAnswer)[] |  |  |
| `photoDocumentCodes` | texte[] |  |  |
| `notes` | texte |  |  |

<a id="m-BookingOperations-UsageRulesView"></a>
#### BookingOperations.UsageRulesView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rules` | texte |  |  |
| `version` | texte |  |  |
| `acceptedAt` | date-heure ISO 8601 UTC |  |  |
| `upToDate` | booléen |  |  |
| `endChecklist` | texte[] |  |  |

<a id="m-BookingOperations-ChecklistAnswer"></a>
#### BookingOperations.ChecklistAnswer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `done` | booléen |  |  |
