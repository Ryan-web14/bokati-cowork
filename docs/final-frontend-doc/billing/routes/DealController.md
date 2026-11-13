# DealController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-deals"></a>
### GET /sni/api/v1/billing/deals

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<DealResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DealResponse](#m-DealResponse), [BillingDealStage](#m-BillingDealStage)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `stage` | requête | `BillingDealStage` |  |  |
| `owner` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `search` | requête | `String` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-post-sni-api-v1-billing-deals"></a>
### POST /sni/api/v1/billing/deals

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `DealRequest`
- Réponse : `DealResponse`
- Modèles : [DealResponse](#m-DealResponse), [DealRequest](#m-DealRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `create`

<a id="r-get-sni-api-v1-billing-deals-export"></a>
### GET /sni/api/v1/billing/deals/export

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `fichier (octets)`
- Modèles : [BillingDealStage](#m-BillingDealStage)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `export`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `stage` | requête | `BillingDealStage` |  |  |
| `owner` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `search` | requête | `String` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-post-sni-api-v1-billing-deals-from-opportunity-opportunitynumber"></a>
### POST /sni/api/v1/billing/deals/from-opportunity/{opportunityNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `DealResponse`
- Modèles : [DealResponse](#m-DealResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `fromOpportunity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `opportunityNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-deals-code"></a>
### GET /sni/api/v1/billing/deals/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `DealDashboardResponse`
- Modèles : [DealDashboardResponse](#m-DealDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-deals-code"></a>
### PUT /sni/api/v1/billing/deals/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `DealRequest`
- Réponse : `DealResponse`
- Modèles : [DealResponse](#m-DealResponse), [DealRequest](#m-DealRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-billing-deals-code-documents-documentnumber"></a>
### DELETE /sni/api/v1/billing/deals/{code}/documents/{documentNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `detach`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-deals-code-documents-documentnumber"></a>
### PATCH /sni/api/v1/billing/deals/{code}/documents/{documentNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `DealResponse`
- Modèles : [DealResponse](#m-DealResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `attach`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-deals-code-stage"></a>
### PATCH /sni/api/v1/billing/deals/{code}/stage

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `DealStageRequest`
- Réponse : `DealResponse`
- Modèles : [DealResponse](#m-DealResponse), [DealStageRequest](#m-DealStageRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DealController.java`, méthode `stage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DealResponse"></a>
#### DealResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `title` | texte |  |  |
| `owner` | texte |  |  |
| `ownerName` | texte |  |  |
| `expectedAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `probability` | entier |  |  |
| `stage` | texte |  |  |
| `lostReason` | texte |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `source` | texte |  |  |
| `crmOpportunityRef` | texte |  |  |
| `analyticSection` | texte |  |  |
| `notes` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BillingDealStage"></a>
#### BillingDealStage

Énumération : `PROSPECTING`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST`

<a id="m-DealRequest"></a>
#### DealRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  | longueur min `0`, longueur max `255` |
| `title` | texte | oui | longueur min `0`, longueur max `500` |
| `owner` | texte |  | longueur min `0`, longueur max `180` |
| `expectedAmount` | nombre décimal |  |  |
| `currency` | texte |  | longueur min `0`, longueur max `3` |
| `probability` | entier |  | min `0`, max `100` |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `analyticSection` | texte |  | longueur min `0`, longueur max `60` |
| `notes` | texte |  | longueur min `0`, longueur max `4000` |

<a id="m-DealDashboardResponse"></a>
#### DealDashboardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `deal` | [DealResponse](#m-DealResponse) |  |  |
| `quoted` | nombre décimal |  |  |
| `accepted` | nombre décimal |  |  |
| `invoiced` | nombre décimal |  |  |
| `creditNotes` | nombre décimal |  |  |
| `invoicedNet` | nombre décimal |  |  |
| `depositsInvoiced` | nombre décimal |  |  |
| `depositsPaid` | nombre décimal |  |  |
| `collected` | nombre décimal |  |  |
| `remainingToInvoice` | nombre décimal |  |  |
| `remainingToCollect` | nombre décimal |  |  |
| `plannedMargin` | nombre décimal |  |  |
| `realizedMargin` | nombre décimal |  |  |
| `costedShare` | nombre décimal |  |  |
| `documents` | [DealDashboardResponse.Document](#m-DealDashboardResponse-Document)[] |  |  |

<a id="m-DealStageRequest"></a>
#### DealStageRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stage` | enum : `PROSPECTING`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST` | oui |  |
| `lostReason` | texte |  | longueur min `0`, longueur max `1000` |

<a id="m-DealDashboardResponse-Document"></a>
#### DealDashboardResponse.Document

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | texte |  |  |
| `status` | texte |  |  |
| `deposit` | booléen |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
