# PurchaseController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 20 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-purchases-invoices"></a>
### GET /sni/api/v1/accounting/purchases/invoices

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `List<AccSupplierInvoice>`
- Modèles : [AccSupplierInvoice](#m-AccSupplierInvoice)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `invoices`

<a id="r-post-sni-api-v1-accounting-purchases-invoices"></a>
### POST /sni/api/v1/accounting/purchases/invoices

- Accès : back-office : rôle d'administration et droit `SUPPLIER_INVOICE:MANAGE`
- Corps JSON (obligatoire) : `PurchaseService.InvoiceRequest`
- Réponse : `PurchaseService.InvoiceView`
- Modèles : [PurchaseService.InvoiceView](#m-PurchaseService-InvoiceView), [PurchaseService.InvoiceRequest](#m-PurchaseService-InvoiceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `createInvoice`

<a id="r-delete-sni-api-v1-accounting-purchases-invoices-number"></a>
### DELETE /sni/api/v1/accounting/purchases/invoices/{number}

- Accès : back-office : rôle d'administration et droit `SUPPLIER_INVOICE:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-purchases-invoices-number"></a>
### GET /sni/api/v1/accounting/purchases/invoices/{number}

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `PurchaseService.InvoiceView`
- Modèles : [PurchaseService.InvoiceView](#m-PurchaseService-InvoiceView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `invoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-purchases-invoices-number-pay"></a>
### POST /sni/api/v1/accounting/purchases/invoices/{number}/pay

- Accès : back-office : rôle d'administration et droit `SUPPLIER_PAYMENT:MANAGE`
- Corps JSON (obligatoire) : `PurchaseService.PaymentRequest`
- Réponse : `PurchaseService.InvoiceView`
- Modèles : [PurchaseService.InvoiceView](#m-PurchaseService-InvoiceView), [PurchaseService.PaymentRequest](#m-PurchaseService-PaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `pay`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-purchases-invoices-number-post"></a>
### POST /sni/api/v1/accounting/purchases/invoices/{number}/post

- Accès : back-office : rôle d'administration et droit `SUPPLIER_INVOICE:MANAGE`
- Réponse : `PurchaseService.InvoiceView`
- Modèles : [PurchaseService.InvoiceView](#m-PurchaseService-InvoiceView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `post`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-purchases-orders"></a>
### GET /sni/api/v1/accounting/purchases/orders

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `List<AccPurchaseOrder>`
- Modèles : [AccPurchaseOrder](#m-AccPurchaseOrder)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `orders`

<a id="r-post-sni-api-v1-accounting-purchases-orders"></a>
### POST /sni/api/v1/accounting/purchases/orders

- Accès : back-office : rôle d'administration et droit `PURCHASE_ORDER:MANAGE`
- Corps JSON (obligatoire) : `PurchaseService.OrderRequest`
- Réponse : `PurchaseService.OrderView`
- Modèles : [PurchaseService.OrderView](#m-PurchaseService-OrderView), [PurchaseService.OrderRequest](#m-PurchaseService-OrderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `createOrder`

<a id="r-post-sni-api-v1-accounting-purchases-orders-from-need"></a>
### POST /sni/api/v1/accounting/purchases/orders/from-need

- Accès : back-office : rôle d'administration et droit `PURCHASE_ORDER:MANAGE`
- Corps JSON (obligatoire) : `PurchaseController.FromNeedRequest`
- Réponse : `PurchaseService.OrderView`
- Modèles : [PurchaseService.OrderView](#m-PurchaseService-OrderView), [PurchaseController.FromNeedRequest](#m-PurchaseController-FromNeedRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `fromNeed`

<a id="r-get-sni-api-v1-accounting-purchases-orders-number"></a>
### GET /sni/api/v1/accounting/purchases/orders/{number}

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `PurchaseService.OrderView`
- Modèles : [PurchaseService.OrderView](#m-PurchaseService-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `order`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-purchases-orders-number-cancel"></a>
### PATCH /sni/api/v1/accounting/purchases/orders/{number}/cancel

- Accès : back-office : rôle d'administration et droit `PURCHASE_ORDER:VALIDATE`
- Corps JSON (obligatoire) : `PurchaseController.Reason`
- Réponse : `PurchaseService.OrderView`
- Modèles : [PurchaseService.OrderView](#m-PurchaseService-OrderView), [PurchaseController.Reason](#m-PurchaseController-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-purchases-orders-number-pdf"></a>
### GET /sni/api/v1/accounting/purchases/orders/{number}/pdf

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `orderPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-purchases-orders-number-validate"></a>
### PATCH /sni/api/v1/accounting/purchases/orders/{number}/validate

- Accès : back-office : rôle d'administration et droit `PURCHASE_ORDER:VALIDATE`
- Corps JSON (facultatif) : `PurchaseController.Reason`
- Réponse : `PurchaseService.OrderView`
- Modèles : [PurchaseService.OrderView](#m-PurchaseService-OrderView), [PurchaseController.Reason](#m-PurchaseController-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-purchases-payables"></a>
### GET /sni/api/v1/accounting/purchases/payables

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `List<PurchaseService.Payable>`
- Modèles : [PurchaseService.Payable](#m-PurchaseService-Payable)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `payables`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `until` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-purchases-suppliers"></a>
### GET /sni/api/v1/accounting/purchases/suppliers

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `List<AccSupplier>`
- Modèles : [AccSupplier](#m-AccSupplier)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `suppliers`

<a id="r-post-sni-api-v1-accounting-purchases-suppliers"></a>
### POST /sni/api/v1/accounting/purchases/suppliers

- Accès : back-office : rôle d'administration et droit `SUPPLIER:MANAGE`
- Corps JSON (obligatoire) : `AccSupplier`
- Réponse : `AccSupplier`
- Modèles : [AccSupplier](#m-AccSupplier)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `saveSupplier`

<a id="r-post-sni-api-v1-accounting-purchases-suppliers-from-inventory-inventorycode"></a>
### POST /sni/api/v1/accounting/purchases/suppliers/from-inventory/{inventoryCode}

- Accès : back-office : rôle d'administration et droit `SUPPLIER:MANAGE`
- Réponse : `AccSupplier`
- Modèles : [AccSupplier](#m-AccSupplier)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `supplierFromInventory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `inventoryCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-purchases-suppliers-code-statement"></a>
### GET /sni/api/v1/accounting/purchases/suppliers/{code}/statement

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `PurchaseService.Statement`
- Modèles : [PurchaseService.Statement](#m-PurchaseService-Statement)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `statement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-purchases-withholdings"></a>
### GET /sni/api/v1/accounting/purchases/withholdings

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `PurchaseService.WithholdingStatement`
- Modèles : [PurchaseService.WithholdingStatement](#m-PurchaseService-WithholdingStatement)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `withholdings`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-purchases-withholdings-export"></a>
### GET /sni/api/v1/accounting/purchases/withholdings/export

- Accès : back-office : rôle d'administration et droit `PURCHASE:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PurchaseController.java`, méthode `withholdingsCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AccSupplierInvoice"></a>
#### AccSupplierInvoice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `kind` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplierReference` | texte |  |  |
| `invoiceDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `orderNumber` | texte |  |  |
| `originalNumber` | texte |  |  |
| `subject` | texte |  |  |
| `totalHt` | nombre décimal |  |  |
| `totalVat` | nombre décimal |  |  |
| `totalTtc` | nombre décimal |  |  |
| `withholdingAmount` | nombre décimal |  |  |
| `netToPay` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `closeOrder` | booléen |  |  |
| `documentRef` | texte |  |  |
| `status` | texte |  |  |
| `entryNumber` | texte |  |  |
| `payableAccount` | texte |  |  |
| `postedBy` | texte |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PurchaseService-InvoiceView"></a>
#### PurchaseService.InvoiceView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoice` | [AccSupplierInvoice](#m-AccSupplierInvoice) |  |  |
| `lines` | [AccSupplierInvoiceLine](#m-AccSupplierInvoiceLine)[] |  |  |
| `payments` | [AccSupplierPayment](#m-AccSupplierPayment)[] |  |  |
| `supplier` | [AccSupplier](#m-AccSupplier) |  |  |

<a id="m-PurchaseService-InvoiceRequest"></a>
#### PurchaseService.InvoiceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplierCode` | texte |  |  |
| `supplierReference` | texte |  |  |
| `kind` | texte |  |  |
| `invoiceDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `orderNumber` | texte |  |  |
| `originalNumber` | texte |  |  |
| `subject` | texte |  |  |
| `documentRef` | texte |  |  |
| `closeOrder` | booléen |  |  |
| `withholdingAmount` | nombre décimal |  |  |
| `lines` | [PurchaseService.LineInput](#m-PurchaseService-LineInput)[] |  |  |

<a id="m-PurchaseService-PaymentRequest"></a>
#### PurchaseService.PaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `treasuryAccount` | texte |  |  |
| `journalCode` | texte |  |  |
| `reference` | texte |  |  |

<a id="m-AccPurchaseOrder"></a>
#### AccPurchaseOrder

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `number` | texte |  |  |
| `supplierCode` | texte |  |  |
| `needStatementNumber` | texte |  |  |
| `subject` | texte |  |  |
| `orderDate` | date AAAA-MM-JJ |  |  |
| `expectedDate` | date AAAA-MM-JJ |  |  |
| `sectionCode` | texte |  |  |
| `totalHt` | nombre décimal |  |  |
| `totalVat` | nombre décimal |  |  |
| `totalTtc` | nombre décimal |  |  |
| `invoicedHt` | nombre décimal |  |  |
| `status` | texte |  |  |
| `budgetCommitted` | booléen |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledBy` | texte |  |  |
| `cancelReason` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PurchaseService-OrderView"></a>
#### PurchaseService.OrderView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `order` | [AccPurchaseOrder](#m-AccPurchaseOrder) |  |  |
| `lines` | [AccPurchaseOrderLine](#m-AccPurchaseOrderLine)[] |  |  |
| `supplier` | [AccSupplier](#m-AccSupplier) |  |  |
| `invoices` | [AccSupplierInvoice](#m-AccSupplierInvoice)[] |  |  |

<a id="m-PurchaseService-OrderRequest"></a>
#### PurchaseService.OrderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplierCode` | texte |  |  |
| `subject` | texte |  |  |
| `orderDate` | date AAAA-MM-JJ |  |  |
| `expectedDate` | date AAAA-MM-JJ |  |  |
| `sectionCode` | texte |  |  |
| `lines` | [PurchaseService.LineInput](#m-PurchaseService-LineInput)[] |  |  |

<a id="m-PurchaseController-FromNeedRequest"></a>
#### PurchaseController.FromNeedRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `needNumber` | texte |  |  |
| `supplierCode` | texte |  |  |
| `lineNos` | entier[] |  |  |

<a id="m-PurchaseController-Reason"></a>
#### PurchaseController.Reason

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-PurchaseService-Payable"></a>
#### PurchaseService.Payable

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `supplierReference` | texte |  |  |
| `invoiceDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `netToPay` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |
| `daysLate` | entier int64 ⚠ |  |  |

<a id="m-AccSupplier"></a>
#### AccSupplier

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `niu` | texte |  |  |
| `rccm` | texte |  |  |
| `address` | texte |  |  |
| `phone` | texte |  |  |
| `email` | texte |  |  |
| `bankDetails` | texte |  |  |
| `paymentTermsDays` | entier |  |  |
| `accountNumber` | texte |  |  |
| `defaultAccount` | texte |  |  |
| `vatExempt` | booléen |  |  |
| `withholdingRate` | nombre décimal |  |  |
| `withholdingAccount` | texte |  |  |
| `withholdingBase` | texte |  |  |
| `withholdingLabel` | texte |  |  |
| `inventorySupplierId` | entier int64 ⚠ |  |  |
| `status` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PurchaseService-Statement"></a>
#### PurchaseService.Statement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplier` | [AccSupplier](#m-AccSupplier) |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `opening` | nombre décimal |  |  |
| `lines` | [PurchaseService.StatementLine](#m-PurchaseService-StatementLine)[] |  |  |
| `closing` | nombre décimal |  |  |

<a id="m-PurchaseService-WithholdingStatement"></a>
#### PurchaseService.WithholdingStatement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | texte |  |  |
| `lines` | [PurchaseService.Withholding](#m-PurchaseService-Withholding)[] |  |  |
| `total` | nombre décimal |  |  |
| `accountMovements` | objet { clé : nombre décimal } |  |  |

<a id="m-AccSupplierInvoiceLine"></a>
#### AccSupplierInvoiceLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `invoiceId` | entier int64 ⚠ |  |  |
| `lineNo` | entier |  |  |
| `designation` | texte |  |  |
| `natureCode` | texte |  |  |
| `accountNumber` | texte |  |  |
| `amountHt` | nombre décimal |  |  |
| `vatRate` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `vatAccount` | texte |  |  |
| `sectionCode` | texte |  |  |
| `orderLineNo` | entier |  |  |
| `quantity` | nombre décimal |  |  |
| `fixedAssetCode` | texte |  |  |
| `serviceStart` | date AAAA-MM-JJ |  |  |
| `serviceEnd` | date AAAA-MM-JJ |  |  |
| `recognitionMethod` | texte |  |  |

<a id="m-AccSupplierPayment"></a>
#### AccSupplierPayment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `invoiceNumber` | texte |  |  |
| `supplierCode` | texte |  |  |
| `paymentDate` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `treasuryAccount` | texte |  |  |
| `journalCode` | texte |  |  |
| `reference` | texte |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PurchaseService-LineInput"></a>
#### PurchaseService.LineInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `designation` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `natureCode` | texte |  |  |
| `accountNumber` | texte |  |  |
| `vatRate` | nombre décimal |  |  |
| `sectionCode` | texte |  |  |
| `orderLineNo` | entier |  |  |
| `fixedAssetCode` | texte |  |  |

<a id="m-AccPurchaseOrderLine"></a>
#### AccPurchaseOrderLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `orderId` | entier int64 ⚠ |  |  |
| `lineNo` | entier |  |  |
| `designation` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `natureCode` | texte |  |  |
| `accountNumber` | texte |  |  |
| `vatRate` | nombre décimal |  |  |
| `amountHt` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `sectionCode` | texte |  |  |
| `invoicedHt` | nombre décimal |  |  |
| `needLineNo` | entier |  |  |

<a id="m-PurchaseService-StatementLine"></a>
#### PurchaseService.StatementLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `entryNumber` | texte |  |  |
| `journal` | texte |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `balance` | nombre décimal |  |  |

<a id="m-PurchaseService-Withholding"></a>
#### PurchaseService.Withholding

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoiceNumber` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `niu` | texte |  |  |
| `supplierReference` | texte |  |  |
| `invoiceDate` | date AAAA-MM-JJ |  |  |
| `base` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `account` | texte |  |  |
