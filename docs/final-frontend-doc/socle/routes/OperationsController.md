# OperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-errors"></a>
### GET /sni/api/v1/admin/audit/errors

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `PaginatedResponse<OperationsService.AppError>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [OperationsService.AppError](#m-OperationsService-AppError)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/ops/OperationsController.java`, méthode `errors`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `path` | requête | `String` |  |  |
| `exception` | requête | `String` |  |  |
| `requestId` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

<a id="r-get-sni-api-v1-admin-audit-errors-id"></a>
### GET /sni/api/v1/admin/audit/errors/{id}

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `OperationsService.AppError`
- Modèles : [OperationsService.AppError](#m-OperationsService-AppError)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/ops/OperationsController.java`, méthode `error`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `long` (code identifiant ou nombre) | oui |  |

<a id="r-get-sni-api-v1-admin-audit-health"></a>
### GET /sni/api/v1/admin/audit/health

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/core/audit/ops/OperationsController.java`, méthode `health`

<a id="r-get-sni-api-v1-admin-audit-jobs"></a>
### GET /sni/api/v1/admin/audit/jobs

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<OperationsService.TaskStatus>`
- Modèles : [OperationsService.TaskStatus](#m-OperationsService-TaskStatus)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/ops/OperationsController.java`, méthode `jobs`

<a id="r-get-sni-api-v1-admin-audit-jobs-runs"></a>
### GET /sni/api/v1/admin/audit/jobs/runs

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `PaginatedResponse<OperationsService.TaskRun>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [OperationsService.TaskRun](#m-OperationsService-TaskRun)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/ops/OperationsController.java`, méthode `runs`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `task` | requête | `String` |  |  |
| `outcome` | requête | `String` |  |  |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-OperationsService-AppError"></a>
#### OperationsService.AppError

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `traceId` | texte |  |  |
| `requestId` | texte |  |  |
| `httpMethod` | texte |  |  |
| `path` | texte |  |  |
| `status` | entier |  |  |
| `errorCode` | texte |  |  |
| `exceptionClass` | texte |  |  |
| `message` | texte |  |  |
| `rootCause` | texte |  |  |
| `userEmail` | texte |  |  |
| `clientIp` | texte |  |  |
| `stackTrace` | texte |  |  |

<a id="m-OperationsService-TaskStatus"></a>
#### OperationsService.TaskStatus

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `taskName` | texte |  |  |
| `lastStartedAt` | date-heure ISO 8601 UTC |  |  |
| `lastFinishedAt` | date-heure ISO 8601 UTC |  |  |
| `lastDurationMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `lastOutcome` | texte |  |  |
| `lastError` | texte |  |  |
| `lastFailureAt` | date-heure ISO 8601 UTC |  |  |
| `runCount` | entier int64 (texte au-delà de 2^53) |  |  |
| `failureCount` | entier int64 (texte au-delà de 2^53) |  |  |
| `consecutiveFailures` | entier |  |  |
| `averageDurationMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `maxDurationMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `instance` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-OperationsService-TaskRun"></a>
#### OperationsService.TaskRun

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `taskName` | texte |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `finishedAt` | date-heure ISO 8601 UTC |  |  |
| `durationMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `outcome` | texte |  |  |
| `error` | texte |  |  |
| `reason` | texte |  |  |
| `instance` | texte |  |  |
