# PaymentController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 22 route(s).

## Routes

<a id="r-get-sni-api-v1-payments-billing-documents-documentnumber-cash-change"></a>
### GET /sni/api/v1/payments/billing-documents/{documentNumber}/cash-change

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `CashChangeResponse`
- Modèles : [CashChangeResponse](#m-CashChangeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `previewDocumentCashChange`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `tenderedAmount` | requête | `BigDecimal` | oui |  |
| `amount` | requête | `BigDecimal` |  |  |

<a id="r-get-sni-api-v1-payments-intents"></a>
### GET /sni/api/v1/payments/intents

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaginatedResponse<PaymentIntentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PaymentIntentResponse](#m-PaymentIntentResponse), [PaymentIntentStatus](#m-PaymentIntentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `listIntents`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `PaymentIntentStatus` |  |  |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `sourceType` | requête | `String` |  |  |
| `sourceCode` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-payments-intents"></a>
### POST /sni/api/v1/payments/intents

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CreatePaymentIntentRequest`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse), [CreatePaymentIntentRequest](#m-CreatePaymentIntentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `createIntent`

<a id="r-post-sni-api-v1-payments-intents-from-billing-document"></a>
### POST /sni/api/v1/payments/intents/from-billing-document

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CreatePaymentIntentFromBillingDocumentRequest`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse), [CreatePaymentIntentFromBillingDocumentRequest](#m-CreatePaymentIntentFromBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `createIntentFromBillingDocument`

<a id="r-post-sni-api-v1-payments-intents-from-billing-documents"></a>
### POST /sni/api/v1/payments/intents/from-billing-documents

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CreatePaymentIntentForDocumentsRequest`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse), [CreatePaymentIntentForDocumentsRequest](#m-CreatePaymentIntentForDocumentsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `createIntentForBillingDocuments`

<a id="r-post-sni-api-v1-payments-intents-recovery"></a>
### POST /sni/api/v1/payments/intents/recovery

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CreatePaymentRecoveryIntentRequest`
- Réponse : `PaymentRecoveryResponse`
- Modèles : [PaymentRecoveryResponse](#m-PaymentRecoveryResponse), [CreatePaymentRecoveryIntentRequest](#m-CreatePaymentRecoveryIntentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `createRecoveryIntent`

<a id="r-get-sni-api-v1-payments-intents-intentnumber"></a>
### GET /sni/api/v1/payments/intents/{intentNumber}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `getIntent`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-intents-intentnumber-cancel"></a>
### POST /sni/api/v1/payments/intents/{intentNumber}/cancel

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (facultatif) : `CancelPaymentIntentRequest`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse), [CancelPaymentIntentRequest](#m-CancelPaymentIntentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `cancelIntent`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-payments-intents-intentnumber-cash"></a>
### PATCH /sni/api/v1/payments/intents/{intentNumber}/cash

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `RegisterCashPaymentRequest`
- Réponse : `PaymentTransactionResponse`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse), [RegisterCashPaymentRequest](#m-RegisterCashPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `registerCashPayment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-payments-intents-intentnumber-cash-change"></a>
### GET /sni/api/v1/payments/intents/{intentNumber}/cash-change

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `CashChangeResponse`
- Modèles : [CashChangeResponse](#m-CashChangeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `previewIntentCashChange`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |
| `tenderedAmount` | requête | `BigDecimal` | oui |  |
| `amount` | requête | `BigDecimal` |  |  |

<a id="r-post-sni-api-v1-payments-intents-intentnumber-link"></a>
### POST /sni/api/v1/payments/intents/{intentNumber}/link

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (facultatif) : `CreatePaymentLinkRequest`
- Réponse : `PaymentLinkResponse`
- Modèles : [PaymentLinkResponse](#m-PaymentLinkResponse), [CreatePaymentLinkRequest](#m-CreatePaymentLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `createPaymentLink`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-payments-intents-intentnumber-mobile-money"></a>
### PATCH /sni/api/v1/payments/intents/{intentNumber}/mobile-money

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `InitiateMobileMoneyDepositRequest`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse), [InitiateMobileMoneyDepositRequest](#m-InitiateMobileMoneyDepositRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `initiateMobileMoneyDeposit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-payments-intents-intentnumber-offline"></a>
### PATCH /sni/api/v1/payments/intents/{intentNumber}/offline

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `RegisterOfflinePaymentRequest`
- Réponse : `PaymentTransactionResponse`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse), [RegisterOfflinePaymentRequest](#m-RegisterOfflinePaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `registerOfflinePayment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-intents-intentnumber-relaunch"></a>
### POST /sni/api/v1/payments/intents/{intentNumber}/relaunch

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (facultatif) : `RelaunchPaymentIntentRequest`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse), [RelaunchPaymentIntentRequest](#m-RelaunchPaymentIntentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `relaunchIntent`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-payments-intents-intentnumber-transactions"></a>
### GET /sni/api/v1/payments/intents/{intentNumber}/transactions

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `List<PaymentTransactionResponse>`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `listIntentTransactions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-payments-intents-intentnumber-wallet"></a>
### PATCH /sni/api/v1/payments/intents/{intentNumber}/wallet

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `WalletPaymentRequest`
- Réponse : `PaymentTransactionResponse`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse), [WalletPaymentRequest](#m-WalletPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `payWithWallet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `intentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-mobile-money-deposits"></a>
### POST /sni/api/v1/payments/mobile-money/deposits

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `InitiateMobileMoneyDepositRequest`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse), [InitiateMobileMoneyDepositRequest](#m-InitiateMobileMoneyDepositRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `initiateMobileMoneyDeposit`

<a id="r-get-sni-api-v1-payments-payable-documents"></a>
### GET /sni/api/v1/payments/payable-documents

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaginatedResponse<BillingDocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BillingDocumentResponse](#m-BillingDocumentResponse), [BillingDocumentType](#m-BillingDocumentType)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `listPayableDocuments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentType` | requête | `BillingDocumentType` |  |  |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `lineSourceType` | requête | `String` |  |  |
| `lineSourceCode` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-payments-recovery-customertype-customercode"></a>
### GET /sni/api/v1/payments/recovery/{customerType}/{customerCode}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaymentRecoveryResponse`
- Modèles : [PaymentRecoveryResponse](#m-PaymentRecoveryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `previewRecovery`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |
| `currency` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-payments-transactions-transactionnumber-refund"></a>
### POST /sni/api/v1/payments/transactions/{transactionNumber}/refund

- Accès : back-office : rôle d'administration et droit `PAYMENT:REFUND`
- Corps JSON (obligatoire) : `RefundPaymentRequest`
- Réponse : `PaymentTransactionResponse`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse), [RefundPaymentRequest](#m-RefundPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `refund`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-payments-transactions-transactionnumber-refunds"></a>
### GET /sni/api/v1/payments/transactions/{transactionNumber}/refunds

- Accès : back-office : rôle d'administration et droit `PAYMENT:REFUND`
- Réponse : `List<PaymentTransactionResponse>`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `listRefunds`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-transactions-transactionnumber-reverse"></a>
### POST /sni/api/v1/payments/transactions/{transactionNumber}/reverse

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `RefundPaymentRequest`
- Réponse : `PaymentTransactionResponse`
- Modèles : [PaymentTransactionResponse](#m-PaymentTransactionResponse), [RefundPaymentRequest](#m-RefundPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java`, méthode `reverse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashChangeResponse"></a>
#### CashChangeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `amountDue` | nombre décimal |  |  |
| `tenderedAmount` | nombre décimal |  |  |
| `appliedAmount` | nombre décimal |  |  |
| `changeAmount` | nombre décimal |  |  |
| `remainingAfter` | nombre décimal |  |  |

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
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledBy` | texte |  |  |
| `cancellationReason` | texte |  |  |
| `relaunchCount` | entier |  |  |
| `lastRelaunchedAt` | date-heure ISO 8601 UTC |  |  |
| `lastRelaunchedBy` | texte |  |  |
| `relaunchable` | booléen |  |  |
| `cancellable` | booléen |  |  |

<a id="m-PaymentIntentStatus"></a>
#### PaymentIntentStatus

Énumération : `PENDING`, `PROCESSING`, `AUTHORIZED`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `REVERSED`

<a id="m-CreatePaymentIntentRequest"></a>
#### CreatePaymentIntentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte | oui | longueur min `1` |
| `customerCode` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `currency` | texte | oui | longueur min `1` |
| `purpose` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |

<a id="m-CreatePaymentIntentFromBillingDocumentRequest"></a>
#### CreatePaymentIntentFromBillingDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  |  |
| `idempotencyKey` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |

<a id="m-CreatePaymentIntentForDocumentsRequest"></a>
#### CreatePaymentIntentForDocumentsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumbers` | texte[] | oui | éléments min `1` |
| `amount` | nombre décimal |  |  |
| `idempotencyKey` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |

<a id="m-PaymentRecoveryResponse"></a>
#### PaymentRecoveryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `currency` | texte |  |  |
| `openDocumentAmount` | nombre décimal |  |  |
| `pendingBillableAmount` | nombre décimal |  |  |
| `totalPayableAmount` | nombre décimal |  |  |
| `documents` | [BillingDocumentResponse](#m-BillingDocumentResponse)[] |  |  |
| `pendingBillableItems` | [BillableItemResponse](#m-BillableItemResponse)[] |  |  |
| `paymentIntent` | [PaymentIntentResponse](#m-PaymentIntentResponse) |  |  |

<a id="m-CreatePaymentRecoveryIntentRequest"></a>
#### CreatePaymentRecoveryIntentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte | oui | longueur min `1` |
| `customerCode` | texte | oui | longueur min `1` |
| `currency` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |

<a id="m-CancelPaymentIntentRequest"></a>
#### CancelPaymentIntentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

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

<a id="m-RegisterCashPaymentRequest"></a>
#### RegisterCashPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `receivedBy` | texte |  |  |
| `cashSessionNumber` | texte | oui |  |
| `amount` | nombre décimal |  |  |
| `providerReference` | texte |  |  |
| `metadataJson` | texte |  |  |
| `tenderedAmount` | nombre décimal |  |  |

<a id="m-PaymentLinkResponse"></a>
#### PaymentLinkResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `intentNumber` | texte |  |  |
| `checkoutUrl` | texte |  |  |
| `depositId` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `token` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `emailedTo` | texte |  |  |

<a id="m-CreatePaymentLinkRequest"></a>
#### CreatePaymentLinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `expiresInMinutes` | entier |  | min `5` |

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

<a id="m-InitiateMobileMoneyDepositRequest"></a>
#### InitiateMobileMoneyDepositRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `intentNumber` | texte |  |  |
| `phoneNumber` | texte | oui | longueur min `1` |
| `correspondent` | enum : `MTN_MOMO_CIV`, `ORANGE_CIV`, `WAVE_CIV`, `AIRTEL_COG`, `MTN_MOMO_COG`, `FREE_SEN`, `ORANGE_SEN`, `WAVE_SEN`, `ORANGE_COD`, `AIRTEL_COD`, `VODACOM_MPESA_COD`, `AIRTEL_GAB`, `MTN_MOMO_CMR`, `AIRTEL_OAPI_COG`, `AIRTEL_OAPI_COD`, `MPESA_COD` | oui |  |
| `amount` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `metadataJson` | texte |  |  |

<a id="m-RegisterOfflinePaymentRequest"></a>
#### RegisterOfflinePaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` | oui |  |
| `amount` | nombre décimal |  |  |
| `reference` | texte |  | longueur min `0`, longueur max `120` |
| `bankName` | texte |  | longueur min `0`, longueur max `180` |
| `payerName` | texte |  | longueur min `0`, longueur max `180` |
| `payerPhone` | texte |  | longueur min `0`, longueur max `40` |
| `operator` | enum : `MTN_MOMO`, `AIRTEL_MONEY` |  |  |
| `cashSessionNumber` | texte |  |  |
| `receivedBy` | texte |  |  |
| `metadataJson` | texte |  |  |

<a id="m-RelaunchPaymentIntentRequest"></a>
#### RelaunchPaymentIntentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletPaymentRequest"></a>
#### WalletPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `metadataJson` | texte |  |  |

<a id="m-BillingDocumentResponse"></a>
#### BillingDocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `REFUNDED`, `APPLIED`, `WRITTEN_OFF` |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `customerRegistered` | booléen |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `resolvedSourceType` | texte |  |  |
| `resolvedSourceCode` | texte |  |  |
| `resolvedSourceLabel` | texte |  |  |
| `resolvedSourceRegistered` | booléen |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `terms` | texte |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxableAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `additionalCentAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `optionsTotal` | nombre décimal |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `customerReference` | texte |  |  |
| `poNumber` | texte |  |  |
| `projectCode` | texte |  |  |
| `salespersonCode` | texte |  |  |
| `deliveryAddressJson` | texte |  |  |
| `language` | texte |  |  |
| `exchangeRate` | nombre décimal |  |  |
| `paymentReference` | texte |  |  |
| `paymentInstructions` | texte |  |  |
| `bankDetailsJson` | texte |  |  |
| `metadataJson` | texte |  |  |
| `lines` | [BillingDocumentLineResponse](#m-BillingDocumentLineResponse)[] |  |  |
| `discounts` | [BillingDocumentDiscountResponse](#m-BillingDocumentDiscountResponse)[] |  |  |
| `taxes` | [BillingDocumentTaxResponse](#m-BillingDocumentTaxResponse)[] |  |  |
| `clauses` | [BillingDocumentClauseResponse](#m-BillingDocumentClauseResponse)[] |  |  |
| `advance` | [BillingDocumentAdvanceResponse](#m-BillingDocumentAdvanceResponse) |  |  |
| `earlyPaymentDiscount` | [EarlyPaymentDiscountResponse](#m-EarlyPaymentDiscountResponse) |  |  |
| `signature` | [BillingDocumentSignatureResponse](#m-BillingDocumentSignatureResponse) |  |  |
| `internalNotes` | texte |  |  |
| `recoverables` | [BillingRecoverableResponse](#m-BillingRecoverableResponse)[] |  |  |
| `locked` | booléen |  |  |
| `fiscalNumber` | texte |  |  |
| `fiscalDate` | date AAAA-MM-JJ |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `sellerName` | texte |  |  |
| `sellerNiu` | texte |  |  |
| `sellerPhone` | texte |  |  |
| `sellerEmail` | texte |  |  |
| `customerNiu` | texte |  |  |
| `customerCategory` | texte |  |  |
| `previousHash` | texte |  |  |
| `currentHash` | texte |  |  |
| `fiscalSignature` | texte |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `originalDocumentNumber` | texte |  |  |
| `originalDocumentType` | texte |  |  |
| `creditNoteReason` | texte |  |  |
| `receivable` | booléen |  |  |
| `customerImpact` | nombre décimal |  |  |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `companyLinkSource` | texte |  |  |
| `author` | [BillingDocumentAuthorResponse](#m-BillingDocumentAuthorResponse) |  |  |

<a id="m-BillingDocumentType"></a>
#### BillingDocumentType

Énumération : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE`

<a id="m-RefundPaymentRequest"></a>
#### RefundPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal |  |  |
| `reason` | texte | oui | longueur min `1` |
| `processedBy` | texte |  |  |

<a id="m-BillableItemResponse"></a>
#### BillableItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billableNumber` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceId` | texte |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `description` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `taxCode` | texte |  |  |
| `billingPeriodStart` | date AAAA-MM-JJ |  |  |
| `billingPeriodEnd` | date AAAA-MM-JJ |  |  |
| `status` | enum : `PENDING`, `SENT_TO_INVOICE`, `INVOICED`, `CANCELLED` |  |  |
| `invoiceId` | entier int64 ⚠ |  |  |

<a id="m-MobileMoneyDepositResponse-PayerResponse"></a>
#### MobileMoneyDepositResponse.PayerResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `accountDetails` | [MobileMoneyDepositResponse.AccountDetailsResponse](#m-MobileMoneyDepositResponse-AccountDetailsResponse) |  |  |

<a id="m-BillingDocumentLineResponse"></a>
#### BillingDocumentLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineOrder` | entier |  |  |
| `lineType` | enum : `PRODUCT`, `SERVICE`, `SUBSCRIPTION`, `PASS`, `ADDON`, `BOOKING`, `OVERAGE`, `PENALTY`, `WALLET`, `ADJUSTMENT`, `DISBURSEMENT` |  |  |
| `itemCode` | texte |  |  |
| `category` | texte |  |  |
| `description` | texte |  |  |
| `detailedDescription` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `discountRate` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxable` | booléen |  |  |
| `taxIncluded` | booléen |  |  |
| `vatRate` | nombre décimal |  |  |
| `additionalCentRate` | nombre décimal |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxableAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `additionalCentAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `externalReference` | texte |  |  |
| `notes` | texte |  |  |
| `optional` | booléen |  |  |
| `serviceStart` | date AAAA-MM-JJ |  |  |
| `serviceEnd` | date AAAA-MM-JJ |  |  |
| `servicePeriod` | texte |  |  |
| `taxRuleCode` | texte |  |  |

<a id="m-BillingDocumentDiscountResponse"></a>
#### BillingDocumentDiscountResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `discountCode` | texte |  |  |
| `description` | texte |  |  |
| `discountType` | enum : `PERCENTAGE`, `FIXED_AMOUNT` |  |  |
| `value` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `baseAmount` | nombre décimal |  |  |

<a id="m-BillingDocumentTaxResponse"></a>
#### BillingDocumentTaxResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `taxCode` | texte |  |  |
| `taxName` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `taxableAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |

<a id="m-BillingDocumentClauseResponse"></a>
#### BillingDocumentClauseResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `clauseCode` | texte |  |  |
| `title` | texte |  |  |
| `body` | texte |  |  |
| `displayOrder` | entier |  |  |

<a id="m-BillingDocumentAdvanceResponse"></a>
#### BillingDocumentAdvanceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `advanceType` | enum : `PERCENTAGE`, `FIXED_AMOUNT` |  |  |
| `advanceValue` | nombre décimal |  |  |
| `computedAmount` | nombre décimal |  |  |
| `includedLineOrders` | entier[] |  |  |
| `excludedLineOrders` | entier[] |  |  |
| `paymentReference` | texte |  |  |
| `referenceLabel` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `PENDING`, `PARTIALLY_PAID`, `PAID`, `CANCELLED` |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `notes` | texte |  |  |

<a id="m-EarlyPaymentDiscountResponse"></a>
#### EarlyPaymentDiscountResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `discountRate` | nombre décimal |  |  |
| `ifPaidBefore` | date AAAA-MM-JJ |  |  |
| `computedAmount` | nombre décimal |  |  |
| `label` | texte |  |  |

<a id="m-BillingDocumentSignatureResponse"></a>
#### BillingDocumentSignatureResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerName` | texte |  |  |
| `signerEmail` | texte |  |  |
| `status` | enum : `PENDING`, `SIGNED`, `EXPIRED`, `CANCELLED` |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BillingRecoverableResponse"></a>
#### BillingRecoverableResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `recoverableNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `itemDescription` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `status` | texte |  |  |
| `notes` | texte |  |  |
| `recoveredAt` | date-heure ISO 8601 UTC |  |  |
| `recoveredBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BillingDocumentAuthorResponse"></a>
#### BillingDocumentAuthorResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `createdBy` | texte |  |  |
| `createdByName` | texte |  |  |
| `createdByTitle` | texte |  |  |
| `versionAuthor` | texte |  |  |
| `versionAuthorName` | texte |  |  |
| `versionAuthorTitle` | texte |  |  |

<a id="m-MobileMoneyDepositResponse-AccountDetailsResponse"></a>
#### MobileMoneyDepositResponse.AccountDetailsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte |  |  |
| `provider` | texte |  |  |
