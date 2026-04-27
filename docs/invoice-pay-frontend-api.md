# Guide Frontend/API - Paiement direct depuis une facture

Base API: `/sni/api/v1`

Ce document decrit l'endpoint de paiement direct depuis une facture. Il permet de regler une facture en un seul appel, sans avoir a creer d'abord une intention de paiement separement. Le backend cree l'intention en interne, traite le paiement et retourne la facture mise a jour ainsi que la transaction generee.

## Principes

- L'appel est atomique: intention, transaction et allocation sont crees en une seule requete.
- Si `amount` est omis, le backend regle automatiquement le solde restant du.
- Un trop-percu est automatiquement credite sur le wallet du client.
- L'idempotency est supportee: si `idempotencyKey` est fourni et qu'une intention existe deja avec cette cle, le backend la reutilise.
- Les paiements cash sont autorises uniquement en `XAF`.
- Pour un paiement `WALLET`, le portefeuille doit appartenir au client de la facture et utiliser la meme devise.

## Champs generes par le backend

Ne pas envoyer ces champs dans la requete:

- `transactionNumber`
- `intentNumber`

## Methodes de paiement supportees

- `CASH`
- `WALLET`
- `MOBILE_MONEY`
- `BANK_TRANSFER`
- `CARD`
- `CHEQUE`

## Endpoint

```http
POST /billing/invoices/{documentNumber}/pay
```

### Parametre de chemin

| Champ          | Type   | Description                          |
|----------------|--------|--------------------------------------|
| `documentNumber` | String | Numero de la facture a regler |

### Corps de la requete

| Champ              | Type          | Requis                      | Description                                                      |
|--------------------|---------------|-----------------------------|------------------------------------------------------------------|
| `paymentMethod`    | Enum          | Oui                         | Methode de paiement (`CASH`, `WALLET`, `MOBILE_MONEY`, etc.)    |
| `amount`           | Decimal       | Non                         | Montant a payer. Si absent, regle la totalite du solde restant   |
| `walletNumber`     | String        | Oui si `WALLET`             | Numero du portefeuille interne du client                         |
| `cashSessionNumber`| String        | Non                         | Numero de session caisse active (uniquement pour `CASH`)         |
| `providerReference`| String        | Non                         | Reference externe du paiement (numero recu, reference virement, etc.) |
| `processedBy`      | String        | Non                         | Identifiant de l'agent ou du systeme qui enregistre le paiement  |
| `idempotencyKey`   | String        | Non                         | Cle unique pour eviter un double enregistrement                  |
| `metadataJson`     | String (JSON) | Non                         | Donnees complementaires libres au format JSON                    |

### Reponse

```json
{
  "invoice": {
    "documentNumber": "INV-MEM-20260426-00000001",
    "documentType": "INVOICE",
    "status": "PAID",
    "customerType": "MEMBER",
    "customerCode": "MBR-000001",
    "customerName": "Jean Dupont",
    "totalAmount": 15000,
    "paidAmount": 15000,
    "balanceDue": 0,
    "currency": "XAF",
    "issueDate": "2026-04-27",
    "paidAt": "2026-04-27T10:30:00Z"
  },
  "transaction": {
    "transactionNumber": "TXN-CSH-20260427-00000001",
    "intentNumber": "INT-MEM-20260427-00000001",
    "paymentMethod": "CASH",
    "provider": "CASH",
    "providerReference": "RECU-001",
    "amount": 15000,
    "currency": "XAF",
    "status": "SUCCEEDED",
    "paidAt": "2026-04-27T10:30:00Z",
    "receivedBy": "admin-001"
  }
}
```

## Exemples par methode de paiement

### Paiement cash

Le solde entier est regle. La session caisse est liee pour alimenter le journal de caisse.

```http
POST /billing/invoices/INV-MEM-20260426-00000001/pay
```

```json
{
  "paymentMethod": "CASH",
  "cashSessionNumber": "CSS-202604-000001",
  "providerReference": "RECU-002",
  "processedBy": "admin-001",
  "idempotencyKey": "pay-cash-INV-MEM-20260426-00000001"
}
```

Comportement backend:

- verifie que la devise de la facture est `XAF`
- cree une `PaymentTransaction` en `SUCCEEDED`
- si `cashSessionNumber` est fourni, cree un mouvement de caisse `PAYMENT`
- alloue le paiement a la facture
- met a jour la facture en `PARTIALLY_PAID` ou `PAID`
- credite le trop-percu sur le wallet client si necessaire

### Paiement wallet

```http
POST /billing/invoices/INV-MEM-20260426-00000001/pay
```

```json
{
  "paymentMethod": "WALLET",
  "walletNumber": "WAL-000001",
  "processedBy": "admin-001",
  "idempotencyKey": "pay-wallet-INV-MEM-20260426-00000001"
}
```

Comportement backend:

- verifie que le wallet est actif
- verifie que le proprietaire du wallet correspond au client de la facture
- verifie que la devise du wallet correspond a la devise de la facture
- debite le wallet
- cree une `PaymentTransaction` en `SUCCEEDED`
- alloue le paiement a la facture

### Paiement virement bancaire

```http
POST /billing/invoices/INV-MEM-20260426-00000001/pay
```

```json
{
  "paymentMethod": "BANK_TRANSFER",
  "amount": 10000,
  "providerReference": "VIR-20260427-001",
  "processedBy": "admin-001"
}
```

Comportement backend:

- enregistre la transaction avec `BANK_TRANSFER` et la reference externe
- alloue le montant partiellement a la facture (solde restant = 5000)
- la facture passe en `PARTIALLY_PAID`

### Paiement mobile money

```http
POST /billing/invoices/INV-MEM-20260426-00000001/pay
```

```json
{
  "paymentMethod": "MOBILE_MONEY",
  "providerReference": "MM-TXN-987654",
  "processedBy": "admin-001",
  "metadataJson": "{\"operator\":\"AIRTEL\"}"
}
```

### Paiement par cheque

```http
POST /billing/invoices/INV-MEM-20260426-00000001/pay
```

```json
{
  "paymentMethod": "CHEQUE",
  "providerReference": "CHQ-00042",
  "processedBy": "admin-001",
  "metadataJson": "{\"bankName\":\"BGFI\",\"checkDate\":\"2026-04-27\"}"
}
```

### Paiement par carte

```http
POST /billing/invoices/INV-MEM-20260426-00000001/pay
```

```json
{
  "paymentMethod": "CARD",
  "providerReference": "CARD-AUTH-112233",
  "processedBy": "admin-001"
}
```

## Paiement partiel

Pour regler partiellement une facture, envoyer `amount` avec un montant inferieur au solde restant.

```json
{
  "paymentMethod": "CASH",
  "amount": 5000,
  "processedBy": "admin-001"
}
```

La facture passe en `PARTIALLY_PAID`. Un appel ulterieur permettra de regler le reliquat.

## Idempotency

Si un meme paiement doit etre rejoue sans risque de doublon, envoyer `idempotencyKey`.

```json
{
  "paymentMethod": "CASH",
  "idempotencyKey": "pay-cash-INV-MEM-20260426-00000001-v1",
  "processedBy": "admin-001"
}
```

Si une intention avec cette cle existe deja, elle est reutilisee et le paiement continue sur cette intention.

## Statuts de la facture apres paiement

| Situation                            | Statut facture   |
|--------------------------------------|------------------|
| Facture reglee en totalite           | `PAID`           |
| Facture reglee partiellement         | `PARTIALLY_PAID` |
| Facture non reglee (avant paiement)  | `ISSUED`         |

## Erreurs courantes

| Code HTTP | Cause                                                        |
|-----------|--------------------------------------------------------------|
| 400       | Facture sans solde restant (`balanceDue = 0`)               |
| 400       | Methode `CASH` utilisee avec une devise autre que `XAF`     |
| 400       | `walletNumber` absent pour un paiement `WALLET`             |
| 400       | Wallet n'appartenant pas au client de la facture            |
| 400       | Devise du wallet differente de celle de la facture          |
| 404       | Facture introuvable avec ce `documentNumber`                |
| 404       | Wallet introuvable avec ce `walletNumber`                   |

## Workflows recommandes

### Paiement direct depuis le detail d'une facture

1. Afficher le detail de la facture avec `GET /billing/documents/{documentNumber}`.
2. Si `balanceDue > 0`, proposer le bouton "Payer".
3. Selectionner la methode de paiement et saisir les informations optionnelles.
4. Appeler `POST /billing/invoices/{documentNumber}/pay`.
5. Afficher la facture mise a jour et le recapitulatif de la transaction.

### Paiement cash avec caisse ouverte

1. Recuperer ou ouvrir la session caisse avec `POST /cash-registers/sessions`.
2. Appeler `POST /billing/invoices/{documentNumber}/pay` avec `cashSessionNumber`.
3. Consulter le resume de session avec `GET /cash-registers/sessions/{sessionNumber}/summary`.

### Paiement wallet depuis le compte client

1. Verifier le solde du wallet avec `GET /wallets/{walletNumber}`.
2. Si solde suffisant, appeler `POST /billing/invoices/{documentNumber}/pay` avec `paymentMethod: WALLET`.
3. Verifier que le nouveau solde wallet a bien ete debite dans le ledger.

## Difference avec le flux en deux etapes

Le flux classique (toujours disponible) necessite deux appels:

1. `POST /payments/intents/from-billing-document` pour creer l'intention.
2. `PATCH /payments/intents/{intentNumber}/cash` ou `/wallet` pour executer le paiement.

L'endpoint `POST /billing/invoices/{documentNumber}/pay` combine les deux en un seul appel. Il est recommande pour les interfaces frontdesk ou backoffice qui veulent simplifier le workflow de caisse.