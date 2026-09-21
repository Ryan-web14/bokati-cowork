# Mobile money (PawaPay) · audit et plan d'implémentation

Périmètre : `features/payment/provider/pawaypay`, `features/payment/service/pawaypay`,
`MobileMoneyController`, `MobileMoneyStatusPollingWorker`, `PaymentServiceImpl.initiateMobileMoneyDeposit`,
`DunningServiceImpl`, les routes publiques de `SecurityConfig` / `JWTFilter`, la configuration
`bokati.payment.pawaypay.*` et la doc de test `docs/backend/mobile-money-pawaypay-testing.md`.

Le module fonctionne dans le cas nominal : intention → transaction `PROCESSING` → dépôt PawaPay →
rappel `COMPLETED` → transaction `SUCCEEDED`, reçu, imputation sur la facture, activation de
l'abonnement ou du pass. Ce qui suit est ce qui casse dès qu'on sort du cas nominal, classé par
gravité, puis le plan pour y remédier.

---

## 1. Failles de sécurité

### S1 · Un rappel non signé est accepté tel quel (critique)

`PawapaySignatureVerifier.verify` renvoie **vrai** quand aucun secret n'est configuré
(`bokati.payment.pawaypay.callback-secret` est vide en production, et le commentaire du yml dit
« leave empty to skip »). Le contrôleur traite alors le corps reçu comme authentique :
`callbackProcessor.process(payload)`. La doc de test montre l'attaque telle quelle :

```
curl -X POST .../payments/mobile-money/pawaypay/callback -d '{"depositId":"<uuid>","status":"COMPLETED"}'
```

Sans authentification (route publique), n'importe qui connaissant un `depositId` ou un
`clientReferenceId` (= numéro de transaction, qui apparaît dans les reçus et les URL) marque une
transaction **payée** : reçu émis, facture imputée, abonnement activé, pass émis. La logique de
repli « vérifier le statut chez PawaPay quand la signature est invalide » existe, mais elle ne
s'applique que si un secret est configuré et que la signature est fausse · jamais quand il n'y a
pas de secret.

Aggravant : `PawapayCallbackProcessor.findLatestCallbackCandidate` retrouve une transaction par
**téléphone + opérateur + devise + montant** quand le `depositId` est inconnu. Un rappel forgé n'a
donc même pas besoin d'un identifiant valide.

**Décision** : sans signature vérifiable, un rappel n'est qu'un signal · le statut est **toujours**
relu auprès de PawaPay (API authentifiée par clé) avant toute écriture. Le repli par téléphone et
montant est supprimé. Chaque rappel est journalisé avec la manière dont il a été vérifié.

### S2 · Clé API de production dans le dépôt (critique)

`application-dev.yml` porte une clé PawaPay réelle en valeur par défaut de `PAWAYPAY_API_KEY`
(un JWT `ES256` avec `pm: DAF,PAF`, c'est-à-dire dépôts et remboursements autorisés). Elle est dans
l'historique git. Quiconque lit le dépôt peut initier des remboursements vers n'importe quel
numéro.

**Décision** : retirer la valeur par défaut, **révoquer la clé dans le tableau de bord PawaPay et
en générer une nouvelle**, la fournir uniquement par variable d'environnement. Le retrait du yml
ne suffit pas · la clé est déjà exposée.

### S3 · Endpoint de test qui prélève de l'argent réel

`POST /payments/mobile-money/pawaypay/test/{phoneNumber}` crée une intention et un dépôt de
10 XAF vers l'API de production, accessible à tout utilisateur authentifié.

**Décision** : réservé `ADMIN`/`SUPER_ADMIN` et seulement quand `app.dev-mode` est vrai.

### S4 · Lecture d'un dépôt sans contrôle de propriété

`GET /payments/mobile-money/deposits/{depositId}` rend à tout utilisateur authentifié le numéro
de téléphone, les métadonnées, la charge utile envoyée et la réponse brute du fournisseur de
n'importe quel dépôt.

**Décision** : route réservée au personnel ; une route client `/client/billing/mobile-money/deposits/{id}`
qui vérifie que le dépôt appartient au membre, et ne rend que ce qui lui sert (statut, message,
étape, peut réessayer).

### S5 · Rappel de remboursement rejeté sans recours

Le `refund-callback` est rejeté quand la signature n'est pas valide · donc **toujours** rejeté en
production où le secret est vide (`verify` renvoie vrai, en fait, donc accepté sans
vérification · même faille que S1, pour les remboursements).

**Décision** : même règle que S1 · relecture du statut du remboursement chez PawaPay (`GET /v2/refunds/{id}`).

### S6 · Double initiation (double prélèvement)

Rien n'empêche deux initiations sur la même intention (double clic, rafraîchissement, deux
onglets). Chacune crée une transaction `PROCESSING` et un dépôt ; le client reçoit deux demandes
sur son téléphone et peut valider les deux. Le second `COMPLETED` marque la seconde transaction
`SUCCEEDED` : trop-perçu, sans avoir automatique.

**Décision** : une initiation sur une intention qui a déjà un dépôt en cours pour le même numéro
(fenêtre de 20 minutes) **rend ce dépôt** au lieu d'en créer un autre. Le `depositId` est déjà un
UUID généré côté serveur, et PawaPay répond `DUPLICATE_IGNORED` sur un renvoi · on s'appuie
dessus pour les retentatives réseau (voir R1).

### S7 · Route de retour publique sans limite

`GET /payments/mobile-money/pawapay/return?depositId=` déclenche une relecture de statut chez
PawaPay pour n'importe quel identifiant. Pas de fuite (le résultat est une redirection), mais un
moyen gratuit de consommer le quota d'appels API.

**Décision** : la relecture est plafonnée (pas plus d'une par dépôt par minute, mémorisée par
`last_status_checked_at`) et un identifiant inconnu redirige sans appel.

---

## 2. Défauts de résilience

### R1 · Une erreur réseau à l'initiation est lue comme un refus (critique)

`PawapayDepositProvider.initiate` attrape toute exception et renvoie `FAILED` ; `PaymentServiceImpl`
marque la transaction `FAILED` et l'intention `PENDING`. Or un délai dépassé **après** l'envoi de
la requête ne dit rien : PawaPay a pu recevoir le dépôt, et le client recevra la demande sur son
téléphone. S'il valide, l'argent part, la transaction est déjà `FAILED`, et le rappel `COMPLETED`
arrive sur une transaction que `handleCompleted` accepte de passer `SUCCEEDED` (bien) · mais entre
temps le client a souvent réessayé, donc payé deux fois (S6).

Le `RestClient` n'a **aucun délai** configuré : un appel peut rester suspendu indéfiniment et
bloquer le fil de la requête HTTP du client.

**Décision** : distinguer « refusé » (réponse `REJECTED`) de « injoignable » (exception réseau,
délai, 5xx). Injoignable ⇒ la transaction reste `PROCESSING`, le dépôt passe
`SUBMITTED_UNCONFIRMED`, et la relecture de statut tranche : `NOT_FOUND` chez PawaPay ⇒ le dépôt
n'est jamais arrivé, échec sans prélèvement, on peut réessayer ; sinon on suit le statut réel.
Délais : 5 s connexion, 20 s réponse.

### R2 · Une erreur réseau à la relecture de statut est lue comme un échec de paiement (critique)

`checkStatus` renvoie `FAILED` sur exception. Ce `FAILED` est consommé tel quel par le worker de
relance, par le repli du rappel non signé et par la route de retour : la transaction est marquée
échouée, l'intention annulée, **alors que le paiement est peut-être en cours ou terminé**. Un
incident réseau de quelques minutes chez nous transforme des paiements réels en échecs.

**Décision** : un statut `UNKNOWN` distinct, qui ne produit aucune écriture · on réessaie plus
tard.

### R3 · Abandon en `FAILED` après vingt-cinq minutes (majeur)

Le worker tourne toutes les 5 minutes, ne regarde que les transactions de plus de 10 minutes, et
abandonne en `FAILED` après 3 relectures. Une demande mobile money attend que le client saisisse
son code ; l'opérateur peut la garder en attente bien plus longtemps, et la confirmer ensuite. Un
paiement confirmé à la 40ᵉ minute arrive sur une transaction `FAILED` et une intention `CANCELLED`.

**Décision** : on n'abandonne **jamais** en `FAILED` de notre propre chef. Calendrier de relecture
dégressif (1, 2, 3, 5, 10, 15, 30 min, puis toutes les heures) pendant 24 h ; au-delà le dépôt
passe `UNRESOLVED` (pas `FAILED`) : la transaction reste `PROCESSING`, l'intention redevient
payable, l'équipe est prévenue et l'écran d'administration liste ce qui est à rapprocher. Un
`COMPLETED` tardif règle toujours la transaction.

### R4 · Un échec annule l'intention (majeur)

`handleFailed` passe l'intention `CANCELLED`. Un client qui s'est trompé de code, qui a refusé
par erreur ou dont le solde était insuffisant ne peut plus payer cette intention : elle n'est plus
« payable ». Pour une facture, `payInvoice` en recrée une ; pour un abonnement ou un pass en
attente d'activation, le client est bloqué.

**Décision** : l'échec d'une tentative rend l'intention `PENDING` (ou `EXPIRED` si sa date est
passée). L'intention n'est annulée que par une décision, pas par un échec d'opérateur.

### R5 · Un rappel en erreur est perdu

Le contrôleur attrape toute exception et répond `200` · PawaPay ne renvoie donc jamais le rappel.
Si la base était indisponible à cet instant, la notification est perdue et seule la relecture de
statut peut la rattraper (avec les limites de R3).

**Décision** : chaque rappel est **écrit avant d'être traité** (`pawapay_callback` : corps brut,
signature, mode de vérification, issue, erreur). Un traitement en erreur laisse le dépôt à
relire dans la minute. La table sert aussi d'audit des tentatives de forge (S1).

### R6 · Forme des réponses PawaPay v2

`PawapayDepositStatusResponse` lit `status` à la racine. L'API v2 enveloppe la réponse
(`{ "status": "FOUND", "data": { "status": "COMPLETED", ... } }`) · lu à plat, `FOUND` tombe dans
le `default` de `mapDepositStatus` ⇒ `FAILED`. `rejectionReason` est lu là où v2 envoie
`failureReason`. Les métadonnées sont envoyées comme des objets libres
(`{"intentNumber": …}`) là où v2 attend `{"fieldName", "fieldValue", "isPII"}`.

**Décision** : un lecteur tolérant qui accepte l'enveloppe v2, la forme à plat v1 et `NOT_FOUND` ;
`failureReason` et `rejectionReason` lus tous deux ; métadonnées au format documenté, le
téléphone marqué PII.

### R7 · Erreurs utilisateur non expliquées

Le client voit `failureReason` brut (`{"failureCode":"PAYER_NOT_FOUND",...}`) ou « Payment failed
at operator ». Rien ne dit s'il doit vérifier son numéro, recharger son compte, ou réessayer.
Aucune vérification avant envoi : un numéro Airtel envoyé à MTN, un opérateur RDC (CDF) sur une
intention en XAF.

**Décision** : catalogue des codes d'échec PawaPay → phrase en français + « peut réessayer » ;
contrôle du préfixe par opérateur (Congo : MTN `06`, Airtel `05`/`04`) et de la devise de
l'opérateur contre celle de l'intention, **avant** l'appel ; le dépôt rend `phase`
(`WAITING_FOR_PAYER`, `CONFIRMING`, `COMPLETED`, `FAILED`, `UNRESOLVED`) pour piloter l'écran.

### R8 · Relance mobile money jamais branchée

`DunningService.scheduleForFailedIntent` (retente le dépôt à J+1, J+3, J+7) n'est appelé nulle
part. Vu R4, une retentative automatique d'un dépôt après un refus explicite du client n'est
d'ailleurs pas souhaitable : c'est lui qui doit relancer.

**Décision** : le laisser débranché et le documenter ; la relance passe par les politiques de
relance d'impayés (lot I) et par l'espace client.

---

## 3. Ce qui manque

| Manque | Pourquoi ça compte |
|---|---|
| Journal des rappels reçus | audit, rejeu, détection de forge (S1, R5) |
| File des dépôts non résolus + relecture manuelle | rapprochement quand l'opérateur n'a pas répondu (R3) |
| Endpoint client pour suivre **son** dépôt | l'écran doit pouvoir demander « où en est mon paiement » sans exposer les autres (S4) |
| Message d'échec compréhensible et `retryable` | l'utilisateur sait quoi faire (R7) |
| Idempotence de l'initiation | double clic = un seul prélèvement (S6) |
| Délais et retentatives réseau | fils bloqués, faux échecs (R1, R2) |
| Alerte sur non-résolution | quelqu'un regarde ce que l'automate n'a pas pu trancher (R3) |
| Tests | aucun test n'existe sur ce module · chaque décision ci-dessus en aura un |

Ce qui n'est pas retenu : un disjoncteur (circuit breaker) devant l'API · le volume ne le
justifie pas et les délais suffisent ; une file de rejeu des rappels · la relecture de statut
couvre le besoin plus simplement.

---

## 4. Plan d'implémentation

Un seul lot, livré d'un bloc parce que les pièces se tiennent : la relecture de statut fiable
(R2, R6) est ce qui rend S1 et R1 sûrs.

### 4.1 Schéma · `V244` (dev) / `V240` (prod) · **fait**

- `pawapay_deposit` + `next_status_check_at`, `unresolved_at`, `failure_code`, `user_message`,
  `retryable`, `provider_transaction_id`, `initiation_attempts` ; index sur l'échéance de
  relecture, le client, le statut.
- `pawapay_callback` : `kind`, `reference_id`, `reported_status`, `raw_body`, `signature_present`,
  `verified_via` (`SIGNATURE` / `PROVIDER_STATUS` / `NONE`), `outcome` (`PROCESSED` / `DEFERRED` /
  `IGNORED` / `FAILED`), `detail`, `remote_address`, `received_at`, `processed_at`.

### 4.2 Fournisseur · **en cours**

- `PawapayClient` : délais de connexion et de réponse, `ProviderUnreachableException` pour réseau,
  délai et 5xx ; `getDepositStatus` rend un `DepositStatus(found, providerStatus, failureReason,
  providerTransactionId)` qui lit v2 enveloppé, v1 à plat et `NOT_FOUND` ; `getRefundStatus`.
  **fait**
- `PawapayFailureCodes` : code → message français + `retryable`. **fait**
- `PawapayProperties` : `connect-timeout-ms` (5000), `read-timeout-ms` (20000), `polling-max-hours`
  (24), `in-flight-window-minutes` (20), `alert-email`. **fait**
- `MobileMoneyStatusResponse` gagne `failureReason`, `providerTransactionId` ; statuts
  `SUCCEEDED` / `FAILED` / `PROCESSING` / `NOT_FOUND` / `UNKNOWN`.
  `MobileMoneyInitiationResponse` gagne `failureReason` ; statuts `PROCESSING` / `FAILED` /
  `UNKNOWN`. `PawapayDepositProvider` ne convertit plus une exception en `FAILED`.
- Métadonnées au format `fieldName` / `fieldValue` / `isPII`.

### 4.3 Initiation · `PaymentServiceImpl` et `PawapayDepositService`

- Contrôles avant appel : devise de l'opérateur = devise de l'intention ; préfixe du numéro par
  opérateur ; montant entier positif.
- Idempotence : dépôt en cours sur (intention, numéro) dans la fenêtre ⇒ on le rend.
- `UNKNOWN` à l'initiation ⇒ transaction `PROCESSING`, dépôt `SUBMITTED_UNCONFIRMED`,
  `provider_reference = depositId`, relecture dans 1 min.
- `FAILED` ⇒ transaction `FAILED` avec `failure_code` / `user_message` / `retryable`, intention
  `PENDING`.
- Réponse au client : + `failureCode`, `userMessage`, `retryable`, `phase`, `nextStatusCheckAt`.

### 4.4 Rappels · `PawapayCallbackGateway` (nouveau, sorti du contrôleur)

1. Écrire le rappel (`pawapay_callback`).
2. Signature valide ⇒ traiter le corps (`verified_via = SIGNATURE`).
3. Sinon ⇒ relire le statut chez PawaPay ; `SUCCEEDED` / `FAILED` / `NOT_FOUND` ⇒ traiter ce
   statut-là (`PROVIDER_STATUS`) ; `PROCESSING` / `UNKNOWN` ⇒ `DEFERRED`, relecture dans 1 min.
4. Corps illisible ou sans identifiant ⇒ `IGNORED`.
5. Exception ⇒ `FAILED` + relecture dans 1 min ; réponse `200` quand même.
6. Même chemin pour les remboursements avec `getRefundStatus`.

Le `verify` du `PawapaySignatureVerifier` renvoie **faux** sans secret (« non vérifiable »), plus
jamais vrai par défaut. `findLatestCallbackCandidate` est supprimé.

### 4.5 Traitement · `PawapayCallbackProcessor`

- `handleFailed` : intention `PENDING` (ou `EXPIRED`), `failure_code` / `user_message` / `retryable`
  sur le dépôt ; ne touche pas une transaction déjà terminale.
- `handleCompleted` : inchangé (idempotent), + `provider_transaction_id`, + clôture de
  `next_status_check_at`.
- Nouveau `markUnresolved` : dépôt `UNRESOLVED`, transaction laissée `PROCESSING`, intention
  `PENDING`, alerte `MOBILE_MONEY_DEPOSIT_UNRESOLVED` (`alert-email`, plusieurs adresses possibles).

### 4.6 Relecture · `MobileMoneyStatusPollingWorker`

- Toutes les minutes, les dépôts dont `next_status_check_at` est échu (par lot de 100).
- `SUCCEEDED` / `FAILED` / `NOT_FOUND` ⇒ traitement ; `PROCESSING` / `UNKNOWN` ⇒ prochaine
  échéance selon le calendrier 1, 2, 3, 5, 10, 15, 30 min puis 60 min ; au-delà de
  `polling-max-hours` ⇒ `markUnresolved`.
- Plus de `max-polling-attempts` ni de `polling-max-age-minutes` (gardés en configuration pour
  ne pas casser les déploiements, ignorés).

### 4.7 Routes

| Route | Rôle | Changement |
|---|---|---|
| `POST /payments/mobile-money/pawa(y)pay/callback`, `/refund-callback` | public | passent par la passerelle (4.4) |
| `GET /payments/mobile-money/pawapay/return` | public | relecture plafonnée, identifiant inconnu ⇒ redirection directe |
| `POST /payments/mobile-money/pawaypay/test/{phone}` | `ADMIN` + `app.dev-mode` | verrouillé |
| `GET /payments/mobile-money/deposits/{id}` | `ADMIN`, `SUPER_ADMIN`, `STAFF` | verrouillé |
| `GET /payments/mobile-money/deposits?status=UNRESOLVED` | staff | nouveau · la file à rapprocher |
| `POST /payments/mobile-money/deposits/{id}/recheck` | staff | nouveau · relecture immédiate |
| `GET /payments/mobile-money/callbacks?referenceId=` | `ADMIN` | nouveau · journal des rappels |
| `GET /client/billing/mobile-money/deposits/{id}` | membre | nouveau · son dépôt, vue réduite |

### 4.8 Configuration

`application-dev.yml` : `PAWAYPAY_API_KEY` sans valeur par défaut. `application-prod.yml` :
`callback-secret` **obligatoire à renseigner** (le code ne fait plus confiance sans lui, mais la
signature évite un appel API par rappel), `connect-timeout-ms`, `read-timeout-ms`,
`polling-delay-ms: 60000`, `polling-max-hours: 24`, `alert-email` (repli : manager support).

### 4.9 Tests

- `PawapayClientTest` : lecture des trois formes de statut, `NOT_FOUND`, `UNKNOWN` sur exception.
- `PawapayCallbackGatewayTest` : sans secret ⇒ relecture obligatoire ; corps forgé `COMPLETED`
  avec statut réel `PROCESSING` ⇒ `DEFERRED`, rien n'est écrit ; signature valide ⇒ traité ;
  erreur ⇒ `FAILED` + relecture programmée.
- `PawapayCallbackProcessorTest` : `COMPLETED` idempotent ; `FAILED` ⇒ intention `PENDING` et
  message français ; `COMPLETED` tardif après `FAILED` ⇒ `SUCCEEDED`.
- `MobileMoneyStatusPollingWorkerTest` : `UNKNOWN` ⇒ aucune écriture, échéance repoussée ;
  calendrier dégressif ; `UNRESOLVED` après 24 h, jamais `FAILED`.
- `PaymentServiceImpl` (initiation) : double initiation ⇒ même dépôt ; injoignable ⇒
  `PROCESSING` + `SUBMITTED_UNCONFIRMED` ; devise et préfixe refusés avant appel.

### 4.10 Hors code, à faire par vous

1. **Révoquer la clé API exposée** dans le tableau de bord PawaPay, en créer une nouvelle, la
   poser en `PAWAYPAY_API_KEY` sur Heroku.
2. Configurer un secret de rappel dans PawaPay et le poser en `PAWAYPAY_CALLBACK_SECRET`.
3. Vérifier l'URL de rappel enregistrée chez PawaPay (`APP_BASE_URL` + `/sni/api/v1/payments/mobile-money/pawapay/callback`).
4. Renseigner `PAWAYPAY_ALERT_EMAIL`.

---

## 5. Ordre de livraison

1. 4.1, 4.2 (schéma, client, codes d'échec) · fait ou en cours.
2. 4.3, 4.5, 4.6 (initiation, traitement, relecture) · le cœur de la résilience.
3. 4.4, 4.7 (passerelle de rappel, routes) · la sécurité.
4. 4.8, 4.9, doc frontend (section mobile money du guide) et doc backend.
