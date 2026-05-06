# Module Support — Documentation complète

> Dernière mise à jour : 2026-05-05  
> Statut : ✅ Complet et production-ready

---

## 1. Vue d'ensemble

Le module Support fournit un système de ticketing complet pour la gestion des demandes clients, avec SLA automatiques, notifications email, scoring de satisfaction (CSAT), réponses rapides et analytics avancés.

---

## 2. Structure des fichiers

```
features/support/
├── enums/
│   ├── TicketStatus.java          → OPEN, IN_PROGRESS, WAITING_CLIENT, RESOLVED, CLOSED
│   ├── TicketPriority.java        → LOW, MEDIUM, HIGH, URGENT
│   ├── TicketCategory.java        → BILLING, BOOKING, ACCESS, TECHNICAL, OTHER
│   └── TicketSenderType.java      → CLIENT, AGENT, SYSTEM
├── model/
│   ├── SupportTicket.java         → entité principale + champs CSAT
│   ├── TicketMessage.java         → fil de conversation
│   ├── TicketAttachment.java      → pièces jointes (via DocumentService)
│   └── QuickReply.java            → templates de réponse rapide
├── repository/
│   ├── SupportTicketRepository.java   → requêtes JPQL + native pour SLA/analytics
│   ├── TicketMessageRepository.java
│   ├── TicketAttachmentRepository.java
│   └── QuickReplyRepository.java
├── dto/
│   └── SupportDtos.java           → tous les records (requêtes + réponses)
├── mapper/
│   └── SupportTicketMapper.java
├── service/
│   ├── interfaces/
│   │   ├── SupportTicketService.java
│   │   └── SupportEmailService.java
│   └── implementation/
│       ├── SupportTicketServiceImpl.java
│       ├── SupportEmailServiceImpl.java
│       └── SupportQuickReplyServiceImpl.java
├── worker/
│   ├── SupportSlaEscalationWorker.java    → toutes les heures
│   └── SupportCsatRequestWorker.java      → toutes les heures
└── controller/
    ├── SupportTicketController.java       → admin
    ├── ClientSupportController.java       → portail client
    └── SupportQuickReplyController.java   → gestion réponses rapides
```

---

## 3. Modèle de données

### SupportTicket

| Champ | Type | Description |
|---|---|---|
| `ticketNumber` | VARCHAR(80) UNIQUE | Format `TCK-{epoch_millis}` |
| `title` | TEXT NOT NULL | Titre de la demande |
| `description` | TEXT | Description initiale |
| `status` | ENUM | OPEN → IN_PROGRESS → WAITING_CLIENT / RESOLVED → CLOSED |
| `priority` | ENUM | LOW / MEDIUM / HIGH / URGENT |
| `category` | ENUM | BILLING / BOOKING / ACCESS / TECHNICAL / OTHER |
| `ownerType` | VARCHAR | MEMBER / CUSTOMER / BUSINESS_ENTITY |
| `ownerCode` | VARCHAR | Code du propriétaire |
| `contactEmail` | VARCHAR | Email de contact pour les notifications |
| `assignedTo` | BIGINT | ID de l'agent assigné (FK users) |
| `firstResponseDueAt` | TIMESTAMPTZ | SLA : délai max première réponse |
| `resolutionDueAt` | TIMESTAMPTZ | SLA : délai max de résolution |
| `firstRespondedAt` | TIMESTAMPTZ | Horodatage première réponse réelle |
| `resolvedAt` | TIMESTAMPTZ | Horodatage résolution |
| `closedAt` | TIMESTAMPTZ | Horodatage clôture |
| `csatScore` | INTEGER (1-5) | Note de satisfaction client |
| `csatComment` | TEXT | Commentaire CSAT |
| `csatSubmittedAt` | TIMESTAMPTZ | Date de soumission CSAT |
| `csatEmailSentAt` | TIMESTAMPTZ | Date d'envoi de l'email CSAT (worker) |

### SLA par priorité

| Priorité | Délai première réponse | Délai résolution |
|---|---|---|
| URGENT | 2h | 8h |
| HIGH | 8h | 24h |
| MEDIUM | 24h | 72h |
| LOW | 48h | 120h |

### QuickReply

| Champ | Type | Description |
|---|---|---|
| `category` | ENUM | Catégorie associée |
| `title` | VARCHAR(200) | Titre court du template |
| `body` | TEXT | Corps de la réponse |
| `active` | BOOLEAN | Soft-delete |

---

## 4. Endpoints Admin

**Base URL :** `/sni/api/v1/support/tickets`

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| POST | `/` | SUPPORT:WRITE | Créer un ticket |
| GET | `/` | SUPPORT:READ | Recherche paginée (status, assignedTo, ownerType, ownerCode, searchText) |
| GET | `/{ticketNumber}` | SUPPORT:READ | Détail d'un ticket |
| PATCH | `/{ticketNumber}/assign` | SUPPORT:ASSIGN | Assigner à un agent |
| PATCH | `/{ticketNumber}/status` | SUPPORT:WRITE | Changer le statut |
| POST | `/{ticketNumber}/messages` | SUPPORT:WRITE | Ajouter un message (interne ou public) |
| GET | `/metrics` | SUPPORT:METRICS | Métriques temps réel |
| GET | `/analytics` | SUPPORT:METRICS | Analytics par période (`?from=&to=`) |

**Quick Replies — Base URL :** `/sni/api/v1/support/quick-replies`

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| GET | `/` | SUPPORT:READ | Lister (`?category=BILLING`) |
| POST | `/` | SUPPORT:WRITE | Créer un template |
| DELETE | `/{id}` | SUPPORT:WRITE | Désactiver (soft-delete) |

---

## 5. Endpoints Portail Client

**Base URL :** `/sni/api/v1/client/support/tickets`

| Méthode | Chemin | Description |
|---|---|---|
| POST | `/` | Ouvrir un ticket |
| GET | `/` | Mes tickets (`?ownerType=&ownerCode=&searchText=`) |
| GET | `/{ticketNumber}` | Détail de mon ticket |
| POST | `/{ticketNumber}/messages` | Répondre (force senderType=CLIENT, internal=false) |
| PATCH | `/{ticketNumber}/close` | Clore mon ticket |
| POST | `/{ticketNumber}/csat` | Soumettre une note CSAT (1-5) |

---

## 6. Notifications email automatiques

Toutes les notifications sont envoyées de manière asynchrone via `SupportEmailService`.  
Le template Thymeleaf est `email/support-event.html`.

| Événement | Destinataire | Déclencheur |
|---|---|---|
| Ticket créé | Client (`contactEmail`) | `create()` |
| Message de l'agent | Client | `addMessage()` si senderType=AGENT et non interne |
| Message du client | Agent assigné (ou manager si non assigné) | `addMessage()` si senderType=CLIENT et non interne |
| Ticket résolu | Client | `updateStatus()` quand passage à RESOLVED |
| Alerte SLA dépassé | Manager (`bokati.support.manager-email`) | `SupportSlaEscalationWorker` toutes les heures |
| Demande CSAT | Client | `SupportCsatRequestWorker` 24h après clôture |

**Configuration :**
```yaml
bokati:
  support:
    manager-email: support@bokaticowork.com   # destinataire alertes SLA
    sla-check-delay-ms: 3600000               # vérification SLA toutes les heures
    csat-check-delay-ms: 3600000              # vérification CSAT toutes les heures
```

---

## 7. Workers automatiques

### SupportSlaEscalationWorker
- **Fréquence :** toutes les heures (configurable)
- **Logique :**
  1. Cherche les tickets OPEN/IN_PROGRESS avec SLA dépassé
  2. Envoie alerte email au manager pour chaque dépassement
  3. Si SLA résolution dépassé → élève la priorité d'un niveau (LOW→MEDIUM→HIGH→URGENT)

### SupportCsatRequestWorker
- **Fréquence :** toutes les heures (configurable)
- **Logique :**
  1. Cherche les tickets CLOSED depuis plus de 24h sans email CSAT envoyé
  2. Envoie l'email de demande CSAT au client
  3. Marque `csatEmailSentAt = now` pour ne pas renvoyer

---

## 8. Analytics

**GET** `/sni/api/v1/support/tickets/analytics?from=2026-01-01T00:00:00Z&to=2026-04-30T23:59:59Z`

```json
{
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-04-30T23:59:59Z",
  "totalCreated": 142,
  "totalResolved": 128,
  "openAtEndOfPeriod": 14,
  "slaBreachCount": 7,
  "avgFirstResponseHours": 4.2,
  "avgResolutionHours": 18.6,
  "byCategory": {
    "BILLING": 54,
    "BOOKING": 38,
    "ACCESS": 21,
    "TECHNICAL": 18,
    "OTHER": 11
  },
  "byPriority": {
    "LOW": 43,
    "MEDIUM": 67,
    "HIGH": 25,
    "URGENT": 7
  },
  "avgCsatScore": 4.3,
  "csatResponseCount": 89,
  "agentWorkload": [
    { "agentId": 12, "assigned": 67, "resolved": 61 },
    { "agentId": 8,  "assigned": 45, "resolved": 42 }
  ]
}
```

---

## 9. Migrations Flyway

| Version | Fichier | Contenu |
|---|---|---|
| V87 | `roadmap_modules_security_resource_support.sql` | Tables initiales support_ticket, ticket_message, ticket_attachment |
| V98 | `support_csat_and_quick_reply.sql` | Colonnes CSAT, table quick_reply, index SLA et CSAT |

---

## 10. Flux de vie d'un ticket

```
[Création]
  Client ou admin → POST /tickets
  → Status: OPEN
  → SLA calculés selon priorité
  → Email "Ticket créé" envoyé au client

[Assignation]
  Admin → PATCH /tickets/{n}/assign
  → Status: IN_PROGRESS (si était OPEN)
  → Email interne à l'agent assigné

[Échanges]
  Agent → POST /tickets/{n}/messages (senderType=AGENT)
    → firstRespondedAt enregistré (si première réponse)
    → Email "Réponse agent" envoyé au client (si non interne)
  Client → POST /client/tickets/{n}/messages
    → Status repasse à IN_PROGRESS
    → Email "Message client" envoyé à l'agent

[Résolution]
  Admin → PATCH /tickets/{n}/status { status: "RESOLVED" }
  → resolvedAt enregistré
  → Email "Ticket résolu" envoyé au client

[Clôture]
  Admin ou client → PATCH /{n}/status CLOSED ou /client/{n}/close
  → closedAt enregistré
  → SupportCsatRequestWorker envoie l'email CSAT 24h après

[CSAT]
  Client → POST /client/tickets/{n}/csat { score: 4, comment: "..." }
  → csatScore, csatComment, csatSubmittedAt enregistrés
```

---

## 11. Permissions requises

| Permission | Accès |
|---|---|
| `SUPPORT:READ` / `SUPPORT_READ` | Lister, voir, métriques, analytics |
| `SUPPORT:WRITE` / `SUPPORT_WRITE` | Créer, répondre, changer statut, quick replies |
| `SUPPORT:ASSIGN` / `SUPPORT_ASSIGN` | Assigner un agent |
| `SUPPORT:METRICS` / `SUPPORT_METRICS` | Métriques et analytics |
