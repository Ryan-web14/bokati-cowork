# Guide Frontend/API - Wallet

Base API: `/sni/api/v1`

Ce document couvre le wallet interne. Le wallet est un portefeuille electronique interne base sur un ledger append-only. Le solde courant est conserve sur `wallet_account` pour lecture rapide, mais chaque mouvement est trace dans `wallet_ledger_entry`.

## Principes

Champs generes par le backend:

- `walletNumber`
- `entryNumber`
- `holdNumber`
- `balanceAfter`
- `openedAt`
- `closedAt`

Le frontend doit utiliser:

- `ownerType`
- `ownerCode`
- `currency`

pour retrouver ou creer un wallet. Il ne doit pas saisir `walletNumber` lors de la creation.

Le wallet peut etre utilise pour:

- top-up admin
- paiement interne
- remboursement
- credit de trop-percu
- reservation de fonds via hold
- liberation ou capture d'un hold

## Statuts wallet

- `ACTIVE`
- `SUSPENDED`
- `LOCKED`
- `CLOSED`
- `UNDER_REVIEW`

## Types ledger

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

Directions ledger:

- `CREDIT`
- `DEBIT`

## Creer ou recuperer un wallet

```http
POST /wallets/admin/get-or-create
```

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-000001",
  "currency": "XAF"
}
```

Si le wallet existe, il est retourne. Sinon le backend le cree automatiquement.

Reponse:

```json
{
  "walletNumber": "WAL-000001",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-000001",
  "currency": "XAF",
  "status": "ACTIVE",
  "availableBalance": 0,
  "ledgerBalance": 0,
  "heldBalance": 0,
  "openedAt": "2026-04-17T10:00:00Z",
  "closedAt": null
}
```

## Top-up admin

```http
POST /wallets/admin/top-up
```

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-000001",
  "currency": "XAF",
  "amount": 100000,
  "createdBy": "admin-001",
  "reference": "CASH-TOPUP-001",
  "metadataJson": "{\"cashSessionNumber\":\"CSS-202604-000001\"}"
}
```

Traitement backend:

- cree le wallet si necessaire
- credite le solde disponible
- ecrit un ledger `ADMIN_TOPUP`
- retourne le wallet actualise

`metadataJson` doit etre une chaine JSON valide.

## Detail wallet

```http
GET /wallets/{walletNumber}
```

Utiliser cet endpoint pour afficher:

- solde disponible
- solde ledger
- solde bloque
- statut
- proprietaire
- devise

## Ledger wallet

```http
GET /wallets/{walletNumber}/ledger
```

Parametres:

- `page`
- `size`
- `sort`

Exemple:

```http
GET /wallets/WAL-000001/ledger?page=0&size=20&sort=createdAt,desc
```

Reponse ligne ledger:

```json
{
  "entryNumber": "WLE-202604-000001",
  "walletNumber": "WAL-000001",
  "direction": "CREDIT",
  "amount": 100000,
  "currency": "XAF",
  "balanceAfter": 100000,
  "entryType": "ADMIN_TOPUP",
  "sourceType": "ADMIN_TOPUP",
  "sourceCode": "CASH-TOPUP-001",
  "reference": "CASH-TOPUP-001",
  "createdBy": "admin-001",
  "createdAt": "2026-04-17T10:00:00Z"
}
```

## Paiement via wallet

Le paiement wallet est expose dans le module payment:

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
- `availableBalance >= intent.amount`

Effets:

- debit wallet
- ledger `PAYMENT`
- transaction payment `SUCCEEDED`
- allocation facture si source billing

## Holds wallet

Un hold reserve une partie du solde disponible sans le debiter definitivement. Il sert pour booking, caution, reservation ou flux qui attend une confirmation.

### Creer un hold

```http
POST /wallet-holds
```

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

Effets:

- diminue `availableBalance`
- augmente `heldBalance`
- cree `WalletHold`
- ecrit un ledger `HOLD`

### Capturer un hold

```http
PATCH /wallet-holds/{holdNumber}/capture?createdBy=admin-001
```

Effets:

- le montant reserve devient definitivement debite
- `heldBalance` diminue
- ledger de paiement/capture selon implementation service

### Liberer un hold

```http
PATCH /wallet-holds/{holdNumber}/release?createdBy=admin-001
```

Effets:

- `heldBalance` diminue
- `availableBalance` remonte
- ledger `HOLD_RELEASE`

## Expiration automatique des holds

Worker:

- `WalletHoldExpiryWorker`

Il libere automatiquement les holds expires.

## Conformite et bonnes pratiques frontend

- Afficher clairement la devise du wallet.
- Ne jamais permettre une saisie manuelle de `walletNumber` en creation.
- Pour un client enregistre, utiliser `ownerType/ownerCode`.
- Pour les operations admin, toujours envoyer `createdBy`.
- Afficher les mouvements ledger en lecture seule.
- Ne jamais modifier une entree ledger.
- Utiliser une reference lisible pour top-up admin: numero caisse, numero recu ou reference manager.
- Avant paiement wallet, afficher le solde disponible et le montant a payer.
- Pour un hold, afficher la date d'expiration.

## Workflows frontend

Top-up admin:

1. Selectionner le client.
2. Appeler `POST /wallets/admin/get-or-create`.
3. Saisir montant, reference, agent.
4. Appeler `POST /wallets/admin/top-up`.
5. Afficher le nouveau solde et la ligne ledger.

Paiement facture:

1. Creer l'intention de paiement depuis la facture.
2. Recuperer le wallet client.
3. Verifier le solde disponible.
4. Appeler `PATCH /payments/intents/{intentNumber}/wallet`.
5. Afficher la transaction et la facture mise a jour.

Reservation booking:

1. Recuperer ou creer le wallet.
2. Creer un hold avec `sourceType = BOOKING`.
3. Confirmer le booking puis capturer le hold, ou annuler puis liberer le hold.

