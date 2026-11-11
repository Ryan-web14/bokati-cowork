# ClientCompanyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies"></a>
### GET /sni/api/v1/client/companies

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<MembershipDtos.MyCompany>`
- Modèles : [MembershipDtos.MyCompany](#m-MembershipDtos-MyCompany)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `myCompanies`

<a id="r-patch-sni-api-v1-client-companies-customerid-invoice-link-mode"></a>
### PATCH /sni/api/v1/client/companies/{customerId}/invoice-link-mode

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `MembershipDtos.CompanyModeRequest`
- Réponse : `MembershipDtos.MyCompany`
- Modèles : [MembershipDtos.MyCompany](#m-MembershipDtos-MyCompany), [MembershipDtos.CompanyModeRequest](#m-MembershipDtos-CompanyModeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `setDefaultMode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-invoices-documentnumber"></a>
### GET /sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientInvoiceResponse`
- Modèles : [ClientInvoiceResponse](#m-ClientInvoiceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `invoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-invoices-documentnumber-pay-cash"></a>
### POST /sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}/pay/cash

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `DeclareCashPaymentRequest`
- Réponse : `CashPaymentDeclarationResponse`
- Modèles : [CashPaymentDeclarationResponse](#m-CashPaymentDeclarationResponse), [DeclareCashPaymentRequest](#m-DeclareCashPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `declareCashPayment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-invoices-documentnumber-pay-mobile-money"></a>
### POST /sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}/pay/mobile-money

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientInitiateMobileMoneyPaymentRequest`
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse), [ClientInitiateMobileMoneyPaymentRequest](#m-ClientInitiateMobileMoneyPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `payWithMobileMoney`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-invoices-documentnumber-pdf"></a>
### GET /sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}/pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `invoicePdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-members"></a>
### GET /sni/api/v1/client/companies/{customerId}/members

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<MembershipDtos.LinkResponse>`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `members`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-client-companies-customerid-members-linkid-invoice-link-mode"></a>
### PATCH /sni/api/v1/client/companies/{customerId}/members/{linkId}/invoice-link-mode

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `MembershipDtos.LinkModeRequest`
- Réponse : `MembershipDtos.LinkResponse`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse), [MembershipDtos.LinkModeRequest](#m-MembershipDtos-LinkModeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `setLinkMode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `linkId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-mobile-money-deposits-depositid"></a>
### GET /sni/api/v1/client/companies/{customerId}/mobile-money/deposits/{depositId}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `MobileMoneyDepositResponse`
- Modèles : [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `mobileMoneyDeposit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `depositId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-payments-transactionnumber-receipt"></a>
### GET /sni/api/v1/client/companies/{customerId}/payments/{transactionNumber}/receipt

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaymentReceiptResponse`
- Modèles : [PaymentReceiptResponse](#m-PaymentReceiptResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `receipt`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-payments-transactionnumber-receipt-pdf"></a>
### GET /sni/api/v1/client/companies/{customerId}/payments/{transactionNumber}/receipt/pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `receiptPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-statement"></a>
### GET /sni/api/v1/client/companies/{customerId}/statement

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `StatementDtos.Statement`
- Modèles : [StatementDtos.Statement](#m-StatementDtos-Statement)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `statement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-client-companies-customerid-statement-pdf"></a>
### GET /sni/api/v1/client/companies/{customerId}/statement.pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyController.java`, méthode `statementPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MembershipDtos-MyCompany"></a>
#### MembershipDtos.MyCompany

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerId` | texte |  |  |
| `name` | texte |  |  |
| `role` | enum : `OWNER`, `EMPLOYEE` |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `effectiveInvoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `companyDefaultMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |

<a id="m-MembershipDtos-CompanyModeRequest"></a>
#### MembershipDtos.CompanyModeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mode` | enum : `AUTOMATIC`, `ON_CHOICE` | oui |  |

<a id="m-ClientInvoiceResponse"></a>
#### ClientInvoiceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `VOIDED`, `REFUNDED`, `WRITTEN_OFF` |  |  |
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

<a id="m-CashPaymentDeclarationResponse"></a>
#### CashPaymentDeclarationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `declarationNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `bookingNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `AWAITING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, `EXPIRED` |  |  |
| `note` | texte |  |  |
| `declaredAt` | date-heure ISO 8601 UTC |  |  |
| `declaredBy` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `confirmedAt` | date-heure ISO 8601 UTC |  |  |
| `confirmedBy` | texte |  |  |
| `confirmedAmount` | nombre décimal |  |  |
| `cashSessionNumber` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `closedBy` | texte |  |  |
| `closeReason` | texte |  |  |

<a id="m-DeclareCashPaymentRequest"></a>
#### DeclareCashPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal |  |  |
| `note` | texte |  | longueur min `0`, longueur max `500` |

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

<a id="m-MembershipDtos-LinkResponse"></a>
#### MembershipDtos.LinkResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `customerId` | texte |  |  |
| `companyName` | texte |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `memberEmail` | texte |  |  |
| `role` | enum : `OWNER`, `EMPLOYEE` |  |  |
| `status` | enum : `ACTIVE`, `ENDED` |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `endedAt` | date-heure ISO 8601 UTC |  |  |
| `endedReason` | texte |  |  |
| `invoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `effectiveInvoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `canSign` | booléen |  |  |
| `canCollectMail` | booléen |  |  |
| `payerChanged` | booléen |  |  |
| `warning` | texte |  |  |

<a id="m-MembershipDtos-LinkModeRequest"></a>
#### MembershipDtos.LinkModeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `useCompanyDefaultMode` | booléen |  |  |

<a id="m-PaymentReceiptResponse"></a>
#### PaymentReceiptResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `receiptNumber` | texte |  |  |
| `receiptIssuedAt` | date-heure ISO 8601 UTC |  |  |
| `transactionNumber` | texte |  |  |
| `intentNumber` | texte |  |  |
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
| `transactionStatus` | enum : `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `REFUNDED`, `REVERSED` |  |  |
| `provider` | texte |  |  |
| `providerReference` | texte |  |  |
| `depositId` | texte |  |  |
| `payerPhone` | texte |  |  |
| `operatorReference` | texte |  |  |
| `purpose` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `paidAmount` | nombre décimal |  |  |
| `allocatedAmount` | nombre décimal |  |  |
| `advanceAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `receivedBy` | texte |  |  |
| `allocations` | [PaymentReceiptAllocationResponse](#m-PaymentReceiptAllocationResponse)[] |  |  |
| `paymentMethodLabel` | texte |  |  |
| `tenderedAmount` | nombre décimal |  |  |
| `changeAmount` | nombre décimal |  |  |
| `bankName` | texte |  |  |
| `payerName` | texte |  |  |

<a id="m-StatementDtos-Statement"></a>
#### StatementDtos.Statement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subject` | enum : `COMPANY`, `MEMBER` |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `billingEmail` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `asOf` | date AAAA-MM-JJ |  |  |
| `creditLimit` | nombre décimal |  |  |
| `paymentTermsDays` | entier |  |  |
| `blocked` | booléen |  |  |
| `blockedReason` | texte |  |  |
| `totals` | [StatementDtos.Totals](#m-StatementDtos-Totals)[] |  |  |
| `accounts` | [StatementDtos.AccountBreakdown](#m-StatementDtos-AccountBreakdown)[] |  |  |
| `lines` | [StatementDtos.Line](#m-StatementDtos-Line)[] |  |  |

<a id="m-BillingDocumentLineResponse"></a>
#### BillingDocumentLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineOrder` | entier |  |  |
| `lineType` | enum : `PRODUCT`, `SERVICE`, `SUBSCRIPTION`, `PASS`, `ADDON`, `BOOKING`, `OVERAGE`, `PENALTY`, `WALLET`, `ADJUSTMENT` |  |  |
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

<a id="m-PaymentReceiptAllocationResponse"></a>
#### PaymentReceiptAllocationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `title` | texte |  |  |
| `documentTotalAmount` | nombre décimal |  |  |
| `allocatedAmount` | nombre décimal |  |  |
| `remainingBalanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |

<a id="m-StatementDtos-Totals"></a>
#### StatementDtos.Totals

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `invoiced` | nombre décimal |  |  |
| `paid` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `overdue` | nombre décimal |  |  |
| `creditAvailable` | nombre décimal |  |  |
| `netBalanceDue` | nombre décimal |  |  |
| `openInvoices` | entier |  |  |
| `overdueInvoices` | entier |  |  |
| `aging` | [StatementDtos.Aging](#m-StatementDtos-Aging) |  |  |

<a id="m-StatementDtos-AccountBreakdown"></a>
#### StatementDtos.AccountBreakdown

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountType` | enum : `COMPANY`, `MEMBER`, `PERSONAL` |  |  |
| `accountCode` | texte |  |  |
| `accountName` | texte |  |  |
| `currency` | texte |  |  |
| `invoiced` | nombre décimal |  |  |
| `paid` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `overdue` | nombre décimal |  |  |
| `documentCount` | entier |  |  |

<a id="m-StatementDtos-Line"></a>
#### StatementDtos.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | texte |  |  |
| `status` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `currency` | texte |  |  |
| `accountType` | enum : `COMPANY`, `MEMBER`, `PERSONAL` |  |  |
| `accountCode` | texte |  |  |
| `accountName` | texte |  |  |
| `companyLinkSource` | texte |  |  |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `impact` | nombre décimal |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |
| `inPeriod` | booléen |  |  |

<a id="m-MobileMoneyDepositResponse-AccountDetailsResponse"></a>
#### MobileMoneyDepositResponse.AccountDetailsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte |  |  |
| `provider` | texte |  |  |

<a id="m-StatementDtos-Aging"></a>
#### StatementDtos.Aging

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notDue` | nombre décimal |  |  |
| `days1To30` | nombre décimal |  |  |
| `days31To60` | nombre décimal |  |  |
| `days61To90` | nombre décimal |  |  |
| `over90` | nombre décimal |  |  |
