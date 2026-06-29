# CRM — Documentation consolidée Frontend

> Dernière mise à jour : 2026-06-07
> Statut : ✅ Backend complet et production-ready — ce document consolide `crm-module-complete.md` + `crm-kanban-board.md` et ajoute les propositions d'écrans / parcours d'utilisation pour l'équipe frontend.

---

## 1. Vue d'ensemble

Le module CRM gère le cycle de vie complet d'un **prospect (lead)** : capture (formulaire public ou saisie backoffice), qualification, suivi via un pipeline commercial visuel (Kanban), gestion des **opportunités**, scoring automatique, génération de devis, notifications email automatiques et alerte sur les leads dormants.

**Base URL admin :** `/sni/api/v1/crm` (authentifié, JWT)
**Base URL publique :** `/sni/api/v1/public/crm` (sans authentification — formulaire site web)

### Permissions RBAC

| Permission | Donne accès à |
|---|---|
| `CRM:READ` / `CRM_READ` | Lecture : leads, board, pipeline, métriques, analytics, opportunités |
| `CRM:WRITE` / `CRM_WRITE` | Écriture : créer/modifier leads, activités, opportunités, changer de stage, générer un devis |
| `CRM:CONVERT` / `CRM_CONVERT` | Conversion d'un lead en client/membre |

> Le frontend doit masquer les actions (boutons "Convertir", drag-and-drop Kanban, etc.) selon les permissions de l'utilisateur connecté, en plus du contrôle serveur (`@PreAuthorize`).

---

## 2. Modèle de données

### 2.1 Lead

| Champ | Type | Notes |
|---|---|---|
| `id` | Long | |
| `leadNumber` | String | Généré, format `LDN-{epoch_millis}`, unique |
| `fullName` | String | Obligatoire |
| `email` / `phone` / `company` | String | Optionnels |
| `note` | String (rich text normalisé) | |
| `source` | `LeadSource` | Canal d'acquisition |
| `interest` | `LeadInterest` | Produit/service recherché |
| `stage` | `LeadStage` | Étape du pipeline |
| `estimatedAmount` | BigDecimal | Valeur estimée du deal |
| `probability` | Integer | Probabilité **manuelle** (%), saisie par l'agent |
| `scoredProbability` | Integer | Probabilité **calculée automatiquement** (voir §4) — présent uniquement dans `LeadResponse`, pas en base |
| `expectedCloseDate` | LocalDate | Date de clôture prévue |
| `assignedTo` | Long | ID de l'agent (utilisateur backoffice) |
| `convertedOwnerType` / `convertedOwnerCode` | String | Renseignés à la conversion (`MEMBER`/`CUSTOMER` + code) |
| `lostReason` | String | Renseigné quand `stage = LOST` |
| `lastActivityAt` | Instant | Mis à jour à chaque activité ajoutée |
| `createdAt` / `updatedAt` | Instant | |
| `activities` | `LeadActivityResponse[]` | Historique trié par date décroissante |

### 2.2 LeadActivity

| Champ | Type | Notes |
|---|---|---|
| `id` | Long | |
| `activityType` | `LeadActivityType` | |
| `subject` | String | Optionnel |
| `notes` | String (rich text) | Optionnel |
| `performedBy` | String | Identifiant/nom de l'auteur |
| `performedAt` | Instant | Si non fourni, peut être `null` (différent de `createdAt`) |

### 2.3 Opportunity

| Champ | Type | Notes |
|---|---|---|
| `id` | Long | |
| `opportunityNumber` | String | Généré, format `OPP-{epoch_millis}` |
| `leadId` / `leadNumber` / `leadFullName` | — | Lead parent (toute opportunité est rattachée à un lead) |
| `title` | String | Obligatoire |
| `estimatedAmount` | BigDecimal | |
| `probability` | Integer | Manuelle |
| `stage` | `OpportunityStage` | Pipeline propre, distinct de `LeadStage` |
| `expectedCloseDate` | LocalDate | |
| `assignedTo` | Long | |
| `notes` | String | |
| `wonAt` / `lostAt` | Instant | Renseignés automatiquement au changement de stage |
| `lostReason` | String | Renseigné quand `stage = LOST` |

### 2.4 Énumérations

```
LeadStage          : NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, WON, LOST
LeadSource         : WEBSITE, REFERRAL, COLD_CALL, TRADE_SHOW, PARTNER, SOCIAL_MEDIA, EVENT, OTHER
LeadInterest       : WORKSPACE, MEETING_ROOM, VIRTUAL_OFFICE, DOMICILIATION, DAY_PASS, DEDICATED_DESK, PRIVATE_OFFICE, OTHER
LeadActivityType   : CALL, EMAIL, MEETING, VISIT, NOTE
OpportunityStage   : OPEN, NEGOTIATION, PROPOSAL_SENT, WON, LOST
```

> ⚠️ `LeadStage` et `OpportunityStage` sont **deux pipelines distincts** — ne pas les confondre dans l'UI. Une opportunité peut très bien être en `NEGOTIATION` alors que son lead est encore en `QUALIFIED`.

---

## 3. Pipeline commercial & scoring

### 3.1 Pipeline des leads

```
NEW → CONTACTED → QUALIFIED → PROPOSAL_SENT → WON
                                            ↘ LOST
```

Chaque transition de `stage` déclenche un email automatique à l'agent assigné (voir §7).

### 3.2 Score automatique (`scoredProbability` / `score`)

Calculé à la volée à chaque lecture (jamais persisté). Présent dans `LeadResponse.scoredProbability` et `KanbanCardResponse.score`.

| Composant | Valeur |
|---|---|
| Stage `NEW` | 5 |
| Stage `CONTACTED` | 20 |
| Stage `QUALIFIED` | 40 |
| Stage `PROPOSAL_SENT` | 65 |
| Stage `WON` | 100 (fixe) |
| Stage `LOST` | 0 (fixe) |
| Bonus activités | +5 par activité, plafonné à +20 |
| Activité récente (≤ 7 jours) | +5 |
| Inactivité (> 7 jours) | -5 |
| Plafond final | min 1, max 95 (sauf WON/LOST) |

**Usage frontend recommandé :** afficher `scoredProbability`/`score` comme une jauge ou un badge coloré (rouge < 30, orange 30-60, vert > 60) à côté de la `probability` manuelle saisie par l'agent — bien différencier les deux valeurs dans l'UI (ex. libellés "Score auto" vs "Probabilité agent").

---

## 4. Endpoints — Leads (admin)

Toutes les routes ci-dessous sont préfixées par `/sni/api/v1/crm`.

| Méthode | Route | Permission | Description |
|---|---|---|---|
| `POST` | `/leads` | `CRM:WRITE` | Créer un lead |
| `GET` | `/leads` | `CRM:READ` | Recherche paginée (`stage`, `assignedTo`, `source`, `searchText`, `page`, `size`, `sort`) |
| `GET` | `/leads/{id}` | `CRM:READ` | Détail par ID (avec activités) |
| `GET` | `/leads/number/{leadNumber}` | `CRM:READ` | Détail par numéro |
| `PUT` | `/leads/{id}` | `CRM:WRITE` | Mise à jour (champs optionnels — patch partiel applicatif) |
| `PATCH` | `/leads/{id}/qualify` | `CRM:WRITE` | Raccourci : passe le lead en `QUALIFIED` |
| `PATCH` | `/leads/{id}/stage` | `CRM:WRITE` | Changer le stage (drag-and-drop Kanban inclus) |
| `PATCH` | `/leads/{id}/convert` | `CRM:CONVERT` | Convertir en client/membre (`stage → WON`) |
| `POST` | `/leads/{id}/activities` | `CRM:WRITE` | Ajouter une activité (appel, email, visite, note…) |
| `POST` | `/leads/{id}/generate-quote` | `CRM:WRITE` | Générer un devis (`QUOTE`) à partir du lead |
| `POST` | `/leads/{id}/opportunities` | `CRM:WRITE` | Créer une opportunité rattachée au lead |

### 4.1 Créer un lead — `POST /leads`

```json
{
  "fullName": "Claire Mavoungou",
  "email": "claire@example.com",
  "phone": "+242000000002",
  "company": "Startup BZV",
  "note": "Cherche un bureau privé pour 3 personnes",
  "source": "WEBSITE",
  "interest": "PRIVATE_OFFICE",
  "estimatedAmount": 250000,
  "probability": 30,
  "expectedCloseDate": "2026-07-30",
  "assignedTo": 7
}
```
→ `201 Created`, retourne un `LeadResponse` complet. Si `assignedTo` est renseigné, un email "Lead assigné" part automatiquement.

### 4.2 Rechercher / lister — `GET /leads`

Paramètres (tous optionnels) : `stage`, `assignedTo`, `source`, `searchText` (recherche sur nom/email/société/numéro), plus pagination Spring (`page`, `size`, `sort`).

Réponse = `PaginatedResponse<LeadResponse>` :
```json
{
  "data": [ { "id": 1, "leadNumber": "LDN-...", "...": "..." } ],
  "pageable": {
    "page": 0, "size": 20, "totalPages": 3, "totalElements": 57,
    "first": true, "last": false, "hasNext": true, "hasPrevious": false,
    "sort": { "...": "..." }
  }
}
```

### 4.3 Changer le stage — `PATCH /leads/{id}/stage`

```json
{ "stage": "PROPOSAL_SENT", "lostReason": null }
```
- Si `stage = "LOST"`, **`lostReason` doit être renseigné** côté UI (champ requis pour la traçabilité, même si le serveur ne le rend pas strictement obligatoire — c'est une bonne pratique métier).
- Déclenche l'email "Avancement" à l'agent assigné si le stage change réellement.
- C'est **le même endpoint** qui sert au déplacement de carte sur le Kanban (drag-and-drop).

### 4.4 Convertir un lead — `PATCH /leads/{id}/convert`

```json
{ "ownerType": "CUSTOMER", "ownerCode": "CUS-000001" }
```
- Force `stage → WON`, enregistre `convertedOwnerType` / `convertedOwnerCode`, envoie l'email de changement de stage.
- **Important :** le backend ne crée PAS le client/membre. Le frontend doit d'abord créer (ou sélectionner) le client/membre dans le module cible (`customer`/`member`), récupérer son `code`, puis appeler `/convert` avec ce code. Prévoir un flux en deux temps dans l'écran de conversion (cf. §10.3).

### 4.5 Ajouter une activité — `POST /leads/{id}/activities`

```json
{
  "activityType": "CALL",
  "subject": "Premier appel",
  "notes": "Intéressé par un bureau mensuel, rappel prévu vendredi.",
  "performedBy": "USER-001",
  "performedAt": "2026-06-05T10:00:00Z"
}
```
→ `201 Created`, retourne le `LeadResponse` mis à jour (avec la nouvelle activité en tête de liste). Réinitialise `lastActivityAt` à *maintenant* et **annule toute alerte de dormance en attente** (`dormantAlertSentAt = null`).

### 4.6 Générer un devis — `POST /leads/{id}/generate-quote`

```json
{ "currency": "XAF", "performedBy": "agent@bokati.com" }
```
→ `201 Created`, `{ "quoteNumber": "QTE-2026-000042" }`

- Crée un document `QUOTE` via le module billing (`createManualQuote`), pré-rempli avec les infos du lead (nom, email, téléphone, intitulé = `interest`, montant = `estimatedAmount`).
- Source tracée `sourceType = CRM_LEAD`, `sourceCode = {leadNumber}` — permet de retrouver le devis depuis le lead et inversement.
- Ajoute automatiquement une activité `NOTE` ("Devis généré") à l'historique du lead.
- **Refusé** (`400`) si le lead est `LOST` ou si `currency` est manquant.

---

## 5. Endpoints — Opportunités

| Méthode | Route | Permission | Description |
|---|---|---|---|
| `POST` | `/leads/{id}/opportunities` | `CRM:WRITE` | Créer une opportunité rattachée à un lead |
| `GET` | `/opportunities` | `CRM:READ` | Lister, paginé, filtrable par `?stage=` |
| `PATCH` | `/opportunities/{id}/stage` | `CRM:WRITE` | Changer le stage (`OPEN/NEGOTIATION/PROPOSAL_SENT/WON/LOST`) |

```json
// POST /leads/{id}/opportunities
{
  "title": "Abonnement bureau privé 6 mois",
  "estimatedAmount": 1500000,
  "probability": 50,
  "expectedCloseDate": "2026-08-01",
  "assignedTo": 7,
  "notes": "Décision attendue après visite des locaux"
}

// PATCH /opportunities/{id}/stage
{ "stage": "WON", "lostReason": null }
```
- Au passage en `WON`/`LOST`, le serveur renseigne automatiquement `wonAt`/`lostAt`.

> Une opportunité ne peut pas exister sans lead parent : le frontend doit toujours naviguer vers la création d'opportunité **depuis** la fiche du lead (pas d'écran de création autonome).

---

## 6. Vue Kanban (Board)

### `GET /board` — `CRM:READ`

Paramètres optionnels : `assignedTo`, `source`, `interest`, `searchText`.

Retourne un `KanbanBoardResponse` avec **toujours les 6 colonnes** (même vides), triées selon l'ordre du pipeline :

| # | Colonne | Stage |
|---|---|---|
| 1 | Nouveaux | `NEW` |
| 2 | Contactés | `CONTACTED` |
| 3 | Qualifiés | `QUALIFIED` |
| 4 | Devis envoyé | `PROPOSAL_SENT` |
| 5 | Gagnés | `WON` |
| 6 | Perdus | `LOST` |

```json
{
  "columns": [
    {
      "stage": "QUALIFIED",
      "count": 5,
      "totalAmount": 22000.00,
      "cards": [
        {
          "id": 42, "leadNumber": "LDN-1746394823456",
          "fullName": "Jean Dupont", "company": "Acme SARL", "email": "jean@acme.com",
          "stage": "QUALIFIED", "source": "REFERRAL", "interest": "PRIVATE_OFFICE",
          "estimatedAmount": 4500.00, "probability": 60, "score": 55,
          "assignedTo": 7, "lastActivityAt": "2026-06-05T10:30:00Z",
          "activityCount": 3, "createdAt": "2026-05-10T08:00:00Z"
        }
      ]
    }
  ],
  "totalLeads": 24,
  "totalPipelineValue": 87500.00
}
```

- Cartes triées par `lastActivityAt DESC` (fallback `createdAt`).
- `totalPipelineValue` **exclut les leads `LOST`**.
- `KanbanCardResponse` est une version **allégée** du lead (pas de `note`, pas de liste d'activités — juste `activityCount`) : pensé pour l'affichage de masse sans N+1.
- Le déplacement d'une carte se fait via `PATCH /leads/{id}/stage` (§4.3) — ne pas créer de route dédiée.

---

## 7. Endpoint public — Capture de lead

**`POST /sni/api/v1/public/crm/leads`** — sans authentification, pensé pour le formulaire de contact du site vitrine.

Requête : identique à `CreateLeadRequest` (§4.1, seul `fullName` est obligatoire).

Réponse `201` :
```json
{
  "leadNumber": "LDN-1746394823456",
  "message": "Votre demande a bien été reçue. Notre équipe vous contactera sous 24h."
}
```

> Le frontend public doit afficher ce `message` tel quel (déjà localisé côté serveur) et peut conserver `leadNumber` pour un éventuel suivi ("référence de votre demande : ...").

---

## 8. Pipeline, métriques et analytics

| Méthode | Route | Permission | Description |
|---|---|---|---|
| `GET` | `/pipeline` | `CRM:READ` | Vue groupée par stage (leads complets, non paginé) |
| `GET` | `/metrics` | `CRM:READ` | KPIs instantanés (snapshot global) |
| `GET` | `/analytics` | `CRM:READ` | Statistiques sur une période (`?from=&to=`, ISO-8601) |

### 8.1 `/pipeline`
```json
{ "stages": { "NEW": [ {"...": "lead complet"} ], "CONTACTED": [...], "...": [] } }
```
> Charge l'historique d'activités complet pour **chaque** lead — privilégier `/board` pour un affichage Kanban en masse ; réserver `/pipeline` à des vues nécessitant le détail complet par colonne (export, vue imprimable…).

### 8.2 `/metrics`
```json
{
  "totalLeads": 89, "newLeads": 12, "wonLeads": 23, "lostLeads": 15,
  "activeLeads": 51, "pipelineValue": 45750000.0000, "conversionRate": 25.8
}
```
`conversionRate` = `won / total * 100`, arrondi à 1 décimale.

### 8.3 `/analytics?from=2026-01-01T00:00:00Z&to=2026-04-30T23:59:59Z`

Si `from`/`to` omis : fenêtre par défaut = 30 derniers jours.

```json
{
  "from": "2026-01-01T00:00:00Z", "to": "2026-04-30T23:59:59Z",
  "totalCreated": 89, "totalWon": 23, "totalLost": 15,
  "conversionRate": 25.8, "avgDaysToWin": 18.4,
  "bySource": { "WEBSITE": 42, "REFERRAL": 28, "COLD_CALL": 12, "OTHER": 7 },
  "byStage": { "NEW": 12, "CONTACTED": 18, "QUALIFIED": 9, "PROPOSAL_SENT": 12, "WON": 23, "LOST": 15 },
  "pipelineValue": 45750000.0000
}
```
> `pipelineValue` (dans `/metrics` et `/analytics`) reflète le **pipeline actif courant**, pas la valeur sur la période demandée — bien le présenter comme un indicateur "instantané" à côté des stats "période".

---

## 9. Notifications email automatiques

Template Thymeleaf unique : `email/crm-event.html` (variable `eventType` pilote l'affichage).

| Événement | `eventType` | Destinataire | Déclencheur |
|---|---|---|---|
| Lead assigné | `LEAD_ASSIGNED` | Agent assigné | `create()` (si `assignedTo` renseigné) ou `update()` (si l'agent change) |
| Stage changé | `STAGE_CHANGED` | Agent assigné | `updateStage()`, `qualify()`, `convert()` — uniquement si le stage change réellement |
| Lead dormant | `LEAD_DORMANT` | Agent assigné | `LeadDormantAlertWorker` (cron) |

> Ces emails sont envoyés de façon asynchrone et best-effort (échec silencieux loggé) — le frontend n'a rien à orchestrer, mais peut informer l'utilisateur que "l'agent sera notifié par email" lors des actions de changement de stage / d'assignation.

---

## 10. Worker — Alertes leads dormants

`LeadDormantAlertWorker` :
- **Cron par défaut :** `0 0 8 * * MON-FRI` (8h, du lundi au vendredi), configurable via `bokati.crm.dormant-cron`.
- **Seuil :** `bokati.crm.dormant-threshold-days` (défaut 7 jours sans activité).
- **Logique :** sélectionne les leads `stage NOT IN (WON, LOST)` avec `lastActivityAt < now - seuil` et `dormantAlertSentAt IS NULL`, envoie l'alerte, marque `dormantAlertSentAt = now`.
- **Reset automatique :** toute nouvelle activité (§4.5) remet `dormantAlertSentAt` à `null`.

> Suggestion UI : indiquer visuellement les leads "à risque" (pas d'activité depuis > seuil) directement sur les cartes Kanban / la liste — par ex. un badge "⏳ Inactif depuis Xj" basé sur `lastActivityAt`, calculable côté frontend sans appel supplémentaire.

---

## 11. Propositions d'écrans (UI)

### 11.1 Cartographie des écrans

| Écran | Route suggérée | Données | Permissions |
|---|---|---|---|
| **Tableau de bord CRM** | `/crm` | `/metrics` + `/analytics` | `CRM:READ` |
| **Pipeline Kanban** | `/crm/pipeline` | `/board` (+ `/leads/{id}/stage` pour drag&drop) | `CRM:READ`, `CRM:WRITE` |
| **Liste des leads** | `/crm/leads` | `/leads` (recherche/filtre paginé) | `CRM:READ` |
| **Fiche lead** | `/crm/leads/:id` | `/leads/{id}` + actions | `CRM:READ`, `CRM:WRITE`, `CRM:CONVERT` |
| **Liste opportunités** | `/crm/opportunities` | `/opportunities` | `CRM:READ` |
| **Formulaire de capture (site public)** | page vitrine externe | `POST /public/crm/leads` | aucune |
| **Analytics CRM** | `/crm/analytics` | `/analytics` avec sélecteur de période | `CRM:READ` |

### 11.2 `CrmDashboardPage` — Tableau de bord

Composants :
- `CrmMetricsCards` : 4-6 cartes KPI — total leads, nouveaux, gagnés, perdus, taux de conversion (%), valeur du pipeline (formatée en devise).
- `CrmPipelineFunnelChart` : graphique en entonnoir basé sur `byStage` (analytics) — visualise la déperdition entre étapes.
- `CrmSourceBreakdownChart` : camembert/barres sur `bySource`.
- `CrmRecentActivityFeed` (optionnel) : derniers leads créés / dernières activités, à dériver de `/leads?sort=createdAt,desc&size=5`.
- Raccourcis vers `Pipeline Kanban` et `Liste des leads`.

### 11.3 `CrmPipelinePage` — Vue Kanban

- 6 colonnes fixes (`NEW → LOST`), chacune avec en-tête `{libellé} · {count} · {totalAmount formaté}`.
- `KanbanCard` compacte : nom, société, badge source, badge intérêt, montant estimé, jauge de score (`score`), avatar/initiales de l'agent (`assignedTo`), badge "⏳ inactif" si pertinent.
- **Drag-and-drop** entre colonnes → `PATCH /leads/{id}/stage` :
  - Si la colonne cible est `LOST`, ouvrir une **modale obligatoire** demandant `lostReason` avant de confirmer le déplacement (le serveur l'accepte vide, mais l'UX doit l'imposer pour la traçabilité).
  - Optimistic UI : déplacer la carte immédiatement, revert si l'appel échoue (toast d'erreur).
- Barre de filtres en en-tête : `assignedTo` (sélecteur d'agent), `source`, `interest`, `searchText` (debounce).
- Clic sur une carte → navigation vers `LeadDetailPage`.
- Bouton "+ Nouveau lead" → ouvre `LeadFormModal` (création rapide, stage forcé à `NEW`).

### 11.4 `LeadListPage` — Liste/Table des leads

- Table paginée (`PaginatedResponse`) avec colonnes : numéro, nom, société, stage (badge coloré), source, agent assigné, montant estimé, score, dernière activité, créé le.
- Filtres : `stage`, `assignedTo`, `source`, `searchText` + pagination/tri serveur.
- Actions de ligne : voir fiche, qualifier rapidement (`PATCH /qualify` si `stage = NEW`), assigner.
- Export CSV (dérivable côté frontend depuis les pages chargées, ou à itérer sur toutes les pages).

### 11.5 `LeadDetailPage` — Fiche lead

Sections :
1. **En-tête** : `leadNumber`, `fullName`, badges stage/source/interest, score (jauge), montant estimé, agent assigné. Actions principales : `Qualifier`, `Changer de stage`, `Convertir`, `Générer un devis`, `Créer une opportunité`.
2. **Informations** : coordonnées (email, téléphone, société), note, date de clôture prévue, probabilité manuelle vs score automatique (afficher les deux côte à côte avec une explication au survol).
3. **Historique d'activités** (`activities[]`) : timeline chronologique avec icônes par `activityType` (📞 appel, ✉️ email, 🤝 rendez-vous, 🚶 visite, 📝 note), auteur (`performedBy`), date (`performedAt`).
   - Formulaire d'ajout rapide (`AddLeadActivityForm`) toujours visible en haut de la timeline → `POST /activities`.
4. **Opportunités liées** : liste des `Opportunity` rattachées (si l'API permet de filtrer par lead, sinon dériver de `/opportunities` côté client ou ajouter le filtre côté backend si besoin futur) + bouton "Nouvelle opportunité".
5. **Documents** : si une conversion a eu lieu, lien vers le client/membre (`convertedOwnerType` + `convertedOwnerCode`) ; si un devis a été généré, lien vers le document de facturation (`quoteNumber` retourné par `/generate-quote`, traçable via `sourceType=CRM_LEAD`/`sourceCode={leadNumber}` côté billing).

### 11.6 `LeadStageModal` / `ConvertLeadModal`

- **Changement de stage** : sélecteur de stage + champ `lostReason` conditionnel (affiché et requis uniquement si `LOST`).
- **Conversion** (`CRM:CONVERT` uniquement) — flux en deux étapes recommandé :
  1. Choix : "Lier à un client/membre existant" (recherche dans `customer`/`member`) ou "Créer un nouveau client/membre" (redirection/ouverture du formulaire du module cible, pré-rempli avec les coordonnées du lead).
  2. Une fois le `code` obtenu, soumission de `PATCH /leads/{id}/convert` avec `{ ownerType, ownerCode }`.
  - Afficher un avertissement clair : "Cette action marque le lead comme Gagné (`WON`) et ne peut pas être annulée depuis cet écran."

### 11.7 `GenerateQuoteModal`

- Sélecteur de devise (`currency`, ex. XAF/EUR/USD selon configuration), champ `performedBy` (auto-rempli avec l'utilisateur connecté).
- Désactiver le bouton si `lead.stage === 'LOST'` (le serveur refuserait de toute façon — anticiper l'erreur `400`).
- À la confirmation, afficher le `quoteNumber` retourné et proposer un lien direct vers le devis dans le module facturation.

### 11.8 `LeadCaptureForm` — Formulaire public (site vitrine)

- Champs : `fullName*`, `email`, `phone`, `company`, `interest` (liste déroulante avec libellés FR), `note`/message libre, `source` (peut être pré-rempli `WEBSITE` ou déduit de l'UTM de la page).
- Soumission → `POST /public/crm/leads`, afficher le `message` retourné + numéro de référence.
- Pas d'authentification, pas de permissions à vérifier — penser rate-limiting/captcha côté infra (hors périmètre backend documenté ici).

### 11.9 `OpportunityListPage` / `OpportunityKanban` (optionnel)

- Réutiliser le pattern Kanban (5 colonnes : `OPEN, NEGOTIATION, PROPOSAL_SENT, WON, LOST`) ou une simple table filtrable par `stage`.
- Chaque carte/ligne doit afficher le lead parent (`leadFullName`, `leadNumber`) avec lien vers `LeadDetailPage`.

### 11.10 `CrmAnalyticsPage`

- Sélecteur de période (`from`/`to`, presets : 7j / 30j / trimestre / année / personnalisé).
- KPIs période : `totalCreated`, `totalWon`, `totalLost`, `conversionRate`, `avgDaysToWin`.
- Graphiques `bySource` et `byStage` (barres ou camemberts).
- `pipelineValue` affiché séparément avec la mention "valeur actuelle du pipeline actif" (non lié à la période sélectionnée — cf. §8.3).

---

## 12. Parcours d'utilisation recommandés

### 12.1 Cycle de vie standard d'un lead

1. **Capture** — via le formulaire public (`LeadCaptureForm`) ou saisie manuelle backoffice (`+ Nouveau lead`). Stage initial toujours `NEW`.
2. **Premier contact** — l'agent ajoute une activité `CALL`/`EMAIL`, puis utilise `Qualifier` (`PATCH /qualify`) ou déplace la carte vers `CONTACTED`/`QUALIFIED` sur le Kanban.
3. **Suivi commercial** — ajout régulier d'activités (`MEETING`, `VISIT`, `NOTE`) ; éventuellement création d'une ou plusieurs `Opportunity` pour structurer des offres distinctes sur le même lead.
4. **Proposition** — génération d'un devis (`Générer un devis`) qui bascule le lead à `PROPOSAL_SENT` (manuellement, via le sélecteur de stage — le devis ne change pas le stage automatiquement).
5. **Issue** :
   - **Gagné** → conversion en client/membre (`Convertir`), stage forcé à `WON`.
   - **Perdu** → déplacement vers `LOST` avec `lostReason` obligatoire côté UI.

### 12.2 Suivi quotidien d'un agent commercial

- Page d'accueil = `CrmPipelinePage` filtrée sur `assignedTo = moi`.
- Repérer les cartes marquées "⏳ inactif" (calcul frontend sur `lastActivityAt`) avant que l'alerte automatique (worker, §10) ne se déclenche.
- Utiliser la recherche (`searchText`) pour retrouver un lead par nom/société/numéro rapidement (champ de recherche globale, debounce ~300ms).

### 12.3 Pilotage / management

- `CrmDashboardPage` pour la vue d'ensemble (KPIs + funnel).
- `CrmAnalyticsPage` pour comparer les périodes, analyser les sources les plus performantes (`bySource`) et le temps moyen de conversion (`avgDaysToWin`).
- Filtrer le Kanban/la liste par `assignedTo` pour examiner la charge et la performance par agent.

### 12.4 Bonnes pratiques d'intégration technique

- **Pagination** : toujours utiliser `PaginatedResponse.pageable` pour piloter une pagination serveur (ne pas recharger toutes les pages côté client).
- **Optimistic updates** : recommandé pour le drag-and-drop Kanban et les actions de stage (UX fluide), avec rollback sur erreur HTTP.
- **Rafraîchissement** : après toute mutation (`POST`/`PATCH`/`PUT`), invalider/recharger à la fois la ressource modifiée et les vues agrégées concernées (`/board`, `/metrics`) si elles sont affichées simultanément.
- **Distinction probabilité manuelle vs score auto** : ne jamais fusionner `probability` (saisie agent) et `scoredProbability`/`score` (calculé) dans un seul champ d'affichage — ce sont deux signaux complémentaires à présenter côte à côte.
- **`LeadStage` ≠ `OpportunityStage`** : prévoir des composants de badge/sélecteur distincts, ne pas les réutiliser tels quels.

---

## 13. Migrations Flyway concernées

| Version | Fichier | Contenu |
|---|---|---|
| (initiale) | — | Tables `crm_lead`, `crm_lead_activity` |
| V99 | `crm_enhancements_opportunity.sql` | Ajout `lead_number`, `last_activity_at`, `dormant_alert_sent_at` sur `crm_lead` ; création table `crm_opportunity` ; index de recherche/dormance |

---

## 14. Récapitulatif des permissions par action UI

| Action UI | Permission requise |
|---|---|
| Voir listes/fiches/board/dashboard/analytics | `CRM:READ` |
| Créer/modifier un lead, ajouter une activité, créer/modifier une opportunité, changer un stage, générer un devis | `CRM:WRITE` |
| Convertir un lead en client/membre | `CRM:CONVERT` |
| Soumettre le formulaire public | aucune (endpoint public) |
