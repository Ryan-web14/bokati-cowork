# DeliveryNoteController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-delivery-notes"></a>
### GET /sni/api/v1/billing/delivery-notes

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<DeliveryNoteDtos.NoteSummary>`
- Modèles : [DeliveryNoteDtos.NoteSummary](#m-DeliveryNoteDtos-NoteSummary)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `status` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-billing-delivery-notes-invoice"></a>
### POST /sni/api/v1/billing/delivery-notes/invoice

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `DeliveryNoteDtos.InvoiceRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [DeliveryNoteDtos.InvoiceRequest](#m-DeliveryNoteDtos-InvoiceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `invoice`

<a id="r-get-sni-api-v1-billing-delivery-notes-notenumber"></a>
### GET /sni/api/v1/billing/delivery-notes/{noteNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `DeliveryNoteDtos.NoteView`
- Modèles : [DeliveryNoteDtos.NoteView](#m-DeliveryNoteDtos-NoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `noteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-delivery-notes-notenumber-cancel"></a>
### PATCH /sni/api/v1/billing/delivery-notes/{noteNumber}/cancel

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Corps JSON (obligatoire) : `DeliveryNoteDtos.CancelRequest`
- Réponse : `DeliveryNoteDtos.NoteView`
- Modèles : [DeliveryNoteDtos.NoteView](#m-DeliveryNoteDtos-NoteView), [DeliveryNoteDtos.CancelRequest](#m-DeliveryNoteDtos-CancelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `noteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-delivery-notes-notenumber-deliver"></a>
### PATCH /sni/api/v1/billing/delivery-notes/{noteNumber}/deliver

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (facultatif) : `DeliveryNoteDtos.DeliverRequest`
- Réponse : `DeliveryNoteDtos.NoteView`
- Modèles : [DeliveryNoteDtos.NoteView](#m-DeliveryNoteDtos-NoteView), [DeliveryNoteDtos.DeliverRequest](#m-DeliveryNoteDtos-DeliverRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `deliver`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `noteNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-delivery-notes-notenumber-pdf"></a>
### GET /sni/api/v1/billing/delivery-notes/{noteNumber}/pdf

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `pdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `noteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-delivery-notes-notenumber-signed-copy"></a>
### PATCH /sni/api/v1/billing/delivery-notes/{noteNumber}/signed-copy

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `DeliveryNoteDtos.SignedCopyRequest`
- Réponse : `DeliveryNoteDtos.NoteView`
- Modèles : [DeliveryNoteDtos.NoteView](#m-DeliveryNoteDtos-NoteView), [DeliveryNoteDtos.SignedCopyRequest](#m-DeliveryNoteDtos-SignedCopyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `signedCopy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `noteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-delivery-notes"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/delivery-notes

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (facultatif) : `DeliveryNoteDtos.CreateRequest`
- Réponse : `DeliveryNoteDtos.NoteView`
- Modèles : [DeliveryNoteDtos.NoteView](#m-DeliveryNoteDtos-NoteView), [DeliveryNoteDtos.CreateRequest](#m-DeliveryNoteDtos-CreateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `create`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-delivery-status"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/delivery-status

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `DeliveryNoteDtos.DeliveryStatus`
- Modèles : [DeliveryNoteDtos.DeliveryStatus](#m-DeliveryNoteDtos-DeliveryStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/DeliveryNoteController.java`, méthode `status`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DeliveryNoteDtos-NoteSummary"></a>
#### DeliveryNoteDtos.NoteSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `noteNumber` | texte |  |  |
| `sourceDocumentNumber` | texte |  |  |
| `sourceDocumentType` | texte |  |  |
| `customerName` | texte |  |  |
| `status` | texte |  |  |
| `deliveredAt` | date-heure ISO 8601 UTC |  |  |
| `invoiceNumber` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

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

<a id="m-DeliveryNoteDtos-InvoiceRequest"></a>
#### DeliveryNoteDtos.InvoiceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `noteNumbers` | texte[] | oui | éléments min `1` |

<a id="m-DeliveryNoteDtos-NoteView"></a>
#### DeliveryNoteDtos.NoteView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `noteNumber` | texte |  |  |
| `sourceDocumentNumber` | texte |  |  |
| `sourceDocumentType` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `status` | texte |  |  |
| `deliveryAddress` | texte |  |  |
| `notes` | texte |  |  |
| `showPrices` | booléen |  |  |
| `stockIssued` | booléen |  |  |
| `deliveredAt` | date-heure ISO 8601 UTC |  |  |
| `deliveredBy` | texte |  |  |
| `receivedBy` | texte |  |  |
| `signedCopyReference` | texte |  |  |
| `invoiceNumber` | texte |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancelReason` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [DeliveryNoteDtos.NoteLine](#m-DeliveryNoteDtos-NoteLine)[] |  |  |

<a id="m-DeliveryNoteDtos-CancelRequest"></a>
#### DeliveryNoteDtos.CancelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-DeliveryNoteDtos-DeliverRequest"></a>
#### DeliveryNoteDtos.DeliverRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `receivedBy` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-DeliveryNoteDtos-SignedCopyRequest"></a>
#### DeliveryNoteDtos.SignedCopyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reference` | texte |  | longueur min `0`, longueur max `120` |

<a id="m-DeliveryNoteDtos-CreateRequest"></a>
#### DeliveryNoteDtos.CreateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lines` | [DeliveryNoteDtos.LineQuantity](#m-DeliveryNoteDtos-LineQuantity)[] |  |  |
| `deliveryAddress` | texte |  | longueur min `0`, longueur max `2000` |
| `notes` | texte |  | longueur min `0`, longueur max `4000` |
| `showPrices` | booléen |  |  |

<a id="m-DeliveryNoteDtos-DeliveryStatus"></a>
#### DeliveryNoteDtos.DeliveryStatus

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | texte |  |  |
| `complete` | booléen |  |  |
| `lines` | [DeliveryNoteDtos.LineStatus](#m-DeliveryNoteDtos-LineStatus)[] |  |  |
| `notes` | [DeliveryNoteDtos.NoteSummary](#m-DeliveryNoteDtos-NoteSummary)[] |  |  |

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

<a id="m-DeliveryNoteDtos-NoteLine"></a>
#### DeliveryNoteDtos.NoteLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineOrder` | entier |  |  |
| `itemCode` | texte |  |  |
| `description` | texte |  |  |
| `unit` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |

<a id="m-DeliveryNoteDtos-LineQuantity"></a>
#### DeliveryNoteDtos.LineQuantity

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineOrder` | entier |  |  |
| `quantity` | nombre décimal |  |  |

<a id="m-DeliveryNoteDtos-LineStatus"></a>
#### DeliveryNoteDtos.LineStatus

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineOrder` | entier |  |  |
| `itemCode` | texte |  |  |
| `description` | texte |  |  |
| `unit` | texte |  |  |
| `ordered` | nombre décimal |  |  |
| `delivered` | nombre décimal |  |  |
| `inDraft` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |
