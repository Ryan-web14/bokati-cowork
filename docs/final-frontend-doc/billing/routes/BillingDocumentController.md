# BillingDocumentController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 46 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-aging-report"></a>
### GET /sni/api/v1/billing/aging-report

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `billing.dto.response.BillingAgingReportResponse`
- Modèles : [billing.dto.response.BillingAgingReportResponse](#m-billing-dto-response-BillingAgingReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `agingReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `currency` | requête | `String` |  | XAF |

<a id="r-patch-sni-api-v1-billing-credit-notes-creditnotenumber-apply"></a>
### PATCH /sni/api/v1/billing/credit-notes/{creditNoteNumber}/apply

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `applyCreditNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `creditNoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-credit-notes-creditnotenumber-apply-to-targetinvoicenumber"></a>
### PATCH /sni/api/v1/billing/credit-notes/{creditNoteNumber}/apply-to/{targetInvoiceNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `applyCreditNoteToInvoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `creditNoteNumber` | chemin | `String` | oui |  |
| `targetInvoiceNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-credit-notes-creditnotenumber-refund-to-wallet"></a>
### POST /sni/api/v1/billing/credit-notes/{creditNoteNumber}/refund-to-wallet

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `refundCreditNoteToWallet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `creditNoteNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-billing-customers-customertype-customercode-statement"></a>
### GET /sni/api/v1/billing/customers/{customerType}/{customerCode}/statement

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `CustomerStatementResponse`
- Modèles : [CustomerStatementResponse](#m-CustomerStatementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `statement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=50, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-billing-customers-customertype-customercode-statement-csv"></a>
### GET /sni/api/v1/billing/customers/{customerType}/{customerCode}/statement.csv

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `statementCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=500, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-billing-documents"></a>
### GET /sni/api/v1/billing/documents

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<BillingDocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BillingDocumentResponse](#m-BillingDocumentResponse), [BillingDocumentType](#m-BillingDocumentType), [BillingDocumentStatus](#m-BillingDocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | requête | `BillingDocumentType` |  |  |
| `status` | requête | `BillingDocumentStatus` |  |  |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `sourceType` | requête | `String` |  |  |
| `sourceCode` | requête | `String` |  |  |
| `fromDate` | requête | `LocalDate` |  |  |
| `toDate` | requête | `LocalDate` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-billing-documents"></a>
### POST /sni/api/v1/billing/documents

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateBillingDocumentRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateBillingDocumentRequest](#m-CreateBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `create`

<a id="r-post-sni-api-v1-billing-documents-simulate"></a>
### POST /sni/api/v1/billing/documents/simulate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `SimulateBillingDocumentRequest`
- Réponse : `SimulateBillingDocumentResponse`
- Modèles : [SimulateBillingDocumentResponse](#m-SimulateBillingDocumentResponse), [SimulateBillingDocumentRequest](#m-SimulateBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `simulate`

<a id="r-delete-sni-api-v1-billing-documents-documentnumber"></a>
### DELETE /sni/api/v1/billing/documents/{documentNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-documents-documentnumber"></a>
### PUT /sni/api/v1/billing/documents/{documentNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `UpdateBillingDocumentRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [UpdateBillingDocumentRequest](#m-UpdateBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-duplicate"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/duplicate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `duplicate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-documentnumber-issue"></a>
### PATCH /sni/api/v1/billing/documents/{documentNumber}/issue

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `issue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `discountOverrideReason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-pdf"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/pdf

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `pdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-documentnumber-recipient"></a>
### PATCH /sni/api/v1/billing/documents/{documentNumber}/recipient

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `UpdateBillingRecipientRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [UpdateBillingRecipientRequest](#m-UpdateBillingRecipientRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `updateRecipient`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-recoverables"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/recoverables

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<BillingRecoverableResponse>`
- Modèles : [BillingRecoverableResponse](#m-BillingRecoverableResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `listRecoverables`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `status` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-recoverables"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/recoverables

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `AddRecoverableItemsRequest`
- Réponse : `List<BillingRecoverableResponse>`
- Modèles : [BillingRecoverableResponse](#m-BillingRecoverableResponse), [AddRecoverableItemsRequest](#m-AddRecoverableItemsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `addRecoverables`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-documentnumber-recoverables-recoverablenumber-recover"></a>
### PATCH /sni/api/v1/billing/documents/{documentNumber}/recoverables/{recoverableNumber}/recover

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (facultatif) : `RecoverItemRequest`
- Réponse : `BillingRecoverableResponse`
- Modèles : [BillingRecoverableResponse](#m-BillingRecoverableResponse), [RecoverItemRequest](#m-RecoverItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `recover`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `recoverableNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-documentnumber-recoverables-recoverablenumber-write-off"></a>
### PATCH /sni/api/v1/billing/documents/{documentNumber}/recoverables/{recoverableNumber}/write-off

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (facultatif) : `WriteOffRecoverableRequest`
- Réponse : `BillingRecoverableResponse`
- Modèles : [BillingRecoverableResponse](#m-BillingRecoverableResponse), [WriteOffRecoverableRequest](#m-WriteOffRecoverableRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `writeOff`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `recoverableNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-documentnumber-send"></a>
### PATCH /sni/api/v1/billing/documents/{documentNumber}/send

- Accès : back-office : rôle d'administration et droit `BILLING:SEND`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `send`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-send-email"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/send-email

- Accès : back-office : rôle d'administration et droit `BILLING:SEND`
- Réponse : `Boolean`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `sendEmail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-documentnumber-validate"></a>
### PATCH /sni/api/v1/billing/documents/{documentNumber}/validate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-versions"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/versions

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<BillingDocumentVersionResponse>`
- Modèles : [BillingDocumentVersionResponse](#m-BillingDocumentVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `versions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-versions-from-diff-to"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/versions/{from}/diff/{to}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `BillingDocumentVersionResponse.Diff`
- Modèles : [BillingDocumentVersionResponse.Diff](#m-BillingDocumentVersionResponse-Diff)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `versionDiff`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `from` | chemin | `Integer` | oui |  |
| `to` | chemin | `Integer` | oui |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-versions-versionnumber"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/versions/{versionNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `BillingDocumentVersionResponse`
- Modèles : [BillingDocumentVersionResponse](#m-BillingDocumentVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `version`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `versionNumber` | chemin | `Integer` | oui |  |

<a id="r-post-sni-api-v1-billing-invoices-from-billable-items"></a>
### POST /sni/api/v1/billing/invoices/from-billable-items

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateInvoiceFromBillableItemsRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateInvoiceFromBillableItemsRequest](#m-CreateInvoiceFromBillableItemsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createInvoiceFromBillableItems`

<a id="r-post-sni-api-v1-billing-invoices-from-reservation"></a>
### POST /sni/api/v1/billing/invoices/from-reservation

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateReservationInvoiceRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateReservationInvoiceRequest](#m-CreateReservationInvoiceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createInvoiceFromReservation`

<a id="r-post-sni-api-v1-billing-invoices-manual"></a>
### POST /sni/api/v1/billing/invoices/manual

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateManualBillingDocumentRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateManualBillingDocumentRequest](#m-CreateManualBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createManualInvoice`

<a id="r-post-sni-api-v1-billing-invoices-documentnumber-pay"></a>
### POST /sni/api/v1/billing/invoices/{documentNumber}/pay

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `PayInvoiceRequest`
- Réponse : `PayInvoiceResponse`
- Modèles : [PayInvoiceResponse](#m-PayInvoiceResponse), [PayInvoiceRequest](#m-PayInvoiceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `payInvoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-invoices-invoicenumber-corrective"></a>
### POST /sni/api/v1/billing/invoices/{invoiceNumber}/corrective

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateManualBillingDocumentRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateManualBillingDocumentRequest](#m-CreateManualBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createCorrectiveInvoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `invoiceNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-invoices-invoicenumber-credit-note"></a>
### POST /sni/api/v1/billing/invoices/{invoiceNumber}/credit-note

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Corps JSON (obligatoire) : `CreateCreditNoteRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateCreditNoteRequest](#m-CreateCreditNoteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createCreditNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `invoiceNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-quotations-manual"></a>
### POST /sni/api/v1/billing/quotations/manual

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateManualBillingDocumentRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateManualBillingDocumentRequest](#m-CreateManualBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createManualQuotation`

<a id="r-patch-sni-api-v1-billing-quotations-quotationnumber-accept"></a>
### PATCH /sni/api/v1/billing/quotations/{quotationNumber}/accept

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `acceptQuotation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quotationNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-quotations-quotationnumber-convert-to-invoice"></a>
### POST /sni/api/v1/billing/quotations/{quotationNumber}/convert-to-invoice

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `convertQuotationToInvoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quotationNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotations-quotationnumber-reject"></a>
### PATCH /sni/api/v1/billing/quotations/{quotationNumber}/reject

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `rejectQuotation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quotationNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-quotes-manual"></a>
### POST /sni/api/v1/billing/quotes/manual

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateManualBillingDocumentRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [CreateManualBillingDocumentRequest](#m-CreateManualBillingDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `createManualQuote`

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-accept"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/accept

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `acceptQuote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-quotes-quotenumber-convert-to-invoice"></a>
### POST /sni/api/v1/billing/quotes/{quoteNumber}/convert-to-invoice

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `convertQuoteToInvoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-mark-deposit-paid"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/mark-deposit-paid

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `markDepositPaid`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-mark-viewed"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/mark-viewed

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `markViewed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-reject"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/reject

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `rejectQuote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-request-deposit"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/request-deposit

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `requestDeposit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-quotes-quotenumber-request-signature"></a>
### POST /sni/api/v1/billing/quotes/{quoteNumber}/request-signature

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (facultatif) : `RequestSignatureRequest`
- Réponse : `BillingDocumentSignatureResponse`
- Modèles : [BillingDocumentSignatureResponse](#m-BillingDocumentSignatureResponse), [RequestSignatureRequest](#m-RequestSignatureRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `requestSignature`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-select-options"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/select-options

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `SelectQuoteOptionsRequest`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse), [SelectQuoteOptionsRequest](#m-SelectQuoteOptionsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `selectQuoteOptions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-quotes-quotenumber-start-negotiation"></a>
### PATCH /sni/api/v1/billing/quotes/{quoteNumber}/start-negotiation

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `BillingDocumentResponse`
- Modèles : [BillingDocumentResponse](#m-BillingDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java`, méthode `startNegotiation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-billing-dto-response-BillingAgingReportResponse"></a>
#### billing.dto.response.BillingAgingReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `currency` | texte |  |  |
| `summary` | [BillingAgingReportResponse.AgingSummary](#m-BillingAgingReportResponse-AgingSummary) |  |  |
| `documents` | [BillingAgingReportResponse.AgingDocument](#m-BillingAgingReportResponse-AgingDocument)[] |  |  |

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

<a id="m-CustomerStatementResponse"></a>
#### CustomerStatementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `totalInvoiced` | nombre décimal |  |  |
| `totalPaid` | nombre décimal |  |  |
| `totalBalanceDue` | nombre décimal |  |  |
| `totalCreditAvailable` | nombre décimal |  |  |
| `netBalanceDue` | nombre décimal |  |  |
| `totalDraft` | nombre décimal |  |  |
| `documents` | [BillingDocumentResponse](#m-BillingDocumentResponse)[] |  |  |

<a id="m-BillingDocumentType"></a>
#### BillingDocumentType

Énumération : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE`

<a id="m-BillingDocumentStatus"></a>
#### BillingDocumentStatus

Énumération : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `VOIDED`, `REFUNDED`, `WRITTEN_OFF`

<a id="m-CreateBillingDocumentRequest"></a>
#### CreateBillingDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` | oui |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `terms` | texte |  |  |
| `currency` | texte | oui | longueur min `1` |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `metadataJson` | texte |  |  |
| `lines` | [CreateBillingDocumentLineRequest](#m-CreateBillingDocumentLineRequest)[] | oui | éléments min `1` |
| `discounts` | [CreateBillingDocumentDiscountRequest](#m-CreateBillingDocumentDiscountRequest)[] |  |  |
| `clauses` | [CreateBillingDocumentClauseRequest](#m-CreateBillingDocumentClauseRequest)[] |  |  |
| `paymentReference` | texte |  |  |
| `paymentInstructions` | texte |  |  |
| `bankDetailsJson` | texte |  |  |
| `advance` | [CreateBillingDocumentAdvanceRequest](#m-CreateBillingDocumentAdvanceRequest) |  |  |
| `customerReference` | texte |  |  |
| `poNumber` | texte |  |  |
| `projectCode` | texte |  |  |
| `salespersonCode` | texte |  |  |
| `deliveryAddressJson` | texte |  |  |
| `language` | texte |  |  |
| `exchangeRate` | nombre décimal |  |  |
| `earlyPaymentDiscount` | [CreateEarlyPaymentDiscountRequest](#m-CreateEarlyPaymentDiscountRequest) |  |  |
| `companyCustomerCode` | texte |  |  |

<a id="m-SimulateBillingDocumentResponse"></a>
#### SimulateBillingDocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `currency` | texte |  |  |
| `title` | texte |  |  |
| `lines` | [SimulateBillingDocumentResponse.SimulatedLine](#m-SimulateBillingDocumentResponse-SimulatedLine)[] |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `lineDiscountAmount` | nombre décimal |  |  |
| `documentDiscountAmount` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxableAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `additionalCentAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `violations` | [BillingDiscountGuard.DiscountViolation](#m-BillingDiscountGuard-DiscountViolation)[] |  |  |
| `blocked` | booléen |  |  |

<a id="m-SimulateBillingDocumentRequest"></a>
#### SimulateBillingDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `currency` | texte |  |  |
| `title` | texte |  |  |
| `lines` | [CreateBillingDocumentLineRequest](#m-CreateBillingDocumentLineRequest)[] | oui | éléments min `1` |
| `discounts` | [CreateBillingDocumentDiscountRequest](#m-CreateBillingDocumentDiscountRequest)[] |  |  |

<a id="m-UpdateBillingDocumentRequest"></a>
#### UpdateBillingDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `terms` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
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
| `internalNotes` | texte |  |  |
| `lines` | [UpdateBillingDocumentLineRequest](#m-UpdateBillingDocumentLineRequest)[] |  |  |
| `discounts` | [CreateBillingDocumentDiscountRequest](#m-CreateBillingDocumentDiscountRequest)[] |  |  |
| `clauses` | [CreateBillingDocumentClauseRequest](#m-CreateBillingDocumentClauseRequest)[] |  |  |

<a id="m-UpdateBillingRecipientRequest"></a>
#### UpdateBillingRecipientRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `customerNiu` | texte |  |  |
| `customerCategory` | texte |  |  |
| `customerReference` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `deliveryAddressJson` | texte |  |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |

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

<a id="m-AddRecoverableItemsRequest"></a>
#### AddRecoverableItemsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `items` | [RecoverableItemRequest](#m-RecoverableItemRequest)[] | oui | éléments min `1` |

<a id="m-RecoverItemRequest"></a>
#### RecoverItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notes` | texte |  |  |
| `partialQuantity` | nombre décimal |  |  |

<a id="m-WriteOffRecoverableRequest"></a>
#### WriteOffRecoverableRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notes` | texte |  |  |

<a id="m-BillingDocumentVersionResponse"></a>
#### BillingDocumentVersionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `versionNumber` | entier |  |  |
| `editType` | texte |  |  |
| `changedBy` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |
| `sentToCustomerAt` | date-heure ISO 8601 UTC |  |  |
| `changeSummary` | texte |  |  |
| `snapshot` | objet { clé : objet libre } |  |  |

<a id="m-BillingDocumentVersionResponse-Diff"></a>
#### BillingDocumentVersionResponse.Diff

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fromVersion` | entier |  |  |
| `toVersion` | entier |  |  |
| `changes` | [BillingDocumentVersionResponse.Diff.FieldChange](#m-BillingDocumentVersionResponse-Diff-FieldChange)[] |  |  |

<a id="m-CreateInvoiceFromBillableItemsRequest"></a>
#### CreateInvoiceFromBillableItemsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `billableNumbers` | texte[] | oui | éléments min `1` |

<a id="m-CreateReservationInvoiceRequest"></a>
#### CreateReservationInvoiceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mode` | texte | oui | longueur min `1` |
| `bookingNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `currency` | texte |  |  |
| `externalBooking` | [ExternalBookingDetails](#m-ExternalBookingDetails) |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `terms` | texte |  |  |
| `paymentInstructions` | texte |  |  |
| `description` | texte |  |  |
| `internalNotes` | texte |  |  |
| `extraLines` | [CreateBillingDocumentLineRequest](#m-CreateBillingDocumentLineRequest)[] |  |  |

<a id="m-CreateManualBillingDocumentRequest"></a>
#### CreateManualBillingDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `terms` | texte |  |  |
| `currency` | texte | oui | longueur min `1` |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `metadataJson` | texte |  |  |
| `lines` | [CreateBillingDocumentLineRequest](#m-CreateBillingDocumentLineRequest)[] | oui | éléments min `1` |
| `discounts` | [CreateBillingDocumentDiscountRequest](#m-CreateBillingDocumentDiscountRequest)[] |  |  |
| `clauses` | [CreateBillingDocumentClauseRequest](#m-CreateBillingDocumentClauseRequest)[] |  |  |
| `paymentReference` | texte |  |  |
| `paymentInstructions` | texte |  |  |
| `bankDetailsJson` | texte |  |  |
| `advance` | [CreateBillingDocumentAdvanceRequest](#m-CreateBillingDocumentAdvanceRequest) |  |  |
| `customerReference` | texte |  |  |
| `poNumber` | texte |  |  |
| `projectCode` | texte |  |  |
| `salespersonCode` | texte |  |  |
| `deliveryAddressJson` | texte |  |  |
| `language` | texte |  |  |
| `exchangeRate` | nombre décimal |  |  |
| `earlyPaymentDiscount` | [CreateEarlyPaymentDiscountRequest](#m-CreateEarlyPaymentDiscountRequest) |  |  |
| `companyCustomerCode` | texte |  | membre facturé pour le compte d'une organisation : vide, selon le mode de son rattachement ; `NONE`, le membre seul ; un code client d'organisation à laquelle il est rattaché. Synonyme accepté : `customerCompanyCode` |

<a id="m-PayInvoiceResponse"></a>
#### PayInvoiceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoice` | [BillingDocumentResponse](#m-BillingDocumentResponse) |  |  |
| `transaction` | [PaymentTransactionResponse](#m-PaymentTransactionResponse) |  |  |

<a id="m-PayInvoiceRequest"></a>
#### PayInvoiceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `MOBILE_MONEY_OFFLINE`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` | oui |  |
| `amount` | nombre décimal |  |  |
| `walletNumber` | texte |  |  |
| `cashSessionNumber` | texte |  |  |
| `providerReference` | texte |  |  |
| `processedBy` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `metadataJson` | texte |  |  |
| `tenderedAmount` | nombre décimal |  |  |
| `bankName` | texte |  |  |
| `payerName` | texte |  |  |
| `payerPhone` | texte |  |  |
| `mobileMoneyOperator` | enum : `MTN_MOMO`, `AIRTEL_MONEY` |  |  |

<a id="m-CreateCreditNoteRequest"></a>
#### CreateCreditNoteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal |  |  |
| `reason` | texte | oui | longueur min `1` |
| `applyImmediately` | booléen |  |  |
| `validateImmediately` | booléen |  |  |
| `lines` | [CreateBillingDocumentLineRequest](#m-CreateBillingDocumentLineRequest)[] |  |  |

<a id="m-BillingDocumentSignatureResponse"></a>
#### BillingDocumentSignatureResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerName` | texte |  |  |
| `signerEmail` | texte |  |  |
| `status` | enum : `PENDING`, `SIGNED`, `EXPIRED` |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-RequestSignatureRequest"></a>
#### RequestSignatureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerName` | texte |  |  |
| `signerEmail` | texte |  |  |
| `customMessage` | texte |  |  |

<a id="m-SelectQuoteOptionsRequest"></a>
#### SelectQuoteOptionsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `selectedLineOrders` | entier[] | oui |  |

<a id="m-BillingAgingReportResponse-AgingSummary"></a>
#### BillingAgingReportResponse.AgingSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `current` | [billing.dto.response.BillingAgingReportResponse.AgingBucket](#m-billing-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `days1To30` | [billing.dto.response.BillingAgingReportResponse.AgingBucket](#m-billing-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `days31To60` | [billing.dto.response.BillingAgingReportResponse.AgingBucket](#m-billing-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `days61To90` | [billing.dto.response.BillingAgingReportResponse.AgingBucket](#m-billing-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `daysOver90` | [billing.dto.response.BillingAgingReportResponse.AgingBucket](#m-billing-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |
| `grandTotal` | [billing.dto.response.BillingAgingReportResponse.AgingBucket](#m-billing-dto-response-BillingAgingReportResponse-AgingBucket) |  |  |

<a id="m-BillingAgingReportResponse-AgingDocument"></a>
#### BillingAgingReportResponse.AgingDocument

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |
| `agingBucket` | texte |  |  |
| `status` | texte |  |  |

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

<a id="m-CreateBillingDocumentLineRequest"></a>
#### CreateBillingDocumentLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineOrder` | entier |  |  |
| `lineType` | enum : `PRODUCT`, `SERVICE`, `SUBSCRIPTION`, `PASS`, `ADDON`, `BOOKING`, `OVERAGE`, `PENALTY`, `WALLET`, `ADJUSTMENT` |  |  |
| `itemCode` | texte |  |  |
| `description` | texte | oui | longueur min `1` |
| `detailedDescription` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal | oui |  |
| `discountRate` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxable` | booléen |  |  |
| `taxIncluded` | booléen |  |  |
| `vatRate` | nombre décimal |  |  |
| `additionalCentRate` | nombre décimal |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `unit` | texte |  |  |
| `externalReference` | texte |  |  |
| `notes` | texte |  |  |
| `optional` | booléen |  |  |
| `category` | texte |  |  |

<a id="m-CreateBillingDocumentDiscountRequest"></a>
#### CreateBillingDocumentDiscountRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `discountCode` | texte |  |  |
| `description` | texte | oui | longueur min `1` |
| `discountType` | enum : `PERCENTAGE`, `FIXED_AMOUNT` | oui |  |
| `value` | nombre décimal | oui |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |

<a id="m-CreateBillingDocumentClauseRequest"></a>
#### CreateBillingDocumentClauseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `clauseCode` | texte |  |  |
| `title` | texte | oui | longueur min `1` |
| `body` | texte | oui | longueur min `1` |
| `displayOrder` | entier |  |  |

<a id="m-CreateBillingDocumentAdvanceRequest"></a>
#### CreateBillingDocumentAdvanceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `advanceType` | enum : `PERCENTAGE`, `FIXED_AMOUNT` | oui |  |
| `advanceValue` | nombre décimal | oui |  |
| `includedLineOrders` | entier[] |  |  |
| `excludedLineOrders` | entier[] |  |  |
| `paymentReference` | texte |  |  |
| `referenceLabel` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `notes` | texte |  |  |

<a id="m-CreateEarlyPaymentDiscountRequest"></a>
#### CreateEarlyPaymentDiscountRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `discountRate` | nombre décimal | oui |  |
| `ifPaidBefore` | date AAAA-MM-JJ | oui |  |
| `label` | texte |  |  |

<a id="m-SimulateBillingDocumentResponse-SimulatedLine"></a>
#### SimulateBillingDocumentResponse.SimulatedLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `description` | texte |  |  |
| `category` | texte |  |  |
| `unit` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `discountRate` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `effectiveRate` | nombre décimal |  |  |
| `netAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `taxable` | booléen |  |  |
| `optional` | booléen |  |  |

<a id="m-BillingDiscountGuard-DiscountViolation"></a>
#### BillingDiscountGuard.DiscountViolation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `severity` | texte |  |  |
| `itemCode` | texte |  |  |
| `lineDescription` | texte |  |  |
| `observed` | nombre décimal |  |  |
| `limit` | nombre décimal |  |  |
| `message` | texte |  |  |

<a id="m-UpdateBillingDocumentLineRequest"></a>
#### UpdateBillingDocumentLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `action` | texte | oui |  |
| `lineOrder` | entier | oui |  |
| `lineType` | enum : `PRODUCT`, `SERVICE`, `SUBSCRIPTION`, `PASS`, `ADDON`, `BOOKING`, `OVERAGE`, `PENALTY`, `WALLET`, `ADJUSTMENT` |  |  |
| `itemCode` | texte |  |  |
| `description` | texte | oui | longueur min `1` |
| `detailedDescription` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `discountRate` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxable` | booléen |  |  |
| `taxIncluded` | booléen |  |  |
| `vatRate` | nombre décimal |  |  |
| `additionalCentRate` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `externalReference` | texte |  |  |
| `notes` | texte |  |  |
| `optional` | booléen |  |  |
| `category` | texte |  |  |

<a id="m-RecoverableItemRequest"></a>
#### RecoverableItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemDescription` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-BillingDocumentVersionResponse-Diff-FieldChange"></a>
#### BillingDocumentVersionResponse.Diff.FieldChange

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `field` | texte |  |  |
| `before` | objet libre |  |  |
| `after` | objet libre |  |  |

<a id="m-ExternalBookingDetails"></a>
#### ExternalBookingDetails

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceName` | texte | oui | longueur min `1` |
| `resourceDescription` | texte |  |  |
| `checkInAt` | date-heure locale sans fuseau | oui |  |
| `checkOutAt` | date-heure locale sans fuseau | oui |  |
| `bookingUnit` | enum : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` | oui |  |
| `quantity` | nombre décimal | oui |  |
| `unitPrice` | nombre décimal | oui |  |
| `externalReference` | texte |  |  |
| `notes` | texte |  |  |

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

<a id="m-billing-dto-response-BillingAgingReportResponse-AgingBucket"></a>
#### billing.dto.response.BillingAgingReportResponse.AgingBucket

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `count` | entier |  |  |
| `totalDue` | nombre décimal |  |  |
