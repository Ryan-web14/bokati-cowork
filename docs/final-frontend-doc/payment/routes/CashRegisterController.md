# CashRegisterController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 22 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers"></a>
### GET /sni/api/v1/cash-registers

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashRegisterResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashRegisterResponse](#m-CashRegisterResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `listRegisters`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |
| `locationCode` | requête | `String` |  |  |
| `businessEntityCode` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-cash-registers"></a>
### POST /sni/api/v1/cash-registers

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (obligatoire) : `CreateCashRegisterRequest`
- Réponse : `CashRegisterResponse`
- Modèles : [CashRegisterResponse](#m-CashRegisterResponse), [CreateCashRegisterRequest](#m-CreateCashRegisterRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `createRegister`

<a id="r-get-sni-api-v1-cash-registers-metrics-overview"></a>
### GET /sni/api/v1/cash-registers/metrics/overview

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashMetricsOverviewResponse`
- Modèles : [CashMetricsOverviewResponse](#m-CashMetricsOverviewResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `overviewMetrics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |
| `businessEntityCode` | requête | `String` |  |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-cash-registers-metrics-registers"></a>
### GET /sni/api/v1/cash-registers/metrics/registers

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashRegisterMetricsResponse>`
- Modèles : [CashRegisterMetricsResponse](#m-CashRegisterMetricsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `registerMetrics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `businessEntityCode` | requête | `String` |  |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-cash-registers-movements"></a>
### GET /sni/api/v1/cash-registers/movements

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashMovementResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashMovementResponse](#m-CashMovementResponse), [CashMovementType](#m-CashMovementType), [CashDocumentType](#m-CashDocumentType)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `listMovements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |
| `sessionNumber` | requête | `String` |  |  |
| `movementType` | requête | `CashMovementType` |  |  |
| `documentType` | requête | `CashDocumentType` |  |  |
| `documentNumber` | requête | `String` |  |  |
| `flowCategory` | requête | `String` |  |  |
| `referenceType` | requête | `String` |  |  |
| `referenceCode` | requête | `String` |  |  |
| `counterpartyCode` | requête | `String` |  |  |
| `counterpartyName` | requête | `String` |  |  |
| `createdBy` | requête | `String` |  |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-cash-registers-movements-movementnumber"></a>
### GET /sni/api/v1/cash-registers/movements/{movementNumber}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `getMovement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-movements-movementnumber-attachments"></a>
### POST /sni/api/v1/cash-registers/movements/{movementNumber}/attachments

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `AddCashMovementAttachmentRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [AddCashMovementAttachmentRequest](#m-AddCashMovementAttachmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `addAttachment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-movements-movementnumber-print"></a>
### PATCH /sni/api/v1/cash-registers/movements/{movementNumber}/print

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `markPrinted`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-movements-movementnumber-sign"></a>
### PATCH /sni/api/v1/cash-registers/movements/{movementNumber}/sign

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `signMovement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |
| `signedBy` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions"></a>
### GET /sni/api/v1/cash-registers/sessions

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashSessionResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashSessionResponse](#m-CashSessionResponse), [CashSessionStatus](#m-CashSessionStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `listSessions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |
| `status` | requête | `CashSessionStatus` |  |  |
| `openedBy` | requête | `String` |  |  |
| `closedBy` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=openedAt,DESC |

<a id="r-post-sni-api-v1-cash-registers-sessions"></a>
### POST /sni/api/v1/cash-registers/sessions

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:OPEN`
- Corps JSON (obligatoire) : `OpenCashSessionRequest`
- Réponse : `CashSessionResponse`
- Modèles : [CashSessionResponse](#m-CashSessionResponse), [OpenCashSessionRequest](#m-OpenCashSessionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `openSession`

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-approve-variance"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/approve-variance

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (obligatoire) : `ApproveCashVarianceRequest`
- Réponse : `CashSessionResponse`
- Modèles : [CashSessionResponse](#m-CashSessionResponse), [ApproveCashVarianceRequest](#m-ApproveCashVarianceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `approveVariance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-close"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/close

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (obligatoire) : `CloseCashSessionRequest`
- Réponse : `CashSessionResponse`
- Modèles : [CashSessionResponse](#m-CashSessionResponse), [CloseCashSessionRequest](#m-CloseCashSessionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `closeSession`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-closing-request"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/closing-request

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (obligatoire) : `CloseCashSessionRequest`
- Réponse : `CashSessionResponse`
- Modèles : [CashSessionResponse](#m-CashSessionResponse), [CloseCashSessionRequest](#m-CloseCashSessionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `requestClosing`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-entry-vouchers"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/entry-vouchers

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashMovementResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashMovementResponse](#m-CashMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `listEntryVouchers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |
| `documentNumber` | requête | `String` |  |  |
| `flowCategory` | requête | `String` |  |  |
| `referenceType` | requête | `String` |  |  |
| `referenceCode` | requête | `String` |  |  |
| `counterpartyCode` | requête | `String` |  |  |
| `counterpartyName` | requête | `String` |  |  |
| `createdBy` | requête | `String` |  |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-entry-vouchers"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/entry-vouchers

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CreateCashVoucherRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [CreateCashVoucherRequest](#m-CreateCashVoucherRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `createEntryVoucher`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-exit-vouchers"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/exit-vouchers

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashMovementResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashMovementResponse](#m-CashMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `listExitVouchers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |
| `documentNumber` | requête | `String` |  |  |
| `flowCategory` | requête | `String` |  |  |
| `referenceType` | requête | `String` |  |  |
| `referenceCode` | requête | `String` |  |  |
| `counterpartyCode` | requête | `String` |  |  |
| `counterpartyName` | requête | `String` |  |  |
| `createdBy` | requête | `String` |  |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-exit-vouchers"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/exit-vouchers

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CreateCashVoucherRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [CreateCashVoucherRequest](#m-CreateCashVoucherRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `createExitVoucher`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-movements"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/movements

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CreateCashMovementRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [CreateCashMovementRequest](#m-CreateCashMovementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `createMovement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-summary"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/summary

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashSessionSummaryResponse`
- Modèles : [CashSessionSummaryResponse](#m-CashSessionSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `sessionSummary`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode-activate"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}/activate

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Réponse : `CashRegisterResponse`
- Modèles : [CashRegisterResponse](#m-CashRegisterResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `activateRegister`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode-deactivate"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}/deactivate

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Réponse : `CashRegisterResponse`
- Modèles : [CashRegisterResponse](#m-CashRegisterResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java`, méthode `deactivateRegister`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashRegisterResponse"></a>
#### CashRegisterResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `name` | texte |  |  |
| `locationCode` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `deviceCode` | texte |  |  |
| `active` | booléen |  |  |
| `cashControlEnabled` | booléen |  |  |
| `systemManaged` | booléen |  |  |
| `restrictedToMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
| `maxCashAmount` | nombre décimal |  |  |
| `managerEmail` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `registerType` | enum : `COUNTER`, `MAIN`, `PETTY_CASH`, `SAFE`, `AUTOMATIC` |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `CLOSED` |  |  |
| `parentRegisterCode` | texte |  |  |
| `lastCountAt` | date-heure ISO 8601 UTC |  |  |
| `lastCountAmount` | nombre décimal |  |  |

<a id="m-CreateCashRegisterRequest"></a>
#### CreateCashRegisterRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `locationCode` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `deviceCode` | texte |  |  |
| `cashControlEnabled` | booléen |  |  |
| `maxCashAmount` | nombre décimal |  |  |
| `managerEmail` | texte |  |  |
| `registerType` | enum : `COUNTER`, `MAIN`, `PETTY_CASH`, `SAFE`, `AUTOMATIC` |  |  |
| `parentRegisterCode` | texte |  |  |
| `acceptedMethods` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE`[] |  |  |
| `acceptedCurrencies` | texte[] |  |  |
| `capBehavior` | enum : `ALERT`, `BLOCK` |  |  |
| `maxOutPerOperation` | nombre décimal |  |  |
| `maxOutPerDay` | nombre décimal |  |  |
| `standardFloat` | nombre décimal |  |  |
| `alertRecipients` | texte[] |  |  |
| `holderEmail` | texte |  |  |
| `backupEmails` | texte[] |  |  |
| `schedules` | [CashRegisterScheduleRequest](#m-CashRegisterScheduleRequest)[] |  |  |

<a id="m-CashMetricsOverviewResponse"></a>
#### CashMetricsOverviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `registerCount` | entier int64 ⚠ |  |  |
| `activeRegisterCount` | entier int64 ⚠ |  |  |
| `openSessionCount` | entier int64 ⚠ |  |  |
| `closingReviewSessionCount` | entier int64 ⚠ |  |  |
| `closedSessionCount` | entier int64 ⚠ |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `openingFloatAmount` | nombre décimal |  |  |
| `totalPayments` | nombre décimal |  |  |
| `totalRefunds` | nombre décimal |  |  |
| `totalCashIn` | nombre décimal |  |  |
| `totalCashOut` | nombre décimal |  |  |
| `totalAdjustments` | nombre décimal |  |  |
| `netCashPosition` | nombre décimal |  |  |
| `pendingVarianceAmount` | nombre décimal |  |  |

<a id="m-CashRegisterMetricsResponse"></a>
#### CashRegisterMetricsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `registerName` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `deviceCode` | texte |  |  |
| `openSessionCount` | entier int64 ⚠ |  |  |
| `closingReviewSessionCount` | entier int64 ⚠ |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `totalPayments` | nombre décimal |  |  |
| `totalRefunds` | nombre décimal |  |  |
| `totalCashIn` | nombre décimal |  |  |
| `totalCashOut` | nombre décimal |  |  |
| `totalAdjustments` | nombre décimal |  |  |
| `netCashPosition` | nombre décimal |  |  |

<a id="m-CashMovementResponse"></a>
#### CashMovementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementNumber` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `registerName` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `deviceCode` | texte |  |  |
| `sessionStatus` | enum : `OPENING`, `OPEN`, `PAUSED`, `SUSPENDED`, `COUNTING`, `CLOSING_REVIEW`, `RECOUNT`, `CLOSED`, `CANCELLED` |  |  |
| `sessionOpenedBy` | texte |  |  |
| `sessionOpenedAt` | date-heure ISO 8601 UTC |  |  |
| `movementType` | enum : `OPENING_FLOAT`, `PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `CLOSING_COUNT`, `EXCHANGE`, `VARIANCE` |  |  |
| `flowDirection` | enum : `IN`, `OUT`, `ADJUSTMENT` |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `documentType` | enum : `CASH_VOUCHER`, `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `RECEIPT`, `INVOICE`, `EXPENSE_NOTE`, `BANK_SLIP`, `TRANSFER_NOTE`, `EXTERNAL_REFERENCE`, `OTHER` |  |  |
| `documentNumber` | texte |  |  |
| `flowCategory` | texte |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `counterpartyType` | texte |  |  |
| `counterpartyCode` | texte |  |  |
| `counterpartyName` | texte |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `relatedTransactionNumber` | texte |  |  |
| `relatedIntentNumber` | texte |  |  |
| `relatedReceiptNumber` | texte |  |  |
| `relatedCustomerType` | texte |  |  |
| `relatedCustomerCode` | texte |  |  |
| `relatedCustomerName` | texte |  |  |
| `relatedSourceType` | texte |  |  |
| `relatedSourceCode` | texte |  |  |
| `relatedSourceLabel` | texte |  |  |
| `metadataJson` | texte |  |  |
| `status` | enum : `PENDING`, `CONFIRMED`, `REVERSED`, `CANCELLED` |  |  |
| `relatedMovementNumber` | texte |  |  |
| `batchId` | texte |  |  |
| `runningBalance` | nombre décimal |  |  |
| `exchangeRate` | nombre décimal |  |  |
| `channel` | enum : `MANUAL`, `AUTO_PAYMENT`, `IMPORT`, `API`, `POS` |  |  |
| `movementDeviceCode` | texte |  |  |
| `deviceIp` | texte |  |  |
| `subCategory` | texte |  |  |
| `tags` | texte |  |  |
| `riskScore` | nombre décimal |  |  |
| `requiresSignature` | booléen |  |  |
| `signedBy` | texte |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `printedAt` | date-heure ISO 8601 UTC |  |  |
| `attachments` | [CashMovementAttachmentResponse](#m-CashMovementAttachmentResponse)[] |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `natureCode` | texte |  |  |
| `transferOrderNumber` | texte |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `reversalReason` | texte |  |  |
| `cancelReason` | texte |  |  |
| `accounting` | [CashMovementResponse.Accounting](#m-CashMovementResponse-Accounting) |  |  |

<a id="m-CashMovementType"></a>
#### CashMovementType

Énumération : `OPENING_FLOAT`, `PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `CLOSING_COUNT`, `EXCHANGE`, `VARIANCE`

<a id="m-CashDocumentType"></a>
#### CashDocumentType

Énumération : `CASH_VOUCHER`, `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `RECEIPT`, `INVOICE`, `EXPENSE_NOTE`, `BANK_SLIP`, `TRANSFER_NOTE`, `EXTERNAL_REFERENCE`, `OTHER`

<a id="m-AddCashMovementAttachmentRequest"></a>
#### AddCashMovementAttachmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fileName` | texte | oui | longueur min `1` |
| `contentType` | texte |  |  |
| `storagePath` | texte | oui | longueur min `1` |
| `label` | texte |  |  |
| `uploadedBy` | texte |  |  |

<a id="m-CashSessionResponse"></a>
#### CashSessionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `status` | enum : `OPENING`, `OPEN`, `PAUSED`, `SUSPENDED`, `COUNTING`, `CLOSING_REVIEW`, `RECOUNT`, `CLOSED`, `CANCELLED` |  |  |
| `openedBy` | texte |  |  |
| `closedBy` | texte |  |  |
| `reviewedBy` | texte |  |  |
| `openingAmount` | nombre décimal |  |  |
| `closingAmount` | nombre décimal |  |  |
| `expectedClosingAmount` | nombre décimal |  |  |
| `countedClosingAmount` | nombre décimal |  |  |
| `varianceAmount` | nombre décimal |  |  |
| `varianceReason` | texte |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `closingRequestedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `expectedOpeningAmount` | nombre décimal |  |  |
| `openingVariance` | nombre décimal |  |  |
| `openingVarianceReason` | texte |  |  |
| `pausedAt` | date-heure ISO 8601 UTC |  |  |
| `pauseSeconds` | entier int64 ⚠ |  |  |
| `suspendedBy` | texte |  |  |
| `suspensionReason` | texte |  |  |
| `keptFloat` | nombre décimal |  |  |
| `amountToRemit` | nombre décimal |  |  |
| `closingCountNumber` | texte |  |  |
| `varianceDecision` | enum : `EXPENSE`, `INCOME`, `CASHIER`, `INVESTIGATION`, `TOLERATED` |  |  |
| `varianceDecidedBy` | texte |  |  |
| `forcedClose` | booléen |  |  |
| `handoverNumber` | texte |  |  |
| `outOfHoursReason` | texte |  |  |
| `expectedHidden` | booléen |  |  |

<a id="m-CashSessionStatus"></a>
#### CashSessionStatus

Énumération : `OPENING`, `OPEN`, `PAUSED`, `SUSPENDED`, `COUNTING`, `CLOSING_REVIEW`, `RECOUNT`, `CLOSED`, `CANCELLED`

<a id="m-OpenCashSessionRequest"></a>
#### OpenCashSessionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte | oui | longueur min `1` |
| `openedBy` | texte |  |  |
| `openingAmount` | nombre décimal |  |  |
| `deviceCode` | texte |  |  |
| `outOfHoursReasonCode` | texte |  |  |
| `outOfHoursReasonText` | texte |  |  |
| `openingCount` | [CashCountRequest](#m-CashCountRequest) |  |  |

<a id="m-ApproveCashVarianceRequest"></a>
#### ApproveCashVarianceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewedBy` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CloseCashSessionRequest"></a>
#### CloseCashSessionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `closedBy` | texte |  |  |
| `closingAmount` | nombre décimal |  |  |
| `countedClosingAmount` | nombre décimal |  |  |
| `varianceReason` | texte |  |  |

<a id="m-CreateCashVoucherRequest"></a>
#### CreateCashVoucherRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal | oui |  |
| `documentNumber` | texte | oui | longueur min `1` |
| `flowCategory` | texte |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `counterpartyType` | texte |  |  |
| `counterpartyCode` | texte |  |  |
| `counterpartyName` | texte |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `documentType` | enum : `CASH_VOUCHER`, `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `RECEIPT`, `INVOICE`, `EXPENSE_NOTE`, `BANK_SLIP`, `TRANSFER_NOTE`, `EXTERNAL_REFERENCE`, `OTHER` |  |  |
| `metadataJson` | texte |  |  |
| `natureCode` | texte |  |  |

<a id="m-CreateCashMovementRequest"></a>
#### CreateCashMovementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementType` | enum : `OPENING_FLOAT`, `PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `CLOSING_COUNT`, `EXCHANGE`, `VARIANCE` | oui |  |
| `amount` | nombre décimal | oui |  |
| `documentType` | enum : `CASH_VOUCHER`, `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `RECEIPT`, `INVOICE`, `EXPENSE_NOTE`, `BANK_SLIP`, `TRANSFER_NOTE`, `EXTERNAL_REFERENCE`, `OTHER` |  |  |
| `documentNumber` | texte |  |  |
| `flowCategory` | texte |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `counterpartyType` | texte |  |  |
| `counterpartyCode` | texte |  |  |
| `counterpartyName` | texte |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `metadataJson` | texte |  |  |
| `relatedMovementNumber` | texte |  |  |
| `batchId` | texte |  |  |
| `exchangeRate` | nombre décimal |  |  |
| `channel` | enum : `MANUAL`, `AUTO_PAYMENT`, `IMPORT`, `API`, `POS` |  |  |
| `deviceCode` | texte |  |  |
| `deviceIp` | texte |  |  |
| `subCategory` | texte |  |  |
| `tags` | texte |  |  |
| `direction` | enum : `IN`, `OUT`, `ADJUSTMENT` |  |  |
| `natureCode` | texte |  |  |
| `journalCode` | texte |  |  |
| `vatAmount` | nombre décimal |  |  |
| `vatCode` | texte |  |  |
| `supplierCode` | texte |  |  |
| `beneficiaryName` | texte |  |  |
| `beneficiaryIdNumber` | texte |  |  |
| `beneficiaryPhone` | texte |  |  |
| `requestNumber` | texte |  |  |

<a id="m-CashSessionSummaryResponse"></a>
#### CashSessionSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `status` | enum : `OPENING`, `OPEN`, `PAUSED`, `SUSPENDED`, `COUNTING`, `CLOSING_REVIEW`, `RECOUNT`, `CLOSED`, `CANCELLED` |  |  |
| `openedBy` | texte |  |  |
| `closedBy` | texte |  |  |
| `openingAmount` | nombre décimal |  |  |
| `totalPayments` | nombre décimal |  |  |
| `totalRefunds` | nombre décimal |  |  |
| `totalCashIn` | nombre décimal |  |  |
| `totalCashOut` | nombre décimal |  |  |
| `totalAdjustments` | nombre décimal |  |  |
| `expectedClosingAmount` | nombre décimal |  |  |
| `countedClosingAmount` | nombre décimal |  |  |
| `varianceAmount` | nombre décimal |  |  |
| `varianceReason` | texte |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `closingRequestedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `nonCashPayments` | [CashSessionSummaryResponse.NonCashPaymentTotal](#m-CashSessionSummaryResponse-NonCashPaymentTotal)[] |  |  |

<a id="m-CashRegisterScheduleRequest"></a>
#### CashRegisterScheduleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `dayOfWeek` | entier | oui | min `1`, max `7` |
| `opensAt` | [LocalTime](#m-LocalTime) | oui |  |
| `closesAt` | [LocalTime](#m-LocalTime) | oui |  |

<a id="m-CashMovementAttachmentResponse"></a>
#### CashMovementAttachmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `fileName` | texte |  |  |
| `contentType` | texte |  |  |
| `storagePath` | texte |  |  |
| `label` | texte |  |  |
| `uploadedBy` | texte |  |  |
| `uploadedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CashMovementResponse-Accounting"></a>
#### CashMovementResponse.Accounting

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `journalCode` | texte |  |  |
| `status` | texte |  |  |
| `entryNumber` | texte |  |  |
| `note` | texte |  |  |
| `vatAmount` | nombre décimal |  |  |
| `vatCode` | texte |  |  |
| `supplierCode` | texte |  |  |
| `beneficiaryName` | texte |  |  |
| `beneficiaryIdNumber` | texte |  |  |
| `beneficiaryPhone` | texte |  |  |
| `requestNumber` | texte |  |  |

<a id="m-CashCountRequest"></a>
#### CashCountRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `lines` | [CashCountRequest.Line](#m-CashCountRequest-Line)[] |  |  |
| `totalAmount` | nombre décimal |  |  |
| `secondCounterEmail` | texte |  |  |
| `witnessEmail` | texte |  |  |
| `keptFloat` | nombre décimal |  |  |
| `varianceReasonCode` | texte |  |  |
| `varianceReasonText` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CashSessionSummaryResponse-NonCashPaymentTotal"></a>
#### CashSessionSummaryResponse.NonCashPaymentTotal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
| `provider` | texte |  |  |
| `label` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-LocalTime"></a>
#### LocalTime

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `minute` | entier |  |  |
| `second` | entier |  |  |
| `nano` | entier |  |  |

<a id="m-CashCountRequest-Line"></a>
#### CashCountRequest.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | nombre décimal |  |  |
| `kind` | enum : `BILL`, `COIN` |  |  |
| `quantity` | entier |  |  |
| `damagedQuantity` | entier |  |  |
