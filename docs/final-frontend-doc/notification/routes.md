# notification : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

21 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| PATCH POST | `/sni/api/v1/admin/notifications/read-all` | ADMIN:ACCESS | [AdminNotificationController](routes/AdminNotificationController.md#r-patch-post-sni-api-v1-admin-notifications-read-all) |
| GET | `/sni/api/v1/admin/notifications/unread` | ADMIN:ACCESS | [AdminNotificationController](routes/AdminNotificationController.md#r-get-sni-api-v1-admin-notifications-unread) |
| GET | `/sni/api/v1/admin/notifications/unread-count` | ADMIN:ACCESS | [AdminNotificationController](routes/AdminNotificationController.md#r-get-sni-api-v1-admin-notifications-unread-count) |
| PATCH | `/sni/api/v1/admin/notifications/{notificationNumber}/read` | ADMIN:ACCESS | [AdminNotificationController](routes/AdminNotificationController.md#r-patch-sni-api-v1-admin-notifications-notificationnumber-read) |
| GET | `/sni/api/v1/notifications` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-get-sni-api-v1-notifications) |
| POST | `/sni/api/v1/notifications` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-post-sni-api-v1-notifications) |
| POST | `/sni/api/v1/notifications/events` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-post-sni-api-v1-notifications-events) |
| PATCH | `/sni/api/v1/notifications/mark-all-read` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-patch-sni-api-v1-notifications-mark-all-read) |
| GET | `/sni/api/v1/notifications/templates` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-get-sni-api-v1-notifications-templates) |
| POST | `/sni/api/v1/notifications/templates` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-post-sni-api-v1-notifications-templates) |
| POST | `/sni/api/v1/notifications/test/email` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-post-sni-api-v1-notifications-test-email) |
| POST | `/sni/api/v1/notifications/test/notification` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-post-sni-api-v1-notifications-test-notification) |
| GET | `/sni/api/v1/notifications/unread` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-get-sni-api-v1-notifications-unread) |
| PATCH | `/sni/api/v1/notifications/{notificationNumber}/read` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-patch-sni-api-v1-notifications-notificationnumber-read) |
| PATCH | `/sni/api/v1/notifications/{notificationNumber}/retry` | ADMIN:ACCESS | [NotificationController](routes/NotificationController.md#r-patch-sni-api-v1-notifications-notificationnumber-retry) |
| GET | `/sni/api/v1/webhook-deliveries` | ADMIN:ACCESS | [WebhookController](routes/WebhookController.md#r-get-sni-api-v1-webhook-deliveries) |
| PATCH | `/sni/api/v1/webhook-deliveries/{deliveryNumber}/retry` | ADMIN:ACCESS | [WebhookController](routes/WebhookController.md#r-patch-sni-api-v1-webhook-deliveries-deliverynumber-retry) |
| GET | `/sni/api/v1/webhook-endpoints` | ADMIN:ACCESS | [WebhookController](routes/WebhookController.md#r-get-sni-api-v1-webhook-endpoints) |
| POST | `/sni/api/v1/webhook-endpoints` | ADMIN:ACCESS | [WebhookController](routes/WebhookController.md#r-post-sni-api-v1-webhook-endpoints) |
| PATCH | `/sni/api/v1/webhook-endpoints/{endpointCode}/activate` | ADMIN:ACCESS | [WebhookController](routes/WebhookController.md#r-patch-sni-api-v1-webhook-endpoints-endpointcode-activate) |
| PATCH | `/sni/api/v1/webhook-endpoints/{endpointCode}/deactivate` | ADMIN:ACCESS | [WebhookController](routes/WebhookController.md#r-patch-sni-api-v1-webhook-endpoints-endpointcode-deactivate) |
