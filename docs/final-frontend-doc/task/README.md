# task : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module gère les tâches internes de l'équipe du centre : création, assignation à un compte du back-office, liste de contrôle (checklist), commentaires, échéance, priorité, récurrence, tableau Kanban et indicateurs. Il est réservé au back-office (droits `TASK:READ`, `TASK:WRITE`, `TASK:ASSIGN`). D'autres modules y créent des tâches automatiquement : `booking` (préparation et nettoyage d'une ressource), `inventory` (réapprovisionnement sur alerte de stock bas) et `support` (tâche créée depuis un ticket). Les rappels d'échéance partent par courriel à l'agent assigné.

## Notions

| Objet | Identifiant exposé | Lien | Remarques |
| --- | --- | --- | --- |
| Tâche (`TaskResponse`) | `id` numérique uniquement (pas de code lisible) | porte ses éléments de checklist et ses commentaires, renvoyés dans la même réponse | Toutes les routes travaillent par `id` : le garder en texte ([Identifiants](../README.md#identifiants--attention-aux-grands-nombres)). |
| Élément de checklist | `id` numérique (`itemId` dans le chemin) | appartient à une tâche | Trié par `displayOrder` croissant puis `id`. |
| Commentaire | `id` numérique, jamais utilisé en entrée | appartient à une tâche | Trié par date croissante. Ni modification ni suppression. |
| Origine (`sourceType`, `sourceCode`) | texte libre | renvoie à l'objet qui a motivé la tâche | Valeurs posées par le serveur : `BOOKING` + numéro de réservation, `INVENTORY` + code d'alerte, `SUPPORT_TICKET` + numéro de ticket. Une tâche manuelle peut porter n'importe quel texte. |
| Tâche parente (`parentTaskId`) | `id` numérique | tâche récurrente terminée dont celle-ci est la suivante | Rempli seulement sur les tâches régénérées par la récurrence. |
| Agent (`assignedTo`) | id numérique d'un compte utilisateur | | C'est l'`id` de `AdminUserResponse` ([securite](../securite/routes/AdminUserController.md#r-get-sni-api-v1-admin-users)), **pas** le `userId` texte de `/auth/me`. |

Énumérations :

- `TaskPriority` : `LOW`, `MEDIUM` (défaut), `HIGH`, `URGENT`.
- `TaskRecurrence` : `NONE` (défaut), `DAILY`, `WEEKLY`, `MONTHLY`, `AFTER_BOOKING` (posée par `booking` sur la tâche de nettoyage, sans effet de régénération).
- `TaskStatus` : voir ci-dessous.

## Statuts et cycle de vie

Le serveur n'impose aucune transition : `PATCH /sni/api/v1/tasks/{id}/status` accepte tout statut depuis tout autre, et `archive` ou `complete` fonctionnent depuis n'importe quel statut.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | À faire (valeur à la création) | création manuelle ou automatique, `PATCH .../status`, régénération d'une tâche récurrente | `IN_PROGRESS` (dont automatiquement à l'assignation), tout autre statut |
| `IN_PROGRESS` | En cours | `PATCH .../status`, ou **automatiquement** quand on appelle `PATCH .../assign` sur une tâche `OPEN` | tout statut |
| `COMPLETED` | Terminée, `completedAt` horodaté à la première fin | `PATCH /sni/api/v1/tasks/{id}/complete` ou `PATCH .../status` avec `COMPLETED` (même traitement) | tout statut ; quitter `COMPLETED` efface `completedAt` |
| `CANCELLED` | Annulée | `PATCH .../status` uniquement | tout statut |
| `ARCHIVED` | Archivée, masquée des listes par défaut et du Kanban | `PATCH /sni/api/v1/tasks/{id}/archive` ou `PATCH .../status` | tout statut par `PATCH .../status` (désarchivage) |

Récurrence (`src/main/java/com/sni/bokaticowork/features/task/service/implementation/TaskManagementServiceImpl.java:395`) : quand une tâche passe en `COMPLETED` pour la première fois depuis un autre statut, qu'elle a une échéance `dueAt` et une récurrence `DAILY`, `WEEKLY` ou `MONTHLY`, le serveur crée une nouvelle tâche `OPEN` identique (titre, description, agent, priorité, origine, récurrence), échéance décalée d'un jour, de 7 jours ou d'un mois (calendrier UTC), avec la checklist recopiée et décochée, et `parentTaskId` = id de la tâche terminée. Une tâche ne génère jamais deux suivantes : rouvrir puis terminer à nouveau ne crée rien de plus. Sans `dueAt`, aucune régénération.

## Parcours

### Kanban des tâches

1. `GET /sni/api/v1/tasks/board` avec filtres facultatifs `assignedTo`, `priority`, `sourceType`, `searchText` (cherche dans titre, description, `sourceCode`).
2. La réponse contient toujours quatre colonnes, dans l'ordre `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` (pas de colonne `ARCHIVED`). Dans chaque colonne, les cartes sont triées par priorité (`URGENT` d'abord) puis par échéance (à défaut date de création) croissante. Pas de pagination : toutes les tâches de ces statuts sont renvoyées, y compris toutes les tâches terminées.
3. Chaque carte porte `checklistTotal`, `checklistDone`, `commentCount` et `overdue` (échéance passée et statut ni `COMPLETED`, ni `CANCELLED`, ni `ARCHIVED`). `overdueTasks` en tête du tableau est la somme des cartes en retard.
4. Glisser une carte : `PATCH /sni/api/v1/tasks/{id}/status` avec `{ "status": "IN_PROGRESS" }`. Vers `COMPLETED`, utiliser indifféremment cette route ou `/complete`.

### Liste paginée

1. `GET /sni/api/v1/tasks` paginé ([pagination](../README.md#paramètres-de-pagination)), filtres `status`, `assignedTo`, `priority`, `sourceType`, `searchText`. Le tri est toujours « création décroissante » : le paramètre `sort` est ignoré.
2. Sans filtre `status`, les tâches `ARCHIVED` sont exclues. Pour un onglet « Archives », passer `status=ARCHIVED`.
3. Chaque élément est un `TaskResponse` complet (checklist et commentaires inclus).

### Tâches du jour

`GET /sni/api/v1/tasks/due-today` renvoie, sans pagination, les tâches dont `dueAt` tombe dans la journée **UTC** en cours et dont le statut n'est ni `COMPLETED`, ni `CANCELLED`, ni `ARCHIVED`. Ordre non garanti : trier côté écran.

### Créer une tâche

1. Charger la liste des agents (`GET /sni/api/v1/admin/users`, droit `SYSTEM:USERS`) pour proposer `assignedTo`.
2. `POST /sni/api/v1/tasks` avec `title` (obligatoire), `description`, `assignedTo`, `priority`, `dueAt` (date-heure avec fuseau), `sourceType` et `sourceCode` (pour rattacher la tâche à un objet), `recurrence`, et éventuellement `checklist` : liste de `{ "label": "...", "completed": false, "displayOrder": 1 }`, chaque `label` obligatoire.
3. Réponse `201`, statut `OPEN`, même si un agent est fourni (le passage automatique en `IN_PROGRESS` ne se fait que par `/assign`). Si `assignedTo` est rempli, l'agent reçoit un courriel.

### Fiche d'une tâche

1. `GET /sni/api/v1/tasks/{id}`.
2. Modifier : `PUT /sni/api/v1/tasks/{id}` avec les seuls champs à changer (`title`, `description`, `priority`, `dueAt`, `sourceType`, `sourceCode`, `recurrence`). Un champ absent ou `null` est laissé tel quel. Un texte vide pour `sourceType` ou `sourceCode` efface la valeur. `title` vide est refusé. `dueAt` ne peut pas être effacé une fois posé. L'agent et le statut ne se changent pas ici. Changer `dueAt` réarme les rappels d'échéance.
3. Assigner ou réassigner : `PATCH /sni/api/v1/tasks/{id}/assign` avec `{ "assignedTo": <id> }` (droit `TASK:ASSIGN`). `{ "assignedTo": null }` retire l'agent. Dans les deux cas, une tâche `OPEN` passe en `IN_PROGRESS`. Un courriel part si le nouvel agent diffère de l'ancien.
4. Checklist : ajouter par `POST /sni/api/v1/tasks/{id}/checklist` (`label` obligatoire, `completed`, `displayOrder`), cocher ou renommer par `PATCH /sni/api/v1/tasks/{id}/checklist/{itemId}` (champs à changer seulement), retirer par `DELETE /sni/api/v1/tasks/{id}/checklist/{itemId}`. Chaque appel renvoie la tâche complète : remplacer l'état de l'écran par la réponse. Cocher tous les éléments ne termine pas la tâche.
5. Commenter : `POST /sni/api/v1/tasks/{id}/comments` avec `comment` (obligatoire), `authorId` et `authorName`. Le serveur ne déduit pas l'auteur du jeton : l'écran doit envoyer l'identifiant et le nom de l'utilisateur connecté. Réponse `201` avec la tâche complète.
6. Terminer : `PATCH /sni/api/v1/tasks/{id}/complete`. Si la tâche est récurrente, recharger la liste : une nouvelle tâche est apparue.
7. Archiver : `PATCH /sni/api/v1/tasks/{id}/archive`. La tâche disparaît du Kanban et de la liste par défaut.

### Tâche liée à un autre objet

- Depuis un ticket de support : `POST /sni/api/v1/support/tickets/{ticketNumber}/task` (module [support](../support/routes.md), droit `SUPPORT:WRITE`) crée la tâche avec `sourceType = SUPPORT_TICKET`.
- Pour afficher les tâches d'un objet (réservation, ticket, alerte de stock) : `GET /sni/api/v1/tasks?sourceType=BOOKING&searchText=<numéro>` (le filtre `searchText` porte notamment sur `sourceCode` ; il n'existe pas de filtre exact sur `sourceCode`).
- Dans la fiche d'une tâche, proposer un lien vers l'objet d'origine selon `sourceType` (`BOOKING` : réservation, `INVENTORY` : alerte de stock, `SUPPORT_TICKET` : ticket).

### Indicateurs

`GET /sni/api/v1/tasks/metrics` : compteurs tous temps par statut, `overdueTasks` (échéance passée, statut actif), `completionRate` (terminées / total, en pourcentage à une décimale, archivées comprises dans le total), `avgCompletionHours` (moyenne entre création et fin des tâches terminées), `byPriority` et `bySourceType` (objets `{ "HIGH": 3, ... }` ; les tâches sans `sourceType` ne sont pas comptées).

## Règles et erreurs à refléter dans l'écran

Les messages du module sont en anglais : prévoir un libellé français côté écran.

| Situation | Statut | `message` |
| --- | --- | --- |
| Création sans `title` | `400` `VALIDATION_FAILED` | `title : must not be blank` |
| Élément de checklist sans `label` (création de tâche ou ajout) | `400` | `Checklist item label is required` |
| `PUT` avec `title` vide | `400` | `Task title is required` |
| `PATCH .../status` sans `status` | `400` `VALIDATION_FAILED` | `status : must not be null` |
| Commentaire vide | `400` `VALIDATION_FAILED` | `comment : must not be blank` |
| Tâche introuvable | `404` | `Task not found: <id>` |
| Élément de checklist introuvable ou d'une autre tâche | `404` | `Checklist item not found: <itemId>` |

Règles non contrôlées par le serveur, à appliquer dans l'écran :

- `assignedTo` n'est pas vérifié : un id inexistant est enregistré, et aucun courriel ne part.
- `dueAt` peut être dans le passé : la tâche apparaît aussitôt en retard et le prochain passage du worker envoie le courriel « En retard ».
- Une tâche `COMPLETED`, `CANCELLED` ou `ARCHIVED` reste modifiable (texte, checklist, commentaires) : griser l'édition si le métier le demande.
- Une récurrence sans `dueAt` ne produit rien : rendre `dueAt` obligatoire dans le formulaire quand `recurrence` vaut `DAILY`, `WEEKLY` ou `MONTHLY`. Ne pas proposer `AFTER_BOOKING` à la saisie manuelle.

## À prévoir côté frontend

Écrans à construire :

1. Kanban des tâches avec filtres (agent, priorité, origine, texte) et badge de retard.
2. Liste paginée avec onglet Archives.
3. Widget « Tâches du jour » (tableau de bord).
4. Fiche tâche : en-tête modifiable, agent, statut, checklist, fil de commentaires, lien vers l'objet d'origine, tâche parente.
5. Formulaire de création (avec checklist initiale).
6. Tableau d'indicateurs.

Droits par action :

| Action | Droit |
| --- | --- |
| Lire (liste, Kanban, fiche, du jour, indicateurs) | `TASK:READ` |
| Créer, modifier, changer de statut, terminer, archiver, gérer la checklist, commenter | `TASK:WRITE` |
| Assigner ou désassigner | `TASK:ASSIGN` |
| Charger la liste des agents (`GET /sni/api/v1/admin/users`) | `SYSTEM:USERS` |

Pièges :

- Pas de route de suppression d'une tâche ni d'un commentaire : archiver à la place.
- L'onglet « Mes tâches » demande l'id numérique du compte connecté (`assignedTo`), que `/auth/me` ne fournit pas (il renvoie `userId`, un code texte). Un utilisateur sans `SYSTEM:USERS` ne peut pas non plus charger la liste des agents.
- `description` et `comment` sont stockés tels quels (seulement tronqués des espaces) : s'ils contiennent du HTML, l'assainir avant affichage.
- `due-today` raisonne en jour UTC : au Congo (UTC+1), une tâche due entre 00 h et 01 h locale apparaît la veille. Pour un affichage exact, filtrer côté écran à partir de la liste.
- Le Kanban renvoie toutes les tâches terminées et annulées sans limite : prévoir de replier ces colonnes.
- Pas de temps réel : recharger après chaque action.

## Effets automatiques

| Déclencheur | Effet | Code |
| --- | --- | --- |
| Création avec agent, assignation à un nouvel agent, régénération d'une tâche récurrente assignée | Courriel « [Task] Tache assignee - <titre> » à l'agent (gabarit `email/task-event`, envoi asynchrone par la file de courriels, dates formatées en heure de `Africa/Lagos`) | `TaskManagementServiceImpl.java:79`, `:187`, `:422` |
| Worker `TaskReminderWorker`, toutes les heures pile (`bokati.task.reminder-cron`, `TASK_REMINDER_CRON`) | Pour chaque tâche active assignée dont l'échéance tombe dans les 24 prochaines heures (`TASK_DUE_SOON_HOURS`) : courriel « Echeance proche », une seule fois. Pour chaque tâche active assignée dont l'échéance est passée : courriel « En retard », une seule fois. Les deux alertes sont réarmées quand `dueAt` change ou quand la tâche revient à un statut actif depuis un autre statut. | `src/main/java/com/sni/bokaticowork/features/task/worker/TaskReminderWorker.java:32` |
| Réservation confirmée (`booking`) | Tâche « Preparer la ressource <nom> », priorité `HIGH`, échéance 2 h avant le début (`TASK_BOOKING_PREP_HOURS`), agent `TASK_BOOKING_DEFAULT_ASSIGNEE` s'il est configuré, `sourceType = BOOKING`, checklist de deux éléments | `src/main/java/com/sni/bokaticowork/features/booking/service/implementation/BookingServiceImpl.java:813` |
| Réservation terminée (`booking`) | Tâche « Nettoyer la ressource <nom> », priorité `MEDIUM`, échéance à la fin de la réservation, agent `TASK_BOOKING_CLEANUP_DEFAULT_ASSIGNEE`, récurrence `AFTER_BOOKING`, checklist de deux éléments | `BookingServiceImpl.java:837` |
| Alerte de stock `LOW_STOCK` ou `RECURRING_LOW_STOCK` (`inventory`) | Tâche « Reapprovisionner <article> », priorité `HIGH` (ou `URGENT` si récurrente), échéance dans 24 h (`TASK_INVENTORY_REORDER_DUE_HOURS`), agent `TASK_INVENTORY_DEFAULT_ASSIGNEE`, `sourceType = INVENTORY`, `sourceCode` = code de l'alerte | `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/service/implementation/InventoryAutomationServiceImpl.java:164` |
| Création depuis un ticket (`support`) | Tâche `sourceType = SUPPORT_TICKET`, événement « Tâche créée » dans l'historique du ticket | `src/main/java/com/sni/bokaticowork/features/support/service/implementation/SupportTicketServiceImpl.java:385` |

Les tâches automatiques ont des titres et descriptions sans accents (« Preparer », « Reapprovisionner »). Toutes les actions du module sont tracées dans le journal d'audit (`@Audited`, module `TASK`).

## Écarts relevés

1. `PATCH /sni/api/v1/tasks/{id}/assign` passe la tâche `OPEN` en `IN_PROGRESS` même quand on retire l'agent (`assignedTo: null`) (`src/main/java/com/sni/bokaticowork/features/task/service/implementation/TaskManagementServiceImpl.java:182` à `:185`).
2. L'auteur d'un commentaire est fourni par le client (`authorId`, `authorName` dans `src/main/java/com/sni/bokaticowork/features/task/dto/TaskDtos.java:40`) et non déduit du jeton : n'importe qui peut commenter au nom d'un autre.
3. `GET /sni/api/v1/tasks/due-today` calcule la journée en UTC (`TaskManagementServiceImpl.java:263`) alors que l'application a un fuseau configuré (`app.time-zone`) et que les courriels formatent en `Africa/Lagos`.
4. Aucune transition contrôlée : `complete` depuis `CANCELLED` ou `ARCHIVED`, `archive` d'une tâche en cours, tout passe (`TaskManagementServiceImpl.java:144`, `:151`, `:160`).
5. `assignedTo` n'est jamais vérifié (`TaskManagementServiceImpl.java:58`, `:182`) ; un id inconnu est seulement journalisé à l'envoi du courriel.
6. Le paramètre `sort` est ignoré sur `GET /sni/api/v1/tasks` (`TaskManagementServiceImpl.java:93`).
7. `GET /sni/api/v1/tasks/board` renvoie toutes les tâches `COMPLETED` et `CANCELLED` sans limite ni pagination (`TaskManagementServiceImpl.java:285`).
8. `TaskStatus.ARCHIVED` est compté dans le total de `completionRate` (`TaskManagementServiceImpl.java:334`) : archiver des tâches terminées fait baisser le taux affiché.
9. Messages d'erreur en anglais dans tout le module ; sujets des courriels sans accents (« Tache assignee », « Echeance proche ») (`src/main/java/com/sni/bokaticowork/features/task/service/implementation/TaskEmailServiceImpl.java`).
10. `PATCH .../assign` n'a pas `@Valid` et son corps peut être absent ou vide : l'appel sans corps renvoie une erreur de lecture du corps plutôt qu'une désassignation (non vérifié : statut exact) (`src/main/java/com/sni/bokaticowork/features/task/controller/TaskManagementController.java:76`).
