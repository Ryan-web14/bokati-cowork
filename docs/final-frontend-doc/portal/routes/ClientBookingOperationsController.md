# ClientBookingOperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 4 route(s).

## Routes

<a id="r-post-sni-api-v1-client-bookings-number-end-report"></a>
### POST /sni/api/v1/client/bookings/{number}/end-report

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `BookingOperations.EndReportRequest`
- Réponse : `BookingOperations.EndReportResponse`
- Modèles : [BookingOperations.EndReportResponse](#m-BookingOperations-EndReportResponse), [BookingOperations.EndReportRequest](#m-BookingOperations-EndReportRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingOperationsController.java`, méthode `endReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-bookings-number-usage-rules"></a>
### GET /sni/api/v1/client/bookings/{number}/usage-rules

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingOperations.UsageRulesView`
- Modèles : [BookingOperations.UsageRulesView](#m-BookingOperations-UsageRulesView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingOperationsController.java`, méthode `usageRules`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-bookings-number-usage-rules-accept"></a>
### POST /sni/api/v1/client/bookings/{number}/usage-rules/accept

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `BookingOperations.UsageRulesView`
- Modèles : [BookingOperations.UsageRulesView](#m-BookingOperations-UsageRulesView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingOperationsController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-resource-incidents"></a>
### POST /sni/api/v1/client/resource-incidents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ResourceOperations.IncidentRequest`
- Réponse : `ResourceOperations.IncidentResponse`
- Modèles : [ResourceOperations.IncidentResponse](#m-ResourceOperations-IncidentResponse), [ResourceOperations.IncidentRequest](#m-ResourceOperations-IncidentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/booking/controller/ClientBookingOperationsController.java`, méthode `report`

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

<a id="m-ResourceOperations-IncidentResponse"></a>
#### ResourceOperations.IncidentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `code` | texte |  |  |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `bookingNumber` | texte |  |  |
| `reportedByType` | texte |  |  |
| `reporterName` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `severity` | texte |  |  |
| `status` | texte |  |  |
| `blocked` | booléen |  |  |
| `photoDocumentCodes` | texte[] |  |  |
| `ticketNumber` | texte |  |  |
| `resolution` | texte |  |  |
| `resolvedBy` | texte |  |  |
| `resolvedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ResourceOperations-IncidentRequest"></a>
#### ResourceOperations.IncidentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `bookingNumber` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `severity` | texte |  |  |
| `block` | booléen |  |  |
| `cancelBookings` | booléen |  |  |
| `photoDocumentCodes` | texte[] |  |  |

<a id="m-BookingOperations-ChecklistAnswer"></a>
#### BookingOperations.ChecklistAnswer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `done` | booléen |  |  |
