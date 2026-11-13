# ClientQuoteController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-client-billing-claim-reasons"></a>
### GET /sni/api/v1/client/billing/claim-reasons

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClaimDtos.ReasonView>`
- Modèles : [ClaimDtos.ReasonView](#m-ClaimDtos-ReasonView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `claimReasons`

<a id="r-get-sni-api-v1-client-billing-claims"></a>
### GET /sni/api/v1/client/billing/claims

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClaimDtos.ClaimView>`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `claims`

<a id="r-post-sni-api-v1-client-billing-claims-claimnumber-withdraw"></a>
### POST /sni/api/v1/client/billing/claims/{claimNumber}/withdraw

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `withdraw`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `claimNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-billing-invoices-documentnumber-claims"></a>
### POST /sni/api/v1/client/billing/invoices/{documentNumber}/claims

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClaimDtos.OpenRequest`
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView), [ClaimDtos.OpenRequest](#m-ClaimDtos-OpenRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `openClaim`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-billing-proformas-documentnumber-pay-mobile-money"></a>
### POST /sni/api/v1/client/billing/proformas/{documentNumber}/pay/mobile-money

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientInitiateMobileMoneyPaymentRequest`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse), [ClientInitiateMobileMoneyPaymentRequest](#m-ClientInitiateMobileMoneyPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `payProforma`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-billing-quotes"></a>
### GET /sni/api/v1/client/billing/quotes

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<ClientInvoiceSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ClientInvoiceSummaryResponse](#m-ClientInvoiceSummaryResponse), [BillingDocumentType](#m-BillingDocumentType)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | requête | `BillingDocumentType` |  | QUOTE |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-client-billing-quotes-documentnumber"></a>
### GET /sni/api/v1/client/billing/quotes/{documentNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientInvoiceResponse`
- Modèles : [ClientInvoiceResponse](#m-ClientInvoiceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-billing-quotes-documentnumber-accept"></a>
### POST /sni/api/v1/client/billing/quotes/{documentNumber}/accept

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientInvoiceResponse`
- Modèles : [ClientInvoiceResponse](#m-ClientInvoiceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-billing-quotes-documentnumber-pdf"></a>
### GET /sni/api/v1/client/billing/quotes/{documentNumber}/pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `pdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-billing-quotes-documentnumber-reject"></a>
### POST /sni/api/v1/client/billing/quotes/{documentNumber}/reject

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientInvoiceResponse`
- Modèles : [ClientInvoiceResponse](#m-ClientInvoiceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/controller/ClientQuoteController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClaimDtos-ReasonView"></a>
#### ClaimDtos.ReasonView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-ClaimDtos-ClaimView"></a>
#### ClaimDtos.ClaimView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `claimNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `reasonCode` | texte |  |  |
| `reasonLabel` | texte |  |  |
| `description` | texte |  |  |
| `lineOrders` | entier[] |  |  |
| `requestedAmount` | nombre décimal |  |  |
| `status` | texte |  |  |
| `channel` | texte |  |  |
| `openedBy` | texte |  |  |
| `grantedAmount` | nombre décimal |  |  |
| `creditNoteNumber` | texte |  |  |
| `decisionComment` | texte |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ClaimDtos-OpenRequest"></a>
#### ClaimDtos.OpenRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte | oui | longueur min `1` |
| `description` | texte |  | longueur min `0`, longueur max `4000` |
| `lineOrders` | entier[] |  |  |
| `requestedAmount` | nombre décimal |  | min `0` |

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

<a id="m-ClientInitiateMobileMoneyPaymentRequest"></a>
#### ClientInitiateMobileMoneyPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte | oui | longueur min `1` |
| `correspondent` | enum : `MTN_MOMO_CIV`, `ORANGE_CIV`, `WAVE_CIV`, `AIRTEL_COG`, `MTN_MOMO_COG`, `FREE_SEN`, `ORANGE_SEN`, `WAVE_SEN`, `ORANGE_COD`, `AIRTEL_COD`, `VODACOM_MPESA_COD`, `AIRTEL_GAB`, `MTN_MOMO_CMR`, `AIRTEL_OAPI_COG`, `AIRTEL_OAPI_COD`, `MPESA_COD` | oui |  |

<a id="m-ClientInvoiceSummaryResponse"></a>
#### ClientInvoiceSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `REFUNDED`, `APPLIED`, `WRITTEN_OFF` |  |  |
| `title` | texte |  |  |
| `currency` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `resolvedSourceLabel` | texte |  |  |

<a id="m-BillingDocumentType"></a>
#### BillingDocumentType

Énumération : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE`

<a id="m-ClientInvoiceResponse"></a>
#### ClientInvoiceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `REFUNDED`, `APPLIED`, `WRITTEN_OFF` |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `paymentReference` | texte |  |  |
| `paymentInstructions` | texte |  |  |
| `bankDetailsJson` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `resolvedSourceLabel` | texte |  |  |
| `lines` | [BillingDocumentLineResponse](#m-BillingDocumentLineResponse)[] |  |  |

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

<a id="m-MobileMoneyDepositResponse-AccountDetailsResponse"></a>
#### MobileMoneyDepositResponse.AccountDetailsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte |  |  |
| `provider` | texte |  |  |
