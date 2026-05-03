# Endpoints Backend Paiement et Facturation

## Objectif

Ce document liste les endpoints backend ajoutés ou enrichis autour du module `payment` et des `billing documents`.

Il complète [payment-backend-workflow.md](/C:/java-project/bokati-cowork/docs/payment-backend-workflow.md) en se concentrant sur le contrat d'API:

- création d'intentions de paiement
- recherche de factures payables
- paiement depuis une facture
- reçus de paiement
- métriques et détails de caisse
- échéanciers liés aux factures

Base path principal:

- ` /sni/api/v1/payments`
- ` /sni/api/v1/billing`
- ` /sni/api/v1/cash-registers`

## Règles backend importantes

### 1. Payment intent enrichi

Les réponses `PaymentIntentResponse` retournent maintenant, quand l'objet existe en base:

- `customerName`
- `customerEmail`
- `customerPhone`
- `billingAddressJson`
- `customerRegistered`
- `resolvedSourceType`
- `resolvedSourceCode`
- `resolvedSourceLabel`
- `resolvedSourceRegistered`

Le backend résout donc le vrai payeur enregistré et la vraie source métier de la transaction, même si la source technique initiale est un `BILLING_DOCUMENT` ou un `BILLABLE_ITEM`.

### 2. Facture enrichie

`BillingDocumentResponse` retourne aussi:

- `customerRegistered`
- `resolvedSourceType`
- `resolvedSourceCode`
- `resolvedSourceLabel`
- `resolvedSourceRegistered`

Cela permet de voir sur une facture si la source réelle est une réservation, un abonnement, un pass ou plusieurs sources.

### 3. Provider reference automatique

Lorsqu'une transaction de paiement est créée sans `providerReference` fourni par le client, le backend la génère automatiquement.

Comportement actuel:

- si `providerReference` est fourni dans la requête, il est conservé
- sinon, le backend génère une valeur de type `PREF-...`
- cette logique couvre les paiements d'intent et les paiements de facture

### 4. Génération automatique d'intention pour les factures métier

Quand une facture est créée automatiquement depuis un `billable item` métier, une `payment intent` liée à la facture est aussi créée automatiquement.

Cela couvre les cas typiques:

- abonnement
- pass
- réservation
- autres objets facturables supportés

## Endpoints Paiement

### POST `/sni/api/v1/payments/intents`

Crée une intention de paiement libre.

Payload:

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-0001",
  "amount": 25000,
  "currency": "XAF",
  "purpose": "Paiement abonnement coworking",
  "sourceType": "SUBSCRIPTION",
  "sourceCode": "SUB-0001",
  "idempotencyKey": "payment-sub-0001",
  "expiresAt": "2026-04-30T12:00:00Z",
  "metadataJson": "{\"origin\":\"backoffice\"}"
}
```

Usage:

- paiement direct hors facture
- création manuelle d'intent
- sources métier non strictement liées à une facture existante

### POST `/sni/api/v1/payments/intents/from-billing-document`

Crée une intention de paiement à partir d'une facture.

Payload:

```json
{
  "documentNumber": "INV-2026-0001",
  "amount": 15000,
  "idempotencyKey": "intent-invoice-0001",
  "expiresAt": "2026-04-30T12:00:00Z",
  "metadataJson": "{\"origin\":\"portal\"}"
}
```

Notes:

- `amount` est optionnel
- si `amount` est absent, le backend prend en général le solde restant
- si `amount` est supérieur au solde, le trop-perçu est géré comme avance via wallet
- la source technique de l'intent est `BILLING_DOCUMENT`
- la réponse remonte aussi la source métier résolue

### POST `/sni/api/v1/payments/intents/from-billing-documents`

Crée une intention de paiement pour plusieurs factures.

Payload:

```json
{
  "documentNumbers": [
    "INV-2026-0001",
    "INV-2026-0002"
  ],
  "amount": 30000,
  "idempotencyKey": "intent-batch-0001",
  "expiresAt": "2026-04-30T12:00:00Z",
  "metadataJson": "{\"origin\":\"cashier\"}"
}
```

Usage:

- paiement groupé de plusieurs factures
- règlement consolidé d'un member, customer ou entreprise

### GET `/sni/api/v1/payments/payable-documents`

Recherche les factures payables pour construire une sélection avant paiement.

Filtres:

- `documentType`
- `customerType`
- `customerCode`
- `lineSourceType`
- `lineSourceCode`
- `searchText`
- pagination standard Spring

Exemples:

- `GET /sni/api/v1/payments/payable-documents?customerType=MEMBER&customerCode=MBR-0001`
- `GET /sni/api/v1/payments/payable-documents?lineSourceType=SUBSCRIPTION&lineSourceCode=SUB-0001`
- `GET /sni/api/v1/payments/payable-documents?customerType=BUSINESS_ENTITY&customerCode=BIZ-ACME`

Usage:

- charger ce qu'un member doit
- charger ce qu'un customer doit
- charger ce qu'une entreprise doit
- filtrer les dettes par réservation, pass ou abonnement

### GET `/sni/api/v1/payments/recovery/{customerType}/{customerCode}`

Prévisualise le recouvrement d'un client:

- factures ouvertes
- billable items encore non convertis en facture
- total payable

Exemple:

- `GET /sni/api/v1/payments/recovery/MEMBER/MBR-0001`

### POST `/sni/api/v1/payments/intents/recovery`

Crée une intention de recouvrement globale pour un client.

Payload:

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-0001",
  "currency": "XAF",
  "expiresAt": "2026-04-30T12:00:00Z",
  "metadataJson": "{\"origin\":\"recovery-screen\"}"
}
```

Note:

- si des billable items doivent d'abord être facturés, le backend peut créer les factures nécessaires avant l'intent

### PATCH `/sni/api/v1/payments/intents/{intentNumber}/cash`

Enregistre un paiement cash sur une intention.

Payload:

```json
{
  "receivedBy": "cashier-001",
  "cashSessionNumber": "CSS-2026-0001",
  "providerReference": null,
  "metadataJson": "{\"desk\":\"front\"}"
}
```

Notes:

- `providerReference` peut être omis
- s'il est absent, le backend le génère automatiquement
- si `cashSessionNumber` est fourni, un mouvement de caisse lié à la transaction est créé

### PATCH `/sni/api/v1/payments/intents/{intentNumber}/wallet`

Paie une intention via wallet.

Payload:

```json
{
  "walletNumber": "WAL-0001",
  "createdBy": "admin-001",
  "metadataJson": "{\"origin\":\"wallet\"}"
}
```

### GET `/sni/api/v1/payments/intents/{intentNumber}`

Retourne une intention enrichie.

### GET `/sni/api/v1/payments/intents/{intentNumber}/transactions`

Retourne les transactions liées à une intention.

### GET `/sni/api/v1/payments/intents`

Liste paginée des intentions.

Filtres:

- `status`
- `customerType`
- `customerCode`
- `sourceType`
- `sourceCode`
- `searchText`

### POST `/sni/api/v1/payments/transactions/{transactionNumber}/refund`

Rembourse une transaction.

### POST `/sni/api/v1/payments/transactions/{transactionNumber}/reverse`

Annule ou reverse une transaction.

## Endpoints Reçu

### GET `/sni/api/v1/payments/transactions/{transactionNumber}/receipt`

Retourne le reçu JSON d'une transaction.

### GET `/sni/api/v1/payments/transactions/{transactionNumber}/receipt/pdf`

Retourne le reçu PDF d'une transaction.

### GET `/sni/api/v1/payments/receipts/{receiptNumber}`

Recherche un reçu par son numéro.

### GET `/sni/api/v1/payments/receipts/{receiptNumber}/pdf`

Retourne le reçu PDF par numéro de reçu.

Le reçu expose notamment:

- `receiptNumber`
- `transactionNumber`
- `intentNumber`
- `paymentMethod`
- `provider`
- `providerReference`
- `customerType`
- `customerCode`
- `customerName`
- `paidAmount`
- `allocatedAmount`
- `advanceAmount`
- `allocations`

## Paiement depuis une facture

Le backend propose maintenant deux approches principales pour payer une facture.

### Option A. Créer d'abord une intention depuis la facture

Flux recommandé pour une UI moderne:

1. charger la facture
2. créer une intention via `POST /payments/intents/from-billing-document`
3. exécuter le paiement via `PATCH /payments/intents/{intentNumber}/cash` ou `wallet`
4. récupérer le reçu

Avantages:

- traçabilité complète
- réutilisation du workflow payment intent
- meilleure compatibilité avec paiement partiel et avance

### Option B. Payer directement la facture

### POST `/sni/api/v1/billing/invoices/{documentNumber}/pay`

Paie directement une facture depuis le module billing.

Payload:

```json
{
  "paymentMethod": "CASH",
  "amount": 15000,
  "walletNumber": null,
  "cashSessionNumber": "CSS-2026-0001",
  "providerReference": null,
  "processedBy": "cashier-001",
  "idempotencyKey": "pay-invoice-0001",
  "metadataJson": "{\"origin\":\"invoice-screen\"}"
}
```

Règles:

- `amount` est optionnel
- paiement partiel supporté
- trop-perçu supporté
- avance supportée via crédit wallet
- `providerReference` est généré automatiquement si absent
- retourne:
  - `invoice`
  - `transaction`

## Endpoints Billing utiles autour du paiement

### GET `/sni/api/v1/billing/documents/{documentNumber}`

Retourne une facture ou un devis enrichi avec:

- client réel si enregistré
- source métier résolue
- détails documentaires

### GET `/sni/api/v1/billing/documents`

Liste paginée des documents de facturation.

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

### POST `/sni/api/v1/billing/invoices/from-billable-items`

Crée une facture depuis un ensemble de `billable items`.

Note importante:

- ce flux crée aussi automatiquement une `payment intent` liée à la facture générée

## Echéanciers de facture

Les endpoints d'échéancier complètent le paiement d'une facture quand le règlement est fractionné.

### POST `/sni/api/v1/billing/invoices/{documentNumber}/schedule`

Crée un échéancier pour une facture.

### GET `/sni/api/v1/billing/invoices/{documentNumber}/schedule`

Retourne l'échéancier actif d'une facture.

### GET `/sni/api/v1/billing/schedules/{scheduleNumber}`

Retourne un échéancier par numéro.

### POST `/sni/api/v1/billing/schedules/installments/{installmentNumber}/pay`

Paie une échéance avec le même payload que `PayInvoiceRequest`.

Cela signifie:

- paiement partiel possible
- `providerReference` optionnel
- génération automatique si absent

### DELETE `/sni/api/v1/billing/schedules/{scheduleNumber}`

Annule un échéancier actif.

## Caisse et mouvements détaillés

## GET `/sni/api/v1/cash-registers/metrics/overview`

Retourne des métriques globales de caisse.

Filtres:

- `registerCode`
- `businessEntityCode`
- `fromDate`
- `toDate`

Contenu principal:

- `registerCount`
- `activeRegisterCount`
- `openSessionCount`
- `closingReviewSessionCount`
- `closedSessionCount`
- `movementCount`
- `openingFloatAmount`
- `totalPayments`
- `totalRefunds`
- `totalCashIn`
- `totalCashOut`
- `totalAdjustments`
- `netCashPosition`
- `pendingVarianceAmount`

## GET `/sni/api/v1/cash-registers/metrics/registers`

Retourne les métriques par caisse.

Filtres:

- `businessEntityCode`
- `fromDate`
- `toDate`

Contenu principal:

- `registerCode`
- `registerName`
- `businessEntityCode`
- `deviceCode`
- `openSessionCount`
- `closingReviewSessionCount`
- `movementCount`
- `totalPayments`
- `totalRefunds`
- `totalCashIn`
- `totalCashOut`
- `totalAdjustments`
- `netCashPosition`

## Détails supplémentaires dans les mouvements de caisse

Les réponses de mouvements de caisse retournent maintenant beaucoup plus de détails:

- `registerName`
- `businessEntityCode`
- `deviceCode`
- `sessionStatus`
- `sessionOpenedBy`
- `sessionOpenedAt`
- `relatedTransactionNumber`
- `relatedIntentNumber`
- `relatedReceiptNumber`
- `relatedCustomerType`
- `relatedCustomerCode`
- `relatedCustomerName`
- `relatedSourceType`
- `relatedSourceCode`
- `relatedSourceLabel`

Cela simplifie:

- l'audit de caisse
- le rapprochement entre caisse et paiement
- la recherche d'un reçu depuis un mouvement
- le suivi métier réel de la transaction

## Recommandations d'intégration

### Pour payer une facture unitaire

- utiliser `GET /payments/payable-documents`
- puis `POST /payments/intents/from-billing-document`
- puis `PATCH /payments/intents/{intentNumber}/cash` ou `wallet`
- puis `GET /payments/transactions/{transactionNumber}/receipt`

### Pour payer plusieurs dettes d'un client

- utiliser `GET /payments/payable-documents` avec `customerType` et `customerCode`
- laisser l'utilisateur sélectionner les factures
- appeler `POST /payments/intents/from-billing-documents`
- finaliser le paiement

### Pour le paiement direct depuis l'écran facture

- utiliser `POST /billing/invoices/{documentNumber}/pay`
- récupérer ensuite le reçu par `transactionNumber`

### Pour abonnement, pass, réservation

- s'appuyer sur la source métier réelle renvoyée par `resolvedSourceType` et `resolvedSourceCode`
- ne plus dépendre uniquement de `sourceCode` ou du `billable item`
