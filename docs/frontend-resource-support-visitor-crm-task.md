# Guide frontend - Resource 2.5, securite DB, Support, Visitor, CRM, Task

Base API: `/sni/api/v1`

Ce document couvre les changements ajoutes pour:

- Resource 2.5;
- migration roles/permissions vers la logique DB;
- Support/Ticketing;
- Visitor Management;
- CRM;
- Task Management.

Note environnement: la politique CORS reste permissive pendant la phase de test. Le frontend peut continuer a appeler l'API depuis les environnements locaux/de test.

## 1. Roles et permissions DB

Le backend expose maintenant les roles et les permissions DB dans les authorities Spring Security.

Format supporte:

- role: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`;
- permission lisible: `RESOURCE:READ`, `SUPPORT:WRITE`, `VISITOR:CHECKIN`;
- permission legacy interne: `RESOURCE_READ`, `SUPPORT_WRITE`, `VISITOR_CHECKIN`.

Le frontend doit utiliser les permissions fonctionnelles pour afficher/masquer les actions.

Permissions ajoutees:

| Module | Permissions |
| --- | --- |
| Resource | `RESOURCE:READ`, `RESOURCE:WRITE`, `RESOURCE:PRICE`, `RESOURCE:GALLERY` |
| Support | `SUPPORT:READ`, `SUPPORT:WRITE`, `SUPPORT:ASSIGN`, `SUPPORT:METRICS` |
| Visitor | `VISITOR:READ`, `VISITOR:WRITE`, `VISITOR:CHECKIN` |
| CRM | `CRM:READ`, `CRM:WRITE`, `CRM:CONVERT` |
| Task | `TASK:READ`, `TASK:WRITE`, `TASK:ASSIGN` |

Usage UI recommande:

- `RESOURCE:PRICE`: afficher les actions de tarification dynamique;
- `RESOURCE:GALLERY`: afficher les actions d'ajout/modification photos;
- `SUPPORT:ASSIGN`: afficher le bouton d'assignation;
- `VISITOR:CHECKIN`: afficher les boutons check-in/check-out;
- `CRM:CONVERT`: afficher l'action de conversion lead;
- `TASK:ASSIGN`: afficher l'action de reassignation.

## 2. Resource 2.5

### Tarification dynamique

Endpoint creation:

`POST /resource-pricing-rules`

Payload:

```json
{
  "resourceCode": "RES-MEETING-01",
  "bookingUnit": "HOUR",
  "price": 10000,
  "label": "Off-peak matin",
  "dayOfWeek": 1,
  "startsAt": "07:00:00",
  "endsAt": "09:00:00",
  "adjustmentType": "PERCENT_DELTA",
  "adjustmentValue": -20,
  "validFrom": "2026-05-01",
  "validUntil": "2026-12-31",
  "lastMinuteMinutes": null,
  "priority": 10,
  "active": true
}
```

Champs:

- `price`: prix de base de la regle;
- `dayOfWeek`: 1 = lundi, 7 = dimanche;
- `startsAt` / `endsAt`: plage horaire d'application;
- `adjustmentType`: `FIXED_PRICE`, `AMOUNT_DELTA`, `PERCENT_DELTA`;
- `adjustmentValue`: montant fixe ou pourcentage selon le type;
- `lastMinuteMinutes`: regle applicable uniquement si la reservation est proche;
- `priority`: plus haut = plus prioritaire.

Endpoints:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/resource-pricing-rules` | Creer une regle |
| `GET` | `/resource-pricing-rules` | Lister les regles |
| `GET` | `/resource-pricing-rules?resourceCode=RES-001` | Regles d'une ressource |
| `GET` | `/resource-pricing-rules/{id}` | Detail regle |
| `PATCH` | `/resource-pricing-rules/{id}/active?active=false` | Activer/desactiver |
| `DELETE` | `/resource-pricing-rules/{id}` | Supprimer |
| `GET` | `/resource-pricing-rules/quote?resourceCode=RES-001&bookingUnit=HOUR&startedAt=2026-05-10T09:00:00&endedAt=2026-05-10T10:00:00` | Obtenir le prix final |

Reponse quote:

```json
{
  "resourceCode": "RES-MEETING-01",
  "bookingUnit": "HOUR",
  "startedAt": "2026-05-10T09:00:00",
  "endedAt": "2026-05-10T10:00:00",
  "basePrice": 10000,
  "finalPrice": 8000,
  "appliedRuleId": 12,
  "appliedRuleLabel": "Off-peak matin",
  "adjustmentType": "PERCENT_DELTA",
  "adjustmentValue": -20
}
```

UI:

- afficher une section `Tarification` dans le detail ressource;
- ajouter un simulateur de prix;
- afficher `basePrice`, `finalPrice` et la regle appliquee;
- gerer les badges `Peak`, `Off-peak`, `Weekend`, `Last minute` cote frontend via `label`.

### Calendrier public de disponibilite

Endpoint public:

`GET /public/resources/{resourceCode}/calendar?fromDate=2026-05-01&toDate=2026-05-31`

Reponse:

```json
{
  "resourceCode": "RES-MEETING-01",
  "resourceName": "Salle Reunion A",
  "fromDate": "2026-05-01",
  "toDate": "2026-05-31",
  "days": [
    {
      "date": "2026-05-10",
      "totalSlots": 16,
      "availableSlots": 8,
      "totalCapacity": 16,
      "remainingCapacity": 8,
      "status": "PARTIAL"
    }
  ]
}
```

Status:

- `AVAILABLE`: tous les slots sont disponibles;
- `PARTIAL`: certains slots sont disponibles;
- `FULL`: aucun slot disponible ou aucun slot configure.

UI:

- calendrier mensuel lecture seule;
- couleur verte pour `AVAILABLE`;
- orange pour `PARTIAL`;
- rouge/gris pour `FULL`;
- clic sur un jour: ouvrir la selection de creneau si des slots existent.

### Galerie photos ressource

Endpoints:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/resources/{resourceCode}/photos` | Ajouter une photo liee a un document |
| `GET` | `/resources/{resourceCode}/photos` | Lister la galerie |
| `PATCH` | `/resources/{resourceCode}/photos/{id}` | Modifier metadata photo |
| `DELETE` | `/resources/{resourceCode}/photos/{id}` | Desactiver une photo |

Payload:

```json
{
  "documentCode": "DOC-202604-000123",
  "caption": "Vue principale",
  "cover": true,
  "displayOrder": 1,
  "active": true
}
```

Reponse:

```json
{
  "id": 1,
  "resourceCode": "RES-MEETING-01",
  "documentCode": "DOC-202604-000123",
  "caption": "Vue principale",
  "cover": true,
  "displayOrder": 1,
  "active": true
}
```

Workflow frontend:

1. Uploader le fichier via `/documents/upload`.
2. Recuperer `documentCode`.
3. Ajouter la photo via `/resources/{resourceCode}/photos`.
4. Afficher l'image via `/documents/{documentCode}/preview`.

### Amenities enrichis

Le lien ressource-amenity accepte maintenant:

```json
{
  "resourceCode": "RES-MEETING-01",
  "amenityCode": "PROJECTOR",
  "quantity": 1,
  "optional": true,
  "extraPrice": 5000
}
```

La liste des amenities retourne aussi:

```json
{
  "code": "PROJECTOR",
  "name": "Video projecteur",
  "description": "HDMI",
  "active": true,
  "quantity": 1,
  "optional": true,
  "extraPrice": 5000
}
```

UI:

- afficher quantite: `8 chaises`, `1 projecteur`;
- afficher badge `Optionnel`;
- afficher supplement si `extraPrice > 0`;
- dans le booking, afficher les options payantes distinctement du prix de base.

## 3. Support / Ticketing

### Endpoints admin

| Methode | Endpoint | Permission | Usage |
| --- | --- | --- | --- |
| `POST` | `/support/tickets` | `SUPPORT:WRITE` | Creer un ticket |
| `GET` | `/support/tickets` | `SUPPORT:READ` | Lister/rechercher |
| `GET` | `/support/tickets/{ticketNumber}` | `SUPPORT:READ` | Detail ticket |
| `PATCH` | `/support/tickets/{ticketNumber}/assign` | `SUPPORT:ASSIGN` | Assigner |
| `PATCH` | `/support/tickets/{ticketNumber}/status` | `SUPPORT:WRITE` | Changer statut |
| `POST` | `/support/tickets/{ticketNumber}/messages` | `SUPPORT:WRITE` | Repondre |
| `GET` | `/support/tickets/{ticketNumber}/timeline` | `SUPPORT:READ` | Historique evenements du ticket |
| `POST` | `/support/tickets/{ticketNumber}/task` | `SUPPORT:WRITE` + `TASK:WRITE` | Convertir le ticket en tache operationnelle |
| `GET` | `/support/tickets/by-owner?ownerType=&ownerCode=` | `SUPPORT:READ` | Resume support pour la fiche client/membre (Client 360) |
| `GET` | `/support/tickets/metrics` | `SUPPORT:METRICS` | Metriques SLA |
| `GET` | `/support/tickets/analytics?from=&to=` | `SUPPORT:METRICS` | Analytics enrichis sur la periode |
| `GET` | `/support/tickets/analytics.csv?from=&to=` | `SUPPORT:METRICS` | Export CSV des analytics |
| `GET` | `/support/tickets/agents/performance?from=&to=` | `SUPPORT:METRICS` | Performance par agent sur la periode |

Filtres:

- `status`;
- `assignedTo`;
- `ownerType`;
- `ownerCode`;
- pagination standard Spring.

Creation:

```json
{
  "title": "Probleme de facture",
  "description": "Le montant ne correspond pas au devis.",
  "priority": "HIGH",
  "category": "BILLING",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "contactName": "Jean Dupont",
  "contactEmail": "jean@example.com",
  "contactPhone": "+242000000000",
  "relatedType": "INVOICE",
  "relatedCode": "INV-202604-0001"
}
```

Reponse:

```json
{
  "ticketNumber": "TCK-1777480000000",
  "title": "Probleme de facture",
  "status": "OPEN",
  "priority": "HIGH",
  "category": "BILLING",
  "assignedTo": null,
  "firstResponseDueAt": "2026-04-29T20:00:00Z",
  "resolutionDueAt": "2026-04-30T18:00:00Z",
  "messages": []
}
```

Statuts:

- `OPEN`;
- `IN_PROGRESS`;
- `WAITING_CLIENT`;
- `RESOLVED`;
- `CLOSED`.

Priorites:

- `LOW`;
- `MEDIUM`;
- `HIGH`;
- `URGENT`.

Categories:

- `BILLING`;
- `BOOKING`;
- `ACCESS`;
- `TECHNICAL`;
- `OTHER`.

### Endpoints client

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/client/support/tickets` | Ouvrir un ticket |
| `GET` | `/client/support/tickets?ownerType=MEMBER&ownerCode=MBR-000001` | Mes tickets |
| `POST` | `/client/support/tickets/{ticketNumber}/messages` | Repondre |
| `PATCH` | `/client/support/tickets/{ticketNumber}/close` | Clore |

### Conversion ticket -> tache

Depuis `SupportTicketDetailPage`, un agent disposant de `SUPPORT:WRITE` et `TASK:WRITE` peut transformer
un ticket en tache operationnelle via `POST /support/tickets/{ticketNumber}/task`:

```json
{
  "title": "Recontacter le client pour verifier la facture",
  "description": "Suite au ticket TCK-1777480000000, verifier le calcul et rappeler le client.",
  "assignedTo": 12,
  "priority": "HIGH",
  "dueAt": "2026-04-30T18:00:00Z",
  "checklist": [
    { "label": "Verifier le montant facture", "completed": false, "displayOrder": 1 },
    { "label": "Rappeler le client", "completed": false, "displayOrder": 2 }
  ]
}
```

La tache creee porte `sourceType = "SUPPORT_TICKET"` et `sourceCode = ticketNumber`: elle est donc
retrouvable depuis `GET /tasks?sourceType=SUPPORT_TICKET` (cf. section 6) et un evenement
`TASK_CREATED` est ajoute a la timeline du ticket (`GET /support/tickets/{ticketNumber}/timeline`).
La reponse est l'objet `TaskResponse` du module Task (memes champs que `POST /tasks`).

UI admin:

- `SupportTicketListPage` avec filtres statut, priorite, agent, owner;
- `SupportTicketDetailPage` avec timeline messages et evenements (`/timeline`);
- panneau SLA: premiere reponse, resolution;
- bouton assigner si `SUPPORT:ASSIGN`;
- bouton "Convertir en tache" si `SUPPORT:WRITE` + `TASK:WRITE`, ouvrant un formulaire reprenant
  titre/description du ticket et permettant d'ajouter une checklist;
- badges couleur par priorite.

UI client:

- `ClientSupportPage`;
- ouvrir ticket;
- voir messages;
- ajouter reponse;
- fermer ticket.

### Client 360 / resume support par owner

`GET /support/tickets/by-owner?ownerType=MEMBER&ownerCode=MBR-000001` (`SUPPORT:READ`) retourne un
resume du support pour un membre/client/entreprise donne, destine aux widgets de la fiche client (CRM)
et de la fiche membre/visiteur :

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "openTickets": 2,
  "slaBreaches": 1,
  "avgCsatScore": 3.5,
  "atRisk": true,
  "recentTickets": [ /* 5 derniers SupportTicketResponse, plus recents en premier */ ]
}
```

`atRisk` est calcule cote serveur: vrai si l'owner a au moins 3 tickets ouverts, au moins une
violation de SLA en cours, ou une note CSAT moyenne inferieure a 3/5. `avgCsatScore` est `null`
si l'owner n'a soumis aucune evaluation CSAT.

UI suggeree: widgets "Tickets ouverts", "Derniers tickets", "CSAT moyen", "Violations SLA" et un
badge "Client a risque" sur la fiche client/membre (CRM 360), alimentes par cet endpoint unique.

### Creation automatique de tickets depuis les anomalies

Deux flux declenchent desormais la creation automatique d'un ticket de support
(`SupportTicketService.createFromAutomation`, qui evite les doublons en verifiant qu'aucun ticket
`OPEN`/`IN_PROGRESS`/`WAITING_CLIENT` n'existe deja avec le meme `relatedType`/`relatedCode`) :

- **Anomalies de caisse (facturation)**: toute anomalie de caisse de severite `HIGH` detectee par
  `CashAnomalyDetectionServiceImpl` ouvre un ticket `BILLING` / `HIGH` avec
  `ownerType = "CASHIER"`, `ownerCode = <caissier>`, `relatedType = "CASH_ANOMALY_FLAG"`,
  `relatedCode = <numero du flag>`.
- **Quota de no-show (reservation)**: lorsque `BookingPolicyEnforcer` detecte qu'un owner a atteint
  le quota mensuel de no-show de sa politique de reservation, un ticket `BOOKING` / `HIGH` est cree
  avec `ownerType`/`ownerCode` correspondant au client, `relatedType = "BOOKING_NO_SHOW_QUOTA"`,
  `relatedCode = <ownerCode>`, **avant** que la reservation ne soit rejetee — le ticket est cree dans
  une transaction independante (`PROPAGATION_REQUIRES_NEW`) afin de survivre au rollback de la
  creation de reservation refusee.

Ces tickets apparaissent dans `SupportTicketListPage` comme tout autre ticket et alimentent le widget
"Derniers tickets" du resume Client 360 ci-dessus.

### Base de connaissances (Knowledge Base)

Endpoints admin (`SUPPORT:READ`/`SUPPORT:WRITE`/`SUPPORT:METRICS`):

| Methode | Endpoint | Permission | Usage |
| --- | --- | --- | --- |
| `GET` | `/support/knowledge-base?category=&searchText=` | `SUPPORT:READ` | Lister/rechercher les articles |
| `GET` | `/support/knowledge-base/{articleCode}` | `SUPPORT:READ` | Detail d'un article |
| `POST` | `/support/knowledge-base` | `SUPPORT:WRITE` | Creer un article |
| `PUT` | `/support/knowledge-base/{articleCode}` | `SUPPORT:WRITE` | Modifier un article |
| `DELETE` | `/support/knowledge-base/{articleCode}` | `SUPPORT:WRITE` | Desactiver (soft-delete) |
| `GET` | `/support/knowledge-base/suggestions?category=&searchText=&limit=5` | `SUPPORT:READ` | Suggestions (articles publics + internes) |
| `GET` | `/support/knowledge-base/analytics?from=&to=` | `SUPPORT:METRICS` | Articles consultes vs tickets crees sur la periode |
| `POST` | `/support/tickets/{ticketNumber}/messages/{messageId}/convert-to-article` | `SUPPORT:WRITE` | Convertir un message agent en article |

Endpoints publics/client (authentifie, sans permission specifique — comme `/client/support/tickets`):

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `GET` | `/client/support/knowledge-base?category=&searchText=` | Rechercher les articles publics actifs |
| `GET` | `/client/support/knowledge-base/{slug}` | Lire un article (incremente `viewCount`) |
| `GET` | `/client/support/knowledge-base/suggestions?category=&searchText=&limit=5` | Suggestions publiques pendant la creation de ticket |

Creation d'article:

```json
{
  "title": "Comment modifier mes informations de facturation ?",
  "slug": "modifier-informations-facturation",
  "body": "<p>Rendez-vous dans...</p>",
  "category": "BILLING",
  "tags": ["facturation", "compte"],
  "publicVisible": true,
  "internalOnly": false,
  "active": true
}
```

Notes:

- `articleCode` (genere, ex. `KB-1777480000000`) et `slug` (genere depuis le titre si absent, deduplique
  automatiquement) sont retournes dans la reponse `KnowledgeArticleResponse`;
- `tags` est expose en `List<String>` cote API (stocke en interne sous forme de chaine);
- un article public (`publicVisible = true` et `active = true`) est consultable via le slug cote client,
  ce qui incremente son `viewCount`;
- la conversion message -> article reprend le contenu du message comme corps, et la categorie du ticket
  si aucune n'est fournie;
- l'endpoint `/suggestions` necessite au moins `category` ou `searchText` (sinon reponse vide), et limite
  les resultats a 10 maximum.

UI admin:

- `KnowledgeBaseListPage` (recherche, filtres categorie, badges public/interne);
- `KnowledgeArticleEditorPage` (titre, slug, corps riche, categorie, tags, visibilite);
- panneau "Suggestions" affiche pendant la creation/edition d'un ticket (appel `/suggestions` avec
  `category` + `searchText` du formulaire);
- bouton "Convertir en article" sur un message agent dans `SupportTicketDetailPage`;
- widget analytics: vues d'articles vs tickets crees sur la periode (`/analytics`).

UI client:

- `ClientKnowledgeBasePage` (recherche d'articles publics, suggestions pendant l'ouverture d'un ticket);
- `ClientArticlePage` (lecture d'un article via son slug).

### Analytics avances et exports

`GET /support/tickets/analytics?from=&to=` (`SUPPORT:METRICS`) a ete enrichi avec de nouveaux
indicateurs de pilotage, en plus des champs deja presents (`totalCreated`, `byCategory`,
`agentWorkload`, etc.) :

```json
{
  "slaBreachRate": 4.2,
  "reopenedCount": 3,
  "waitingClientCount": 7,
  "volumeByRelatedType": { "INVOICE": 12, "BOOKING": 5, "CASH_ANOMALY_FLAG": 2 },
  "backlogByAgent": { "12": 8, "15": 3 },
  "csatByCategory": { "BILLING": 4.1, "TECHNICAL": 3.6 },
  "csatByAgent": { "12": 4.4, "15": 3.9 }
}
```

Notes :

- `slaBreachRate` est le pourcentage de tickets crees sur la periode ayant subi une violation SLA ;
- `reopenedCount` compte les evenements timeline `REOPENED` sur la periode ;
- `waitingClientCount` est le nombre de tickets actuellement en statut `WAITING_CLIENT` (instantane,
  non borne par `from`/`to`) — sert de proxy pour le widget "en attente client" ;
- `backlogByAgent` est la charge actuelle (tickets `OPEN`/`IN_PROGRESS`/`WAITING_CLIENT`) par agent,
  cle = `agentId` ;
- `csatByCategory`/`csatByAgent` : note CSAT moyenne (1-5) groupee par categorie / par agent, absente
  si aucune evaluation n'a ete soumise pour la cle.

`GET /support/tickets/analytics.csv?from=&to=` (`SUPPORT:METRICS`) telecharge le meme rapport au
format CSV (sections par indicateur puis par repartition), pret pour un export Excel.

`GET /support/tickets/agents/performance?from=&to=` (`SUPPORT:METRICS`) retourne le detail par agent :

```json
[
  {
    "agentId": 12,
    "assignedCount": 24,
    "resolvedCount": 21,
    "avgFirstResponseHours": 2.3,
    "avgResolutionHours": 14.8,
    "avgCsatScore": 4.4
  }
]
```

UI suggeree :

- `SupportAnalyticsPage` : tuiles d'indicateurs (SLA breach rate, reouvertures, en attente client),
  graphiques de repartition (categorie, priorite, type lie, tags), et bouton "Exporter en CSV"
  pointant vers `/analytics.csv` ;
- `SupportAgentPerformancePage` (ou onglet de la page analytics) : tableau triable par agent avec
  charge assignee, taux de resolution, temps moyen de premiere reponse/resolution et CSAT moyen.

## 4. Visitor Management

Endpoints:

| Methode | Endpoint | Permission | Usage |
| --- | --- | --- | --- |
| `POST` | `/visitors/passes` | `VISITOR:WRITE` | Creer un pass visiteur |
| `GET` | `/visitors/passes?status=SCHEDULED` | `VISITOR:READ` | Lister les pass |
| `GET` | `/visitors/passes/today` | `VISITOR:READ` | Visites du jour |
| `POST` | `/visitors/{passNumber}/check-in` | `VISITOR:CHECKIN` | Enregistrer l'arrivee |
| `POST` | `/visitors/{passNumber}/check-out` | `VISITOR:CHECKIN` | Enregistrer le depart |
| `GET` | `/visitors/log` | `VISITOR:READ` | Journal |

Creation pass:

```json
{
  "fullName": "Marie Ngoma",
  "email": "marie@example.com",
  "phone": "+242000000001",
  "company": "Acme",
  "hostMemberCode": "MBR-000001",
  "hostName": "Jean Dupont",
  "validFrom": "2026-05-10T08:00:00Z",
  "validUntil": "2026-05-10T18:00:00Z",
  "purpose": "Rendez-vous commercial",
  "createdBy": "USER-001"
}
```

Reponse:

```json
{
  "passNumber": "VIS-1777480000000",
  "fullName": "Marie Ngoma",
  "hostName": "Jean Dupont",
  "validFrom": "2026-05-10T08:00:00Z",
  "validUntil": "2026-05-10T18:00:00Z",
  "status": "SCHEDULED",
  "qrValue": "bokati:visitor:VIS-1777480000000"
}
```

Check-in/check-out:

```json
{
  "agent": "USER-001",
  "notes": "Badge remis"
}
```

Statuts:

- `SCHEDULED`;
- `CHECKED_IN`;
- `CHECKED_OUT`;
- `CANCELLED`;
- `EXPIRED`.

UI:

- `VisitorTodayPage`: liste des visiteurs du jour;
- `VisitorPassFormPage`: creation pass;
- `VisitorCheckInDeskPage`: scan QR ou saisie `passNumber`;
- `VisitorLogPage`: journal entrees/sorties;
- rendre `qrValue` en QR code imprimable sur badge.

## 5. CRM

Endpoints:

| Methode | Endpoint | Permission | Usage |
| --- | --- | --- | --- |
| `POST` | `/crm/leads` | public ou backoffice | Creer un lead |
| `GET` | `/crm/leads?stage=NEW` | `CRM:READ` | Lister leads |
| `PATCH` | `/crm/leads/{id}/qualify` | `CRM:WRITE` | Qualifier |
| `PATCH` | `/crm/leads/{id}/stage` | `CRM:WRITE` | Changer etape |
| `PATCH` | `/crm/leads/{id}/convert` | `CRM:CONVERT` | Convertir |
| `POST` | `/crm/leads/{id}/activities` | `CRM:WRITE` | Ajouter activite |
| `GET` | `/crm/pipeline` | `CRM:READ` | Kanban pipeline |
| `GET` | `/crm/metrics` | `CRM:READ` | Metriques |

Creation lead:

```json
{
  "fullName": "Claire Mavoungou",
  "email": "claire@example.com",
  "phone": "+242000000002",
  "company": "Startup BZV",
  "source": "WEBSITE",
  "interest": "Bureau prive",
  "estimatedAmount": 250000,
  "probability": 30,
  "expectedCloseDate": "2026-05-30",
  "assignedTo": 7
}
```

Stages:

- `NEW`;
- `CONTACTED`;
- `QUALIFIED`;
- `PROPOSAL_SENT`;
- `WON`;
- `LOST`.

Changer stage:

```json
{
  "stage": "PROPOSAL_SENT",
  "lostReason": null
}
```

Convertir:

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-000001"
}
```

Activite:

```json
{
  "activityType": "CALL",
  "subject": "Premier appel",
  "notes": "Interesse par un bureau mensuel.",
  "performedBy": "USER-001",
  "performedAt": "2026-05-01T10:00:00Z"
}
```

Activity types:

- `CALL`;
- `EMAIL`;
- `MEETING`;
- `VISIT`;
- `NOTE`.

UI:

- `CrmPipelinePage`: kanban par `LeadStage`;
- `LeadListPage`: table filtre stage/assigned;
- `LeadDetailPage`: infos, activites, conversion;
- `LeadCaptureForm`: formulaire public site;
- `CrmMetricsCards`: total leads, won, lost, valeur pipeline.

Important:

- La conversion actuelle trace `convertedOwnerType` et `convertedOwnerCode`.
- Si le frontend doit creer automatiquement un member/customer, il doit appeler le module cible avant `/crm/leads/{id}/convert`, puis envoyer le code obtenu.

## 6. Task Management

Endpoints:

| Methode | Endpoint | Permission | Usage |
| --- | --- | --- | --- |
| `POST` | `/tasks` | `TASK:WRITE` | Creer une tache |
| `GET` | `/tasks?assignedTo=7` | `TASK:READ` | Lister |
| `GET` | `/tasks/{id}` | `TASK:READ` | Detail |
| `PATCH` | `/tasks/{id}/complete` | `TASK:WRITE` | Marquer terminee |
| `PATCH` | `/tasks/{id}/assign` | `TASK:ASSIGN` | Reassigner |
| `POST` | `/tasks/{id}/comments` | `TASK:WRITE` | Ajouter commentaire |
| `GET` | `/tasks/due-today` | `TASK:READ` | Taches du jour |

Creation:

```json
{
  "title": "Preparer la salle Reunion A",
  "description": "Verifier chaises, projecteur et eau.",
  "assignedTo": 7,
  "priority": "HIGH",
  "dueAt": "2026-05-10T08:30:00Z",
  "sourceType": "BOOKING",
  "sourceCode": "BKG-202605-000001",
  "recurrence": "AFTER_BOOKING",
  "checklist": [
    { "label": "Verifier projecteur", "completed": false, "displayOrder": 1 },
    { "label": "Installer 8 chaises", "completed": false, "displayOrder": 2 }
  ]
}
```

Statuts:

- `OPEN`;
- `IN_PROGRESS`;
- `COMPLETED`;
- `CANCELLED`.

Priorites:

- `LOW`;
- `MEDIUM`;
- `HIGH`;
- `URGENT`.

Recurrences:

- `NONE`;
- `DAILY`;
- `WEEKLY`;
- `MONTHLY`;
- `AFTER_BOOKING`.

Commentaire:

```json
{
  "authorId": "USER-001",
  "authorName": "Agent Front Desk",
  "comment": "Salle prete."
}
```

Assignation:

```json
{
  "assignedTo": 9
}
```

UI:

- `TaskBoardPage`: colonnes `OPEN`, `IN_PROGRESS`, `COMPLETED`;
- `MyTasksPage`: filtre `assignedTo`;
- `DueTodayWidget`: via `/tasks/due-today`;
- `TaskDetailDrawer`: checklist + commentaires;
- actions rapides: assigner, commenter, terminer.

Automatisation:

- le backend expose un service `createFromAutomation`;
- les workers futurs peuvent creer des taches depuis booking/inventory;
- le frontend affiche `sourceType` et `sourceCode` pour relier la tache a l'objet d'origine.

## 7. Ordre d'integration frontend recommande

1. Permissions UI: brancher l'affichage conditionnel par authorities.
2. Resource: quote pricing + calendrier public.
3. Resource: galerie et amenities enrichis.
4. Support admin + support client.
5. Visitor desk: pass, QR, check-in/check-out.
6. CRM pipeline.
7. Task management.

## 8. Pages a creer

Resource:

- `ResourcePricingRulesPage`;
- `ResourcePriceSimulator`;
- `PublicResourceCalendar`;
- `ResourceGalleryPanel`;
- `ResourceAmenitiesPanel`.

Support:

- `SupportTicketListPage`;
- `SupportTicketDetailPage`;
- `ClientSupportPage`;
- `SupportMetricsPage`.

Visitor:

- `VisitorTodayPage`;
- `VisitorPassFormPage`;
- `VisitorCheckInDeskPage`;
- `VisitorLogPage`;
- `VisitorBadgePrintView`.

CRM:

- `CrmPipelinePage`;
- `LeadListPage`;
- `LeadDetailPage`;
- `LeadCaptureForm`;
- `CrmMetricsPage`.

Task:

- `TaskBoardPage`;
- `MyTasksPage`;
- `TaskDetailDrawer`;
- `DueTodayWidget`.

## 9. Tests frontend minimaux

- Creer une regle de prix et verifier le quote.
- Charger un calendrier public sans authentification.
- Ajouter une photo ressource apres upload document.
- Creer un ticket client puis repondre cote admin.
- Creer un pass visiteur, scanner/afficher `qrValue`, check-in puis check-out.
- Creer un lead, ajouter une activite, le convertir.
- Creer une tache, l'assigner, commenter, terminer.
