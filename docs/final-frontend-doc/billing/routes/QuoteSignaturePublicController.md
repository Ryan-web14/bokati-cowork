# QuoteSignaturePublicController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 2 route(s).

## Routes

<a id="r-get-public-quotes-sign-token"></a>
### GET /public/quotes/sign/{token}

- Accès : public, sans jeton
- Renvoie : `text/html`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteSignaturePublicController.java`, méthode `signaturePage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

<a id="r-post-public-quotes-sign-token-confirm"></a>
### POST /public/quotes/sign/{token}/confirm

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `ConfirmSignatureRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [ConfirmSignatureRequest](#m-ConfirmSignatureRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteSignaturePublicController.java`, méthode `confirmSignature`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BillingDocumentResponse"></a>
#### BillingDocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `VOIDED`, `REFUNDED`, `WRITTEN_OFF` |  |  |
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

<a id="m-ConfirmSignatureRequest"></a>
#### ConfirmSignatureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerName` | texte | oui | longueur min `1` |
| `signerEmail` | texte |  |  |
| `signatureImageBase64` | texte | oui | longueur min `1` |
| `ipAddress` | texte |  |  |
| `userAgent` | texte |  |  |

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
| `serviceStart` | date AAAA-MM-JJ |  |  |
| `serviceEnd` | date AAAA-MM-JJ |  |  |
| `servicePeriod` | texte |  |  |

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
| `status` | enum : `PENDING`, `SIGNED`, `EXPIRED` |  |  |
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
