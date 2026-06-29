# Mobile Money — PawaPay · Guide de test

## URLs de base

| Environnement | Base URL |
|---|---|
| Local (ngrok) | `https://7d66-102-129-81-212.ngrok-free.app/sni/api/v1` |
| Production | `https://<domaine-prod>/sni/api/v1` |

---

## Configuration PawaPay Dashboard

Dans le tableau de bord PawaPay ([dashboard.pawapay.io](https://dashboard.pawapay.io)), configurer :

| Champ | Valeur |
|---|---|
| **Deposit Callback URL** | `https://7d66-102-129-81-212.ngrok-free.app/sni/api/v1/payments/mobile-money/pawaypay/callback` |
| **Refund Callback URL** | `https://7d66-102-129-81-212.ngrok-free.app/sni/api/v1/payments/mobile-money/pawaypay/refund-callback` |
| **Callback Secret** (HMAC) | Copier dans `PAWAYPAY_CALLBACK_SECRET` |

> Mettre `PAWAYPAY_CALLBACK_SECRET=` vide en dev pour désactiver la vérification de signature.

---

## Opérateurs disponibles (Congo)

| Code | Opérateur | Pays |
|---|---|---|
| `MTN_MOMO_COG` | MTN Mobile Money | République du Congo (Brazzaville) |
| `AIRTEL_OAPI_COG` | Airtel Money | République du Congo |
| `ORANGE_COG` | Orange Money | République du Congo |
| `AIRTEL_OAPI_COD` | Airtel Money | RDC (Kinshasa) |
| `ORANGE_COD` | Orange Money | RDC |
| `MPESA_COD` | M-Pesa | RDC |

---

## Flux complet — Deposit

### Étape 1 — Créer un PaymentIntent

```http
POST /sni/api/v1/payments/intents
Authorization: Bearer <token>
Content-Type: application/json

{
  "customerType": "CUSTOMER",
  "customerCode": "CUST-001",
  "amount": 5000,
  "currency": "XAF",
  "purpose": "BILLING_DOCUMENT_PAYMENT",
  "sourceType": "BILLING_DOCUMENT",
  "sourceCode": "INV-20240101-001"
}
```

**Réponse** — noter `intentNumber` :
```json
{
  "intentNumber": "INT-CUST-20240101-00001",
  "status": "PENDING",
  "amount": 5000.0000,
  "currency": "XAF"
}
```

---

### Étape 2 — Initier le deposit mobile money

```http
PATCH /sni/api/v1/payments/intents/INT-CUST-20240101-00001/mobile-money
Authorization: Bearer <token>
Content-Type: application/json

{
  "phoneNumber": "242060000001",
  "correspondent": "MTN_MOMO_COG",
  "createdBy": "admin@bokati.com"
}
```

**Réponse** — la transaction est en attente de confirmation opérateur :
```json
{
  "transactionNumber": "TXN-MOB-20240101-00001",
  "status": "PROCESSING",
  "paymentMethod": "MOBILE_MONEY",
  "provider": "PAWAYPAY",
  "providerReference": "<depositId-uuid>",
  "amount": 5000.0000,
  "currency": "XAF"
}
```

Le client reçoit une demande USSD/push sur son téléphone.

---

### Étape 3 — Callback PawaPay (automatique)

PawaPay appelle `POST /sni/api/v1/payments/mobile-money/pawaypay/callback` :

```json
{
  "depositId": "<depositId-uuid>",
  "status": "COMPLETED",
  "amount": "5000.00",
  "currency": "XAF",
  "correspondent": "MTN_MOMO_COG",
  "payer": {
    "type": "MSISDN",
    "address": { "value": "242060000001" }
  },
  "respondedByPayer": "2024-01-01T12:00:00Z"
}
```

→ La transaction passe en `SUCCEEDED`, le reçu est émis, la facture est allouée.

---

### Simuler le callback manuellement (dev/test)

```bash
curl -X POST https://7d66-102-129-81-212.ngrok-free.app/sni/api/v1/payments/mobile-money/pawaypay/callback \
  -H "Content-Type: application/json" \
  -d '{
    "depositId": "<depositId-uuid>",
    "status": "COMPLETED",
    "amount": "5000.00",
    "currency": "XAF",
    "correspondent": "MTN_MOMO_COG"
  }'
```

Pour simuler un échec :
```bash
curl -X POST https://7d66-102-129-81-212.ngrok-free.app/sni/api/v1/payments/mobile-money/pawaypay/callback \
  -H "Content-Type: application/json" \
  -d '{
    "depositId": "<depositId-uuid>",
    "status": "FAILED",
    "failureReason": { "failureCode": "PAYER_LIMIT_REACHED", "failureMessage": "Limite dépassée" }
  }'
```

---

## Remboursements

### Initier un remboursement

```http
POST /sni/api/v1/payments/transactions/TXN-MOB-20240101-00001/refund
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 5000,
  "reason": "Annulation client",
  "processedBy": "admin@bokati.com"
}
```

**Réponse** — la transaction de remboursement est en `PROCESSING` :
```json
{
  "transactionNumber": "TXN-MOB-20240101-00002",
  "status": "PROCESSING",
  "paymentMethod": "MOBILE_MONEY",
  "provider": "PAWAYPAY",
  "providerReference": "<refundId-uuid>"
}
```

PawaPay envoie l'argent sur le téléphone du client, puis appelle le refund-callback.

---

### Callback de remboursement (automatique)

PawaPay appelle `POST /sni/api/v1/payments/mobile-money/pawaypay/refund-callback` :

```json
{
  "refundId": "<refundId-uuid>",
  "depositId": "<depositId-uuid-original>",
  "status": "COMPLETED",
  "amount": "5000.00",
  "currency": "XAF"
}
```

→ La transaction de remboursement passe en `REFUNDED`.

### Simuler le callback remboursement manuellement

```bash
curl -X POST https://7d66-102-129-81-212.ngrok-free.app/sni/api/v1/payments/mobile-money/pawaypay/refund-callback \
  -H "Content-Type: application/json" \
  -d '{
    "refundId": "<refundId-uuid>",
    "depositId": "<depositId-uuid>",
    "status": "COMPLETED",
    "amount": "5000.00",
    "currency": "XAF"
  }'
```

---

## Consulter le statut d'un intent / ses transactions

```http
GET /sni/api/v1/payments/intents/INT-CUST-20240101-00001
GET /sni/api/v1/payments/intents/INT-CUST-20240101-00001/transactions
```

---

## Numéros de test PawaPay Sandbox

PawaPay sandbox accepte n'importe quel numéro au format E.164 (sans `+`).  
Pour déclencher des scénarios spécifiques, utiliser les numéros de test documentés dans le portail sandbox PawaPay.

| Numéro | Format | Résultat attendu |
|---|---|---|
| `242060000001` | MTN Congo | Succès (simulé par le sandbox) |
| `242040000001` | Airtel Congo | Succès |
| `242050000001` | Orange Congo | Succès |
| `243810000001` | Airtel RDC | Succès |

> En sandbox PawaPay, le résultat réel dépend de la configuration du compte sandbox — vérifier les numéros de test dans le dashboard.

---

## Worker de polling

Si un callback n'arrive pas (timeout réseau, ngrok coupé, etc.), le worker `MobileMoneyStatusPollingWorker` interroge PawaPay automatiquement toutes les **5 minutes** pour les transactions en `PROCESSING` depuis plus de **10 minutes**.

Configurable dans `application.yml` :
```yaml
bokati:
  payment:
    pawaypay:
      polling-delay-ms: 300000        # intervalle entre chaque run
      polling-max-age-minutes: 10     # âge minimum d'une transaction avant polling
```

---

## Variables d'environnement requises

| Variable | Description |
|---|---|
| `PAWAYPAY_API_KEY` | Clé API PawaPay (Bearer token) |
| `PAWAYPAY_CALLBACK_SECRET` | Secret HMAC pour vérifier les signatures des callbacks (optionnel en dev) |
| `APP_BASE_URL` | URL publique du backend (ex: URL ngrok en dev) |
