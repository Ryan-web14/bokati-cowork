# TaskManagementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [task](../README.md) · 15 route(s).

## Routes

<a id="r-get-sni-api-v1-tasks"></a>
### GET /sni/api/v1/tasks

- Accès : back-office : rôle d'administration et droit `TASK:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:READ','TASK_READ')`
- Réponse : `PaginatedResponse<TaskDtos.TaskResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskStatus](#m-TaskStatus), [TaskPriority](#m-TaskPriority)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `TaskStatus` |  |  |
| `assignedTo` | requête | `Long` |  |  |
| `priority` | requête | `TaskPriority` |  |  |
| `sourceType` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-tasks"></a>
### POST /sni/api/v1/tasks

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `TaskDtos.CreateTaskRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.CreateTaskRequest](#m-TaskDtos-CreateTaskRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `create`

<a id="r-get-sni-api-v1-tasks-board"></a>
### GET /sni/api/v1/tasks/board

- Accès : back-office : rôle d'administration et droit `TASK:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:READ','TASK_READ')`
- Réponse : `TaskDtos.TaskKanbanBoardResponse`
- Modèles : [TaskDtos.TaskKanbanBoardResponse](#m-TaskDtos-TaskKanbanBoardResponse), [TaskPriority](#m-TaskPriority)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `board`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assignedTo` | requête | `Long` |  |  |
| `priority` | requête | `TaskPriority` |  |  |
| `sourceType` | requête | `String` |  |  |
| `searchText` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-tasks-due-today"></a>
### GET /sni/api/v1/tasks/due-today

- Accès : back-office : rôle d'administration et droit `TASK:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:READ','TASK_READ')`
- Réponse : `List<TaskDtos.TaskResponse>`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `dueToday`

<a id="r-get-sni-api-v1-tasks-metrics"></a>
### GET /sni/api/v1/tasks/metrics

- Accès : back-office : rôle d'administration et droit `TASK:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:READ','TASK_READ')`
- Réponse : `TaskDtos.TaskMetricsResponse`
- Modèles : [TaskDtos.TaskMetricsResponse](#m-TaskDtos-TaskMetricsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `metrics`

<a id="r-get-sni-api-v1-tasks-id"></a>
### GET /sni/api/v1/tasks/{id}

- Accès : back-office : rôle d'administration et droit `TASK:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:READ','TASK_READ')`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-tasks-id"></a>
### PUT /sni/api/v1/tasks/{id}

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `TaskDtos.UpdateTaskRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.UpdateTaskRequest](#m-TaskDtos-UpdateTaskRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-tasks-id-archive"></a>
### PATCH /sni/api/v1/tasks/{id}/archive

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `archive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-tasks-id-assign"></a>
### PATCH /sni/api/v1/tasks/{id}/assign

- Accès : back-office : rôle d'administration et droit `TASK:ASSIGN` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:ASSIGN','TASK_ASSIGN')`
- Corps JSON (obligatoire) : `TaskDtos.AssignTaskRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.AssignTaskRequest](#m-TaskDtos-AssignTaskRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-tasks-id-checklist"></a>
### POST /sni/api/v1/tasks/{id}/checklist

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `TaskDtos.ChecklistRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.ChecklistRequest](#m-TaskDtos-ChecklistRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `addChecklistItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-tasks-id-checklist-itemid"></a>
### DELETE /sni/api/v1/tasks/{id}/checklist/{itemId}

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `removeChecklistItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `itemId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-tasks-id-checklist-itemid"></a>
### PATCH /sni/api/v1/tasks/{id}/checklist/{itemId}

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `TaskDtos.UpdateChecklistItemRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.UpdateChecklistItemRequest](#m-TaskDtos-UpdateChecklistItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `updateChecklistItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `itemId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-tasks-id-comments"></a>
### POST /sni/api/v1/tasks/{id}/comments

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `TaskDtos.AddTaskCommentRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.AddTaskCommentRequest](#m-TaskDtos-AddTaskCommentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `comment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-tasks-id-complete"></a>
### PATCH /sni/api/v1/tasks/{id}/complete

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `complete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-tasks-id-status"></a>
### PATCH /sni/api/v1/tasks/{id}/status

- Accès : back-office : rôle d'administration et droit `TASK:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('TASK:WRITE','TASK_WRITE')`
- Corps JSON (obligatoire) : `TaskDtos.UpdateTaskStatusRequest`
- Réponse : `TaskDtos.TaskResponse`
- Modèles : [TaskDtos.TaskResponse](#m-TaskDtos-TaskResponse), [TaskDtos.UpdateTaskStatusRequest](#m-TaskDtos-UpdateTaskStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java`, méthode `updateStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-TaskDtos-TaskResponse"></a>
#### TaskDtos.TaskResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `recurrence` | enum : `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `AFTER_BOOKING` |  |  |
| `parentTaskId` | entier int64 ⚠ |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `checklist` | [TaskDtos.ChecklistResponse](#m-TaskDtos-ChecklistResponse)[] |  |  |
| `comments` | [TaskDtos.TaskCommentResponse](#m-TaskDtos-TaskCommentResponse)[] |  |  |

<a id="m-TaskStatus"></a>
#### TaskStatus

Énumération : `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`

<a id="m-TaskPriority"></a>
#### TaskPriority

Énumération : `LOW`, `MEDIUM`, `HIGH`, `URGENT`

<a id="m-TaskDtos-CreateTaskRequest"></a>
#### TaskDtos.CreateTaskRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `recurrence` | enum : `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `AFTER_BOOKING` |  |  |
| `checklist` | [TaskDtos.ChecklistRequest](#m-TaskDtos-ChecklistRequest)[] |  |  |

<a id="m-TaskDtos-TaskKanbanBoardResponse"></a>
#### TaskDtos.TaskKanbanBoardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `columns` | [TaskDtos.TaskKanbanColumnResponse](#m-TaskDtos-TaskKanbanColumnResponse)[] |  |  |
| `totalTasks` | entier int64 ⚠ |  |  |
| `overdueTasks` | entier int64 ⚠ |  |  |

<a id="m-TaskDtos-TaskMetricsResponse"></a>
#### TaskDtos.TaskMetricsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `totalTasks` | entier int64 ⚠ |  |  |
| `openTasks` | entier int64 ⚠ |  |  |
| `inProgressTasks` | entier int64 ⚠ |  |  |
| `completedTasks` | entier int64 ⚠ |  |  |
| `cancelledTasks` | entier int64 ⚠ |  |  |
| `archivedTasks` | entier int64 ⚠ |  |  |
| `overdueTasks` | entier int64 ⚠ |  |  |
| `completionRate` | nombre décimal |  |  |
| `avgCompletionHours` | nombre décimal |  |  |
| `byPriority` | objet { clé : entier int64 ⚠ } |  |  |
| `bySourceType` | objet { clé : entier int64 ⚠ } |  |  |

<a id="m-TaskDtos-UpdateTaskRequest"></a>
#### TaskDtos.UpdateTaskRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `recurrence` | enum : `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `AFTER_BOOKING` |  |  |

<a id="m-TaskDtos-AssignTaskRequest"></a>
#### TaskDtos.AssignTaskRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assignedTo` | entier int64 ⚠ |  |  |

<a id="m-TaskDtos-ChecklistRequest"></a>
#### TaskDtos.ChecklistRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `completed` | booléen |  |  |
| `displayOrder` | entier |  |  |

<a id="m-TaskDtos-UpdateChecklistItemRequest"></a>
#### TaskDtos.UpdateChecklistItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `completed` | booléen |  |  |
| `displayOrder` | entier |  |  |

<a id="m-TaskDtos-AddTaskCommentRequest"></a>
#### TaskDtos.AddTaskCommentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `authorId` | texte |  |  |
| `authorName` | texte |  |  |
| `comment` | texte | oui | longueur min `1` |

<a id="m-TaskDtos-UpdateTaskStatusRequest"></a>
#### TaskDtos.UpdateTaskStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED` | oui |  |

<a id="m-TaskDtos-ChecklistResponse"></a>
#### TaskDtos.ChecklistResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `label` | texte |  |  |
| `completed` | booléen |  |  |
| `displayOrder` | entier |  |  |

<a id="m-TaskDtos-TaskCommentResponse"></a>
#### TaskDtos.TaskCommentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `authorId` | texte |  |  |
| `authorName` | texte |  |  |
| `comment` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-TaskDtos-TaskKanbanColumnResponse"></a>
#### TaskDtos.TaskKanbanColumnResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED` |  |  |
| `count` | entier int64 ⚠ |  |  |
| `cards` | [TaskDtos.TaskKanbanCardResponse](#m-TaskDtos-TaskKanbanCardResponse)[] |  |  |

<a id="m-TaskDtos-TaskKanbanCardResponse"></a>
#### TaskDtos.TaskKanbanCardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `title` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `status` | enum : `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `recurrence` | enum : `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `AFTER_BOOKING` |  |  |
| `parentTaskId` | entier int64 ⚠ |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `checklistTotal` | entier |  |  |
| `checklistDone` | entier |  |  |
| `commentCount` | entier |  |  |
| `overdue` | booléen |  |  |
