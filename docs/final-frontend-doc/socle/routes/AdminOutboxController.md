# AdminOutboxController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-outbox-events"></a>
### GET /sni/api/v1/admin/outbox-events

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `PaginatedResponse<OutboxEvent>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [OutboxEvent](#m-OutboxEvent), [OutboxEventStatus](#m-OutboxEventStatus)
- Code : `src/main/java/com/sni/bokaticowork/core/outbox/controller/AdminOutboxController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `eventType` | requête | `String` |  |  |
| `aggregateType` | requête | `String` |  |  |
| `aggregateId` | requête | `String` |  |  |
| `status` | requête | `OutboxEventStatus` |  |  |
| `createdFrom` | requête | `Instant` |  |  |
| `createdTo` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-admin-outbox-events-process"></a>
### POST /sni/api/v1/admin/outbox-events/process

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/core/outbox/controller/AdminOutboxController.java`, méthode `process`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchSize` | requête | `int` |  | 25 |

<a id="r-post-sni-api-v1-admin-outbox-events-requeue-failed"></a>
### POST /sni/api/v1/admin/outbox-events/requeue-failed

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/core/outbox/controller/AdminOutboxController.java`, méthode `requeueFailed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `aggregateType` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-admin-outbox-events-id-requeue"></a>
### POST /sni/api/v1/admin/outbox-events/{id}/requeue

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/core/outbox/controller/AdminOutboxController.java`, méthode `requeue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-OutboxEvent"></a>
#### OutboxEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `eventType` | texte |  |  |
| `aggregateType` | texte |  |  |
| `aggregateId` | texte |  |  |
| `payload` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED`, `DEAD` |  |  |
| `attemptCount` | entier |  |  |
| `availableAt` | date-heure ISO 8601 UTC |  |  |
| `lastError` | texte |  |  |
| `publishedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-OutboxEventStatus"></a>
#### OutboxEventStatus

Énumération : `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED`, `DEAD`
