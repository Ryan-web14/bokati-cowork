# AdminNotificationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [notification](../README.md) · 4 route(s).

## Routes

<a id="r-patch-post-sni-api-v1-admin-notifications-read-all"></a>
### PATCH | POST /sni/api/v1/admin/notifications/read-all

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/AdminNotificationController.java`, méthode `markAllRead`

<a id="r-get-sni-api-v1-admin-notifications-unread"></a>
### GET /sni/api/v1/admin/notifications/unread

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<NotificationMessageResponse>`
- Modèles : [NotificationMessageResponse](#m-NotificationMessageResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/AdminNotificationController.java`, méthode `listUnread`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 20 |

<a id="r-get-sni-api-v1-admin-notifications-unread-count"></a>
### GET /sni/api/v1/admin/notifications/unread-count

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Map<String, Long>`
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/AdminNotificationController.java`, méthode `countUnread`

<a id="r-patch-sni-api-v1-admin-notifications-notificationnumber-read"></a>
### PATCH /sni/api/v1/admin/notifications/{notificationNumber}/read

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `NotificationMessageResponse`
- Modèles : [NotificationMessageResponse](#m-NotificationMessageResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/AdminNotificationController.java`, méthode `markAsRead`

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
| `channel` | enum : `EMAIL`, `IN_APP`, `WEBHOOK`, `WHATSAPP` |  |  |
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
