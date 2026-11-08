# payment : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module encaisse et rembourse : il porte les **intentions de paiement** (ce qu'un client doit régler), leurs **transactions** (espèces, portefeuille interne, mobile money PawaPay), les **reçus**, les **caisses** (sessions, mouvements, écarts), les **portefeuilles** électroniques (soldes, retenues, contrôle, conformité) et la **page de paiement publique** d'un lien. Il sert surtout le back-office (guichet, finance, administration) ; le portail membre l'utilise à travers ses propres routes (module `portal`), et deux familles de routes sont publiques (rappels de PawaPay, page `/public/pay/**`). Il travaille avec `billing` (une transaction réussie solde la facture ; un remboursement la rouvre), `subscription`, `booking` et `marketplace` (un paiement réussi active l'abonnement, le pass, la réservation ou la commande), et `notification` (courriels et alertes par l'outbox).

Le module est découpé en quatre fichiers de parcours :

| Fichier | Contenu |
| --- | --- |
| [parcours-paiements.md](parcours-paiements.md) | Intentions (création depuis une ou plusieurs factures, recouvrement global), encaissement en espèces et au portefeuille, remboursement et annulation, reçus, lots de rapprochement. |
| [parcours-mobile-money.md](parcours-mobile-money.md) | Opérateurs, envoi d'un dépôt, attente du rappel et sondage du statut, lien de paiement et page publique, supervision et résolution, remboursement mobile money, idempotence. |
| [parcours-caisses.md](parcours-caisses.md) | Caisses, sessions, mouvements, bons, demandes, clôture et écarts, anomalies, statistiques, annonces d'espèces du portail. |
| [parcours-portefeuilles.md](parcours-portefeuilles.md) | Portefeuilles, écritures, retenues, actions administratives à double visa, signalements, conformité (dossiers, gels, filtrage, règles), code secret, trésorerie. |

## Notions

| Objet | Identifiant exposé | Liens | Détail |
| --- | --- | --- | --- |
| Intention de paiement | `intentNumber` | un client (`customerType` + `customerCode`), une origine (`sourceType` + `sourceCode` : facture, plusieurs factures, recouvrement, abonnement, pass, réservation, rechargement…), 0 à n transactions | [paiements](parcours-paiements.md#notions) |
| Transaction | `transactionNumber` | une intention ; un remboursement pointe sa transaction d'origine ; une transaction en espèces écrit un mouvement de caisse | [paiements](parcours-paiements.md#notions) |
| Reçu | `receiptNumber` | une transaction encaissée, ses imputations sur les factures | [paiements](parcours-paiements.md#6-afficher-et-imprimer-un-reçu) |
| Dépôt mobile money | `depositId` (UUID) | une transaction `MOBILE_MONEY`, une intention, un opérateur, un numéro | [mobile money](parcours-mobile-money.md#notions) |
| Lien de paiement | `token` | une intention ; ouvre `/public/pay/{token}` | [mobile money](parcours-mobile-money.md#3-envoyer-un-lien-de-paiement-paiement-sans-compte) |
| Caisse | `registerCode` | des sessions ; plafond `maxCashAmount` (alerte seulement) ; deux caisses automatiques `CSR-AUTO-WALLET` et `CSR-AUTO-MOBILE-MONEY` journalisent seules les paiements portefeuille et mobile money, toute saisie humaine y est refusée | [caisses](parcours-caisses.md#notions) |
| Session de caisse | `sessionNumber` | une caisse, un caissier ; une seule session `OPEN` ou `CLOSING_REVIEW` par caisse et par caissier ; obligatoire pour encaisser en espèces | [caisses](parcours-caisses.md#notions) |
| Mouvement de caisse | `movementNumber` | une session ; jamais modifié, corrigé par un mouvement inverse ; XAF uniquement | [caisses](parcours-caisses.md#notions) |
| Demande de caisse | `requestNumber` | une session ; son approbation crée le mouvement | [caisses](parcours-caisses.md#notions) |
| Anomalie de caisse | `flagNumber` | une session ; calculée à la clôture ou à la demande | [caisses](parcours-caisses.md#notions) |
| Annonce d'espèces | `declarationNumber` | une facture du membre ; confirmée par la caisse sur sa session ouverte, ou expirée après 4 h | [caisses](parcours-caisses.md#notions) |
| Portefeuille | `walletNumber` | un titulaire et une devise ; écritures, retenues | [portefeuilles](parcours-portefeuilles.md#notions) |
| Retenue | `holdNumber` | un portefeuille, une opération (`sourceType` + `sourceCode`) | [portefeuilles](parcours-portefeuilles.md#notions) |
| Action administrative, signalement, dossier, gel, contrôle de filtrage, règle | `actionNumber`, `flagNumber`, `caseNumber`, `freezeNumber`, `checkNumber`, `ruleCode` | un portefeuille | [portefeuilles](parcours-portefeuilles.md#notions) |

Toutes les routes du module désignent les objets par ces codes lisibles. Les seules réponses qui exposent un `id` int64 sont les entités renvoyées telles quelles (rapprochements de trésorerie, piste d'audit de conformité).

Monnaie : montants en `BigDecimal` à 4 décimales. Espèces en `XAF` uniquement ; mobile money dans la devise de l'opérateur, en montant entier.

## Statuts et cycle de vie

Résumé ; les tables complètes (statut, signification, entrée, sortie) sont dans chaque fichier de parcours.

| Objet | Statuts réellement atteints | Table complète |
| --- | --- | --- |
| Intention | `PENDING` → `PROCESSING` (dépôt en cours) → `SUCCEEDED` ; `EXPIRED` (échéance, worker 5 min) ; `REFUNDED` / `REVERSED` (remboursement total) | [paiements](parcours-paiements.md#intention-paymentintentstatus) |
| Transaction | `PROCESSING` → `SUCCEEDED` / `FAILED` ; `REFUNDED` / `REVERSED` | [paiements](parcours-paiements.md#transaction-paymenttransactionstatus) |
| Dépôt mobile money (`phase`) | `WAITING_FOR_PAYER`, `CONFIRMING` → `COMPLETED` / `FAILED` / `UNRESOLVED` (24 h sans réponse) | [mobile money](parcours-mobile-money.md#dépôt--status-technique-et-phase-pour-lécran) |
| Session de caisse | `OPEN` → `CLOSED`, ou `OPEN` → `CLOSING_REVIEW` (écart) → `CLOSED` | [caisses](parcours-caisses.md#session-cashsessionstatus) |
| Demande de caisse | `PENDING` → `EXECUTED` / `REJECTED` | [caisses](parcours-caisses.md#demande-de-caisse-cashrequeststatus) |
| Annonce d'espèces | `AWAITING_CONFIRMATION` → `CONFIRMED` / `CANCELLED` / `EXPIRED` | [caisses](parcours-caisses.md#annonce-despèces-cashdeclarationstatus) |
| Portefeuille | `ACTIVE`, `SUSPENDED`, `UNDER_REVIEW`, `LOCKED`, `CLOSED` | [portefeuilles](parcours-portefeuilles.md#portefeuille-walletstatus) |
| Retenue | `ACTIVE` → `CAPTURED` / `RELEASED` / `EXPIRED` | [portefeuilles](parcours-portefeuilles.md#retenue-walletholdstatus) |
| Action administrative | `PENDING_APPROVAL` → `EXECUTED` / `REJECTED`, ou `EXECUTED` directement | [portefeuilles](parcours-portefeuilles.md#action-administrative) |

## Parcours

Table des matières des parcours détaillés :

- Paiements : [créer une intention](parcours-paiements.md#1-créer-une-intention-de-paiement), [encaisser en espèces](parcours-paiements.md#2-encaisser-en-espèces), [payer au portefeuille](parcours-paiements.md#3-payer-avec-le-portefeuille-du-client-guichet), [recouvrer tout ce que doit un client](parcours-paiements.md#4-recouvrer-tout-ce-que-doit-un-client), [rembourser ou annuler](parcours-paiements.md#5-rembourser-ou-annuler-une-transaction), [reçus](parcours-paiements.md#6-afficher-et-imprimer-un-reçu), [consulter les intentions](parcours-paiements.md#7-consulter-les-intentions), [lots de rapprochement](parcours-paiements.md#8-lots-de-rapprochement).
- Mobile money : [encaisser par mobile money](parcours-mobile-money.md#1-encaisser-une-intention-par-mobile-money-depuis-le-back-office), [attendre le rappel et sonder le statut](parcours-mobile-money.md#2-attendre-le-rappel-et-sonder-le-statut-écran-dattente), [lien de paiement](parcours-mobile-money.md#3-envoyer-un-lien-de-paiement-paiement-sans-compte), [page publique](parcours-mobile-money.md#4-la-page-publique-publicpaytoken-servie-par-le-serveur), [supervision et résolution](parcours-mobile-money.md#5-superviser-et-rapprocher-les-paiements-mobile-money-back-office), [remboursement](parcours-mobile-money.md#6-rembourser-un-paiement-mobile-money), [idempotence](parcours-mobile-money.md#idempotence).
- Caisses : voir [parcours-caisses.md](parcours-caisses.md#parcours) (12 parcours, de la création de caisse aux annonces d'espèces).
- Portefeuilles : [consulter](parcours-portefeuilles.md#1-retrouver-ouvrir-et-consulter-un-portefeuille), [créditer ou débiter](parcours-portefeuilles.md#2-créditer-ou-débiter-un-portefeuille), [retenues](parcours-portefeuilles.md#3-bloquer-puis-prélever-ou-rendre-des-fonds-retenues), [double visa](parcours-portefeuilles.md#4-double-visa-des-actions-administratives), [signalements](parcours-portefeuilles.md#5-traiter-les-signalements-de-risque), [dossiers de conformité](parcours-portefeuilles.md#6-dossiers-de-conformité), [gel, filtrage, règles, audit](parcours-portefeuilles.md#7-gel-sur-instruction-filtrage-règles-audit), [code secret et tableau de bord](parcours-portefeuilles.md#8-code-secret-et-tableau-de-bord-du-contrôle).

Enchaînement type au guichet : facture → `POST /sni/api/v1/payments/intents/from-billing-document` → choix du moyen → `PATCH /sni/api/v1/payments/intents/{intentNumber}/cash` (session de caisse ouverte), `.../wallet` ou `.../mobile-money` (puis attente) → reçu PDF.

## Règles et erreurs à refléter dans l'écran

Règles transverses (le détail est dans chaque fichier) :

- **Échéance par défaut de 15 minutes** sur toute intention créée sans `expiresAt` (`src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentServiceImpl.java:1092`). Une intention expirée n'est plus payable (`400` « Payment intent has expired » ou « Payment intent is not payable »).
- **Reste payable** : on ne peut encaisser que `payableAmount` (reste dû moins ce qui attend un opérateur). Tant qu'un dépôt mobile money est en cours pour tout le reste, tout autre encaissement est refusé (`400` « Payment intent has pending mobile money transactions... »).
- **Espèces** : `XAF` seulement, session de caisse `OPEN` obligatoire.
- **Portefeuille** : même titulaire et même devise que l'intention, statut `ACTIVE`, solde disponible suffisant.
- **Mobile money** : montant entier, numéro du pays de l'opérateur, opérateur activé ; l'échec d'un envoi revient en `200` avec `phase` = `FAILED`.
- **Remboursement** : motif obligatoire, au plus le reste remboursable ; un remboursement mobile money est asynchrone.
- **Langue des messages** : les contrôles récents (mobile money, caisses, portefeuilles, conformité) renvoient des messages français affichables ; ceux des intentions et transactions sont en anglais et doivent être traduits par l'écran (liste dans [parcours-paiements.md](parcours-paiements.md#règles-et-erreurs-à-refléter-dans-lécran)).

## À prévoir côté frontend

- **Droits du module** : `PAYMENT:READ`, `PAYMENT:PROCESS`, `PAYMENT:REFUND`, `CASH:READ`, `CASH:ADJUST`, `CASH:CLOSE_SESSION`. Plusieurs contrôleurs ajoutent une condition de **rôle** (`@PreAuthorize`) que le droit seul ne révèle pas :
  - suivi et supervision mobile money : `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` ou `ROLE_MANAGER` ; forcer l'issue d'un dépôt : `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` ;
  - centre de contrôle des portefeuilles, conformité, intégrité, plafonds, réinitialisation du code secret : `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN`.
  Masquer ces écrans selon la liste `roles` reçue à la connexion, qui contient rôles et droits.
- **Écrans principaux** : fiche d'intention et choix du moyen de paiement ; écran d'attente mobile money ; génération de lien ; recouvrement d'un client ; remboursement ; caisse du caissier (session, mouvements, clôture) et supervision des caisses ; recherche et fiche de portefeuille ; centre de contrôle ; conformité ; supervision mobile money.
- **Listes de référence** : `GET /sni/api/v1/payments/mobile-money/providers` (publique) ; `GET /sni/api/v1/cash-registers` (caisses).
- **Fichiers** : reçus PDF, rapports de clôture et documents de caisse PDF, export comptable CSV des portefeuilles, export JSON d'un dossier de conformité.
- **Idempotence** : aucune route du module ne lit l'en-tête `Idempotency-Key`. La création d'intention accepte un champ `idempotencyKey` dans le corps ; le reste est protégé côté métier (voir [mobile money](parcours-mobile-money.md#idempotence) et [paiements](parcours-paiements.md#à-prévoir-côté-frontend)). Bloquer les boutons d'encaissement pendant l'appel.
- **Temps réel** : aucun sujet WebSocket propre au module ; le suivi d'un dépôt se fait par sondage.
- **Tri** : la plupart des listes du module construisent leur page sans tri ; le paramètre `sort` y est sans effet.

## Effets automatiques

| Automatisme | Fréquence | Effet visible |
| --- | --- | --- |
| Suite d'un paiement réussi (événement outbox `PAYMENT_TRANSACTION_WORKFLOW`, traité toutes les 10 s) | après chaque encaissement | Crédit du rechargement de portefeuille ; mouvement dans la caisse automatique (portefeuille, mobile money) ; validation des factures soldées ; activation de l'abonnement (ou sortie de la période de tolérance, recalage d'une échéance payée en retard), du pass, du renouvellement de pass ; confirmation de la réservation entièrement payée ; synchronisation de la commande de la place de marché ; courriel de confirmation de paiement de facture. `src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:63` |
| Suite d'un remboursement ou d'une annulation (même événement) | après chaque remboursement | Annulation et archivage des factures liées ; annulation de l'abonnement, du pass ou de la réservation d'origine et clôture du contrat ; courriel de remboursement. Les annulations n'ont lieu qu'après un remboursement **total** ; un remboursement partiel n'envoie que le courriel (corrigé le 2026-11-08). |
| Imputation en échec | à l'encaissement mobile money | Rattrapage par l'événement outbox `PAYMENT_ALLOCATION_RETRY`. |
| `PaymentIntentExpiryWorker` | toutes les 5 min | Intentions `PENDING`, `PROCESSING` ou `AUTHORIZED` échues → `EXPIRED`. |
| `PaymentIntentExpiryAlertWorker` | toutes les 30 min | Courriel de rappel au client 2 h avant l'échéance, une seule fois, pour les intentions d'une seule facture dont le client a une adresse. |
| Relecture des dépôts mobile money | toutes les 60 s | Voir [mobile money](parcours-mobile-money.md#effets-automatiques). |
| Rapprochement mobile money quotidien | 06:00 (Brazzaville) | Voir [mobile money](parcours-mobile-money.md#effets-automatiques). |
| `PaymentReconciliationWorker` | 02:30 | Crée et clôt un lot de rapprochement vide (sans effet visible). |
| `DunningWorker` | toutes les 60 s | Exécute les relances échues ; aucune n'est jamais planifiée (voir Écarts). |
| Caisses | fin de mois 00:05, annonces toutes les 15 min, clôture | Voir [caisses](parcours-caisses.md#effets-automatiques). |
| Portefeuilles et conformité | 03:15 à 08:00, toutes les 5 à 10 min | Voir [portefeuilles](parcours-portefeuilles.md#effets-automatiques). |
| Paiement en libre-service | à la confirmation | Notification `SELF_SERVICE_PAYMENT_RECEIVED` dans le portail d'administration et courriel à la caisse. |

## Écarts relevés

Les plus importants pour un frontend ; chaque fichier de parcours a sa propre liste complète.

- **Corrigé le 2026-11-08** : un remboursement partiel ne déclenche plus les annulations. Constat d'origine : un remboursement **partiel** déclenchait la même suite qu'un remboursement total : factures liées annulées et archivées, abonnement, pass ou réservation annulés. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentServiceImpl.java:918`, `src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:266`.
- Le dédoublonnage d'un dépôt mobile money en cours est précédé par le contrôle du reste payable : un double envoi reçoit `400` au lieu du dépôt existant. `PaymentServiceImpl.java:331` et `:337`.
- Un lien de paiement vaut 24 h mais l'intention sous-jacente expire au bout de 15 minutes si elle a été créée sans `expiresAt`. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentLinkServiceImpl.java:59`.
- Clôture de session de caisse possible avec `CASH:ADJUST` par `POST .../closing-request`, qui fait la même chose que `PATCH .../close` (`CASH:CLOSE_SESSION`). `src/main/java/com/sni/bokaticowork/features/payment/controller/CashRegisterController.java:87`.
- Auteurs en texte libre dans le corps (`openedBy`, `closedBy`, `reviewedBy`, `receivedBy`, `createdBy`) sur les caisses, les paiements, les recharges et les retenues : rien n'empêche un caissier de valider son propre écart. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/CashRegisterServiceImpl.java:142`.
- `POST /sni/api/v1/wallets/admin/top-up` crédite sans le double visa que le centre de contrôle impose au même geste au-delà de 100 000. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/WalletServiceImpl.java:33`.
- Second paiement partiel au portefeuille sur une même intention : transaction `SUCCEEDED` créée sans débit (clé d'idempotence fixe par intention). `PaymentServiceImpl.java:315`.
- Le portail membre reçoit `requestPayloadJson`, `providerResponseJson` et `callbackUrl` d'un dépôt. `src/main/java/com/sni/bokaticowork/features/payment/service/pawaypay/PawapayDepositService.java:316`.
- Statuts déclarés jamais atteints : intention `AUTHORIZED`, `FAILED`, `CANCELLED`, `PARTIALLY_REFUNDED` ; transaction `PENDING`, `CANCELLED` ; session `SUSPENDED`, `CANCELLED`.
- Code mort côté écran : lots de rapprochement sans contenu ni liste, relances jamais planifiées (`DunningServiceImpl.java:59`), retour de page hébergée PawaPay (`GET /sni/api/v1/payments/mobile-money/pawapay/return`).
