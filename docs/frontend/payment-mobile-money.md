# Integration frontend Paiements, Factures et Mobile Money

Ce document explique comment le frontend doit integrer les paiements, les factures et les depots Mobile Money via PawaPay.

## 1. Configuration backend PawaPay

Le frontend ne doit jamais envoyer la cle API PawaPay.

Le backend envoie lui-meme la requete vers PawaPay avec le header suivant :

```http
Authorization: Bearer <bokati.payment.pawaypay.api-key>
Content-Type: application/json
```

Configuration attendue cote backend :

```yaml
bokati:
  payment:
    pawaypay:
      enabled: true
      base-url: https://api.sandbox.pawapay.io
      api-key: ${PAWAPAY_API_KEY}
      callback-base-url: https://votre-domaine.com
```

Important :

- La cle API doit etre stockee dans une variable d'environnement ou un secret.
- Ne pas hardcoder la cle dans le frontend.
- Ne pas exposer la cle dans une reponse API.
- Le backend utilise l'endpoint PawaPay `POST /v2/deposits`.

## 2. Recuperer les providers Mobile Money

Endpoint pour alimenter le select cote frontend :

```http
GET /api/v1/payments/mobile-money/providers
```

Exemple de reponse :

```json
[
  {
    "code": "MTN_MOMO_COG",
    "displayName": "MTN Mobile Money Congo",
    "countryCode": "COG",
    "currency": "XAF"
  }
]
```

Providers supportes :

- `MTN_MOMO_CIV`
- `ORANGE_CIV`
- `WAVE_CIV`
- `AIRTEL_COG`
- `MTN_MOMO_COG`
- `FREE_SEN`
- `ORANGE_SEN`
- `WAVE_SEN`
- `ORANGE_COD`
- `AIRTEL_COD`
- `VODACOM_MPESA_COD`
- `AIRTEL_GAB`

## 3. Creer une intention de paiement depuis une facture

Pour payer une facture, le frontend cree d'abord une intention de paiement.

```http
POST /api/v1/payments/intents/from-billing-document
```

Payload :

```json
{
  "documentNumber": "INV-202604-000001",
  "amount": 5000,
  "idempotencyKey": "INV-202604-000001-MM-5000"
}
```

Regles :

- `documentNumber` est obligatoire.
- `amount` est optionnel.
- Si `amount` est absent, le backend utilise le solde restant de la facture.
- Si l'admin veut encaisser seulement une partie par Mobile Money, il peut envoyer un montant partiel ici ou au moment du depot Mobile Money.

## 4. Initier un depot Mobile Money

Endpoint recommande :

```http
POST /api/v1/payments/mobile-money/deposits
```

Payload minimal :

```json
{
  "intentNumber": "INT-CUS-202604-00000001",
  "phoneNumber": "242061135836",
  "correspondent": "MTN_MOMO_COG"
}
```

Payload avec montant partiel admin :

```json
{
  "intentNumber": "INT-CUS-202604-00000001",
  "phoneNumber": "242061135836",
  "correspondent": "MTN_MOMO_COG",
  "amount": 2500,
  "createdBy": "admin"
}
```

Champs envoyes par le frontend :

- `intentNumber` : obligatoire. Permet au backend de retrouver la facture, le client, le montant et la devise.
- `phoneNumber` : obligatoire. Numero Mobile Money du client.
- `correspondent` : obligatoire. Provider choisi depuis `/payments/mobile-money/providers`.
- `amount` : optionnel. Montant partiel saisi par l'admin.
- `createdBy` : optionnel. Identifiant de l'utilisateur qui initie le paiement.
- `metadataJson` : optionnel. Contexte additionnel du frontend sous forme de chaine JSON.

Champs generes par le backend :

- `depositId`
- `clientReferenceId`
- `customerMessage`
- `metadata` PawaPay
- `amount` final envoye a PawaPay
- `currency`
- `payer`
- `callbackUrl`
- `transactionNumber`

Note sur le montant envoye a PawaPay :

- Le backend garde la precision interne pour la base de donnees.
- Avant l'appel PawaPay, le montant est arrondi a l'entier le plus proche.
- Exemple : `10.0000` devient `"10"`.
- Exemple : `10.5` devient `"11"`.

Le frontend ne doit pas envoyer :

- la cle API PawaPay
- `depositId`
- `clientReferenceId`
- `customerMessage`
- la metadata PawaPay finale
- la devise si elle vient de l'intention de paiement

## 5. Payload envoye par le backend a PawaPay

Le backend construit automatiquement un payload de ce type :

```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "payer": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "242061135836",
      "provider": "MTN_MOMO_COG"
    }
  },
  "amount": "2500",
  "currency": "XAF",
  "preAuthorisationCode": null,
  "clientReferenceId": "TXN-MM-202604-00000001",
  "customerMessage": "Invoice payment",
  "metadata": [
    {
      "intentNumber": "INT-CUS-202604-00000001",
      "purpose": "INVOICE_PAYMENT"
    },
    {
      "transactionNumber": "TXN-MM-202604-00000001",
      "paymentMethod": "MOBILE_MONEY"
    }
  ]
}
```

La requete est envoyee par le backend vers :

```http
POST https://api.sandbox.pawapay.io/v2/deposits
```

Avec :

```http
Authorization: Bearer <PAWAPAY_API_KEY>
Content-Type: application/json
```

## 6. Reponse retournee au frontend

Exemple :

```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "clientReferenceId": "TXN-MM-202604-00000001",
  "customerMessage": "Invoice payment",
  "metadata": [
    {
      "intentNumber": "INT-CUS-202604-00000001",
      "purpose": "INVOICE_PAYMENT"
    }
  ],
  "payer": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "242061135836",
      "provider": "MTN_MOMO_COG"
    }
  },
  "provider": "MTN_MOMO_COG",
  "amount": 2500,
  "currency": "XAF",
  "status": "PROCESSING",
  "intentNumber": "INT-CUS-202604-00000001",
  "transactionNumber": "TXN-MM-202604-00000001",
  "transaction": {
    "paymentMethod": "MOBILE_MONEY",
    "provider": "PAWAYPAY",
    "providerReference": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
    "status": "PROCESSING"
  }
}
```

Comportement frontend :

- Afficher un etat `PROCESSING` apres l'initiation.
- Proposer un rafraichissement ou polling si necessaire.
- Recharger la facture apres confirmation pour afficher le solde restant.

## 7. Recuperer un depot

```http
GET /api/v1/payments/mobile-money/deposits/{depositId}
```

Utilisation :

- suivi de paiement
- ecran support/admin
- verification du statut apres callback ou polling

## 8. Endpoint de test

Pour tester rapidement avec MTN Congo et un montant fixe de `10 XAF` :

```http
POST /api/v1/payments/mobile-money/pawaypay/test/{phoneNumber}
```

Exemple :

```http
POST /api/v1/payments/mobile-money/pawaypay/test/242061135836
```

Le backend utilise automatiquement :

- provider : `MTN_MOMO_COG`
- montant : `10`
- devise : `XAF`
- `depositId` genere cote backend
- `clientReferenceId` genere cote backend
- `customerMessage` genere cote backend
- metadata generee cote backend

## 9. Regles de paiement partiel

- Si `amount` est absent lors de l'initiation Mobile Money, le backend utilise le solde restant de l'intention.
- Si `amount` est present, il doit etre positif.
- Si `amount` depasse le solde restant, le backend rejette la demande.
- Quand PawaPay confirme un paiement partiel, seule la somme payee est appliquee sur la facture.
- La facture garde un solde restant si elle n'est pas totalement payee.
- L'intention de paiement passe a `SUCCEEDED` uniquement lorsque les transactions reussies couvrent tout le montant de l'intention.
- Si le paiement est partiel, l'intention repasse a `PENDING` pour permettre un autre encaissement.
