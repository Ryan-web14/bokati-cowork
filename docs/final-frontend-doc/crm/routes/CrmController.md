# CrmController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [crm](../README.md) · 17 route(s).

## Routes

<a id="r-get-sni-api-v1-crm-analytics"></a>
### GET /sni/api/v1/crm/analytics

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `CrmDtos.CrmAnalyticsResponse`
- Modèles : [CrmDtos.CrmAnalyticsResponse](#m-CrmDtos-CrmAnalyticsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `analytics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-crm-board"></a>
### GET /sni/api/v1/crm/board

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `CrmDtos.KanbanBoardResponse`
- Modèles : [CrmDtos.KanbanBoardResponse](#m-CrmDtos-KanbanBoardResponse), [LeadSource](#m-LeadSource), [LeadInterest](#m-LeadInterest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `board`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assignedTo` | requête | `Long` |  |  |
| `source` | requête | `LeadSource` |  |  |
| `interest` | requête | `LeadInterest` |  |  |
| `searchText` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-crm-leads"></a>
### GET /sni/api/v1/crm/leads

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `PaginatedResponse<CrmDtos.LeadResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse), [LeadStage](#m-LeadStage), [LeadSource](#m-LeadSource)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `stage` | requête | `LeadStage` |  |  |
| `assignedTo` | requête | `Long` |  |  |
| `source` | requête | `LeadSource` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-crm-leads"></a>
### POST /sni/api/v1/crm/leads

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.CreateLeadRequest`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse), [CrmDtos.CreateLeadRequest](#m-CrmDtos-CreateLeadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `create`

<a id="r-get-sni-api-v1-crm-leads-number-leadnumber"></a>
### GET /sni/api/v1/crm/leads/number/{leadNumber}

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `getByNumber`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `leadNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-crm-leads-id"></a>
### GET /sni/api/v1/crm/leads/{id}

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-crm-leads-id"></a>
### PUT /sni/api/v1/crm/leads/{id}

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.UpdateLeadRequest`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse), [CrmDtos.UpdateLeadRequest](#m-CrmDtos-UpdateLeadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-crm-leads-id-activities"></a>
### POST /sni/api/v1/crm/leads/{id}/activities

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.AddLeadActivityRequest`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse), [CrmDtos.AddLeadActivityRequest](#m-CrmDtos-AddLeadActivityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `activity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-crm-leads-id-convert"></a>
### PATCH /sni/api/v1/crm/leads/{id}/convert

- Accès : back-office : rôle d'administration et droit `CRM:CONVERT` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')`
- Corps JSON (obligatoire) : `CrmDtos.ConvertLeadRequest`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse), [CrmDtos.ConvertLeadRequest](#m-CrmDtos-ConvertLeadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `convert`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-crm-leads-id-generate-quote"></a>
### POST /sni/api/v1/crm/leads/{id}/generate-quote

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.GenerateQuoteRequest`
- Réponse : `Map<String, String>`
- Modèles : [CrmDtos.GenerateQuoteRequest](#m-CrmDtos-GenerateQuoteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `generateQuote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-crm-leads-id-opportunities"></a>
### POST /sni/api/v1/crm/leads/{id}/opportunities

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.CreateOpportunityRequest`
- Réponse : `CrmDtos.OpportunityResponse`
- Modèles : [CrmDtos.OpportunityResponse](#m-CrmDtos-OpportunityResponse), [CrmDtos.CreateOpportunityRequest](#m-CrmDtos-CreateOpportunityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `createOpportunity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-crm-leads-id-qualify"></a>
### PATCH /sni/api/v1/crm/leads/{id}/qualify

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `qualify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-crm-leads-id-stage"></a>
### PATCH /sni/api/v1/crm/leads/{id}/stage

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.UpdateLeadStageRequest`
- Réponse : `CrmDtos.LeadResponse`
- Modèles : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse), [CrmDtos.UpdateLeadStageRequest](#m-CrmDtos-UpdateLeadStageRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `stage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-crm-metrics"></a>
### GET /sni/api/v1/crm/metrics

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `CrmDtos.CrmMetricsResponse`
- Modèles : [CrmDtos.CrmMetricsResponse](#m-CrmDtos-CrmMetricsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `metrics`

<a id="r-get-sni-api-v1-crm-opportunities"></a>
### GET /sni/api/v1/crm/opportunities

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `PaginatedResponse<CrmDtos.OpportunityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CrmDtos.OpportunityResponse](#m-CrmDtos-OpportunityResponse), [OpportunityStage](#m-OpportunityStage)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `opportunities`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `stage` | requête | `OpportunityStage` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-patch-sni-api-v1-crm-opportunities-id-stage"></a>
### PATCH /sni/api/v1/crm/opportunities/{id}/stage

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `CrmDtos.UpdateOpportunityStageRequest`
- Réponse : `CrmDtos.OpportunityResponse`
- Modèles : [CrmDtos.OpportunityResponse](#m-CrmDtos-OpportunityResponse), [CrmDtos.UpdateOpportunityStageRequest](#m-CrmDtos-UpdateOpportunityStageRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `opportunityStage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-crm-pipeline"></a>
### GET /sni/api/v1/crm/pipeline

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `CrmDtos.PipelineResponse`
- Modèles : [CrmDtos.PipelineResponse](#m-CrmDtos-PipelineResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java`, méthode `pipeline`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CrmDtos-CrmAnalyticsResponse"></a>
#### CrmDtos.CrmAnalyticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date-heure ISO 8601 UTC |  |  |
| `to` | date-heure ISO 8601 UTC |  |  |
| `totalCreated` | entier int64 ⚠ |  |  |
| `totalWon` | entier int64 ⚠ |  |  |
| `totalLost` | entier int64 ⚠ |  |  |
| `conversionRate` | nombre décimal |  |  |
| `avgDaysToWin` | nombre décimal |  |  |
| `bySource` | objet { clé : entier int64 ⚠ } |  |  |
| `byStage` | objet { clé : entier int64 ⚠ } |  |  |
| `pipelineValue` | nombre décimal |  |  |

<a id="m-CrmDtos-KanbanBoardResponse"></a>
#### CrmDtos.KanbanBoardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `columns` | [CrmDtos.KanbanColumnResponse](#m-CrmDtos-KanbanColumnResponse)[] |  |  |
| `totalLeads` | entier int64 ⚠ |  |  |
| `totalPipelineValue` | nombre décimal |  |  |

<a id="m-LeadSource"></a>
#### LeadSource

Énumération : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER`

<a id="m-LeadInterest"></a>
#### LeadInterest

Énumération : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER`

<a id="m-CrmDtos-LeadResponse"></a>
#### CrmDtos.LeadResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `leadNumber` | texte |  |  |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `company` | texte |  |  |
| `note` | texte |  |  |
| `source` | enum : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER` |  |  |
| `interest` | enum : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER` |  |  |
| `stage` | enum : `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL_SENT`, `WON`, `LOST` |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `scoredProbability` | entier |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `convertedOwnerType` | texte |  |  |
| `convertedOwnerCode` | texte |  |  |
| `lostReason` | texte |  |  |
| `lastActivityAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `activities` | [CrmDtos.LeadActivityResponse](#m-CrmDtos-LeadActivityResponse)[] |  |  |

<a id="m-LeadStage"></a>
#### LeadStage

Énumération : `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL_SENT`, `WON`, `LOST`

<a id="m-CrmDtos-CreateLeadRequest"></a>
#### CrmDtos.CreateLeadRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fullName` | texte | oui | longueur min `1` |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `company` | texte |  |  |
| `note` | texte |  |  |
| `source` | enum : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER` |  |  |
| `interest` | enum : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER` |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |

<a id="m-CrmDtos-UpdateLeadRequest"></a>
#### CrmDtos.UpdateLeadRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `company` | texte |  |  |
| `note` | texte |  |  |
| `source` | enum : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER` |  |  |
| `interest` | enum : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER` |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |

<a id="m-CrmDtos-AddLeadActivityRequest"></a>
#### CrmDtos.AddLeadActivityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `activityType` | enum : `CALL`, `EMAIL`, `MEETING`, `VISIT`, `NOTE` | oui |  |
| `subject` | texte |  |  |
| `notes` | texte |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CrmDtos-ConvertLeadRequest"></a>
#### CrmDtos.ConvertLeadRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |

<a id="m-CrmDtos-GenerateQuoteRequest"></a>
#### CrmDtos.GenerateQuoteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte | oui | longueur min `1` |
| `performedBy` | texte |  |  |

<a id="m-CrmDtos-OpportunityResponse"></a>
#### CrmDtos.OpportunityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `opportunityNumber` | texte |  |  |
| `leadId` | entier int64 ⚠ |  |  |
| `leadNumber` | texte |  |  |
| `leadFullName` | texte |  |  |
| `title` | texte |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `stage` | enum : `OPEN`, `NEGOTIATION`, `PROPOSAL_SENT`, `WON`, `LOST` |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `notes` | texte |  |  |
| `wonAt` | date-heure ISO 8601 UTC |  |  |
| `lostAt` | date-heure ISO 8601 UTC |  |  |
| `lostReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CrmDtos-CreateOpportunityRequest"></a>
#### CrmDtos.CreateOpportunityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `notes` | texte |  |  |

<a id="m-CrmDtos-UpdateLeadStageRequest"></a>
#### CrmDtos.UpdateLeadStageRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stage` | enum : `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL_SENT`, `WON`, `LOST` |  |  |
| `lostReason` | texte |  |  |

<a id="m-CrmDtos-CrmMetricsResponse"></a>
#### CrmDtos.CrmMetricsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `totalLeads` | entier int64 ⚠ |  |  |
| `newLeads` | entier int64 ⚠ |  |  |
| `wonLeads` | entier int64 ⚠ |  |  |
| `lostLeads` | entier int64 ⚠ |  |  |
| `activeLeads` | entier int64 ⚠ |  |  |
| `pipelineValue` | nombre décimal |  |  |
| `conversionRate` | nombre décimal |  |  |

<a id="m-OpportunityStage"></a>
#### OpportunityStage

Énumération : `OPEN`, `NEGOTIATION`, `PROPOSAL_SENT`, `WON`, `LOST`

<a id="m-CrmDtos-UpdateOpportunityStageRequest"></a>
#### CrmDtos.UpdateOpportunityStageRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stage` | enum : `OPEN`, `NEGOTIATION`, `PROPOSAL_SENT`, `WON`, `LOST` | oui |  |
| `lostReason` | texte |  |  |

<a id="m-CrmDtos-PipelineResponse"></a>
#### CrmDtos.PipelineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stages` | objet { clé : [CrmDtos.LeadResponse](#m-CrmDtos-LeadResponse)[] } |  |  |

<a id="m-CrmDtos-KanbanColumnResponse"></a>
#### CrmDtos.KanbanColumnResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stage` | enum : `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL_SENT`, `WON`, `LOST` |  |  |
| `count` | entier int64 ⚠ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `cards` | [CrmDtos.KanbanCardResponse](#m-CrmDtos-KanbanCardResponse)[] |  |  |

<a id="m-CrmDtos-LeadActivityResponse"></a>
#### CrmDtos.LeadActivityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `activityType` | enum : `CALL`, `EMAIL`, `MEETING`, `VISIT`, `NOTE` |  |  |
| `subject` | texte |  |  |
| `notes` | texte |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CrmDtos-KanbanCardResponse"></a>
#### CrmDtos.KanbanCardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `leadNumber` | texte |  |  |
| `fullName` | texte |  |  |
| `company` | texte |  |  |
| `email` | texte |  |  |
| `stage` | enum : `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL_SENT`, `WON`, `LOST` |  |  |
| `source` | enum : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER` |  |  |
| `interest` | enum : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER` |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `score` | entier |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `lastActivityAt` | date-heure ISO 8601 UTC |  |  |
| `activityCount` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
