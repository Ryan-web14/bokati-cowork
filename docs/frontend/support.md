# Module Support / Ticketing — Guide consolidé Frontend

Ce document consolide **toutes** les fonctionnalités du module `support` (tickets, pièces jointes, tags,
règles de routage, réponses rapides, base de connaissances, CSAT, Client 360, analytics/exports,
conversion en tâche, création automatique de tickets) en une référence unique pour l'équipe frontend,
avec une proposition d'écrans et de workflows.

> Préfixe API commun : `ApiPath.V1` = `/sni/api/v1`
> Les endpoints "admin" nécessitent une authentification agent/admin avec les autorités `SUPPORT:*`.
> Les endpoints "client" sont accessibles aux espaces membres/clients authentifiés (portail client).
> Le endpoint CSAT public est accessible sans authentification via un lien signé envoyé par email.

---

## 1. Modèle de données — référence rapide

### 1.1 Énumérations

| Enum | Valeurs |
|---|---|
| `TicketStatus` | `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED` |
| `TicketPriority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `TicketCategory` | `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |
| `TicketSenderType` | `CLIENT`, `AGENT`, `SYSTEM` |
| `TicketEventType` | `TICKET_CREATED`, `ASSIGNED`, `STATUS_CHANGED`, `PRIORITY_CHANGED`, `CATEGORY_CHANGED`, `MESSAGE_ADDED`, `SLA_ESCALATED`, `CSAT_SUBMITTED`, `REOPENED`, `MERGED`, `AUTO_CLOSED`, `TASK_CREATED` |

> Note : `TicketEventType.MERGED` existe dans le schéma mais la fonctionnalité de fusion de tickets
> (« Phase 9 — Fusion et doublons ») n'est **pas encore implémentée** côté backend — ne pas exposer
> d'action « fusionner » dans l'UI pour le moment.

### 1.2 Cycle de vie d'un ticket

```
OPEN ──assign──> IN_PROGRESS ──message client──> WAITING_CLIENT
  │                   │                                │
  │                   └──────── resolve ───────────────┘
  │                                  │
  │                                  v
  │                              RESOLVED ──client ferme / auto-close──> CLOSED
  │                                  │
  └──────────── reopen (réponse client après résolution) ────────────────┘
```

- **SLA** : chaque ticket porte `firstResponseDueAt` et `resolutionDueAt` (calculés à la création selon
  priorité/catégorie). `overdueOnly=true` dans la recherche filtre les tickets en dépassement.
- **Escalade** : un worker (`SupportSlaEscalationWorker`, toutes les heures par défaut) détecte les
  dépassements SLA, envoie des alertes email, **augmente la priorité** automatiquement en cas de
  dépassement de résolution (`LOW → MEDIUM → HIGH → URGENT`), et incrémente `escalationLevel`
  (0 → 1 → 2 → 3, paliers : agent assigné → manager support → admin/direction), avec un cooldown
  croissant entre escalades (0h / 4h / 8h) pour éviter le spam de notifications.
- **CSAT** : une fois le ticket `RESOLVED`/`CLOSED`, un email avec un lien signé (token) est envoyé au
  client pour noter le support de 1 à 5 ; la soumission passe par l'endpoint public CSAT (page HTML
  servie directement, pas de SPA requise) ou par l'API client authentifiée.

### 1.3 Champs clés de `SupportTicketResponse`

```json
{
  "ticketNumber": "TCK-2026-000123",
  "title": "Problème d'accès au badge",
  "description": "...",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "category": "ACCESS",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-00045",
  "contactName": "Jeanne Dupont",
  "contactEmail": "jeanne@example.com",
  "contactPhone": "+243...",
  "assignedTo": 12,
  "relatedType": "BOOKING",
  "relatedCode": "BK-2026-000789",
  "firstResponseDueAt": "2026-06-08T10:00:00Z",
  "resolutionDueAt": "2026-06-09T10:00:00Z",
  "firstRespondedAt": "2026-06-08T09:15:00Z",
  "resolvedAt": null,
  "closedAt": null,
  "csatScore": null,
  "csatComment": null,
  "csatSubmittedAt": null,
  "escalationLevel": 0,
  "escalatedAt": null,
  "escalationReason": null,
  "createdAt": "2026-06-08T08:00:00Z",
  "updatedAt": "2026-06-08T09:15:00Z",
  "messages": [ /* TicketMessageResponse[] */ ],
  "attachments": [ /* AttachmentResponse[] */ ],
  "tags": ["urgent", "badge"]
}
```

`ownerType`/`ownerCode` identifient le titulaire (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` + code) ;
`relatedType`/`relatedCode` lient le ticket à un objet métier (`BOOKING`, `CASH_ANOMALY_FLAG`,
`BOOKING_NO_SHOW_QUOTA`, etc.) — utilisés pour la déduplication et la navigation croisée.

---

## 2. Référence des endpoints

### 2.1 Tickets — back-office (`/support/tickets`, autorité `SUPPORT:*`)

| Méthode | Chemin | Autorité | Description |
|---|---|---|---|
| POST | `/support/tickets` | `SUPPORT:WRITE` | Créer un ticket |
| GET | `/support/tickets` | `SUPPORT:READ` | Recherche paginée (filtres ci-dessous) |
| GET | `/support/tickets/{ticketNumber}` | `SUPPORT:READ` | Détail d'un ticket |
| GET | `/support/tickets/{ticketNumber}/timeline` | `SUPPORT:READ` | Historique d'événements |
| PATCH | `/support/tickets/{ticketNumber}/assign` | `SUPPORT:ASSIGN` | Assigner à un agent |
| PATCH | `/support/tickets/{ticketNumber}/status` | `SUPPORT:WRITE` | Changer le statut |
| POST | `/support/tickets/{ticketNumber}/messages` | `SUPPORT:WRITE` | Ajouter un message (interne ou visible client) |
| POST | `/support/tickets/{ticketNumber}/messages/{messageId}/convert-to-article` | `SUPPORT:WRITE` | Convertir un message en article KB |
| POST | `/support/tickets/{ticketNumber}/attachments` (multipart) | `SUPPORT:WRITE` | Uploader une pièce jointe |
| GET | `/support/tickets/{ticketNumber}/attachments` | `SUPPORT:READ` | Lister les pièces jointes (incl. internes) |
| GET | `/support/tickets/{ticketNumber}/attachments/{id}/download` | `SUPPORT:READ` | Télécharger une pièce jointe |
| DELETE | `/support/tickets/{ticketNumber}/attachments/{id}` | `SUPPORT:WRITE` | Supprimer une pièce jointe |
| POST | `/support/tickets/{ticketNumber}/tags` | `SUPPORT:WRITE` | Ajouter un tag |
| DELETE | `/support/tickets/{ticketNumber}/tags/{tag}` | `SUPPORT:WRITE` | Retirer un tag |
| POST | `/support/tickets/{ticketNumber}/task` | `SUPPORT:WRITE` + `TASK:WRITE` | Convertir le ticket en tâche |
| GET | `/support/tickets/by-owner` | `SUPPORT:READ` | Résumé Client 360 (`ownerType`+`ownerCode`) |
| GET | `/support/tickets/metrics` | `SUPPORT:METRICS` | Indicateurs temps réel (snapshot) |
| GET | `/support/tickets/analytics` | `SUPPORT:METRICS` | Analytics sur une période (`from`/`to`) |
| GET | `/support/tickets/analytics.csv` | `SUPPORT:METRICS` | Export CSV des analytics |
| GET | `/support/tickets/agents/performance` | `SUPPORT:METRICS` | Performance par agent sur une période |

**Filtres de recherche** (`GET /support/tickets`) : `status`, `assignedTo`, `ownerType`, `ownerCode`,
`category`, `priority`, `relatedType`, `relatedCode`, `overdueOnly` (booléen — uniquement les tickets
en dépassement SLA), `searchText` (recherche plein texte titre/description/contact), + pagination
standard (`page`, `size`, `sort`).

**Création** (`POST /support/tickets`) :
```json
{
  "title": "Problème d'accès au badge",
  "description": "Le badge ne fonctionne plus depuis ce matin",
  "priority": "HIGH",
  "category": "ACCESS",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-00045",
  "contactName": "Jeanne Dupont",
  "contactEmail": "jeanne@example.com",
  "contactPhone": "+243...",
  "relatedType": "BOOKING",
  "relatedCode": "BK-2026-000789"
}
```
Tous les champs sauf `title` sont optionnels. `firstResponseDueAt`/`resolutionDueAt` sont calculés
automatiquement à la création selon priorité/catégorie ; un email de confirmation est envoyé au
contact si `contactEmail` est renseigné.

**Ajout de message** (`POST /support/tickets/{ticketNumber}/messages`) :
```json
{ "senderType": "AGENT", "senderId": "12", "senderName": "Marc K.", "content": "...", "internal": false }
```
`internal: true` crée une note interne (visible uniquement back-office, jamais envoyée au client par
email, jamais retournée par les endpoints client). `internal: false` envoie un email au contact.

### 2.2 Tickets — espace client (`/client/support/tickets`, sans autorité `SUPPORT:*`, scope = identité connectée)

| Méthode | Chemin | Description |
|---|---|---|
| POST | `/client/support/tickets` | Créer un ticket (le client est `ownerType`/`ownerCode`) |
| GET | `/client/support/tickets` | Lister mes tickets (`ownerType`, `ownerCode`, `searchText`, pagination) |
| GET | `/client/support/tickets/{ticketNumber}` | Détail (filtré : pas de messages internes) |
| POST | `/client/support/tickets/{ticketNumber}/messages` | Répondre (forcé `senderType=CLIENT`, `internal=false`) |
| POST | `/client/support/tickets/{ticketNumber}/attachments` (multipart) | Uploader une pièce jointe (jamais interne) |
| GET | `/client/support/tickets/{ticketNumber}/attachments` | Lister mes pièces jointes (hors internes) |
| GET | `/client/support/tickets/{ticketNumber}/attachments/{id}/download` | Télécharger |
| PATCH | `/client/support/tickets/{ticketNumber}/close` | Fermer le ticket (depuis `RESOLVED`) |
| POST | `/client/support/tickets/{ticketNumber}/csat` | Soumettre une note de satisfaction |

> Important : côté client, `addMessage` répond et **rouvre automatiquement** un ticket `WAITING_CLIENT`/
> `RESOLVED` vers `IN_PROGRESS` (événement `REOPENED`), et notifie l'agent assigné par email.

### 2.3 Base de connaissances — back-office (`/support/knowledge-base`, autorité `SUPPORT:*`)

| Méthode | Chemin | Autorité | Description |
|---|---|---|---|
| GET | `/support/knowledge-base` | `SUPPORT:READ` | Recherche paginée (`category`, `searchText`) |
| GET | `/support/knowledge-base/{articleCode}` | `SUPPORT:READ` | Détail d'un article |
| POST | `/support/knowledge-base` | `SUPPORT:WRITE` | Créer un article |
| PUT | `/support/knowledge-base/{articleCode}` | `SUPPORT:WRITE` | Modifier un article |
| DELETE | `/support/knowledge-base/{articleCode}` | `SUPPORT:WRITE` | Supprimer un article |
| GET | `/support/knowledge-base/suggestions` | `SUPPORT:READ` | Suggestions d'articles (`category`, `searchText`, `limit`) |
| GET | `/support/knowledge-base/analytics` | `SUPPORT:METRICS` | Analytics KB sur une période (`from`/`to`) |

Création/édition (`KnowledgeArticleRequest`) :
```json
{
  "title": "Comment réinitialiser mon badge d'accès ?",
  "slug": "reinitialiser-badge-acces",
  "body": "...markdown ou HTML...",
  "category": "ACCESS",
  "tags": ["badge", "acces"],
  "publicVisible": true,
  "internalOnly": false,
  "active": true
}
```

Conversion d'un message de ticket en article (`POST /support/tickets/{ticketNumber}/messages/{messageId}/convert-to-article`) :
```json
{ "title": "...", "slug": "...", "category": "ACCESS", "tags": ["badge"], "publicVisible": true }
```
→ Permet de capitaliser une réponse d'agent jugée utile directement en article KB sans recopie manuelle.

Réponse `KnowledgeArticleResponse` inclut `viewCount`, `createdBy`/`updatedBy` (IDs agent), et les
indicateurs de visibilité `publicVisible` (visible côté client) / `internalOnly` (jamais exposé au client).

`KnowledgeAnalyticsResponse` :
```json
{
  "from": "2026-06-01T00:00:00Z", "to": "2026-06-30T23:59:59Z",
  "totalArticles": 48, "activeArticles": 42, "totalViews": 1320,
  "ticketsCreatedInPeriod": 96,
  "topArticles": [ { "articleCode": "KB-000012", "title": "...", "viewCount": 215 } ]
}
```

### 2.4 Base de connaissances — espace client (`/client/support/knowledge-base`, public/connecté)

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/client/support/knowledge-base` | Recherche (uniquement articles `publicVisible=true` et `active=true`) |
| GET | `/client/support/knowledge-base/{slug}` | Lecture d'un article (incrémente `viewCount`) |
| GET | `/client/support/knowledge-base/suggestions` | Suggestions (`category`, `searchText`, `limit`) |

### 2.5 Réponses rapides (`/support/quick-replies`, autorité `SUPPORT:*`)

| Méthode | Chemin | Autorité | Description |
|---|---|---|---|
| GET | `/support/quick-replies` | `SUPPORT:READ` | Lister (filtre `category` optionnel) |
| POST | `/support/quick-replies` | `SUPPORT:WRITE` | Créer (`{ category, title, body }`) |
| DELETE | `/support/quick-replies/{id}` | `SUPPORT:WRITE` | Supprimer |

→ Modèles de réponse réutilisables par catégorie, insérables en un clic dans l'éditeur de message agent.

### 2.6 Règles de routage (`/support/routing-rules`, autorité `SUPPORT:*`)

| Méthode | Chemin | Autorité | Description |
|---|---|---|---|
| GET | `/support/routing-rules` | `SUPPORT:READ` | Lister toutes les règles (ordonnées par `sortOrder`) |
| POST | `/support/routing-rules` | `SUPPORT:WRITE` | Créer une règle |
| PUT | `/support/routing-rules/{id}` | `SUPPORT:WRITE` | Modifier |
| DELETE | `/support/routing-rules/{id}` | `SUPPORT:WRITE` | Supprimer |

`RoutingRuleRequest`/`RoutingRuleResponse` :
```json
{
  "name": "Tickets facturation prioritaires",
  "active": true,
  "category": "BILLING",
  "priority": "HIGH",
  "ownerType": "BUSINESS_ENTITY",
  "relatedType": null,
  "assignedTo": 7,
  "teamCode": "BILLING_TEAM",
  "sortOrder": 1
}
```
→ À la création d'un ticket, les règles actives sont évaluées dans l'ordre de `sortOrder` ; la première
qui correspond aux critères (`category`/`priority`/`ownerType`/`relatedType`, chacun optionnel = joker)
détermine `assignedTo`/`teamCode` automatique.

### 2.7 Tags (`/support/tags`, autorité `SUPPORT:READ`)

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/support/tags` | Lister tous les tags existants (`{ id, name }[]`) — pour autocomplétion |

Ajout/retrait de tags se fait directement sur le ticket (`POST`/`DELETE /support/tickets/{ticketNumber}/tags...`).

### 2.8 CSAT public (`/public/support/csat`, sans authentification)

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/public/support/csat?ticket=...&score=...&token=...` | Page HTML de confirmation de notation (lien email) |

→ Ce endpoint **rend une page HTML autonome** (pas de JSON, pas de SPA) ; le lien est généré et signé
côté backend (`CsatTokenService`) et inséré dans l'email de demande de CSAT (`sendCsatRequest`).
Le frontend n'a rien à implémenter ici — uniquement à savoir que ce flux existe en parallèle du flux
client authentifié `POST /client/support/tickets/{ticketNumber}/csat`.

Soumission authentifiée (`SubmitCsatRequest`) :
```json
{ "score": 5, "comment": "Très réactif, merci !" }
```
`score` : entier 1 à 5, requis. Possible uniquement quand le ticket est `RESOLVED`/`CLOSED` et n'a pas
déjà reçu de note (sinon `400 Bad Request` → message contenant `"already"`).

### 2.9 Client 360 — résumé support par titulaire (`GET /support/tickets/by-owner`)

```
GET /support/tickets/by-owner?ownerType=MEMBER&ownerCode=MBR-00045
```
```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-00045",
  "openTickets": 2,
  "slaBreaches": 1,
  "avgCsatScore": 4.2,
  "atRisk": true,
  "recentTickets": [ /* 5 derniers SupportTicketResponse */ ]
}
```
`atRisk = true` si `openTickets >= 3` **ou** `slaBreaches > 0` **ou** `avgCsatScore < 3.0`.
→ Pensé pour être affiché en widget sur les fiches CRM/membre/client/entreprise (vue 360°).

### 2.10 Création automatique de tickets (déclenchée par d'autres modules)

Deux intégrations existantes créent des tickets de façon autonome via `createFromAutomation()`
(qui **déduplique** : aucun nouveau ticket n'est créé tant qu'un ticket `OPEN`/`IN_PROGRESS`/
`WAITING_CLIENT` existe déjà avec le même `relatedType`+`relatedCode`) :

1. **Anomalie de caisse à risque élevé** (`CashAnomalyDetectionServiceImpl`) : quand une anomalie de
   session de caisse est classée `HIGH`, un ticket `BILLING`/`HIGH` est créé automatiquement
   (`relatedType=CASH_ANOMALY_FLAG`, `relatedCode=<numéro du flag>`, `ownerType=CASHIER`).
2. **Quota mensuel de no-show dépassé** (`BookingPolicyEnforcer`) : quand un titulaire dépasse son
   quota mensuel d'absences (`maxNoShowsPerMonth`), un ticket `BOOKING`/`HIGH` est créé
   (`relatedType=BOOKING_NO_SHOW_QUOTA`, `relatedCode=<code du titulaire>`).

→ Ces tickets apparaissent dans la file d'attente agent comme n'importe quel autre ticket ; le frontend
n'a rien de spécifique à implémenter, mais peut afficher une **puce "Créé automatiquement"** quand
`relatedType` correspond à l'une de ces valeurs (utile pour le contexte agent).

### 2.11 Conversion ticket → tâche (`POST /support/tickets/{ticketNumber}/task`)

```json
{
  "title": "Vérifier le badge défectueux du membre MBR-00045",
  "description": "...",
  "assignedTo": 9,
  "priority": "HIGH",
  "dueAt": "2026-06-10T17:00:00Z",
  "checklist": [ { "label": "Contacter le prestataire badge", "done": false } ]
}
```
→ Crée une `TaskResponse` liée au ticket (visible dans le module `task`/Kanban), écrit un événement
`TASK_CREATED` dans la timeline. Nécessite **les deux** autorités `SUPPORT:WRITE` et `TASK:WRITE`.

### 2.12 Métriques temps réel (`GET /support/tickets/metrics`)

```json
{ "openTickets": 34, "overdueFirstResponse": 3, "overdueResolution": 1, "resolvedTickets": 512 }
```
→ Snapshot instantané (sans bornes de dates), pensé pour un bandeau de KPI en haut de la liste de tickets.

### 2.13 Analytics avancés et exports

`GET /support/tickets/analytics?from=...&to=...` (`from`/`to` optionnels, format ISO `DateTime`) :
```json
{
  "from": "2026-06-01T00:00:00Z", "to": "2026-06-30T23:59:59Z",
  "totalCreated": 96, "totalResolved": 88, "openAtEndOfPeriod": 12,
  "slaBreachCount": 7, "slaBreachRate": 7.3,
  "avgFirstResponseHours": 2.4, "avgResolutionHours": 18.6,
  "reopenedCount": 5, "waitingClientCount": 9,
  "byCategory": { "BILLING": 30, "BOOKING": 25, "ACCESS": 18, "TECHNICAL": 15, "OTHER": 8 },
  "byPriority": { "LOW": 10, "MEDIUM": 40, "HIGH": 35, "URGENT": 11 },
  "topTags": { "urgent": 22, "badge": 14, "facture": 11 },
  "volumeByRelatedType": { "BOOKING": 30, "CASH_ANOMALY_FLAG": 4, "BOOKING_NO_SHOW_QUOTA": 2 },
  "backlogByAgent": { "12": 8, "7": 5, "9": 3 },
  "avgCsatScore": 4.1, "csatResponseCount": 41,
  "csatByCategory": { "BILLING": 4.3, "ACCESS": 3.8 },
  "csatByAgent": { "12": 4.5, "7": 3.9 },
  "agentWorkload": [ { "agentId": 12, "assigned": 20, "resolved": 18 } ]
}
```

> ⚠️ **Note de cadrage** : `waitingClientCount` est le **nombre instantané** de tickets actuellement au
> statut `WAITING_CLIENT` (non borné par `from`/`to`) — c'est un proxy documenté pour le widget
> "en attente client", car le schéma actuel ne stocke pas d'horodatages structurés d'entrée/sortie de
> ce statut (uniquement des descriptions textuelles dans la timeline `STATUS_CHANGED`). Le frontend ne
> doit donc pas l'interpréter comme une "durée moyenne d'attente client" mais comme un compteur "à date".

`GET /support/tickets/analytics.csv?from=...&to=...` → export CSV (`text/csv`,
`Content-Disposition: attachment; filename=support-analytics.csv`) reprenant l'ensemble des sections
ci-dessus en sections CSV successives (catégorie, priorité, tags, type lié, backlog agent, CSAT...).
Le frontend peut simplement déclencher un téléchargement de fichier (lien direct ou `fetch` + blob).

`GET /support/tickets/agents/performance?from=...&to=...` :
```json
[
  {
    "agentId": 12, "assignedCount": 20, "resolvedCount": 18,
    "avgFirstResponseHours": 1.8, "avgResolutionHours": 14.2, "avgCsatScore": 4.5
  }
]
```
> `agentId` est le seul identifiant exposé (le modèle `Users`/`UserResponse` ne porte pas de nom
> affichable) — le frontend doit résoudre `agentId → nom complet` via l'annuaire agents/utilisateurs
> existant (ex. cache local des agents support déjà chargé pour les listes d'assignation).

---

## 3. Écrans proposés (back-office agent/admin)

### 3.1 `SupportDashboardPage` — tableau de bord support
- Bandeau de KPI alimenté par `GET /support/tickets/metrics` (tickets ouverts, dépassements 1ère
  réponse / résolution, total résolus).
- Raccourcis vers les files "À traiter", "Mes tickets", "En dépassement SLA" (filtres pré-remplis vers
  `SupportTicketListPage`).
- Widget "Performance agents (30 derniers jours)" résumant `GET /support/tickets/agents/performance`.

### 3.2 `SupportTicketListPage` — file de tickets
- Table paginée sur `GET /support/tickets` avec filtres combinables : statut, priorité, catégorie,
  agent assigné, titulaire (`ownerType`/`ownerCode`), objet lié (`relatedType`/`relatedCode`),
  `overdueOnly` (toggle "En retard uniquement"), recherche plein texte.
- Colonnes : numéro, titre, titulaire, priorité (badge coloré), statut (badge), agent assigné, échéance
  SLA (avec indicateur visuel rouge si dépassée), tags, dernière mise à jour.
- Actions rapides en ligne : assignation (popover liste agents), changement de statut, ajout de tag.
- Vue "Kanban" alternative groupée par statut (`OPEN` → `IN_PROGRESS` → `WAITING_CLIENT` →
  `RESOLVED` → `CLOSED`), drag & drop déclenchant `PATCH /status`.

### 3.3 `SupportTicketDetailPage` — fiche ticket
- En-tête : numéro, titre, badges statut/priorité/catégorie, titulaire (lien vers Client 360), agent
  assigné (sélecteur), échéances SLA avec compte à rebours, niveau d'escalade (`escalationLevel` avec
  icône d'alerte si > 0 et `escalationReason` en tooltip).
- Fil de conversation type messagerie : messages `messages[]` triés chronologiquement, distinction
  visuelle agent / client / **note interne** (`internal: true`, fond différent, jamais visible client),
  pièces jointes inline.
- Éditeur de réponse : zone de texte + toggle "Note interne / Réponse client" + bouton **réponses
  rapides** (liste filtrée par `category` du ticket, insertion en un clic via `GET /support/quick-replies`)
  + upload de pièce jointe (`multipart`).
- Panneau latéral : tags (ajout/retrait avec autocomplétion via `GET /support/tags`), objet lié
  (`relatedType`/`relatedCode` avec lien profond vers le module concerné), bouton **"Convertir en
  tâche"** (`POST /{ticketNumber}/task`, formulaire titre/description/assigné/priorité/échéance/checklist).
- Onglet "Timeline" : `GET /{ticketNumber}/timeline` — historique chronologique de tous les
  `TicketEventType` avec icônes différenciées (création, assignation, changement statut/priorité/
  catégorie, message, escalade SLA, CSAT reçu, réouverture, clôture auto, tâche créée).
- Action "Convertir un message en article KB" sur chaque message d'agent (icône au survol →
  ouvre un formulaire pré-rempli `ConvertMessageToArticleRequest`).
- Bandeau d'alerte si `escalationLevel > 0` ou échéance dépassée (couleur croissante avec la gravité).

### 3.4 `SupportKnowledgeBasePage` (admin) — gestion des articles
- Liste paginée/filtrée par catégorie et recherche (`GET /support/knowledge-base`), avec indicateurs
  `active`/`publicVisible`/`internalOnly` et `viewCount`.
- Éditeur d'article (markdown ou riche texte) : titre, slug (auto-généré, éditable), corps, catégorie,
  tags, visibilité (public / interne uniquement / actif).
- Onglet "Analytics KB" : `GET /support/knowledge-base/analytics` — total articles/actifs/vues,
  tickets créés sur la période, top articles consultés (graphique barres horizontal).

### 3.5 `SupportQuickRepliesPage` (admin) — gestion des réponses rapides
- Liste/CRUD simple groupée par catégorie (`GET`/`POST`/`DELETE /support/quick-replies`).
- Aperçu du rendu du corps avant sauvegarde.

### 3.6 `SupportRoutingRulesPage` (admin) — gestion des règles de routage
- Table ordonnée par `sortOrder` avec drag & drop pour réordonner (`PUT` avec `sortOrder` mis à jour).
- Formulaire de règle : nom, actif (toggle), critères (`category`/`priority`/`ownerType`/`relatedType`,
  chacun optionnel = "tous"), cible (`assignedTo` agent ou `teamCode` équipe).
- Bandeau explicatif : "Les règles sont évaluées dans l'ordre ; la première correspondance s'applique."

### 3.7 `SupportAnalyticsPage` (admin) — analytics et exports
- Sélecteur de période (`from`/`to`, préréglages "7 jours", "30 jours", "Ce mois", "Personnalisé").
- KPI principaux en cartes : créés, résolus, ouverts en fin de période, taux de dépassement SLA,
  temps moyen 1ère réponse / résolution, tickets réouverts, en attente client (avec l'infobulle de
  cadrage citée en §2.13).
- Graphiques : répartition par catégorie / priorité (camemberts ou barres), top tags (nuage ou liste),
  volume par type d'objet lié, charge (backlog) par agent, CSAT moyen par catégorie / par agent.
- Bouton "Exporter en CSV" → `GET /support/tickets/analytics.csv` (téléchargement direct).
- Lien vers `SupportAgentPerformancePage` pour le détail par agent.

### 3.8 `SupportAgentPerformancePage` (admin) — performance par agent
- Table sur `GET /support/tickets/agents/performance` (même sélecteur de période que l'analytics) :
  agent (résolu via l'annuaire interne), nombre assigné/résolu, taux de résolution, temps moyens
  1ère réponse/résolution, CSAT moyen — triable par chaque colonne, utile pour le coaching d'équipe.

### 3.9 Widget `ClientSupportSummaryWidget` — intégration Client 360
- Petit composant réutilisable injecté dans les fiches CRM / membre / client / entreprise, alimenté par
  `GET /support/tickets/by-owner`.
- Affiche : badge "À risque" (rouge) si `atRisk=true`, compteurs tickets ouverts / dépassements SLA,
  CSAT moyen (étoiles), liste des 5 tickets récents avec lien vers `SupportTicketDetailPage`.
- Bouton "Créer un ticket pour ce titulaire" → ouvre le formulaire de création pré-rempli avec
  `ownerType`/`ownerCode`.

---

## 4. Écrans proposés (espace client)

### 4.1 `ClientSupportTicketListPage` — "Mon support"
- Liste paginée de mes tickets (`GET /client/support/tickets`), filtrable par recherche texte et statut
  (filtrage côté UI sur les résultats déjà retournés, ou en réutilisant la recherche serveur).
- Bouton "Nouveau ticket" → `ClientSupportTicketCreatePage`.
- Indicateur visuel si une réponse agent attend ma lecture (statut `WAITING_CLIENT` mis en avant).

### 4.2 `ClientSupportTicketCreatePage` — créer un ticket
- Formulaire simple : titre, description, catégorie (sélecteur avec icônes), objet lié optionnel
  (sélection contextuelle si lancé depuis une réservation/facture — pré-remplit `relatedType`/`relatedCode`).
- **Suggestions KB en direct** : pendant la saisie du titre/description, appeler
  `GET /client/support/knowledge-base/suggestions?searchText=...&category=...` (debounced) et afficher
  les articles pertinents dans un panneau latéral — *"Cette réponse résout peut-être votre problème ?"*
  avant même la création du ticket (réduit le volume entrant).

### 4.3 `ClientSupportTicketDetailPage` — suivi d'un ticket
- Fil de conversation (messages non internes uniquement), pièces jointes, statut avec explication en
  langage clair ("En cours de traitement", "En attente de votre réponse", "Résolu — confirmez la
  clôture ou répondez pour rouvrir").
- Zone de réponse + upload pièce jointe.
- Bouton "Marquer comme résolu / Fermer" visible quand `status=RESOLVED` (`PATCH /close`).
- Bloc de notation CSAT visible quand `status` ∈ {`RESOLVED`,`CLOSED`} et `csatSubmittedAt=null`
  (étoiles 1-5 + commentaire optionnel, `POST /csat`).

### 4.4 `ClientKnowledgeBasePage` — centre d'aide
- Recherche + filtre par catégorie sur les articles publics (`GET /client/support/knowledge-base`).
- Page de lecture d'article (`GET /{slug}`) avec rendu du corps, tags, articles suggérés liés.
- Bandeau "Vous ne trouvez pas de réponse ? Contactez le support" → lien vers création de ticket.

---

## 5. Workflows proposés

### 5.1 Workflow agent — triage et résolution d'un ticket
1. L'agent ouvre `SupportDashboardPage`, voit les KPI et clique sur "En dépassement SLA" ou "À traiter".
2. `SupportTicketListPage` affiche la file filtrée ; l'agent ouvre un ticket → `SupportTicketDetailPage`.
3. L'agent s'assigne le ticket (ou il est déjà routé automatiquement via une règle de routage) →
   `PATCH /assign`, statut passe en pratique à `IN_PROGRESS`.
4. L'agent consulte la timeline et l'historique des messages, éventuellement le panneau Client 360
   (lien depuis le titulaire) pour avoir le contexte complet (autres tickets, factures, réservations).
5. L'agent répond (réponse rapide ou message libre), bascule en `WAITING_CLIENT` si une action client
   est attendue, ou ajoute une note interne pour ses collègues.
6. Si le sujet dépasse le cadre du support (ex. intervention technique), l'agent convertit le ticket en
   tâche (`POST /task`) assignée à l'équipe compétente — la tâche reste tracée et liée au ticket.
7. Une fois la solution confirmée, l'agent passe le statut à `RESOLVED` → un email avec lien CSAT est
   envoyé automatiquement au client.
8. Si la réponse s'avère réutilisable, l'agent la convertit en article de base de connaissances
   (icône sur le message → formulaire pré-rempli) pour réduire les tickets similaires futurs.

### 5.2 Workflow client — création et suivi d'un ticket
1. Le client ouvre "Mon support" (`ClientSupportTicketListPage`) ou clique sur "Besoin d'aide" depuis
   une autre section (réservation, facture) — dans ce cas `relatedType`/`relatedCode` sont pré-remplis.
2. Sur `ClientSupportTicketCreatePage`, en tapant son problème, des suggestions d'articles KB
   s'affichent en direct ; si l'une d'elles répond à la question, le client peut abandonner la création.
3. Sinon il soumet le formulaire → ticket créé, email de confirmation envoyé, redirection vers
   `ClientSupportTicketDetailPage`.
4. Le client suit les échanges, reçoit des emails à chaque réponse d'agent, peut joindre des fichiers,
   et répondre — toute réponse client sur un ticket `WAITING_CLIENT`/`RESOLVED` le **rouvre**
   automatiquement (`IN_PROGRESS`, événement `REOPENED`) et notifie l'agent.
5. Une fois satisfait, le client peut fermer le ticket lui-même (`PATCH /close`) ou laisser le système
   le clore automatiquement après un délai d'inactivité post-résolution (`AUTO_CLOSED`).
6. Le client reçoit un email de demande de notation CSAT (lien signé, page autonome) ou peut noter
   directement depuis `ClientSupportTicketDetailPage`.

### 5.3 Workflow — suggestion de base de connaissances pendant la création
1. Le client (ou l'agent, lors de la création manuelle d'un ticket) saisit un titre/une description.
2. Le frontend interroge (debounced, ~300ms) `GET .../knowledge-base/suggestions?searchText=...&category=...`.
3. Les articles suggérés s'affichent en panneau latéral avec extrait (`excerpt`).
4. Le clic sur un article ouvre sa lecture en aperçu (modal ou nouvel onglet) sans perdre la saisie en
   cours ; si l'article résout le problème, le formulaire peut être abandonné.

### 5.4 Workflow — escalade SLA et alertes
1. Le worker `SupportSlaEscalationWorker` détecte (toutes les heures) les tickets en dépassement de
   1ère réponse ou de résolution.
2. Des emails d'alerte sont envoyés (à l'agent assigné, puis selon le niveau : manager support, puis
   admin/direction) ; en cas de dépassement de résolution, la priorité du ticket est **automatiquement
   augmentée** d'un cran (ex. `MEDIUM → HIGH`).
3. Le ticket affiche dans l'UI un badge d'escalade (`escalationLevel` 1 à 3) avec la raison
   (`escalationReason`) en tooltip — l'agent et son manager voient immédiatement la criticité.
4. Le frontend devrait permettre de filtrer/trier la liste de tickets par `overdueOnly=true` et par
   `escalationLevel` pour prioriser visuellement ces dossiers.

### 5.5 Workflow — conversion ticket → tâche
1. Depuis `SupportTicketDetailPage`, l'agent clique "Convertir en tâche".
2. Un formulaire pré-rempli (titre dérivé du ticket, description, checklist optionnelle) s'ouvre ;
   l'agent choisit l'assigné, la priorité et l'échéance.
3. À la soumission (`POST /{ticketNumber}/task`), une tâche est créée dans le module Task/Kanban et
   un événement `TASK_CREATED` apparaît dans la timeline du ticket avec un lien profond vers la tâche.
4. Le ticket reste piloté indépendamment (le client continue de suivre son ticket, l'équipe interne
   pilote la tâche) — utile pour découpler le suivi client du travail opérationnel sous-jacent.

### 5.6 Workflow — Client 360 / vue croisée CRM-Support
1. Sur une fiche CRM (membre, client, entreprise), le widget `ClientSupportSummaryWidget` affiche le
   résumé support du titulaire (`GET /support/tickets/by-owner`).
2. Si `atRisk=true`, un badge visuel alerte le commercial/gestionnaire de compte (risque de churn :
   beaucoup de tickets ouverts, dépassements SLA, ou faible CSAT moyen).
3. Le gestionnaire peut cliquer sur un ticket récent pour ouvrir son détail, ou créer directement un
   nouveau ticket pré-rempli pour ce titulaire (ex. suivi proactif après une réclamation commerciale).

### 5.7 Workflow — création automatique de tickets depuis d'autres modules
1. Un autre module détecte un événement nécessitant un suivi support (anomalie de caisse à risque
   élevé, quota de no-show dépassé) et appelle `createFromAutomation()`.
2. Si un ticket équivalent (`relatedType`+`relatedCode`, statut actif) existe déjà, rien n'est créé
   (anti-spam) ; sinon un nouveau ticket est créé avec priorité `HIGH` et catégorie adaptée.
3. Le ticket apparaît dans la file agent comme tout autre ticket — le frontend peut afficher une
   puce "Créé automatiquement — {relatedType}" pour donner le contexte d'origine immédiatement.
4. L'agent traite le ticket normalement ; le lien `relatedType`/`relatedCode` permet de naviguer vers
   l'objet d'origine (flag de caisse, dossier de réservation du titulaire, etc.).

### 5.8 Workflow — administration des règles de routage et réponses rapides
1. Un admin support ouvre `SupportRoutingRulesPage`, crée/réordonne des règles par glisser-déposer
   (persisté via `sortOrder`).
2. Lors de la création d'un ticket (manuelle, client, ou automatique), le backend évalue les règles
   actives dans l'ordre et assigne automatiquement l'agent/équipe à la première correspondance.
3. En parallèle, les admins alimentent `SupportQuickRepliesPage` avec des modèles de réponse par
   catégorie ; les agents les retrouvent instantanément dans l'éditeur de message du ticket, ce qui
   accélère le traitement des demandes récurrentes.

---

## 6. Récapitulatif des autorités requises

| Autorité | Usage |
|---|---|
| `SUPPORT:READ` / `SUPPORT_READ` | Lecture tickets, timeline, pièces jointes, tags, KB, règles, réponses rapides |
| `SUPPORT:WRITE` / `SUPPORT_WRITE` | Création/modification tickets, messages, pièces jointes, tags, KB, règles, réponses rapides |
| `SUPPORT:ASSIGN` / `SUPPORT_ASSIGN` | Assignation de tickets à un agent |
| `SUPPORT:METRICS` / `SUPPORT_METRICS` | Metrics, analytics, export CSV, performance agents, analytics KB |
| `TASK:WRITE` / `TASK_WRITE` | Requis **en plus** de `SUPPORT:WRITE` pour la conversion ticket → tâche |

Les endpoints `/client/support/**` ne requièrent aucune de ces autorités — ils sont scopés à
l'identité du titulaire connecté côté portail client. L'endpoint `/public/support/csat` est totalement
public (sécurisé par token signé dans l'URL, pas par authentification de session).

---

## 7. WebSocket — Messages en temps réel

Le module support expose un canal WebSocket (STOMP over SockJS) pour recevoir les nouveaux messages
d'un ticket **en temps réel**, sans rechargement de page.

### 7.1 Architecture

```
┌──────────┐    POST /messages     ┌──────────────┐   afterCommit()   ┌──────────────────┐
│ Frontend │ ────────────────────> │   REST API   │ ───────────────> │ SimpMessaging    │
│ (Agent / │                      │ addMessage() │                   │ Template         │
│  Client) │                      └──────────────┘                   └────────┬─────────┘
│          │                                                                  │
│          │  <──── /topic/support/tickets/{ticketNumber} ────────────────────┘
│          │        (TicketMessageResponse)
└──────────┘
```

- Les messages sont broadcastés **après le commit de la transaction** pour éviter d'envoyer des données
  qui pourraient être rollback.
- Les deux sources de messages sont couvertes : ajout via l'API REST (`addMessage`) et réception
  d'emails entrants (`addEmailReply`).

### 7.2 Connexion

| Paramètre | Valeur |
|---|---|
| **Endpoint** | `/ws` (SockJS activé) |
| **Protocole** | STOMP over WebSocket / SockJS fallback |
| **Auth** | Header STOMP `Authorization: Bearer <jwt>` envoyé au `CONNECT` |

**Exemple de connexion (JavaScript avec `@stomp/stompjs` + `sockjs-client`) :**

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const stompClient = new Client({
  webSocketFactory: () => new SockJS('https://<host>/ws'),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  reconnectDelay: 5000,
});

stompClient.activate();
```

### 7.3 Souscription aux messages d'un ticket

Une fois connecté, souscrire au topic du ticket en cours de consultation :

```javascript
stompClient.onConnect = () => {
  stompClient.subscribe(
    `/topic/support/tickets/${ticketNumber}`,
    (message) => {
      const newMessage = JSON.parse(message.body);
      // newMessage est un TicketMessageResponse
      // → Ajouter au fil de conversation sans recharger la page
    }
  );
};
```

### 7.4 Payload reçu — `TicketMessageResponse`

Chaque message reçu sur le topic est un objet `TicketMessageResponse` :

```json
{
  "id": 42,
  "senderType": "AGENT",
  "senderId": "12",
  "senderName": "Marc K.",
  "content": "Votre badge a été réinitialisé, merci de tester.",
  "internal": false,
  "createdAt": "2026-06-20T14:30:00Z"
}
```

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` | ID unique du message |
| `senderType` | `TicketSenderType` | `CLIENT`, `AGENT`, ou `SYSTEM` |
| `senderId` | `String` | Identifiant de l'expéditeur (ID utilisateur ou email) |
| `senderName` | `String` | Nom affiché de l'expéditeur |
| `content` | `String` | Contenu du message (rich text normalisé) |
| `internal` | `Boolean` | `true` = note interne (visible uniquement back-office) |
| `createdAt` | `Instant` | Horodatage ISO 8601 |

> **Note :** Les messages avec `internal: true` sont envoyés sur le même topic. Côté client portail,
> filtrer ces messages pour ne pas les afficher (`internal === false` uniquement).

### 7.5 Intégration recommandée

#### Back-office (écran agent)

1. À l'ouverture du détail d'un ticket, charger les messages via `GET /support/tickets/{ticketNumber}`.
2. Se connecter au WebSocket et souscrire à `/topic/support/tickets/{ticketNumber}`.
3. À chaque message reçu, l'ajouter au fil de conversation en temps réel.
4. Se désabonner et déconnecter en quittant la page du ticket.

#### Portail client

1. À l'ouverture du détail d'un ticket, charger via `GET /client/support/tickets/{ticketNumber}`.
2. Se connecter au WebSocket et souscrire au même topic.
3. **Filtrer les messages** : n'afficher que ceux avec `internal === false`.
4. Se désabonner et déconnecter en quittant la page.

#### Gestion des erreurs et reconnexion

```javascript
stompClient.onStompError = (frame) => {
  console.error('STOMP error:', frame.headers['message']);
};

stompClient.onWebSocketClose = () => {
  // La reconnexion est automatique avec reconnectDelay
  console.log('WebSocket fermé, reconnexion en cours...');
};
```

### 7.6 Notes techniques

- **SockJS** : le endpoint `/ws` supporte SockJS comme fallback pour les navigateurs/proxies qui
  bloquent les WebSocket natifs.
- **CORS** : les origines sont configurées avec `allowedOriginPatterns("*")`.
- **Sécurité** : l'authentification se fait au niveau STOMP (pas HTTP) — le JWT est validé lors du
  frame `CONNECT` par le `WebSocketAuthInterceptor`. L'endpoint HTTP `/ws/**` est ouvert (permitAll).
- **Pas de persistance WebSocket** : le WebSocket ne remplace pas le REST. Si un client n'était pas
  connecté au moment de l'envoi, il retrouvera les messages au prochain chargement via l'API REST.
