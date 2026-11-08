# payment : parcours des paiements (intentions, encaissements, remboursements, reçus)

> Rédigé d'après le code. Point d'entrée du module : [README.md](README.md). Routes exactes : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Toutes les routes de ce fichier sont du back-office (`PaymentController`, `PaymentReceiptController`, `PaymentReconciliationController`), droits `PAYMENT:READ`, `PAYMENT:PROCESS` ou `PAYMENT:REFUND`. Le portail membre paie par ses propres routes (module `portal`), qui appellent les mêmes services.

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Intention de paiement | `intentNumber` (`INT-<abréviation du type de client>-<jour>-<séquence>`, `PaymentServiceImpl.java:128`) | Ce qu'un client doit payer : client (`customerType` + `customerCode`), montant, devise, origine (`sourceType` + `sourceCode`), échéance `expiresAt`. Peut être réglée en plusieurs transactions. |
| Transaction | `transactionNumber` (`TXN-...`) | Un encaissement, un remboursement ou une annulation, par un moyen (`paymentMethod`). Les remboursements portent dans `metadataJson` le `originalTransactionNumber`. |
| Reçu | `receiptNumber` | Émis sur chaque transaction encaissée (`SUCCEEDED`). Consultable et imprimable en PDF. |
| Imputation | aucun | Lien transaction → facture (`PaymentAllocation`), créé automatiquement ; il fait baisser le `balanceDue` de la facture (module `billing`). |
| Lot de rapprochement | `batchNumber` | Coquille `OPEN` → `COMPLETED` ; voir Écarts. |

Origines (`sourceType`) produites par ce module : `BILLING_DOCUMENT` (une facture, `sourceCode` = numéro de facture), `MULTI_BILLING_DOCUMENT` (plusieurs factures, `sourceCode` = numéros séparés par des virgules), `PAYABLE_RECOVERY` (recouvrement global, `sourceCode` = empreinte SHA-256). D'autres modules créent des intentions avec `SUBSCRIPTION`, `PASS`, `PASS_RENEWAL`, `BOOKING`, `WALLET_TOPUP`, commande de la place de marché ; le paiement réussi déclenche alors la suite métier (voir [README.md](README.md#effets-automatiques)).

Types de client acceptés : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`. Le serveur traduit `MEMBRE` → `MEMBER`, `CLIENT` → `CUSTOMER`, `BUSINESS`/`COMPANY`/`ENTREPRISE` → `BUSINESS_ENTITY`, et déduit le type d'un code `MBR-`, `CUS-`, `BUS-`/`BIZ-` quand on envoie `CLIENT` ou `CUSTOMER` (`PaymentServiceImpl.java:1154`).

Montants de `PaymentIntentResponse` utiles à l'écran : `amount` (total), `paidAmount` (encaissé), `processingAmount` (en attente d'un opérateur), `remainingAmount` (= total - encaissé), `payableAmount` (= reste - en attente : ce qu'on peut encore encaisser maintenant). Calcul : `PaymentServiceImpl.java:1111`.

## Statuts et cycle de vie

### Intention (`PaymentIntentStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | Payable, rien en cours. | Création ; échec d'un dépôt mobile money ; paiement partiel sans rien en cours | `PROCESSING`, `SUCCEEDED`, `EXPIRED` |
| `PROCESSING` | Un dépôt mobile money est en attente de l'opérateur, ou paiement partiel avec un dépôt en cours. Reste payable pour la part non engagée. | Envoi d'un dépôt mobile money | `SUCCEEDED`, `PENDING`, `EXPIRED` |
| `SUCCEEDED` | Entièrement payée (`paidAmount` ≥ `amount`). | Encaissement espèces, portefeuille, mobile money confirmé | `REFUNDED`, `REVERSED` (remboursement ou annulation **total**) |
| `EXPIRED` | Échéance `expiresAt` dépassée sans paiement complet. | Worker toutes les 5 min (`PaymentIntentExpiryWorker`) ; échec mobile money après échéance | `SUCCEEDED` si un dépôt en cours aboutit après coup (le rapprochement de l'intention réécrit le statut, `PawapayCallbackProcessor.java:288`) |
| `REFUNDED` | Totalement remboursée. | Remboursement couvrant tout le montant de la transaction d'origine | aucun |
| `REVERSED` | Totalement annulée. | Route `reverse` couvrant tout le montant | aucun |
| `AUTHORIZED`, `FAILED`, `CANCELLED`, `PARTIALLY_REFUNDED` | Déclarés dans l'énumération, **jamais écrits** par le code. | | |

Un remboursement partiel ne change pas le statut de l'intention (elle reste `SUCCEEDED`).

### Transaction (`PaymentTransactionStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PROCESSING` | Dépôt ou remboursement mobile money en attente de l'opérateur. | Envoi mobile money, remboursement mobile money | `SUCCEEDED`/`REFUNDED`, `FAILED` |
| `SUCCEEDED` | Encaissée, reçu émis. | Espèces, portefeuille, confirmation mobile money, `payInvoice` (facturation) | `REFUNDED` ou `REVERSED` quand le cumul des remboursements atteint son montant |
| `FAILED` | Refusée par l'opérateur (dépôt ou remboursement). | Refus, échec confirmé, résolution manuelle | aucun |
| `REFUNDED` | Sur une transaction de remboursement : remboursement effectué. Sur l'originale : totalement remboursée. | Route `refund` (immédiat hors mobile money ; au rappel pour le mobile money) | aucun |
| `REVERSED` | Même chose pour une annulation. | Route `reverse` | aucun |
| `PENDING`, `CANCELLED` | Jamais écrits. | | |

`paymentMethod` : `CASH`, `WALLET`, `MOBILE_MONEY` sont produits par les routes de ce module ; `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` arrivent par `POST /sni/api/v1/billing/invoices/{documentNumber}/pay` (module `billing`). `channel` : `SELF_SERVICE` (le client depuis son espace), `BACK_OFFICE` (un agent), `SYSTEM` (automate), nul pour les anciennes transactions.

## Parcours

### 1. Créer une intention de paiement

Trois façons, selon l'écran :

1. **Depuis une facture** : `POST /sni/api/v1/payments/intents/from-billing-document` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-intents-from-billing-document)) avec `{ "documentNumber": "..." }`, et au besoin `amount` (paiement partiel ; absent ou ≤ 0 : tout le `balanceDue`), `expiresAt`. Si une intention de la même facture, du même montant, payable et non expirée existe, elle est **renvoyée au lieu d'en créer une** (`PaymentServiceImpl.java:154`).
2. **Depuis plusieurs factures** : `POST /sni/api/v1/payments/intents/from-billing-documents` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-intents-from-billing-documents)) avec `{ "documentNumbers": [...] }`. Les factures soldées sont ignorées en silence ; les autres doivent avoir le même client et la même devise. L'encaissement solde les factures dans l'ordre de la liste.
3. **Libre** : `POST /sni/api/v1/payments/intents` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-intents)) avec `customerType`, `customerCode`, `amount`, `currency` (obligatoires) et `purpose`, `sourceType`, `sourceCode`, `idempotencyKey`, `expiresAt`, `metadataJson`. `createdBy` est accepté et ignoré. Un paiement sur une intention dont l'origine n'est pas une facture (ni `WALLET_TOPUP`) crée automatiquement une facture d'après le paiement (`BillingAutoInvoiceService.shouldAutoInvoice`, module `billing`).

Dans les trois cas, **`expiresAt` vaut 15 minutes après la création s'il est absent** (`PaymentServiceImpl.java:1092`). Pour un encaissement immédiat au guichet, c'est suffisant ; pour un lien de paiement ou un paiement différé, envoyer une échéance explicite (date-heure ISO avec fuseau).

Pour trouver les factures payables d'un client : `GET /sni/api/v1/payments/payable-documents?customerType=&customerCode=` ([route](routes/PaymentController.md#r-get-sni-api-v1-payments-payable-documents)), filtres `documentType`, `lineSourceType`, `lineSourceCode` (origine d'une ligne, par exemple un abonnement), `searchText`. Renvoie une page de `BillingDocumentResponse` (modèle du module `billing`).

Réponse : `PaymentIntentResponse`. L'écran passe ensuite au choix du moyen : espèces (parcours 2), portefeuille (parcours 3), mobile money ([parcours-mobile-money.md](parcours-mobile-money.md)), lien ([parcours-mobile-money.md](parcours-mobile-money.md#3-envoyer-un-lien-de-paiement-paiement-sans-compte)).

### 2. Encaisser en espèces

Prérequis : une session de caisse `OPEN` sur une caisse ordinaire, ouverte par le caissier (voir [parcours-caisses.md](parcours-caisses.md)). Devise `XAF` uniquement.

1. `PATCH /sni/api/v1/payments/intents/{intentNumber}/cash` ([route](routes/PaymentController.md#r-patch-sni-api-v1-payments-intents-intentnumber-cash)) avec `{ "receivedBy": "<code ou nom du caissier>", "cashSessionNumber": "...", "amount": 15000 }`. `amount` absent : tout le reste payable. `providerReference` facultatif (sinon une référence `PREF-...` est générée).
2. Réponse : `PaymentTransactionResponse` `SUCCEEDED` avec `receiptNumber`. Un mouvement `PAYMENT` est écrit dans la session de caisse.
3. Si l'encaissement dépasse le solde des factures (cas d'une facture déjà réglée entre-temps), le surplus est crédité au portefeuille du client (`OVERPAYMENT_CREDIT`).
4. L'écran propose le reçu (parcours 6) et recharge l'intention.

### 3. Payer avec le portefeuille du client (guichet)

1. Trouver le portefeuille : `GET /sni/api/v1/wallets?ownerType=&ownerCode=` (voir [parcours-portefeuilles.md](parcours-portefeuilles.md)). Afficher `availableBalance`.
2. `PATCH /sni/api/v1/payments/intents/{intentNumber}/wallet` ([route](routes/PaymentController.md#r-patch-sni-api-v1-payments-intents-intentnumber-wallet)) avec `{ "walletNumber": "...", "amount": ... }` (`amount` facultatif). Aucun code secret n'est demandé sur cette route du back-office (le portail, lui, exige le code du titulaire).
3. Contrôles : le titulaire du portefeuille doit être le client de l'intention, même devise, portefeuille `ACTIVE`, solde disponible suffisant.
4. Réponse : transaction `SUCCEEDED` (`provider` = `INTERNAL_WALLET`, `providerReference` = numéro de portefeuille). Un double clic ne débite pas deux fois : l'écriture est indexée par `WALLET_PAYMENT:<intentNumber>` (`PaymentServiceImpl.java:315`). Attention, cette clé rend aussi impossible un second paiement partiel au portefeuille sur la même intention (voir Écarts).

### 4. Recouvrer tout ce que doit un client

1. Aperçu : `GET /sni/api/v1/payments/recovery/{customerType}/{customerCode}?currency=` ([route](routes/PaymentController.md#r-get-sni-api-v1-payments-recovery-customertype-customercode)). Renvoie `openDocumentAmount` (factures ouvertes), `pendingBillableAmount` (éléments facturables pas encore facturés, module `subscription`), `totalPayableAmount`, `documents`, `pendingBillableItems`, `paymentIntent` nul. Si le client doit dans plusieurs devises sans `currency` : `400` « Multiple payable currencies found. Specify a currency to recover payment ».
2. Confirmation : `POST /sni/api/v1/payments/intents/recovery` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-intents-recovery)) avec `{ "customerType", "customerCode", "currency"?, "expiresAt"? }`. **Effet de bord** : les éléments facturables en attente sont d'abord transformés en une facture « Recouvrement <code> », émise aussitôt. Puis une intention `PAYABLE_RECOVERY` est créée sur la somme des factures ouvertes, ou réutilisée si la même liste de factures et le même montant ont déjà une intention non expirée.
3. Réponse : le même aperçu, avec `paymentIntent` renseigné. L'écran enchaîne sur un moyen de paiement ; l'encaissement solde les factures de la plus ancienne à la plus récente.

L'aperçu n'est donc pas neutre à la confirmation : prévenir l'utilisateur qu'une facture va être émise quand `pendingBillableItems` n'est pas vide.

### 5. Rembourser ou annuler une transaction

1. Liste des transactions d'une intention : `GET /sni/api/v1/payments/intents/{intentNumber}/transactions` ([route](routes/PaymentController.md#r-get-sni-api-v1-payments-intents-intentnumber-transactions)), plus récentes d'abord.
2. Déjà remboursé : `GET /sni/api/v1/payments/transactions/{transactionNumber}/refunds` ([route](routes/PaymentController.md#r-get-sni-api-v1-payments-transactions-transactionnumber-refunds)) ; le reste remboursable = montant d'origine - remboursements `REFUNDED`, `REVERSED` ou `PROCESSING`.
3. `POST /sni/api/v1/payments/transactions/{transactionNumber}/refund` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-transactions-transactionnumber-refund), droit `PAYMENT:REFUND`) ou `.../reverse` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-transactions-transactionnumber-reverse), droit `PAYMENT:PROCESS`) avec `{ "reason": "...", "amount": ... }`. `amount` absent : tout le reste remboursable. `processedBy` facultatif.
4. Selon le moyen d'origine :
   - `WALLET` : le portefeuille est recrédité (`REFUND`) immédiatement.
   - `MOBILE_MONEY` : demande envoyée à PawaPay, transaction de remboursement `PROCESSING`, confirmée plus tard ([parcours-mobile-money.md](parcours-mobile-money.md#6-rembourser-un-paiement-mobile-money)).
   - `CASH` et autres : la transaction de remboursement est créée `REFUNDED`/`REVERSED` ; **aucun mouvement de caisse** n'est écrit par cette route : la sortie d'espèces se saisit à part dans la caisse.
   - Dans tous les cas hors mobile money, l'imputation sur la facture est défaite immédiatement (la facture redevient due du montant remboursé).
5. Réponse : la transaction de remboursement. Si le cumul atteint le montant d'origine, l'originale et l'intention passent `REFUNDED` ou `REVERSED`.
6. **Effet automatique fort** : tout remboursement ou annulation, même partiel, déclenche par l'outbox l'annulation et l'archivage des factures liées et l'annulation de l'abonnement, du pass ou de la réservation d'origine (voir [README.md](README.md#effets-automatiques) et Écarts). Demander une confirmation explicite à l'utilisateur.

### 6. Afficher et imprimer un reçu

1. `GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt` ([route](routes/PaymentReceiptController.md#r-get-sni-api-v1-payments-transactions-transactionnumber-receipt)) ou `GET /sni/api/v1/payments/receipts/{receiptNumber}` ([route](routes/PaymentReceiptController.md#r-get-sni-api-v1-payments-receipts-receiptnumber)) : `PaymentReceiptResponse` avec ses imputations par facture.
2. PDF : `.../receipt/pdf` ou `/sni/api/v1/payments/receipts/{receiptNumber}/pdf`, servi `inline` (`application/pdf`, nom `<receiptNumber>.pdf`). La version du back-office contient l'original et son duplicata. Un reçu sans numéro en reçoit un à la première consultation (`PaymentReceiptServiceImpl.java:120`).

### 7. Consulter les intentions

`GET /sni/api/v1/payments/intents` ([route](routes/PaymentController.md#r-get-sni-api-v1-payments-intents)) avec `status`, `customerType`, `customerCode`, `sourceType`, `sourceCode`, `searchText`. Le paramètre `sort` est ignoré (la page est construite sans tri : `PaymentServiceImpl.java:597`) ; l'ordre est celui de la requête native (non vérifié). Fiche : `GET /sni/api/v1/payments/intents/{intentNumber}`.

### 8. Lots de rapprochement

`POST /sni/api/v1/payments/reconciliation-batches` avec `{ "provider": "...", "createdBy"? }` crée un lot `OPEN` ; `PATCH /sni/api/v1/payments/reconciliation-batches/{batchNumber}/complete` le passe `COMPLETED` (`400` « Reconciliation batch is not open » sinon). Aucun calcul n'est fait et aucune route ne liste les lots : ne pas construire d'écran dessus. Le vrai rapprochement mobile money est dans [parcours-mobile-money.md](parcours-mobile-money.md#5-superviser-et-rapprocher-les-paiements-mobile-money-back-office).

## Règles et erreurs à refléter dans l'écran

Messages en anglais (sauf mention), à traduire :

| Cas | Statut | Message |
| --- | --- | --- |
| Montant nul ou négatif | 400 | « Payment amount must be positive » |
| Type de client absent ou illisible | 400 | « Customer type is required » / « Customer code is required » |
| Facture sans solde dû | 400 | « Billing document has no balance due » |
| Plusieurs factures de clients ou devises différents | 400 | « All billing documents must have the same customer and currency » |
| Aucune facture payable dans la liste | 400 | « No payable billing document found » |
| Recouvrement sans rien à payer | 400 | « No payable item found for this customer » / « No payable amount found for this customer » |
| Intention non payable (statut) | 400 | « Payment intent is not payable » |
| Intention expirée | 400 | « Payment intent has expired » |
| Déjà payée | 400 | « Payment intent is already fully paid » |
| Montant au-delà du payable | 400 | « Payment amount cannot exceed remaining payable intent balance » |
| Dépôt mobile money en cours | 400 | « Payment intent has pending mobile money transactions. Wait for their final status before adding another payment » |
| Espèces hors XAF | 400 | « Cash payments are only allowed in XAF » |
| Session de caisse absente | 400 | `VALIDATION_FAILED` sur `cashSessionNumber` (`@NotNull`) ; chaîne vide : « Cash session number is required for cash payments » |
| Session non ouverte ou caisse automatique | 400/409 | messages de la caisse, voir [parcours-caisses.md](parcours-caisses.md) |
| Portefeuille d'un autre client | 400 | « Wallet owner does not match payment intent customer » |
| Devise du portefeuille | 400 | « Wallet currency does not match payment intent currency » |
| Portefeuille non actif | 400 | « Wallet is not active » |
| Solde insuffisant | 400 | « Insufficient wallet balance » |
| Remboursement au-delà du reste | 400 | « Refund amount cannot exceed remaining refundable amount (X) » |
| Déjà totalement remboursée | 409 | « transaction X has already been fully refunded » (ou « reversed ») |
| Motif de remboursement absent | 400 | `VALIDATION_FAILED` sur `reason` |
| Introuvable | 404 | « Payment intent not found », « Payment transaction not found », « Payment receipt not found » |

## À prévoir côté frontend

- **Droits** : lecture `PAYMENT:READ` ; création d'intention, encaissement, lien, annulation (`reverse`) `PAYMENT:PROCESS` ; remboursement et liste des remboursements `PAYMENT:REFUND`.
- **Écrans** : liste des intentions avec filtres ; fiche d'intention (montants `paidAmount`, `processingAmount`, `remainingAmount`, `payableAmount`, transactions, bouton par moyen de paiement, bouton lien) ; écran de recouvrement d'un client (aperçu, puis confirmation) ; écran de remboursement (reste remboursable, motif obligatoire, confirmation des effets) ; visionneuse de reçu PDF.
- **Désactiver les moyens de paiement** quand `payableAmount` vaut 0 et que `processingAmount` est positif (un dépôt est en cours).
- **Pas d'en-tête `Idempotency-Key`** dans ce module. L'idempotence d'une création d'intention se fait par le champ `idempotencyKey` du corps : un UUID par intention de l'utilisateur ; la même clé renvoie l'intention existante. Les encaissements au portefeuille sont protégés par la clé interne `WALLET_PAYMENT:<intentNumber>` ; les encaissements en espèces **ne sont pas protégés** contre le double clic, seul le contrôle du reste payable limite le dégât : bloquer le bouton pendant l'appel.
- **Intention réutilisée** : `from-billing-document(s)` et `recovery` peuvent renvoyer une intention existante ; ne pas supposer qu'elle est neuve.
- **`sort` ignoré** sur les listes du module (pages construites sans tri).

## Écarts relevés

Voir aussi la section commune du [README.md](README.md#écarts-relevés).

- Un remboursement **partiel** publie le même événement qu'un remboursement total, et le traitement asynchrone annule et archive alors les factures liées et annule l'abonnement, le pass ou la réservation d'origine. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentServiceImpl.java:918` et `src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:266`.
- La clé d'idempotence `WALLET_PAYMENT:<intentNumber>` est fixe par intention : un second paiement partiel au portefeuille sur la même intention retombe sur la première écriture (pas de débit) alors qu'une nouvelle transaction `SUCCEEDED` est créée. `PaymentServiceImpl.java:315` ; au rejeu, `WalletLedgerService` renvoie l'écriture d'origine sans débiter (`src/main/java/com/sni/bokaticowork/features/payment/service/support/WalletLedgerService.java:131`).
- `creditOverpayment` du service crédite au portefeuille tout reste non imputé, quelle que soit l'origine ; pour une intention `WALLET_TOPUP` payée en espèces ou au portefeuille depuis le back-office, le montant entier serait crédité en `OVERPAYMENT_CREDIT` en plus du crédit de rechargement (`WalletTopUpService.settle`). Le processeur des rappels mobile money, lui, limite ce crédit aux origines de facturation. `PaymentServiceImpl.java:1067` contre `PawapayCallbackProcessor.java:249` (risque déduit du code, non vérifié en exécution).
- `pendingIntent` passe l'intention `EXPIRED` puis lève une exception dans la même transaction : l'écriture est annulée et seul le worker l'expirera. `PaymentServiceImpl.java:832`.
- Statuts jamais atteints : `AUTHORIZED`, `FAILED`, `CANCELLED`, `PARTIALLY_REFUNDED` (intention), `PENDING`, `CANCELLED` (transaction). `src/main/java/com/sni/bokaticowork/features/payment/enums/PaymentIntentStatus.java`.
- Le remboursement d'une transaction en espèces ne crée aucun mouvement de sortie dans la caisse. `PaymentServiceImpl.java:890`.
- Lots de rapprochement sans contenu ni liste ; le worker quotidien en crée et clôt un vide chaque nuit. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentReconciliationServiceImpl.java` et `worker/PaymentReconciliationWorker.java:17`.
- Relances (`DunningServiceImpl.scheduleForFailedIntent`) jamais appelées : le `DunningWorker` tourne toutes les minutes sur une file toujours vide. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/DunningServiceImpl.java:59`.
- `resolveMemberProcessing` lève une `NullPointerException` si `processedBy` est nul sur un paiement de facture au portefeuille par `POST /sni/api/v1/billing/invoices/{documentNumber}/pay`. `PaymentServiceImpl.java:522`.
