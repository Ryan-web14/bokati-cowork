# portal : factures, paiements, reçus, état de compte

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientBillingController](routes/ClientBillingController.md). Fonctionnement de fond : factures et statuts dans [../billing/README.md](../billing/README.md), intentions de paiement, mobile money, annonces d'espèces et reçus dans [../payment/README.md](../payment/README.md), états de compte dans [../company/README.md](../company/README.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/billing/`.

## Ce qui est propre au portail

- **Périmètre** : factures, paiements, reçus, dépôts et annonces dont le client est le membre (`customerType = MEMBER`, `customerCode = memberId`). Une facture reliée à une organisation mais émise au nom du membre reste visible ici ; une facture émise au nom de l'organisation n'est visible que du gérant ([parcours-organisation.md](parcours-organisation.md)).
- **Brouillons invisibles** : une facture `DRAFT` n'est ni listée, ni lisible (`404` « Invoice not found »), ni comptée dans le résumé.
- **Espèces = annonce** : le membre ne paie pas en espèces depuis le portail, il **annonce** qu'il passera payer. La facture reste due jusqu'à l'encaissement en caisse.
- **Portefeuille** : le paiement prend le portefeuille du membre dans la devise de la facture, créé s'il n'existe pas ; le code secret n'est exigé que si la politique le demande (`MERCHANT_PAYMENT` pour une facture payée par cette route).

## Parcours

### Tableau de bord financier

1. `GET /sni/api/v1/client/billing/summary` : `totalInvoiced`, `totalPaid`, `totalBalanceDue`, `totalCreditAvailable`, `netBalanceDue`, `invoiceCount` (factures émises), `unpaidCount`, `overdueCount`.
2. `GET /sni/api/v1/client/billing/invoices/payable` (page, tri `dueDate` croissant) : les factures à régler, pour un bloc « à payer ».

### Lister et lire ses factures

1. `GET /sni/api/v1/client/billing/invoices?status=...&from=AAAA-MM-JJ&to=AAAA-MM-JJ` (page, tri `issueDate` décroissant). Chaque facture : `documentNumber`, `documentType`, `status`, `title`, `currency`, `totalAmount`, `paidAmount`, `balanceDue`, `issueDate`, `dueDate`, `paidAt`, `sourceType` et `sourceCode` (ce que la facture règle : abonnement, pass, réservation...), `resolvedSourceLabel`. Les brouillons étant retirés après la pagination, une page peut compter moins d'éléments que `size`.
2. Détail : `GET /sni/api/v1/client/billing/invoices/{documentNumber}` : en plus `lines`, `subtotalAmount`, `discountAmount`, `vatAmount`, `paymentReference`, `paymentInstructions`, `bankDetailsJson`.
3. PDF : `GET .../invoices/{documentNumber}/pdf` (fichier `invoice-<numéro>.pdf`).

### Payer une facture

Afficher les trois moyens quand `balanceDue > 0`.

**Mobile money**

1. `POST /sni/api/v1/client/billing/invoices/{documentNumber}/pay/mobile-money` avec `{ "phoneNumber", "correspondent" }` (les deux obligatoires). Le numéro peut être saisi au format local : le serveur complète l'indicatif selon l'opérateur.
2. Réponse : le dépôt (`depositId`, `phase`, `userMessage`, `retryable`, `nextStatusCheckAt`).
3. Suivi : `GET /sni/api/v1/client/billing/mobile-money/deposits/{depositId}` jusqu'à `phase` `COMPLETED` ou `FAILED` (ou `UNRESOLVED` : sans réponse définitive de l'opérateur, le serveur continue de vérifier). Afficher `userMessage` ; sur `FAILED` avec `retryable = true`, proposer de réessayer.
4. Un dépôt qui n'est pas au membre répond `404` « Paiement mobile money introuvable : ... », sans distinguer l'inexistant de celui d'un autre.

**Portefeuille**

1. `POST .../invoices/{documentNumber}/pay/wallet`, corps facultatif `{ "pin" }`.
2. Réponse : la transaction (`PaymentTransactionResponse`). Paiement immédiat ; relire la facture.

**Espèces** : voir [ci-dessous](#annoncer-un-paiement-en-espèces).

Le paiement d'une facture d'abonnement ou de pass peut aussi passer par les routes de l'abonnement ou du pass, qui choisissent la facture elles-mêmes ([parcours-abonnements-pass.md](parcours-abonnements-pass.md#payer-un-abonnement-ou-un-pass)).

### Annoncer un paiement en espèces

1. `POST /sni/api/v1/client/billing/invoices/{documentNumber}/pay/cash` avec `{ "amount" (facultatif : le solde), "note" (500 caractères au plus) }`, corps facultatif. Pour une réservation, la route dédiée est `POST /sni/api/v1/client/bookings/{bookingNumber}/pay/cash`.
2. Réponse : l'annonce (`declarationNumber`, statut `AWAITING_CONFIRMATION`, montant, échéance). Validité : 4 heures par défaut.
3. Mes annonces : `GET /sni/api/v1/client/billing/cash-declarations` (page). Celles `AWAITING_CONFIRMATION` sont « à aller régler ».
4. Se raviser : `POST /sni/api/v1/client/billing/cash-declarations/{declarationNumber}/cancel`. La facture reste due.

Statuts d'une annonce : `AWAITING_CONFIRMATION` (la caisse attend), `CONFIRMED` (encaissée, un paiement en espèces existe), `CANCELLED` (par le membre ou la caisse), `EXPIRED` (délai passé, worker toutes les 15 minutes).

Règles (module `payment`, `CashPaymentDeclarationService`) : montant positif et au plus égal au solde ; facture non soldée et dans un état payable ; espèces acceptées **en XAF seulement** ; une annonce déjà close ne s'annule plus.

### Paiements et reçus

1. Historique : `GET /sni/api/v1/client/billing/payments` (page, tri `createdAt` décroissant) : les intentions de paiement du membre (`intentNumber`, statut, montant, source...).
2. Reçu : `GET /sni/api/v1/client/billing/payments/{transactionNumber}/receipt` ; PDF : `.../receipt/pdf` (fichier `receipt-<numéro>.pdf`). `404` « Receipt not found » si le reçu n'est pas au membre.

### État de compte

1. `GET /sni/api/v1/client/billing/statement?from=AAAA-MM-JJ&to=AAAA-MM-JJ` : par défaut les douze derniers mois jusqu'à aujourd'hui ; trois ans au plus. Les lignes sont réparties entre le compte personnel du membre et chaque organisation à laquelle une facture est reliée (`accounts`), avec les totaux.
2. PDF : `GET /sni/api/v1/client/billing/statement.pdf` avec les mêmes paramètres.

Erreurs `400` : « La période se termine avant de commencer. », « Période trop longue : trois ans au plus par état de compte. »

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | Message |
| --- | --- | --- |
| Facture d'un autre ou brouillon | `404` | `Invoice not found` |
| Corps mobile money incomplet | `400` | `VALIDATION_FAILED`, champ `phoneNumber` ou `correspondent` |
| Portefeuille verrouillé par le membre | `409` | `votre portefeuille est verrouillé · déverrouillez-le pour payer` |
| Portefeuille non débitable | `409` | `votre portefeuille ne peut pas payer pour le moment` |
| Code exigé, non envoyé | `400` | `Ce paiement exige votre code secret` |
| Montant annoncé supérieur au solde | `400` | `Le montant annoncé dépasse le solde de la facture (...)` |
| Facture déjà soldée | `400` | `La facture ... est déjà soldée.` |
| Facture hors XAF en espèces | `400` | `Les paiements en espèces ne sont acceptés qu'en XAF.` |
| Annonce déjà close | `400` | `L'annonce ... est déjà ...` |

## À prévoir côté frontend

- Écrans : résumé, liste des factures avec filtres, détail avec PDF et boutons de paiement, suivi d'un paiement mobile money (écran d'attente avec interrogation périodique), annonces d'espèces, historique des paiements et reçus, état de compte (et son PDF).
- Liste des opérateurs : n'afficher que ceux du pays de l'établissement (XAF Congo : `MTN_MOMO_COG`, `AIRTEL_COG`), sans les alias de compatibilité.
- Désactiver le bouton pendant l'appel : aucune idempotence sur `pay/*` ; un second clic crée une seconde intention ou un second dépôt.
- Ne pas afficher les champs techniques du dépôt (`requestPayloadJson`, `providerResponseJson`, `callbackUrl`) même s'ils arrivent.
- Les PDF se téléchargent avec le jeton (requête authentifiée puis `Blob`).
