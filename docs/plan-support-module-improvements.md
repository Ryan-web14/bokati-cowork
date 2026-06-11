# Plan d'amélioration — Module Support

> Dernière mise à jour : 2026-06-07  
> Statut : proposition fonctionnelle et technique — à implémenter par phases

---

## 1. Contexte

Le module `support` fournit déjà un socle solide de ticketing : création de tickets, portail client, assignation, messages publics/internes, SLA, emails automatiques, CSAT, quick replies, analytics, inbound email et workers d'automatisation.

L'objectif de ce plan est de faire évoluer ce module vers un centre de support plus complet : meilleur suivi opérationnel, routage automatique, pièces jointes, base de connaissance, intégrations avec Task/CRM/Billing/Booking et analytics plus exploitables.

---

## 2. État actuel du module

### Fonctionnalités déjà présentes

- Tickets admin : création, recherche paginée, détail, assignation, changement de statut, réponse agent.
- Portail client : création ticket, liste des tickets client, détail, réponse client, clôture, CSAT.
- Statuts : `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED`.
- Priorités : `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Catégories : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER`.
- SLA première réponse et résolution calculés à la création selon la priorité.
- Messages publics et internes.
- Emails automatiques via `SupportEmailService`.
- Demande CSAT publique avec token.
- Quick replies.
- Metrics temps réel et analytics par période.
- Workers :
  - SLA escalation.
  - CSAT request.
  - Inbound email.
  - Auto-close.
- Modèle `TicketAttachment` déjà présent.

### Fichiers principaux

- `features/support/controller/SupportTicketController.java`
- `features/support/controller/ClientSupportController.java`
- `features/support/controller/SupportQuickReplyController.java`
- `features/support/controller/SupportCsatPublicController.java`
- `features/support/service/implementation/SupportTicketServiceImpl.java`
- `features/support/service/implementation/SupportEmailServiceImpl.java`
- `features/support/worker/SupportSlaEscalationWorker.java`
- `features/support/worker/SupportCsatRequestWorker.java`
- `features/support/worker/SupportInboundEmailWorker.java`
- `features/support/worker/SupportAutoCloseWorker.java`
- `features/support/model/SupportTicket.java`
- `features/support/model/TicketMessage.java`
- `features/support/model/TicketAttachment.java`
- `features/support/model/QuickReply.java`

---

## 3. Manques identifiés

### 3.1 Pièces jointes incomplètes

Le modèle `TicketAttachment` existe, mais les endpoints d'upload, liste, téléchargement et suppression ne sont pas exposés. C'est un manque important pour les tickets liés à des factures, captures écran, preuves de paiement, contrats, badges d'accès ou incidents techniques.

### 3.2 Routage automatique absent

L'assignation est principalement manuelle. Le support gagnerait à router automatiquement les tickets selon :
- catégorie ;
- priorité ;
- propriétaire (`ownerType`) ;
- source liée (`relatedType`) ;
- charge des agents ;
- horaires de support.

### 3.3 Escalade SLA encore limitée

Le worker SLA existe, mais l'escalade reste relativement simple. Il manque :
- niveaux d'escalade ;
- traçabilité d'escalade ;
- raison d'escalade ;
- notification ciblée selon niveau ;
- escalade vers manager/admin si non traité.

### 3.4 Pas de tags

Les catégories sont utiles, mais trop larges. Des tags permettraient une classification plus fine :
- `wifi`
- `refund`
- `invoice`
- `access-card`
- `booking-change`
- `bug`
- `vip`

### 3.5 Audit trail / timeline métier incomplet

Les messages donnent une partie de l'historique, mais les actions métier ne sont pas toutes exposées comme événements :
- assignation ;
- changement de priorité ;
- changement de catégorie ;
- changement de statut ;
- escalade ;
- clôture automatique ;
- réouverture ;
- fusion.

### 3.6 Base de connaissance absente

Le support ne dispose pas encore d'articles FAQ ou procédures :
- articles publics pour clients ;
- articles internes pour agents ;
- suggestions d'articles lors de la création ticket ;
- conversion d'une réponse agent en article.

### 3.7 Intégration Task absente

Certains tickets nécessitent une action opérationnelle terrain. Exemple :
- `ACCESS` → vérifier badge ou accès porte.
- `TECHNICAL` → inspecter équipement ou réseau.
- `BOOKING` → vérifier salle ou réservation.

Le module Task peut porter cette exécution opérationnelle, avec `sourceType = SUPPORT_TICKET`.

### 3.8 Intégration CRM / Client 360 limitée

Le support devrait contribuer à la vision client :
- afficher les tickets liés sur fiche membre/client ;
- détecter clients à risque ;
- faire remonter mauvais CSAT ;
- voir tickets ouverts dans le CRM.

### 3.9 Détection de doublons absente

Un client peut ouvrir plusieurs tickets similaires. Il manque :
- suggestion de doublons ;
- fusion de tickets ;
- champ `mergedIntoTicketNumber` ;
- conservation historique.

### 3.10 Analytics à enrichir

Les analytics existent, mais peuvent être approfondies :
- réouvertures ;
- backlog par agent ;
- temps en statut `WAITING_CLIENT`;
- respect SLA par catégorie ;
- volume par source liée ;
- top tags ;
- export CSV/PDF.

---

## 4. Fonctionnalités proposées

### 4.1 Pièces jointes

Endpoints à ajouter :

| Méthode | Route | Permission | Description |
|---|---|---|---|
| `POST` | `/support/tickets/{ticketNumber}/attachments` | `SUPPORT:WRITE` | Ajouter une pièce jointe |
| `GET` | `/support/tickets/{ticketNumber}/attachments` | `SUPPORT:READ` | Lister les pièces jointes |
| `GET` | `/support/tickets/{ticketNumber}/attachments/{id}/download` | `SUPPORT:READ` | Télécharger |
| `DELETE` | `/support/tickets/{ticketNumber}/attachments/{id}` | `SUPPORT:WRITE` | Supprimer / désactiver |

À prévoir :
- validation taille fichier ;
- types MIME autorisés ;
- association avec message optionnelle ;
- visibilité client/interne.

### 4.2 Routage automatique

Créer une entité `SupportRoutingRule`.

Champs proposés :
- `name`
- `active`
- `category`
- `priority`
- `ownerType`
- `relatedType`
- `assignedTo`
- `teamCode`
- `sortOrder`

Comportement :
- appliqué à la création du ticket ;
- première règle active correspondante gagne ;
- si aucune règle ne correspond, ticket non assigné.

### 4.3 Escalade avancée

Ajouter à `SupportTicket` :
- `escalationLevel`
- `escalatedAt`
- `escalationReason`
- `lastSlaAlertSentAt`

Comportement :
- niveau 1 : agent assigné ;
- niveau 2 : manager support ;
- niveau 3 : admin / direction ;
- possibilité d'élever automatiquement la priorité.

### 4.4 Tags

Ajouter :
- `SupportTag`
- `SupportTicketTag`

Endpoints :
- `POST /support/tickets/{ticketNumber}/tags`
- `DELETE /support/tickets/{ticketNumber}/tags/{tag}`
- `GET /support/tags`

Utilisations :
- filtres ;
- analytics ;
- détection de sujets fréquents ;
- base de connaissance.

### 4.5 Timeline / audit métier

Créer `SupportTicketEvent`.

Événements proposés :
- `TICKET_CREATED`
- `ASSIGNED`
- `STATUS_CHANGED`
- `PRIORITY_CHANGED`
- `CATEGORY_CHANGED`
- `MESSAGE_ADDED`
- `SLA_ESCALATED`
- `CSAT_SUBMITTED`
- `REOPENED`
- `MERGED`
- `AUTO_CLOSED`

Endpoint :
- `GET /support/tickets/{ticketNumber}/timeline`

### 4.6 Knowledge Base

Créer `KnowledgeArticle`.

Champs :
- `articleCode`
- `title`
- `slug`
- `body`
- `category`
- `tags`
- `publicVisible`
- `internalOnly`
- `active`
- `createdBy`
- `updatedBy`

Endpoints :
- CRUD admin articles.
- Recherche publique articles actifs.
- Suggestions par catégorie/tags/searchText.
- Conversion message → article.

### 4.7 Support → Task

Ajouter action :
- `POST /support/tickets/{ticketNumber}/tasks`

Payload :
- `title`
- `description`
- `assignedTo`
- `priority`
- `dueAt`
- `checklist`

Création Task :
- `sourceType = SUPPORT_TICKET`
- `sourceCode = ticketNumber`
- `relatedType/relatedCode` conservés côté support.

Cas d'usage :
- accès badge ;
- intervention technique ;
- vérification facturation ;
- préparation d'un geste commercial.

### 4.8 Client 360 / CRM

Améliorations :
- endpoint tickets par owner :
  - `GET /support/tickets/by-owner?ownerType=&ownerCode=`
- widgets CRM :
  - tickets ouverts ;
  - derniers tickets ;
  - CSAT moyen ;
  - SLA breaches ;
  - client à risque.

### 4.9 Fusion de tickets

Ajouter :
- `mergedIntoTicketNumber`
- `mergedAt`
- `mergedBy`

Endpoint :
- `POST /support/tickets/{ticketNumber}/merge`

Comportement :
- ticket source passe en `CLOSED` ou statut `MERGED` si nouvel enum accepté ;
- messages conservés ;
- événement timeline créé ;
- ticket cible reçoit une note interne.

### 4.10 Analytics avancés

Nouveaux indicateurs :
- tickets réouverts ;
- SLA breach rate ;
- moyenne temps en attente client ;
- backlog par agent ;
- top catégories/tags ;
- CSAT par agent/catégorie ;
- volume par `relatedType`;
- export CSV/PDF.

---

## 5. Plan d'implémentation par phases

## Phase 1 — Complétude API et pièces jointes

**Objectif :** combler le manque fonctionnel le plus visible.

Actions :
- exposer les endpoints pièces jointes ;
- intégrer `attachments` dans `SupportTicketResponse` ;
- ajouter `AttachmentResponse`;
- vérifier stockage via service existant si disponible ;
- ajouter validation taille/type fichier ;
- permettre pièce jointe interne ou visible client.

Fichiers :
- `SupportTicketController`
- `ClientSupportController`
- `SupportDtos`
- `TicketAttachment`
- `TicketAttachmentRepository`
- nouveau `SupportAttachmentService`

Priorité : haute.

---

## Phase 2 — Filtres enrichis et tags

**Objectif :** améliorer recherche, triage et reporting.

Actions :
- ajouter filtres `category`, `priority`, `relatedType`, `relatedCode`, `overdueOnly`;
- créer tags support ;
- endpoints add/remove/list tags ;
- exposer tags dans `SupportTicketResponse`;
- ajouter analytics par tag.

Fichiers :
- `SupportTicketRepository`
- `SupportTicketServiceImpl`
- `SupportDtos`
- nouveaux modèles `SupportTag`, `SupportTicketTag`
- migrations Flyway.

Priorité : haute.

---

## Phase 3 — Routage automatique

**Objectif :** réduire le tri manuel.

Actions :
- créer `SupportRoutingRule`;
- endpoints CRUD admin ;
- appliquer règle à la création ticket ;
- assigner automatiquement si règle trouvée ;
- envoyer notification agent ;
- conserver ticket non assigné si aucune règle.

Fichiers :
- nouveau package `support/routing`
- `SupportTicketServiceImpl.create`
- migration Flyway.

Priorité : haute.

---

## Phase 4 — Escalade avancée et SLA renforcé

**Objectif :** mieux gérer les retards et tickets critiques.

Actions :
- ajouter champs escalade au ticket ;
- enrichir `SupportSlaEscalationWorker`;
- notifier agent/manager/admin selon niveau ;
- éviter spam via `lastSlaAlertSentAt`;
- historiser événements d'escalade.

Fichiers :
- `SupportTicket`
- `SupportSlaEscalationWorker`
- `SupportEmailService`
- migration Flyway.

Priorité : moyenne à haute.

---

## Phase 5 — Timeline / audit métier

**Objectif :** rendre chaque action traçable.

Actions :
- créer `SupportTicketEvent`;
- écrire événement à chaque mutation ;
- endpoint timeline ;
- ajouter événements système pour workers ;
- éventuellement annoter les mutations avec `@Audited`.

Fichiers :
- `SupportTicketServiceImpl`
- workers support ;
- nouveau repository/service timeline ;
- migration Flyway.

Priorité : moyenne.

---

## Phase 6 — Intégration Support → Task

**Objectif :** transformer certains tickets en actions opérationnelles.

Actions :
- ajouter endpoint de création task depuis ticket ;
- créer task avec `sourceType = SUPPORT_TICKET`;
- ajouter checklist optionnelle ;
- ajouter lien dans la réponse ticket ou via recherche Task ;
- documenter le parcours frontend.

Fichiers :
- `SupportTicketController`
- `SupportTicketServiceImpl`
- `TaskManagementService`
- `SupportDtos`

Priorité : moyenne.

---

## Phase 7 — Knowledge Base

**Objectif :** réduire le volume de tickets répétitifs.

Actions :
- créer modèle `KnowledgeArticle`;
- CRUD admin ;
- recherche publique/client ;
- suggestions pendant création ticket ;
- conversion message agent en article ;
- analytics articles consultés vs tickets évités.

Fichiers :
- nouveau package `features/support/knowledge`
- controllers admin/public ;
- migrations Flyway.

Priorité : moyenne.

---

## Phase 8 — Client 360 et intégrations CRM/Billing/Booking

**Objectif :** connecter le support aux autres modules métier.

Actions :
- endpoint tickets par owner ;
- widgets CRM/client ;
- création automatique de ticket depuis anomalies facturation/réservation ;
- affichage support dans fiche client/membre.

Fichiers :
- `SupportTicketRepository`
- `SupportTicketController`
- modules CRM/Billing/Booking selon triggers retenus.

Priorité : moyenne.

---

## Phase 9 — Fusion et doublons

**Objectif :** éviter les tickets redondants.

Actions :
- ajouter champs de fusion ;
- endpoint merge ;
- note interne automatique ;
- suggestion de doublons par owner/category/searchText ;
- analytics doublons.

Fichiers :
- `SupportTicket`
- `SupportTicketServiceImpl`
- `SupportTicketRepository`
- migration Flyway.

Priorité : basse à moyenne.

---

## Phase 10 — Analytics avancés et exports

**Objectif :** donner une vraie vision pilotage support.

Actions :
- enrichir `/support/tickets/analytics`;
- ajouter endpoint agent performance ;
- ajouter exports CSV/PDF ;
- ajouter métriques CSAT par agent/catégorie ;
- ajouter SLA breach rate.

Fichiers :
- `SupportTicketRepository`
- `SupportTicketServiceImpl`
- `SupportDtos`
- éventuel service export.

Priorité : basse à moyenne.

---

## 6. Ordre recommandé

| Ordre | Phase | Pourquoi |
|---|---|---|
| 1 | Pièces jointes | Manque fonctionnel concret, modèle déjà présent |
| 2 | Filtres + tags | Améliore immédiatement triage et analytics |
| 3 | Routage automatique | Réduit la charge manuelle |
| 4 | Escalade avancée | Rend SLA plus actionnable |
| 5 | Timeline / audit | Traçabilité nécessaire avant automatisations lourdes |
| 6 | Support → Task | Connecte ticketing et opérations terrain |
| 7 | Knowledge Base | Réduit tickets répétitifs |
| 8 | Client 360 | Améliore vision client |
| 9 | Fusion doublons | Utile quand volume augmente |
| 10 | Analytics avancés | Pilotage long-terme |

---

## 7. Décisions à valider

1. **Stockage pièces jointes** : service document existant, stockage local, cloud, ou base documentaire interne ?
2. **Visibilité fichiers** : tout fichier client-visible par défaut ou visibilité explicite ?
3. **Routage** : agent fixe, équipe, round-robin ou charge minimale ?
4. **Escalade** : combien de niveaux et quels destinataires ?
5. **Tags** : tags libres ou dictionnaire administré ?
6. **Knowledge base** : articles publics, internes, ou les deux ?
7. **Fusion** : ajouter statut `MERGED` ou conserver `CLOSED` avec `mergedIntoTicketNumber` ?
8. **Support → Task** : création manuelle seulement ou automatique selon catégorie/priorité ?

---

## 8. Prochaine étape recommandée

Commencer par la **Phase 1 — Pièces jointes** :
- le modèle existe déjà ;
- l'impact est limité au module support ;
- le frontend gagne immédiatement une fonctionnalité attendue ;
- les tickets deviennent plus exploitables pour billing, booking, accès et technique.

