# AdminIdempotencyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-idempotency-records"></a>
### GET /sni/api/v1/admin/idempotency-records

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `PaginatedResponse<IdempotencyRecord>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [IdempotencyRecord](#m-IdempotencyRecord), [IdempotencyStatus](#m-IdempotencyStatus)
- Code : `src/main/java/com/sni/bokaticowork/core/idempotency/controller/AdminIdempotencyController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `operation` | requête | `String` |  |  |
| `idempotencyKey` | requête | `String` |  |  |
| `status` | requête | `IdempotencyStatus` |  |  |
| `createdFrom` | requête | `Instant` |  |  |
| `createdTo` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-IdempotencyRecord"></a>
#### IdempotencyRecord

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `operation` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `requestHash` | texte |  |  |
| `status` | enum : `PROCESSING`, `COMPLETED`, `FAILED` |  |  |
| `responseBody` | texte |  |  |
| `errorBody` | texte |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-IdempotencyStatus"></a>
#### IdempotencyStatus

Énumération : `PROCESSING`, `COMPLETED`, `FAILED`
