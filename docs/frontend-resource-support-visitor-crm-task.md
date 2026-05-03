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
| `GET` | `/support/tickets/metrics` | `SUPPORT:METRICS` | Metriques SLA |

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

UI admin:

- `SupportTicketListPage` avec filtres statut, priorite, agent, owner;
- `SupportTicketDetailPage` avec timeline messages;
- panneau SLA: premiere reponse, resolution;
- bouton assigner si `SUPPORT:ASSIGN`;
- badges couleur par priorite.

UI client:

- `ClientSupportPage`;
- ouvrir ticket;
- voir messages;
- ajouter reponse;
- fermer ticket.

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
