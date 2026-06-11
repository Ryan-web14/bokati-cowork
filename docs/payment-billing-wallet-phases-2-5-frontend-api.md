# Guide Frontend/API — Payment / Billing / Wallet, phases 2 à 5

Ce document décrit l'impact frontend des évolutions implémentées suite à
`docs/plan-payment-billing-wallet-improvements.md`, phases 2 à 5 :

1. Alerte de dépassement de plafond de caisse (manager dédié + anti-spam)
2. Avoir et remboursement automatiques sur annulation de réservation (politique d'annulation)
3. Acompte/caution d'abonnement bloqué via `WalletHold`
4. Historique des remboursements partiels et plafonnement cumulé

Toutes les routes mentionnées sont préfixées par `/api/v1` (voir `ApiPath.V1`).

---

## 1. Caisse — alerte de dépassement de plafond (`/payments/cash-registers`)

### 1.1 Nouveau champ `managerEmail`

`CreateCashRegisterRequest` et `CashRegisterResponse` exposent désormais un champ optionnel
`managerEmail` (string, max 180 caractères) :

```json
{
  "name": "Caisse reception",
  "locationCode": "LOC-001",
  "businessEntityCode": "BUS-0001",
  "deviceCode": "POS-001",
  "cashControlEnabled": true,
  "maxCashAmount": 500000,
  "managerEmail": "manager.reception@elleaose.com"
}
```

`CashRegisterResponse` renvoie ce champ tel quel (`managerEmail`). Si non renseigné, les
alertes de dépassement (voir ci-dessous) sont envoyées à l'adresse superviseur générique
(`bokati.payment.cash-session.supervisor-email`).

> Aucun endpoint de mise à jour de caisse n'existe actuellement (`managerEmail` se renseigne
> uniquement à la création). Si le besoin de modification apparaît côté UI, il faudra
> demander l'ajout d'un endpoint `PATCH`/`PUT` dédié.

### 1.2 Comportement : alerte de dépassement de plafond

Lorsqu'une session de caisse est analysée (`POST /payments/cash-registers/sessions/{sessionNumber}/analyze`,
ou tout déclenchement automatique de `analyzeSession`), un nouveau contrôle
`detectMaxCashOverflow` s'exécute en complément de la détection d'anomalies existante :

- Si un mouvement fait dépasser le solde théorique (`runningBalance`) au-delà de `maxCashAmount`,
  un email est envoyé **directement au manager de la caisse** (`managerEmail`, ou à
  l'adresse superviseur si absente), via le template générique de notification.
- Cette alerte est **indépendante** du flag d'anomalie `NEAR_MAX_CASH_RECURRENCE` — elle ne
  crée **aucun** `CashAnomalyFlag` (ce n'est pas un signal de fraude mais une notification
  opérationnelle directe). Elle n'apparaît donc pas dans
  `GET /payments/cash-registers/anomalies`.
- **Anti-spam** : une fois l'alerte envoyée, `lastAnomalyAlertSentAt` est horodaté sur la
  caisse ; aucune nouvelle alerte de dépassement n'est renvoyée avant l'écoulement du délai
  configuré par `bokati.payment.cash-register.alert-cooldown-hours` (par défaut **6 heures**).

Côté frontend : aucun nouvel endpoint à consommer pour ce point — le comportement est
purement notificatif (email). Si une vue "historique des alertes caisse" est envisagée plus
tard, il faudra prévoir un endpoint dédié (non implémenté ici).

---

## 2. Réservations — annulation : avoir et remboursement automatiques

### 2.1 Nouvel endpoint : politique d'annulation

```http
GET /bookings/cancellation-policy
```

Retourne la liste des paliers actifs de la politique d'annulation globale, triés par ordre
d'application (`ruleOrder` croissant) :

```json
[
  { "id": 1280001, "hoursBeforeStart": 24, "refundPercentage": 100.00, "ruleOrder": 1, "active": true },
  { "id": 1280002, "hoursBeforeStart": 12, "refundPercentage": 50.00,  "ruleOrder": 2, "active": true },
  { "id": 1280003, "hoursBeforeStart": 0,  "refundPercentage": 0.00,   "ruleOrder": 3, "active": true }
]
```

Le frontend peut utiliser ces données pour afficher, **avant confirmation d'annulation**, un
message du type *"Vous serez remboursé(e) à hauteur de X % du montant payé"*, en calculant
côté client le délai restant avant le début de la réservation et en sélectionnant le premier
palier dont `hoursBeforeStart` est ≤ à ce délai.

> Cette politique est globale (pas par ressource/établissement) et se configure actuellement
> en base ; aucun écran d'administration n'est prévu dans ce lot.

### 2.2 Comportement enrichi de l'annulation

```http
PATCH /bookings/{bookingNumber}/cancel
```

Quand une réservation **confirmée ou en cours** (`CONFIRMED`/`IN_PROGRESS`), déjà facturée et
**partiellement ou totalement payée**, est annulée :

1. Le système détermine automatiquement le palier de remboursement applicable selon le délai
   restant avant le début (`startedAt`).
2. Si le pourcentage de remboursement est > 0 :
   - Un **avoir (credit note)** est généré automatiquement sur la facture liée
     (`montant = montant payé × pourcentage / 100`), avec un motif indiquant la politique
     appliquée.
   - Les transactions de paiement réussies (`SUCCEEDED`) liées à cette facture sont
     remboursées automatiquement (réparties par ordre chronologique jusqu'à couvrir le
     montant de l'avoir).
   - Une entrée d'historique de modification est tracée sur le document de facturation
     (`editType = "CANCELLATION_AUTO_CREDIT_NOTE"`), visible via
     `GET /billing/documents/{documentNumber}/edit-history` (si exposé côté UI facturation).
3. Si aucun palier ne s'applique ou que le pourcentage est nul (ex. annulation tardive),
   **aucun avoir ni remboursement n'est généré** — uniquement l'annulation standard.

Ce traitement est **best-effort et non bloquant** : une erreur lors de la génération de
l'avoir/remboursement n'empêche jamais l'annulation de la réservation (elle est journalisée
côté serveur). Le frontend ne doit donc pas s'attendre à un échec de `cancel` lié à ce
nouveau comportement, mais peut afficher après coup, sur la fiche réservation/facture, le
nouvel avoir et le statut de remboursement (en rafraîchissant la facture et la liste des
transactions).

---

## 3. Abonnements — acompte/caution bloqué via Wallet (`WalletHold`)

### 3.1 Création d'abonnement

Lorsqu'un plan tarifaire (`PlanPrice.depositAmount`) prévoit un acompte/caution > 0, la
création de l'abonnement (`POST /subscriptions`) déclenche désormais automatiquement :

- La résolution (ou création) du wallet du souscripteur dans la devise du plan
  (`GET /wallets?ownerType=...&ownerCode=...` existant, créé si absent).
- La pose d'un **blocage de fonds** (`WalletHold`, `sourceType = "SUBSCRIPTION"`,
  `sourceCode = <subscriptionNumber>`, `status = ACTIVE`) du montant `depositAmount`.

Ce blocage est visible côté frontend via l'endpoint existant :

```http
GET /wallets/{walletNumber}/holds?sourceType=SUBSCRIPTION&sourceCode={subscriptionNumber}
```

> Comportement **non bloquant** : si le wallet ne peut pas être résolu ou si la pose du hold
> échoue (ex. solde insuffisant — selon l'implémentation de `WalletHoldService.create`),
> l'abonnement est tout de même créé ; l'erreur est journalisée côté serveur. Le frontend
> peut donc afficher un état "acompte non bloqué" si `GET .../holds` ne retourne aucun hold
> `ACTIVE` pour cet abonnement, et orienter vers une relance manuelle.

### 3.2 Annulation d'abonnement

À l'annulation d'un abonnement (`POST /subscriptions/{subscriptionNumber}/cancel` ou
équivalent), tout `WalletHold` actif lié (`sourceType = SUBSCRIPTION`,
`sourceCode = subscriptionNumber`) est traité automatiquement :

- **Aucune facture impayée** pour le souscripteur → le hold est **libéré**
  (`WalletHoldStatus.RELEASED`) — les fonds redeviennent disponibles dans le wallet.
- **Facture(s) impayée(s)** détectée(s) (via les documents "recouvrables" du souscripteur)
  → le hold est **capturé** (`WalletHoldStatus.CAPTURED`) — le montant est définitivement
  déduit du wallet pour compenser l'impayé. Un événement est tracé dans l'historique de
  l'abonnement (`GET /subscriptions/{subscriptionNumber}/events` ou équivalent).

> Limite de scope volontaire : aucune facture de régularisation n'est générée
> automatiquement lors d'une capture — cela reste une suite manuelle pour l'équipe finance.
> Le frontend peut simplement refléter le statut final du hold (`CAPTURED`/`RELEASED`) dans
> la vue wallet/abonnement.

---

## 4. Paiements — historique des remboursements partiels et plafonnement

### 4.1 Nouvel endpoint : liste des remboursements d'une transaction

```http
GET /payments/transactions/{transactionNumber}/refunds
```

Retourne la liste chronologique (`PaymentTransactionResponse[]`) de tous les remboursements
et contrepassations émis pour cette transaction d'origine — utile pour afficher un historique
détaillé "remboursé X le DD/MM, remboursé Y le DD/MM…" sur la fiche transaction/paiement.

### 4.2 Changement de comportement : `POST /payments/transactions/{transactionNumber}/refund`

**Avant** : un seul remboursement (partiel ou total) était possible par transaction — toute
tentative suivante échouait avec un conflit "transaction already refunded", même si le
premier remboursement n'était que partiel.

**Maintenant** :
- Plusieurs remboursements partiels successifs sont possibles, **plafonnés cumulativement**
  au montant de la transaction d'origine.
- Si `amount` n'est pas fourni dans la requête, le système rembourse automatiquement le
  **solde restant remboursable** (montant d'origine − somme déjà remboursée), et non plus
  systématiquement le montant total d'origine.
- Si le montant demandé dépasse le solde restant remboursable, l'API répond **400 Bad
  Request** avec un message indiquant le montant restant disponible.
- Si la transaction est déjà **intégralement** remboursée/contrepassée, l'API répond
  **409 Conflict** ("transaction already fully refunded/reversed").
- Le **statut de la transaction d'origine** ne bascule à `REFUNDED`/`REVERSED` (et celui de
  l'intention de paiement associée) **que lorsque le cumul des remboursements atteint le
  montant total d'origine** — pas de nouveau statut intermédiaire introduit (la transaction
  reste `SUCCEEDED` tant que le remboursement est partiel). Le frontend doit donc se fier au
  cumul (`GET .../refunds`) plutôt qu'au seul statut pour déterminer si une transaction a été
  partiellement remboursée.
- Ce même comportement de plafonnement cumulé s'applique aux remboursements mobile money
  asynchrones (callback PawaPay) : le statut de la transaction d'origine ne change qu'au
  remboursement complet, une fois le callback de complétion reçu.

---

## Récapitulatif des nouveaux/changés endpoints

| Méthode | Route | Nouveauté |
|---|---|---|
| GET | `/bookings/cancellation-policy` | Nouveau — paliers de remboursement actifs |
| GET | `/payments/transactions/{transactionNumber}/refunds` | Nouveau — historique des remboursements |
| POST | `/payments/cash-registers` (create) | Champ `managerEmail` ajouté à la requête et à la réponse |
| POST | `/payments/transactions/{transactionNumber}/refund` | Comportement changé — plafonnement cumulé, 400 si dépassement, statut d'origine ne flippe qu'au remboursement complet |
| PATCH | `/bookings/{bookingNumber}/cancel` | Comportement enrichi — avoir + remboursement automatiques selon la politique d'annulation (non bloquant) |
| POST | `/subscriptions` | Comportement enrichi — pose automatique d'un `WalletHold` si `depositAmount > 0` (non bloquant) |
| (lifecycle) | annulation d'abonnement | Comportement enrichi — libération ou capture automatique du `WalletHold` de dépôt |
