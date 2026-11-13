# CashRegisterAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 19 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-form-defaults"></a>
### GET /sni/api/v1/cash-registers/form-defaults

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashFormDefaultsResponse`
- Modèles : [CashFormDefaultsResponse](#m-CashFormDefaultsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `formDefaults`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `context` | requête | `String` | oui |  |
| `parameters` | requête | `Map<String, String>` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-mine"></a>
### GET /sni/api/v1/cash-registers/mine

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashRegisterDetailResponse>`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `myRegisters`

<a id="r-get-sni-api-v1-cash-registers-reference-lists"></a>
### GET /sni/api/v1/cash-registers/reference-lists

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `Map<String, List<CashReferenceValueResponse>>`
- Modèles : [CashReferenceValueResponse](#m-CashReferenceValueResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `referenceLists`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-cash-registers-reference-lists-listcode"></a>
### GET /sni/api/v1/cash-registers/reference-lists/{listCode}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashReferenceValueResponse>`
- Modèles : [CashReferenceValueResponse](#m-CashReferenceValueResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `referenceList`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-cash-registers-reference-lists-listcode"></a>
### PUT /sni/api/v1/cash-registers/reference-lists/{listCode}

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (obligatoire) : `List<CashReferenceValueRequest>`
- Réponse : `List<CashReferenceValueResponse>`
- Modèles : [CashReferenceValueResponse](#m-CashReferenceValueResponse), [CashReferenceValueRequest](#m-CashReferenceValueRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `replaceReferenceList`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-responsibility-transfers-transfernumber-sign"></a>
### PATCH /sni/api/v1/cash-registers/responsibility-transfers/{transferNumber}/sign

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:OPEN`
- Réponse : `CashResponsibilityTransferResponse`
- Modèles : [CashResponsibilityTransferResponse](#m-CashResponsibilityTransferResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `signTransfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode"></a>
### GET /sni/api/v1/cash-registers/{registerCode}

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashRegisterDetailResponse`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (obligatoire) : `UpdateCashRegisterRequest`
- Réponse : `CashRegisterDetailResponse`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse), [UpdateCashRegisterRequest](#m-UpdateCashRegisterRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-assignments"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/assignments

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashRegisterAssignmentResponse>`
- Modèles : [CashRegisterAssignmentResponse](#m-CashRegisterAssignmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `assignments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |
| `includeEnded` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-cash-registers-registercode-assignments"></a>
### PUT /sni/api/v1/cash-registers/{registerCode}/assignments

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Corps JSON (obligatoire) : `List<CashRegisterAssignmentRequest>`
- Réponse : `List<CashRegisterAssignmentResponse>`
- Modèles : [CashRegisterAssignmentResponse](#m-CashRegisterAssignmentResponse), [CashRegisterAssignmentRequest](#m-CashRegisterAssignmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `replaceAssignments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode-close"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}/close

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (facultatif) : `CashRegisterStatusRequest`
- Réponse : `CashRegisterDetailResponse`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse), [CashRegisterStatusRequest](#m-CashRegisterStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `close`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode-devices-devicecode"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}/devices/{deviceCode}

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (obligatoire) : `CashDeviceDecisionRequest`
- Réponse : `CashRegisterDetailResponse`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse), [CashDeviceDecisionRequest](#m-CashDeviceDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `decideDevice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |
| `deviceCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-registercode-emergency-assignments"></a>
### POST /sni/api/v1/cash-registers/{registerCode}/emergency-assignments

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Corps JSON (obligatoire) : `EmergencyAssignmentRequest`
- Réponse : `CashRegisterAssignmentResponse`
- Modèles : [CashRegisterAssignmentResponse](#m-CashRegisterAssignmentResponse), [EmergencyAssignmentRequest](#m-EmergencyAssignmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `emergencyAssignment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-history"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/history

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashRegisterHistoryResponse>`
- Modèles : [CashRegisterHistoryResponse](#m-CashRegisterHistoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode-reactivate"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}/reactivate

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Réponse : `CashRegisterDetailResponse`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `reactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-responsibility-transfers"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/responsibility-transfers

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashResponsibilityTransferResponse>`
- Modèles : [CashResponsibilityTransferResponse](#m-CashResponsibilityTransferResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `transfers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-registercode-responsibility-transfers"></a>
### POST /sni/api/v1/cash-registers/{registerCode}/responsibility-transfers

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Corps JSON (obligatoire) : `CreateResponsibilityTransferRequest`
- Réponse : `CashResponsibilityTransferResponse`
- Modèles : [CashResponsibilityTransferResponse](#m-CashResponsibilityTransferResponse), [CreateResponsibilityTransferRequest](#m-CreateResponsibilityTransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `createTransfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-responsibility-transfers-pending-items"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/responsibility-transfers/pending-items

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashPendingItemResponse>`
- Modèles : [CashPendingItemResponse](#m-CashPendingItemResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `pendingItems`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-registercode-suspend"></a>
### PATCH /sni/api/v1/cash-registers/{registerCode}/suspend

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (facultatif) : `CashRegisterStatusRequest`
- Réponse : `CashRegisterDetailResponse`
- Modèles : [CashRegisterDetailResponse](#m-CashRegisterDetailResponse), [CashRegisterStatusRequest](#m-CashRegisterStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterAdminController.java`, méthode `suspend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashFormDefaultsResponse"></a>
#### CashFormDefaultsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `context` | texte |  |  |
| `values` | objet { clé : objet libre } |  |  |
| `origins` | objet { clé : texte } |  |  |
| `choices` | objet { clé : [CashFormDefaultsResponse.Choice](#m-CashFormDefaultsResponse-Choice)[] } |  |  |
| `warnings` | texte[] |  |  |

<a id="m-CashRegisterDetailResponse"></a>
#### CashRegisterDetailResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `name` | texte |  |  |
| `registerType` | enum : `COUNTER`, `MAIN`, `PETTY_CASH`, `SAFE`, `AUTOMATIC` |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `CLOSED` |  |  |
| `statusReason` | texte |  |  |
| `locationCode` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `deviceCode` | texte |  |  |
| `parentRegisterCode` | texte |  |  |
| `systemManaged` | booléen |  |  |
| `restrictedToMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
| `acceptedMethods` | texte[] |  |  |
| `acceptedCurrencies` | texte[] |  |  |
| `cashControlEnabled` | booléen |  |  |
| `maxCashAmount` | nombre décimal |  |  |
| `capBehavior` | enum : `ALERT`, `BLOCK` |  |  |
| `maxOutPerOperation` | nombre décimal |  |  |
| `maxOutPerDay` | nombre décimal |  |  |
| `standardFloat` | nombre décimal |  |  |
| `alertRecipients` | texte[] |  |  |
| `treasuryAccountNumber` | texte |  |  |
| `defaultJournalCode` | texte |  |  |
| `lastCountAt` | date-heure ISO 8601 UTC |  |  |
| `lastCountAmount` | nombre décimal |  |  |
| `currentHolder` | texte |  |  |
| `assignments` | [CashRegisterAssignmentResponse](#m-CashRegisterAssignmentResponse)[] |  |  |
| `schedules` | [CashRegisterDetailResponse.Schedule](#m-CashRegisterDetailResponse-Schedule)[] |  |  |
| `devices` | [CashRegisterDetailResponse.Device](#m-CashRegisterDetailResponse-Device)[] |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `updatedBy` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CashReferenceValueResponse"></a>
#### CashReferenceValueResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `listCode` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `requiresText` | booléen |  |  |
| `systemValue` | booléen |  |  |
| `usageCount` | entier int64 ⚠ |  |  |

<a id="m-CashReferenceValueRequest"></a>
#### CashReferenceValueRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `label` | texte | oui | longueur min `1` |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `requiresText` | booléen |  |  |

<a id="m-CashResponsibilityTransferResponse"></a>
#### CashResponsibilityTransferResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transferNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `fromUser` | texte |  |  |
| `toUser` | texte |  |  |
| `reasonCode` | texte |  |  |
| `reasonLabel` | texte |  |  |
| `reasonText` | texte |  |  |
| `permanent` | booléen |  |  |
| `effectiveAt` | date-heure ISO 8601 UTC |  |  |
| `endsAt` | date-heure ISO 8601 UTC |  |  |
| `expectedAmount` | nombre décimal |  |  |
| `countedAmount` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `witness` | texte |  |  |
| `pendingItems` | [CashPendingItemResponse](#m-CashPendingItemResponse)[] |  |  |
| `status` | enum : `PENDING_SIGNATURES`, `SIGNED`, `CANCELLED` |  |  |
| `outgoingSignedBy` | texte |  |  |
| `outgoingSignedAt` | date-heure ISO 8601 UTC |  |  |
| `incomingSignedBy` | texte |  |  |
| `incomingSignedAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-UpdateCashRegisterRequest"></a>
#### UpdateCashRegisterRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `locationCode` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `parentRegisterCode` | texte |  |  |
| `cashControlEnabled` | booléen |  |  |
| `maxCashAmount` | nombre décimal |  |  |
| `acceptedMethods` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE`[] |  |  |
| `acceptedCurrencies` | texte[] |  |  |
| `capBehavior` | enum : `ALERT`, `BLOCK` |  |  |
| `maxOutPerOperation` | nombre décimal |  |  |
| `maxOutPerDay` | nombre décimal |  |  |
| `standardFloat` | nombre décimal |  |  |
| `alertRecipients` | texte[] |  |  |
| `treasuryAccountNumber` | texte |  |  |
| `defaultJournalCode` | texte |  |  |
| `schedules` | [CashRegisterScheduleRequest](#m-CashRegisterScheduleRequest)[] |  |  |
| `effectiveAt` | date-heure ISO 8601 UTC |  |  |
| `reason` | texte |  |  |

<a id="m-CashRegisterAssignmentResponse"></a>
#### CashRegisterAssignmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `registerCode` | texte |  |  |
| `userEmail` | texte |  |  |
| `role` | enum : `CASHIER`, `HEAD_CASHIER`, `SUPERVISOR`, `VIEWER` |  |  |
| `quality` | enum : `HOLDER`, `BACKUP`, `INTERIM` |  |  |
| `backupRank` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validTo` | date-heure ISO 8601 UTC |  |  |
| `emergency` | booléen |  |  |
| `source` | texte |  |  |
| `sourceReference` | texte |  |  |
| `grantedBy` | texte |  |  |
| `note` | texte |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedBy` | texte |  |  |
| `effectiveNow` | booléen |  |  |

<a id="m-CashRegisterAssignmentRequest"></a>
#### CashRegisterAssignmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userEmail` | texte | oui | longueur min `1` |
| `role` | enum : `CASHIER`, `HEAD_CASHIER`, `SUPERVISOR`, `VIEWER` | oui |  |
| `quality` | enum : `HOLDER`, `BACKUP`, `INTERIM` | oui |  |
| `backupRank` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validTo` | date-heure ISO 8601 UTC |  |  |
| `note` | texte |  |  |

<a id="m-CashRegisterStatusRequest"></a>
#### CashRegisterStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte |  |  |
| `reasonText` | texte |  |  |

<a id="m-CashDeviceDecisionRequest"></a>
#### CashDeviceDecisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | enum : `PROPOSED`, `ALLOWED`, `REFUSED` | oui |  |
| `label` | texte |  |  |

<a id="m-EmergencyAssignmentRequest"></a>
#### EmergencyAssignmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userEmail` | texte | oui | longueur min `1` |
| `hours` | entier |  |  |
| `reason` | texte | oui | longueur min `1` |

<a id="m-CashRegisterHistoryResponse"></a>
#### CashRegisterHistoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `field` | texte |  |  |
| `oldValue` | texte |  |  |
| `newValue` | texte |  |  |
| `changedBy` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |
| `effectiveAt` | date-heure ISO 8601 UTC |  |  |
| `reason` | texte |  |  |

<a id="m-CreateResponsibilityTransferRequest"></a>
#### CreateResponsibilityTransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `toUserEmail` | texte | oui | longueur min `1` |
| `reasonCode` | texte | oui | longueur min `1` |
| `reasonText` | texte |  |  |
| `permanent` | booléen | oui |  |
| `effectiveAt` | date-heure ISO 8601 UTC |  |  |
| `endsAt` | date-heure ISO 8601 UTC |  |  |
| `countedAmount` | nombre décimal |  |  |
| `witnessEmail` | texte |  |  |
| `outgoingAbsent` | booléen |  |  |
| `keepFormerAsBackup` | booléen |  |  |

<a id="m-CashPendingItemResponse"></a>
#### CashPendingItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `reference` | texte |  |  |
| `label` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `blocking` | booléen |  |  |
| `fate` | texte |  |  |

<a id="m-CashFormDefaultsResponse-Choice"></a>
#### CashFormDefaultsResponse.Choice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |

<a id="m-CashRegisterDetailResponse-Schedule"></a>
#### CashRegisterDetailResponse.Schedule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `dayOfWeek` | entier |  |  |
| `opensAt` | [LocalTime](#m-LocalTime) |  |  |
| `closesAt` | [LocalTime](#m-LocalTime) |  |  |

<a id="m-CashRegisterDetailResponse-Device"></a>
#### CashRegisterDetailResponse.Device

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `deviceCode` | texte |  |  |
| `label` | texte |  |  |
| `status` | enum : `PROPOSED`, `ALLOWED`, `REFUSED` |  |  |
| `firstSeenAt` | date-heure ISO 8601 UTC |  |  |
| `firstSeenBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `decidedBy` | texte |  |  |

<a id="m-CashRegisterScheduleRequest"></a>
#### CashRegisterScheduleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `dayOfWeek` | entier | oui | min `1`, max `7` |
| `opensAt` | [LocalTime](#m-LocalTime) | oui |  |
| `closesAt` | [LocalTime](#m-LocalTime) | oui |  |

<a id="m-LocalTime"></a>
#### LocalTime

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `minute` | entier |  |  |
| `second` | entier |  |  |
| `nano` | entier |  |  |
