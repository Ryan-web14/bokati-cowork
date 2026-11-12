# Encaissement au guichet : monnaie rendue, chèque, virement, mobile money hors plateforme, annulation et relance

Back-office (caisse). Tous les montants sont en `BigDecimal` à 4 décimales ; espèces en `XAF`.

Ce fichier couvre cinq besoins :

1. [Rendre la monnaie](#1-rendre-la-monnaie-espèces) : le client tend plus que le dû, l'écran affiche en direct ce qu'il faut lui rendre.
2. [Chèque, virement, mobile money hors plateforme](#2-chèque-virement-mobile-money-hors-plateforme) (MTN MoMo Pay, Airtel Money sur le numéro marchand), imprimés sur le reçu et la facture.
3. [Mobile money de la plateforme](#3-mobile-money-de-la-plateforme-et-caisse-automatique) : rangé seul dans la caisse automatique, ouverte au besoin.
4. [Annuler une intention en cours](#4-annuler-une-intention-en-cours).
5. [Relancer une intention expirée, annulée ou en échec](#5-relancer-une-intention).

## 1. Rendre la monnaie (espèces)

### Règle

| Saisie | Montant encaissé (imputé, entre en caisse) | Monnaie rendue |
| --- | --- | --- |
| `tenderedAmount` seul, supérieur ou égal au dû | le dû | `tenderedAmount - dû` |
| `tenderedAmount` seul, inférieur au dû | `tenderedAmount` (paiement partiel) | 0 |
| `amount` et `tenderedAmount` | `amount` | `tenderedAmount - amount` ; refus `400` si `tenderedAmount < amount` |
| ni l'un ni l'autre | le dû | aucune (`null`) |

Le « dû » est le reste à payer : `payableAmount` de l'intention, ou `balanceDue` de la facture.

La caisse n'enregistre que le montant encaissé : la monnaie sort du tiroir au même moment, le solde attendu de la session reste juste.

### Calcul en direct pendant la saisie

Calcul local, à chaque frappe, avec le dû déjà affiché (`payableAmount` ou `balanceDue`) :

```js
const applied = amount ?? Math.min(tendered, due);
const change  = Math.max(tendered - applied, 0);
const remainingAfter = Math.max(due - applied, 0);
```

Pour une confirmation serveur (règle identique, arrondis compris), appeler l'aperçu (sans effet, à espacer de 300 ms environ) :

| Méthode | Route |
| --- | --- |
| `GET` | `/sni/api/v1/payments/intents/{intentNumber}/cash-change?tenderedAmount=20000[&amount=]` |
| `GET` | `/sni/api/v1/payments/billing-documents/{documentNumber}/cash-change?tenderedAmount=20000[&amount=]` |

Réponse :

```json
{
  "currency": "XAF",
  "amountDue": 17500.0000,
  "tenderedAmount": 20000.0000,
  "appliedAmount": 17500.0000,
  "changeAmount": 2500.0000,
  "remainingAfter": 0.0000
}
```

Affichage conseillé : un grand chiffre « À rendre : 2 500 XAF » ; si `remainingAfter > 0`, un bandeau « Paiement partiel, reste dû : … ».

### Encaisser

Sur une intention, `PATCH /sni/api/v1/payments/intents/{intentNumber}/cash` :

```json
{ "receivedBy": "caisse@sni-cg.com", "cashSessionNumber": "CSS-...", "tenderedAmount": 20000 }
```

Sur une facture, `POST /sni/api/v1/billing/invoices/{documentNumber}/pay` (le règlement direct existant), avec `"paymentMethod": "CASH"`, `cashSessionNumber` (obligatoire en espèces) et `tenderedAmount`.

La transaction renvoyée porte `tenderedAmount` et `changeAmount`. Le reçu imprime « Montant remis » et « Monnaie rendue ».

`tenderedAmount` sur un autre moyen que `CASH` : `400`.

## 2. Chèque, virement, mobile money hors plateforme

Nouveau moyen `MOBILE_MONEY_OFFLINE` : le client a payé sur le numéro marchand (MTN MoMo Pay, Airtel Money), hors de la plateforme, et la caisse le saisit. Il ne passe pas par PawaPay et n'entre pas dans la caisse automatique ; un remboursement se fait à la main.

Le mobile money **de la plateforme** (`MOBILE_MONEY`) reste réservé à la route de dépôt PawaPay. Un `POST .../pay` avec `"paymentMethod": "MOBILE_MONEY"` est désormais refusé (`400`) et renvoie vers `MOBILE_MONEY_OFFLINE`.

### Champs par moyen

| Moyen | `reference` (sur l'intention) / `providerReference` (sur la facture) | Autres champs |
| --- | --- | --- |
| `CHEQUE` | numéro du chèque, **obligatoire** | `bankName` **obligatoire**, `payerName` (tireur) |
| `BANK_TRANSFER` | référence du virement, **obligatoire** | `bankName` (banque d'origine), `payerName` (donneur d'ordre) |
| `MOBILE_MONEY_OFFLINE` | identifiant de la transaction chez l'opérateur, **obligatoire** | `operator` / `mobileMoneyOperator` **obligatoire** : `MTN_MOMO` ou `AIRTEL_MONEY` ; `payerPhone`, `payerName` |

Une même référence déjà encaissée pour le même moyen (et la même banque pour un chèque, le même opérateur pour le mobile money) est refusée : `409 CONFLICT`, message « Le chèque 0012345 est déjà enregistré ».

`cashSessionNumber` est facultatif : s'il est donné, la session doit être ouverte et ne pas être une caisse automatique. Le paiement est alors rattaché au guichet, **sans entrer dans le compte des espèces** (voir le résumé de session plus bas).

### Sur une intention

`PATCH /sni/api/v1/payments/intents/{intentNumber}/offline`

```json
{
  "paymentMethod": "MOBILE_MONEY_OFFLINE",
  "amount": 12500,
  "reference": "MP261111.1530.A12345",
  "operator": "MTN_MOMO",
  "payerPhone": "+242061234567",
  "payerName": "Grace Mavoungou",
  "cashSessionNumber": "CSS-...",
  "receivedBy": "caisse@sni-cg.com"
}
```

`amount` absent : le reste à payer. Réponse : la transaction (`PaymentTransactionResponse`).

### Sur une facture

`POST /sni/api/v1/billing/invoices/{documentNumber}/pay` (règlement direct existant) :

```json
{
  "paymentMethod": "CHEQUE",
  "amount": 30000,
  "providerReference": "0012345",
  "bankName": "BGFI Bank",
  "payerName": "SARL Alpha",
  "cashSessionNumber": "CSS-...",
  "processedBy": "caisse@sni-cg.com"
}
```

Pour le mobile money hors plateforme : `"paymentMethod": "MOBILE_MONEY_OFFLINE"`, `"mobileMoneyOperator": "AIRTEL_MONEY"`, `payerPhone`.

### Ce qui s'imprime

| Où | Ce qui apparaît |
| --- | --- |
| Transaction (`PaymentTransactionResponse`) | `paymentMethodLabel` (« Chèque », « Virement bancaire », « Mobile money hors plateforme (MTN MoMo Pay) »), `providerReference`, `bankName`, `payerName`, `payerPhone`, `cashSessionNumber` |
| Reçu (JSON et PDF) | le libellé du moyen ; « Chèque n° … », « Réf. virement … » ou « Réf. opérateur … » ; banque ; tireur, donneur d'ordre ou titulaire ; numéro du payeur masqué |
| Facture PDF, tableau « Paiements reçus » | le libellé du moyen (opérateur compris), la référence, le numéro du payeur masqué |

Afficher `paymentMethodLabel` plutôt que traduire `paymentMethod` côté écran.

### Résumé de session

`GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/summary` porte maintenant `nonCashPayments` :

```json
"nonCashPayments": [
  { "paymentMethod": "CHEQUE", "provider": "CHEQUE", "label": "Chèque", "count": 2, "amount": 55000.0000 },
  { "paymentMethod": "MOBILE_MONEY_OFFLINE", "provider": "MTN_MOMO", "label": "Mobile money hors plateforme (MTN MoMo Pay)", "count": 1, "amount": 12500.0000 }
]
```

Ces montants ne sont **pas** dans `totalPayments` ni dans `expectedClosingAmount` (qui restent des espèces). À la clôture, les afficher à part : « Chèques à remettre en banque : 2, 55 000 XAF ».

## 3. Mobile money de la plateforme et caisse automatique

Rien à faire côté écran. Quand un dépôt PawaPay est confirmé, la transaction est rangée dans la caisse `CSR-AUTO-MOBILE-MONEY` :

- si aucune session n'y est ouverte, le système en ouvre une (`openedBy: SYSTEM_MOBILE_MONEY`) ;
- sinon le paiement entre dans la session ouverte ;
- au seuil de session (10 000 000 XAF par défaut), la session est close et une autre s'ouvre.

Deux confirmations simultanées n'ouvrent pas deux sessions. Une erreur à cette étape ne bloque plus la confirmation du paiement (facture soldée, réservation confirmée) ; elle est seulement journalisée.

Le mobile money hors plateforme (`MOBILE_MONEY_OFFLINE`) n'y entre jamais.

## 4. Annuler une intention en cours

`POST /sni/api/v1/payments/intents/{intentNumber}/cancel`, corps facultatif :

```json
{ "reason": "Le client paiera par chèque" }
```

| Cas | Résultat |
| --- | --- |
| `PENDING`, `PROCESSING` ou `AUTHORIZED`, échéance non passée, aucun dépôt mobile money en cours | `CANCELLED` ; `cancelledAt`, `cancelledBy`, `cancellationReason` remplis ; le lien de paiement envoyé au client est fermé |
| un dépôt mobile money attend encore la réponse de l'opérateur | `409` : attendre sa confirmation ou son échec |
| déjà `CANCELLED` | rendue telle quelle (`200`) |
| `SUCCEEDED`, `EXPIRED`, `REFUNDED`... | `400` |

Les paiements déjà reçus sur l'intention restent imputés sur leurs factures. L'annulation ne touche ni la facture ni ce qu'elle règle (réservation, abonnement) : elle ferme seulement cette demande de paiement.

## 5. Relancer une intention

`POST /sni/api/v1/payments/intents/{intentNumber}/relaunch`, corps facultatif :

```json
{ "expiresAt": "2026-11-18T18:00:00Z" }
```

L'intention est **rouverte** sous le même numéro (même lien de paiement) et repasse `PENDING`.

| Règle | Détail |
| --- | --- |
| États acceptés | `EXPIRED`, `CANCELLED`, `FAILED`, ou `PENDING`/`PROCESSING` dont l'échéance est passée (avant le passage du worker) |
| Échéance | `expiresAt` s'il est donné (doit être dans le futur, sinon `400`) ; sinon la durée de validité d'origine repart de maintenant (entre 15 minutes et 30 jours) |
| Montant | si l'intention règle des factures payées en partie entre-temps, son montant est ramené à « déjà payé sur l'intention + reste dû des factures » |
| Refus `400` | intention déjà entièrement payée ; factures déjà soldées ; facture annulée, remboursée ou passée en perte |
| Refus `409` | une autre intention est déjà en cours pour la même facture (le message la nomme : l'utiliser ou l'annuler) ; dépôt mobile money encore en cours |

La réponse porte `relaunchCount`, `lastRelaunchedAt`, `lastRelaunchedBy`. L'annulation précédente est effacée.

### Boutons sur la fiche d'une intention

`PaymentIntentResponse` porte deux booléens calculés par le serveur avec la même règle :

- `cancellable` : afficher « Annuler l'intention » ;
- `relaunchable` : afficher « Relancer ».

Après l'action, remplacer l'intention affichée par la réponse.

## Erreurs

| Code | Cas |
| --- | --- |
| `400` | montant remis inférieur au montant à encaisser ; `tenderedAmount` hors espèces ; champ obligatoire manquant (numéro de chèque, banque, opérateur, référence) ; session fermée ou caisse automatique ; `MOBILE_MONEY` sur le règlement direct ; relance ou annulation dans un état qui ne le permet pas |
| `409` | référence de chèque, virement ou mobile money déjà encaissée ; dépôt mobile money en cours ; autre intention en cours pour la même facture |
