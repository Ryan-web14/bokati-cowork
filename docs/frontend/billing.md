# Guide Frontend/API - Billing, Invoice et Quotation

Base API: `/sni/api/v1`

Ce document couvre le module billing/invoicing. Le module est un moteur general de documents commerciaux: factures, devis, proformas, avoirs et notes de debit. Il peut creer des documents manuellement pour un client enregistre ou non enregistre, ou automatiquement depuis des elements facturables.

## Principes

Le frontend ne doit pas saisir ce que le backend peut resoudre.

Champs generes par le backend:

- `documentNumber`
- `customerCode` interne quand le client n'est pas enregistre
- montants calcules: `subtotalAmount`, `discountAmount`, `taxableAmount`, `vatAmount`, `additionalCentAmount`, `taxAmount`, `totalAmount`, `paidAmount`, `balanceDue`
- dates systeme: `issuedAt`, `sentAt`, `paidAt`, `cancelledAt`

Pour un client enregistre, envoyer uniquement:

- `customerType`
- `customerCode`

Le backend recupere le nom, l'email, le telephone et les informations client disponibles.

Pour un client non enregistre, ne pas envoyer `customerType/customerCode`. Envoyer les informations snapshot:

- `customerName`
- `customerEmail`
- `customerPhone`
- `billingAddressJson`

`billingAddressJson` et `metadataJson` doivent etre des chaines JSON valides quand elles sont renseignees.

## Types et statuts

Types `BillingDocumentType`:

- `QUOTE`
- `PROFORMA_INVOICE`
- `INVOICE`
- `CREDIT_NOTE`
- `DEBIT_NOTE`

Statuts `BillingDocumentStatus`:

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

Types de lignes usuels:

- `SERVICE`
- `PRODUCT`
- `BOOKING`
- `SUBSCRIPTION`
- `PASS`
- `ADDON`
- `OVERAGE`
- `DISCOUNT`
- `FEE`
- `OTHER`

## Creation generique

Endpoint:

```http
POST /billing/documents
```

Utiliser cet endpoint quand le frontend veut choisir explicitement `documentType`.

```json
{
  "documentType": "PROFORMA_INVOICE",
  "customerType": "BUSINESS_ENTITY",
  "customerCode": "BUS-000001",
  "title": "Proforma produit",
  "description": "Vente produit inventaire",
  "currency": "XAF",
  "issueDate": "2026-04-17",
  "dueDate": "2026-04-30",
  "lines": [
    {
      "lineType": "PRODUCT",
      "itemCode": "ITEM-001",
      "description": "Produit inventaire",
      "detailedDescription": "Reference interne et details",
      "quantity": 3,
      "unitPrice": 12000,
      "discountRate": 0,
      "taxable": true,
      "sourceType": "INVENTORY_ITEM",
      "sourceCode": "ITEM-001"
    }
  ],
  "discounts": [],
  "clauses": []
}
```

## Auto-facturation systeme

Le billing expose un support interne d'auto-facturation pour les autres modules.

Quand une transaction systeme est confirmee et qu'elle n'est pas deja liee a une facture, le backend peut generer automatiquement une `INVOICE` avec:

- `sourceType`
- `sourceCode`
- client
- montant
- devise
- description

Pour les paiements, ce mecanisme est branche automatiquement:

- si l'intention vient de `BILLING_DOCUMENT`, aucune nouvelle facture n'est creee;
- si l'intention vient de `MULTI_BILLING_DOCUMENT`, aucune nouvelle facture n'est creee;
- sinon, une facture est creee avec `sourceType = PAYMENT_INTENT` et `sourceCode = intentNumber`, puis le paiement est alloue dessus.

La facture automatique est emise immediatement. Par defaut, la ligne est non taxable afin que le montant facture corresponde exactement au montant deja paye. Les modules qui veulent une fiscalite detaillee doivent creer des billable items ou une facture explicite avant paiement.

## Facture manuelle

Endpoint:

```http
POST /billing/invoices/manual
```

Client enregistre:

```json
{
  "customerType": "CUSTOMER",
  "customerCode": "CUS-000001",
  "title": "Facture prestation",
  "description": "Prestation coworking",
  "currency": "XAF",
  "issueDate": "2026-04-17",
  "dueDate": "2026-04-30",
  "lines": [
    {
      "lineType": "SERVICE",
      "description": "Location salle de reunion",
      "quantity": 2,
      "unitPrice": 25000,
      "taxable": true
    }
  ]
}
```

Client non enregistre:

```json
{
  "customerName": "Client Externe SARL",
  "customerEmail": "client@example.com",
  "customerPhone": "060000000",
  "billingAddressJson": "{\"city\":\"Brazzaville\",\"country\":\"CG\"}",
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

## Devis manuel

Endpoints:

```http
POST /billing/quotes/manual
POST /billing/quotations/manual
```

Payload identique a une facture manuelle. Le backend force `documentType = QUOTE`.

## Facture depuis billable items

Endpoint:

```http
POST /billing/invoices/from-billable-items
```

Le frontend envoie uniquement les numeros des elements facturables. Le backend deduit le client, la devise, les lignes et les sources.

```json
{
  "title": "Facture elements facturables",
  "description": "Facture generee depuis des billable items",
  "issueDate": "2026-04-17",
  "dueDate": "2026-04-30",
  "billableNumbers": [
    "BIL-000001",
    "BIL-000002"
  ]
}
```

## Taxes Congo CG

Pays fiscal: Congo `CG`.

Regles seed:

- `TVA_CG_18`: TVA 18%
- `CAC_CG_ON_VAT_5`: centimes additionnels 5% sur la TVA

Pour une ligne taxable, le backend calcule:

- TVA = base taxable * 18%
- centimes additionnels = TVA * 5%

Ligne non taxable:

```json
{
  "description": "Service exonere",
  "quantity": 1,
  "unitPrice": 100000,
  "taxable": false
}
```

Forcer les taux:

```json
{
  "vatRate": 18,
  "additionalCentRate": 5
}
```

## Remises

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

Types de remise:

- `PERCENTAGE`
- `FIXED_AMOUNT`

## Clauses

Les clauses sont des textes affiches sur le document et le PDF.

```json
{
  "clauses": [
    {
      "clauseCode": "CG_PAYMENT_TERMS",
      "title": "Conditions de paiement",
      "body": "Paiement exigible a la date indiquee.",
      "displayOrder": 10
    }
  ]
}
```

## Workflow document

Emettre:

```http
PATCH /billing/documents/{documentNumber}/issue
```

Envoyer:

```http
PATCH /billing/documents/{documentNumber}/send
```

Envoyer par email via le module core email:

```http
POST /billing/documents/{documentNumber}/send-email
```

Detail:

```http
GET /billing/documents/{documentNumber}
```

PDF:

```http
GET /billing/documents/{documentNumber}/pdf
```

## Workflow devis

Accepter:

```http
PATCH /billing/quotes/{quoteNumber}/accept
PATCH /billing/quotations/{quotationNumber}/accept
```

Rejeter:

```http
PATCH /billing/quotes/{quoteNumber}/reject
PATCH /billing/quotations/{quotationNumber}/reject
```

Convertir en facture:

```http
POST /billing/quotes/{quoteNumber}/convert-to-invoice
POST /billing/quotations/{quotationNumber}/convert-to-invoice
```

La conversion reprend automatiquement le client snapshot, les lignes, les remises, les taxes, les clauses et la devise.

## Avoirs

Creer un avoir depuis une facture:

```http
POST /billing/invoices/{invoiceNumber}/credit-note
```

```json
{
  "amount": 25000,
  "reason": "Correction commerciale",
  "createdBy": "admin-001"
}
```

Appliquer un avoir:

```http
PATCH /billing/credit-notes/{creditNoteNumber}/apply
```

## Releve client

```http
GET /billing/customers/{customerType}/{customerCode}/statement
```

Parametres:

- `page`
- `size`
- `sort`

## Listing et recherche

```http
GET /billing/documents
```

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
- `page`
- `size`
- `sort`

Exemple:

```http
GET /billing/documents?type=INVOICE&searchText=client&page=0&size=20&sort=createdAt,desc
```

`searchText` s'appuie sur les index PostgreSQL `pg_trgm` du module billing.

## Reponse principale

`BillingDocumentResponse` contient:

- informations document: numero, type, statut, titre, description, source
- snapshot client: type, code, nom, email, telephone, adresse
- montants: subtotal, remise, taxable, TVA, centimes additionnels, total, paye, reste du
- dates: issue, due, issued, sent, paid, cancelled
- lignes
- remises
- taxes
- clauses
- `metadataJson`

## Workflows frontend

Facture manuelle client enregistre:

1. Selectionner le client.
2. Envoyer `customerType/customerCode`.
3. Saisir les lignes.
4. Appeler `POST /billing/invoices/manual`.
5. Appeler `PATCH /billing/documents/{documentNumber}/issue`.
6. Creer une intention de paiement depuis la facture.

Facture manuelle client externe:

1. Saisir le snapshot client.
2. Ne pas envoyer `customerType/customerCode`.
3. Appeler `POST /billing/invoices/manual`.
4. Le backend genere le code client billing.

Devis puis facture:

1. Appeler `POST /billing/quotations/manual`.
2. Afficher ou envoyer le PDF.
3. Accepter le devis.
4. Convertir en facture.
5. Creer une intention de paiement.

Facture inventory ou autre module:

1. Option simple: facture manuelle avec `lineType = PRODUCT`, `sourceType = INVENTORY_ITEM`, `sourceCode = itemCode`.
2. Option automatique: le module source cree des billable items, puis `POST /billing/invoices/from-billable-items`.
