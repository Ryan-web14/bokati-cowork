# ProposalController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [crm](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-crm-proposals"></a>
### GET /sni/api/v1/crm/proposals

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `PaginatedResponse<ProposalDtos.ProposalResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse), [ProposalStatus](#m-ProposalStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `ProposalStatus` |  |  |
| `opportunityId` | requête | `Long` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-crm-proposals"></a>
### POST /sni/api/v1/crm/proposals

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `ProposalDtos.CreateProposalRequest`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse), [ProposalDtos.CreateProposalRequest](#m-ProposalDtos-CreateProposalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `create`

<a id="r-get-sni-api-v1-crm-proposals-number-proposalnumber"></a>
### GET /sni/api/v1/crm/proposals/number/{proposalNumber}

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `getByNumber`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `proposalNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-crm-proposals-id"></a>
### GET /sni/api/v1/crm/proposals/{id}

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-crm-proposals-id"></a>
### PUT /sni/api/v1/crm/proposals/{id}

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `ProposalDtos.UpdateProposalRequest`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse), [ProposalDtos.UpdateProposalRequest](#m-ProposalDtos-UpdateProposalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-crm-proposals-id-accept"></a>
### PATCH /sni/api/v1/crm/proposals/{id}/accept

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-crm-proposals-id-lines"></a>
### POST /sni/api/v1/crm/proposals/{id}/lines

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `ProposalDtos.AddProposalLineRequest`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse), [ProposalDtos.AddProposalLineRequest](#m-ProposalDtos-AddProposalLineRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `addLine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-crm-proposals-id-lines-lineid"></a>
### DELETE /sni/api/v1/crm/proposals/{id}/lines/{lineId}

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `removeLine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `lineId` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-crm-proposals-id-lines-lineid"></a>
### PUT /sni/api/v1/crm/proposals/{id}/lines/{lineId}

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `ProposalDtos.UpdateProposalLineRequest`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse), [ProposalDtos.UpdateProposalLineRequest](#m-ProposalDtos-UpdateProposalLineRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `updateLine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `lineId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-crm-proposals-id-reject"></a>
### PATCH /sni/api/v1/crm/proposals/{id}/reject

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Corps JSON (obligatoire) : `ProposalDtos.RejectProposalRequest`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse), [ProposalDtos.RejectProposalRequest](#m-ProposalDtos-RejectProposalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-crm-proposals-id-send"></a>
### PATCH /sni/api/v1/crm/proposals/{id}/send

- Accès : back-office : rôle d'administration et droit `CRM:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:WRITE','CRM_WRITE')`
- Réponse : `ProposalDtos.ProposalResponse`
- Modèles : [ProposalDtos.ProposalResponse](#m-ProposalDtos-ProposalResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/ProposalController.java`, méthode `send`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ProposalDtos-ProposalResponse"></a>
#### ProposalDtos.ProposalResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `proposalNumber` | texte |  |  |
| `title` | texte |  |  |
| `opportunityId` | entier int64 ⚠ |  |  |
| `leadId` | entier int64 ⚠ |  |  |
| `recipientType` | texte |  |  |
| `recipientCode` | texte |  |  |
| `recipientName` | texte |  |  |
| `recipientEmail` | texte |  |  |
| `status` | enum : `DRAFT`, `SENT`, `VIEWED`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CONVERTED` |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `notes` | texte |  |  |
| `internalNotes` | texte |  |  |
| `convertedInvoiceNumber` | texte |  |  |
| `convertedContractCode` | texte |  |  |
| `convertedSubscriptionNumber` | texte |  |  |
| `convertedPassNumber` | texte |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `viewedAt` | date-heure ISO 8601 UTC |  |  |
| `acceptedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectionReason` | texte |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [ProposalDtos.ProposalLineResponse](#m-ProposalDtos-ProposalLineResponse)[] |  |  |

<a id="m-ProposalStatus"></a>
#### ProposalStatus

Énumération : `DRAFT`, `SENT`, `VIEWED`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CONVERTED`

<a id="m-ProposalDtos-CreateProposalRequest"></a>
#### ProposalDtos.CreateProposalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `opportunityId` | entier int64 ⚠ |  |  |
| `leadId` | entier int64 ⚠ |  |  |
| `recipientType` | texte |  |  |
| `recipientCode` | texte |  |  |
| `recipientName` | texte |  |  |
| `recipientEmail` | texte |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `currency` | texte |  |  |
| `notes` | texte |  |  |
| `internalNotes` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |

<a id="m-ProposalDtos-UpdateProposalRequest"></a>
#### ProposalDtos.UpdateProposalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `recipientName` | texte |  |  |
| `recipientEmail` | texte |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `notes` | texte |  |  |
| `internalNotes` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |

<a id="m-ProposalDtos-AddProposalLineRequest"></a>
#### ProposalDtos.AddProposalLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `serviceType` | texte |  |  |
| `serviceRefCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal | oui |  |
| `discountPercent` | nombre décimal |  |  |
| `taxRate` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `currency` | texte |  |  |

<a id="m-ProposalDtos-UpdateProposalLineRequest"></a>
#### ProposalDtos.UpdateProposalLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `discountPercent` | nombre décimal |  |  |
| `taxRate` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `sortOrder` | entier |  |  |

<a id="m-ProposalDtos-RejectProposalRequest"></a>
#### ProposalDtos.RejectProposalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-ProposalDtos-ProposalLineResponse"></a>
#### ProposalDtos.ProposalLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `sortOrder` | entier |  |  |
| `serviceType` | texte |  |  |
| `serviceRefCode` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `discountPercent` | nombre décimal |  |  |
| `taxRate` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
