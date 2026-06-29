# Plan d'implémentation — Améliorations Payment / Billing / Wallet

## Contexte

Les trois modules sont déjà matures (paiements multi-canaux, liens de paiement, avoirs,
échéanciers, holds wallet, dunning, réconciliation, relevés client CSV/JSON, dépôts mobile
money PawaPay...). Plutôt que de tout traiter, ce plan retient un sous-ensemble priorisé
par valeur business / effort, qui s'appuie au maximum sur l'infrastructure existante :

- `WalletService.credit/debit` + `WalletLedgerEntry` → transferts et relevés
- `WalletHold` (blocage réversible de fonds) → caution/acompte d'abonnement
- `CashAnomalyDetectionServiceImpl` / `CashAnomalyFlag` (type `NEAR_MAX_CASH_RECURRENCE`
  déjà défini) → alerte de dépassement de caisse
- `BillingDocumentService.createCreditNote` + `PaymentService` refund → avoir automatique
- `BillingEmailServiceImpl` comme modèle pour les nouveaux services d'email
- Pipeline PDF OpenHtmlToPDF (mirroring `BillingDocumentPdfServiceImpl`) → export wallet
- PawaPay (`PawapayClient`, `MobileMoneyController`) → recharge automatique du wallet

Prochaine migration Flyway : **V127** (dernière existante : `V126__task_parent_task_reference.sql`).

---

## Phase 1 — Wallet : transfert pair-à-pair (P2P) + relevé exportable

### 1.1 Transfert entre wallets

- `POST /wallets/{walletNumber}/transfer`
- `WalletTransferRequest(String targetWalletNumber, BigDecimal amount, String reason)`
- `WalletServiceImpl.transfer(...)` : dans une seule transaction, `debit(source, ...)`
  puis `credit(target, ...)`, en réutilisant les méthodes `credit`/`debit` existantes
- Nouveaux types `WalletEntryType.TRANSFER_OUT` / `TRANSFER_IN` (l'enum actuel ne couvre
  que `ADMIN_TOPUP/ADMIN_DEBIT/PAYMENT/REFUND/REVERSAL/HOLD/HOLD_RELEASE/ADJUSTMENT/
  CASHBACK/PROMOTIONAL_CREDIT/OVERPAYMENT_CREDIT`)
- Validations : wallets distincts, même `currency`, statut `ACTIVE`, `availableBalance`
  source suffisant
- Migration `V127__wallet_entry_type_transfer.sql` (si `WalletEntryType` est persisté en
  colonne `CHECK`/enum côté DB — à vérifier sur la table `wallet_ledger_entry`)

### 1.2 Export du relevé (PDF / CSV)

- `GET /wallets/{walletNumber}/ledger/export?format=pdf|csv&from=...&to=...`
- Mirroring du pattern `statement` / `statement.csv` de `BillingDocumentController`
- Pour le PDF : nouveau template JTE + service `WalletStatementPdfServiceImpl`
  (mirroring `BillingDocumentPdfServiceImpl`, pipeline OpenHtmlToPDF déjà en place)

---

## Phase 2 — Payment : alerte de dépassement de caisse

### 2.1 Notification du manager au franchissement de `maxCashAmount`

- `CashAnomalyDetectionServiceImpl` détecte déjà `NEAR_MAX_CASH_RECURRENCE` et crée des
  `CashAnomalyFlag` — il manque la notification proactive
- Ajouter, à la création d'un flag de sévérité élevée (ou dépassement effectif de
  `maxCashAmount`), l'envoi d'un email au manager du point de vente
- Nouveau `CashAlertEmailService` / `CashAlertEmailServiceImpl` (mirroring
  `BillingEmailServiceImpl` : `@Async`, try/catch + log, Freemarker)
- Template `email/cash-anomaly-alert.html`
- Option : champ `lastAnomalyAlertSentAt` sur `CashRegister` pour éviter le spam
  (similaire au pattern `dormantAlertSentAt` du module CRM)

---

## Phase 3 — Billing : avoir automatique sur annulation

### 3.1 Politique d'annulation configurable

- Nouvelle entité `CancellationPolicy` (ou table de configuration simple à paliers) :
  ```
  hoursBeforeStart | refundPercentage
  > 24h            | 100%
  12h–24h          | 50%
  < 12h            | 0%
  ```
- Démarrer avec une politique **globale** (1 jeu de paliers par défaut), extensible plus
  tard par type de ressource ou par plan
- Migration `V128__billing_cancellation_policy.sql`

### 3.2 Génération automatique de l'avoir + remboursement

- Hook dans le flux d'annulation de réservation existant : si une facture
  `BillingDocument` est `ISSUED/SENT/PAID/PARTIALLY_PAID` pour la réservation annulée :
  1. calcule le `refundPercentage` selon la politique et le délai avant le début
  2. appelle `billingDocumentService.createCreditNote(invoiceNumber, ...)` pour le
     montant correspondant
  3. déclenche le remboursement via `PaymentService` (transaction d'origine) pour le
     même montant
- Tracer la politique appliquée (pourcentage, règle déclenchée) dans l'historique du
  document (`BillingDocumentEditHistory` existe déjà)

---

## Phase 4 — Billing : acompte / caution sur abonnement

### 4.1 Configuration du dépôt sur le plan

- Champ `depositAmount` (nullable) sur le plan d'abonnement (`PlanPrice` ou entité plan
  associée — à confirmer lors de l'implémentation)
- Migration `V129__subscription_plan_deposit.sql`

### 4.2 Blocage et restitution du dépôt via `WalletHold`

- À la création de l'abonnement : si `depositAmount > 0`, créer un `WalletHold` sur le
  wallet du membre (réutilise le mécanisme déjà en place pour les réservations — pas de
  nouveau concept à introduire)
- À la fin du contrat : si aucun impayé, `release` le hold (restitution) ; sinon,
  `capture` partiel/total pour compenser l'impayé puis avoir/facture de régularisation

---

## Phase 5 — Payment : historique des remboursements partiels

### 5.1 Plafonner et tracer les remboursements cumulés

- `RefundPaymentRequest` accepte déjà un `amount` arbitraire — rien n'empêche
  aujourd'hui de dépasser le montant remboursable
- Calculer le **montant restant remboursable** = montant de la transaction − somme des
  remboursements déjà effectués (agrégation sur `PaymentTransaction`/allocations liées,
  pas de nouvelle colonne nécessaire)
- Rejeter (400) toute demande dépassant ce solde
- `GET /payments/transactions/{transactionNumber}/refunds` → historique des
  remboursements partiels (montant, raison, date, auteur)

---

## Phase 6 — Wallet : recharge automatique (auto-reload)

### 6.1 Configuration du seuil et du montant de recharge

- Champs sur `WalletAccount` : `autoReloadEnabled`, `autoReloadThreshold`,
  `autoReloadAmount`, `autoReloadMsisdn` (numéro mobile money pour le débit)
- Migration `V130__wallet_auto_reload.sql`
- Endpoint `PUT /wallets/{walletNumber}/auto-reload` (configuration par le titulaire,
  avec confirmation via OTP/callback mobile money à la première activation)

### 6.2 Worker de déclenchement

- Nouveau `WalletAutoReloadWorker` (`@Scheduled`, cadence configurable, mirroring
  `WalletHoldExpiryWorker`/`MobileMoneyStatusPollingWorker`)
- Pour chaque wallet `autoReloadEnabled = true` avec `availableBalance < autoReloadThreshold`
  et pas de recharge en cours : crée un `PaymentIntent` de type `MOBILE_MONEY` pour
  `autoReloadAmount`, redirige vers PawaPay ; le callback existant crédite le wallet

---

## Décisions à valider

| Sujet | Option recommandée | Pourquoi |
|---|---|---|
| **Transfert P2P** : entre quels wallets ? | Même `ownerType` et même `currency`, statut `ACTIVE` des deux côtés | Évite les transferts inter-types (ex. company → resource) et les soucis de change |
| **Politique d'annulation** : portée | Politique globale unique au démarrage (paliers par défaut), pas encore par plan/ressource | Livre la valeur (avoir auto) rapidement ; granularité par plan ajoutable en V2 sans casser l'API |
| **Dépôt d'abonnement** : mécanisme de blocage | `WalletHold` (réversible) plutôt qu'un débit ferme | Réutilise une infra déjà testée et auditée ; restitution = simple `release` |
| **Recharge automatique** : qui configure | Le titulaire du wallet, en self-service (espace client), avec confirmation mobile money | Cohérent avec le modèle actuel où le dépôt mobile money est initié côté client |

---

## Ordre de développement recommandé

| Ordre | Phase | Valeur | Effort | Dépendances |
|---|---|---|---|---|
| 1 | Phase 1 — Transfert P2P + relevé wallet | Élevée (demande fréquente côté membres) | Moyen | Aucune — s'appuie sur `credit`/`debit` existants |
| 2 | Phase 2 — Alerte dépassement caisse | Élevée (risque opérationnel) | Faible | `CashAnomalyDetectionServiceImpl` déjà en place |
| 3 | Phase 5 — Historique remboursements partiels | Moyenne (fiabilisation) | Faible | Aucune |
| 4 | Phase 3 — Avoir automatique sur annulation | Élevée (réduit la charge manuelle) | Moyen-élevé | Flux d'annulation booking + `createCreditNote` |
| 5 | Phase 4 — Acompte/caution abonnement | Moyenne (dépend du besoin business) | Moyen | `WalletHold` |
| 6 | Phase 6 — Recharge automatique wallet | Moyenne (confort client) | Moyen-élevé | PawaPay + nouveau worker |

---

## Prochaine étape

Valider les décisions ci-dessus, puis démarrer par la **Phase 1** (transfert P2P +
export du relevé wallet), qui ne touche à aucun flux financier sensible (pas de
remboursement, pas d'annulation) et permet de roder le pattern `WalletEntryType` étendu
avant d'attaquer les phases à plus fort impact (avoirs automatiques, dépôts).
