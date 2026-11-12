# PaymentReceiptController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-payments-receipts-receiptnumber"></a>
### GET /sni/api/v1/payments/receipts/{receiptNumber}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaymentReceiptResponse`
- Modèles : [PaymentReceiptResponse](#m-PaymentReceiptResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentReceiptController.java`, méthode `getByReceipt`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `receiptNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-payments-receipts-receiptnumber-pdf"></a>
### GET /sni/api/v1/payments/receipts/{receiptNumber}/pdf

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentReceiptController.java`, méthode `pdfByReceipt`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `receiptNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-payments-transactions-transactionnumber-receipt"></a>
### GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaymentReceiptResponse`
- Modèles : [PaymentReceiptResponse](#m-PaymentReceiptResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentReceiptController.java`, méthode `getByTransaction`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-payments-transactions-transactionnumber-receipt-pdf"></a>
### GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt/pdf

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentReceiptController.java`, méthode `pdfByTransaction`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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
