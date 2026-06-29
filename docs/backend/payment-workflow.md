# Module Paiement - Workflow Backend

## Objectif

Ce document explique comment fonctionne le module `payment` côté backend dans `bokati-cowork`, quels sont ses composants, ses workflows métier, ses dépendances et ses limites actuelles.

Le module paiement ne vit pas seul:

- il s'appuie fortement sur le module `billing`
- il interagit avec les `billable_item` du domaine subscription/booking
- il pilote aussi le wallet interne et la caisse cash register
- il expose un début d'infrastructure pour réconciliation et provider externe

Ce document décrit l'implémentation actuelle, pas une cible théorique.

## Portée

Le périmètre principal couvert ici:

- intentions de paiement
- transactions de paiement
- allocation vers factures
- paiement de facture direct
- recouvrement member/customer
- wallet interne
- wallet holds
- cash register / cash session / cash movements
- remboursement et reverse
- lots de réconciliation
- workers d'expiration / alerte

Hors coeur du module mais lié:

- échéanciers de paiement (`payment_schedule`) dans le module `billing`
- provider mobile money réel non encore branché

## Vue d'ensemble

Le workflow standard est:

1. créer une `PaymentIntent`
2. exécuter une `PaymentTransaction`
3. allouer la transaction à une ou plusieurs factures si nécessaire
4. mettre à jour les effets secondaires:
   wallet, caisse, facture, trop-perçu, email

La règle centrale du backend est donc:

`PaymentIntent -> PaymentTransaction -> PaymentAllocation -> BillingDocument / Wallet / Cash`

## Cartographie du code

### Controllers

- `features.payment.controller.PaymentController`
- `features.payment.controller.WalletController`
- `features.payment.controller.WalletHoldController`
- `features.payment.controller.CashRegisterController`
- `features.payment.controller.PaymentReconciliationController`
- `features.billing.controller.BillingDocumentController`

Point important:

- le module payment expose les intents, cash, wallet, refund, reverse et recovery
- le endpoint de paiement direct d'une facture est exposé dans `BillingDocumentController` via `POST /billing/invoices/{documentNumber}/pay`

### Services principaux

- `PaymentServiceImpl`
- `WalletServiceImpl`
- `WalletHoldServiceImpl`
- `CashRegisterServiceImpl`
- `PaymentReconciliationServiceImpl`

### Services de support

- `PaymentAllocationService`
- `WalletLedgerService`
- `CashSessionSummarySupport`

### Dépendances externes au module

- `BillingDocumentService`
- `BillingDocumentRepository`
- `BillingEmailService`
- `BillingAutoInvoiceService`
- `BillableItemRepository`

## Modèle métier

### PaymentIntent

Rôle:

- représente l'intention de payer
- fige le client, le montant, la devise, le contexte métier et l'idempotence

Champs structurants:

- `intentNumber`
- `customerType`
- `customerCode`
- `amount`
- `currency`
- `status`
- `purpose`
- `sourceType`
- `sourceCode`
- `idempotencyKey`
- `expiresAt`
- `metadataJson`

Statuts définis:

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

Statuts réellement utilisés aujourd'hui par le service principal:

- `PENDING`
- `SUCCEEDED`
- `EXPIRED`

Les autres existent dans l'enum mais ne sont pas encore pilotés par un workflow complet.

### PaymentTransaction

Rôle:

- matérialise une exécution de paiement
- porte la méthode, la référence provider, le montant final et le statut d'exécution

Champs structurants:

- `transactionNumber`
- `paymentIntentId`
- `paymentMethod`
- `provider`
- `providerReference`
- `amount`
- `currency`
- `status`
- `paidAt`
- `receivedBy`
- `failureReason`
- `metadataJson`

Méthodes définies:

- `CASH`
- `WALLET`
- `MOBILE_MONEY`
- `BANK_TRANSFER`
- `CARD`
- `CHEQUE`
- `MANUAL_ADJUSTMENT`
- `CREDIT_NOTE`

Méthodes réellement branchées de bout en bout:

- `CASH`
- `WALLET`

Les autres peuvent être utilisées dans `payInvoice`, mais sans orchestration provider réelle dans le module.

### PaymentAllocation

Rôle:

- lie une transaction à une facture
- mémorise combien a été affecté à quel document

Cas d'usage:

- facture unique
- plusieurs factures
- recovery
- facture auto-générée pour source non billing

### WalletAccount

Rôle:

- portefeuille interne par `ownerType + ownerCode + currency`

Soldes maintenus:

- `availableBalance`
- `ledgerBalance`
- `heldBalance`

Invariants:

- les trois soldes restent positifs ou nuls
- un wallet est unique pour un propriétaire et une devise

### WalletLedgerEntry

Rôle:

- journal comptable du wallet
- source de traçabilité pour top-up, paiement, hold, release, refund, trop-perçu

Important:

- `entryOnly(...)` écrit un mouvement sans recalculer les soldes
- ce mode est utilisé pour les holds, captures et releases afin de journaliser des mouvements déjà appliqués ailleurs

### WalletHold

Rôle:

- réserve temporairement une somme sur le solde disponible

Statuts:

- `ACTIVE`
- `CAPTURED`
- `RELEASED`
- `EXPIRED`

### CashRegister / CashSession / CashMovement

Le sous-module caisse gère:

- la caisse physique
- sa session d'ouverture / fermeture
- les mouvements internes et externes
- les paiements cash rattachés à la session

Entités:

- `CashRegister`
- `CashSession`
- `CashMovement`

Statuts de session:

- `OPEN`
- `SUSPENDED`
- `CLOSING_REVIEW`
- `CLOSED`
- `CANCELLED`

### PaymentReconciliationBatch

Rôle:

- lot logique de rapprochement par provider

Etat actuel:

- création du lot
- fermeture du lot
- pas encore de rattachement automatique des transactions au batch

## Conventions de numérotation

Les numéros métier sont générés via `SequenceGeneratorFacade` et enrichis via `CodeComposer`.

Séquences principales:

- `payment_intent`
- `payment_transaction`
- `wallet_account`
- `wallet_entry`
- `wallet_hold`
- `cash_register`
- `cash_session`
- `cash_movement`
- `payment_reconciliation_batch`

Le code final dépend du contexte. Exemple:

- `INT-...` pour intent
- `TXN-...` pour transaction
- `WAL-...` pour wallet
- `CSR-...` pour caisse
- `CSS-...` pour session

## Principes transverses

### Idempotence

Le module utilise deux mécanismes:

- `idempotencyKey` explicite côté API
- réutilisation métier d'une `PaymentIntent` existante dans certains workflows

Exemples:

- `createIntent(...)` retourne l'intention existante si `idempotencyKey` existe déjà
- `payInvoice(...)` reconstruit ou réutilise l'intention avant d'exécuter le paiement
- le recovery calcule un fingerprint du lot de documents pour réutiliser une intention encore valide

### Précision monétaire

Le module normalise les montants en `NUMERIC(19,4)` et applique `setScale(4, HALF_UP)` dans plusieurs services.

Conséquences:

- le stockage garde 4 décimales
- les vues PDF peuvent ensuite choisir un autre format d'affichage

### metadataJson

Plusieurs entités stockent un `metadataJson` en `jsonb`:

- `payment_intent`
- `payment_transaction`
- `cash_movement`
- `billing_document`

Usages:

- tracer l'origine front/back
- embarquer le contexte métier
- porter les `documentNumbers` utilisés par le recovery

## Workflows backend

## 1. Création d'une intention manuelle

Entrée:

- `POST /payments/intents`

Traitement:

1. vérifier l'idempotence si `idempotencyKey` est fourni
2. valider que `amount > 0`
3. générer `intentNumber`
4. enregistrer l'intention en `PENDING`

Sortie:

- une `PaymentIntentResponse`

Ce flow ne déclenche aucun mouvement financier réel.

## 2. Création d'une intention depuis une facture

Entrée:

- `POST /payments/intents/from-billing-document`

Traitement:

1. charger la facture
2. vérifier que `balanceDue > 0`
3. construire une intention avec:
   `sourceType = BILLING_DOCUMENT`
4. `sourceCode = documentNumber`
5. `amount = balanceDue`

Objectif:

- figer un paiement payable sur le solde courant de la facture

## 3. Création d'une intention pour plusieurs factures

Entrée:

- `POST /payments/intents/from-billing-documents`

Traitement:

1. charger chaque facture
2. ignorer les documents déjà soldés
3. vérifier l'unicité:
   même client, même devise
4. sommer les `balanceDue`
5. créer une intention avec:
   `sourceType = MULTI_BILLING_DOCUMENT`
6. `sourceCode = document1,document2,...`

Allocation future:

- l'ordre d'allocation suit l'ordre des documents dans `sourceCode`

## 4. Preview de recouvrement

Entrée:

- `GET /payments/recovery/{customerType}/{customerCode}`

Ce workflow cherche tout ce qui reste payable pour un client:

- factures ouvertes recouvrables
- proformas recouvrables
- billable items encore non facturés

Source des données:

- `BillingDocumentRepository.findRecoverableDocuments(...)`
- `BillableItemRepository.findRecoverableItems(...)`

Règles:

1. si plusieurs devises ouvertes existent et qu'aucune devise n'est fournie, le backend rejette la demande
2. les documents sont triés par ancienneté
3. les billable items sont triés par ancienneté

La réponse expose:

- `openDocumentAmount`
- `pendingBillableAmount`
- `totalPayableAmount`
- `documents`
- `pendingBillableItems`
- `paymentIntent = null`

But:

- permettre au frontend d'afficher un reste à payer global sans créer encore d'intention

## 5. Création ou reprise d'une intention de recouvrement

Entrée:

- `POST /payments/intents/recovery`

Traitement:

1. reconstruire le même contexte que le preview
2. si des `billable_item` non facturés existent:
   créer automatiquement une facture depuis ces items
3. si cette facture est `DRAFT`:
   l'émettre immédiatement
4. recalculer les documents ouverts
5. construire un fingerprint SHA-256 sur:
   `customerType|customerCode|currency|documentNumbers`
6. chercher une `PaymentIntent` réutilisable sur:
   `customerType + customerCode + sourceType=PAYABLE_RECOVERY + sourceCode=fingerprint`
7. si une intention `PENDING/PROCESSING/AUTHORIZED` existe encore et que le montant correspond, la réutiliser
8. sinon créer une nouvelle intention

Particularité:

- `metadataJson` de l'intention de recovery contient explicitement `documentNumbers` et `pendingBillableNumbers`
- cette information est ensuite relue par `PaymentAllocationService`

Objectif métier:

- reconstituer un "panier payable" stable pour un client

## 6. Paiement cash d'une intention

Entrée:

- `PATCH /payments/intents/{intentNumber}/cash`

Règles:

- seule la devise `XAF` est autorisée
- l'intention doit être `PENDING` ou `PROCESSING`
- si `expiresAt` est dépassé, l'intention passe en `EXPIRED`

Traitement:

1. créer une `PaymentTransaction` `SUCCEEDED`
2. si `cashSessionNumber` est fourni:
   écrire un mouvement de caisse `PAYMENT`
3. marquer l'intention en `SUCCEEDED`
4. appeler `PaymentAllocationService.allocateIfBillingDocument(...)`
5. si le paiement dépasse le total alloué:
   créditer le trop-perçu dans le wallet du client

Effets secondaires:

- caisse mise à jour si session présente
- factures allouées
- wallet crédité en cas de surplus

## 7. Paiement wallet d'une intention

Entrée:

- `PATCH /payments/intents/{intentNumber}/wallet`

Validations:

- le wallet doit exister
- le wallet doit appartenir au même client que l'intention
- la devise doit correspondre
- le solde disponible doit être suffisant

Traitement:

1. débiter le wallet avec une entrée `WalletEntryType.PAYMENT`
2. créer une `PaymentTransaction` `SUCCEEDED`
3. marquer l'intention en `SUCCEEDED`
4. allouer la transaction aux factures si nécessaire
5. si surplus:
   recréditer le client dans son wallet avec `OVERPAYMENT_CREDIT`

## 8. Paiement direct d'une facture

Entrée:

- `POST /billing/invoices/{documentNumber}/pay`

Pourquoi ce flow existe:

- il simplifie le paiement direct d'une facture sans que le client appelle explicitement les endpoints `payments/intents`

Traitement:

1. charger la facture
2. calculer le montant à payer:
   soit le montant demandé, soit le solde restant
3. créer ou réutiliser une intention dédiée à cette facture
4. exécuter `processPayment(...)` selon `PaymentMethod`
5. marquer l'intention `SUCCEEDED`
6. allouer la transaction à la facture
7. créditer un éventuel trop-perçu
8. recharger la facture mise à jour
9. envoyer un email de confirmation via `BillingEmailService`

Point important:

- ce flow envoie une confirmation email
- les endpoints `PATCH /payments/intents/{intentNumber}/cash|wallet` ne le font pas

## 9. Allocation aux documents de facturation

Service responsable:

- `PaymentAllocationService`

Comportement selon `sourceType`:

- `BILLING_DOCUMENT`
  allocation sur une seule facture
- `MULTI_BILLING_DOCUMENT`
  allocation séquentielle sur plusieurs factures
- `PAYABLE_RECOVERY`
  allocation séquentielle sur la liste `documentNumbers` stockée dans `metadataJson`
- toute autre source
  création automatique d'une facture si `BillingAutoInvoiceService.shouldAutoInvoice(...)` le permet

Ordre et stratégie:

1. lire le montant restant à allouer
2. pour chaque document, prendre `min(remaining, balanceDue)`
3. appliquer le paiement via `BillingDocumentService.applyPayment(...)`
4. enregistrer une `PaymentAllocation`
5. renvoyer le reliquat non alloué

Effets sur la facture:

- si `balanceDue == 0` -> `PAID`
- sinon -> `PARTIALLY_PAID`

## 10. Auto-invoicing

Si une intention n'est pas liée à `BILLING_DOCUMENT`, `MULTI_BILLING_DOCUMENT` ou `PAYABLE_RECOVERY`, le backend peut générer automatiquement une facture avant allocation.

Service:

- `BillingAutoInvoiceService`

Cas typiques:

- paiement issu d'une transaction ou d'un source métier non encore facturée

But:

- conserver une traçabilité comptable côté billing même pour des flux non initialement facturés

## 11. Remboursement et reverse

Entrées:

- `POST /payments/transactions/{transactionNumber}/refund`
- `POST /payments/transactions/{transactionNumber}/reverse`

Traitement commun:

1. charger la transaction originale
2. calculer le montant remboursé ou reversé
3. vérifier que le montant ne dépasse pas la transaction source
4. inverser les allocations facture via `reverseAllocations(...)`
5. si la méthode originale est `WALLET`:
   recréditer le wallet
6. créer une nouvelle transaction de type:
   `REFUNDED` ou `REVERSED`

Important:

- le module ne modifie pas l'ancienne transaction
- il crée une nouvelle transaction métier
- pour les providers externes, aucun appel provider réel n'est fait aujourd'hui

## 12. Wallet interne

Entrées principales:

- `POST /wallets/admin/top-up`
- `POST /wallets/admin/get-or-create`
- `GET /wallets/{walletNumber}`
- `GET /wallets/{walletNumber}/ledger`

Règles:

- un wallet est unique par propriétaire et devise
- `debit(...)` exige un wallet `ACTIVE`
- `credit(...)` ne bloque pas explicitement les statuts non actifs

Effets des opérations:

- top-up admin -> `ADMIN_TOPUP`
- paiement -> `PAYMENT`
- remboursement -> `REFUND`
- trop-perçu -> `OVERPAYMENT_CREDIT`

## 13. Wallet holds

Entrées:

- `POST /wallet-holds`
- `PATCH /wallet-holds/{holdNumber}/capture`
- `PATCH /wallet-holds/{holdNumber}/release`

Création d'un hold:

1. vérifier le wallet
2. vérifier le solde disponible
3. déplacer le montant:
   `available -> held`
4. journaliser une entrée `HOLD`
5. créer le `WalletHold`

Capture:

1. vérifier que le hold est `ACTIVE`
2. diminuer `heldBalance`
3. diminuer `ledgerBalance`
4. journaliser une entrée `PAYMENT`
5. marquer le hold `CAPTURED`

Release:

1. vérifier que le hold est `ACTIVE`
2. déplacer le montant:
   `held -> available`
3. journaliser `HOLD_RELEASE`
4. marquer `RELEASED`

Expiration:

- traitée par worker
- le hold passe en `EXPIRED`
- les fonds reviennent dans `availableBalance`

## 14. Caisse / Cash register

### Création d'une caisse

Entrée:

- `POST /cash-registers`

Données persistées:

- code
- nom
- emplacement
- entité métier
- device
- flag `cashControlEnabled`
- `maxCashAmount`

Note:

- `maxCashAmount` est stocké mais n'est pas encore réellement contrôlé par la logique métier

### Ouverture de session

Entrée:

- `POST /cash-registers/sessions`

Règles:

- la caisse doit être active
- une caisse ne peut pas avoir une session `OPEN` ou `CLOSING_REVIEW`
- un caissier ne peut pas avoir une autre session active

Effet:

- si `openingAmount > 0`, un mouvement `OPENING_FLOAT` est créé

### Enregistrement d'un paiement cash

Quand `PaymentService` reçoit un paiement cash avec `cashSessionNumber`:

1. il appelle `CashRegisterService.recordPayment(...)`
2. cela crée un `CashMovement`:
   `movementType = PAYMENT`
3. la devise est forcée à `XAF`

### Mouvements manuels

Entrée:

- `POST /cash-registers/sessions/{sessionNumber}/movements`
- `POST /entry-vouchers`
- `POST /exit-vouchers`

Règles:

- `PAYMENT`, `OPENING_FLOAT`, `CLOSING_COUNT` sont réservés au système
- certains types exigent un motif
- certains documents exigent un `documentNumber`

Le modèle accepte des flux plus larges que le coworking pur:

- fournisseur
- banque
- coffre
- transfert inter-caisses
- ajustements

### Résumé de session

Calculé par `CashSessionSummarySupport`:

```text
expectedClosingAmount =
openingAmount
+ totalPayments
+ totalCashIn
+ totalAdjustments
- totalRefunds
- totalCashOut
```

Agrégats utilisés:

- `PAYMENT`
- `REFUND`
- `CASH_IN`, `TRANSFER_IN`
- `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_OUT`
- `ADJUSTMENT`

### Fermeture de session

Entrée:

- `POST /cash-registers/sessions/{sessionNumber}/closing-request`
- ou `PATCH /cash-registers/sessions/{sessionNumber}/close`

Traitement:

1. vérifier que la session est `OPEN`
2. calculer `expectedClosingAmount`
3. calculer `varianceAmount = counted - expected`
4. si écart nul:
   session `CLOSED`
5. si écart non nul:
   `varianceReason` obligatoire
6. la session passe en `CLOSING_REVIEW`

Validation d'écart:

- `PATCH /cash-registers/sessions/{sessionNumber}/approve-variance`

Effet:

- la session passe en `CLOSED`

## 15. Réconciliation

Entrées:

- `POST /payments/reconciliation-batches`
- `PATCH /payments/reconciliation-batches/{batchNumber}/complete`

Etat actuel:

- un batch est créé avec `provider` et `createdBy`
- `complete(...)` le passe de `OPEN` à `COMPLETED`

Limite importante:

- aucune transaction n'est liée automatiquement à un batch
- il s'agit aujourd'hui d'un conteneur métier minimal, pas encore d'un moteur complet de rapprochement

## Workers et automatisation

### PaymentIntentExpiryWorker

Propriété:

- `bokati.payment.workers.intent-expiry-delay-ms`

Rôle:

- passe en `EXPIRED` les intents `PENDING/PROCESSING/AUTHORIZED` dépassés

### PaymentIntentExpiryAlertWorker

Propriété:

- `bokati.payment.workers.expiry-alert-delay-ms`

Rôle:

- détecte les intents expirant dans moins de 2 heures
- journalise un warning
- envoie un email uniquement si:
  `sourceType = BILLING_DOCUMENT`

### WalletHoldExpiryWorker

Propriété:

- `bokati.payment.workers.wallet-hold-expiry-delay-ms`

Rôle:

- expire les `WalletHold` arrivés à échéance

## Endpoints backend

### Payment

- `POST /payments/intents`
- `POST /payments/intents/from-billing-document`
- `POST /payments/intents/from-billing-documents`
- `GET /payments/recovery/{customerType}/{customerCode}`
- `POST /payments/intents/recovery`
- `PATCH /payments/intents/{intentNumber}/cash`
- `PATCH /payments/intents/{intentNumber}/wallet`
- `POST /payments/transactions/{transactionNumber}/refund`
- `POST /payments/transactions/{transactionNumber}/reverse`
- `GET /payments/intents/{intentNumber}`
- `GET /payments/intents/{intentNumber}/transactions`
- `GET /payments/intents`

### Wallet

- `GET /wallets`
- `POST /wallets/admin/top-up`
- `POST /wallets/admin/get-or-create`
- `GET /wallets/{walletNumber}`
- `GET /wallets/{walletNumber}/ledger`

### Wallet holds

- `POST /wallet-holds`
- `PATCH /wallet-holds/{holdNumber}/capture`
- `PATCH /wallet-holds/{holdNumber}/release`

### Cash register

- `POST /cash-registers`
- `GET /cash-registers`
- `PATCH /cash-registers/{registerCode}/activate`
- `PATCH /cash-registers/{registerCode}/deactivate`
- `POST /cash-registers/sessions`
- `GET /cash-registers/sessions`
- `GET /cash-registers/sessions/{sessionNumber}/summary`
- `POST /cash-registers/sessions/{sessionNumber}/movements`
- `POST /cash-registers/sessions/{sessionNumber}/entry-vouchers`
- `POST /cash-registers/sessions/{sessionNumber}/exit-vouchers`
- `GET /cash-registers/movements`
- `POST /cash-registers/sessions/{sessionNumber}/closing-request`
- `PATCH /cash-registers/sessions/{sessionNumber}/close`
- `PATCH /cash-registers/sessions/{sessionNumber}/approve-variance`

### Réconciliation

- `POST /payments/reconciliation-batches`
- `PATCH /payments/reconciliation-batches/{batchNumber}/complete`

### Billing lié au paiement

- `POST /billing/invoices/{documentNumber}/pay`

## Limites et points d'attention

### 1. Le provider mobile money est un stub

Le provider actuel est:

- `NoopMobileMoneyPaymentProvider`

Conséquence:

- l'API métier existe
- l'intégration réelle n'est pas opérationnelle en production

### 2. Les enums sont plus riches que les flows branchés

Exemples:

- `PaymentIntentStatus.AUTHORIZED` existe mais n'est pas réellement produit par un flow actuel
- `PaymentMethod.CARD`, `BANK_TRANSFER`, `MOBILE_MONEY` existent mais sans orchestration complète dans `PaymentController`

### 3. La réconciliation est minimale

Le batch de réconciliation n'agrège pas encore:

- transactions
- écarts
- rapprochements automatiques provider

### 4. La caisse est limitée à XAF

Constaté dans:

- `CashRegisterServiceImpl`
- `PaymentServiceImpl.registerCashPayment(...)`

### 5. Le trop-perçu est recrédité en wallet

Après allocation:

- tout reliquat est crédité au wallet du client
- même si le paiement initial était cash

### 6. Refund / reverse ne pilotent pas un provider externe

Aujourd'hui:

- les allocations billing sont inversées
- le wallet est recrédité si la source était wallet
- aucun appel réel vers un PSP externe n'est fait

### 7. Le paiement direct facture et le paiement via intent ne sont pas symétriques

Différence notable:

- `payInvoice(...)` envoie une confirmation email
- `registerCashPayment(...)` et `payWithWallet(...)` ne le font pas

### 8. L'échéancier de paiement est dans billing

Le support `payment_schedule` a été ajouté en base et vit sous `features.billing`.

Cela signifie:

- c'est un composant proche du paiement
- mais son ownership fonctionnel est côté billing, pas dans `features.payment`

## Références utiles

Pour les payloads API frontend:

- `docs/payment-frontend-api.md`
- `docs/billing-payment-frontend-api.md`
- `docs/wallet-frontend-api.md`
- `docs/invoice-pay-frontend-api.md`

Pour le code backend principal:

- `features.payment.service.implementation.PaymentServiceImpl`
- `features.payment.service.support.PaymentAllocationService`
- `features.payment.service.implementation.WalletServiceImpl`
- `features.payment.service.implementation.WalletHoldServiceImpl`
- `features.payment.service.implementation.CashRegisterServiceImpl`
- `features.payment.service.implementation.PaymentReconciliationServiceImpl`
- `features.billing.service.implementation.BillingDocumentServiceImpl`
- `features.billing.service.support.BillingAutoInvoiceService`

## Résumé opérationnel

Le module paiement est aujourd'hui un orchestrateur backend centré sur quatre responsabilités:

- transformer un besoin de paiement en `PaymentIntent`
- exécuter ce paiement en `PaymentTransaction`
- répercuter le paiement sur la facturation via allocation
- gérer les effets financiers secondaires dans le wallet et la caisse

En pratique, les flows les plus matures sont:

- paiement de facture
- paiement cash
- paiement wallet
- recovery client
- caisse cash register

Les zones encore légères ou incomplètes sont:

- mobile money réel
- réconciliation avancée
- exploitation complète de tous les statuts / méthodes définis dans les enums
