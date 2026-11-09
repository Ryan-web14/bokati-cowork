# ClientSupportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-client-support-tickets"></a>
### GET /sni/api/v1/client/support/tickets

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<SupportDtos.SupportTicketResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [TicketStatus](#m-TicketStatus), [TicketCategory](#m-TicketCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `TicketStatus` |  |  |
| `category` | requête | `TicketCategory` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-client-support-tickets"></a>
### POST /sni/api/v1/client/support/tickets

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientCreateTicketRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [ClientCreateTicketRequest](#m-ClientCreateTicketRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `create`

<a id="r-get-sni-api-v1-client-support-tickets-summary"></a>
### GET /sni/api/v1/client/support/tickets/summary

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `SupportDtos.OwnerTicketSummaryResponse`
- Modèles : [SupportDtos.OwnerTicketSummaryResponse](#m-SupportDtos-OwnerTicketSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `summary`

<a id="r-get-sni-api-v1-client-support-tickets-ticketnumber"></a>
### GET /sni/api/v1/client/support/tickets/{ticketNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-support-tickets-ticketnumber-attachments"></a>
### GET /sni/api/v1/client/support/tickets/{ticketNumber}/attachments

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<SupportDtos.AttachmentResponse>`
- Modèles : [SupportDtos.AttachmentResponse](#m-SupportDtos-AttachmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `listAttachments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-support-tickets-ticketnumber-attachments"></a>
### POST /sni/api/v1/client/support/tickets/{ticketNumber}/attachments

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `SupportDtos.AttachmentResponse`
- Modèles : [SupportDtos.AttachmentResponse](#m-SupportDtos-AttachmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `uploadAttachment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-client-support-tickets-ticketnumber-attachments-id-download"></a>
### GET /sni/api/v1/client/support/tickets/{ticketNumber}/attachments/{id}/download

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `downloadAttachment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-support-tickets-ticketnumber-close"></a>
### POST /sni/api/v1/client/support/tickets/{ticketNumber}/close

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `close`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-support-tickets-ticketnumber-csat"></a>
### POST /sni/api/v1/client/support/tickets/{ticketNumber}/csat

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SupportDtos.SubmitCsatRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [SupportDtos.SubmitCsatRequest](#m-SupportDtos-SubmitCsatRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `csat`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticketNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-support-tickets-ticketnumber-messages"></a>
### POST /sni/api/v1/client/support/tickets/{ticketNumber}/messages

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientAddMessageRequest`
- Réponse : `SupportDtos.SupportTicketResponse`
- Modèles : [SupportDtos.SupportTicketResponse](#m-SupportDtos-SupportTicketResponse), [ClientAddMessageRequest](#m-ClientAddMessageRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientSupportController.java`, méthode `addMessage`

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
| `assignedTo` | texte |  | nombre envoyé en texte |
| `assignedToEmail` | texte |  |  |
| `assignedToName` | texte |  |  |
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

<a id="m-ClientCreateTicketRequest"></a>
#### ClientCreateTicketRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `0`, longueur max `255` |
| `description` | texte |  | longueur min `0`, longueur max `5000` |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `relatedType` | texte |  |  |
| `relatedCode` | texte |  |  |

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

<a id="m-SupportDtos-SubmitCsatRequest"></a>
#### SupportDtos.SubmitCsatRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `score` | entier | oui | min `1`, max `5` |
| `comment` | texte |  |  |

<a id="m-ClientAddMessageRequest"></a>
#### ClientAddMessageRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `content` | texte | oui | longueur min `0`, longueur max `10000` |

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
