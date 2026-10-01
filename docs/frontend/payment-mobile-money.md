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
  "phase": "WAITING_FOR_PAYER",
  "providerMessage": "PROCESSING",
  "failureReason": null,
  "failureCode": null,
  "userMessage": null,
  "retryable": null,
  "nextStatusCheckAt": "2026-09-23T13:51:00Z",
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

### 6.1 `phase` · ce qu'il faut afficher

N'affichez jamais `status` ni `providerMessage` a un client : ce sont des termes techniques.
Utilisez `phase`.

| `phase` | Ce que vit le client | Ecran |
|---|---|---|
| `WAITING_FOR_PAYER` | La demande est sur son telephone, il doit saisir son code | « Validez la demande sur votre telephone » + compte a rebours |
| `CONFIRMING` | Il a valide (ou l'appel n'a pas abouti), on attend l'operateur | « Confirmation en cours… » |
| `COMPLETED` | L'operateur a confirme l'encaissement | Recu, solde de la facture rafraichi |
| `FAILED` | L'operateur a refuse, ou la demande n'est jamais partie | `userMessage` + bouton « Reessayer » si `retryable` |
| `UNRESOLVED` | Aucune reponse definitive apres 24 h | `userMessage` + « Notre equipe verifie aupres de l'operateur » |

Une initiation peut repondre directement `CONFIRMING` : cela veut dire que l'appel vers l'operateur
n'a pas abouti a une reponse, **pas** que le paiement a echoue. La demande est peut-etre partie
sur le telephone du client. Ne proposez pas de reessayer dans cet etat.

### 6.2 `failureCode`, `userMessage`, `retryable`

Quand `phase` vaut `FAILED`, ces trois champs sont renseignes :

- `userMessage` est une phrase francaise prete a afficher · affichez-la telle quelle ;
- `retryable` dit si reessayer **avec le meme numero** a un sens. Faux pour un numero inconnu de
  l'operateur, un plafond atteint, un compte non autorise : proposez alors de changer de numero ou
  de moyen de paiement, pas de recommencer a l'identique ;
- `failureCode` est le code brut de l'operateur, utile pour vos propres statistiques.

Codes les plus frequents : `PAYMENT_NOT_APPROVED` (le client n'a pas valide), `INSUFFICIENT_BALANCE`,
`PAYER_NOT_FOUND`, `PAYER_LIMIT_REACHED`, `TRANSACTION_ALREADY_IN_PROCESS`, `EXPIRED`.

### 6.3 Erreurs d'initiation · `400` avant tout appel

Le backend verifie ce qui peut l'etre avant d'appeler l'operateur, et repond `400` avec un message
francais deja affichable :

- opérateur absent ;
- montant nul, negatif, ou avec des centimes (les operateurs n'acceptent que des montants entiers) ;
- devise de l'operateur differente de celle de la facture (un operateur RDC ne regle pas une
  facture en XAF) ;
- numero de telephone illisible ou, si les plages sont configurees, incompatible avec l'operateur
  choisi.

### 6.4 Double initiation

Une seconde initiation pour la **meme intention et le meme numero** dans les 20 minutes ne cree pas
un second depot : le backend rend le depot en cours, avec le meme `depositId`. Le bouton de
paiement peut donc etre re-clique sans faire sonner deux fois le telephone du client · mais
desactivez-le quand meme pendant la requete.

## 7. Suivre un depot

### 7.1 Cote client

```http
GET /api/v1/client/billing/mobile-money/deposits/{depositId}
```

Reponse : le meme `MobileMoneyDepositResponse`. Un membre ne peut lire que **ses** depots · un
depot qui ne lui appartient pas repond `404`.

Rythme de rafraichissement conseille : toutes les 3 secondes pendant la premiere minute, puis
toutes les 10 secondes. Arretez des que `phase` vaut `COMPLETED`, `FAILED` ou `UNRESOLVED`.
Le champ `nextStatusCheckAt` indique quand le backend reparlera lui-meme a l'operateur · inutile
d'interroger plus vite que ca apres les premieres minutes.

Le backend continue a relire le statut de son cote pendant 24 h, meme si l'utilisateur ferme la
page. Un client qui valide sa demande vingt minutes plus tard verra sa facture reglee.

### 7.2 Cote personnel (staff)

```http
GET  /api/v1/payments/mobile-money/deposits/{depositId}
GET  /api/v1/payments/mobile-money/deposits?status=UNRESOLVED&page=0&size=20
POST /api/v1/payments/mobile-money/deposits/{depositId}/recheck
GET  /api/v1/payments/mobile-money/callbacks?outcome=FAILED&page=0&size=20
```

Ces routes demandent `ADMIN`, `SUPER_ADMIN` ou `MANAGER`. `GET /deposits/{id}` **n'est plus
publique** : elle exposait le numero de telephone et le montant de n'importe quel payeur a qui
connaissait un identifiant de depot.

Ecran de rapprochement suggere : la liste `status=UNRESOLVED` est la file des paiements dont
l'operateur n'a jamais tranche. Le bouton « Verifier maintenant » appelle `/recheck`, qui relit le
statut chez l'operateur et met a jour le depot si celui-ci a fini par repondre.

Le journal `/callbacks` montre chaque rappel recu et ce qui en a ete fait (`PROCESSED`, `DEFERRED`,
`IGNORED`, `FAILED`), avec le detail. C'est ce qui permet d'expliquer un paiement manquant.

### 7.3 Ce que le client recoit par courriel

Le backend envoie deux courriels que le frontend n'a pas a reproduire, mais dont il doit tenir
compte dans ses ecrans :

| Quand | Courriel | Consequence pour vos ecrans |
|---|---|---|
| `phase` passe a `FAILED` (echec asynchrone) | Raison en francais + conduite a tenir | Inutile d'afficher une banniere « nous vous avons envoye un mail » : dites simplement ce que `userMessage` contient |
| `phase` passe a `UNRESOLVED` | « Ne payez pas une seconde fois » | **Desactivez le bouton de paiement de cette facture** tant que `phase` vaut `UNRESOLVED` · c'est exactement ce que le courriel demande au client |

Un echec **synchrone** (l'operateur refuse dans la seconde, pendant l'appel d'initiation) ne
declenche aucun courriel : le client est devant son ecran, la reponse HTTP suffit.

## 8. Endpoint de test

```http
POST /api/v1/payments/mobile-money/pawaypay/test/{phoneNumber}
```

Cette route envoie une **vraie** demande de `10 XAF` sur un **vrai** telephone, avec le compte
marchand reel. Elle demande donc un compte `ADMIN` ou `SUPER_ADMIN`, et n'est disponible que si
`bokati.payment.pawaypay.test-endpoint-enabled` est vrai (vrai en developpement, faux en
production). Sinon elle repond `403`.

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
