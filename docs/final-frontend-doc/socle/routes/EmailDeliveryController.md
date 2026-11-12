# EmailDeliveryController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-email-deliveries"></a>
### GET /sni/api/v1/admin/email-deliveries

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `PaginatedResponse<EmailDeliveryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [EmailDeliveryResponse](#m-EmailDeliveryResponse), [EmailDeliveryStatus](#m-EmailDeliveryStatus)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/mailService/controller/EmailDeliveryController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `EmailDeliveryStatus` |  |  |
| `provider` | requête | `String` |  |  |
| `recipientEmail` | requête | `String` |  |  |
| `relatedType` | requête | `String` |  |  |
| `relatedCode` | requête | `String` |  |  |
| `createdFrom` | requête | `Instant` |  |  |
| `createdTo` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-admin-email-deliveries-emailnumber"></a>
### GET /sni/api/v1/admin/email-deliveries/{emailNumber}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `EmailDeliveryResponse`
- Modèles : [EmailDeliveryResponse](#m-EmailDeliveryResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/mailService/controller/EmailDeliveryController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `emailNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-email-deliveries-emailnumber-retry"></a>
### PATCH /sni/api/v1/admin/email-deliveries/{emailNumber}/retry

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `EmailDeliveryResponse`
- Modèles : [EmailDeliveryResponse](#m-EmailDeliveryResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/mailService/controller/EmailDeliveryController.java`, méthode `retry`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `emailNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-EmailDeliveryStatus"></a>
#### EmailDeliveryStatus

Énumération : `QUEUED`, `SENDING`, `SENT`, `FAILED`
