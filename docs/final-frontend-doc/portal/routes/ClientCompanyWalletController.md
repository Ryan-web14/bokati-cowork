# ClientCompanyWalletController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 18 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies-customerid-approvals"></a>
### GET /sni/api/v1/client/companies/{customerId}/approvals

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<CompanyWalletDtos.SpendView>`
- Modèles : [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `approvals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-approvals-spendid-approve"></a>
### POST /sni/api/v1/client/companies/{customerId}/approvals/{spendId}/approve

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `spendId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-approvals-spendid-reject"></a>
### POST /sni/api/v1/client/companies/{customerId}/approvals/{spendId}/reject

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `CompanyWalletDtos.DecisionRequest`
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult), [CompanyWalletDtos.DecisionRequest](#m-CompanyWalletDtos-DecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `spendId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-invoices-documentnumber-pay-wallet"></a>
### POST /sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}/pay/wallet

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `CompanyWalletDtos.PayRequest`
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult), [CompanyWalletDtos.PayRequest](#m-CompanyWalletDtos-PayRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `payAsOwner`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-spending-authorizations"></a>
### GET /sni/api/v1/client/companies/{customerId}/spending-authorizations

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<CompanyWalletDtos.AuthorizationView>`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `authorizations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-spending-authorizations"></a>
### POST /sni/api/v1/client/companies/{customerId}/spending-authorizations

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `CompanyWalletDtos.AuthorizationRequest`
- Réponse : `CompanyWalletDtos.AuthorizationView`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView), [CompanyWalletDtos.AuthorizationRequest](#m-CompanyWalletDtos-AuthorizationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `authorize`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-client-companies-customerid-spending-authorizations-authorizationid"></a>
### PATCH /sni/api/v1/client/companies/{customerId}/spending-authorizations/{authorizationId}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `CompanyWalletDtos.AuthorizationUpdateRequest`
- Réponse : `CompanyWalletDtos.AuthorizationView`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView), [CompanyWalletDtos.AuthorizationUpdateRequest](#m-CompanyWalletDtos-AuthorizationUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `authorizationId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-spending-authorizations-authorizationid-pin-reset"></a>
### POST /sni/api/v1/client/companies/{customerId}/spending-authorizations/{authorizationId}/pin/reset

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `CompanyWalletDtos.AuthorizationView`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `resetPin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `authorizationId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-wallet"></a>
### GET /sni/api/v1/client/companies/{customerId}/wallet

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `CompanyWalletDtos.WalletView`
- Modèles : [CompanyWalletDtos.WalletView](#m-CompanyWalletDtos-WalletView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `wallet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `currency` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-client-companies-customerid-wallet-security-pin"></a>
### POST /sni/api/v1/client/companies/{customerId}/wallet/security/pin

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SetWalletPinRequest`
- Réponse : `vide`
- Modèles : [SetWalletPinRequest](#m-SetWalletPinRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `setPin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-wallet-spends"></a>
### GET /sni/api/v1/client/companies/{customerId}/wallet/spends

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<CompanyWalletDtos.SpendView>`
- Modèles : [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView), [CompanyWalletSpend.Status](#m-CompanyWalletSpend-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `spends`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `currency` | requête | `String` |  |  |
| `status` | requête | `CompanyWalletSpend.Status` |  |  |

<a id="r-post-sni-api-v1-client-companies-customerid-wallet-spends-spendid-confirm"></a>
### POST /sni/api/v1/client/companies/{customerId}/wallet/spends/{spendId}/confirm

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `CompanyWalletDtos.ConfirmRequest`
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult), [CompanyWalletDtos.ConfirmRequest](#m-CompanyWalletDtos-ConfirmRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `confirmAsOwner`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `spendId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-wallet-top-ups"></a>
### POST /sni/api/v1/client/companies/{customerId}/wallet/top-ups

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `WalletUsageDtos.TopUpRequest`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse), [WalletUsageDtos.TopUpRequest](#m-WalletUsageDtos-TopUpRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `topUp`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-me-company-wallets"></a>
### GET /sni/api/v1/client/me/company-wallets

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<CompanyWalletDtos.MyCompanyWallet>`
- Modèles : [CompanyWalletDtos.MyCompanyWallet](#m-CompanyWalletDtos-MyCompanyWallet)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `myWallets`

<a id="r-post-sni-api-v1-client-me-company-wallets-customerid-invoices-documentnumber-pay"></a>
### POST /sni/api/v1/client/me/company-wallets/{customerId}/invoices/{documentNumber}/pay

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `CompanyWalletDtos.PayRequest`
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult), [CompanyWalletDtos.PayRequest](#m-CompanyWalletDtos-PayRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `pay`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-me-company-wallets-customerid-pin"></a>
### POST /sni/api/v1/client/me/company-wallets/{customerId}/pin

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SetWalletPinRequest`
- Réponse : `vide`
- Modèles : [SetWalletPinRequest](#m-SetWalletPinRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `setMyPin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-me-company-wallets-customerid-spends-spendid-cancel"></a>
### POST /sni/api/v1/client/me/company-wallets/{customerId}/spends/{spendId}/cancel

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `spendId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-me-company-wallets-customerid-spends-spendid-confirm"></a>
### POST /sni/api/v1/client/me/company-wallets/{customerId}/spends/{spendId}/confirm

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `CompanyWalletDtos.ConfirmRequest`
- Réponse : `CompanyWalletDtos.SpendResult`
- Modèles : [CompanyWalletDtos.SpendResult](#m-CompanyWalletDtos-SpendResult), [CompanyWalletDtos.ConfirmRequest](#m-CompanyWalletDtos-ConfirmRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyWalletController.java`, méthode `confirm`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `spendId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CompanyWalletDtos-SpendView"></a>
#### CompanyWalletDtos.SpendView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `operation` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE` |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `documentNumber` | texte |  |  |
| `status` | enum : `AWAITING_OTP`, `HELD`, `PENDING_APPROVAL`, `EXECUTED`, `REJECTED`, `EXPIRED`, `CANCELLED` |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `transactionNumber` | texte |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `decisionReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyWalletDtos-SpendResult"></a>
#### CompanyWalletDtos.SpendResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `spend` | [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView) |  |  |
| `message` | texte |  |  |

<a id="m-CompanyWalletDtos-DecisionRequest"></a>
#### CompanyWalletDtos.DecisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-CompanyWalletDtos-PayRequest"></a>
#### CompanyWalletDtos.PayRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `pin` | texte |  |  |

<a id="m-CompanyWalletDtos-AuthorizationView"></a>
#### CompanyWalletDtos.AuthorizationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `companyCustomerCode` | texte |  |  |
| `walletNumber` | texte |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `operations` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`[] |  |  |
| `maxPerTransaction` | nombre décimal |  |  |
| `maxPerDay` | nombre décimal |  |  |
| `maxPerMonth` | nombre décimal |  |  |
| `approvalAbove` | nombre décimal |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `REVOKED` |  |  |
| `statusReason` | texte |  |  |
| `inForce` | booléen |  |  |
| `pinSet` | booléen |  |  |
| `engagedToday` | nombre décimal |  |  |
| `engagedThisMonth` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyWalletDtos-AuthorizationRequest"></a>
#### CompanyWalletDtos.AuthorizationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte | oui | longueur min `1` |
| `operations` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`[] | oui | éléments min `1` |
| `maxPerTransaction` | nombre décimal |  |  |
| `maxPerDay` | nombre décimal |  |  |
| `maxPerMonth` | nombre décimal |  |  |
| `approvalAbove` | nombre décimal |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `currency` | texte |  |  |

<a id="m-CompanyWalletDtos-AuthorizationUpdateRequest"></a>
#### CompanyWalletDtos.AuthorizationUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `operations` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`[] | oui | éléments min `1` |
| `maxPerTransaction` | nombre décimal |  |  |
| `maxPerDay` | nombre décimal |  |  |
| `maxPerMonth` | nombre décimal |  |  |
| `approvalAbove` | nombre décimal |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `REVOKED` |  |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-CompanyWalletDtos-WalletView"></a>
#### CompanyWalletDtos.WalletView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `wallet` | [WalletResponse](#m-WalletResponse) |  |  |
| `entries` | [CompanyWalletDtos.EntryView](#m-CompanyWalletDtos-EntryView)[] |  |  |
| `pendingApprovals` | [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView)[] |  |  |

<a id="m-SetWalletPinRequest"></a>
#### SetWalletPinRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currentPin` | texte |  |  |
| `newPin` | texte | oui | longueur min `1` |

<a id="m-CompanyWalletSpend-Status"></a>
#### CompanyWalletSpend.Status

Énumération : `AWAITING_OTP`, `HELD`, `PENDING_APPROVAL`, `EXECUTED`, `REJECTED`, `EXPIRED`, `CANCELLED`

<a id="m-CompanyWalletDtos-ConfirmRequest"></a>
#### CompanyWalletDtos.ConfirmRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `12` |

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

<a id="m-CompanyWalletDtos-MyCompanyWallet"></a>
#### CompanyWalletDtos.MyCompanyWallet

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `authorization` | [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView) |  |  |
| `mySpends` | [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView)[] |  |  |

<a id="m-WalletResponse"></a>
#### WalletResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte |  |  |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `currency` | texte |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `LOCKED`, `CLOSED`, `UNDER_REVIEW` |  |  |
| `availableBalance` | nombre décimal |  |  |
| `ledgerBalance` | nombre décimal |  |  |
| `heldBalance` | nombre décimal |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyWalletDtos-EntryView"></a>
#### CompanyWalletDtos.EntryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `direction` | enum : `DEBIT`, `CREDIT` |  |  |
| `entryType` | enum : `ADMIN_TOPUP`, `TOPUP`, `TRANSFER_IN`, `TRANSFER_OUT`, `TRANSFER_FEE`, `ADMIN_DEBIT`, `PAYMENT`, `REFUND`, `REVERSAL`, `HOLD`, `HOLD_RELEASE`, `ADJUSTMENT`, `CASHBACK`, `PROMOTIONAL_CREDIT`, `OVERPAYMENT_CREDIT` |  |  |
| `amount` | nombre décimal |  |  |
| `balanceAfter` | nombre décimal |  |  |
| `reference` | texte |  |  |
| `spentByMember` | texte |  |  |
| `spentByName` | texte |  |  |
| `documentNumber` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

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
