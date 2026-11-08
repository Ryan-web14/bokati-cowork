# MobileMoneyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-payments-mobile-money-callbacks"></a>
### GET /sni/api/v1/payments/mobile-money/callbacks

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `PaginatedResponse<MobileMoneyCallbackResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [MobileMoneyCallbackResponse](#m-MobileMoneyCallbackResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `listCallbacks`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `outcome` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=receivedAt,DESC |

<a id="r-get-sni-api-v1-payments-mobile-money-deposits"></a>
### GET /sni/api/v1/payments/mobile-money/deposits

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `PaginatedResponse<MobileMoneyDepositResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `listDeposits`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-payments-mobile-money-deposits-depositid"></a>
### GET /sni/api/v1/payments/mobile-money/deposits/{depositId}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `getDeposit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-mobile-money-deposits-depositid-recheck"></a>
### POST /sni/api/v1/payments/mobile-money/deposits/{depositId}/recheck

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `recheck`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-mobile-money-pawapay-callback"></a>
### POST /sni/api/v1/payments/mobile-money/pawapay/callback

- Accès : public, sans jeton
- Corps JSON (facultatif) : `binary`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `depositCallback`

<a id="r-post-sni-api-v1-payments-mobile-money-pawapay-refund-callback"></a>
### POST /sni/api/v1/payments/mobile-money/pawapay/refund-callback

- Accès : public, sans jeton
- Corps JSON (facultatif) : `binary`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `refundCallback`

<a id="r-get-sni-api-v1-payments-mobile-money-pawapay-return"></a>
### GET /sni/api/v1/payments/mobile-money/pawapay/return

- Accès : public, sans jeton
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `paymentPageReturn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositId` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-mobile-money-pawaypay-callback"></a>
### POST /sni/api/v1/payments/mobile-money/pawaypay/callback

- Accès : public, sans jeton
- Corps JSON (facultatif) : `binary`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `depositCallback`

<a id="r-post-sni-api-v1-payments-mobile-money-pawaypay-refund-callback"></a>
### POST /sni/api/v1/payments/mobile-money/pawaypay/refund-callback

- Accès : public, sans jeton
- Corps JSON (facultatif) : `binary`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `refundCallback`

<a id="r-post-sni-api-v1-payments-mobile-money-pawaypay-test-phonenumber"></a>
### POST /sni/api/v1/payments/mobile-money/pawaypay/test/{phoneNumber}

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `MobileMoneyTestDepositResponse`
- Modèles : [MobileMoneyTestDepositResponse](#m-MobileMoneyTestDepositResponse), [CongoCorrespondent](#m-CongoCorrespondent)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `testDeposit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `phoneNumber` | chemin | `String` | oui |  |
| `operator` | requête | `CongoCorrespondent` |  | MTN_MOMO_COG |
| `amount` | requête | `BigDecimal` |  | 10 |

<a id="r-get-sni-api-v1-payments-mobile-money-providers"></a>
### GET /sni/api/v1/payments/mobile-money/providers

- Accès : public, sans jeton
- Réponse : `List<MobileMoneyProviderOptionResponse>`
- Modèles : [MobileMoneyProviderOptionResponse](#m-MobileMoneyProviderOptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java`, méthode `providers`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MobileMoneyCallbackResponse"></a>
#### MobileMoneyCallbackResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `referenceId` | texte |  |  |
| `reportedStatus` | texte |  |  |
| `signaturePresent` | booléen |  |  |
| `verifiedVia` | texte |  |  |
| `outcome` | texte |  |  |
| `detail` | texte |  |  |
| `remoteAddress` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `processedAt` | date-heure ISO 8601 UTC |  |  |

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

<a id="m-MobileMoneyTestDepositResponse"></a>
#### MobileMoneyTestDepositResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `deposit` | [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse) |  |  |
| `intent` | [PaymentIntentResponse](#m-PaymentIntentResponse) |  |  |
| `providers` | [MobileMoneyProviderOptionResponse](#m-MobileMoneyProviderOptionResponse)[] |  |  |

<a id="m-CongoCorrespondent"></a>
#### CongoCorrespondent

Énumération : `MTN_MOMO_CIV`, `ORANGE_CIV`, `WAVE_CIV`, `AIRTEL_COG`, `MTN_MOMO_COG`, `FREE_SEN`, `ORANGE_SEN`, `WAVE_SEN`, `ORANGE_COD`, `AIRTEL_COD`, `VODACOM_MPESA_COD`, `AIRTEL_GAB`, `MTN_MOMO_CMR`, `AIRTEL_OAPI_COG`, `AIRTEL_OAPI_COD`, `MPESA_COD`

<a id="m-MobileMoneyProviderOptionResponse"></a>
#### MobileMoneyProviderOptionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `displayName` | texte |  |  |
| `countryCode` | texte |  |  |
| `currency` | texte |  |  |

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
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
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

<a id="m-PaymentIntentResponse"></a>
#### PaymentIntentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `intentNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `customerRegistered` | booléen |  |  |
| `amount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `processingAmount` | nombre décimal |  |  |
| `remainingAmount` | nombre décimal |  |  |
| `payableAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `AUTHORIZED`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `REVERSED` |  |  |
| `purpose` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `resolvedSourceType` | texte |  |  |
| `resolvedSourceCode` | texte |  |  |
| `resolvedSourceLabel` | texte |  |  |
| `resolvedSourceRegistered` | booléen |  |  |
| `paymentLinkToken` | texte |  |  |
| `paymentLinkExpiresAt` | date-heure ISO 8601 UTC |  |  |
| `idempotencyKey` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |

<a id="m-MobileMoneyDepositResponse-AccountDetailsResponse"></a>
#### MobileMoneyDepositResponse.AccountDetailsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte |  |  |
| `provider` | texte |  |  |
