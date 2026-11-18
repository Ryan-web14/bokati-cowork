# ResourceOperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-incidents"></a>
### GET /sni/api/v1/resource-incidents

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceOperations.IncidentResponse>`
- Modèles : [ResourceOperations.IncidentResponse](#m-ResourceOperations-IncidentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `incidents`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` |  |  |
| `status` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-resource-incidents"></a>
### POST /sni/api/v1/resource-incidents

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceOperations.IncidentRequest`
- Réponse : `ResourceOperations.IncidentResponse`
- Modèles : [ResourceOperations.IncidentResponse](#m-ResourceOperations-IncidentResponse), [ResourceOperations.IncidentRequest](#m-ResourceOperations-IncidentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `report`

<a id="r-get-sni-api-v1-resource-incidents-code"></a>
### GET /sni/api/v1/resource-incidents/{code}

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `ResourceOperations.IncidentResponse`
- Modèles : [ResourceOperations.IncidentResponse](#m-ResourceOperations-IncidentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `incident`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resource-incidents-code-resolve"></a>
### POST /sni/api/v1/resource-incidents/{code}/resolve

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceOperationsController.ResolveRequest`
- Réponse : `ResourceOperations.IncidentResponse`
- Modèles : [ResourceOperations.IncidentResponse](#m-ResourceOperations-IncidentResponse), [ResourceOperationsController.ResolveRequest](#m-ResourceOperationsController-ResolveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `resolve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resource-incidents-code-start"></a>
### POST /sni/api/v1/resource-incidents/{code}/start

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Réponse : `ResourceOperations.IncidentResponse`
- Modèles : [ResourceOperations.IncidentResponse](#m-ResourceOperations-IncidentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `start`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-task-templates"></a>
### GET /sni/api/v1/resource-task-templates

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceOperations.TemplateResponse>`
- Modèles : [ResourceOperations.TemplateResponse](#m-ResourceOperations-TemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `templates`

<a id="r-post-sni-api-v1-resource-task-templates"></a>
### POST /sni/api/v1/resource-task-templates

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceOperations.TemplateRequest`
- Réponse : `ResourceOperations.TemplateResponse`
- Modèles : [ResourceOperations.TemplateResponse](#m-ResourceOperations-TemplateResponse), [ResourceOperations.TemplateRequest](#m-ResourceOperations-TemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `createTemplate`

<a id="r-delete-sni-api-v1-resource-task-templates-id"></a>
### DELETE /sni/api/v1/resource-task-templates/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `deleteTemplate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-patch-sni-api-v1-resource-task-templates-id"></a>
### PATCH /sni/api/v1/resource-task-templates/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceOperations.TemplateRequest`
- Réponse : `ResourceOperations.TemplateResponse`
- Modèles : [ResourceOperations.TemplateResponse](#m-ResourceOperations-TemplateResponse), [ResourceOperations.TemplateRequest](#m-ResourceOperations-TemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `updateTemplate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-get-sni-api-v1-resources-code-maintenance-history"></a>
### GET /sni/api/v1/resources/{code}/maintenance-history

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceOperations.HistoryEntry>`
- Modèles : [ResourceOperations.HistoryEntry](#m-ResourceOperations-HistoryEntry)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOperationsController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-ResourceOperationsController-ResolveRequest"></a>
#### ResourceOperationsController.ResolveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resolution` | texte |  |  |
| `reopen` | booléen |  |  |

<a id="m-ResourceOperations-TemplateResponse"></a>
#### ResourceOperations.TemplateResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `resourceCode` | texte |  |  |
| `serviceItemCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `moment` | texte |  |  |
| `offsetMinutes` | entier |  |  |
| `priority` | texte |  |  |
| `assignedTo` | entier int64 (texte au-delà de 2^53) |  |  |
| `checklist` | texte[] |  |  |
| `active` | booléen |  |  |

<a id="m-ResourceOperations-TemplateRequest"></a>
#### ResourceOperations.TemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `serviceItemCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `moment` | texte |  |  |
| `offsetMinutes` | entier |  |  |
| `priority` | texte |  |  |
| `assignedTo` | entier int64 (texte au-delà de 2^53) |  |  |
| `checklist` | texte[] |  |  |
| `active` | booléen |  |  |

<a id="m-ResourceOperations-HistoryEntry"></a>
#### ResourceOperations.HistoryEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `reference` | texte |  |  |
| `title` | texte |  |  |
| `status` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `detail` | texte |  |  |
