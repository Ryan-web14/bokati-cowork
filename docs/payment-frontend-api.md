ces nouvelle # Guide Frontend/API - Payment et Cash Register

Base API: `/sni/api/v1`

Ce document couvre le module payment et la caisse cash register. Le paiement passe par une intention de paiement, puis par une transaction. Les paiements cash peuvent etre lies a une session de caisse pour alimenter le journal de caisse et le controle de fermeture.

## Principes

Champs generes par le backend:

- `intentNumber`
- `transactionNumber`
- `batchNumber`
- `registerCode`
- `sessionNumber`
- `movementNumber`
- `expectedClosingAmount`
- `varianceAmount`

Le frontend doit envoyer uniquement l'identifiant suffisant:

- pour payer une facture: envoyer `documentNumber`
- pour payer plusieurs factures: envoyer les `documentNumbers`
- pour encaisser cash: envoyer `cashSessionNumber` si une session existe
- pour payer wallet: envoyer `walletNumber`

Les paiements cash sont autorises uniquement en `XAF`.

## Payment intents

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

Methodes:

- `CASH`
- `WALLET`
- mobile money prepare cote provider, non expose comme production tant que le provider reel n'est pas branche

## Creer une intention manuelle

```http
POST /payments/intents
```

```json
{
  "customerType": "CUSTOMER",
  "customerCode": "CUS-000001",
  "amount": 50000,
  "currency": "XAF",
  "purpose": "MANUAL_PAYMENT",
  "sourceType": "MANUAL",
  "sourceCode": "REF-001",
  "idempotencyKey": "manual-payment-REF-001",
  "expiresAt": "2026-04-17T18:00:00Z",
  "metadataJson": "{\"channel\":\"frontdesk\"}"
}
```

`metadataJson` doit etre une chaine JSON valide.

## Creer une intention depuis une facture

```http
POST /payments/intents/from-billing-document
```

```json
{
  "documentNumber": "INV-202604-000001",
  "idempotencyKey": "pay-INV-202604-000001",
  "expiresAt": "2026-04-17T18:00:00Z",
  "metadataJson": "{\"origin\":\"billing\"}"
}
```

Le backend deduit:

- client
- montant restant du
- devise
- `sourceType = BILLING_DOCUMENT`
- `sourceCode = documentNumber`

## Creer une intention pour plusieurs factures

```http
POST /payments/intents/from-billing-documents
```

```json
{
  "documentNumbers": [
    "INV-202604-000001",
    "INV-202604-000002"
  ],
  "idempotencyKey": "pay-batch-202604-001",
  "expiresAt": "2026-04-17T18:00:00Z",
  "metadataJson": "{\"origin\":\"bulk-payment\"}"
}
```

Le backend verifie que les factures ont le meme client et la meme devise.

## Recouvrement automatique member/customer

Le frontend peut demander au backend de reconstruire tout ce qui reste a payer pour un `customerType/customerCode`:

- factures ouvertes non soldées
- proformas ouvertes
- reservations, addons, overages et autres `billable_item` encore non factures
- reprise d'une intention de paiement deja en attente si l'utilisateur revient plus tard

Types supportes selon les donnees du dossier:

- `MEMBER`
- `CUSTOMER`
- autres types deja manipules par le module billing/payment si des `billable_item` existent

### Apercu du reste a payer

```http
GET /payments/recovery/{customerType}/{customerCode}
```

Exemple:

```http
GET /payments/recovery/MEMBER/MBR-000003
```

Optionnel:

- `currency`

Si plusieurs devises ouvertes existent, le frontend doit preciser la devise:

```http
GET /payments/recovery/CUSTOMER/CUS-000123?currency=XAF
```

Reponse cible:

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-000003",
  "currency": "XAF",
  "openDocumentAmount": 15000,
  "pendingBillableAmount": 5000,
  "totalPayableAmount": 20000,
  "documents": [
    {
      "documentNumber": "INV-MEM-20260426-00000001",
      "documentType": "INVOICE",
      "status": "ISSUED",
      "balanceDue": 15000
    }
  ],
  "pendingBillableItems": [
    {
      "billableNumber": "BIL-202604-00000001",
      "sourceType": "BOOKING_RESERVATION",
      "amount": 5000,
      "currency": "XAF",
      "status": "PENDING"
    }
  ],
  "paymentIntent": null
}
```

Interpretation:

- `openDocumentAmount`: total deja facture mais non regle
- `pendingBillableAmount`: total pris/consomme mais pas encore facture
- `totalPayableAmount`: total global a afficher au frontend

### Creer ou reprendre une intention de recouvrement

```http
POST /payments/intents/recovery
```

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-000003",
  "currency": "XAF",
  "expiresAt": "2026-04-27T18:00:00Z",
  "metadataJson": "{\"origin\":\"member-portal\"}"
}
```

Comportement backend:

- recupere les factures/proformas encore payables
- recupere aussi les `billable_item` non encore factures, comme une reservation prise non encore payee
- si des `billable_item` existent, cree automatiquement une facture puis l'emet
- calcule l'ensemble payable final
- si une intention de recouvrement encore valide existe deja pour exactement le meme ensemble, la retourne pour continuer le paiement
- sinon cree une nouvelle intention `sourceType = PAYABLE_RECOVERY`

Le frontend peut donc:

1. appeler le preview pour afficher le resume
2. appeler `POST /payments/intents/recovery`
3. rediriger l'utilisateur vers le paiement cash, wallet ou provider externe a partir de `paymentIntent`
4. si l'utilisateur revient plus tard, rappeler le meme endpoint pour reprendre l'intention existante

## Payer en cash

```http
PATCH /payments/intents/{intentNumber}/cash
```

```json
{
  "receivedBy": "admin-001",
  "cashSessionNumber": "CSS-202604-000001",
  "providerReference": "RECU-001",
  "metadataJson": "{\"locationCode\":\"HQ\"}"
}
```

Traitement backend:

- verifie que l'intention est payable
- verifie que la devise est `XAF`
- cree une `PaymentTransaction` en `SUCCEEDED`
- si `cashSessionNumber` est fourni, cree un mouvement caisse `PAYMENT`
- alloue le paiement aux factures si la source est billing
- alloue aussi automatiquement les intentions `PAYABLE_RECOVERY` sur toutes les factures ouvertes du recouvrement
- si aucune facture n'est liee, genere automatiquement une facture puis alloue le paiement dessus
- met a jour la facture en `PARTIALLY_PAID` ou `PAID`
- credite le trop-percu sur wallet client si necessaire

## Payer avec wallet

```http
PATCH /payments/intents/{intentNumber}/wallet
```

```json
{
  "walletNumber": "WAL-000001",
  "createdBy": "admin-001",
  "metadataJson": "{\"source\":\"customer-wallet\"}"
}
```

Validation backend:

- wallet actif
- proprietaire wallet = client de l'intention
- devise wallet = devise intention
- solde disponible suffisant
- si aucune facture n'est liee, generation automatique d'une facture puis allocation du paiement

## Detail et listing payment intents

Detail:

```http
GET /payments/intents/{intentNumber}
```

Transactions d'une intention:

```http
GET /payments/intents/{intentNumber}/transactions
```

Cette route retourne la liste des `PaymentTransactionResponse` lies a l'intention, tries du plus recent au plus ancien.

Listing:

```http
GET /payments/intents
```

Filtres:

- `status`
- `customerType`
- `customerCode`
- `sourceType`
- `sourceCode`
- `searchText`
- `page`
- `size`
- `sort`

Exemple:

```http
GET /payments/intents?status=PENDING&searchText=CUS-000001&page=0&size=20
```

## Refund et reverse

Rembourser:

```http
POST /payments/transactions/{transactionNumber}/refund
```

Annuler/reverser:

```http
POST /payments/transactions/{transactionNumber}/reverse
```

```json
{
  "amount": 10000,
  "reason": "Erreur d'encaissement",
  "processedBy": "admin-001"
}
```

Le backend inverse les allocations facture. Pour un paiement wallet, il credite automatiquement le wallet.

## Reconciliation payment

Creer un lot:

```http
POST /payments/reconciliation-batches
```

```json
{
  "provider": "CASH",
  "createdBy": "admin-001"
}
```

Marquer termine:

```http
PATCH /payments/reconciliation-batches/{batchNumber}/complete
```

## Cash register

La caisse est rattachee au module payment. Elle permet de controler les encaissements cash par session, caissier, emplacement et mouvements.

### Creer une caisse

```http
POST /cash-registers
```

```json
{
  "name": "Caisse reception",
  "locationCode": "HQ",
  "businessEntityCode": "BUS-000001",
  "deviceCode": "POS-RECEPTION-01",
  "cashControlEnabled": true,
  "maxCashAmount": 500000
}
```

Le backend genere `registerCode`.

### Lister les caisses

```http
GET /cash-registers
```

Filtres:

- `active`
- `locationCode`
- `businessEntityCode`
- `searchText`
- `page`
- `size`

### Activer/desactiver

```http
PATCH /cash-registers/{registerCode}/activate
PATCH /cash-registers/{registerCode}/deactivate
```

Une caisse avec une session active ne doit pas etre desactivee.

### Ouvrir une session

```http
POST /cash-registers/sessions
```

```json
{
  "registerCode": "CSR-00001",
  "openedBy": "admin-001",
  "openingAmount": 50000
}
```

Regles:

- une caisse inactive ne peut pas ouvrir de session
- une caisse ne peut avoir qu'une session `OPEN` ou `CLOSING_REVIEW`
- un caissier ne peut avoir qu'une session active
- si `openingAmount > 0`, le backend cree un mouvement `OPENING_FLOAT`

### Lister les sessions

```http
GET /cash-registers/sessions
```

Filtres:

- `registerCode`
- `status`
- `openedBy`
- `closedBy`
- `searchText`
- `page`
- `size`

Exemple:

```http
GET /cash-registers/sessions?status=OPEN&registerCode=CSR-00001&page=0&size=20
```

### Resume de session

```http
GET /cash-registers/sessions/{sessionNumber}/summary
```

Reponse cible:

```json
{
  "sessionNumber": "CSS-202604-000001",
  "registerCode": "CSR-00001",
  "status": "OPEN",
  "openedBy": "admin-001",
  "closedBy": null,
  "openingAmount": 50000,
  "totalPayments": 125000,
  "totalRefunds": 10000,
  "totalCashIn": 20000,
  "totalCashOut": 5000,
  "totalAdjustments": 0,
  "expectedClosingAmount": 180000,
  "countedClosingAmount": null,
  "varianceAmount": null
}
```

Calcul:

```text
expectedClosingAmount =
openingAmount
+ totalPayments
+ totalCashIn
+ totalAdjustments
- totalRefunds
- totalCashOut
```

### Mouvements manuels

```http
POST /cash-registers/sessions/{sessionNumber}/movements
```

```json
{
  "movementType": "CASH_OUT",
  "amount": 25000,
  "documentType": "EXIT_VOUCHER",
  "documentNumber": "BS-202604-000001",
  "flowCategory": "SUPPLIER_PAYMENT",
  "referenceType": "SAFE_DEPOSIT",
  "referenceCode": "SAFE-001",
  "counterpartyType": "SUPPLIER",
  "counterpartyCode": "SUP-001",
  "counterpartyName": "Fournisseur ABC",
  "reason": "Depot coffre",
  "createdBy": "admin-001",
  "metadataJson": "{\"department\":\"operations\"}"
}
```

Champs enrichis pour tous les flux caisse:

- `documentType`
- `documentNumber`
- `flowCategory`
- `counterpartyType`
- `counterpartyCode`
- `counterpartyName`
- `metadataJson`

Cela permet de suivre des flux externes et internes sans les limiter aux operations coworking:

- avance employe
- paiement fournisseur
- encaissement partenaire
- depot bancaire
- retrait bancaire
- depense administrative
- approvisionnement de caisse
- transfert inter-caisses

Types:

- `REFUND`
- `CASH_IN`
- `CASH_OUT`
- `SAFE_DEPOSIT`
- `TRANSFER_IN`
- `TRANSFER_OUT`
- `ADJUSTMENT`

Types systeme non saisissables manuellement:

- `PAYMENT`
- `OPENING_FLOAT`
- `CLOSING_COUNT`

Une raison est obligatoire pour:

- `REFUND`
- `CASH_OUT`
- `SAFE_DEPOSIT`
- `TRANSFER_OUT`
- `ADJUSTMENT`

Une piece est attendue pour les types documentaires:

- `ENTRY_VOUCHER`
- `EXIT_VOUCHER`
- `CASH_VOUCHER`
- `BANK_SLIP`
- `EXPENSE_NOTE`

### Bons d'entree et bons de sortie

Bon d'entree:

```http
POST /cash-registers/sessions/{sessionNumber}/entry-vouchers
```

```json
{
  "amount": 15000,
  "documentNumber": "BE-202604-000001",
  "flowCategory": "EXTERNAL_COLLECTION",
  "referenceType": "MANUAL_COLLECTION",
  "referenceCode": "COL-001",
  "counterpartyType": "PARTNER",
  "counterpartyCode": "PTN-001",
  "counterpartyName": "Partenaire externe",
  "reason": "Encaissement externe",
  "createdBy": "cashier-1",
  "metadataJson": "{\"origin\":\"event\"}"
}
```

Bon de sortie:

```http
POST /cash-registers/sessions/{sessionNumber}/exit-vouchers
```

```json
{
  "amount": 12000,
  "documentNumber": "BS-202604-000001",
  "flowCategory": "SUPPLIER_PAYMENT",
  "referenceType": "SUPPLIER_INVOICE",
  "referenceCode": "INV-SUP-001",
  "counterpartyType": "SUPPLIER",
  "counterpartyCode": "SUP-001",
  "counterpartyName": "Fournisseur ABC",
  "reason": "Paiement fournisseur",
  "createdBy": "cashier-1",
  "metadataJson": "{\"approvedBy\":\"manager-1\"}"
}
```

Comportement backend:

- `entry-vouchers` cree un mouvement `CASH_IN`
- `exit-vouchers` cree un mouvement `CASH_OUT`
- le backend renseigne la direction de flux dans la reponse
- le module conserve la piece de caisse, la categorie de flux et le tiers concerne
- ces flux sont integres automatiquement au resume de session et a la cloture

### Lister les bons d'une session

```http
GET /cash-registers/sessions/{sessionNumber}/entry-vouchers
GET /cash-registers/sessions/{sessionNumber}/exit-vouchers
```

Ces routes retournent une page de `CashMovementResponse` filtree automatiquement sur:

- `entry-vouchers`: `documentType = ENTRY_VOUCHER`
- `exit-vouchers`: `documentType = EXIT_VOUCHER`
- `sessionNumber`: pris dans le chemin

Filtres optionnels disponibles (identiques a `GET /cash-registers/movements`, sans `registerCode`/`movementType`/`documentType` qui sont deja fixes par la route):

- `documentNumber`
- `flowCategory`
- `referenceType`
- `referenceCode`
- `counterpartyCode`
- `counterpartyName`
- `createdBy`
- `fromDate`
- `toDate`
- `searchText`
- `page`
- `size`

Exemple:

```http
GET /cash-registers/sessions/CSS-202604-000001/exit-vouchers?flowCategory=SUPPLIER_PAYMENT&page=0&size=20
```

Reponse cible (extrait, structure `PaginatedResponse<CashMovementResponse>`):

```json
{
  "content": [
    {
      "movementNumber": "CMV-202604-000045",
      "sessionNumber": "CSS-202604-000001",
      "registerCode": "CR-HQ-01",
      "movementType": "CASH_OUT",
      "flowDirection": "OUT",
      "amount": 12000,
      "currency": "XAF",
      "documentType": "EXIT_VOUCHER",
      "documentNumber": "BS-202604-000001",
      "flowCategory": "SUPPLIER_PAYMENT",
      "referenceType": "SUPPLIER_INVOICE",
      "referenceCode": "INV-SUP-001",
      "counterpartyType": "SUPPLIER",
      "counterpartyCode": "SUP-001",
      "counterpartyName": "Fournisseur ABC",
      "reason": "Paiement fournisseur",
      "createdBy": "cashier-1",
      "status": "RECORDED",
      "createdAt": "2026-04-26T09:12:00Z"
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
}
```

Le tri est impose par le backend (du plus recent au plus ancien); un `sort` envoye par le frontend est ignore pour ces routes.

### Detail d'un mouvement / bon

```http
GET /cash-registers/movements/{movementNumber}
```

Retourne le `CashMovementResponse` complet (montant, document, tiers, justificatif, signature, impression, pieces jointes `attachments`, etc.) pour un mouvement donne, qu'il s'agisse d'un bon d'entree, d'un bon de sortie ou de tout autre type de mouvement de caisse. Renvoie `404 RESOURCE_NOT_FOUND` si `movementNumber` est inconnu.

Exemple:

```http
GET /cash-registers/movements/CMV-202604-000045
```

### Lister les mouvements

```http
GET /cash-registers/movements
```

Filtres:

- `registerCode`
- `sessionNumber`
- `movementType`
- `documentType`
- `documentNumber`
- `flowCategory`
- `referenceType`
- `referenceCode`
- `counterpartyCode`
- `counterpartyName`
- `createdBy`
- `fromDate`
- `toDate`
- `searchText`
- `page`
- `size`

### Demander fermeture

```http
POST /cash-registers/sessions/{sessionNumber}/closing-request
```

ou

```http
PATCH /cash-registers/sessions/{sessionNumber}/close
```

```json
{
  "closedBy": "admin-001",
  "countedClosingAmount": 178500,
  "varianceReason": "Manque constate lors du comptage"
}
```

Le backend calcule:

- `expectedClosingAmount`
- `varianceAmount = countedClosingAmount - expectedClosingAmount`

Si `varianceAmount = 0`, la session passe en `CLOSED`.

Si `varianceAmount != 0`, la session passe en `CLOSING_REVIEW` et `varianceReason` est obligatoire.

### Valider un ecart

```http
PATCH /cash-registers/sessions/{sessionNumber}/approve-variance
```

```json
{
  "reviewedBy": "manager-001",
  "note": "Ecart valide apres verification"
}
```

La session passe en `CLOSED`.

## Anomalies de caisse

Le backend analyse automatiquement les sessions de caisse et signale les mouvements suspects (`CashAnomalyFlag`).

### Lancer une analyse

```http
POST /cash-registers/anomalies/sessions/{sessionNumber}/analyze
```

Recalcule et retourne la liste des anomalies detectees pour la session (`List<CashAnomalyFlagResponse>`).

### Lister les anomalies

```http
GET /cash-registers/anomalies
```

Filtres:

- `registerCode`
- `sessionNumber`
- `severity` (`LOW`, `MEDIUM`, `HIGH`)
- `status` (`OPEN`, `ACKNOWLEDGED`, `DISMISSED`, `ESCALATED`, `RESOLVED`)
- `anomalyType` (`VARIANCE_OUTLIER`, `ROUND_NUMBER_PATTERN`, `EXCESSIVE_REFUNDS`, `EXCESSIVE_ADJUSTMENTS`, `OFF_HOURS_SESSION`, `THRESHOLD_STRUCTURING`, `NEAR_MAX_CASH_RECURRENCE`)
- `page`, `size`

Retourne une `PaginatedResponse<CashAnomalyFlagResponse>`, triee par `detectedAt` decroissant.

### Detail d'une anomalie

Le detail complet (incluant `description`, `score`, `reviewedBy`, `reviewedAt`, `reviewNote`) est porte par chaque element de `CashAnomalyFlagResponse` retourne par la liste — il n'y a pas d'endpoint `GET /{flagNumber}` dedie.

### Examiner / cloturer une anomalie

```http
PATCH /cash-registers/anomalies/{flagNumber}/review
```

```json
{
  "reviewedBy": "manager-001",
  "status": "RESOLVED",
  "note": "Ecart explique par un retrait non enregistre, corrige sur la session"
}
```

- `status` doit etre l'une des valeurs de `CashAnomalyStatus` ci-dessus (a l'exclusion de `OPEN`, qui est l'etat initial pose par l'analyse automatique).
- `DISMISSED`: l'anomalie est un faux positif, aucune action requise.
- `RESOLVED`: l'anomalie etait reelle et a ete corrigee/expliquee.
- `ESCALATED`: transmise a un niveau superieur pour traitement.
- `ACKNOWLEDGED`: prise en compte, en attente d'investigation.
- Le backend rejette (400) la revue d'un flag dont le statut n'est plus `OPEN` ni `ACKNOWLEDGED` (deja `DISMISSED`, `ESCALATED` ou `RESOLVED`) avec le message `Anomaly flag {flagNumber} has already been resolved`.

## Workflows frontend

Paiement facture en cash avec caisse:

1. Creer la facture billing.
2. Emettre la facture.
3. Creer l'intention depuis la facture.
4. Ouvrir ou recuperer la session caisse ouverte.
5. Appeler `PATCH /payments/intents/{intentNumber}/cash` avec `cashSessionNumber`.
6. Consulter le resume de session.
7. En fin de shift, fermer la session.

Paiement wallet:

1. Creer l'intention.
2. Verifier ou afficher le wallet client.
3. Appeler `PATCH /payments/intents/{intentNumber}/wallet`.
4. Afficher transaction et statut document.

Fermeture de caisse:

1. Ouvrir le resume session.
2. Saisir le montant compte physiquement.
3. Appeler closing request.
4. Si statut `CLOSING_REVIEW`, afficher une action manager.
5. Apres validation, la session est fermee.

## Mobile money

Le contrat technique provider existe mais l'integration fournisseur reelle n'est pas active.

Provider actuel:

- `NoopMobileMoneyPaymentProvider`

Ne pas exposer le paiement mobile money comme disponible en production tant qu'un provider reel n'est pas branche.

## Workers

- `PaymentIntentExpiryWorker`: expire les intentions non payees.
- Les workers cash register prevus peuvent etre ajoutes ensuite: sessions ouvertes trop longtemps, alertes ecart, reconciliation caisse.
