   # Guide Frontend - Paiement, Facture, Recu et Caisse

## Objectif

Ce document explique comment integrer cote frontend les nouveaux flux du module paiement et les nouvelles capacites liees aux factures.

Il couvre surtout:

- paiement depuis une facture
- paiement de plusieurs factures
- recherche des dettes d'un `member`, `customer` ou `business/company`
- affichage du recu apres paiement
- exploitation des nouvelles metriques caisse
- affichage des nouvelles informations enrichies dans `paymentIntent`, facture et mouvements de caisse

Base API:

- `/sni/api/v1/payments`
- `/sni/api/v1/billing`
- `/sni/api/v1/cash-registers`

## Regles frontend importantes

### 1. Ne pas demander les references generees

Le frontend ne doit pas demander en saisie libre:

- `intentNumber`
- `transactionNumber`
- `receiptNumber`
- `registerCode`
- `deviceCode`
- `businessEntityCode`
- `sessionNumber`
- `movementNumber`

Le backend les genere.

### 2. `providerReference` devient optionnel

Le frontend peut envoyer `providerReference` si une vraie reference externe existe deja:

- numero de cheque
- reference virement
- reference mobile money
- numero recu papier deja emis

Sinon:

- ne pas forcer la saisie
- laisser `providerReference` absent ou `null`
- le backend le genere automatiquement

### 3. Utiliser les objets resolus, pas seulement les codes techniques

Le frontend doit afficher prioritairement les champs enrichis:

- dans `PaymentIntentResponse`
  - `customerName`
  - `customerEmail`
  - `customerPhone`
  - `customerRegistered`
  - `resolvedSourceType`
  - `resolvedSourceCode`
  - `resolvedSourceLabel`
  - `resolvedSourceRegistered`
- dans `BillingDocumentResponse`
  - `customerRegistered`
  - `resolvedSourceType`
  - `resolvedSourceCode`
  - `resolvedSourceLabel`
  - `resolvedSourceRegistered`

Ne pas afficher seulement:

- `sourceType`
- `sourceCode`

Ces champs restent utiles pour le debug et le filtrage, mais l'UI metier doit preferer la source resolue.

## Ecrans frontend recommandes

### 1. Ecran "Dettes client / member / entreprise"

Objectif:

- charger tout ce que doit un payeur
- filtrer par type ou source metier
- selectionner une ou plusieurs factures
- lancer le paiement

API principale:

`GET /sni/api/v1/payments/payable-documents`

Filtres a exposer dans l'UI:

- `customerType`
- `customerCode`
- `documentType`
- `lineSourceType`
- `lineSourceCode`
- `searchText`

Exemples:

```http
GET /sni/api/v1/payments/payable-documents?customerType=MEMBER&customerCode=MBR-0001
```

```http
GET /sni/api/v1/payments/payable-documents?customerType=BUSINESS_ENTITY&customerCode=BIZ-ACME
```

```http
GET /sni/api/v1/payments/payable-documents?lineSourceType=SUBSCRIPTION&lineSourceCode=SUB-0001
```

Comportement UI conseille:

1. rechercher un member, customer ou entreprise
2. appeler `payable-documents`
3. afficher la liste avec:
   - numero facture
   - client
   - montant total
   - solde restant
   - devise
   - source resolue
4. permettre la selection multiple
5. proposer:
   - paiement total
   - paiement partiel
   - paiement avec avance

## Paiement depuis une facture

Le frontend a deux options.

### Option A. Flux recommande

Utiliser une `payment intent` puis executer le paiement.

Etapes:

1. charger la facture
2. creer l'intention
3. executer le paiement
4. recuperer le recu

### Creer l'intention depuis la facture

```http
POST /sni/api/v1/payments/intents/from-billing-document
```

Exemple:

```json
{
  "documentNumber": "INV-2026-0001",
  "amount": 15000,
  "idempotencyKey": "invoice-intent-0001",
  "expiresAt": "2026-04-30T18:00:00Z",
  "metadataJson": "{\"origin\":\"invoice-detail\"}"
}
```

Quand utiliser `amount`:

- paiement partiel
- paiement volontairement superieur au solde pour creer une avance

Quand omettre `amount`:

- bouton "payer le solde"

### Executer le paiement cash

```http
PATCH /sni/api/v1/payments/intents/{intentNumber}/cash
```

Exemple:

```json
{
  "receivedBy": "cashier-001",
  "cashSessionNumber": "CSS-2026-0001",
  "metadataJson": "{\"desk\":\"front\"}"
}
```

UI:

- `cashSessionNumber` doit etre selectionne depuis une session ouverte
- ne pas rendre `providerReference` obligatoire

### Executer le paiement wallet

```http
PATCH /sni/api/v1/payments/intents/{intentNumber}/wallet
```

Exemple:

```json
{
  "walletNumber": "WAL-0001",
  "createdBy": "admin-001",
  "metadataJson": "{\"origin\":\"invoice-detail\"}"
}
```

### Recuperer le recu

```http
GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt
```

ou

```http
GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt/pdf
```

### Option B. Paiement direct depuis l'ecran facture

```http
POST /sni/api/v1/billing/invoices/{documentNumber}/pay
```

Exemple:

```json
{
  "paymentMethod": "CASH",
  "amount": 15000,
  "cashSessionNumber": "CSS-2026-0001",
  "processedBy": "cashier-001",
  "idempotencyKey": "invoice-pay-0001",
  "metadataJson": "{\"origin\":\"invoice-detail\"}"
}
```

A utiliser quand l'UI veut un bouton simple "Payer maintenant" sans separer la creation d'intent.

La reponse retourne:

- `invoice`
- `transaction`

Bon usage frontend:

- recharger la facture avec la reponse `invoice`
- afficher le ticket de succes
- proposer l'ouverture du recu PDF

## Paiement de plusieurs factures

### Creer une intention pour plusieurs factures

```http
POST /sni/api/v1/payments/intents/from-billing-documents
```

Exemple:

```json
{
  "documentNumbers": [
    "INV-2026-0001",
    "INV-2026-0002"
  ],
  "amount": 30000,
  "idempotencyKey": "multi-docs-pay-0001",
  "expiresAt": "2026-04-30T18:00:00Z",
  "metadataJson": "{\"origin\":\"customer-debt-screen\"}"
}
```

UI conseillee:

- liste selectable avec checkbox
- resume du total du
- champ montant libre optionnel si paiement partiel global
- bouton de paiement

## Recouvrement global d'un client

Quand le frontend ne veut pas seulement payer des factures existantes, mais reconstituer toute la dette d'un client:

### Apercu

```http
GET /sni/api/v1/payments/recovery/{customerType}/{customerCode}
```

Exemple:

```http
GET /sni/api/v1/payments/recovery/MEMBER/MBR-0001
```

Cet endpoint permet d'afficher:

- total des factures ouvertes
- total des billable items encore non factures
- total payable
- intention en attente reutilisable si elle existe deja

### Creation d'intention de recouvrement

```http
POST /sni/api/v1/payments/intents/recovery
```

Exemple:

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-0001",
  "currency": "XAF",
  "expiresAt": "2026-04-30T18:00:00Z",
  "metadataJson": "{\"origin\":\"recovery-screen\"}"
}
```

## Ecran Recu

Le frontend doit idealement proposer:

- vue JSON detaillee
- bouton PDF
- lien depuis transaction, facture et mouvement de caisse

Endpoints:

- `GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt`
- `GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt/pdf`
- `GET /sni/api/v1/payments/receipts/{receiptNumber}`
- `GET /sni/api/v1/payments/receipts/{receiptNumber}/pdf`

Champs utiles a afficher:

- `receiptNumber`
- `transactionNumber`
- `intentNumber`
- `paymentMethod`
- `provider`
- `providerReference`
- `customerName`
- `paidAmount`
- `allocatedAmount`
- `advanceAmount`
- `paidAt`
- `receivedBy`
- `allocations`

Si `advanceAmount > 0`:

- afficher clairement qu'une partie du paiement a ete transformee en avance

## Facture et detail facture

Sur l'ecran detail facture, afficher:

- `customerName`
- `customerEmail`
- `customerPhone`
- `balanceDue`
- `status`
- `resolvedSourceLabel`
- `resolvedSourceType`
- `resolvedSourceCode`

API de lecture:

- `GET /sni/api/v1/billing/documents/{documentNumber}`
- `GET /sni/api/v1/billing/documents/{documentNumber}/pdf`

Liste facture:

- `GET /sni/api/v1/billing/documents`

Filtres utiles:

- `type`
- `status`
- `customerType`
- `customerCode`
- `sourceType`
- `sourceCode`
- `fromDate`
- `toDate`
- `searchText`

## Echeanciers de facture

Si l'UI gere le paiement en plusieurs tranches:

- `POST /sni/api/v1/billing/invoices/{documentNumber}/schedule`
- `GET /sni/api/v1/billing/invoices/{documentNumber}/schedule`
- `GET /sni/api/v1/billing/schedules/{scheduleNumber}`
- `POST /sni/api/v1/billing/schedules/installments/{installmentNumber}/pay`
- `DELETE /sni/api/v1/billing/schedules/{scheduleNumber}`

UX conseillee:

1. afficher l'echeancier sur la page facture
2. afficher les tranches avec statut
3. proposer un bouton "payer cette echeance"
4. reutiliser le meme formulaire que `PayInvoiceRequest`

## Ecran caisse

### Ce que le frontend n'a plus a saisir

Les codes suivants sont generes automatiquement:

- `businessEntityCode`
- `deviceCode`
- `registerCode`

Le frontend doit plutot collecter:

- nom caisse
- informations fonctionnelles minimales

### Metriques globales

```http
GET /sni/api/v1/cash-registers/metrics/overview
```

Filtres:

- `registerCode`
- `businessEntityCode`
- `fromDate`
- `toDate`

Usage UI:

- dashboard caisse
- resume fin de journee
- suivi multi-caisses

### Metriques par caisse

```http
GET /sni/api/v1/cash-registers/metrics/registers
```

Usage UI:

- tableau comparatif des caisses
- suivi des caisses actives
- surveillance des sessions ouvertes et en revue

### Liste des mouvements

```http
GET /sni/api/v1/cash-registers/movements
```

L'UI doit maintenant afficher plus de colonnes utiles:

- `registerCode`
- `registerName`
- `businessEntityCode`
- `deviceCode`
- `sessionStatus`
- `sessionOpenedBy`
- `sessionOpenedAt`
- `relatedTransactionNumber`
- `relatedIntentNumber`
- `relatedReceiptNumber`
- `relatedCustomerName`
- `relatedSourceLabel`

Usages:

- cliquer vers la transaction
- cliquer vers le recu
- cliquer vers la facture ou la source metier

## Parcours frontend recommandes

### Parcours 1. Ecran detail facture

1. `GET /billing/documents/{documentNumber}`
2. afficher facture et source resolue
3. bouton `Payer`
4. soit:
   - `POST /payments/intents/from-billing-document`
   - puis `PATCH /payments/intents/{intentNumber}/cash|wallet`
5. afficher le recu

### Parcours 2. Ecran dettes client

1. rechercher le payeur
2. `GET /payments/payable-documents`
3. selectionner une ou plusieurs factures
4. `POST /payments/intents/from-billing-documents`
5. finaliser le paiement
6. afficher le recu et recharger la liste

### Parcours 3. Guichet caisse

1. choisir une session de caisse ouverte
2. charger les dettes du client
3. creer l'intention
4. payer via `PATCH /payments/intents/{intentNumber}/cash`
5. afficher:
   - transaction
   - recu
   - mouvement de caisse cree

### Parcours 4. Recouvrement global

1. `GET /payments/recovery/{customerType}/{customerCode}`
2. afficher la dette globale
3. `POST /payments/intents/recovery`
4. executer le paiement
5. afficher l'avance creee si le montant depasse le du

## Recommandations UI

### Champs a rendre lecture seule ou caches

- tous les codes generes
- `providerReference` par defaut

### Champs a afficher en priorite

- nom du client
- source resolue
- solde restant
- avance creee
- numero de recu

### Messages utiles

- "Le backend generera la reference de paiement si elle n'est pas renseignee."
- "Une partie du paiement a ete creditee comme avance."
- "Le paiement a ete lie a la session de caisse selectionnee."
