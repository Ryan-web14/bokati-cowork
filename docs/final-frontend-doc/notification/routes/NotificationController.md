# NotificationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [notification](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-notifications"></a>
### GET /sni/api/v1/notifications

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Page<NotificationMessageResponse>`
- Modèles : [NotificationMessageResponse](#m-NotificationMessageResponse), [NotificationDeliveryStatus](#m-NotificationDeliveryStatus), [NotificationChannel](#m-NotificationChannel)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `NotificationDeliveryStatus` |  |  |
| `channel` | requête | `NotificationChannel` |  |  |
| `eventType` | requête | `String` |  |  |
| `recipientEmail` | requête | `String` |  |  |
| `search` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  |  |

<a id="r-post-sni-api-v1-notifications"></a>
### POST /sni/api/v1/notifications

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `SendNotificationRequest`
- Réponse : `CompletableFuture<ResponseEntity<NotificationDispatchResponse>>`
- Modèles : [NotificationDispatchResponse](#m-NotificationDispatchResponse), [SendNotificationRequest](#m-SendNotificationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `send`

<a id="r-post-sni-api-v1-notifications-events"></a>
### POST /sni/api/v1/notifications/events

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `PublishNotificationEventRequest`
- Réponse : `vide`
- Modèles : [PublishNotificationEventRequest](#m-PublishNotificationEventRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `publish`

<a id="r-patch-sni-api-v1-notifications-mark-all-read"></a>
### PATCH /sni/api/v1/notifications/mark-all-read

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `markAllRead`

<a id="r-get-sni-api-v1-notifications-templates"></a>
### GET /sni/api/v1/notifications/templates

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<NotificationTemplateResponse>`
- Modèles : [NotificationTemplateResponse](#m-NotificationTemplateResponse), [NotificationChannel](#m-NotificationChannel)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `templates`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `channel` | requête | `NotificationChannel` |  |  |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-notifications-templates"></a>
### POST /sni/api/v1/notifications/templates

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `NotificationTemplateRequest`
- Réponse : `NotificationTemplateResponse`
- Modèles : [NotificationTemplateResponse](#m-NotificationTemplateResponse), [NotificationTemplateRequest](#m-NotificationTemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `upsertTemplate`

<a id="r-post-sni-api-v1-notifications-test-email"></a>
### POST /sni/api/v1/notifications/test/email

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `TestEmailRequest`
- Réponse : `EmailDeliveryResponse`
- Modèles : [EmailDeliveryResponse](#m-EmailDeliveryResponse), [TestEmailRequest](#m-TestEmailRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `testEmail`

<a id="r-post-sni-api-v1-notifications-test-notification"></a>
### POST /sni/api/v1/notifications/test/notification

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `TestNotificationRequest`
- Réponse : `CompletableFuture<ResponseEntity<NotificationDispatchResponse>>`
- Modèles : [NotificationDispatchResponse](#m-NotificationDispatchResponse), [TestNotificationRequest](#m-TestNotificationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `testNotification`

<a id="r-get-sni-api-v1-notifications-unread"></a>
### GET /sni/api/v1/notifications/unread

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<NotificationMessageResponse>`
- Modèles : [NotificationMessageResponse](#m-NotificationMessageResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `unread`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 20 |

<a id="r-patch-sni-api-v1-notifications-notificationnumber-read"></a>
### PATCH /sni/api/v1/notifications/{notificationNumber}/read

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `NotificationMessageResponse`
- Modèles : [NotificationMessageResponse](#m-NotificationMessageResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `markAsRead`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `notificationNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-notifications-notificationnumber-retry"></a>
### PATCH /sni/api/v1/notifications/{notificationNumber}/retry

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `NotificationMessageResponse`
- Modèles : [NotificationMessageResponse](#m-NotificationMessageResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/NotificationController.java`, méthode `retry`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `notificationNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-NotificationMessageResponse"></a>
#### NotificationMessageResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notificationNumber` | texte |  |  |
| `eventType` | texte |  |  |
| `aggregateType` | texte |  |  |
| `aggregateId` | texte |  |  |
| `channel` | enum : `EMAIL`, `IN_APP`, `WEBHOOK` |  |  |
| `recipientType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS`, `ADMIN`, `GUEST`, `SYSTEM` |  |  |
| `recipientCode` | texte |  |  |
| `recipientEmail` | texte |  |  |
| `recipientName` | texte |  |  |
| `subject` | texte |  |  |
| `templateCode` | texte |  |  |
| `templateName` | texte |  |  |
| `payloadJson` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `SENT`, `DELIVERED`, `FAILED`, `CANCELLED`, `SKIPPED` |  |  |
| `attempts` | entier |  |  |
| `availableAt` | date-heure ISO 8601 UTC |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `readAt` | date-heure ISO 8601 UTC |  |  |
| `lastError` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-NotificationDeliveryStatus"></a>
#### NotificationDeliveryStatus

Énumération : `PENDING`, `PROCESSING`, `SENT`, `DELIVERED`, `FAILED`, `CANCELLED`, `SKIPPED`

<a id="m-NotificationChannel"></a>
#### NotificationChannel

Énumération : `EMAIL`, `IN_APP`, `WEBHOOK`

<a id="m-NotificationDispatchResponse"></a>
#### NotificationDispatchResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notificationNumber` | texte |  |  |
| `webhookDeliveries` | entier |  |  |

<a id="m-SendNotificationRequest"></a>
#### SendNotificationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventType` | texte | oui | longueur min `1` |
| `aggregateType` | texte |  |  |
| `aggregateId` | texte |  |  |
| `channel` | enum : `EMAIL`, `IN_APP`, `WEBHOOK` | oui |  |
| `recipientType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS`, `ADMIN`, `GUEST`, `SYSTEM` |  |  |
| `recipientCode` | texte |  |  |
| `recipientEmail` | texte |  |  |
| `recipientName` | texte |  |  |
| `subject` | texte |  |  |
| `templateCode` | texte |  |  |
| `templateName` | texte |  |  |
| `payload` | objet { clé : objet libre } |  |  |
| `availableAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PublishNotificationEventRequest"></a>
#### PublishNotificationEventRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventType` | texte | oui | longueur min `1` |
| `aggregateType` | texte | oui | longueur min `1` |
| `aggregateId` | texte | oui | longueur min `1` |
| `payload` | objet { clé : objet libre } |  |  |

<a id="m-NotificationTemplateResponse"></a>
#### NotificationTemplateResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `channel` | enum : `EMAIL`, `IN_APP`, `WEBHOOK` |  |  |
| `subject` | texte |  |  |
| `templateName` | texte |  |  |
| `bodyTemplate` | texte |  |  |
| `active` | booléen |  |  |
| `adminOnly` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-NotificationTemplateRequest"></a>
#### NotificationTemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte | oui | longueur min `1` |
| `channel` | enum : `EMAIL`, `IN_APP`, `WEBHOOK` | oui |  |
| `subject` | texte |  |  |
| `templateName` | texte |  |  |
| `bodyTemplate` | texte |  |  |
| `active` | booléen |  |  |
| `adminOnly` | booléen |  |  |

<a id="m-EmailDeliveryResponse"></a>
#### EmailDeliveryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `emailNumber` | texte |  |  |
| `provider` | texte |  |  |
| `fromEmail` | texte |  |  |
| `recipientEmail` | texte |  |  |
| `subject` | texte |  |  |
| `bodyType` | texte |  |  |
| `status` | enum : `QUEUED`, `SENDING`, `SENT`, `FAILED` |  |  |
| `attempts` | entier |  |  |
| `lastError` | texte |  |  |
| `relatedType` | texte |  |  |
| `relatedCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `failedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-TestEmailRequest"></a>
#### TestEmailRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `to` | string (email) | oui | longueur min `1` |
| `subject` | texte |  |  |
| `content` | texte |  |  |
| `html` | booléen |  |  |

<a id="m-TestNotificationRequest"></a>
#### TestNotificationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `to` | string (email) | oui | longueur min `1` |
| `recipientName` | texte |  |  |
| `subject` | texte |  |  |
| `message` | texte |  |  |
| `payload` | objet { clé : objet libre } |  |  |
