# task : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

15 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/tasks` | TASK:READ | [TaskManagementController](routes/TaskManagementController.md#r-get-sni-api-v1-tasks) |
| POST | `/sni/api/v1/tasks` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-post-sni-api-v1-tasks) |
| GET | `/sni/api/v1/tasks/board` | TASK:READ | [TaskManagementController](routes/TaskManagementController.md#r-get-sni-api-v1-tasks-board) |
| GET | `/sni/api/v1/tasks/due-today` | TASK:READ | [TaskManagementController](routes/TaskManagementController.md#r-get-sni-api-v1-tasks-due-today) |
| GET | `/sni/api/v1/tasks/metrics` | TASK:READ | [TaskManagementController](routes/TaskManagementController.md#r-get-sni-api-v1-tasks-metrics) |
| GET | `/sni/api/v1/tasks/{id}` | TASK:READ | [TaskManagementController](routes/TaskManagementController.md#r-get-sni-api-v1-tasks-id) |
| PUT | `/sni/api/v1/tasks/{id}` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-put-sni-api-v1-tasks-id) |
| PATCH | `/sni/api/v1/tasks/{id}/archive` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-patch-sni-api-v1-tasks-id-archive) |
| PATCH | `/sni/api/v1/tasks/{id}/assign` | TASK:ASSIGN | [TaskManagementController](routes/TaskManagementController.md#r-patch-sni-api-v1-tasks-id-assign) |
| POST | `/sni/api/v1/tasks/{id}/checklist` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-post-sni-api-v1-tasks-id-checklist) |
| DELETE | `/sni/api/v1/tasks/{id}/checklist/{itemId}` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-delete-sni-api-v1-tasks-id-checklist-itemid) |
| PATCH | `/sni/api/v1/tasks/{id}/checklist/{itemId}` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-patch-sni-api-v1-tasks-id-checklist-itemid) |
| POST | `/sni/api/v1/tasks/{id}/comments` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-post-sni-api-v1-tasks-id-comments) |
| PATCH | `/sni/api/v1/tasks/{id}/complete` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-patch-sni-api-v1-tasks-id-complete) |
| PATCH | `/sni/api/v1/tasks/{id}/status` | TASK:WRITE | [TaskManagementController](routes/TaskManagementController.md#r-patch-sni-api-v1-tasks-id-status) |
