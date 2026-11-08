# payment : parcours mobile money (PawaPay)

> Rédigé d'après le code. Point d'entrée du module : [README.md](README.md). Routes exactes : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Le mobile money passe par l'agrégateur PawaPay. Un paiement mobile money est toujours le règlement d'une **intention de paiement** (voir [parcours-paiements.md](parcours-paiements.md)) : on n'envoie jamais un dépôt sans intention.

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Opérateur (`CongoCorrespondent`) | code d'énumération (`MTN_MOMO_COG`, `AIRTEL_COG`…) | Fixe le pays, la devise et l'indicatif. Liste complète : `src/main/java/com/sni/bokaticowork/features/payment/provider/pawaypay/CongoCorrespondent.java`. |
| Dépôt (`PawapayDeposit`) | `depositId` (UUID texte) | La demande envoyée sur le téléphone du payeur. Une transaction `MOBILE_MONEY` lui correspond (`transactionNumber`, aussi envoyé à PawaPay comme `clientReferenceId`). |
| Transaction mobile money | `transactionNumber` (`TXN-...`) | Créée `PROCESSING` à l'envoi, passe `SUCCEEDED` ou `FAILED` quand l'opérateur tranche. Porte le `receiptNumber` une fois encaissée. |
| Rappel (`PawapayCallback`) | aucun identifiant exposé utile | Trace de chaque rappel reçu de PawaPay et de ce qu'on en a fait (`outcome`). |
| Lien de paiement | `token` (32 caractères hexadécimaux) | Donne accès, sans compte, à la page de paiement servie par le serveur pour **une** intention. |

Opérateurs : seuls ceux dont `providerCode` n'est pas un alias sont proposés (`AIRTEL_OAPI_COG`, `AIRTEL_OAPI_COD`, `MPESA_COD` sont des alias acceptés en entrée mais jamais listés), et seulement ceux activés par la configuration `bokati.payment.pawaypay.enabled-providers` (`src/main/java/com/sni/bokaticowork/features/payment/service/pawaypay/PawapayDepositService.java:300`). Valeur par défaut en dev et en prod : `MTN_MOMO_COG, AIRTEL_COG, AIRTEL_GAB, MTN_MOMO_CMR, MTN_MOMO_CIV, ORANGE_CIV, FREE_SEN, ORANGE_SEN, AIRTEL_COD, ORANGE_COD, VODACOM_MPESA_COD` (`src/main/resources/application-prod.yml`). Devises : `XAF` pour Congo, Gabon, Cameroun ; `XOF` pour Côte d'Ivoire et Sénégal ; `CDF` pour la RD Congo.

## Statuts et cycle de vie

### Dépôt : `status` (technique) et `phase` (pour l'écran)

Le champ `phase` de `MobileMoneyDepositResponse` est calculé pour l'affichage (`PawapayDepositService.java:362`). L'écran doit raisonner sur `phase`, et afficher `userMessage` (phrase en français écrite pour le payeur). Ne jamais afficher `providerMessage`, `failureReason` ni `providerResponseJson` à un payeur.

| `status` | `phase` | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- | --- |
| `CREATED` | `WAITING_FOR_PAYER` | Enregistré, pas encore envoyé (état transitoire dans la même requête). | Envoi d'un dépôt | `PROCESSING`, `SUBMITTED_UNCONFIRMED`, `FAILED` |
| `PROCESSING` | `WAITING_FOR_PAYER` | La demande est sur le téléphone ; le payeur doit la valider avec son code. `userMessage` : « Validez la demande reçue sur votre téléphone avec votre code secret. » | Réponse `PROCESSING` de l'opérateur à l'envoi | `SUCCEEDED`, `FAILED`, `UNRESOLVED` |
| `SUBMITTED_UNCONFIRMED` | `CONFIRMING` | L'opérateur n'a pas répondu à l'envoi : la demande est peut-être partie. `userMessage` : « Nous confirmons votre paiement auprès de l'opérateur. Ne payez pas une seconde fois. » | Réponse `UNKNOWN` (délai dépassé, erreur réseau) | `SUCCEEDED`, `FAILED`, `UNRESOLVED` |
| `SUCCEEDED` | `COMPLETED` | Payé. `userMessage` : « Paiement reçu. Merci. » La transaction est `SUCCEEDED` et a un `receiptNumber`. | Rappel PawaPay signé ou relu, relecture du worker, bouton « Relire », résolution manuelle `MARK_SUCCEEDED` | aucun (un retour passe par un remboursement) |
| `FAILED` | `FAILED` | Refusé. `failureCode` (code PawaPay), `userMessage` et `retryable` disent quoi faire. | Refus à l'envoi, rappel `FAILED`/`REJECTED`/`EXPIRED`, dépôt inconnu de PawaPay (`NOT_FOUND`), résolution `MARK_FAILED` | aucun ; un nouvel essai crée un nouveau dépôt |
| `UNRESOLVED` | `UNRESOLVED` | Aucune réponse définitive après `polling-max-hours` (24 h par défaut), ou confirmation avec un montant ou une devise différents (`failureCode` = `AMOUNT_MISMATCH`). `retryable` = `false`. La transaction reste `PROCESSING`. | Worker de relecture, rappel avec écart de montant | `SUCCEEDED` ou `FAILED` (réponse tardive, relecture, résolution manuelle) |

`ACCEPTED` est aussi traité comme « en attente » (`PawapayDeposit.java:144`) mais aucun code de ce module ne l'écrit.

Codes `failureCode` connus et leur `retryable` : `src/main/java/com/sni/bokaticowork/features/payment/provider/pawaypay/PawapayFailureCodes.java:20`. Exemples : `PAYMENT_NOT_APPROVED` (réessayable), `INSUFFICIENT_BALANCE` (réessayable), `PAYER_NOT_FOUND` (non), `AMOUNT_TOO_LARGE` (non), `EXPIRED` (réessayable), `MANUAL_RESOLUTION` (posé par une résolution manuelle). Un code inconnu donne `UNKNOWN_ERROR`, réessayable.

### Effet sur la transaction et l'intention

| Événement | Transaction | Intention |
| --- | --- | --- |
| Envoi accepté ou sans réponse | `PROCESSING` | `PROCESSING` |
| Envoi refusé immédiatement | `FAILED` | `PENDING` |
| Confirmation | `SUCCEEDED`, reçu émis | `SUCCEEDED` si tout est payé, sinon `PROCESSING` ou `PENDING` |
| Échec confirmé | `FAILED` | `PENDING` (de nouveau payable), ou `EXPIRED` si `expiresAt` est passé |
| `UNRESOLVED` | reste `PROCESSING` | inchangée, et **plus payable** tant que le montant en cours couvre le reste dû |

Sources : `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentServiceImpl.java:329` (envoi), `src/main/java/com/sni/bokaticowork/features/payment/service/pawaypay/PawapayCallbackProcessor.java` (`handleCompleted`, `handleFailed`).

### Rappel : `outcome`

`PROCESSED` (le sort du dépôt a été tranché), `DEFERRED` (l'opérateur n'a pas encore tranché ou ne répond pas, le worker reprend), `IGNORED` (le rappel ne désigne rien de connu), `FAILED` (le traitement a échoué, trace conservée). Filtre accepté par `GET /sni/api/v1/payments/mobile-money/callbacks?outcome=` ; toute autre valeur : `400` « Issue inconnue : X. Valeurs possibles : PROCESSED, DEFERRED, IGNORED, FAILED. »

## Parcours

### 1. Encaisser une intention par mobile money depuis le back-office

Prérequis : une intention `PENDING` ou `PROCESSING`, non expirée, dans la devise de l'opérateur (création : [parcours-paiements.md](parcours-paiements.md#1-créer-une-intention-de-paiement)).

1. `GET /sni/api/v1/payments/mobile-money/providers` ([route](routes/MobileMoneyController.md#r-get-sni-api-v1-payments-mobile-money-providers), publique). Renvoie `[{ code, displayName, countryCode, currency }]`. Filtrer côté écran sur `currency` = devise de l'intention : le serveur refuse un opérateur d'une autre devise.
2. Formulaire : opérateur (liste ci-dessus), numéro de téléphone tel que le payeur le compose (`06 123 45 67`, `+242061234567`… : le serveur complète l'indicatif du pays de l'opérateur), montant facultatif.
3. `PATCH /sni/api/v1/payments/intents/{intentNumber}/mobile-money` ([route](routes/PaymentController.md#r-patch-sni-api-v1-payments-intents-intentnumber-mobile-money)) avec `{ "phoneNumber": "...", "correspondent": "MTN_MOMO_COG" }` et, au besoin, `amount` (entier, au plus le reste payable). Variante équivalente : `POST /sni/api/v1/payments/mobile-money/deposits` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-mobile-money-deposits)) avec `intentNumber` dans le corps (obligatoire, sinon `400` « intentNumber is required »). Avec le `PATCH`, un `intentNumber` présent dans le corps doit être celui du chemin (`400` « Body intentNumber must match path intentNumber »).
4. La réponse est **toujours `200`** dès que l'appel à l'opérateur a eu lieu, même si l'opérateur a refusé : lire `phase`.
   - `WAITING_FOR_PAYER` : afficher `userMessage` et un indicateur d'attente, conserver `depositId`.
   - `CONFIRMING` : même attente, avec l'avertissement de ne pas repayer.
   - `FAILED` : afficher `userMessage` ; proposer « Réessayer » si `retryable` vaut `true`, sinon proposer de corriger le numéro ou l'opérateur, ou un autre moyen de paiement.
5. Attente du résultat : voir le parcours 2.

### 2. Attendre le rappel et sonder le statut (écran d'attente)

Le serveur apprend l'issue de trois façons : le **rappel** de PawaPay (immédiat, quelques secondes à quelques minutes après la validation sur le téléphone), le **worker** de relecture (première relecture une minute après l'envoi, puis à 2, 3, 5, 10, 15, 30 minutes, puis toutes les heures, pendant 24 h : `PawapayDepositService.java:274`), et la **relecture à la demande**.

Côté back-office :

1. Sonder `GET /sni/api/v1/payments/mobile-money/deposits/{depositId}` ([route](routes/MobileMoneyController.md#r-get-sni-api-v1-payments-mobile-money-deposits-depositid)). Cette lecture ne contacte pas l'opérateur : elle lit la base, que le rappel met à jour. Un intervalle de 3 à 5 secondes pendant les deux premières minutes, puis espacé, suffit. Arrêter dès que `phase` vaut `COMPLETED`, `FAILED` ou `UNRESOLVED`. `nextStatusCheckAt` indique la prochaine relecture prévue par le worker (nul quand il n'y a plus rien à attendre).
2. Bouton « Relire auprès de l'opérateur » : `POST /sni/api/v1/payments/mobile-money/deposits/{depositId}/recheck` ([route](routes/MobileMoneyController.md#r-post-sni-api-v1-payments-mobile-money-deposits-depositid-recheck)). Relit chez PawaPay et applique la réponse si le dépôt est en attente ou `UNRESOLVED` ; sinon renvoie l'état tel quel. Tant que l'opérateur répond `PROCESSING`, rien ne change.
3. À `COMPLETED` : la réponse contient `transaction` (avec `transactionNumber`, `receiptNumber`). Proposer le reçu : `GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt/pdf` ([route](routes/PaymentReceiptController.md#r-get-sni-api-v1-payments-transactions-transactionnumber-receipt-pdf)). Recharger l'intention (`GET /sni/api/v1/payments/intents/{intentNumber}`) pour afficher `remainingAmount`.
4. À `FAILED` : afficher `userMessage`, puis proposer un nouvel essai (retour au parcours 1, étape 3) si `retryable`.
5. À `UNRESOLVED` : afficher `userMessage` (« Nous n'avons pas reçu de réponse définitive de l'opérateur… ») et ne proposer **aucun** nouvel essai mobile money : le reste payable de l'intention est bloqué par la transaction encore `PROCESSING`. Orienter vers l'écran de rapprochement (parcours 5).

Côté portail membre, le sondage passe par `GET /sni/api/v1/client/billing/mobile-money/deposits/{depositId}` (module `portal`), qui vérifie que le dépôt appartient au membre ; même réponse `MobileMoneyDepositResponse`, mêmes règles de `phase`.

### 3. Envoyer un lien de paiement (paiement sans compte)

1. L'intention doit être `PENDING`, `PROCESSING` ou `AUTHORIZED` avec un reste dû positif. **Attention à son échéance** : une intention créée sans `expiresAt` expire 15 minutes après sa création (`PaymentServiceImpl.java:1092`), et la page du lien se ferme alors (« Ce lien de paiement n'est plus valable »). Pour un lien, créer l'intention avec un `expiresAt` au moins égal à la durée du lien.
2. `POST /sni/api/v1/payments/intents/{intentNumber}/link` ([route](routes/PaymentController.md#r-post-sni-api-v1-payments-intents-intentnumber-link)) avec un corps facultatif `{ "expiresInMinutes": 1440 }` (défaut 1440, minimum effectif 5). Un nouvel appel remplace le jeton précédent : l'ancien lien cesse de fonctionner.
3. Réponse `PaymentLinkResponse` : `checkoutUrl` (`<app.api-base-url>/public/pay/<token>`), `token`, `expiresAt`, `emailedTo`. Si l'intention a une adresse de client (`customerEmail`), le lien lui est envoyé par courriel après validation de la transaction ; `emailedTo` est alors renseigné, sinon nul. `depositId` et `transactionNumber` sont toujours nuls.
4. L'écran affiche `checkoutUrl` avec un bouton « Copier » et, si `emailedTo` est nul, l'indique (aucun courriel envoyé).
5. Contrôle depuis le back-office : `GET /sni/api/v1/payment-links/{token}` ([route](routes/PaymentLinkController.md#r-get-sni-api-v1-payment-links-token)) renvoie l'intention, ou `400` « Payment link has expired » / « Payment link is no longer payable », `404` « Payment link not found ».

### 4. La page publique `/public/pay/{token}` (servie par le serveur)

Ces quatre routes rendent des pages HTML Thymeleaf (`templates/payment/pay-form`, `pay-status`, `pay-closed`) ; le frontend n'a rien à construire, seulement à partager l'URL. À connaître pour le support :

1. `GET /public/pay/{token}` ([route](routes/PublicPaymentPageController.md#r-get-public-pay-token)) : formulaire (pays, numéro, opérateur seulement si nécessaire). Si un dépôt est déjà en attente sur l'intention, redirection directe vers son suivi. Lien inconnu, expiré, déjà réglé : page « fermée » avec un motif en français.
2. `POST /public/pay/{token}` ([route](routes/PublicPaymentPageController.md#r-post-public-pay-token)), formulaire `country`, `phone`, `operator` (facultatif). Si le pays a un seul opérateur actif, il est pris d'office ; sinon PawaPay devine l'opérateur à partir du numéro, et s'il n'y arrive pas, le formulaire revient avec « Nous n'avons pas reconnu l'opérateur de ce numéro. Choisissez-le ci-dessous. ». Le montant envoyé est tout le reste dû (`remainingAmount`), sans choix. Redirection vers le suivi.
3. `GET /public/pay/{token}/suivi/{depositId}` ([route](routes/PublicPaymentPageController.md#r-get-public-pay-token-suivi-depositid)) : page de suivi qui se rafraîchit seule.
4. `GET /public/pay/{token}/suivi/{depositId}/etat` ([route](routes/PublicPaymentPageController.md#r-get-public-pay-token-suivi-depositid-etat)) : JSON `{ "phase", "status", "message" }` interrogé par la page de suivi. Au plus une relecture chez l'opérateur toutes les 20 secondes par dépôt, quel que soit le rythme des appels (`PublicPaymentPageService.java:53`). `404` si le dépôt n'appartient pas à l'intention du jeton.

Les chemins `/public/**` sont publics (`src/main/java/com/sni/bokaticowork/security/config/PublicPaths.java:65`). Le commentaire du contrôleur annonce une limite de 10 écritures publiques par 10 minutes et par adresse (non vérifié dans le filtre de limitation).

### 5. Superviser et rapprocher les paiements mobile money (back-office)

Réservé aux rôles `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`, `ROLE_MANAGER` en plus du droit (voir « À prévoir »).

1. Liste : `GET /sni/api/v1/payments/mobile-money/operations` ([route](routes/MobileMoneyOperationsController.md#r-get-sni-api-v1-payments-mobile-money-operations)), filtres `from`, `to` (dates `AAAA-MM-JJ`, fuseau `app.time-zone`), `status` (statut technique du dépôt), `provider`, `customerCode`, `phone` (recherche partielle sur les chiffres), `reference` (égalité sur `depositId`, référence opérateur, `intentNumber`, `transactionNumber` ou `clientReferenceId`), `page`, `size` (plafonné à 100). Pour retrouver les dépôts d'une intention : `reference={intentNumber}`.
   Liste plus simple : `GET /sni/api/v1/payments/mobile-money/deposits?status=UNRESOLVED` ([route](routes/MobileMoneyController.md#r-get-sni-api-v1-payments-mobile-money-deposits)) donne la file à rapprocher.
2. Synthèse : `GET /sni/api/v1/payments/mobile-money/operations/summary` (30 derniers jours par défaut) renvoie `[{ status, currency, count, amount }]`.
3. Fiche : `GET /sni/api/v1/payments/mobile-money/operations/{depositId}` ([route](routes/MobileMoneyOperationsController.md#r-get-sni-api-v1-payments-mobile-money-operations-depositid)) renvoie `deposit`, `intentStatus`, `transactionStatus`, `provider` (ce que PawaPay dit du dépôt, relu en direct ; `available` = `false` si PawaPay n'a pas pu être lu), `discrepancies` (écarts avec, pour chacun, l'action conseillée) et `callbacks` (rappels reçus).
4. Résolution : `POST /sni/api/v1/payments/mobile-money/operations/{depositId}/resolve` ([route](routes/MobileMoneyOperationsController.md#r-post-sni-api-v1-payments-mobile-money-operations-depositid-resolve)) avec `{ "action": "SYNC" | "MARK_SUCCEEDED" | "MARK_FAILED", "reason": "...", "providerTransactionId": "..." }`.
   - `SYNC` : relit PawaPay et applique sa réponse ; ouvert à `ROLE_MANAGER`.
   - `MARK_SUCCEEDED` / `MARK_FAILED` : forcent l'issue, réservés à `ROLE_ADMIN` et `ROLE_SUPER_ADMIN` (`403` sinon : « Seuls un administrateur ou un super-administrateur peuvent forcer l'issue d'un paiement. »). Motif d'au moins 10 caractères ; `providerTransactionId` obligatoire pour `MARK_SUCCEEDED`. Seulement sur un dépôt en attente ou `UNRESOLVED`. La décision est journalisée (`@Audited`) et envoyée aux super-administrateurs.
   - Réponse : la fiche à jour.
5. Rapprochement d'une période : `GET /sni/api/v1/payments/mobile-money/operations/reconciliation?from=&to=` (7 derniers jours par défaut) renvoie `{ from, to, checked, truncated, discrepancies: [{ deposit, provider, discrepancies }] }`. `truncated` = `true` : la période contenait trop de dépôts, réduire l'intervalle.
6. Rappels : `GET /sni/api/v1/payments/mobile-money/callbacks?outcome=` pour expliquer un paiement manquant.

### 6. Rembourser un paiement mobile money

1. `POST /sni/api/v1/payments/transactions/{transactionNumber}/refund` (droit `PAYMENT:REFUND`) avec `{ "amount": ..., "reason": "..." }`, sur la transaction mobile money `SUCCEEDED`. Détail commun aux remboursements : [parcours-paiements.md](parcours-paiements.md#5-rembourser-ou-annuler-une-transaction).
2. Spécificité mobile money : le remboursement est **asynchrone**. La réponse est une transaction de remboursement `PROCESSING` ; la facture n'est rouverte qu'à la confirmation de PawaPay (rappel `refund-callback` ou relecture du worker), qui passe la transaction `REFUNDED`. Un refus immédiat de l'opérateur renvoie `400` « Remboursement refusé par l'opérateur : ... » et laisse une transaction de remboursement `FAILED` tracée.
3. Suivi : `GET /sni/api/v1/payments/transactions/{transactionNumber}/refunds` liste les remboursements de la transaction d'origine avec leur statut.

### 7. Essai réel (développement)

`POST /sni/api/v1/payments/mobile-money/pawaypay/test/{phoneNumber}?operator=MTN_MOMO_COG&amount=10` envoie une vraie demande sur un vrai téléphone. Réservé à `ROLE_ADMIN` et `ROLE_SUPER_ADMIN`, et seulement si `bokati.payment.pawaypay.test-endpoint-enabled` est vrai (vrai en dev, faux en prod) ; sinon `403` « L'essai mobile money est désactivé... ». Renvoie `{ deposit, intent, providers }`. Ne pas exposer dans un écran de production.

## Règles et erreurs à refléter dans l'écran

Contrôles avant l'appel à l'opérateur (`src/main/java/com/sni/bokaticowork/features/payment/service/pawaypay/MobileMoneyInitiationGuard.java:55`), tous en `400` avec un message français prêt à afficher :

| Cas | Message |
| --- | --- |
| Opérateur non activé | « Le paiement par <opérateur> n'est pas disponible. » |
| Montant décimal | « Le montant doit être un nombre entier de XAF. » (l'opérateur n'accepte pas les centimes) |
| Devise de l'opérateur différente de celle de l'intention | « L'opérateur ... encaisse en XOF, or cette facture est en XAF. » |
| Numéro invalide | « Le numéro de téléphone n'est pas valide pour <opérateur>. » |
| Numéro d'un autre pays | « Ce numéro n'est pas un numéro <opérateur> · il doit commencer par +242. » |
| Préfixe hors plage (si `operator-prefixes` est configuré ; vide par défaut) | « Ce numéro ne semble pas être un compte <opérateur>... » |

Contrôles sur l'intention (`PaymentServiceImpl.java:826` et `:1095`), messages en anglais, à traduire dans l'écran :

| Statut | Message | À afficher |
| --- | --- | --- |
| `400` | « Payment intent is not payable » | L'intention n'est plus payable (payée, expirée, remboursée). |
| `400` | « Payment intent has expired » | L'intention a expiré : en créer une nouvelle. |
| `400` | « Payment intent is already fully paid » | Déjà réglée. |
| `400` | « Payment intent has pending mobile money transactions. Wait for their final status before adding another payment » | Un dépôt est déjà en cours : retrouver son `depositId` (parcours 5, `reference={intentNumber}`) et reprendre l'attente. |
| `400` | « Payment amount cannot exceed remaining payable intent balance » | Montant trop élevé. |
| `404` | « Payment intent not found » | |

Autres :

- Le corps de `PATCH .../mobile-money` exige `phoneNumber` et `correspondent` (`400` `VALIDATION_FAILED` sinon). `correspondent` doit être un code de la liste, sinon `400`.
- Un échec à l'envoi n'est **pas** une erreur HTTP : `200` avec `phase` = `FAILED`.
- `GET .../deposits/{depositId}` inconnu : `404` (message « PawaPay deposit not found with depositId : ... », format du `ResourceNotFoundException`, non vérifié mot pour mot).
- Résolution manuelle : `400` « Indiquez l'action : SYNC, MARK_SUCCEEDED ou MARK_FAILED. », « Ce paiement est déjà SUCCEEDED · une résolution manuelle ne s'applique qu'à un paiement en attente ou non résolu... », « Expliquez la décision en au moins 10 caractères... », « Pour déclarer un paiement réussi, indiquez la référence de l'opérateur qui le prouve. ».

## Idempotence

Aucune route de ce parcours ne lit l'en-tête `Idempotency-Key` (aucune annotation `@Idempotent` dans `features/payment`). Les protections sont dans le métier :

1. **Même intention, même numéro, dépôt encore en attente** créé depuis moins de `in-flight-window-minutes` (20 min) : le dépôt existant est renvoyé tel quel au lieu d'en créer un second (`PaymentServiceImpl.java:337`). Mais le contrôle du reste payable passe avant : si le premier dépôt couvre tout le reste dû, le second appel reçoit `400` « Payment intent has pending mobile money transactions... » (voir Écarts). L'écran doit donc **bloquer le bouton** pendant l'appel et, sur ce `400`, reprendre l'attente du dépôt existant.
2. **Un seul encaissement par dépôt** : le rappel, le worker et un agent peuvent trancher en même temps ; la transaction est verrouillée et relue, le second passage ne fait rien (`PawapayCallbackProcessor.java:84`). Un rappel `FAILED` arrivant après un succès est ignoré.
3. **Le dépôt n'est jamais déclaré échoué faute de réponse** : `UNKNOWN` donne `SUBMITTED_UNCONFIRMED`, puis `UNRESOLVED` après 24 h. C'est ce qui évite de faire repayer un client déjà prélevé : ne jamais proposer de repayer sur `CONFIRMING` ou `UNRESOLVED`.
4. **Trop-perçu** : si un dépôt aboutit alors que la facture a déjà été réglée autrement, le reste est crédité au portefeuille du client (`OVERPAYMENT_CREDIT`), une seule fois (`PawapayCallbackProcessor.java:232`).

## À prévoir côté frontend

- **Droits.** Toutes les routes de suivi du `MobileMoneyController` et du `MobileMoneyOperationsController` exigent en plus un rôle `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` ou `ROLE_MANAGER` (`@PreAuthorize`). Un `ROLE_CASHIER` ou `ROLE_FINANCE` qui porte `PAYMENT:READ` reçoit `403` : masquer ces écrans selon le rôle, pas seulement le droit. L'envoi d'un dépôt (`PATCH .../mobile-money`) n'exige que `PAYMENT:PROCESS`, sans condition de rôle : un caissier peut lancer un dépôt mais pas le sonder par `GET .../deposits/{depositId}`. Pour lui, sonder plutôt `GET /sni/api/v1/payments/intents/{intentNumber}` (`PAYMENT:READ`) jusqu'à ce que `processingAmount` revienne à 0, puis lire les transactions de l'intention.
- **Écrans.** Formulaire de paiement mobile money (opérateur, numéro, montant) ; écran d'attente avec message, minuterie et bouton « Relire » ; écran de supervision (liste filtrable, synthèse par statut, fiche comparée à PawaPay, action de résolution avec motif) ; écran de génération de lien (URL à copier, échéance).
- **Liste de référence** : `GET /sni/api/v1/payments/mobile-money/providers`, publique, à charger une fois.
- **Montant** : entier, sans décimales ; par défaut ne rien envoyer (le serveur prend tout le reste payable).
- **Champs à ne pas envoyer** : `createdBy` est accepté mais n'est qu'une étiquette ; `intentNumber` dans le corps du `PATCH` est inutile.
- **Champs à ne pas montrer à un payeur** : `requestPayloadJson`, `providerResponseJson`, `callbackUrl`, `providerMessage`, `failureReason`, `metadata`.
- **Temps réel** : aucun sujet WebSocket ne publie l'état d'un dépôt dans ce module ; le sondage est la seule voie.

## Effets automatiques

| Automatisme | Fréquence | Effet visible |
| --- | --- | --- |
| `MobileMoneyStatusPollingWorker` (`src/main/java/com/sni/bokaticowork/features/payment/worker/MobileMoneyStatusPollingWorker.java:43`) | toutes les 60 s (`polling-delay-ms`), seulement si `bokati.payment.pawaypay.enabled` | Relit les dépôts échus (lots de 100) : `SUCCEEDED`, `FAILED`, ou nouvelle échéance ; `UNRESOLVED` après 24 h. Relit aussi les remboursements en cours. |
| `MobileMoneyDailyReconciliation` | chaque jour à 06:00, heure de Brazzaville | Compare la veille avec PawaPay, tranche automatiquement les dépôts dont PawaPay a l'issue, envoie un rapport (`MOBILE_MONEY_DAILY_RECONCILIATION`) aux destinataires d'alerte. |
| Rappel PawaPay | à chaque rappel | Toujours `200`. Signature vérifiée si `callback-secret` est configuré ; sinon le statut est relu chez PawaPay avant toute écriture. |
| Rappel arrivé avant l'enregistrement du dépôt | après validation de la transaction d'envoi | Le dépôt est relu dès son enregistrement (`EarlyCallbackRelay`), le rappel est noté `DEFERRED`. |
| Courriel au client (outbox) | à l'échec confirmé, et au passage `UNRESOLVED` ou écart de montant | `MOBILE_MONEY_DEPOSIT_FAILED` (raison et conduite à tenir), `MOBILE_MONEY_DEPOSIT_PENDING_REVIEW` (« ne payez pas une seconde fois »). Seulement si le client a une adresse. |
| Alerte interne (outbox) | passage `UNRESOLVED`, écart de montant | `MOBILE_MONEY_DEPOSIT_UNRESOLVED` à `alert-email`, à défaut aux super-administrateurs. |
| Paiement en libre-service confirmé | à la confirmation d'un paiement dont `channel` = `SELF_SERVICE` | `SELF_SERVICE_PAYMENT_RECEIVED` : notification dans le portail d'administration et courriel à `app.self-service.confirmation-email`, pour que la caisse sache que le client a payé. |
| Suite métier du paiement | outbox, toutes les 10 s | Voir [README.md](README.md#effets-automatiques) : facture soldée, abonnement, pass ou réservation activés, caisse automatique. |

## Écarts relevés

- Dédoublonnage d'un dépôt en cours presque inatteignable : `requestedIntentAmount` (contrôle du reste payable) est appelé avant `inFlight`, donc un second envoi sur une intention dont le premier dépôt couvre tout le reste dû échoue en `400` au lieu de rendre le dépôt existant. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentServiceImpl.java:331` et `:337`.
- Lien de paiement plus long que l'intention : le lien vaut 24 h par défaut mais l'intention créée sans `expiresAt` expire au bout de 15 minutes, et `createLink` ne prolonge pas l'intention. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentLinkServiceImpl.java:59` et `PaymentServiceImpl.java:1092`.
- `CreatePaymentLinkRequest.expiresInMinutes` porte `@Min(5)` mais le contrôleur n'a pas `@Valid` : la contrainte n'est pas appliquée (le service ramène à 5 en silence). `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentController.java:153`.
- `PaymentLinkResponse.depositId` et `transactionNumber` sont toujours nuls. `PaymentLinkServiceImpl.java:72`.
- Le portail membre reçoit la même `MobileMoneyDepositResponse` que le back-office, avec `requestPayloadJson`, `providerResponseJson` et `callbackUrl` (URL interne du serveur). `src/main/java/com/sni/bokaticowork/features/portal/billing/service/ClientBillingService.java:139`, construit par `PawapayDepositService.java:316`.
- Rôle `ROLE_MANAGER` autorisé par `@PreAuthorize` alors que les autres rôles d'administration (`ROLE_FINANCE`, `ROLE_CASHIER`…) sont refusés même avec `PAYMENT:READ` : règle d'accès à deux niveaux, invisible dans le droit seul. `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyController.java:53`, `MobileMoneyOperationsController.java` (constante `STAFF`).
- `GET /sni/api/v1/payments/mobile-money/pawapay/return` redirige vers `payment-page-result-url` avec `status` lu de la `phase` ; ce retour servait la page hébergée de PawaPay, que le lien de paiement n'utilise plus (`PaymentLinkServiceImpl.java:55`). Route publique probablement morte. `MobileMoneyController.java:204`.
- Sans fournisseur configuré (`bokati.payment.pawaypay.enabled` = `false`), l'envoi reçoit `NOT_IMPLEMENTED` : la transaction passe `FAILED` mais le dépôt reste `PROCESSING` (phase `WAITING_FOR_PAYER`) pour toujours, le worker étant désactivé. `PawapayDepositService.java:160` et `PaymentServiceImpl.java:384`.
- Messages d'erreur en anglais sur l'intention (« Payment intent is not payable », etc.), alors que les contrôles de l'opérateur sont en français. `PaymentServiceImpl.java:829`, `:1101`.
