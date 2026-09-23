# Mobile money · PawaPay

Ce document décrit le module mobile money après le lot de résilience et de sécurité
(`docs/a-realiser/audit-et-plan-mobile-money.md`). Il remplace toute description antérieure du
comportement de la passerelle, du worker et des routes.

## La règle qui gouverne tout le module

**Nous ne tranchons jamais à la place de l'opérateur.** Un paiement mobile money est confirmé par
l'opérateur, refusé par l'opérateur, ou toujours en attente. Une erreur réseau, un délai dépassé,
une absence de réponse ne sont aucune de ces trois choses : ce sont des absences d'information.

Trois conséquences, qui expliquent presque tout le code :

- une exception à l'initiation ne donne jamais un paiement échoué · elle donne un paiement dont on
  ignore le sort, qu'on relira ;
- le temps qui passe ne produit jamais un échec · au bout du délai imparti le dépôt devient
  `UNRESOLVED`, un état qui demande une personne, pas une décision automatique ;
- un corps de rappel non signé ne déclenche aucune écriture par lui-même · c'est le statut relu
  chez l'opérateur, avec notre clé d'API, qui fait foi.

## Cycle de vie d'un dépôt

| Statut du dépôt | Ce que ça veut dire | Transaction | Intention |
|---|---|---|---|
| `PROCESSING` | La demande est partie, le client n'a pas encore validé | `PROCESSING` | `PROCESSING` |
| `SUBMITTED_UNCONFIRMED` | L'appel d'initiation n'a pas abouti à une réponse · la demande est peut-être partie | `PROCESSING` | `PROCESSING` |
| `SUCCEEDED` | L'opérateur confirme l'encaissement | `SUCCEEDED` | `SUCCEEDED` ou `PROCESSING` si partiel |
| `FAILED` | L'opérateur refuse, ou ne connaît pas la demande | `FAILED` | `PENDING` (ou `EXPIRED`) |
| `UNRESOLVED` | Aucune réponse définitive après `polling-max-hours` | laissée `PROCESSING` | laissée telle quelle |

Un échec **ne clôt pas l'intention**. Le client s'est trompé de code, son solde était insuffisant,
il a refusé par mégarde : la facture doit rester payable. L'intention ne devient `EXPIRED` que si
sa propre date d'expiration est passée.

La réponse `MobileMoneyDepositResponse` porte, en plus de `status` :

- `phase` · `WAITING_FOR_PAYER`, `CONFIRMING`, `COMPLETED`, `FAILED`, `UNRESOLVED` · ce qu'un écran
  affiche, sans jamais montrer un statut technique ;
- `failureCode` · le code de l'opérateur, pour compter par cause ;
- `userMessage` · la phrase française correspondante (`PawapayFailureCodes`) ;
- `retryable` · réessayer avec le même numéro a-t-il un sens ;
- `nextStatusCheckAt` · quand nous reparlerons à l'opérateur · `null` quand il n'y a plus rien à
  attendre.

## Initiation

`PaymentServiceImpl.initiateMobileMoneyDeposit` enchaîne trois choses avant tout appel réseau :

1. **`MobileMoneyInitiationGuard`** · opérateur choisi, montant entier positif, devise de
   l'opérateur égale à celle de l'intention, numéro normalisable en E.164. Optionnellement les
   plages de numéros par opérateur (`operator-prefixes`) · vide par défaut, parce qu'une plage mal
   recopiée refuserait un paiement valable.
2. **`PawapayDepositService.inFlight`** · un dépôt déjà en cours pour la même intention et le même
   numéro dans les `in-flight-window-minutes` est rendu tel quel. Un double clic envoyait
   auparavant deux demandes sur le téléphone du client, qu'il pouvait valider toutes les deux.
3. L'appel lui-même. Une réponse `UNKNOWN` (réseau, délai, 5xx) laisse le dépôt
   `SUBMITTED_UNCONFIRMED` avec une relecture dans une minute · jamais `FAILED`.

## Rappels de l'opérateur · `PawapayCallbackGateway`

Le contrôleur ne fait plus que relayer. Toute la logique est dans la passerelle :

1. Le rappel est écrit dans `pawapay_callback` **avant** traitement, dans sa propre transaction
   (`PawapayCallbackJournal`, `REQUIRES_NEW`) · un traitement qui échoue annule sa transaction, et
   la trace doit lui survivre.
2. Signature valide ⇒ le corps est traité (`verified_via = SIGNATURE`).
3. Sinon ⇒ le statut est relu chez l'opérateur :
   - `SUCCEEDED` / `FAILED` ⇒ traité selon **ce statut-là** (`PROVIDER_STATUS`, `PROCESSED`) ;
   - `NOT_FOUND` ⇒ `IGNORED`, rien n'est écrit · un rappel forgé finit ici ;
   - `PROCESSING` / opérateur injoignable ⇒ `DEFERRED`, prochaine échéance posée.
4. Corps illisible ou sans identifiant ⇒ `FAILED` / `IGNORED`, avec le détail consigné.
5. La réponse HTTP est **toujours 200** · un non-2xx fait rejouer PawaPay indéfiniment, alors que
   la trace suffit à rattraper ce qui n'a pas abouti.

`PawapaySignatureVerifier.verify` renvoie **faux** sans secret configuré. Il renvoyait vrai :
n'importe qui connaissant l'URL du webhook pouvait déclarer un paiement reçu.

`findLatestCallbackCandidate` a été supprimé. Il retrouvait, à défaut d'identifiant connu, le
dernier dépôt du même numéro pour le même montant : un rappel forgé avec des valeurs plausibles
encaissait le dépôt de quelqu'un d'autre.

## Relecture · `MobileMoneyStatusPollingWorker` et `MobileMoneySupervisionService`

Le worker passe toutes les minutes et prend les dépôts dont `next_status_check_at` est échu, par
lot de 100. La règle elle-même vit dans `MobileMoneySupervisionService.settle`, pour qu'une
relecture demandée par un agent et une relecture automatique donnent le même résultat.

Calendrier de relecture : **1, 2, 3, 5, 10, 15, 30 minutes, puis toutes les heures**. Un client
valide en général dans la minute ; passé le quart d'heure il est parti manger.

Au-delà de `polling-max-hours` (24 par défaut) sans réponse définitive : `markUnresolved`, et une
alerte `MOBILE_MONEY_DEPOSIT_UNRESOLVED` par destinataire de `alert-email` (plusieurs adresses,
séparées par des virgules ou des points-virgules). L'alerte n'est envoyée qu'une fois · un dépôt
déjà `UNRESOLVED` ne se redéclare pas.

## Ce que le client apprend · `MobileMoneyClientNotifier`

Un échec mobile money survient souvent plusieurs minutes après que le client a fermé sa page : il
a saisi un mauvais code, son solde ne suffisait pas, la demande a expiré pendant qu'il était en
réunion. Jusqu'ici il ne l'apprenait nulle part · sa facture restait ouverte sans explication, et
il rappelait l'accueil pour comprendre.

Deux courriels, jamais plus :

| Événement | Gabarit | Quand | Ce qu'il dit |
|---|---|---|---|
| `MOBILE_MONEY_DEPOSIT_FAILED` | `mobile-money-deposit-failed` | `PawapayCallbackProcessor.handleFailed` | La raison en français (`userMessage`), et la conduite à tenir selon `retryable` : reprendre le même paiement, ou changer de numéro / de moyen |
| `MOBILE_MONEY_DEPOSIT_PENDING_REVIEW` | `mobile-money-deposit-pending-review` | `MobileMoneySupervisionService` au passage en `UNRESOLVED` | **Ne pas payer une seconde fois.** Nous vérifions auprès de l'opérateur |

Le second est le message le plus important du module. Un client sans nouvelles qui voit sa facture
ouverte repaye, et se retrouve débité deux fois pour une seule prestation.

Les deux courriels annoncent explicitement qu'aucun montant n'a été prélevé (échec) ou que le
rapprochement est en cours (non tranché). Le code technique de l'opérateur n'apparaît jamais dans
le corps : `MTN_MOMO_COG` devient « MTN Mobile Money Congo », `INSUFFICIENT_BALANCE` devient la
phrase correspondante de `PawapayFailureCodes`.

Un échec **synchrone** à l'initiation (l'opérateur refuse dans la seconde) ne déclenche pas de
courriel : le client est devant son écran et reçoit le message dans la réponse HTTP.

Sans adresse joignable pour le client, rien ne part · c'est journalisé, et aucun encaissement
n'est bloqué pour autant.

Voir aussi `docs/backend/paiement-libre-service.md` : un règlement abouti depuis l'espace client
prévient aussi le back-office.

## Routes

| Route | Accès | Ce qu'elle fait |
|---|---|---|
| `GET /payments/mobile-money/providers` | public | La liste des opérateurs pour le formulaire |
| `POST /payments/mobile-money/pawa(y)pay/callback` | public | Rappel de dépôt · passerelle, toujours 200 |
| `POST /payments/mobile-money/pawa(y)pay/refund-callback` | public | Rappel de remboursement · même chemin |
| `GET /payments/mobile-money/pawapay/return` | public, plafonné | Retour navigateur · relit le statut puis redirige |
| `GET /payments/mobile-money/deposits/{id}` | staff | Un dépôt · **n'est plus public** |
| `GET /payments/mobile-money/deposits?status=UNRESOLVED` | staff | La file à rapprocher |
| `POST /payments/mobile-money/deposits/{id}/recheck` | staff | Relecture immédiate, y compris d'un `UNRESOLVED` |
| `GET /payments/mobile-money/callbacks?outcome=FAILED` | staff | Le journal des rappels |
| `POST /payments/mobile-money/pawaypay/test/{phone}` | `ADMIN` **et** `test-endpoint-enabled` | Envoie une vraie demande de 10 XAF |
| `GET /client/billing/mobile-money/deposits/{id}` | membre | Son propre dépôt, propriété vérifiée |

La route de retour est désormais soumise au plafond des rappels : elle déclenche une lecture chez
l'opérateur à chaque appel, et une boucle sur cette URL consommait notre quota d'API.

Un membre qui demande le dépôt d'un autre reçoit `404`, pas `403` · la différence renseignerait sur
les paiements des autres.

## Configuration

```yaml
bokati:
  payment:
    pawaypay:
      enabled: ${PAWAYPAY_ENABLED:true}
      api-key: ${PAWAYPAY_API_KEY:}            # jamais de valeur par défaut dans le dépôt
      callback-secret: ${PAWAYPAY_CALLBACK_SECRET:}
      connect-timeout-ms: 5000
      read-timeout-ms: 20000
      polling-delay-ms: 60000                  # cadence du worker
      polling-max-hours: 24                    # au-delà : UNRESOLVED, jamais FAILED
      in-flight-window-minutes: 20
      alert-email: ${PAWAYPAY_ALERT_EMAIL:}    # une ou plusieurs adresses
      operator-prefixes: ${PAWAYPAY_OPERATOR_PREFIXES:}   # vide = aucun contrôle de préfixe
      test-endpoint-enabled: false             # vrai en développement seulement
```

Sans `callback-secret`, le module reste correct mais chaque rappel coûte un appel d'API
supplémentaire pour reconfirmer le statut. Configurer le secret côté PawaPay économise cet appel.

## Migrations

- `V244` (dev) / `V240` (prod) · colonnes de résilience sur `pawapay_deposit`, table
  `pawapay_callback`.
- `V245` (dev) / `V241` (prod) · gabarit de courriel `MOBILE_MONEY_DEPOSIT_UNRESOLVED`.
- `V247` (dev) / `V243` (prod) · gabarits `MOBILE_MONEY_DEPOSIT_FAILED` et
  `MOBILE_MONEY_DEPOSIT_PENDING_REVIEW`.

## Ce qui reste à faire hors code

1. Révoquer la clé API PawaPay qui figurait en clair dans `application-dev.yml` et dans `.env`,
   en créer une nouvelle, la poser en `PAWAYPAY_API_KEY`.
2. Configurer un secret de rappel dans PawaPay, le poser en `PAWAYPAY_CALLBACK_SECRET`.
3. Vérifier l'URL de rappel enregistrée chez PawaPay :
   `APP_BASE_URL` + `/sni/api/v1/payments/mobile-money/pawapay/callback`.
4. Renseigner `PAWAYPAY_ALERT_EMAIL`.
