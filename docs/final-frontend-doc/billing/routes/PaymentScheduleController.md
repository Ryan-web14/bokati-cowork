# PaymentScheduleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-invoices-documentnumber-schedule"></a>
### GET /sni/api/v1/billing/invoices/{documentNumber}/schedule

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaymentScheduleResponse`
- Modèles : [PaymentScheduleResponse](#m-PaymentScheduleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/PaymentScheduleController.java`, méthode `getScheduleByDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-invoices-documentnumber-schedule"></a>
### POST /sni/api/v1/billing/invoices/{documentNumber}/schedule

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreatePaymentScheduleRequest`
- Réponse : `PaymentScheduleResponse`
- Modèles : [PaymentScheduleResponse](#m-PaymentScheduleResponse), [CreatePaymentScheduleRequest](#m-CreatePaymentScheduleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/PaymentScheduleController.java`, méthode `createSchedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-schedules-installments-installmentnumber-pay"></a>
### POST /sni/api/v1/billing/schedules/installments/{installmentNumber}/pay

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `PayInvoiceRequest`
- Réponse : `PaymentScheduleResponse`
- Modèles : [PaymentScheduleResponse](#m-PaymentScheduleResponse), [PayInvoiceRequest](#m-PayInvoiceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/PaymentScheduleController.java`, méthode `payInstallment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `installmentNumber` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-billing-schedules-schedulenumber"></a>
### DELETE /sni/api/v1/billing/schedules/{scheduleNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `PaymentScheduleResponse`
- Modèles : [PaymentScheduleResponse](#m-PaymentScheduleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/PaymentScheduleController.java`, méthode `cancelSchedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `scheduleNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-schedules-schedulenumber"></a>
### GET /sni/api/v1/billing/schedules/{scheduleNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaymentScheduleResponse`
- Modèles : [PaymentScheduleResponse](#m-PaymentScheduleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/PaymentScheduleController.java`, méthode `getSchedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `scheduleNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PaymentScheduleResponse"></a>
#### PaymentScheduleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scheduleNumber` | texte |  |  |
| `billingDocumentNumber` | texte |  |  |
| `status` | enum : `ACTIVE`, `COMPLETED`, `CANCELLED` |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `notes` | texte |  |  |
| `installments` | [PaymentScheduleInstallmentResponse](#m-PaymentScheduleInstallmentResponse)[] |  |  |

<a id="m-CreatePaymentScheduleRequest"></a>
#### CreatePaymentScheduleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `installments` | [CreateInstallmentInput](#m-CreateInstallmentInput)[] | oui | éléments min `1` |
| `notes` | texte |  |  |

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

<a id="m-PaymentScheduleInstallmentResponse"></a>
#### PaymentScheduleInstallmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `installmentNumber` | texte |  |  |
| `installmentOrder` | entier |  |  |
| `label` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `status` | enum : `PENDING`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateInstallmentInput"></a>
#### CreateInstallmentInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `amount` | nombre décimal | oui |  |
| `dueDate` | date AAAA-MM-JJ | oui |  |
