# Task Management — Documentation consolidée Frontend

> Dernière mise à jour : 2026-06-07
> Statut : ✅ Backend complet et production-ready — document frontend unique pour le module Task. Il remplace l'ancien document séparé `task-kanban-board.md`.

---

## 1. Vue d'ensemble

Le module Task gère des **tâches internes** assignables à un agent backoffice : suivi commercial (relance d'un lead CRM), préparation de dossiers, actions liées à une réservation, etc. Chaque tâche peut comporter une **checklist** (sous-étapes cochables) et des **commentaires** (fil de discussion), être **récurrente**, **priorisée**, et **rattachée à une entité source** (`sourceType`/`sourceCode`, ex. un lead CRM ou une réservation) pour assurer la traçabilité depuis d'autres modules.

Les tâches peuvent être créées manuellement ou automatiquement :
- Booking crée une tâche de préparation à la confirmation d'une réservation (`sourceType = BOOKING`) et une tâche de remise en état à la complétion.
- Inventory crée une tâche de réapprovisionnement lorsqu'une alerte `LOW_STOCK` ou `RECURRING_LOW_STOCK` est ouverte (`sourceType = INVENTORY`).

**Base URL :** `/sni/api/v1/tasks` (authentifié, JWT)

### Permissions RBAC

| Permission | Donne accès à |
|---|---|
| `TASK:READ` / `TASK_READ` | Lecture : liste, détail, board, tâches du jour |
| `TASK:WRITE` / `TASK_WRITE` | Écriture : créer/modifier/archiver une tâche, marquer terminée, changer le statut, gérer la checklist, ajouter un commentaire |
| `TASK:ASSIGN` / `TASK_ASSIGN` | Assigner/réassigner une tâche à un agent |

> Le frontend doit conditionner l'affichage des actions (réassignation, changement de statut…) à ces permissions, en plus du contrôle serveur (`@PreAuthorize`).

---

## 2. Modèle de données

### 2.1 TaskItem (`task_item`)

| Champ | Type | Notes |
|---|---|---|
| `id` | Long | |
| `title` | String | Obligatoire |
| `description` | String (rich text normalisé) | Optionnel |
| `assignedTo` | Long | ID de l'agent backoffice ; peut être `null` |
| `status` | `TaskStatus` | Cycle de vie (voir §3) |
| `priority` | `TaskPriority` | Défaut `MEDIUM` si non fourni à la création |
| `dueAt` | Instant | Date limite (échéance) |
| `sourceType` / `sourceCode` | String | Origine de la tâche (ex. `CRM_LEAD` / `LDN-...`, `BOOKING` / `BKG-...`) — permet de naviguer vers l'entité d'origine |
| `recurrence` | `TaskRecurrence` | Défaut `NONE` si non fourni |
| `parentTaskId` | Long | `null` sur une tâche originale ; renseigné sur une occurrence générée par récurrence |
| `completedAt` | Instant | Renseigné automatiquement quand `status = COMPLETED` ; effacé dès qu'on repasse à un autre statut |
| `createdAt` | Instant | |
| `updatedAt` | Instant | Mis à jour lors des modifications de la tâche, de la checklist et des commentaires |
| `checklist` | `ChecklistResponse[]` | Sous-éléments cochables |
| `comments` | `TaskCommentResponse[]` | Fil de commentaires, triés par date croissante |

### 2.2 TaskChecklist

| Champ | Type | Notes |
|---|---|---|
| `id` | Long | |
| `label` | String | Libellé de l'étape |
| `completed` | Boolean | Coché / non coché |
| `displayOrder` | Integer | Ordre d'affichage |

### 2.3 TaskComment

| Champ | Type | Notes |
|---|---|---|
| `id` | Long | |
| `authorId` / `authorName` | String | Identité de l'auteur (fournie par le frontend, pas résolue serveur) |
| `comment` | String (rich text) | Contenu |
| `createdAt` | Instant | |

### 2.4 Énumérations

```
TaskStatus      : OPEN, IN_PROGRESS, COMPLETED, CANCELLED, ARCHIVED
TaskPriority    : LOW, MEDIUM, HIGH, URGENT
TaskRecurrence  : NONE, DAILY, WEEKLY, MONTHLY, AFTER_BOOKING
```

### 2.5 Sources automatiques connues

| `sourceType` | `sourceCode` | Généré par | Notes UI |
|---|---|---|---|
| `BOOKING` | `bookingNumber` | Confirmation / complétion Booking | Lier vers la fiche réservation. Les tâches de préparation ont `recurrence = NONE`; les tâches post-réservation ont `recurrence = AFTER_BOOKING`. |
| `INVENTORY` | `alertCode` | Alertes `LOW_STOCK` / `RECURRING_LOW_STOCK` | Lier vers la fiche alerte inventaire ou l'article. `RECURRING_LOW_STOCK` crée une tâche `URGENT`, `LOW_STOCK` une tâche `HIGH`. |
| `CRM_LEAD` | `leadNumber` | Frontend CRM ou automatisation future | Pattern recommandé pour le bouton "Créer une tâche de suivi" depuis une fiche lead. |

---

## 3. Cycle de vie d'une tâche

```
OPEN → IN_PROGRESS → COMPLETED
     ↘ CANCELLED   ↗
       ARCHIVED depuis tout statut
```

- **Création** : statut initial toujours `OPEN`, priorité par défaut `MEDIUM`, récurrence par défaut `NONE`.
- **Assignation** (`PATCH /{id}/assign`) : si la tâche est encore `OPEN`, elle bascule **automatiquement** en `IN_PROGRESS` dès qu'un agent lui est assigné.
- **Complétion** : `status = COMPLETED` ⇒ `completedAt = now`. Tout passage vers un statut différent de `COMPLETED` **efface** `completedAt`.
- **Récurrence** : lors de la première complétion d'une tâche `DAILY`, `WEEKLY` ou `MONTHLY`, le backend crée immédiatement la prochaine occurrence en `OPEN`. La nouvelle tâche reprend titre, description, assignation, priorité, source, récurrence et checklist réinitialisée non cochée ; son `dueAt` est avancé de 1 jour / 1 semaine / 1 mois et son `parentTaskId` pointe vers la tâche complétée.
- **Annulation** : possible depuis n'importe quel statut actif via `PATCH /{id}/status` avec `{ "status": "CANCELLED" }`.
- **Archivage** : `PATCH /{id}/archive` force `status = ARCHIVED`. Les tâches archivées sont exclues de `/tasks` quand aucun filtre `status` n'est fourni, et exclues du Kanban. Pour les retrouver : `GET /tasks?status=ARCHIVED`.
- Aucune validation serveur de transitions strictes (toutes les combinaisons `status` sont acceptées par `/status`) — le diagramme ci-dessus représente le flux **recommandé côté UI**.

> `AFTER_BOOKING` est réservé aux triggers du module Booking : la complétion manuelle d'une tâche `AFTER_BOOKING` ne génère pas d'occurrence générique. Le backend évite aussi la duplication si une tâche déjà complétée est complétée à nouveau.

---

## 4. Endpoints

Toutes les routes ci-dessous sont préfixées par `/sni/api/v1/tasks`.

| Méthode | Route | Permission | Description |
|---|---|---|---|
| `POST` | `` (racine) | `TASK:WRITE` | Créer une tâche (avec checklist optionnelle) |
| `GET` | `` (racine) | `TASK:READ` | Lister, paginé, filtrable par `status`, `assignedTo`, `priority`, `sourceType`, `searchText` |
| `GET` | `/{id}` | `TASK:READ` | Détail (avec checklist + commentaires) |
| `PUT` | `/{id}` | `TASK:WRITE` | Modifier les champs généraux d'une tâche |
| `PATCH` | `/{id}/complete` | `TASK:WRITE` | Raccourci : marque `COMPLETED` + `completedAt = now` |
| `PATCH` | `/{id}/archive` | `TASK:WRITE` | Archive la tâche (`status = ARCHIVED`) |
| `PATCH` | `/{id}/status` | `TASK:WRITE` | Changer le statut (drag-and-drop Kanban inclus) |
| `PATCH` | `/{id}/assign` | `TASK:ASSIGN` | (Ré)assigner à un agent |
| `POST` | `/{id}/comments` | `TASK:WRITE` | Ajouter un commentaire |
| `POST` | `/{id}/checklist` | `TASK:WRITE` | Ajouter un item de checklist |
| `PATCH` | `/{id}/checklist/{itemId}` | `TASK:WRITE` | Modifier/cocher un item de checklist |
| `DELETE` | `/{id}/checklist/{itemId}` | `TASK:WRITE` | Supprimer un item de checklist |
| `GET` | `/due-today` | `TASK:READ` | Tâches dont l'échéance tombe aujourd'hui (UTC), non terminées |
| `GET` | `/board` | `TASK:READ` | Vue Kanban — toutes les colonnes et cartes |
| `GET` | `/metrics` | `TASK:READ` | Métriques globales du module Task |

### 4.1 Créer une tâche — `POST /tasks`

```json
{
  "title": "Préparer le contrat pour Acme SARL",
  "description": "Relire les clauses spécifiques à la domiciliation avant envoi.",
  "assignedTo": 4,
  "priority": "HIGH",
  "dueAt": "2026-06-12T17:00:00Z",
  "sourceType": "CRM_LEAD",
  "sourceCode": "LDN-1746394823456",
  "recurrence": "NONE",
  "checklist": [
    { "label": "Vérifier les coordonnées du client", "completed": false, "displayOrder": 1 },
    { "label": "Joindre le tarif personnalisé", "completed": false, "displayOrder": 2 }
  ]
}
```
→ `201 Created`, retourne un `TaskResponse` complet (avec la checklist créée). Seul `title` est obligatoire ; `priority` et `recurrence` reçoivent une valeur par défaut côté serveur si omis.

### 4.2 Lister — `GET /tasks`

Paramètres : `status`, `assignedTo`, `priority`, `sourceType`, `searchText` (optionnels) + pagination Spring (`page`, `size`, `sort`).

Réponse = `PaginatedResponse<TaskResponse>` (même enveloppe que les autres modules — voir `PageInfo` : `page`, `size`, `totalPages`, `totalElements`, `first`, `last`, `hasNext`, `hasPrevious`, `sort`).

Quand `status` est absent, les tâches `ARCHIVED` sont exclues par défaut. Pour consulter l'archive, filtrer explicitement avec `status=ARCHIVED`.

### 4.3 Modifier — `PUT /tasks/{id}`

Tous les champs sont optionnels ; seuls les champs non `null` sont appliqués. `sourceType` et `sourceCode` envoyés en chaîne vide sont effacés.

```json
{
  "title": "Préparer le contrat Acme SARL",
  "description": "Relire et envoyer avant vendredi.",
  "priority": "URGENT",
  "dueAt": "2026-06-12T17:00:00Z",
  "sourceType": "CRM_LEAD",
  "sourceCode": "LDN-1746394823456",
  "recurrence": "NONE"
}
```

### 4.4 Marquer terminée — `PATCH /tasks/{id}/complete`

Aucun corps de requête. Force `status = COMPLETED` et `completedAt = now`, peu importe le statut courant. Équivalent à `PATCH /status` avec `{ "status": "COMPLETED" }`, mais en un seul appel sans body.

### 4.5 Archiver — `PATCH /tasks/{id}/archive`

Aucun corps de requête. Force `status = ARCHIVED`. Le statut peut être restauré ensuite via `PATCH /tasks/{id}/status`.

### 4.6 Changer le statut — `PATCH /tasks/{id}/status`

```json
{ "status": "IN_PROGRESS" }
```
- C'est **le même endpoint** qui sert au déplacement de carte sur le Kanban (drag-and-drop).
- Gère automatiquement `completedAt` (cf. §3).
- Si le nouveau statut est `COMPLETED` et que la tâche est récurrente (`DAILY`/`WEEKLY`/`MONTHLY`), une nouvelle occurrence peut être créée côté serveur. Après un drag-and-drop vers `COMPLETED`, recharger le board ou invalider les caches `tasks/board` et `tasks` pour afficher cette nouvelle carte.

### 4.7 (Ré)assigner — `PATCH /tasks/{id}/assign`

```json
{ "assignedTo": 7 }
```
- `assignedTo: null` est accepté → désassigne la tâche.
- **Effet de bord important :** si la tâche est `OPEN`, l'assignation la fait automatiquement passer en `IN_PROGRESS`. Le frontend doit refléter ce changement de statut dans l'UI après l'appel (recharger la tâche / mettre à jour la carte Kanban en conséquence).

### 4.8 Ajouter un commentaire — `POST /tasks/{id}/comments`

```json
{ "authorId": "USER-004", "authorName": "Awa N.", "comment": "Contrat envoyé, en attente de signature." }
```
→ `201 Created`. `authorId`/`authorName` sont **fournis par le frontend** (à pré-remplir avec l'utilisateur connecté) — le serveur ne les résout pas depuis le token.

### 4.9 Gérer la checklist

Ajouter un item :

```json
{ "label": "Joindre le tarif personnalisé", "completed": false, "displayOrder": 2 }
```

Modifier ou cocher un item :

```json
{ "completed": true, "label": "Tarif personnalisé joint", "displayOrder": 2 }
```

Tous les champs de `PATCH /checklist/{itemId}` sont optionnels, mais `label` ne peut pas être vide quand il est fourni.

### 4.10 Tâches du jour — `GET /tasks/due-today`

Retourne un tableau simple `TaskResponse[]` (pas paginé) : toutes les tâches dont `dueAt` tombe dans la journée UTC courante et dont le statut n'est ni `COMPLETED`, ni `CANCELLED`, ni `ARCHIVED`.

> Pensé pour un widget de type `DueTodayWidget` sur le tableau de bord ou la page "Mes tâches".

### 4.11 Métriques — `GET /tasks/metrics`

Retourne les compteurs globaux et agrégations utiles au pilotage :

```json
{
  "totalTasks": 142,
  "openTasks": 38,
  "inProgressTasks": 21,
  "completedTasks": 75,
  "cancelledTasks": 8,
  "archivedTasks": 0,
  "overdueTasks": 6,
  "completionRate": 52.8,
  "avgCompletionHours": 14.2,
  "byPriority": { "LOW": 20, "MEDIUM": 60, "HIGH": 45, "URGENT": 17 },
  "bySourceType": { "CRM_LEAD": 30, "BOOKING": 80, "INVENTORY": 10, "MANUAL": 22 }
}
```

`completionRate` = `completedTasks / totalTasks * 100`, arrondi à 1 décimale. `overdueTasks` compte les tâches actives en retard, donc hors `COMPLETED`, `CANCELLED`, `ARCHIVED`.

---

## 5. Vue Kanban (Board)

### `GET /board` — `TASK:READ`

Paramètres optionnels : `assignedTo`, `priority`, `sourceType` (ex. `CRM_LEAD`, `BOOKING`), `searchText` (recherche sur titre/description/code source).

Retourne un `TaskKanbanBoardResponse` avec **toujours les 4 colonnes actives** (même vides). Les tâches `ARCHIVED` ne sont pas affichées sur le board :

| # | Colonne | Statut |
|---|---|---|
| 1 | À faire | `OPEN` |
| 2 | En cours | `IN_PROGRESS` |
| 3 | Terminées | `COMPLETED` |
| 4 | Annulées | `CANCELLED` |

```json
{
  "columns": [
    {
      "status": "OPEN",
      "count": 3,
      "cards": [
        {
          "id": 10,
          "title": "Rappeler le prospect Dupont",
          "assignedTo": 2,
          "status": "OPEN",
          "priority": "URGENT",
          "recurrence": "NONE",
          "parentTaskId": null,
          "sourceType": "CRM_LEAD",
          "sourceCode": "LDN-1746000001000",
          "dueAt": "2026-06-09T12:00:00Z",
          "completedAt": null,
          "createdAt": "2026-06-05T08:00:00Z",
          "updatedAt": "2026-06-05T08:00:00Z",
          "checklistTotal": 0,
          "checklistDone": 0,
          "commentCount": 0,
          "overdue": true
        }
      ]
    }
  ],
  "totalTasks": 14,
  "overdueTasks": 1
}
```

- Cartes triées par **priorité décroissante** (`URGENT → HIGH → MEDIUM → LOW`) puis par **échéance croissante** (`dueAt`, fallback `createdAt`).
- `overdue` (par carte) / `overdueTasks` (global) : `true`/compté si `dueAt` est dépassé **et** le statut est actif (ni `COMPLETED`, ni `CANCELLED`, ni `ARCHIVED`).
- `TaskKanbanCardResponse` est une version **allégée** de la tâche (pas de `description`, pas de checklist/commentaires détaillés — juste les compteurs `checklistTotal`/`checklistDone`/`commentCount`) : pensé pour l'affichage de masse sans N+1.
- Le déplacement d'une carte se fait via `PATCH /{id}/status` (§4.6) — ne pas créer de route dédiée.

### 5.1 Performance du board

- Chaque colonne déclenche 1 requête filtrée sur `task_item`.
- `checklistTotal` / `checklistDone` sont calculés via une requête bulk par colonne, sans N+1.
- `commentCount` est calculé via une requête bulk par colonne, sans N+1.
- Total maximum : 12 requêtes pour un board complet (4 colonnes × 3 requêtes).

---

## 6. Propositions d'écrans (UI)

### 6.1 Cartographie des écrans

| Écran | Route suggérée | Données | Permissions |
|---|---|---|---|
| **Tableau de bord / Mes tâches** | `/tasks` ou `/my-tasks` | `/tasks?assignedTo=moi` + `/due-today` | `TASK:READ` |
| **Tableau Kanban** | `/tasks/board` | `/board` (+ `/{id}/status` pour drag&drop) | `TASK:READ`, `TASK:WRITE` |
| **Liste des tâches** | `/tasks/list` | `/tasks` (paginé, filtres riches) | `TASK:READ` |
| **Détail tâche** (panneau latéral ou page) | `/tasks/:id` | `/tasks/{id}` + actions | `TASK:READ`, `TASK:WRITE`, `TASK:ASSIGN` |

### 6.2 `TaskBoardPage` — Vue Kanban

- 4 colonnes fixes (`OPEN → CANCELLED`), en-tête `{libellé} · {count}`.
- `TaskKanbanCard` compacte : titre, badge priorité (couleur par niveau : gris=LOW, bleu=MEDIUM, orange=HIGH, rouge=URGENT), badge récurrence si ≠ `NONE`, avatar/initiales de l'agent (`assignedTo`), barre de progression checklist (`checklistDone / checklistTotal`), icône commentaires + `commentCount`, badge "⚠️ En retard" si `overdue = true`, badge source (`sourceType`/`sourceCode`) cliquable pour naviguer vers l'entité d'origine (ex. fiche lead CRM).
- **Drag-and-drop** entre colonnes → `PATCH /{id}/status` :
  - Optimistic UI recommandé (déplacer la carte immédiatement, revert + toast d'erreur si l'appel échoue).
  - Glisser vers `COMPLETED` doit visuellement confirmer le calcul auto de `completedAt`.
- Barre de filtres en en-tête : `assignedTo` (sélecteur d'agent), `priority`, `sourceType` (liste des types connus : `CRM_LEAD`, `BOOKING`, …), `searchText` (debounce ~300 ms).
- Clic sur une carte → ouverture de `TaskDetailDrawer`.
- Bouton "+ Nouvelle tâche" → `TaskFormModal`.

### 6.3 `MyTasksPage` — Mes tâches / Tableau de bord agent

- Filtre implicite `assignedTo = utilisateur connecté`.
- Sections : "À faire aujourd'hui" (`DueTodayWidget`, alimenté par `/due-today`, en filtrant côté client sur `assignedTo === moi` si l'API ne le fait pas), "En retard" (cartes `overdue = true` du board filtré sur soi), "En cours", "Terminées récemment".
- Compteurs rapides en en-tête : utiliser `/tasks/metrics` pour les totaux globaux et `/board?assignedTo=moi` pour les compteurs spécifiques à l'agent connecté.

### 6.4 `TaskListPage` — Liste/Table

- Table paginée (`PaginatedResponse`) : titre, agent assigné, statut (badge), priorité (badge), échéance, source, créé le.
- Filtres serveur disponibles : `status`, `assignedTo`, `priority`, `sourceType`, `searchText`.
- Actions de ligne : ouvrir le détail, marquer terminée (`PATCH /complete`), réassigner.

### 6.5 `TaskDetailDrawer` / `TaskDetailPage` — Détail

Sections :
1. **En-tête** : titre, badges statut/priorité/récurrence, échéance (`dueAt`, avec mise en évidence si dépassée), lien vers l'entité source (`sourceType`/`sourceCode`) si renseignée. Actions : `Changer le statut`, `Marquer terminée`, `Réassigner`.
2. **Description** : rendu rich text (`description`).
3. **Checklist** : liste d'items cochables (`checklist[]`) avec barre de progression globale (`X / Y complétés`). Les interactions utilisent `POST /checklist`, `PATCH /checklist/{itemId}` et `DELETE /checklist/{itemId}`.
4. **Commentaires** : fil chronologique (`comments[]`, triés croissant), formulaire d'ajout (`AddTaskCommentForm`) pré-rempli avec l'auteur connecté → `POST /comments`.

### 6.6 `TaskFormModal` — Création / édition rapide

- Champs : `title*`, `description`, `assignedTo` (sélecteur d'agent), `priority` (défaut `MEDIUM`), `dueAt` (date/heure), `sourceType`/`sourceCode` (pré-remplis automatiquement quand la création est déclenchée **depuis** une autre fiche, ex. bouton "Créer une tâche de suivi" sur `LeadDetailPage`), `recurrence`, et une checklist éditable (ajout/suppression/réordonnancement de lignes avant soumission).
- En édition, utiliser `PUT /tasks/{id}` pour les champs généraux. L'assignation reste une action séparée (`PATCH /assign`) car elle utilise la permission `TASK:ASSIGN`.

### 6.7 `AssignTaskModal`

- Sélecteur d'agent (recherche/autocomplete sur les utilisateurs backoffice), option "Désassigner".
- Avertissement si la tâche est `OPEN` : "L'assignation passera automatiquement cette tâche en `En cours`."

---

## 7. Parcours d'utilisation recommandés

### 7.1 Création depuis un autre module (intégration croisée)

Les tâches portent `sourceType`/`sourceCode` pour tracer leur origine (ex. `CRM_LEAD` / `LDN-...`, `BOOKING` / `BKG-...`). Lorsqu'un écran d'un autre module propose "Créer une tâche de suivi" :
1. Pré-remplir `TaskFormModal` avec `sourceType`/`sourceCode` correspondant à l'entité courante (non modifiables par l'utilisateur).
2. Après création, afficher un lien retour ("Tâche créée — voir dans le tableau des tâches").
3. Sur la fiche source (ex. `LeadDetailPage`), envisager d'afficher les tâches liées (en filtrant `/board?sourceType=CRM_LEAD&searchText={leadNumber}` ou via une future route dédiée si le volume le justifie).

Les intégrations automatiques déjà câblées côté backend :
- Booking confirmation : crée "Preparer la ressource ..." avec `sourceType = BOOKING`, priorité `HIGH`, échéance `startedAt - TASK_BOOKING_PREP_HOURS`.
- Booking complétion : crée "Nettoyer la ressource ..." avec `sourceType = BOOKING`, priorité `MEDIUM`, `recurrence = AFTER_BOOKING`.
- Inventory low stock : crée "Reapprovisionner ..." avec `sourceType = INVENTORY`, priorité `HIGH` ou `URGENT`.

### 7.2 Routine quotidienne d'un agent

1. Page d'accueil = `MyTasksPage`, widget "Aujourd'hui" (`/due-today`) en haut.
2. Traiter les tâches `OPEN` en priorité `URGENT`/`HIGH` d'abord (tri déjà fourni par `/board`).
3. Cocher les items de checklist au fur et à mesure, ajouter des commentaires de suivi.
4. Glisser la carte vers `IN_PROGRESS` puis `COMPLETED` (ou utiliser le bouton rapide "Marquer terminée").

### 7.3 Pilotage / répartition de charge (manager)

- `TaskBoardPage` filtré par `assignedTo` pour visualiser la charge de chaque agent colonne par colonne.
- Repérer les cartes `overdue = true` (badge rouge) pour réassigner ou réajuster les priorités via `AssignTaskModal` / `PATCH /status`.
- Filtrer par `sourceType` pour analyser, par exemple, le volume de tâches générées par le pipeline CRM vs les réservations.

### 7.4 Bonnes pratiques d'intégration technique

- **Deux sources de données concurrentes** : `/tasks` (paginé, filtres riches, retourne des `TaskResponse` complets) vs `/board` (non paginé par colonne, cartes allégées). Ne pas mélanger ces deux modèles dans un même composant de liste — choisir l'un ou l'autre selon l'écran.
- **Effets de bord à refléter dans l'UI** : assignation d'une tâche `OPEN` → passage auto à `IN_PROGRESS` ; tout changement de statut → mise à jour/effacement de `completedAt`. Toujours utiliser la réponse serveur (`TaskResponse`/carte mise à jour) comme source de vérité après une mutation plutôt que de deviner l'état côté client.
- **Récurrence** : après une complétion réussie, invalider aussi les requêtes de liste/board. Une tâche récurrente peut produire une nouvelle carte avec `parentTaskId` renseigné.
- **Optimistic updates** recommandés pour le drag-and-drop Kanban et les actions rapides (`complete`, `assign`), avec rollback sur erreur HTTP.
- **Pagination** : toujours piloter via `PaginatedResponse.pageable` pour la `TaskListPage` (ne pas recharger toutes les pages côté client).

---

## 8. Récapitulatif des permissions par action UI

| Action UI | Permission requise |
|---|---|
| Voir listes/détail/board/widget "aujourd'hui" | `TASK:READ` |
| Créer/modifier/archiver une tâche, marquer terminée, changer le statut, gérer la checklist, ajouter un commentaire | `TASK:WRITE` |
| (Ré)assigner une tâche | `TASK:ASSIGN` |
