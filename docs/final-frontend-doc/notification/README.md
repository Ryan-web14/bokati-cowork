# notification : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module enregistre et distribue les notifications de la plateforme sur trois canaux : courriel (`EMAIL`), cloche dans l'application (`IN_APP`) et appel HTTP sortant vers un système tiers (`WEBHOOK`). Ses 21 routes sont toutes réservées au back-office (droit `ADMIN:ACCESS`) : la cloche de l'administration, le journal de toutes les notifications, les modèles, les envois de test et les points de terminaison webhook.

Les notifications sont presque toujours créées par les autres modules (réservation, abonnement, facturation, contrat, paiement, support, sécurité), directement ou par la file d'événements (`outbox`, module [socle](../socle/README.md)). La cloche du **portail membre** lit la même table mais par ses propres routes, dans le module [portal](../portal/routes/ClientNotificationController.md). Le temps réel passe par le WebSocket décrit plus bas.

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Notification (`NotificationMessage`, table `notification_message`) | `notificationNumber` (code produit par le moteur de séquences, séquence `notification_message` ; format exact non vérifié) | Un message pour un destinataire sur un canal. Porte `eventType`, `aggregateType` + `aggregateId` (l'objet métier concerné, par exemple `BOOKING` + numéro de réservation), `recipientEmail`, `subject`, `payloadJson` (texte JSON), `status`, `attempts`, `sentAt`, `readAt`, `lastError`. |
| Modèle (`NotificationTemplate`) | `templateCode` (unique, mis en majuscules) | Surcharge, pour un code et un canal, l'objet du message (`subject`), le nom du gabarit Thymeleaf (`templateName`) ou un corps en ligne (`bodyTemplate`). Utilisé uniquement s'il est `active`. |
| Point de terminaison webhook (`WebhookEndpoint`) | `endpointCode` (séquence `webhook_endpoint`) | URL d'un tiers appelée en `POST` pour chaque événement abonné. Le secret n'est jamais renvoyé. |
| Livraison webhook (`WebhookDelivery`) | `deliveryNumber` (séquence `webhook_delivery`) | Une tentative d'appel d'un point de terminaison pour un événement. Porte `httpStatus`, `attempts`, `lastError`, `deliveredAt`. |

Énumérations (`src/main/java/com/sni/bokaticowork/features/notification/enums/`) :

- `NotificationChannel` : `EMAIL`, `IN_APP`, `WEBHOOK`.
- `NotificationRecipientType` : `MEMBER`, `CUSTOMER`, `BUSINESS`, `ADMIN`, `GUEST`, `SYSTEM`. Purement descriptif : aucune règle ne dépend de lui.
- `NotificationDeliveryStatus` : `PENDING`, `PROCESSING`, `SENT`, `DELIVERED`, `FAILED`, `CANCELLED`, `SKIPPED` (partagé par les notifications et les livraisons webhook).

Le destinataire d'une notification est identifié **par son adresse électronique** (mise en minuscules à l'enregistrement, `NotificationMapperDecorator.java:200`). « Mes notifications » signifie donc « les notifications dont `recipientEmail` est l'adresse du compte connecté ».

## Statuts et cycle de vie

### Notification

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | Créée, en attente d'envoi (ou envoi programmé via `availableAt`). | Création (`POST /sni/api/v1/notifications`, notifieurs des autres modules, outbox) ; `PATCH .../{notificationNumber}/retry`. | `PROCESSING` immédiatement si `availableAt` est vide ou passé, sinon au passage du worker. |
| `PROCESSING` | En cours de traitement (transitoire, dans la même transaction). | Traitement immédiat ou worker. | `SENT` ou `FAILED`. |
| `SENT` | Remise effectuée de notre côté : pour `IN_APP`, la notification est visible dans la cloche ; pour `EMAIL`, le courriel est confié à l'expéditeur (la livraison réelle se suit dans le journal des courriels du [socle](../socle/README.md)) ; pour `WEBHOOK`, l'événement est transmis à la file des webhooks. | Traitement réussi. | Terminal (le relancer est refusé). |
| `FAILED` | Échec (adresse absente pour un courriel, gabarit en erreur…). `lastError` dit pourquoi. | Traitement en erreur (`NotificationDispatchSupport.java:310`). | `PROCESSING` au prochain passage du worker après `availableAt` (délai `60 s x tentatives`, plafonné à 1 h), ou `PENDING` par la relance manuelle. Pas de nombre maximal de tentatives. |
| `DELIVERED`, `CANCELLED`, `SKIPPED` | Jamais attribués à une notification par le code actuel. | Aucun. | |

La lecture est indépendante du statut : `readAt` est rempli quand le destinataire marque la notification comme lue. Seules les notifications `SENT` (ou `DELIVERED`) et non lues comptent dans la cloche.

### Livraison webhook

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | À envoyer. | Toute notification dont l'`eventType` correspond à un point de terminaison actif ; `PATCH /sni/api/v1/webhook-deliveries/{deliveryNumber}/retry`. | `PROCESSING` au passage du worker webhook (toutes les 60 s). |
| `PROCESSING` | Appel HTTP en cours. | Worker. | `DELIVERED` ou `FAILED`. |
| `DELIVERED` | Le tiers a répondu 2xx. `deliveredAt` et `httpStatus` remplis. | Worker. | Terminal, sauf relance manuelle (acceptée, voir Écarts). |
| `FAILED` | Réponse non 2xx, délai dépassé (20 s) ou erreur réseau. `httpStatus` et `lastError` remplis. | Worker. | Retentée automatiquement après `60 s x tentatives` (plafond 1 h), sans limite de tentatives ; ou relance manuelle. |

Un point de terminaison n'a pas de statut mais un booléen `active` (activer et désactiver par route ; pas de modification ni de suppression).

## Parcours

### Cloche du back-office

1. Au chargement de l'interface : `GET /sni/api/v1/admin/notifications/unread-count` renvoie `{ "count": 3 }` ([route](routes/AdminNotificationController.md#r-get-sni-api-v1-admin-notifications-unread-count)). Afficher le badge.
2. À l'ouverture du panneau : `GET /sni/api/v1/admin/notifications/unread?limit=20` ([route](routes/AdminNotificationController.md#r-get-sni-api-v1-admin-notifications-unread)). Liste des notifications `IN_APP` non lues du compte connecté, la plus récente d'abord (`sentAt` décroissant). `limit` est ramené entre 1 et 50. Afficher `subject` comme titre, `createdAt` ou `sentAt` comme date ; le détail métier est dans `payloadJson` (texte JSON à analyser, contenu variable selon `eventType`, voir [Effets automatiques](#effets-automatiques)). `aggregateType` + `aggregateId` permettent d'ouvrir l'objet concerné (par exemple `SUPPORT_TICKET` + numéro de ticket).
3. Au clic sur une notification : `PATCH /sni/api/v1/admin/notifications/{notificationNumber}/read` ([route](routes/AdminNotificationController.md#r-patch-sni-api-v1-admin-notifications-notificationnumber-read)). Renvoie la notification avec `readAt` rempli. Décrémenter le badge côté écran (le serveur ne pousse aucune décrémentation).
4. « Tout marquer comme lu » : `PATCH` (ou `POST`) `/sni/api/v1/admin/notifications/read-all` ([route](routes/AdminNotificationController.md#r-patch-post-sni-api-v1-admin-notifications-read-all)). Réponse `204` sans corps. Remettre le badge à 0. Attention : marque comme lues **toutes** les notifications de l'adresse, tous canaux confondus, courriels compris.
5. Temps réel : voir [Temps réel](#temps-réel-websocket).

Les routes `GET /sni/api/v1/notifications/unread` et `PATCH /sni/api/v1/notifications/mark-all-read` font presque la même chose, mais la première renvoie aussi les notifications `EMAIL` du compte (pas de filtre de canal) et la seconde renvoie `{ "updated": n }`. Pour la cloche, utiliser les routes `/admin/notifications`.

### Cloche du portail membre (module portal)

Pour mémoire, car elle exploite les mêmes données : `GET /sni/api/v1/client/notifications?limit=20`, `GET /sni/api/v1/client/notifications/unread-count`, `PATCH /sni/api/v1/client/notifications/{notificationNumber}/read` (`204`, vérifie que la notification appartient au membre, sinon `404` « Notification introuvable : ... »), `PATCH|POST /sni/api/v1/client/notifications/read-all` (`204`). La liste renvoie un modèle réduit `{ notificationNumber, title, message, createdAt, read }` où `message` est une phrase en français construite par le serveur pour les événements de réservation, d'abonnement, de contrat et de facture (`src/main/java/com/sni/bokaticowork/features/portal/notification/service/ClientNotificationFormatter.java:100`), vide pour les autres. Détails : [portal/routes/ClientNotificationController.md](../portal/routes/ClientNotificationController.md).

### Journal des notifications (supervision)

1. `GET /sni/api/v1/notifications?status=FAILED&channel=EMAIL&eventType=...&recipientEmail=...&search=...&page=0&size=20` ([route](routes/NotificationController.md#r-get-sni-api-v1-notifications)). Réponse `Page<NotificationMessageResponse>` (page Spring, éléments dans `content`). Tri imposé : `createdAt` décroissant (requête native, le paramètre `sort` n'a pas d'effet utile). `search` cherche, sans tenir compte de la casse, dans le numéro, l'adresse, le nom du destinataire, `eventType` et `aggregateId`. `eventType` est comparé en égalité stricte après mise en majuscules ; `recipientEmail` en égalité après mise en minuscules.
2. Colonnes utiles : `notificationNumber`, `channel`, `eventType`, `recipientEmail`, `subject`, `status`, `attempts`, `sentAt`, `readAt`, `lastError`.
3. Sur une ligne non `SENT` : bouton « Relancer », `PATCH /sni/api/v1/notifications/{notificationNumber}/retry` ([route](routes/NotificationController.md#r-patch-sni-api-v1-notifications-notificationnumber-retry)). La notification repasse `PENDING`, `lastError` est vidé ; elle part au prochain passage du worker (30 s au plus). Sur une ligne `SENT`, la relance est refusée en `400`.

### Envoyer une notification à la main

1. `POST /sni/api/v1/notifications` ([route](routes/NotificationController.md#r-post-sni-api-v1-notifications)) avec au minimum `eventType` et `channel` ; `recipientEmail` est obligatoire pour `EMAIL` et indispensable pour `IN_APP` (sans adresse, personne ne la voit). Facultatifs : `subject`, `recipientName`, `recipientType`, `aggregateType`, `aggregateId`, `templateCode` (par défaut égal à `eventType`), `templateName`, `payload` (objet libre), `availableAt` (date-heure avec fuseau, pour un envoi différé).
2. Réponse `{ "notificationNumber": "...", "webhookDeliveries": 2 }` : le numéro créé et le nombre de livraisons webhook programmées pour cet `eventType`.
3. Si `subject` est vide et qu'aucun modèle actif ne le fournit, l'objet devient `Notification <EVENT_TYPE>`. Le gabarit de courriel par défaut est `generic-notification` (`src/main/resources/templates/email/generic-notification.html`).

### Publier un événement

`POST /sni/api/v1/notifications/events` ([route](routes/NotificationController.md#r-post-sni-api-v1-notifications-events)) avec `eventType`, `aggregateType`, `aggregateId` (obligatoires) et `payload`. Réponse `202` sans corps. L'événement est écrit dans la file `outbox` (traitée toutes les 10 s). Il ne produit une notification que si `eventType` commence par `NOTIFICATION_` ou si `aggregateType` fait partie de `NOTIFICATION`, `SUBSCRIPTION`, `SUBSCRIPTION_PASS`, `PAYMENT`, `BILLING`, `BILLING_DOCUMENT`, `INVOICE`, `QUOTE`, `WALLET`, `DOMICILIATION`, `SUBSCRIPTION_QUOTE`, `CASH_REGISTER`, `INVENTORY`, `ADMIN` (`src/main/java/com/sni/bokaticowork/core/outbox/service/implementation/NotificationOutboxEventProcessor.java:24`). Le traitement crée alors un **courriel** si le `payload` contient `recipientEmail` ou `adminEmail`, et programme les webhooks abonnés. Clés reconnues dans `payload` : `recipientEmail`, `adminEmail`, `recipientName`, `recipientCode`, `recipientType` (valeur de l'énumération), `subject`, `templateCode` ou `notificationTemplateCode`, `templateName` ; le reste sert de variables au gabarit.

### Modèles de notification

1. Liste : `GET /sni/api/v1/notifications/templates?channel=EMAIL&active=true` ([route](routes/NotificationController.md#r-get-sni-api-v1-notifications-templates)), triée par `templateCode`.
2. Créer ou modifier : `POST /sni/api/v1/notifications/templates` ([route](routes/NotificationController.md#r-post-sni-api-v1-notifications-templates)) avec `templateCode` et `channel` obligatoires. C'est un « créer ou remplacer » : si le code existe (casse ignorée), **tous** les champs sont réécrits, un champ omis est vidé ; `active` omis vaut `true`, `adminOnly` omis vaut `false`. Pas de suppression : désactiver avec `active: false`.
3. Effet : un modèle s'applique à une notification dont le `templateCode` (par défaut son `eventType`) et le canal correspondent. `subject` remplace l'objet, `templateName` le gabarit ; `bodyTemplate`, s'il est rempli, est un gabarit **Thymeleaf en ligne** utilisé pour le corps du courriel, avec comme variables les clés de `payload` plus `recipientName`, `eventType`, `subject`, `aggregateId`, `aggregateType`. Un gabarit invalide fait passer la notification en `FAILED`.
4. `adminOnly` est enregistré et renvoyé mais n'est lu par aucune règle.

### Envois de test

- `POST /sni/api/v1/notifications/test/email` ([route](routes/NotificationController.md#r-post-sni-api-v1-notifications-test-email)) avec `to` (adresse valide, obligatoire), `subject`, `content`, `html`. Met directement un courriel en file (sans passer par la table des notifications) et répond `202` avec le suivi `EmailDeliveryResponse` (`emailNumber`, `status`…), consultable ensuite dans le journal des courriels du [socle](../socle/README.md). Valeurs par défaut : objet « Test email Bokati », contenu « Ceci est un email de test envoye par Bokati. ».
- `POST /sni/api/v1/notifications/test/notification` ([route](routes/NotificationController.md#r-post-sni-api-v1-notifications-test-notification)) avec `to` (obligatoire), `recipientName`, `subject`, `message`, `payload`. Crée une vraie notification `EMAIL` d'`eventType` `NOTIFICATION_TEST`, gabarit `generic-notification`, et répond comme l'envoi manuel.

### Webhooks sortants

1. Liste : `GET /sni/api/v1/webhook-endpoints?active=true&search=...` ([route](routes/WebhookController.md#r-get-sni-api-v1-webhook-endpoints)), liste simple (non paginée), plus récent d'abord. `search` porte sur le code, le nom et l'URL.
2. Création : `POST /sni/api/v1/webhook-endpoints` ([route](routes/WebhookController.md#r-post-sni-api-v1-webhook-endpoints)) avec `name` et `url` obligatoires, `secret` (facultatif), `eventTypes` (liste ; vide ou absente = tous les événements, renvoyée `["*"]`), `active` (défaut `true`). La réponse donne l'`endpointCode`, pas le secret : l'écran doit prévenir qu'il ne sera plus affiché.
3. Activer ou désactiver : `PATCH /sni/api/v1/webhook-endpoints/{endpointCode}/activate` ou `/deactivate`. Pas de route de modification ni de suppression : pour changer l'URL ou les événements, créer un nouveau point de terminaison et désactiver l'ancien.
4. Livraisons : `GET /sni/api/v1/webhook-deliveries?status=FAILED&endpointCode=...&eventType=...&page=0&size=20` ([route](routes/WebhookController.md#r-get-sni-api-v1-webhook-deliveries)), page Spring. Le tri envoyé est ignoré (le serveur ne garde que page et taille, `WebhookServiceImpl.java:107`). Colonnes utiles : `deliveryNumber`, `endpointName`, `eventType`, `aggregateId`, `status`, `httpStatus`, `attempts`, `lastError`, `deliveredAt`.
5. Relance : `PATCH /sni/api/v1/webhook-deliveries/{deliveryNumber}/retry` ([route](routes/WebhookController.md#r-patch-sni-api-v1-webhook-deliveries-deliverynumber-retry)) : repasse en `PENDING`, envoyée au prochain passage du worker (60 s au plus).

Ce que reçoit le tiers (utile pour l'écran d'aide) : `POST` du `payloadJson` brut, en-têtes `Content-Type: application/json`, `X-Bokati-Event: <EVENT_TYPE>`, `X-Bokati-Delivery: <deliveryNumber>`, et, si un secret est défini, `X-Bokati-Signature: sha256=<HMAC-SHA256 hexadécimal du corps avec le secret>` (`src/main/java/com/sni/bokaticowork/features/notification/service/support/WebhookSignatureSupport.java:27`).

## Temps réel (WebSocket)

Connexion : STOMP sur `/ws` avec SockJS, en-tête `Authorization: Bearer <accessToken>` dans la trame `CONNECT` (règles communes : [README racine](../README.md#temps-réel)). Points vérifiés dans `src/main/java/com/sni/bokaticowork/core/configuration/` :

- `WebSocketAuthInterceptor.java:349` : seul un **jeton d'accès** est accepté ; un jeton de rafraîchissement, une session révoquée (déconnexion), un compte désactivé ou un jeton expiré font **refuser** la connexion. Un `CONNECT` sans en-tête ouvre une connexion anonyme sur laquelle tout abonnement est refusé. Après un rafraîchissement du jeton, se reconnecter avec le nouveau jeton.
- `WebSocketDestinations.java:198` et `WebSocketSubscriptionInterceptor.java:273` : tout abonnement exige une identité ; `/topic/admin/`, `/topic/inventory/`, `/topic/support/` exigent en plus un rôle d'administration. Un refus d'abonnement lève une erreur STOMP (trame `ERROR`).
- `WebSocketConfig.java:144` : battement SockJS 25 s, message entrant limité à 64 Ko. Préfixes : `/app` (envois), `/user` (files personnelles), courtier sur `/topic` et `/queue` (courtier simple en mémoire ou relais RabbitMQ STOMP selon `bokati.websocket.use-stomp-relay`, activé en dev et en prod).

Destinations publiées par ce module et ses voisins (`src/main/java/com/sni/bokaticowork/core/event/WebSocketTopics.java`) :

| S'abonner à | Qui | Contenu reçu |
| --- | --- | --- |
| `/user/queue/notifications` | Tout compte connecté (admin ou membre) | Une trame par nouvelle notification `IN_APP` adressée à l'adresse du compte : `{ notificationNumber, title, eventType, aggregateType, aggregateId, payloadJson, createdAt, read: false }` (`ClientNotificationPayload`, `title` = `subject`). |
| `/user/queue/notifications/unread-count` | Tout compte connecté | `{ "action": "INCREMENT" }` à chaque nouvelle notification `IN_APP`. Aucun autre message (ni décrément, ni valeur absolue). |
| `/topic/admin/alerts` | Back-office seulement | Diffusion à tout le back-office : `{ alertType, module, title, message, severity, payloadJson, createdAt }` (`AdminAlertEvent`). Non persistée : une alerte manquée hors connexion est perdue. |

Alertes diffusées sur `/topic/admin/alerts` (vérifiées dans le code) :

| `alertType` | `module` | `severity` | Déclencheur |
| --- | --- | --- | --- |
| `BILLING_OVERDUE` | `BILLING` | `WARNING` | Worker horaire qui passe des factures en retard (`features/billing/worker/BillingOverdueWorker.java:23`). |
| `CASH_ANOMALY` | `PAYMENT` | `CRITICAL` | Anomalie détectée à la clôture d'une session de caisse (`features/payment/service/support/CashSessionAnomalyDetectionListener.java:26`). |
| `CASH_PAYMENT_DECLARED` | `PAYMENT` | `WARNING` | Un client annonce un paiement en espèces (`features/payment/cash/service/CashDeclarationNotifier.java:73`). |
| `SELF_SERVICE_PAYMENT_RECEIVED` | `PAYMENT` | `INFO` | Paiement reçu depuis l'espace client (`features/payment/service/support/SelfServicePaymentNotifier.java:94`). |

La file `/topic/inventory/alerts` et les sujets `/topic/support/tickets/...` sont décrits dans les README `inventory` et `support`.

Conduite conseillée : à chaque (re)connexion, recharger le compteur et la liste par les routes REST (le serveur considère le temps réel comme « au mieux » : un échec de diffusion est ignoré, `NotificationInAppConsumer.java:178`), puis incrémenter le badge et insérer la trame reçue en tête de liste.

## Règles et erreurs à refléter dans l'écran

| Situation | Statut | Message (`message`) |
| --- | --- | --- |
| `eventType` ou `channel` manquant à l'envoi | `400` `VALIDATION_FAILED` | détail dans `errors` (validation du corps) |
| `channel: EMAIL` sans `recipientEmail` | `400` | `recipientEmail is required for email notifications` (en anglais) |
| `to` absent ou mal formé (tests) | `400` `VALIDATION_FAILED` | détail dans `errors` |
| Relance d'une notification `SENT` | `400` | `Notification <n> was already sent on <date> and cannot be retried` (en anglais) |
| Numéro de notification inconnu (relance) | `404` | `Notification <n> not found` |
| Numéro inconnu (marquer comme lu) | `404` | `Notification introuvable : <n>` |
| `name` ou `url` de webhook manquant | `400` `VALIDATION_FAILED` | détail dans `errors` |
| Point de terminaison ou livraison inconnu | `404` | `Webhook endpoint <code> not found` / `Webhook delivery <n> not found` |
| Valeur d'énumération inconnue en filtre (`status`, `channel`) | `400` `TYPE_MISMATCH` | |

Autres règles :

- `eventType`, `aggregateType`, `templateCode` et les `eventTypes` des webhooks sont mis en **majuscules** par le serveur ; les adresses en minuscules. Afficher les valeurs renvoyées, pas celles saisies.
- Une `availableAt` dans le futur programme l'envoi : la réponse arrive tout de suite avec le numéro, la notification reste `PENDING` jusqu'à l'échéance (au plus 30 s de retard).
- La correspondance webhook / événement est une recherche de sous-chaîne : un point de terminaison abonné à `INVOICE_PAID` reçoit aussi l'événement `INVOICE` ou `PAID` (voir Écarts). Conseiller de saisir des types complets.
- L'URL d'un webhook n'est pas validée à la création : une URL invalide n'échoue qu'à la livraison (`FAILED`, `lastError` renseigné).

## À prévoir côté frontend

Écrans back-office (droit `ADMIN:ACCESS` pour toutes les routes du module ; masquer l'entrée de menu sans ce droit) :

1. **Cloche** dans l'en-tête : compteur, panneau des non lues, « tout marquer comme lu », abonnement WebSocket `/user/queue/notifications` et `/user/queue/notifications/unread-count`.
2. **Bandeau ou toasts d'alertes** : abonnement `/topic/admin/alerts`, couleur selon `severity` (`INFO`, `WARNING`, `CRITICAL`).
3. **Journal des notifications** : table paginée avec filtres `status`, `channel`, `eventType`, recherche, détail (afficher `payloadJson` formaté et `lastError`), bouton « Relancer » masqué si `status` est `SENT`.
4. **Modèles** : liste filtrable par canal et activité, formulaire créer ou modifier (envoyer tous les champs à chaque enregistrement, puisque le serveur réécrit tout), aide sur les variables disponibles.
5. **Webhooks** : liste des points de terminaison avec bascule active / inactive, formulaire de création (secret affiché une seule fois), table des livraisons avec relance.
6. **Outils de test** : envoi d'un courriel de test et d'une notification de test, avec lien vers le journal des courriels.

Pièges :

- Identifiants : uniquement des codes (`notificationNumber`, `endpointCode`, `deliveryNumber`, `templateCode`) ; aucun `id` numérique n'est exposé.
- `payloadJson` est une **chaîne** contenant du JSON, pas un objet : l'analyser avant usage, et tolérer `null` ou un contenu inattendu.
- Les deux routes de liste paginée renvoient une page Spring (`content`), pas la page maison (`data`).
- Les routes `/notifications/{notificationNumber}/read` et `/admin/notifications/{notificationNumber}/read` ne vérifient pas le destinataire : n'appeler que pour les notifications du compte connecté.
- Le badge n'est jamais remis à jour par le serveur après une lecture faite dans un autre onglet : recharger le compteur au retour sur l'onglet.
- L'en-tête `Idempotency-Key` n'est pas pris en charge par ces routes : protéger les boutons d'envoi contre le double clic.

## Effets automatiques

| Automatisme | Fréquence | Effet visible |
| --- | --- | --- |
| `NotificationWorker` (`features/notification/worker/NotificationWorker.java`) | toutes les 30 s (`bokati.notification.worker.delay-ms`), lot de 25 | Envoie les notifications `PENDING` échues et retente les `FAILED` dont `availableAt` est passée. |
| `WebhookDeliveryWorker` (`features/notification/worker/WebhookDeliveryWorker.java`) | toutes les 60 s (`bokati.webhook.worker.delay-ms`), lot de 25 | Appelle les points de terminaison, passe les livraisons en `DELIVERED` ou `FAILED`. |
| Traitement de l'outbox (socle) | toutes les 10 s | Transforme les événements des modules listés plus haut en courriels et livraisons webhook. |
| Consommateur RabbitMQ `notification.inapp` | à la réception | Pousse la notification `IN_APP` et l'incrément du compteur sur le WebSocket du destinataire. |
| Consommateur RabbitMQ `notification.admin` et file d'erreurs `notification.dlq` | à la réception | Journalisation serveur seulement, rien côté écran. |

Notifications `IN_APP` créées par les autres modules (vérifié par recherche dans le code) :

| `eventType` | Destinataire | Origine |
| --- | --- | --- |
| `BOOKING_CREATED`, `BOOKING_CONFIRMED`, `BOOKING_CANCELLED` et autres changements de statut de réservation | Contact de la réservation (membre) | `features/booking/service/implementation/BookingServiceImpl.java:1128` |
| `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_RENEWAL_PAYMENT_DUE` et autres événements d'abonnement | Membre | `features/subscription/subscription/service/support/subscription/SubscriptionCreationOperator.java:179`, `SubscriptionLifecycleOperator.java:655`, `:810` |
| `CONTRACT_SIGNING_REQUESTED`, `CONTRACT_SIGNED_VIA_ESIGN` | Membre | `features/contract/service/implementation/ContractSigningServiceImpl.java:121`, `:170` |
| `BILLING_DOCUMENT_ISSUED` | Membre | `features/billing/service/implementation/BillingDocumentServiceImpl.java:1030` |
| `CASH_PAYMENT_DECLARED` | Adresses de caisse (`bokati.payment.cash-declaration.desk-email`) | `features/payment/cash/service/CashDeclarationNotifier.java:68` |
| `SELF_SERVICE_PAYMENT_RECEIVED` | Adresses `bokati.payment.self-service.confirmation-email` | `features/payment/service/support/SelfServicePaymentNotifier.java:89` |
| `SUPPORT_TICKET_ROUTED`, `SUPPORT_CLIENT_REPLY` | Agent assigné au ticket | `features/support/service/implementation/SupportTicketServiceImpl.java:824`, `:874` |
| `PASSWORD_RESET_COMPLETED` | Administrateur à l'origine de la réinitialisation | `security/service/passwordResetService/support/PasswordResetNotifier.java:58` |

## Écarts relevés

1. **Livraisons webhook en double** pour les événements de l'outbox : `createForEvent` programme les webhooks (`src/main/java/com/sni/bokaticowork/features/notification/service/support/NotificationDispatchSupport.java:76`), puis appelle `create` qui les programme une seconde fois (`:60`, appelé à `:103`) dès que le payload porte une adresse. De même, une notification de canal `WEBHOOK` est programmée dans `create` (`:60`) puis à nouveau par `NotificationWebhookConsumer.java:17`. Le tiers peut recevoir deux fois le même événement, avec deux `deliveryNumber` différents.
2. **Marquer comme lu sans contrôle du destinataire** côté back-office : `AdminNotificationController.java:43` et `NotificationController.java:73` appellent `markAsRead` et non `markAsReadFor` (qui existe, `NotificationServiceImpl.java:128`) : tout compte `ADMIN:ACCESS` peut marquer lue la notification d'un autre, y compris d'un membre, s'il connaît son numéro.
3. **Correspondance webhook par sous-chaîne** : `WebhookEndpointRepository.java:21` compare `event_types ILIKE '%<eventType>%'` sur la liste jointe par virgules ; un abonnement à `BOOKING_CREATED` capte aussi un événement `BOOKING` ou `CREATED`.
4. **Relance webhook sans contrôle d'état** : `WebhookServiceImpl.java:113` remet en `PENDING` une livraison déjà `DELIVERED`, qui sera renvoyée au tiers. À l'inverse, une notification n'est protégée que contre `SENT` (`NotificationDispatchSupport.java:129`).
5. **Pas de plafond de tentatives** : notifications et livraisons `FAILED` sont retentées indéfiniment toutes les heures au plus (`NotificationDispatchSupport.java:310`, `WebhookServiceImpl.java:178`). Une URL morte génère une tentative par heure sans fin.
6. `GET /sni/api/v1/notifications/unread` borne `limit` par le haut seulement (`NotificationController.java:70`) : `limit=0` ou négatif provoque une erreur de `Pageable.ofSize` (non vérifié : statut exact renvoyé, probablement `500`). La route `/admin/notifications/unread` borne correctement.
7. Pour un courriel, `SENT` est posé avant la remise réelle ; un échec de mise en file après validation n'est que journalisé et la notification reste `SENT` (`NotificationDispatchSupport.java:266`).
8. Statuts `DELIVERED` (pour les notifications), `CANCELLED` et `SKIPPED` jamais atteints ; champ `adminOnly` des modèles jamais lu.
9. Messages d'erreur en anglais (`recipientEmail is required...`, `Notification ... not found`, `Webhook endpoint ... not found`), et deux messages différents pour un même numéro introuvable selon la route.
10. La trame WebSocket `/user/queue/notifications` n'a pas le champ `message` construit pour la liste du portail : un membre qui l'affiche telle quelle n'a que le titre.
11. Non vérifié : comportement de `POST /sni/api/v1/notifications` et `/test/notification` quand la validation du service échoue, car l'envoi est `@Async` et l'exception remonte enveloppée dans le `CompletableFuture` (`NotificationServiceImpl.java:45`) ; le statut renvoyé pourrait être `500` au lieu de `400`.
12. Non vérifié : avec le relais RabbitMQ STOMP, la livraison sur la destination personnelle `/queue/notifications/unread-count` (nom contenant une barre oblique) n'a pas été testée.
