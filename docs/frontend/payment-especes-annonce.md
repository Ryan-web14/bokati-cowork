# Paiement en espèces · annonce et confirmation

Le mobile money et le portefeuille aboutissent seuls. Les espèces arrivent avec la personne :
entre l'annonce et les billets comptés à la caisse, **rien n'est encaissé**.

C'est toute la différence à faire passer dans l'interface.

## 1. Côté client

### 1.1 Annoncer

```http
POST /api/v1/client/billing/invoices/{documentNumber}/pay/cash
POST /api/v1/client/bookings/{bookingNumber}/pay/cash
```

Corps facultatif :

```json
{ "amount": 10000, "note": "Je passe vers 14h" }
```

Sans `amount`, c'est le solde de la facture. `note` est un mot libre que la caisse verra.

Réponse :

```json
{
  "declarationNumber": "ESP-2026-000001",
  "documentNumber": "INV-2026-000042",
  "bookingNumber": "BKG-2026-000017",
  "amount": 25000,
  "currency": "XAF",
  "status": "AWAITING_CONFIRMATION",
  "note": "Je passe vers 14h",
  "declaredAt": "2026-09-23T13:02:11Z",
  "expiresAt": "2026-09-24T13:02:11Z",
  "confirmedAt": null,
  "confirmedAmount": null,
  "transactionNumber": null
}
```

### 1.2 Ce qu'il faut afficher · et ne pas afficher

**N'affichez jamais « payé ».** La facture reste due, la réservation reste en attente de paiement.
Le bon message est du genre :

> Passez régler **25 000 XAF** en espèces à l'accueil avant le **24/09 à 13h02**.
> Votre créneau est tenu jusque-là.

Sur la facture concernée, remplacez le bouton de paiement en espèces par l'état de l'annonce et un
bouton « Annuler ». Les autres moyens de paiement (mobile money, portefeuille) **restent
proposés** : un client qui change d'avis doit pouvoir payer tout de suite, et la confirmation de
ce paiement ferme l'annonce d'elle-même.

Affichez le compte à rebours jusqu'à `expiresAt`. Pour une réservation, dites explicitement que le
créneau sera rendu passé ce délai.

### 1.3 Ses annonces, et se raviser

```http
GET  /api/v1/client/billing/cash-declarations?page=0&size=20
POST /api/v1/client/billing/cash-declarations/{declarationNumber}/cancel
```

Annuler ne change rien à la facture · elle reste réglable, elle n'est simplement plus annoncée.

### 1.4 Erreurs à l'annonce · `400`

Messages français déjà affichables :

- facture en brouillon, annulée ou déjà soldée ;
- montant nul, négatif, ou supérieur au solde ;
- devise autre que XAF (la caisse ne tient que du XAF) ;
- réservation qui n'attend pas de paiement.

Une seconde annonce sur la même facture ne crée rien : elle rend l'annonce en cours, avec le même
`declarationNumber`. Le bouton peut être re-cliqué sans dommage.

## 2. Côté caisse

### 2.1 La file

```http
GET /api/v1/payments/cash-declarations?status=AWAITING_CONFIRMATION&page=0&size=20
```

Les plus anciennes d'abord. C'est l'écran de travail : qui doit passer, combien, pour quelle
facture, et jusqu'à quand.

Accès : `ADMIN`, `SUPER_ADMIN`, `MANAGER` ou `CASHIER`.

### 2.2 Confirmer

```http
POST /api/v1/payments/cash-declarations/{declarationNumber}/confirm
```

```json
{ "cashSessionNumber": "CSH-2026-000012", "amount": 10000, "receivedBy": "Awa", "note": "" }
```

- `cashSessionNumber` est **obligatoire** · un encaissement qui n'atterrit dans aucune caisse rend
  le comptage du soir faux. Proposez la session ouverte de l'agent, ne la faites pas saisir.
- `amount` est facultatif · sans lui, c'est le montant annoncé. **Laissez-le modifiable** : le cas
  fréquent est celui où la personne ne règle qu'une partie de ce qu'elle avait annoncé.

La réponse porte `transactionNumber` : le reçu est disponible immédiatement à
`/client/billing/payments/{transactionNumber}/receipt`.

### 2.3 Annuler

```http
POST /api/v1/payments/cash-declarations/{declarationNumber}/cancel
{ "reason": "Client injoignable" }
```

Retire la ligne de la file. La facture reste due.

### 2.4 L'alerte temps réel

Un client qui annonce diffuse sur `/topic/admin/alerts` :

```json
{
  "alertType": "CASH_PAYMENT_DECLARED",
  "module": "PAYMENT",
  "severity": "WARNING",
  "title": "Espèces annoncées · 25000 XAF par Joël Bikindou",
  "message": "Joël Bikindou passera régler 25000 XAF en espèces. À confirmer à l'encaissement."
}
```

`WARNING` et non `INFO` : ce message demande une action, il ne la constate pas. À distinguer
visuellement de `SELF_SERVICE_PAYMENT_RECEIVED`, qui lui est un simple constat
(voir `docs/frontend/payment-libre-service.md`).

La notification persistante correspondante est lisible par
`GET /api/v1/admin/notifications/unread`.

## 3. Expiration

Un worker horaire ferme les annonces échues :

| Ce qui était annoncé | Ce qui se passe |
|---|---|
| Une réservation encore `PENDING_PAYMENT` | Annulée, créneau rendu · le client est prévenu par courriel |
| Une réservation déjà `CONFIRMED` | **Rien** · le client a réglé autrement entre-temps |
| Une simple facture | Rien à annuler · elle reste due, simplement plus annoncée |

Côté client, une annonce `EXPIRED` doit redonner tous les moyens de paiement sur la facture.
