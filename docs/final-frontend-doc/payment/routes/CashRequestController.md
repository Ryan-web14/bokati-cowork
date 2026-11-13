# CashRequestController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-requests"></a>
### GET /sni/api/v1/cash-registers/requests

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashRequestResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashRequestResponse](#m-CashRequestResponse), [CashRequestType](#m-CashRequestType), [CashRequestStatus](#m-CashRequestStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRequestController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |
| `sessionNumber` | requête | `String` |  |  |
| `requestType` | requête | `CashRequestType` |  |  |
| `status` | requête | `CashRequestStatus` |  |  |
| `requestedBy` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=requestedAt,DESC |

<a id="r-get-sni-api-v1-cash-registers-requests-requestnumber"></a>
### GET /sni/api/v1/cash-registers/requests/{requestNumber}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashRequestResponse`
- Modèles : [CashRequestResponse](#m-CashRequestResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRequestController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-requests-requestnumber-approve"></a>
### PATCH /sni/api/v1/cash-registers/requests/{requestNumber}/approve

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:APPROVE`
- Corps JSON (obligatoire) : `ReviewCashRequestRequest`
- Réponse : `CashRequestResponse`
- Modèles : [CashRequestResponse](#m-CashRequestResponse), [ReviewCashRequestRequest](#m-ReviewCashRequestRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRequestController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-requests-requestnumber-reject"></a>
### PATCH /sni/api/v1/cash-registers/requests/{requestNumber}/reject

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:APPROVE`
- Corps JSON (obligatoire) : `ReviewCashRequestRequest`
- Réponse : `CashRequestResponse`
- Modèles : [CashRequestResponse](#m-CashRequestResponse), [ReviewCashRequestRequest](#m-ReviewCashRequestRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRequestController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-requests"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/requests

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CreateCashRequestRequest`
- Réponse : `CashRequestResponse`
- Modèles : [CashRequestResponse](#m-CashRequestResponse), [CreateCashRequestRequest](#m-CreateCashRequestRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRequestController.java`, méthode `submit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashRequestResponse"></a>
#### CashRequestResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requestNumber` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `requestType` | enum : `JUSTIFICATIF`, `REMISE_DE_FONDS`, `CASH_ADVANCE`, `EXPENSE_REIMBURSEMENT` |  |  |
| `status` | enum : `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`, `CANCELLED` |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `reason` | texte |  |  |
| `requestedBy` | texte |  |  |
| `requestedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewedBy` | texte |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewNote` | texte |  |  |
| `executedMovementNumber` | texte |  |  |
| `executedAt` | date-heure ISO 8601 UTC |  |  |
| `attachments` | [CashRequestResponse.AttachmentRef](#m-CashRequestResponse-AttachmentRef)[] |  |  |

<a id="m-CashRequestType"></a>
#### CashRequestType

Énumération : `JUSTIFICATIF`, `REMISE_DE_FONDS`, `CASH_ADVANCE`, `EXPENSE_REIMBURSEMENT`

<a id="m-CashRequestStatus"></a>
#### CashRequestStatus

Énumération : `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`, `CANCELLED`

<a id="m-ReviewCashRequestRequest"></a>
#### ReviewCashRequestRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewedBy` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CreateCashRequestRequest"></a>
#### CreateCashRequestRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requestType` | enum : `JUSTIFICATIF`, `REMISE_DE_FONDS`, `CASH_ADVANCE`, `EXPENSE_REIMBURSEMENT` | oui |  |
| `amount` | nombre décimal | oui |  |
| `currency` | texte |  |  |
| `reason` | texte |  |  |
| `requestedBy` | texte |  |  |
| `attachments` | [CreateCashRequestRequest.CashRequestAttachmentInput](#m-CreateCashRequestRequest-CashRequestAttachmentInput)[] |  |  |

<a id="m-CashRequestResponse-AttachmentRef"></a>
#### CashRequestResponse.AttachmentRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fileName` | texte |  |  |
| `contentType` | texte |  |  |
| `storagePath` | texte |  |  |
| `label` | texte |  |  |

<a id="m-CreateCashRequestRequest-CashRequestAttachmentInput"></a>
#### CreateCashRequestRequest.CashRequestAttachmentInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fileName` | texte |  |  |
| `contentType` | texte |  |  |
| `storagePath` | texte |  |  |
| `label` | texte |  |  |
