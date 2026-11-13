# QuoteTemplateController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-catalog-itemcode-components"></a>
### GET /sni/api/v1/billing/catalog/{itemCode}/components

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<QuoteTemplateDtos.Component>`
- Modèles : [QuoteTemplateDtos.Component](#m-QuoteTemplateDtos-Component)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteTemplateController.java`, méthode `components`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-catalog-itemcode-components"></a>
### PUT /sni/api/v1/billing/catalog/{itemCode}/components

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `QuoteTemplateDtos.ComponentsRequest`
- Réponse : `List<QuoteTemplateDtos.Component>`
- Modèles : [QuoteTemplateDtos.Component](#m-QuoteTemplateDtos-Component), [QuoteTemplateDtos.ComponentsRequest](#m-QuoteTemplateDtos-ComponentsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteTemplateController.java`, méthode `setComponents`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-quote-templates"></a>
### GET /sni/api/v1/billing/quote-templates

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<QuoteTemplateDtos.TemplateView>`
- Modèles : [QuoteTemplateDtos.TemplateView](#m-QuoteTemplateDtos-TemplateView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteTemplateController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-billing-quote-templates"></a>
### PUT /sni/api/v1/billing/quote-templates

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `QuoteTemplateDtos.TemplateRequest`
- Réponse : `QuoteTemplateDtos.TemplateView`
- Modèles : [QuoteTemplateDtos.TemplateView](#m-QuoteTemplateDtos-TemplateView), [QuoteTemplateDtos.TemplateRequest](#m-QuoteTemplateDtos-TemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteTemplateController.java`, méthode `save`

<a id="r-get-sni-api-v1-billing-quote-templates-code"></a>
### GET /sni/api/v1/billing/quote-templates/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `QuoteTemplateDtos.TemplateView`
- Modèles : [QuoteTemplateDtos.TemplateView](#m-QuoteTemplateDtos-TemplateView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteTemplateController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-quotes-from-template-code"></a>
### POST /sni/api/v1/billing/quotes/from-template/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `QuoteTemplateDtos.FromTemplateRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [QuoteTemplateDtos.FromTemplateRequest](#m-QuoteTemplateDtos-FromTemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteTemplateController.java`, méthode `fromTemplate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-QuoteTemplateDtos-Component"></a>
#### QuoteTemplateDtos.Component

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `componentCode` | texte | oui | longueur min `1` |
| `componentSource` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui | min `0.0001` |

<a id="m-QuoteTemplateDtos-ComponentsRequest"></a>
#### QuoteTemplateDtos.ComponentsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `components` | [QuoteTemplateDtos.Component](#m-QuoteTemplateDtos-Component)[] | oui |  |

<a id="m-QuoteTemplateDtos-TemplateView"></a>
#### QuoteTemplateDtos.TemplateView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `validityDays` | entier |  |  |
| `terms` | texte |  |  |
| `lines` | [QuoteTemplateDtos.TemplateLine](#m-QuoteTemplateDtos-TemplateLine)[] |  |  |
| `active` | booléen |  |  |
| `createdBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-QuoteTemplateDtos-TemplateRequest"></a>
#### QuoteTemplateDtos.TemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `60` |
| `name` | texte | oui | longueur min `0`, longueur max `255` |
| `description` | texte |  |  |
| `validityDays` | entier |  | min `1`, max `365` |
| `terms` | texte |  |  |
| `lines` | [QuoteTemplateDtos.TemplateLine](#m-QuoteTemplateDtos-TemplateLine)[] | oui | éléments min `1` |
| `active` | booléen |  |  |

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

<a id="m-QuoteTemplateDtos-FromTemplateRequest"></a>
#### QuoteTemplateDtos.FromTemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte | oui | longueur min `1` |
| `customerCode` | texte | oui | longueur min `1` |
| `customerName` | texte | oui | longueur min `1` |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `segment` | texte |  |  |
| `title` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-QuoteTemplateDtos-TemplateLine"></a>
#### QuoteTemplateDtos.TemplateLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui | min `0.0001` |
| `description` | texte |  | longueur min `0`, longueur max `4000` |

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
