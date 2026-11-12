# portal : portefeuille personnel

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientWalletController](routes/ClientWalletController.md), [ClientWalletSecurityController](routes/ClientWalletSecurityController.md), [ClientWalletOperationsController](routes/ClientWalletOperationsController.md). Fonctionnement de fond (écritures, blocages, plafonds par niveau KYC, frais de transfert, politique de code secret, dormance, conformité) : [../payment/README.md](../payment/README.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/`.

## Ce qui est propre au portail

- **Périmètre** : chaque route commence par retrouver le portefeuille par `walletNumber` et vérifie qu'il appartient au membre (`ownerType = MEMBER`, `ownerCode = memberId`). Sinon `404` « Portefeuille introuvable » (ou « Wallet not found » sur les routes de lecture), jamais `403`.
- **Création** : le portail n'a aucune route de création. Le portefeuille XAF du membre est créé à l'approbation de son KYC, et un portefeuille est aussi créé d'office au premier paiement par portefeuille dans une devise. Tant qu'il n'existe pas, `GET /sni/api/v1/client/wallet` rend une liste vide : l'écran ne peut alors ni recharger ni transférer.
- **En-tête `X-Device-Id`** : lu par `POST .../transfers/simulate`, `POST .../transfers`, `POST .../lock` et `POST .../unlock` (`src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletOperationsController.java:77`). Facultatif : sans lui, l'opération passe et est notée comme venant d'un appareil anonyme. Le frontend web génère un identifiant stable par navigateur (par exemple un UUID gardé en stockage local) et l'envoie sur ces routes ; les appareils vus apparaissent ensuite dans `GET .../devices`.
- **Adresse IP** : relevée côté serveur (`X-Forwarded-For`, sinon adresse distante) pour les transferts et le verrou.
- **Destinataire** : désigné par un numéro de portefeuille, un téléphone ou une adresse électronique d'abonné ; la réponse ne rend qu'un nom d'affichage (`counterpartyName`), jamais les coordonnées.

## Parcours

### Voir son portefeuille

1. `GET /sni/api/v1/client/wallet` (page) : les portefeuilles du membre (un par devise). Retenir `walletNumber`.
2. `GET /sni/api/v1/client/wallet/{walletNumber}` : solde et état (modèle `WalletResponse`, voir le fichier de routes).
3. Mouvements : `GET .../{walletNumber}/ledger` (page de 30, tri `createdAt` décroissant).
4. Reçu PDF d'un mouvement : `GET .../{walletNumber}/ledger/{transactionNumber}/receipt` (fichier `recu-<numéro>.pdf`).
5. Relevé PDF : `GET .../{walletNumber}/statement?from=AAAA-MM-JJ&to=AAAA-MM-JJ` (les deux obligatoires, fichier `releve-<portefeuille>-<du>-<au>.pdf`).
6. État du titulaire : `GET .../{walletNumber}/state` : `lockedByOwner`, `lockedAt`, `frozen` (gel par l'établissement), `lowBalanceThreshold`, `notifyOnCredit`, `notifyOnDebit`.

### Code secret et plafonds

1. `GET /sni/api/v1/client/wallet/{walletNumber}/security` : `pinDefined`, `pinMustChange`, `pinExpired`, `locked` (trop d'essais), `lockedForMinutes`, `pinLength`, `operationsRequiringPin` (par exemple `TRANSFER`, `BILL_PAYMENT`, `MERCHANT_PAYMENT`), `limits` (plafonds et consommation selon le niveau KYC). À lire **avant** de proposer une opération : demander le code d'emblée si l'opération figure dans `operationsRequiringPin`, afficher les plafonds restants.
2. Poser ou changer le code : `POST .../security/pin` avec `{ "currentPin", "newPin" }`. `currentPin` est vide à la première pose et après une réinitialisation par l'établissement. Réponse : l'état de sécurité à jour.
3. Si `pinMustChange` ou `pinExpired` : imposer l'écran de changement avant toute opération.

### Recharger par mobile money

1. `POST /sni/api/v1/client/wallet/{walletNumber}/top-ups` avec `{ "amount", "phoneNumber", "correspondent" }` (tous obligatoires).
2. Réponse : le dépôt (`depositId`, `phase`, `userMessage`...). Afficher « validez sur votre téléphone ».
3. Suivi : `GET /sni/api/v1/client/billing/mobile-money/deposits/{depositId}` jusqu'à `COMPLETED` ou `FAILED` (non vérifié : le dépôt de recharge porte le membre comme client, condition de cette route ; d'après `WalletTopUpService`, l'intention est créée au nom du titulaire du portefeuille).
4. À `COMPLETED`, relire le solde. La recharge produit un reçu, pas une facture.

### Envoyer de l'argent à un autre abonné

1. Simuler : `POST .../{walletNumber}/transfers/simulate` avec `{ "counterparty", "amount", "message", "paymentRequestNumber" }` et l'en-tête `X-Device-Id`. Réponse : destinataire (`counterpartyWallet`, `counterpartyName`), frais, total débité, contrôles. Rien n'est enregistré.
2. Lancer : `POST .../{walletNumber}/transfers` (même corps, même en-tête). Réponse `InitiatedTransferView` : `transfer` (statut `PENDING_CONFIRMATION`, `transferNumber`, `expiresAt`), `confirmationCode`, `challenge` (`PIN`, `OTP` ou `PIN_AND_OTP`), `confirmationExpiresAt`, `maxAttempts`.
3. Demander le code secret, puis confirmer : `POST .../{walletNumber}/transfers/{transferNumber}/confirm` avec `{ "pin" }`. La confirmation vérifie toujours le **code secret** du portefeuille (`WalletConfirmationService.confirm`) ; aucune route ne reçoit d'OTP. Réponse : le transfert `COMPLETED`, ou `FAILED` avec `failureReason` (plafond ou solde au moment de l'exécution).
4. Abandonner avant confirmation : `POST .../transfers/{transferNumber}/cancel`.
5. Historique : `GET .../{walletNumber}/transfers` (page) et `GET .../transfers/{transferNumber}`. `direction` vaut `OUT` ou `IN` ; `feeAmount` et `totalDebit` ne concernent que l'émetteur.

Statuts : `PENDING_CONFIRMATION` (attend le code), `COMPLETED`, `FAILED` (confirmation refusée, plafond ou solde), `CANCELLED` (abandonné, ou jamais confirmé et fermé par le serveur).

### Bénéficiaires enregistrés

1. `GET .../{walletNumber}/beneficiaries` : `id`, `alias`, `walletNumber`, `name`, `transferCount`, `lastUsedAt`.
2. Ajouter : `POST .../beneficiaries` avec `{ "counterparty", "alias" }` (les deux obligatoires).
3. Retirer : `DELETE .../beneficiaries/{id}` (`204`). **`id` est un grand entier généré** : le garder en texte.

### Demandes de paiement

1. Demander de l'argent : `POST .../{walletNumber}/payment-requests` avec `{ "payer", "amount", "reason" }` (`payer` : numéro de portefeuille, téléphone ou adresse). Réponse : `requestNumber`, `status` `PENDING`, `expiresAt`.
2. Liste (reçues et émises) : `GET .../payment-requests` (page) ; `direction` dit le sens.
3. Payer une demande reçue : lancer un transfert avec `paymentRequestNumber` et **exactement** le montant demandé, puis confirmer (parcours ci-dessus). La demande passe `PAID`.
4. Refuser une demande reçue : `POST .../payment-requests/{requestNumber}/decline`. Annuler une demande émise : `POST .../payment-requests/{requestNumber}/cancel`.

Statuts : `PENDING`, `PAID`, `DECLINED`, `CANCELLED`, `EXPIRED`.

### Verrouiller, déverrouiller, préférences

1. Verrouiller (perte de téléphone) : `POST .../{walletNumber}/lock`, **sans code**, avec `X-Device-Id`. Un portefeuille verrouillé ne paie plus rien, factures comprises, et n'émet plus de transfert.
2. Déverrouiller : `POST .../unlock` avec `{ "pin" }` si un code existe (corps facultatif sinon).
3. Préférences : `PUT .../preferences` avec `{ "lowBalanceThreshold", "notifyOnCredit", "notifyOnDebit" }`.

### Appareils

1. `GET .../{walletNumber}/devices` : `id`, `deviceId` (valeur envoyée dans `X-Device-Id`), `label`, `firstSeenAt`, `lastSeenAt`, `lastIpAddress`, `useCount`, `trusted`, `revokedAt`.
2. Renommer ou marquer de confiance : `PATCH .../devices/{id}` avec `{ "label", "trusted" }` (chacun facultatif).
3. Révoquer : `POST .../devices/{id}/revoke`.

`id` d'un appareil est un grand entier généré : le garder en texte.

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | Message |
| --- | --- | --- |
| Destinataire vide | `400` | `Indiquez le destinataire · numéro de portefeuille, téléphone ou courriel` |
| Destinataire qui n'est pas un portefeuille d'abonné | `400` | `Ce portefeuille n'est pas celui d'un abonné` |
| Montant nul ou négatif | `400` | `Le montant doit être positif` |
| Transfert vers soi-même | `400` | `Un portefeuille ne peut pas se transférer à lui-même` |
| Devises différentes | `400` | `Les deux portefeuilles n'ont pas la même devise` |
| Portefeuille verrouillé par le titulaire | `409` | `votre portefeuille est verrouillé · déverrouillez-le pour envoyer` |
| Portefeuille non émetteur, destinataire non receveur | `409` | `votre portefeuille ne peut pas émettre pour le moment`, `le portefeuille du destinataire ne peut pas recevoir pour le moment` |
| Solde insuffisant | `409` | `solde disponible insuffisant` |
| Confirmation hors délai | `400` | `Le délai de confirmation est dépassé, recommencez le transfert` |
| Trop d'essais de code | `400` | `Trop de tentatives, recommencez l'opération` |
| Transfert déjà confirmé ou annulé | `400` | `Ce transfert n'est plus en attente de confirmation` |
| Montant différent de la demande payée | `400` | `Le montant doit être celui de la demande · ...` |
| Demande close | `400` | `Cette demande de paiement n'est plus ouverte` |

Les plafonds, frais et refus de conformité ont leurs propres messages, décrits dans [../payment/README.md](../payment/README.md).

## À prévoir côté frontend

- Écrans : solde et mouvements, sécurité (code, plafonds), recharge avec écran d'attente, transfert en trois temps (saisie, simulation, code), bénéficiaires, demandes de paiement, verrou et préférences, appareils, relevés.
- Générer et conserver un identifiant d'appareil, l'envoyer en `X-Device-Id` sur les quatre routes qui le lisent.
- Lire `security` avant chaque opération sensible ; afficher `limits`.
- Garder en texte les `id` de bénéficiaire et d'appareil.
- Pas d'idempotence sur `top-ups` et `transfers` : désactiver le bouton pendant l'appel ; un transfert non confirmé est fermé par le serveur à son échéance.
