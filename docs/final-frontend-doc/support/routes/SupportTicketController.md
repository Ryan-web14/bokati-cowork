# SupportTicketController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 20 route(s).

## Routes

<a id="r-get-sni-api-v1-support-tickets"></a>
### GET /sni/api/v1/support/tickets

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `PaginatedResponse<SupportDtos.SupportTicketResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [TicketStatus](#m-TicketStatus), [TicketCategory](#m-TicketCategory), [TicketPriority](#m-TicketPriority)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `TicketStatus` |  |  |
| `assignedTo` | requête | `String` |  |  |
| `ownerType` | requête | `String` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `category` | requête | `TicketCategory` |  |  |
| `priority` | requête | `TicketPriority` |  |  |
| `relatedType` | requête | `String` |  |  |
| `relatedCode` | requête | `String` |  |  |
| `overdueOnly` | requête | `Boolean` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-support-tickets"></a>
### POST /sni/api/v1/support/tickets

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.CreateTicketRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [SupportDtos.CreateTicketRequest](#m-SupportDtos-CreateTicketRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `create`

<a id="r-get-sni-api-v1-support-tickets-agents-performance"></a>
### GET /sni/api/v1/support/tickets/agents/performance

- Accès : back-office : rôle d'administration et droit `SUPPORT:METRICS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')`
- Réponse : `List<SupportDtos.AgentPerformanceEntry>`
- Modèles : [SupportDtos.AgentPerformanceEntry](#m-SupportDtos-AgentPerformanceEntry)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `agentPerformance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-support-tickets-analytics"></a>
### GET /sni/api/v1/support/tickets/analytics

- Accès : back-office : rôle d'administration et droit `SUPPORT:METRICS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')`
- Réponse : `SupportDtos.SupportAnalyticsResponse`
- Modèles : [SupportDtos.SupportAnalyticsResponse](#m-SupportDtos-SupportAnalyticsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `analytics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-support-tickets-analytics-csv"></a>
### GET /sni/api/v1/support/tickets/analytics.csv

- Accès : back-office : rôle d'administration et droit `SUPPORT:METRICS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `analyticsCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-support-tickets-by-owner"></a>
### GET /sni/api/v1/support/tickets/by-owner

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `SupportDtos.OwnerTicketSummaryResponse`
- Modèles : [SupportDtos.OwnerTicketSummaryResponse](#m-SupportDtos-OwnerTicketSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `byOwner`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `String` | oui |  |
| `ownerCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-support-tickets-metrics"></a>
### GET /sni/api/v1/support/tickets/metrics

- Accès : back-office : rôle d'administration et droit `SUPPORT:METRICS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')`
- Réponse : `SupportDtos.SupportMetricsResponse`
- Modèles : [SupportDtos.SupportMetricsResponse](#m-SupportDtos-SupportMetricsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `metrics`

<a id="r-get-sni-api-v1-support-tickets-ticketnumber"></a>
### GET /sni/api/v1/support/tickets/{ticketNumber}

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-support-tickets-ticketnumber-assign"></a>
### PATCH /sni/api/v1/support/tickets/{ticketNumber}/assign

- Accès : back-office : rôle d'administration et droit `SUPPORT:ASSIGN` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:ASSIGN','SUPPORT_ASSIGN')`
- Corps JSON (obligatoire) : `SupportDtos.AssignTicketRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [SupportDtos.AssignTicketRequest](#m-SupportDtos-AssignTicketRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-support-tickets-ticketnumber-attachments"></a>
### GET /sni/api/v1/support/tickets/{ticketNumber}/attachments

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `List<SupportDtos.AttachmentResponse>`
- Modèles : [SupportDtos.AttachmentResponse](#m-SupportDtos-AttachmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `listAttachments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-support-tickets-ticketnumber-attachments"></a>
### POST /sni/api/v1/support/tickets/{ticketNumber}/attachments

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Envoi : `multipart/form-data`
- Réponse : `SupportDtos.AttachmentResponse`
- Modèles : [SupportDtos.AttachmentResponse](#m-SupportDtos-AttachmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `uploadAttachment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `internal` | requête | `Boolean` |  |  |
| `messageId` | requête | `Long` |  |  |

<a id="r-delete-sni-api-v1-support-tickets-ticketnumber-attachments-id"></a>
### DELETE /sni/api/v1/support/tickets/{ticketNumber}/attachments/{id}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `deleteAttachment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-support-tickets-ticketnumber-attachments-id-download"></a>
### GET /sni/api/v1/support/tickets/{ticketNumber}/attachments/{id}/download

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `downloadAttachment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-support-tickets-ticketnumber-messages"></a>
### POST /sni/api/v1/support/tickets/{ticketNumber}/messages

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.AddTicketMessageRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [SupportDtos.AddTicketMessageRequest](#m-SupportDtos-AddTicketMessageRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `message`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-support-tickets-ticketnumber-messages-messageid-convert-to-article"></a>
### POST /sni/api/v1/support/tickets/{ticketNumber}/messages/{messageId}/convert-to-article

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `KnowledgeDtos.ConvertMessageToArticleRequest`
- Réponse : `KnowledgeDtos.KnowledgeArticleResponse`
- Modèles : [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse), [KnowledgeDtos.ConvertMessageToArticleRequest](#m-KnowledgeDtos-ConvertMessageToArticleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `convertMessageToArticle`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `messageId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-support-tickets-ticketnumber-status"></a>
### PATCH /sni/api/v1/support/tickets/{ticketNumber}/status

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.UpdateTicketStatusRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [SupportDtos.UpdateTicketStatusRequest](#m-SupportDtos-UpdateTicketStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `status`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-support-tickets-ticketnumber-tags"></a>
### POST /sni/api/v1/support/tickets/{ticketNumber}/tags

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.AddTagRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [SupportDtos.AddTagRequest](#m-SupportDtos-AddTagRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `addTag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-support-tickets-ticketnumber-tags-tag"></a>
### DELETE /sni/api/v1/support/tickets/{ticketNumber}/tags/{tag}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `removeTag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `tag` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-support-tickets-ticketnumber-task"></a>
### POST /sni/api/v1/support/tickets/{ticketNumber}/task

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE') and hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.CreateTicketTaskRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [SupportDtos.CreateTicketTaskRequest](#m-SupportDtos-CreateTicketTaskRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `createTask`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-support-tickets-ticketnumber-timeline"></a>
### GET /sni/api/v1/support/tickets/{ticketNumber}/timeline

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `List<SupportDtos.TicketEventResponse>`
- Modèles : [SupportDtos.TicketEventResponse](#m-SupportDtos-TicketEventResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTicketController.java`, méthode `timeline`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SupportDtos-SupportTicketResponse"></a>
#### SupportDtos.SupportTicketResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ticketNumber` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | texte |  |  |
| `contactPhone` | texte |  |  |
| `assignedTo` | texte |  |  |
| `relatedType` | texte |  |  |
| `relatedCode` | texte |  |  |
| `firstResponseDueAt` | date-heure ISO 8601 UTC |  |  |
| `resolutionDueAt` | date-heure ISO 8601 UTC |  |  |
| `firstRespondedAt` | date-heure ISO 8601 UTC |  |  |
| `resolvedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `csatScore` | entier |  |  |
| `csatComment` | texte |  |  |
| `csatSubmittedAt` | date-heure ISO 8601 UTC |  |  |
| `escalationLevel` | entier |  |  |
| `escalatedAt` | date-heure ISO 8601 UTC |  |  |
| `escalationReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `messages` | [SupportDtos.TicketMessageResponse](#m-SupportDtos-TicketMessageResponse)[] |  |  |
| `attachments` | [SupportDtos.AttachmentResponse](#m-SupportDtos-AttachmentResponse)[] |  |  |
| `tags` | texte[] |  |  |
| `messageCount` | entier |  |  |
| `attachmentCount` | entier |  |  |

<a id="m-TicketStatus"></a>
#### TicketStatus

Énumération : `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED`

<a id="m-TicketCategory"></a>
#### TicketCategory

Énumération : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER`

<a id="m-TicketPriority"></a>
#### TicketPriority

Énumération : `LOW`, `MEDIUM`, `HIGH`, `URGENT`

<a id="m-SupportDtos-CreateTicketRequest"></a>
#### SupportDtos.CreateTicketRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `contactName` | texte |  |  |
| `contactEmail` | texte |  |  |
| `contactPhone` | texte |  |  |
| `relatedType` | texte |  |  |
| `relatedCode` | texte |  |  |

<a id="m-SupportDtos-AgentPerformanceEntry"></a>
#### SupportDtos.AgentPerformanceEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `agentId` | entier int64 ⚠ |  |  |
| `assignedCount` | entier int64 ⚠ |  |  |
| `resolvedCount` | entier int64 ⚠ |  |  |
| `avgFirstResponseHours` | nombre décimal |  |  |
| `avgResolutionHours` | nombre décimal |  |  |
| `avgCsatScore` | nombre décimal |  |  |

<a id="m-SupportDtos-SupportAnalyticsResponse"></a>
#### SupportDtos.SupportAnalyticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date-heure ISO 8601 UTC |  |  |
| `to` | date-heure ISO 8601 UTC |  |  |
| `totalCreated` | entier int64 ⚠ |  |  |
| `totalResolved` | entier int64 ⚠ |  |  |
| `openAtEndOfPeriod` | entier int64 ⚠ |  |  |
| `slaBreachCount` | entier int64 ⚠ |  |  |
| `slaBreachRate` | nombre décimal |  |  |
| `avgFirstResponseHours` | nombre décimal |  |  |
| `avgResolutionHours` | nombre décimal |  |  |
| `reopenedCount` | entier int64 ⚠ |  |  |
| `waitingClientCount` | entier int64 ⚠ |  |  |
| `byCategory` | objet { clé : entier int64 ⚠ } |  |  |
| `byPriority` | objet { clé : entier int64 ⚠ } |  |  |
| `topTags` | objet { clé : entier int64 ⚠ } |  |  |
| `volumeByRelatedType` | objet { clé : entier int64 ⚠ } |  |  |
| `backlogByAgent` | objet { clé : entier int64 ⚠ } |  |  |
| `avgCsatScore` | nombre décimal |  |  |
| `csatResponseCount` | entier int64 ⚠ |  |  |
| `csatByCategory` | objet { clé : nombre décimal } |  |  |
| `csatByAgent` | objet { clé : nombre décimal } |  |  |
| `agentWorkload` | [SupportDtos.AgentWorkloadEntry](#m-SupportDtos-AgentWorkloadEntry)[] |  |  |

<a id="m-SupportDtos-OwnerTicketSummaryResponse"></a>
#### SupportDtos.OwnerTicketSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `openTickets` | entier int64 ⚠ |  |  |
| `slaBreaches` | entier int64 ⚠ |  |  |
| `avgCsatScore` | nombre décimal |  |  |
| `atRisk` | booléen |  |  |
| `recentTickets` | [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse)[] |  |  |

<a id="m-SupportDtos-SupportMetricsResponse"></a>
#### SupportDtos.SupportMetricsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `openTickets` | entier int64 ⚠ |  |  |
| `overdueFirstResponse` | entier int64 ⚠ |  |  |
| `overdueResolution` | entier int64 ⚠ |  |  |
| `resolvedTickets` | entier int64 ⚠ |  |  |

<a id="m-SupportDtos-AssignTicketRequest"></a>
#### SupportDtos.AssignTicketRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assignedTo` | texte |  |  |

<a id="m-SupportDtos-AttachmentResponse"></a>
#### SupportDtos.AttachmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `fileName` | texte |  |  |
| `contentType` | texte |  |  |
| `fileSize` | entier int64 ⚠ |  |  |
| `internal` | booléen |  |  |
| `uploadedBy` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SupportDtos-AddTicketMessageRequest"></a>
#### SupportDtos.AddTicketMessageRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `senderType` | enum : `CLIENT`, `AGENT`, `SYSTEM` |  |  |
| `senderId` | texte |  |  |
| `senderName` | texte |  |  |
| `content` | texte | oui | longueur min `1` |
| `internal` | booléen |  |  |
| `authorId` | texte |  |  |
| `authorName` | texte |  |  |

<a id="m-KnowledgeDtos-KnowledgeArticleResponse"></a>
#### KnowledgeDtos.KnowledgeArticleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `articleCode` | texte |  |  |
| `title` | texte |  |  |
| `slug` | texte |  |  |
| `body` | texte |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `tags` | texte[] |  |  |
| `publicVisible` | booléen |  |  |
| `internalOnly` | booléen |  |  |
| `active` | booléen |  |  |
| `viewCount` | entier int64 ⚠ |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `updatedBy` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-KnowledgeDtos-ConvertMessageToArticleRequest"></a>
#### KnowledgeDtos.ConvertMessageToArticleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `slug` | texte |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `tags` | texte[] |  |  |
| `publicVisible` | booléen |  |  |

<a id="m-SupportDtos-UpdateTicketStatusRequest"></a>
#### SupportDtos.UpdateTicketStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED` |  |  |

<a id="m-SupportDtos-AddTagRequest"></a>
#### SupportDtos.AddTagRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `tag` | texte | oui | longueur min `1` |

<a id="m-TaskDtos-TaskResponse"></a>
#### TaskDtos.TaskResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `recurrence` | enum : `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `AFTER_BOOKING` |  |  |
| `parentTaskId` | entier int64 ⚠ |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `checklist` | [TaskDtos.ChecklistResponse](#m-TaskDtos-ChecklistResponse)[] |  |  |
| `comments` | [TaskDtos.TaskCommentResponse](#m-TaskDtos-TaskCommentResponse)[] |  |  |

<a id="m-SupportDtos-CreateTicketTaskRequest"></a>
#### SupportDtos.CreateTicketTaskRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `checklist` | [TaskDtos.ChecklistRequest](#m-TaskDtos-ChecklistRequest)[] |  |  |

<a id="m-SupportDtos-TicketEventResponse"></a>
#### SupportDtos.TicketEventResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `eventType` | enum : `TICKET_CREATED`, `ASSIGNED`, `STATUS_CHANGED`, `PRIORITY_CHANGED`, `CATEGORY_CHANGED`, `MESSAGE_ADDED`, `SLA_ESCALATED`, `CSAT_SUBMITTED`, `REOPENED`, `MERGED`, `AUTO_CLOSED`, `TASK_CREATED` |  |  |
| `actorType` | texte |  |  |
| `actorId` | texte |  |  |
| `description` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SupportDtos-TicketMessageResponse"></a>
#### SupportDtos.TicketMessageResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `senderType` | enum : `CLIENT`, `AGENT`, `SYSTEM` |  |  |
| `senderId` | texte |  |  |
| `senderName` | texte |  |  |
| `content` | texte |  |  |
| `internal` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SupportDtos-AgentWorkloadEntry"></a>
#### SupportDtos.AgentWorkloadEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `agentId` | entier int64 ⚠ |  |  |
| `assigned` | entier int64 ⚠ |  |  |
| `resolved` | entier int64 ⚠ |  |  |

<a id="m-TaskDtos-ChecklistResponse"></a>
#### TaskDtos.ChecklistResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `label` | texte |  |  |
| `completed` | booléen |  |  |
| `displayOrder` | entier |  |  |

<a id="m-TaskDtos-TaskCommentResponse"></a>
#### TaskDtos.TaskCommentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `authorId` | texte |  |  |
| `authorName` | texte |  |  |
| `comment` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-TaskDtos-ChecklistRequest"></a>
#### TaskDtos.ChecklistRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `completed` | booléen |  |  |
| `displayOrder` | entier |  |  |
