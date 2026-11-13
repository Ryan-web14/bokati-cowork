# CashOperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 12 route(s).

## Routes

<a id="r-patch-sni-api-v1-cash-registers-movements-movementnumber-approve"></a>
### PATCH /sni/api/v1/cash-registers/movements/{movementNumber}/approve

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:APPROVE`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-movements-movementnumber-cancel"></a>
### PATCH /sni/api/v1/cash-registers/movements/{movementNumber}/cancel

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CashReasonRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-movements-movementnumber-reverse"></a>
### POST /sni/api/v1/cash-registers/movements/{movementNumber}/reverse

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CashReasonRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `reverse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-natures"></a>
### GET /sni/api/v1/cash-registers/natures

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashNatureResponse>`
- Modèles : [CashNatureResponse](#m-CashNatureResponse), [CashFlowDirection](#m-CashFlowDirection), [CashNatureFamily](#m-CashNatureFamily)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `natures`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `direction` | requête | `CashFlowDirection` |  |  |
| `family` | requête | `CashNatureFamily` |  |  |
| `search` | requête | `String` |  |  |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-cash-registers-natures"></a>
### PUT /sni/api/v1/cash-registers/natures

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (obligatoire) : `List<CashNatureRequest>`
- Réponse : `List<CashNatureResponse>`
- Modèles : [CashNatureResponse](#m-CashNatureResponse), [CashNatureRequest](#m-CashNatureRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `replaceNatures`

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-exchanges"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/exchanges

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `CashExchangeRequest`
- Réponse : `CashMovementResponse`
- Modèles : [CashMovementResponse](#m-CashMovementResponse), [CashExchangeRequest](#m-CashExchangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `exchange`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-transfers"></a>
### GET /sni/api/v1/cash-registers/transfers

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashTransferResponse>`
- Modèles : [CashTransferResponse](#m-CashTransferResponse), [CashTransferStatus](#m-CashTransferStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `transfers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |
| `status` | requête | `CashTransferStatus` |  |  |

<a id="r-post-sni-api-v1-cash-registers-transfers"></a>
### POST /sni/api/v1/cash-registers/transfers

- Accès : back-office : rôle d'administration et droit `CASH_TRANSFER:SEND`
- Corps JSON (obligatoire) : `CashTransferRequest`
- Réponse : `CashTransferResponse`
- Modèles : [CashTransferResponse](#m-CashTransferResponse), [CashTransferRequest](#m-CashTransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `send`

<a id="r-patch-sni-api-v1-cash-registers-transfers-transfernumber-cancel"></a>
### PATCH /sni/api/v1/cash-registers/transfers/{transferNumber}/cancel

- Accès : back-office : rôle d'administration et droit `CASH_TRANSFER:SEND`
- Corps JSON (obligatoire) : `CashReasonRequest`
- Réponse : `CashTransferResponse`
- Modèles : [CashTransferResponse](#m-CashTransferResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `cancelTransfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-transfers-transfernumber-receive"></a>
### PATCH /sni/api/v1/cash-registers/transfers/{transferNumber}/receive

- Accès : back-office : rôle d'administration et droit `CASH_TRANSFER:RECEIVE`
- Corps JSON (obligatoire) : `CashTransferReceiveRequest`
- Réponse : `CashTransferResponse`
- Modèles : [CashTransferResponse](#m-CashTransferResponse), [CashTransferReceiveRequest](#m-CashTransferReceiveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `receive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-petty-cash-refill"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/petty-cash/refill

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashPettyRefillResponse`
- Modèles : [CashPettyRefillResponse](#m-CashPettyRefillResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `pettyRefillProposal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-registercode-petty-cash-refill"></a>
### POST /sni/api/v1/cash-registers/{registerCode}/petty-cash/refill

- Accès : back-office : rôle d'administration et droit `CASH_TRANSFER:SEND`
- Réponse : `CashTransferResponse`
- Modèles : [CashTransferResponse](#m-CashTransferResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashOperationsController.java`, méthode `pettyRefill`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |
| `carrierEmail` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-CashReasonRequest"></a>
#### CashReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte |  |  |
| `reasonText` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CashNatureResponse"></a>
#### CashNatureResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `family` | enum : `ENCAISSEMENTS`, `ACHATS`, `DEPLACEMENTS`, `AVANCES`, `REMISES`, `TRANSFERTS`, `REMBOURSEMENTS`, `DIVERS` |  |  |
| `direction` | enum : `IN`, `OUT`, `ADJUSTMENT` |  |  |
| `movementType` | enum : `OPENING_FLOAT`, `PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `CLOSING_COUNT`, `EXCHANGE`, `VARIANCE` |  |  |
| `accountNumber` | texte |  |  |
| `vatCode` | texte |  |  |
| `documentRequired` | booléen |  |  |
| `justificationDays` | entier |  |  |
| `defaultSection` | texte |  |  |
| `maxPerOperation` | nombre décimal |  |  |
| `priorRequestRequired` | booléen |  |  |
| `doubleVisaThreshold` | nombre décimal |  |  |
| `keywords` | texte |  |  |
| `sortOrder` | entier |  |  |
| `usageCount` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |
| `systemValue` | booléen |  |  |

<a id="m-CashFlowDirection"></a>
#### CashFlowDirection

Énumération : `IN`, `OUT`, `ADJUSTMENT`

<a id="m-CashNatureFamily"></a>
#### CashNatureFamily

Énumération : `ENCAISSEMENTS`, `ACHATS`, `DEPLACEMENTS`, `AVANCES`, `REMISES`, `TRANSFERTS`, `REMBOURSEMENTS`, `DIVERS`

<a id="m-CashNatureRequest"></a>
#### CashNatureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `label` | texte | oui | longueur min `1` |
| `family` | enum : `ENCAISSEMENTS`, `ACHATS`, `DEPLACEMENTS`, `AVANCES`, `REMISES`, `TRANSFERTS`, `REMBOURSEMENTS`, `DIVERS` | oui |  |
| `direction` | enum : `IN`, `OUT`, `ADJUSTMENT` | oui |  |
| `movementType` | enum : `OPENING_FLOAT`, `PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `CLOSING_COUNT`, `EXCHANGE`, `VARIANCE` |  |  |
| `accountNumber` | texte |  |  |
| `vatCode` | texte |  |  |
| `documentRequired` | booléen |  |  |
| `justificationDays` | entier |  |  |
| `defaultSection` | texte |  |  |
| `maxPerOperation` | nombre décimal |  |  |
| `priorRequestRequired` | booléen |  |  |
| `doubleVisaThreshold` | nombre décimal |  |  |
| `keywords` | texte |  |  |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-CashExchangeRequest"></a>
#### CashExchangeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `given` | [CashCountRequest.Line](#m-CashCountRequest-Line)[] | oui | éléments min `1` |
| `received` | [CashCountRequest.Line](#m-CashCountRequest-Line)[] | oui | éléments min `1` |
| `note` | texte |  |  |

<a id="m-CashTransferResponse"></a>
#### CashTransferResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transferNumber` | texte |  |  |
| `kind` | enum : `FEED`, `REMITTANCE`, `SAFE_DEPOSIT`, `SAFE_WITHDRAWAL`, `PETTY_REFILL`, `OTHER` |  |  |
| `fromRegister` | texte |  |  |
| `toRegister` | texte |  |  |
| `currency` | texte |  |  |
| `sentAmount` | nombre décimal |  |  |
| `receivedAmount` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `status` | enum : `IN_TRANSIT`, `RECEIVED`, `DISPUTED`, `CANCELLED` |  |  |
| `carrier` | texte |  |  |
| `sentBy` | texte |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `fromSession` | texte |  |  |
| `outMovement` | texte |  |  |
| `receivedBy` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `toSession` | texte |  |  |
| `inMovement` | texte |  |  |
| `reviewNumber` | texte |  |  |
| `cancelledBy` | texte |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancelReason` | texte |  |  |
| `minutesInTransit` | entier int64 ⚠ |  |  |
| `note` | texte |  |  |

<a id="m-CashTransferStatus"></a>
#### CashTransferStatus

Énumération : `IN_TRANSIT`, `RECEIVED`, `DISPUTED`, `CANCELLED`

<a id="m-CashTransferRequest"></a>
#### CashTransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fromRegisterCode` | texte | oui | longueur min `1` |
| `toRegisterCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `count` | [CashCountRequest](#m-CashCountRequest) |  |  |
| `carrierEmail` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CashTransferReceiveRequest"></a>
#### CashTransferReceiveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `count` | [CashCountRequest](#m-CashCountRequest) | oui |  |
| `reason` | texte |  |  |

<a id="m-CashPettyRefillResponse"></a>
#### CashPettyRefillResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `parentRegisterCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `since` | date-heure ISO 8601 UTC |  |  |
| `expenseCount` | entier |  |  |
| `standardFloat` | nombre décimal |  |  |

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

<a id="m-CashCountRequest-Line"></a>
#### CashCountRequest.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | nombre décimal |  |  |
| `kind` | enum : `BILL`, `COIN` |  |  |
| `quantity` | entier |  |  |
| `damagedQuantity` | entier |  |  |

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
