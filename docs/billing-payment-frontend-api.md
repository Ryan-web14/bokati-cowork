# Guide Frontend/API - Billing, Invoice, Quotation, Payment et Wallet

Base API: `/sni/api/v1`

Ce document decrit l'integration frontend des modules billing/invoicing, payment et wallet. Le module billing est un moteur general: il gere les factures, devis, proformas, avoirs et notes de debit. Le module payment gere les intentions de paiement, paiements cash, paiements wallet et prepare l'integration mobile money. Le wallet est un portefeuille interne base sur un ledger.

## Principes importants

### Ne pas saisir ce qui peut etre resolu

Le frontend doit envoyer le minimum d'information robuste.

Si une facture/devis concerne un client deja enregistre, envoyer:

- `customerType`
- `customerCode`

Le backend recupere automatiquement:

- `customerName`
- `customerEmail`
- `customerPhone`

Si une facture/devis concerne un client non enregistre, ne pas envoyer `customerType/customerCode`. Envoyer uniquement les informations snapshot:

- `customerName`
- `customerEmail`
- `customerPhone`
- `billingAddressJson`

Le backend genere alors un `customerCode` interne avec la sequence `billing_customer`.

### Codes generes par le backend

Ne jamais afficher ces champs en saisie libre dans les formulaires de creation:

- `documentNumber`
- `intentNumber`
- `transactionNumber`
- `walletNumber`
- `entryNumber`
- `holdNumber`
- `registerCode`
- `sessionNumber`
- `movementNumber`
- `batchNumber`
- `billing customerCode` pour client externe non enregistre

### BillingDocument

Une facture, un devis ou un avoir sont tous des `BillingDocument`.

Types:

- `QUOTE`
- `PROFORMA_INVOICE`
- `INVOICE`
- `CREDIT_NOTE`
- `DEBIT_NOTE`

Statuts:

- `DRAFT`
- `ISSUED`
- `SENT`
- `ACCEPTED`
- `REJECTED`
- `EXPIRED`
- `CONVERTED`
- `PARTIALLY_PAID`
- `PAID`
- `OVERDUE`
- `CANCELLED`
- `VOIDED`
- `REFUNDED`
- `WRITTEN_OFF`

## Billing / Invoice / Quotation

### Creer une facture manuelle

Endpoint:

`POST /billing/invoices/manual`

Cas client enregistre:

```json
{
  "customerType": "CUSTOMER",
  "customerCode": "CUS-000001",
  "title": "Facture prestation",
  "description": "Prestation coworking",
  "currency": "XAF",
  "issueDate": "2026-04-16",
  "dueDate": "2026-04-30",
  "lines": [
    {
      "lineType": "SERVICE",
      "description": "Location salle de reunion",
      "quantity": 2,
      "unitPrice": 25000,
      "taxable": true
    }
  ],
  "clauses": [
    {
      "clauseCode": "PAYMENT_TERMS",
      "title": "Conditions de paiement",
      "body": "Facture payable a reception.",
      "displayOrder": 1
    }
  ]
}
```

Cas client non enregistre:

```json
{
  "customerName": "Client Externe SARL",
  "customerEmail": "client@example.com",
  "customerPhone": "060000000",
  "title": "Facture manuelle externe",
  "description": "Service ponctuel hors plateforme",
  "currency": "XAF",
  "lines": [
    {
      "lineType": "SERVICE",
      "description": "Assistance administrative",
      "quantity": 1,
      "unitPrice": 50000,
      "taxable": true
    }
  ]
}
```

Reponse: `BillingDocumentResponse`.

Le backend calcule automatiquement:

- `subtotalAmount`
- `discountAmount`
- `taxableAmount`
- `vatAmount`
- `additionalCentAmount`
- `taxAmount`
- `totalAmount`
- `balanceDue`

### Creer un devis manuel

Endpoints equivalentes:

`POST /billing/quotes/manual`

`POST /billing/quotations/manual`

Payload identique a une facture manuelle. Le backend cree un document avec:

```json
"documentType": "QUOTE"
```

### Creer un document billing generique

Endpoint:

`POST /billing/documents`

Utiliser cet endpoint quand le frontend veut choisir explicitement le `documentType`.

Exemple:

```json
{
  "documentType": "PROFORMA_INVOICE",
  "customerType": "BUSINESS_ENTITY",
  "customerCode": "BUS-000001",
  "currency": "XAF",
  "lines": [
    {
      "lineType": "PRODUCT",
      "itemCode": "ITEM-001",
      "description": "Produit inventaire",
      "quantity": 3,
      "unitPrice": 12000,
      "sourceType": "INVENTORY_ITEM",
      "sourceCode": "ITEM-001"
    }
  ]
}
```

### Creer une facture depuis billable items

Endpoint:

`POST /billing/invoices/from-billable-items`

Le frontend envoie uniquement les billable items. Le backend deduit:

- client
- devise
- lignes
- source
- type de ligne

Payload:

```json
{
  "title": "Facture elements facturables",
  "description": "Facture generee depuis des billable items",
  "issueDate": "2026-04-16",
  "dueDate": "2026-04-30",
  "billableNumbers": [
    "BIL-000001",
    "BIL-000002"
  ]
}
```

Le type de ligne est deduit de `sourceType`:

- contient `BOOKING` -> `BOOKING`
- contient `PASS` -> `PASS`
- contient `ADDON` -> `ADDON`
- contient `SUBSCRIPTION` -> `SUBSCRIPTION`
- contient `OVERAGE` -> `OVERAGE`
- contient `INVENTORY`, `ITEM` ou `PRODUCT` -> `PRODUCT`
- sinon -> `SERVICE`

### Taxes Congo CG

Le pays fiscal configure est le Congo CG.

Regles seed:

- `TVA_CG_18`: TVA 18%
- `CAC_CG_ON_VAT_5`: centimes additionnels 5% sur la TVA

Par defaut, si une ligne est taxable, le backend applique:

- TVA = base taxable * 18%
- centimes additionnels = TVA * 5%

Pour exonerer une ligne:

```json
{
  "taxable": false
}
```

Pour forcer des taux specifiques:

```json
{
  "vatRate": 18,
  "additionalCentRate": 5
}
```

### Remises

Remise de ligne:

```json
{
  "description": "Service",
  "quantity": 1,
  "unitPrice": 100000,
  "discountRate": 10
}
```

Remise document:

```json
{
  "discounts": [
    {
      "discountCode": "COMMERCIAL",
      "description": "Remise commerciale",
      "discountType": "PERCENTAGE",
      "value": 5
    }
  ]
}
```

Types:

- `PERCENTAGE`
- `FIXED_AMOUNT`

### Workflow devis

Accepter:

`PATCH /billing/quotes/{quoteNumber}/accept`

ou

`PATCH /billing/quotations/{quotationNumber}/accept`

Rejeter:

`PATCH /billing/quotes/{quoteNumber}/reject`

ou

`PATCH /billing/quotations/{quotationNumber}/reject`

Convertir en facture:

`POST /billing/quotes/{quoteNumber}/convert-to-invoice`

ou

`POST /billing/quotations/{quotationNumber}/convert-to-invoice`

La conversion reprend automatiquement:

- client snapshot
- lignes
- remises
- clauses
- taxes
- devise

### Emettre et envoyer

Emettre:

`PATCH /billing/documents/{documentNumber}/issue`

Envoyer:

`PATCH /billing/documents/{documentNumber}/send`

### Detail, listing et PDF

Detail:

`GET /billing/documents/{documentNumber}`

PDF:

`GET /billing/documents/{documentNumber}/pdf`

Listing:

`GET /billing/documents`

Filtres:

- `type`
- `status`
- `customerType`
- `customerCode`
- `sourceType`
- `sourceCode`
- `fromDate`
- `toDate`
- `searchText`
- pagination Spring: `page`, `size`, `sort`

Exemple:

`GET /billing/documents?type=INVOICE&searchText=client&page=0&size=20&sort=createdAt,desc`

`searchText` utilise les index PostgreSQL `pg_trgm` sur les factures, devis et proformas.

### Avoir et releve client

Creer un avoir depuis une facture:

`POST /billing/invoices/{invoiceNumber}/credit-note`

```json
{
  "amount": 25000,
  "reason": "Correction commerciale",
  "createdBy": "admin-001"
}
```

Appliquer un avoir:

`PATCH /billing/credit-notes/{creditNoteNumber}/apply`

Releve client:

`GET /billing/customers/{customerType}/{customerCode}/statement`

Envoyer par email via le module email core:

`POST /billing/documents/{documentNumber}/send-email`

## Payment

### Payment intent

Un paiement passe par une intention de paiement.

Statuts:

- `PENDING`
- `PROCESSING`
- `AUTHORIZED`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`
- `EXPIRED`
- `REFUNDED`
- `PARTIALLY_REFUNDED`
- `REVERSED`

### Creer une intention manuelle

Endpoint:

`POST /payments/intents`

Payload:

```json
{
  "customerType": "CUSTOMER",
  "customerCode": "CUS-000001",
  "amount": 50000,
  "currency": "XAF",
  "purpose": "MANUAL_PAYMENT",
  "sourceType": "MANUAL",
  "sourceCode": "REF-001",
  "idempotencyKey": "manual-payment-REF-001"
}
```

### Creer une intention depuis une facture

Endpoint:

`POST /payments/intents/from-billing-document`

Payload:

```json
{
  "documentNumber": "INV-202604-000001",
  "idempotencyKey": "pay-INV-202604-000001"
}
```

Le backend deduit automatiquement:

- client
- montant restant du
- devise
- sourceType = `BILLING_DOCUMENT`
- sourceCode = `documentNumber`

### Creer une intention pour plusieurs factures

Endpoint:

`POST /payments/intents/from-billing-documents`

Payload:

```json
{
  "documentNumbers": [
    "INV-202604-000001",
    "INV-202604-000002"
  ],
  "idempotencyKey": "pay-batch-202604-001"
}
```

Le backend verifie que toutes les factures appartiennent au meme client et utilisent la meme devise, puis calcule automatiquement le total restant du.

### Paiement cash

Endpoint:

`PATCH /payments/intents/{intentNumber}/cash`

Le cash est autorise uniquement en `XAF`.

Payload:

```json
{
  "receivedBy": "admin-001",
  "cashSessionNumber": "CSS-202604-000001",
  "providerReference": "RECU-001",
  "metadataJson": "{\"locationCode\":\"HQ\"}"
}
```

Si `cashSessionNumber` est fourni, le backend enregistre aussi un mouvement de caisse. Si l'intention vient d'une facture ou d'un lot de factures, le paiement est alloue automatiquement. Tout trop-percu est credite sur le wallet client.

### Paiement wallet

Endpoint:

`PATCH /payments/intents/{intentNumber}/wallet`

Payload:

```json
{
  "walletNumber": "WAL-000001",
  "createdBy": "admin-001"
}
```

Le backend verifie:

- wallet actif
- proprietaire wallet = client de l'intention
- devise wallet = devise intention
- solde suffisant

Si tout est valide:

- debit wallet ledger
- transaction payment `SUCCEEDED`
- allocation facture si source = `BILLING_DOCUMENT`
- facture mise a jour en `PARTIALLY_PAID` ou `PAID`

### Listing payment intents

Endpoint:

`GET /payments/intents`

### Transactions d'une intention

Endpoint:

`GET /payments/intents/{intentNumber}/transactions`

Retourne la liste des transactions rattachees a cette intention, triees par creation descendante.

Filtres:

- `status`
- `customerType`
- `customerCode`
- `sourceType`
- `sourceCode`
- `searchText`

Exemple:

`GET /payments/intents?status=PENDING&searchText=CUS-000001`

`searchText` utilise les index PostgreSQL `pg_trgm` sur les intentions de paiement.

### Refund et reverse

Rembourser:

`POST /payments/transactions/{transactionNumber}/refund`

Annuler/reverser:

`POST /payments/transactions/{transactionNumber}/reverse`

Payload:

```json
{
  "amount": 10000,
  "reason": "Erreur d'encaissement",
  "processedBy": "admin-001"
}
```

Le backend inverse les allocations facture. Pour un paiement wallet, il credite automatiquement le wallet.

## Cash register

Creer une caisse:

`POST /cash-registers`

```json
{
  "name": "Caisse reception",
  "locationCode": "HQ"
}
```

Ouvrir une session:

`POST /cash-registers/sessions`

```json
{
  "registerCode": "CSR-00001",
  "openedBy": "admin-001",
  "openingAmount": 50000
}
```

Fermer une session:

`PATCH /cash-registers/sessions/{sessionNumber}/close`

```json
{
  "closedBy": "admin-001",
  "closingAmount": 175000
}
```

Le frontend passe `sessionNumber` dans le paiement cash pour lier l'encaissement a la caisse.

## Wallet

Le wallet est un portefeuille interne avec ledger append-only. Le solde est conserve sur `wallet_account` pour lecture rapide, mais chaque mouvement est trace dans `wallet_ledger_entry`.

Statuts wallet:

- `ACTIVE`
- `SUSPENDED`
- `LOCKED`
- `CLOSED`
- `UNDER_REVIEW`

Types de mouvement:

- `ADMIN_TOPUP`
- `ADMIN_DEBIT`
- `PAYMENT`
- `REFUND`
- `REVERSAL`
- `HOLD`
- `HOLD_RELEASE`
- `ADJUSTMENT`
- `CASHBACK`
- `PROMOTIONAL_CREDIT`
- `OVERPAYMENT_CREDIT`

### Creer ou recuperer un wallet

Endpoint:

`POST /wallets/admin/get-or-create`

Payload:

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-000001",
  "currency": "XAF"
}
```

Si le wallet existe, il est retourne. Sinon il est cree automatiquement.

### Recharger un wallet

Endpoint:

`POST /wallets/admin/top-up`

Payload:

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-000001",
  "currency": "XAF",
  "amount": 100000,
  "createdBy": "admin-001",
  "reference": "CASH-TOPUP-001"
}
```

Le backend:

- cree le wallet si necessaire
- credite le solde
- ecrit un mouvement ledger `ADMIN_TOPUP`

### Detail wallet

Endpoint:

`GET /wallets/{walletNumber}`

### Ledger wallet

Endpoint:

`GET /wallets/{walletNumber}/ledger`

Pagination:

- `page`
- `size`
- `sort`

### Wallet holds

Creer une reservation wallet:

`POST /wallet-holds`

```json
{
  "walletNumber": "WAL-000001",
  "amount": 25000,
  "sourceType": "BOOKING",
  "sourceCode": "BKG-000001",
  "expiresAt": "2026-04-17T18:00:00Z",
  "createdBy": "admin-001"
}
```

Capturer:

`PATCH /wallet-holds/{holdNumber}/capture?createdBy=admin-001`

Liberer:

`PATCH /wallet-holds/{holdNumber}/release?createdBy=admin-001`

Les holds expirent automatiquement via worker.

## Reconciliation

Creer un lot de rapprochement:

`POST /payments/reconciliation-batches`

```json
{
  "provider": "CASH",
  "createdBy": "admin-001"
}
```

Marquer termine:

`PATCH /payments/reconciliation-batches/{batchNumber}/complete`

## Workers actifs

- Billing overdue: marque les factures en retard.
- Payment intent expiry: expire les intentions non payees.
- Wallet hold expiry: libere les holds expires.

## Mobile Money

Le contrat technique est prepare mais l'integration fournisseur n'est pas encore implementee.

Provider actuel:

`NoopMobileMoneyPaymentProvider`

Interface future:

- `initiate`
- `checkStatus`
- `refund`

Le frontend ne doit pas encore exposer le paiement mobile money comme disponible en production.

## Workflows recommandes

### Facture manuelle pour client enregistre

1. Selectionner un `CUSTOMER`, `MEMBER` ou `BUSINESS_ENTITY`.
2. Envoyer `customerType/customerCode`.
3. Saisir les lignes.
4. Appeler `POST /billing/invoices/manual`.
5. Appeler `PATCH /billing/documents/{documentNumber}/issue`.
6. Creer un payment intent depuis la facture.
7. Payer par cash ou wallet.

### Facture manuelle pour client externe

1. Saisir `customerName`, email, phone.
2. Ne pas envoyer `customerType/customerCode`.
3. Appeler `POST /billing/invoices/manual`.
4. Le backend genere un code client billing.

### Devis puis facture

1. Appeler `POST /billing/quotations/manual`.
2. Envoyer au client avec le PDF.
3. Accepter le devis.
4. Convertir en facture.
5. Creer un payment intent depuis la facture.
6. Encaisser.

### Facture depuis inventory ou autre module

Option 1: facture manuelle avec ligne sourcee:

```json
{
  "lineType": "PRODUCT",
  "itemCode": "ITEM-001",
  "sourceType": "INVENTORY_ITEM",
  "sourceCode": "ITEM-001"
}
```

Option 2: le module inventory cree des `BillableItem`, puis:

`POST /billing/invoices/from-billable-items`

Le billing engine ne depend pas de subscription.

## Points restant pour une V3

- export comptable complet
- workflow approval pour ajustements wallet sensibles
- integration mobile money reelle
- rapprochement automatique fournisseur avec import de fichiers
