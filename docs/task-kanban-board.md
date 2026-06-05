# Tasks — Kanban Board (Vue tableau interactif)

> Dernière mise à jour : 2026-05-21  
> Statut : ✅ Complet et production-ready

---

## 1. Vue d'ensemble

Le Kanban Board Tasks expose l'ensemble des tâches regroupées par **statut** sous forme de colonnes, à l'image d'un tableau Jira. Chaque tâche est représentée par une **carte compacte** incluant sa priorité, sa progression de checklist, son nombre de commentaires et un indicateur de retard. Les filtres permettent de restreindre la vue par agent, priorité, source ou texte libre. Le déplacement d'une carte se fait via `PATCH /tasks/{id}/status`.

---

## 2. Endpoints

**Base URL :** `/sni/api/v1/tasks`

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| GET | `/board` | TASK:READ | Tableau Kanban — toutes les colonnes et cartes |
| PATCH | `/{id}/status` | TASK:WRITE | Déplacer une carte (changer de colonne) |

---

## 3. GET `/board` — Paramètres

| Paramètre | Type | Obligatoire | Description |
|---|---|---|---|
| `assignedTo` | Long | Non | Filtrer par ID de l'agent assigné |
| `priority` | TaskPriority | Non | Filtrer par niveau de priorité |
| `sourceType` | String | Non | Filtrer par type de source (ex : `CRM_LEAD`, `BOOKING`) |
| `searchText` | String | Non | Recherche sur titre, description, code source |

**Valeurs `priority` :** `LOW`, `MEDIUM`, `HIGH`, `URGENT`

---

## 4. Colonnes du tableau

Les colonnes suivent le cycle de vie d'une tâche. Elles sont toujours toutes retournées, même vides.

| Colonne | Statut | Description |
|---|---|---|
| 1 | `OPEN` | Tâches créées, non démarrées |
| 2 | `IN_PROGRESS` | En cours de traitement |
| 3 | `COMPLETED` | Terminées |
| 4 | `CANCELLED` | Annulées |

Les cartes dans chaque colonne sont triées par **priorité décroissante** (`URGENT → HIGH → MEDIUM → LOW`) puis par **date d'échéance croissante** (les plus urgentes en haut).

---

## 5. Structure de réponse

### `TaskKanbanBoardResponse`

```json
{
  "columns": [ ... ],
  "totalTasks": 18,
  "overdueTasks": 3
}
```

> `overdueTasks` : nombre total de cartes dont `dueAt` est dépassé et dont le statut n'est pas `COMPLETED` ni `CANCELLED`.

### `TaskKanbanColumnResponse`

```json
{
  "status": "IN_PROGRESS",
  "count": 6,
  "cards": [ ... ]
}
```

### `TaskKanbanCardResponse`

```json
{
  "id": 15,
  "title": "Préparer le contrat pour Acme SARL",
  "assignedTo": 4,
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "recurrence": "NONE",
  "sourceType": "CRM_LEAD",
  "sourceCode": "LDN-1746394823456",
  "dueAt": "2026-05-23T17:00:00Z",
  "completedAt": null,
  "createdAt": "2026-05-18T09:00:00Z",
  "checklistTotal": 4,
  "checklistDone": 2,
  "commentCount": 1,
  "overdue": false
}
```

| Champ | Description |
|---|---|
| `checklistTotal` | Nombre d'items dans la checklist |
| `checklistDone` | Items cochés comme complétés |
| `commentCount` | Nombre de commentaires sur la tâche |
| `overdue` | `true` si `dueAt` est dépassé et statut actif |
| `sourceType` / `sourceCode` | Origine de la tâche (ex : lead CRM, réservation) |

---

## 6. Déplacer une carte (drag-and-drop)

**PATCH** `/sni/api/v1/tasks/{id}/status`

```json
{
  "status": "IN_PROGRESS"
}
```

**Transitions possibles :**

```
OPEN → IN_PROGRESS → COMPLETED
                   → CANCELLED
     → CANCELLED
```

Passer une tâche à `COMPLETED` enregistre automatiquement `completedAt = now`. Repasser à un autre statut efface `completedAt`.

**Réponse :** `TaskResponse` complet (avec checklist et commentaires).

---

## 7. Exemple de réponse complète

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
          "sourceType": "CRM_LEAD",
          "sourceCode": "LDN-1746000001000",
          "dueAt": "2026-05-21T12:00:00Z",
          "completedAt": null,
          "createdAt": "2026-05-20T08:00:00Z",
          "checklistTotal": 0,
          "checklistDone": 0,
          "commentCount": 0,
          "overdue": true
        }
      ]
    },
    {
      "status": "IN_PROGRESS",
      "count": 2,
      "cards": [ { "..." } ]
    },
    {
      "status": "COMPLETED",
      "count": 8,
      "cards": [ { "..." } ]
    },
    {
      "status": "CANCELLED",
      "count": 1,
      "cards": [ { "..." } ]
    }
  ],
  "totalTasks": 14,
  "overdueTasks": 1
}
```

---

## 8. Performance

- Chaque colonne déclenche **1 requête filtrée** sur `task_item`
- `checklistTotal` / `checklistDone` calculés via une **requête bulk** par colonne — aucun N+1
- `commentCount` calculé via une **requête bulk** par colonne — aucun N+1
- Total maximum : **12 requêtes** pour un board complet (4 colonnes × 3 queries)

---

## 9. Modèle de données — rappel

### TaskItem (`task_item`)

| Champ | Type | Description |
|---|---|---|
| `title` | VARCHAR | Titre de la tâche |
| `description` | TEXT | Description riche |
| `assignedTo` | BIGINT | ID de l'agent |
| `status` | ENUM | OPEN / IN_PROGRESS / COMPLETED / CANCELLED |
| `priority` | ENUM | LOW / MEDIUM / HIGH / URGENT |
| `dueAt` | TIMESTAMPTZ | Date limite |
| `sourceType` | VARCHAR | Type d'origine (CRM_LEAD, BOOKING…) |
| `sourceCode` | VARCHAR | Code de l'entité d'origine |
| `recurrence` | ENUM | NONE / DAILY / WEEKLY / MONTHLY / AFTER_BOOKING |
| `completedAt` | TIMESTAMPTZ | Date de complétion |

---

## 10. Permissions requises

| Action | Permission |
|---|---|
| Voir le board | `TASK:READ` ou `TASK_READ` |
| Déplacer une carte | `TASK:WRITE` ou `TASK_WRITE` |
| Assigner une carte | `TASK:ASSIGN` ou `TASK_ASSIGN` |