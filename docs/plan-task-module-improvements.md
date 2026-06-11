# Plan d'implémentation — Améliorations du module Task Management

## Contexte

Le module `task` (`features/task`) est fonctionnel (CRUD basique, Kanban, checklist à la création, commentaires) mais reste en retrait par rapport à la vision initiale (`roadmap-features-ameliorations.md` §1.3) et par rapport à la maturité d'autres modules comparables (CRM : notifications email automatiques, scoring, worker dormants…).

Ce document découpe les améliorations identifiées en **phases indépendantes et livrables séparément**, dans un ordre qui maximise la valeur immédiate (complétude API → notifications → automatisation → analytics). Chaque phase peut être développée, testée et déployée seule.

---

## Phase 1 — Complétude de l'API (fondations)

Sans ces correctifs, les écrans frontend prévus (`TaskDetailDrawer`, `TaskFormModal`) ne peuvent pas être pleinement fonctionnels.

### 1.1 Cocher/modifier un item de checklist

**Problème :** la checklist n'est alimentée qu'à la création (`CreateTaskRequest.checklist`) ; aucun moyen de cocher un item ou d'en ajouter/supprimer après coup.

**Action :**
- Nouveaux endpoints sur `TaskManagementController` :
  - `PATCH /tasks/{id}/checklist/{itemId}` — body `{ "completed": true, "label": "...", "displayOrder": 2 }` (champs optionnels, mise à jour partielle)
  - `POST /tasks/{id}/checklist` — ajouter un item (`ChecklistRequest`)
  - `DELETE /tasks/{id}/checklist/{itemId}` — supprimer un item
- Étendre `TaskManagementService`/`TaskManagementServiceImpl` avec `toggleChecklistItem`, `addChecklistItem`, `removeChecklistItem`
- Permission : `TASK:WRITE`

**Fichiers :**
- `features/task/controller/TaskManagementController.java`
- `features/task/dto/TaskDtos.java` (ajouter `UpdateChecklistItemRequest`)
- `features/task/service/interfaces/TaskManagementService.java`
- `features/task/service/implementation/TaskManagementServiceImpl.java`
- `features/task/repository/TaskChecklistRepository.java` (vérifier `findById` + appartenance à la tâche)

### 1.2 Édition générale d'une tâche

**Problème :** pas de `PUT`/`PATCH /tasks/{id}` pour modifier titre, description, priorité, échéance, source après création.

**Action :**
- `PUT /tasks/{id}` avec un `UpdateTaskRequest` (mêmes champs que `CreateTaskRequest`, tous optionnels — même pattern que `UpdateLeadRequest` côté CRM : on ne modifie que les champs non-null)
- Permission : `TASK:WRITE`

**Fichiers :** mêmes que 1.1 (ajouter `UpdateTaskRequest`, `update()`)

### 1.3 Archivage

**Décision validée :** ajout d'un statut `ARCHIVED` à `TaskStatus` (réversible, conserve l'historique — pas de suppression physique).

**Action :**
- Ajouter `ARCHIVED` à l'enum `TaskStatus`
- `PATCH /tasks/{id}/archive` (ou simplement autoriser `ARCHIVED` comme cible de `PATCH /{id}/status`) — à trancher selon la cohérence avec le pattern existant : comme `complete()` a son propre raccourci, prévoir `archive()` en miroir est cohérent
- Mettre à jour le board Kanban : soit ajouter une 5ᵉ colonne `ARCHIVED`, soit l'exclure par défaut du board (filtre implicite) — **à valider avec le frontend** car cela change la structure de `TaskKanbanBoardResponse`
- Permission : `TASK:WRITE` (réutilisée — décision validée, pas de nouvelle permission)

**Fichiers :**
- `features/task/enums/TaskStatus.java` (+ `ARCHIVED`)
- `TaskManagementController/Service(Impl)` (+ `archive()`)
- `TaskManagementServiceImpl.getBoard()` (décider de l'inclusion/exclusion d'`ARCHIVED`)
- Vérifier le mapping DB de `TaskStatus` (`@Enumerated(EnumType.STRING)` → pas de migration de schéma nécessaire, juste une nouvelle valeur de chaîne acceptée)

### 1.4 Recherche/filtrage unifié sur la liste paginée

**Problème :** `/tasks` (paginé) n'accepte que `assignedTo` ; `/board` a des filtres riches mais n'est pas paginé.

**Action :**
- Étendre `GET /tasks` avec les mêmes filtres que `/board` (`status`, `priority`, `sourceType`, `searchText`), en réutilisant/adaptant la requête native `findByStatusFiltered` (la transformer en recherche paginée `Page<TaskItem>` avec `Pageable`)
- Garder `/board` tel quel (vue agrégée non paginée, optimisée pour le Kanban)

**Fichiers :**
- `features/task/repository/TaskItemRepository.java` (nouvelle requête paginée `search(...)`)
- `features/task/service/implementation/TaskManagementServiceImpl.java`
- `features/task/controller/TaskManagementController.java`

### 1.5 Exposer `updatedAt`

**Action :** ajouter `updatedAt` à `TaskResponse` et `TaskKanbanCardResponse` (déjà présent dans l'entité `TaskItem`, juste absent des DTOs/mapper).

**Fichiers :** `TaskDtos.java`, `TaskMapper.java`

---

## Phase 2 — Notifications

Aligner Task sur le pattern déjà éprouvé en CRM (`CrmEmailService` + template Thymeleaf `email/crm-event.html` + `@Async`).

### 2.1 Service d'email Task

**Action :**
- Créer `TaskEmailService` (interface + impl), miroir de `CrmEmailServiceImpl` :
  - `sendTaskAssigned(TaskItem task)` — déclenché dans `assign()` (et `create()` si `assignedTo` renseigné)
  - `sendTaskDueSoon(TaskItem task)` — déclenché par le worker de rappel (§2.2)
  - `sendTaskOverdue(TaskItem task)` — déclenché par le même worker
- Nouveau template Thymeleaf `email/task-event.html` (variable `eventType` : `TASK_ASSIGNED`, `TASK_DUE_SOON`, `TASK_OVERDUE`)
- Résolution de l'email de l'agent via `UserService.getUserByIdForService(assignedTo)` (même pattern que `CrmEmailServiceImpl.resolveAgentEmail`)
- Envoi asynchrone (`@Async`), best-effort (try/catch + log, ne bloque jamais la requête principale)

**Fichiers (nouveaux) :**
- `features/task/service/interfaces/TaskEmailService.java`
- `features/task/service/implementation/TaskEmailServiceImpl.java`
- `src/main/resources/templates/email/task-event.html`

**Fichiers modifiés :**
- `TaskManagementServiceImpl` (`@Lazy private final TaskEmailService emailService`, appels dans `create`/`assign`)

### 2.2 Worker de rappels d'échéance

**Action :**
- Nouveau `TaskReminderWorker` (`@Scheduled`, cron configurable `bokati.task.reminder-cron`, défaut horaire ou quotidien selon préférence)
- Logique :
  1. Sélectionne les tâches actives (`status NOT IN (COMPLETED, CANCELLED)`) dont `dueAt` est dans la fenêtre de rappel (`bokati.task.due-soon-hours`, défaut 24h) et qui n'ont pas déjà reçu d'alerte (nouveau champ `dueSoonAlertSentAt`, sur le modèle de `Lead.dormantAlertSentAt`)
  2. Envoie `sendTaskDueSoon`, marque `dueSoonAlertSentAt = now`
  3. Sélectionne les tâches `overdue` (actives, `dueAt < now`) sans alerte de retard (`overdueAlertSentAt`), envoie `sendTaskOverdue`, marque `overdueAlertSentAt = now`
- **Reset** : toute mise à jour de `dueAt` ou de `status` (vers actif) remet ces deux champs à `null`

**Migration Flyway requise :** `V125__task_reminder_alert_columns.sql`
```sql
ALTER TABLE task_item
  ADD COLUMN due_soon_alert_sent_at TIMESTAMPTZ,
  ADD COLUMN overdue_alert_sent_at  TIMESTAMPTZ;
```

**Fichiers (nouveaux) :**
- `features/task/worker/TaskReminderWorker.java`
- `src/main/resources/db/migration/V125__task_reminder_alert_columns.sql`

**Fichiers modifiés :**
- `TaskItem` (+ 2 champs), `TaskItemRepository` (requêtes de sélection des candidats), `application.yml` (cron + seuils)

---

## Phase 3 — Triggers d'automatisation inter-modules

C'était l'objectif initial du module (`roadmap-features-ameliorations.md` §1.3 "Triggers automatiques"). `createFromAutomation()` existe déjà dans `TaskManagementService` mais n'est appelé par aucun module — il s'agit ici de **câbler les appelants**, pas de modifier Task.

> ⚠️ Cette phase touche d'autres modules (`booking`, `inventory`). À faire en coordination avec les équipes/branches concernées, et après que Phases 1-2 soient stables (pour que les tâches générées bénéficient des notifications).

### 3.1 Booking → Task

**Déclencheurs proposés :**
- "Préparer la salle {resourceName}" — créée X heures avant le début d'une réservation `CONFIRMED` (`sourceType = BOOKING`, `sourceCode = bookingNumber`, `recurrence = NONE`, échéance = `startTime - X h`)
- "Nettoyer la salle {resourceName}" — créée à la fin d'une réservation (`sourceType = BOOKING`, `recurrence = AFTER_BOOKING`)

**Action implémentée :**
- `BookingServiceImpl.confirmInternal(...)` crée une tâche de préparation via `TaskManagementService.createFromAutomation(...)`.
- `BookingServiceImpl.completeInternal(...)` crée une tâche de remise en état via `TaskManagementService.createFromAutomation(...)`.
- Paramètres configurables : `bokati.task.booking-prep-hours`, `bokati.task.booking-default-assignee`, `bokati.task.booking-cleanup-default-assignee`.
- Si aucun assignee n'est configuré, les tâches sont créées non assignées (`assignedTo = null`).

**Fichiers concernés :**
- `features/booking/service/implementation/BookingServiceImpl.java`
- `features/task/service/...` (consommé)

### 3.2 Inventory → Task

**Déclencheur proposé :**
- "Réapprovisionner {itemName}" — créée quand `InventoryAlert`/`InventoryReorderRule` détecte un seuil bas (`sourceType = INVENTORY`, priorité `HIGH`)

**Action implémentée :**
- `InventoryAutomationServiceImpl.openAlert(...)` crée une tâche de réapprovisionnement quand une alerte `LOW_STOCK` ou `RECURRING_LOW_STOCK` est ouverte.
- `sourceType = INVENTORY`, `sourceCode = alertCode`.
- Priorité `HIGH` pour `LOW_STOCK`, `URGENT` pour `RECURRING_LOW_STOCK`.
- Paramètres configurables : `bokati.task.inventory-reorder-due-hours`, `bokati.task.inventory-default-assignee`.

### 3.3 CRM → Task (optionnel, complète l'intégration croisée déjà documentée)

**Déclencheur proposé :**
- Bouton "Créer une tâche de suivi" depuis `LeadDetailPage` (frontend) → simple `POST /tasks` pré-rempli avec `sourceType = CRM_LEAD`/`sourceCode = leadNumber` — **ne nécessite aucun changement backend**, à documenter seulement comme pattern d'intégration (déjà fait dans `task-frontend-api.md` §7.1).

---

## Phase 4 — Régénération des tâches récurrentes

**Problème :** `TaskRecurrence` (`DAILY/WEEKLY/MONTHLY/AFTER_BOOKING`) est stockée mais aucune logique ne régénère une occurrence après complétion.

**Décision validée :** régénération **synchrone**, déclenchée dans `complete()`/`updateStatus(COMPLETED)`.

**Action :**
- Dans `TaskManagementServiceImpl`, quand une tâche passe à `COMPLETED` et que `recurrence != NONE` (et `!= AFTER_BOOKING`, piloté par le trigger booking de la Phase 3) :
  - Calculer la prochaine échéance (`dueAt + 1 jour/semaine/mois` selon `DAILY/WEEKLY/MONTHLY`)
  - Cloner la tâche (titre, description, assignedTo, priority, sourceType/sourceCode, recurrence, **checklist** — réinitialisée non cochée) avec le nouveau `dueAt`, statut `OPEN`
  - Lier l'ancienne et la nouvelle occurrence via `parentTaskId` (traçabilité — permet d'afficher l'historique des occurrences dans l'UI)

**Migration Flyway :** `V126__task_recurrence_lineage.sql` — ajout `parent_task_id BIGINT REFERENCES task_item(id)`

**Fichiers :**
- `TaskManagementServiceImpl` (logique de clonage dans `complete`/`updateStatus`)
- `TaskItem`, `TaskDtos`, migration éventuelle

---

## Phase 5 — Métriques & analytics

Aligner sur ce qui existe en CRM (`/crm/metrics`, `/crm/analytics`).

**Nouvel endpoint :** `GET /tasks/metrics` — `TASK:READ`

```json
{
  "totalTasks": 142,
  "openTasks": 38,
  "inProgressTasks": 21,
  "completedTasks": 75,
  "cancelledTasks": 8,
  "overdueTasks": 6,
  "completionRate": 58.6,
  "avgCompletionHours": 14.2,
  "byPriority": { "LOW": 20, "MEDIUM": 60, "HIGH": 45, "URGENT": 17 },
  "bySourceType": { "CRM_LEAD": 30, "BOOKING": 80, "INVENTORY": 10, "MANUAL": 22 }
}
```

**Action :**
- Requêtes d'agrégation natives sur `TaskItemRepository` (`countByStatus`, `avgCompletionDuration`, `countByPriority`, `countBySourceType`) — même approche que `LeadRepository.sumActivePipelineValue`/`countByStageBetween`
- `CrmAnalyticsResponse`/`CrmMetricsResponse` comme modèles de DTO

**Fichiers :**
- `TaskDtos.java` (+ `TaskMetricsResponse`)
- `TaskItemRepository.java`, `TaskManagementService(Impl)`, `TaskManagementController`

---

## Phase 6 — Audit trail (optionnel, qualité long-terme)

**Action :** annoter les méthodes mutantes (`create`, `update`, `assign`, `updateStatus`, `addComment`) avec `@Audited` (AOP existant dans `core/audit`) pour tracer qui a fait quoi et quand — cohérent avec les autres modules sensibles (billing, contract).

**Fichiers :** `TaskManagementServiceImpl` (annotations uniquement, aucune nouvelle infrastructure).

---

## Décisions validées (2026-06-07)

1. **Archivage (1.3)** : ajout du statut `ARCHIVED` à `TaskStatus` (réversible, conserve l'historique). Pas de `DELETE` physique.
2. **Permission archivage** : réutilisation de `TASK:WRITE` (pas de nouvelle permission `TASK:DELETE`).
3. **Régénération récurrente (Phase 4)** : mode **synchrone** — clonage de la tâche dans `complete()`/`updateStatus(COMPLETED)` quand `recurrence != NONE`.
4. **Point de départ du développement** : Phase 1 (complétude API), en commençant par 1.1 (checklist) et 1.2 (édition).

## État d'avancement (2026-06-07)

- **Phase 1 implémentée** : checklist mutable, édition générale, archivage par statut `ARCHIVED`, filtres paginés enrichis sur `/tasks`, exposition de `updatedAt`.
- **Décision Kanban appliquée** : les tâches archivées sont exclues du board pour conserver les 4 colonnes actives existantes ; elles restent consultables via `/tasks?status=ARCHIVED`.
- **Phase 2 implémentée** : notifications email d'assignation, de tâche bientôt due et de tâche en retard ; worker horaire configurable ; colonnes `due_soon_alert_sent_at` et `overdue_alert_sent_at` ajoutées via migration `V125`.
- **Phase 5 implémentée** : endpoint `GET /tasks/metrics`, DTO `TaskMetricsResponse`, agrégations par statut/priorité/source, retard actif et durée moyenne de complétion.
- **Phase 6 implémentée** : annotations `@Audited` ajoutées sur les mutations principales du service Task, y compris les créations issues d'automatisation.
- **Phase 4 implémentée** : régénération synchrone des tâches `DAILY`/`WEEKLY`/`MONTHLY` à la première complétion, traçabilité via `parentTaskId`, checklist copiée et réinitialisée.
- **Phase 3 implémentée** : Booking crée les tâches de préparation/remise en état ; Inventory crée les tâches de réapprovisionnement depuis les alertes stock bas.
- **Documentation frontend mise à jour** : voir `docs/task-frontend-api.md`.

## Décisions encore ouvertes (à trancher au moment de coder la phase concernée)

- **Règle d'assignation avancée des tâches générées par triggers (3.1/3.2)** : aujourd'hui agent fixe configurable ou file d'attente non-assignée (`assignedTo = null`) ; à améliorer plus tard si le métier veut round-robin ou planning d'accueil.
- **Cadence des rappels (2.2)** : seuil "due soon" par défaut (24h ? configurable par priorité ?) et fréquence du worker (horaire vs quotidien).

---

## Ordre de développement recommandé

| Ordre | Phase | Pourquoi |
|---|---|---|
| 1 | Phase 1 (complétude API) | Bloque les écrans frontend déjà documentés ; pas de dépendance externe ; risque faible |
| 2 | Phase 2 (notifications) | Valeur immédiate pour les agents ; pattern déjà éprouvé (CRM) à dupliquer ; nécessite une migration légère |
| 3 | Phase 5 (métriques) | Indépendante, faible risque, forte valeur pour le pilotage |
| 4 | Phase 6 (audit) | Trivial à ajouter une fois le reste stable |
| 5 | Phase 4 (récurrence) | Implémentée : clonage, lignée et checklist copiée |
| 6 | Phase 3 (triggers inter-modules) | Implémentée pour Booking et Inventory ; CRM reste un pattern frontend documenté |

---

## Prochaine étape

Ajouter des tests ciblés pour les triggers Booking/Inventory ou enrichir la règle d'assignation si le métier demande autre chose qu'un assignee fixe configurable.
