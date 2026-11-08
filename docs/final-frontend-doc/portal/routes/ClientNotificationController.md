# ClientNotificationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-client-notifications"></a>
### GET /sni/api/v1/client/notifications

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientNotificationResponse>`
- Modèles : [ClientNotificationResponse](#m-ClientNotificationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/notification/controller/ClientNotificationController.java`, méthode `listUnread`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 20 |

<a id="r-patch-post-sni-api-v1-client-notifications-read-all"></a>
### PATCH | POST /sni/api/v1/client/notifications/read-all

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/notification/controller/ClientNotificationController.java`, méthode `markAllRead`

<a id="r-get-sni-api-v1-client-notifications-unread-count"></a>
### GET /sni/api/v1/client/notifications/unread-count

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `Map<String, Long>`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/notification/controller/ClientNotificationController.java`, méthode `countUnread`

<a id="r-patch-sni-api-v1-client-notifications-notificationnumber-read"></a>
### PATCH /sni/api/v1/client/notifications/{notificationNumber}/read

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/notification/controller/ClientNotificationController.java`, méthode `markAsRead`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `notificationNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientNotificationResponse"></a>
#### ClientNotificationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notificationNumber` | texte |  |  |
| `title` | texte |  |  |
| `message` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `read` | booléen |  |  |
