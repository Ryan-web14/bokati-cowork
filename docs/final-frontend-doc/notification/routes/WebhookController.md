# WebhookController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [notification](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-webhook-deliveries"></a>
### GET /sni/api/v1/webhook-deliveries

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Page<WebhookDeliveryResponse>`
- Modèles : [WebhookDeliveryResponse](#m-WebhookDeliveryResponse), [NotificationDeliveryStatus](#m-NotificationDeliveryStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/WebhookController.java`, méthode `deliveries`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `NotificationDeliveryStatus` |  |  |
| `endpointCode` | requête | `String` |  |  |
| `eventType` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  |  |

<a id="r-patch-sni-api-v1-webhook-deliveries-deliverynumber-retry"></a>
### PATCH /sni/api/v1/webhook-deliveries/{deliveryNumber}/retry

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `WebhookDeliveryResponse`
- Modèles : [WebhookDeliveryResponse](#m-WebhookDeliveryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/WebhookController.java`, méthode `retry`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `deliveryNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-webhook-endpoints"></a>
### GET /sni/api/v1/webhook-endpoints

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<WebhookEndpointResponse>`
- Modèles : [WebhookEndpointResponse](#m-WebhookEndpointResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/WebhookController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |
| `search` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-webhook-endpoints"></a>
### POST /sni/api/v1/webhook-endpoints

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `WebhookEndpointRequest`
- Réponse : `WebhookEndpointResponse`
- Modèles : [WebhookEndpointResponse](#m-WebhookEndpointResponse), [WebhookEndpointRequest](#m-WebhookEndpointRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/WebhookController.java`, méthode `create`

<a id="r-patch-sni-api-v1-webhook-endpoints-endpointcode-activate"></a>
### PATCH /sni/api/v1/webhook-endpoints/{endpointCode}/activate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `WebhookEndpointResponse`
- Modèles : [WebhookEndpointResponse](#m-WebhookEndpointResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/WebhookController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `endpointCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-webhook-endpoints-endpointcode-deactivate"></a>
### PATCH /sni/api/v1/webhook-endpoints/{endpointCode}/deactivate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `WebhookEndpointResponse`
- Modèles : [WebhookEndpointResponse](#m-WebhookEndpointResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/notification/controller/WebhookController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `endpointCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WebhookDeliveryResponse"></a>
#### WebhookDeliveryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `deliveryNumber` | texte |  |  |
| `endpointCode` | texte |  |  |
| `endpointName` | texte |  |  |
| `eventType` | texte |  |  |
| `aggregateType` | texte |  |  |
| `aggregateId` | texte |  |  |
| `payloadJson` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `SENT`, `DELIVERED`, `FAILED`, `CANCELLED`, `SKIPPED` |  |  |
| `attempts` | entier |  |  |
| `availableAt` | date-heure ISO 8601 UTC |  |  |
| `deliveredAt` | date-heure ISO 8601 UTC |  |  |
| `httpStatus` | entier |  |  |
| `lastError` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-NotificationDeliveryStatus"></a>
#### NotificationDeliveryStatus

Énumération : `PENDING`, `PROCESSING`, `SENT`, `DELIVERED`, `FAILED`, `CANCELLED`, `SKIPPED`

<a id="m-WebhookEndpointResponse"></a>
#### WebhookEndpointResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `endpointCode` | texte |  |  |
| `name` | texte |  |  |
| `url` | texte |  |  |
| `eventTypes` | texte[] |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WebhookEndpointRequest"></a>
#### WebhookEndpointRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `url` | texte | oui | longueur min `1` |
| `secret` | texte |  |  |
| `eventTypes` | texte[] |  |  |
| `active` | booléen |  |  |
