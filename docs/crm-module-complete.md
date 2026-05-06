# Module CRM — Documentation complète

> Dernière mise à jour : 2026-05-05  
> Statut : ✅ Complet et production-ready

---

## 1. Vue d'ensemble

Le module CRM gère le cycle de vie complet des prospects (leads) depuis leur première interaction jusqu'à leur conversion en client ou membre. Il inclut un pipeline commercial visuel, la gestion des opportunités, le scoring automatique, les notifications email et un worker d'alertes pour les leads dormants.

---

## 2. Structure des fichiers

```
features/crm/
├── enums/
│   ├── LeadStage.java         → NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, WON, LOST
│   ├── LeadSource.java        → WEBSITE, REFERRAL, COLD_CALL, TRADE_SHOW, PARTNER, SOCIAL_MEDIA, EVENT, OTHER
│   ├── LeadInterest.java      → WORKSPACE, MEETING_ROOM, VIRTUAL_OFFICE, DOMICILIATION, DAY_PASS, DEDICATED_DESK, PRIVATE_OFFICE, OTHER
│   ├── LeadActivityType.java  → CALL, EMAIL, MEETING, VISIT, NOTE
│   └── OpportunityStage.java  → OPEN, NEGOTIATION, PROPOSAL_SENT, WON, LOST
├── model/
│   ├── Lead.java              → entité principale + lastActivityAt + dormantAlertSentAt
│   ├── LeadActivity.java      → historique des interactions
│   └── Opportunity.java       → opportunités commerciales liées à un lead
├── repository/
│   ├── LeadRepository.java    → search + dormant + métriques + analytics
│   ├── LeadActivityRepository.java
│   └── OpportunityRepository.java
├── dto/
│   └── CrmDtos.java           → tous les records
├── mapper/
│   └── CrmMapper.java         → Lead + Activity + Opportunity
├── service/
│   ├── interfaces/
│   │   ├── CrmService.java
│   │   └── CrmEmailService.java
│   └── implementation/
│       ├── CrmServiceImpl.java
│       └── CrmEmailServiceImpl.java
├── worker/
│   └── LeadDormantAlertWorker.java    → cron quotidien (lun-ven 8h)
└── controller/
    ├── CrmController.java             → admin authentifié
    └── PublicCrmController.java       → formulaire site web (sans auth)
```

---

## 3. Modèle de données

### Lead (`crm_lead`)

| Champ | Type | Description |
|---|---|---|
| `leadNumber` | VARCHAR(80) UNIQUE | Format `LDN-{epoch_millis}` |
| `fullName` | VARCHAR NOT NULL | Nom complet du prospect |
| `email` / `phone` / `company` | VARCHAR | Coordonnées |
| `note` | TEXT | Note libre |
| `source` | ENUM | Canal d'acquisition (LeadSource) |
| `interest` | ENUM | Produit/service souhaité (LeadInterest) |
| `stage` | ENUM | Étape du pipeline (LeadStage) |
| `estimatedAmount` | NUMERIC(19,4) | Valeur estimée du deal |
| `probability` | INTEGER | Probabilité manuelle (%) |
| `expectedCloseDate` | DATE | Date de clôture prévue |
| `assignedTo` | BIGINT | ID de l'agent commercial |
| `convertedOwnerType` | VARCHAR | MEMBER / CUSTOMER (après conversion) |
| `convertedOwnerCode` | VARCHAR | Code du client créé |
| `lostReason` | TEXT | Raison de perte si stage=LOST |
| `lastActivityAt` | TIMESTAMPTZ | Dernière interaction enregistrée |
| `dormantAlertSentAt` | TIMESTAMPTZ | Date d'envoi de la dernière alerte dormant |

### Opportunity (`crm_opportunity`)

| Champ | Type | Description |
|---|---|---|
| `opportunityNumber` | VARCHAR(80) UNIQUE | Format `OPP-{epoch_millis}` |
| `lead_id` | FK crm_lead | Lead associé |
| `title` | VARCHAR(200) | Titre de l'opportunité |
| `estimatedAmount` | NUMERIC(19,4) | Valeur estimée |
| `probability` | INTEGER | Probabilité (%) |
| `stage` | ENUM | OpportunityStage |
| `expectedCloseDate` | DATE | Date cible |
| `assignedTo` | BIGINT | Agent commercial |
| `notes` | TEXT | Notes |
| `wonAt` / `lostAt` | TIMESTAMPTZ | Horodatages |
| `lostReason` | TEXT | Raison de perte |

---

## 4. Pipeline commercial

```
NEW → CONTACTED → QUALIFIED → PROPOSAL_SENT → WON
                                            ↘ LOST
```

Chaque transition déclenche une notification email à l'agent assigné.

---

## 5. Scoring automatique (scoredProbability)

Calculé automatiquement à chaque lecture, stocké dans `scoredProbability` dans la réponse.

| Composant | Valeur |
|---|---|
| Stage NEW | 5 |
| Stage CONTACTED | 20 |
| Stage QUALIFIED | 40 |
| Stage PROPOSAL_SENT | 65 |
| Stage WON | 100 (fixe) |
| Stage LOST | 0 (fixe) |
| Bonus activités | +5 par activité, max +20 |
| Activité récente (< 7 jours) | +5 |
| Inactivité (> 7 jours) | -5 |
| Plafond | max 95 (hors WON) |

Le champ `probability` reste la valeur manuelle saisie par l'agent. `scoredProbability` est le calcul automatique.

---

## 6. Endpoints Admin

**Base URL :** `/sni/api/v1/crm`

### Leads

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| POST | `/leads` | CRM:WRITE | Créer un lead |
| GET | `/leads` | CRM:READ | Recherche (`stage`, `assignedTo`, `source`, `searchText`) |
| GET | `/leads/{id}` | CRM:READ | Détail par ID |
| GET | `/leads/number/{leadNumber}` | CRM:READ | Détail par numéro |
| PUT | `/leads/{id}` | CRM:WRITE | Mise à jour complète |
| PATCH | `/leads/{id}/qualify` | CRM:WRITE | Passage à QUALIFIED |
| PATCH | `/leads/{id}/stage` | CRM:WRITE | Changement de stage |
| PATCH | `/leads/{id}/convert` | CRM:CONVERT | Conversion en client/membre |
| POST | `/leads/{id}/activities` | CRM:WRITE | Logger une interaction |
| POST | `/leads/{id}/generate-quote` | CRM:WRITE | Générer un devis QUOTE |
| POST | `/leads/{id}/opportunities` | CRM:WRITE | Créer une opportunité |

### Opportunités

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| GET | `/opportunities` | CRM:READ | Lister (`?stage=`) |
| PATCH | `/opportunities/{id}/stage` | CRM:WRITE | Changer le stage |

### Vue globale

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| GET | `/pipeline` | CRM:READ | Vue Kanban par stage |
| GET | `/metrics` | CRM:READ | KPIs temps réel |
| GET | `/analytics` | CRM:READ | Analytics par période (`?from=&to=`) |

---

## 7. Endpoint Public

**Base URL :** `/sni/api/v1/public/crm` — aucune authentification

| Méthode | Chemin | Description |
|---|---|---|
| POST | `/leads` | Formulaire de contact depuis le site web |

**Réponse :**
```json
{
  "leadNumber": "LDN-1746394823456",
  "message": "Votre demande a bien été reçue. Notre équipe vous contactera sous 24h."
}
```

---

## 8. Notifications email automatiques

Template Thymeleaf : `email/crm-event.html`

| Événement | Destinataire | Déclencheur |
|---|---|---|
| Lead assigné | Agent (email via UserService) | `create()` ou `update()` si agent change |
| Stage changé | Agent assigné | `updateStage()`, `qualify()`, `convert()` |
| Lead dormant | Agent assigné | `LeadDormantAlertWorker` (cron lun-ven 8h) |

---

## 9. Worker automatique — Leads dormants

### LeadDormantAlertWorker
- **Déclenchement :** cron `0 0 8 * * MON-FRI` (lun-ven à 8h, configurable)
- **Logique :**
  1. Cherche les leads avec `stage NOT IN (WON, LOST)` ET `lastActivityAt < now - threshold` ET `dormantAlertSentAt IS NULL`
  2. Envoie un email d'alerte à l'agent assigné
  3. Met à jour `dormantAlertSentAt = now` pour ne pas renvoyer
- **Reset :** `dormantAlertSentAt` est remis à `null` dès qu'une nouvelle activité est ajoutée

**Configuration :**
```yaml
bokati:
  crm:
    dormant-threshold-days: 7      # jours sans activité avant alerte
    dormant-cron: "0 0 8 * * MON-FRI"  # expression cron
```

---

## 10. Analytics CRM

**GET** `/sni/api/v1/crm/analytics?from=2026-01-01T00:00:00Z&to=2026-04-30T23:59:59Z`

```json
{
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-04-30T23:59:59Z",
  "totalCreated": 89,
  "totalWon": 23,
  "totalLost": 15,
  "conversionRate": 25.8,
  "avgDaysToWin": 18.4,
  "bySource": {
    "WEBSITE": 42,
    "REFERRAL": 28,
    "COLD_CALL": 12,
    "OTHER": 7
  },
  "byStage": {
    "NEW": 12,
    "CONTACTED": 18,
    "QUALIFIED": 9,
    "PROPOSAL_SENT": 12,
    "WON": 23,
    "LOST": 15
  },
  "pipelineValue": 45750000.0000
}
```

---

## 11. Génération de devis depuis un lead

**POST** `/sni/api/v1/crm/leads/{id}/generate-quote`

```json
{ "currency": "XAF", "performedBy": "agent@bokati.com" }
```

- Crée un document de type `QUOTE` via `BillingDocumentService.createManualQuote()`
- Pré-remplit avec les données du lead (nom, email, téléphone, intérêt, montant estimé)
- Source tracée : `sourceType=CRM_LEAD`, `sourceCode={leadNumber}`
- Enregistre automatiquement une activité `NOTE` sur le lead
- Retourne : `{ "quoteNumber": "QTE-2026-000042" }`

---

## 12. Migrations Flyway

| Version | Fichier | Contenu |
|---|---|---|
| (existant) | tables `crm_lead` et `crm_lead_activity` créées | Structure initiale |
| V99 | `crm_enhancements_opportunity.sql` | `lead_number`, `last_activity_at`, `dormant_alert_sent_at` sur crm_lead ; table `crm_opportunity` ; index |

---

## 13. Permissions requises

| Permission | Accès |
|---|---|
| `CRM:READ` / `CRM_READ` | Lire leads, pipeline, métriques, analytics, opportunités |
| `CRM:WRITE` / `CRM_WRITE` | Créer/modifier leads, activités, opportunités, générer devis |
| `CRM:CONVERT` / `CRM_CONVERT` | Convertir un lead en client/membre |
