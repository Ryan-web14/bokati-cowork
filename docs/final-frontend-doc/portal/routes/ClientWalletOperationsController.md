# ClientWalletOperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 23 route(s).

## Routes

<a id="r-get-sni-api-v1-client-wallet-walletnumber-beneficiaries"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/beneficiaries

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<WalletUsageDtos.BeneficiaryView>`
- Modèles : [WalletUsageDtos.BeneficiaryView](#m-WalletUsageDtos-BeneficiaryView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `beneficiaries`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-beneficiaries"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/beneficiaries

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.BeneficiaryRequest`
- Réponse : `WalletUsageDtos.BeneficiaryView`
- Modèles : [WalletUsageDtos.BeneficiaryView](#m-WalletUsageDtos-BeneficiaryView), [WalletUsageDtos.BeneficiaryRequest](#m-WalletUsageDtos-BeneficiaryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `addBeneficiary`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-client-wallet-walletnumber-beneficiaries-id"></a>
### DELETE /sni/api/v1/client/wallet/{walletNumber}/beneficiaries/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `removeBeneficiary`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-devices"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/devices

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<WalletUsageDtos.DeviceView>`
- Modèles : [WalletUsageDtos.DeviceView](#m-WalletUsageDtos-DeviceView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `devices`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-client-wallet-walletnumber-devices-id"></a>
### PATCH /sni/api/v1/client/wallet/{walletNumber}/devices/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.DeviceUpdateRequest`
- Réponse : `WalletUsageDtos.DeviceView`
- Modèles : [WalletUsageDtos.DeviceView](#m-WalletUsageDtos-DeviceView), [WalletUsageDtos.DeviceUpdateRequest](#m-WalletUsageDtos-DeviceUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `updateDevice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-devices-id-revoke"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/devices/{id}/revoke

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.DeviceView`
- Modèles : [WalletUsageDtos.DeviceView](#m-WalletUsageDtos-DeviceView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `revokeDevice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-ledger-transactionnumber-receipt"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/ledger/{transactionNumber}/receipt

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Renvoie : `application/pdf`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `receipt`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-lock"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/lock

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.OwnerStateView`
- Modèles : [WalletUsageDtos.OwnerStateView](#m-WalletUsageDtos-OwnerStateView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `lock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `X-Device-Id` | en-tête | `String` |  |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-payment-requests"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/payment-requests

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<WalletUsageDtos.PaymentRequestView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletUsageDtos.PaymentRequestView](#m-WalletUsageDtos-PaymentRequestView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `paymentRequests`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-payment-requests"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/payment-requests

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.PaymentRequestRequest`
- Réponse : `WalletUsageDtos.PaymentRequestView`
- Modèles : [WalletUsageDtos.PaymentRequestView](#m-WalletUsageDtos-PaymentRequestView), [WalletUsageDtos.PaymentRequestRequest](#m-WalletUsageDtos-PaymentRequestRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `requestPayment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-payment-requests-requestnumber-cancel"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/payment-requests/{requestNumber}/cancel

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.PaymentRequestView`
- Modèles : [WalletUsageDtos.PaymentRequestView](#m-WalletUsageDtos-PaymentRequestView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `cancelRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `requestNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-payment-requests-requestnumber-decline"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/payment-requests/{requestNumber}/decline

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.PaymentRequestView`
- Modèles : [WalletUsageDtos.PaymentRequestView](#m-WalletUsageDtos-PaymentRequestView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `decline`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `requestNumber` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-client-wallet-walletnumber-preferences"></a>
### PUT /sni/api/v1/client/wallet/{walletNumber}/preferences

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.PreferencesRequest`
- Réponse : `WalletUsageDtos.OwnerStateView`
- Modèles : [WalletUsageDtos.OwnerStateView](#m-WalletUsageDtos-OwnerStateView), [WalletUsageDtos.PreferencesRequest](#m-WalletUsageDtos-PreferencesRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `preferences`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-state"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/state

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.OwnerStateView`
- Modèles : [WalletUsageDtos.OwnerStateView](#m-WalletUsageDtos-OwnerStateView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `state`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-statement"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/statement

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Renvoie : `application/pdf`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `statement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-top-ups"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/top-ups

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.TopUpRequest`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse), [WalletUsageDtos.TopUpRequest](#m-WalletUsageDtos-TopUpRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `topUp`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-transfers"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/transfers

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<WalletUsageDtos.TransferView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletUsageDtos.TransferView](#m-WalletUsageDtos-TransferView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `transfers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-transfers"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/transfers

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.TransferRequest`
- Réponse : `WalletUsageDtos.InitiatedTransferView`
- Modèles : [WalletUsageDtos.InitiatedTransferView](#m-WalletUsageDtos-InitiatedTransferView), [WalletUsageDtos.TransferRequest](#m-WalletUsageDtos-TransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `initiate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `X-Device-Id` | en-tête | `String` |  |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-transfers-simulate"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/transfers/simulate

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.TransferRequest`
- Réponse : `WalletTransferService.TransferPreview`
- Modèles : [WalletTransferService.TransferPreview](#m-WalletTransferService-TransferPreview), [WalletUsageDtos.TransferRequest](#m-WalletUsageDtos-TransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `simulate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `X-Device-Id` | en-tête | `String` |  |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-transfers-transfernumber"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/transfers/{transferNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.TransferView`
- Modèles : [WalletUsageDtos.TransferView](#m-WalletUsageDtos-TransferView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `transfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `transferNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-transfers-transfernumber-cancel"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/transfers/{transferNumber}/cancel

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletUsageDtos.TransferView`
- Modèles : [WalletUsageDtos.TransferView](#m-WalletUsageDtos-TransferView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `transferNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-transfers-transfernumber-confirm"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/transfers/{transferNumber}/confirm

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.PinRequest`
- Réponse : `WalletUsageDtos.TransferView`
- Modèles : [WalletUsageDtos.TransferView](#m-WalletUsageDtos-TransferView), [WalletUsageDtos.PinRequest](#m-WalletUsageDtos-PinRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `confirm`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `transferNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-unlock"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/unlock

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `WalletUsageDtos.PinRequest`
- Réponse : `WalletUsageDtos.OwnerStateView`
- Modèles : [WalletUsageDtos.OwnerStateView](#m-WalletUsageDtos-OwnerStateView), [WalletUsageDtos.PinRequest](#m-WalletUsageDtos-PinRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java`, méthode `unlock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `X-Device-Id` | en-tête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WalletUsageDtos-BeneficiaryView"></a>
#### WalletUsageDtos.BeneficiaryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `alias` | texte |  |  |
| `walletNumber` | texte |  |  |
| `name` | texte |  |  |
| `transferCount` | entier |  |  |
| `lastUsedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletUsageDtos-BeneficiaryRequest"></a>
#### WalletUsageDtos.BeneficiaryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `counterparty` | texte | oui | longueur min `1` |
| `alias` | texte | oui | longueur min `1` |

<a id="m-WalletUsageDtos-DeviceView"></a>
#### WalletUsageDtos.DeviceView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `deviceId` | texte |  |  |
| `label` | texte |  |  |
| `firstSeenAt` | date-heure ISO 8601 UTC |  |  |
| `lastSeenAt` | date-heure ISO 8601 UTC |  |  |
| `lastIpAddress` | texte |  |  |
| `useCount` | entier |  |  |
| `trusted` | booléen |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletUsageDtos-DeviceUpdateRequest"></a>
#### WalletUsageDtos.DeviceUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `trusted` | booléen |  |  |

<a id="m-WalletUsageDtos-OwnerStateView"></a>
#### WalletUsageDtos.OwnerStateView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte |  |  |
| `lockedByOwner` | booléen |  |  |
| `lockedAt` | date-heure ISO 8601 UTC |  |  |
| `frozen` | booléen |  |  |
| `lowBalanceThreshold` | nombre décimal |  |  |
| `notifyOnCredit` | booléen |  |  |
| `notifyOnDebit` | booléen |  |  |

<a id="m-WalletUsageDtos-PaymentRequestView"></a>
#### WalletUsageDtos.PaymentRequestView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requestNumber` | texte |  |  |
| `direction` | texte |  |  |
| `counterpartyWallet` | texte |  |  |
| `counterpartyName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `reason` | texte |  |  |
| `status` | enum : `PENDING`, `PAID`, `DECLINED`, `CANCELLED`, `EXPIRED` |  |  |
| `transferNumber` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `resolvedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletUsageDtos-PaymentRequestRequest"></a>
#### WalletUsageDtos.PaymentRequestRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `payer` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `reason` | texte |  |  |

<a id="m-WalletUsageDtos-PreferencesRequest"></a>
#### WalletUsageDtos.PreferencesRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lowBalanceThreshold` | nombre décimal |  |  |
| `notifyOnCredit` | booléen |  |  |
| `notifyOnDebit` | booléen |  |  |

<a id="m-MobileMoneyDepositResponse"></a>
#### MobileMoneyDepositResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `depositId` | texte |  |  |
| `clientReferenceId` | texte |  |  |
| `customerMessage` | texte |  |  |
| `metadata` | objet { clé : objet libre }[] |  |  |
| `payer` | [MobileMoneyDepositResponse.PayerResponse](#m-MobileMoneyDepositResponse-PayerResponse) |  |  |
| `provider` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | texte |  |  |
| `phase` | texte |  |  |
| `providerMessage` | texte |  |  |
| `failureReason` | texte |  |  |
| `failureCode` | texte |  |  |
| `userMessage` | texte |  |  |
| `retryable` | booléen |  |  |
| `nextStatusCheckAt` | date-heure ISO 8601 UTC |  |  |
| `intentNumber` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `callbackUrl` | texte |  |  |
| `requestPayloadJson` | texte |  |  |
| `providerResponseJson` | texte |  |  |
| `lastStatusCheckedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `failedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `transaction` | [PaymentTransactionResponse](#m-PaymentTransactionResponse) |  |  |

<a id="m-WalletUsageDtos-TopUpRequest"></a>
#### WalletUsageDtos.TopUpRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal | oui |  |
| `phoneNumber` | texte | oui | longueur min `1` |
| `correspondent` | enum : `MTN_MOMO_CIV`, `ORANGE_CIV`, `WAVE_CIV`, `AIRTEL_COG`, `MTN_MOMO_COG`, `FREE_SEN`, `ORANGE_SEN`, `WAVE_SEN`, `ORANGE_COD`, `AIRTEL_COD`, `VODACOM_MPESA_COD`, `AIRTEL_GAB`, `MTN_MOMO_CMR`, `AIRTEL_OAPI_COG`, `AIRTEL_OAPI_COD`, `MPESA_COD` | oui |  |

<a id="m-WalletUsageDtos-TransferView"></a>
#### WalletUsageDtos.TransferView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transferNumber` | texte |  |  |
| `transferUuid` | uuid |  |  |
| `direction` | texte |  |  |
| `counterpartyWallet` | texte |  |  |
| `counterpartyName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `feeAmount` | nombre décimal |  |  |
| `totalDebit` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `PENDING_CONFIRMATION`, `COMPLETED`, `FAILED`, `CANCELLED` |  |  |
| `message` | texte |  |  |
| `paymentRequestNumber` | texte |  |  |
| `failureReason` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletUsageDtos-InitiatedTransferView"></a>
#### WalletUsageDtos.InitiatedTransferView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transfer` | [WalletUsageDtos.TransferView](#m-WalletUsageDtos-TransferView) |  |  |
| `confirmationCode` | texte |  |  |
| `challenge` | enum : `PIN`, `OTP`, `PIN_AND_OTP` |  |  |
| `confirmationExpiresAt` | date-heure ISO 8601 UTC |  |  |
| `maxAttempts` | entier |  |  |

<a id="m-WalletUsageDtos-TransferRequest"></a>
#### WalletUsageDtos.TransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `counterparty` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `message` | texte |  |  |
| `paymentRequestNumber` | texte |  |  |

<a id="m-WalletTransferService-TransferPreview"></a>
#### WalletTransferService.TransferPreview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `counterpartyWallet` | texte |  |  |
| `counterpartyName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `fee` | nombre décimal |  |  |
| `totalDebit` | nombre décimal |  |  |
| `balanceAfter` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `allowed` | booléen |  |  |
| `blockingReason` | texte |  |  |
| `upgradePath` | texte |  |  |
| `pinRequired` | booléen |  |  |
| `pinMissing` | booléen |  |  |
| `otpRequired` | booléen |  |  |
| `knownBeneficiary` | booléen |  |  |
| `newDevice` | booléen |  |  |

<a id="m-WalletUsageDtos-PinRequest"></a>
#### WalletUsageDtos.PinRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `pin` | texte | oui | longueur min `1` |

<a id="m-MobileMoneyDepositResponse-PayerResponse"></a>
#### MobileMoneyDepositResponse.PayerResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `accountDetails` | [MobileMoneyDepositResponse.AccountDetailsResponse](#m-MobileMoneyDepositResponse-AccountDetailsResponse) |  |  |

<a id="m-PaymentTransactionResponse"></a>
#### PaymentTransactionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `intentNumber` | texte |  |  |
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
| `provider` | texte |  |  |
| `providerReference` | texte |  |  |
| `receiptNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `REFUNDED`, `REVERSED` |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `receivedBy` | texte |  |  |
| `failureReason` | texte |  |  |
| `metadataJson` | texte |  |  |
| `channel` | enum : `SELF_SERVICE`, `BACK_OFFICE`, `SYSTEM` |  |  |
| `paymentMethodLabel` | texte |  |  |
| `tenderedAmount` | nombre décimal |  |  |
| `changeAmount` | nombre décimal |  |  |
| `cashSessionNumber` | texte |  |  |
| `bankName` | texte |  |  |
| `payerName` | texte |  |  |
| `payerPhone` | texte |  |  |

<a id="m-MobileMoneyDepositResponse-AccountDetailsResponse"></a>
#### MobileMoneyDepositResponse.AccountDetailsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte |  |  |
| `provider` | texte |  |  |
