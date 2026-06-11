# CRM — Kanban Board (Vue tableau interactif)

> Dernière mise à jour : 2026-05-21  
> Statut : ✅ Complet et production-ready

---

## 1. Vue d'ensemble

Le Kanban Board CRM expose l'ensemble des leads regroupés par **stage** sous forme de colonnes, à l'image d'un tableau Jira. Chaque lead est représenté par une **carte compacte** (sans la liste détaillée des activités) pour des performances optimales. Les filtres permettent de restreindre la vue par agent, source, intérêt ou texte libre. Le déplacement d'une carte d'une colonne à l'autre se fait via l'endpoint de mise à jour de stage existant.

---

## 2. Endpoints

**Base URL :** `/sni/api/v1/crm`

| Méthode | Chemin | Permission | Description |
|---|---|---|---|
| GET | `/board` | CRM:READ | Tableau Kanban — toutes les colonnes et cartes |
| PATCH | `/leads/{id}/stage` | CRM:WRITE | Déplacer une carte (changer de colonne) |

---

## 3. GET `/board` — Paramètres

| Paramètre | Type | Obligatoire | Description |
|---|---|---|---|
| `assignedTo` | Long | Non | Filtrer par ID d'agent assigné |
| `source` | LeadSource | Non | Filtrer par canal d'acquisition |
| `interest` | LeadInterest | Non | Filtrer par intérêt produit |
| `searchText` | String | Non | Recherche sur nom, email, company, numéro |

**Valeurs `source` :** `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER`

**Valeurs `interest` :** `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER`

---

## 4. Colonnes du tableau

Les colonnes suivent l'ordre du pipeline commercial. Elles sont toujours toutes retournées, même vides.

| Colonne | Stage | Description |
|---|---|---|
| 1 | `NEW` | Leads nouvellement créés |
| 2 | `CONTACTED` | Premier contact établi |
| 3 | `QUALIFIED` | Lead qualifié |
| 4 | `PROPOSAL_SENT` | Devis envoyé |
| 5 | `WON` | Convertis / gagnés |
| 6 | `LOST` | Perdus |

Les cartes dans chaque colonne sont triées par **activité la plus récente en premier** (`last_activity_at DESC`, fallback sur `created_at`).

---

## 5. Structure de réponse

### `KanbanBoardResponse`

```json
{
  "columns": [ ... ],
  "totalLeads": 24,
  "totalPipelineValue": 87500.00
}
```

> `totalPipelineValue` exclut les leads `LOST`.

### `KanbanColumnResponse`

```json
{
  "stage": "QUALIFIED",
  "count": 5,
  "totalAmount": 22000.00,
  "cards": [ ... ]
}
```

### `KanbanCardResponse`

```json
{
  "id": 42,
  "leadNumber": "LDN-1746394823456",
  "fullName": "Jean Dupont",
  "company": "Acme SARL",
  "email": "jean@acme.com",
  "stage": "QUALIFIED",
  "source": "REFERRAL",
  "interest": "PRIVATE_OFFICE",
  "estimatedAmount": 4500.00,
  "probability": 60,
  "score": 55,
  "assignedTo": 7,
  "lastActivityAt": "2026-05-19T10:30:00Z",
  "activityCount": 3,
  "createdAt": "2026-05-10T08:00:00Z"
}
```

| Champ | Description |
|---|---|
| `score` | Score automatique calculé (0–100) — voir scoring dans `crm-module-complete.md` §5 |
| `activityCount` | Nombre total d'activités enregistrées |
| `probability` | Probabilité manuelle saisie par l'agent |

---

## 6. Déplacer une carte (drag-and-drop)

Utiliser l'endpoint de stage existant :

**PATCH** `/sni/api/v1/crm/leads/{id}/stage`

```json
{
  "stage": "PROPOSAL_SENT",
  "lostReason": null
}
```

Si la cible est `LOST`, le champ `lostReason` est obligatoire pour traçabilité.

```json
{
  "stage": "LOST",
  "lostReason": "Budget insuffisant"
}
```

**Réponse :** `LeadResponse` complet (avec activités). Un email est envoyé automatiquement à l'agent assigné lors de chaque changement de stage.

---

## 7. Exemple de réponse complète

```json
{
  "columns": [
    {
      "stage": "NEW",
      "count": 2,
      "totalAmount": 3000.00,
      "cards": [
        {
          "id": 1,
          "leadNumber": "LDN-1746000001000",
          "fullName": "Marie Martin",
          "company": null,
          "email": "marie@example.com",
          "stage": "NEW",
          "source": "WEBSITE",
          "interest": "DAY_PASS",
          "estimatedAmount": 1500.00,
          "probability": null,
          "score": 10,
          "assignedTo": null,
          "lastActivityAt": null,
          "activityCount": 0,
          "createdAt": "2026-05-20T14:00:00Z"
        }
      ]
    },
    {
      "stage": "CONTACTED",
      "count": 0,
      "totalAmount": 0,
      "cards": []
    },
    {
      "stage": "QUALIFIED",
      "count": 1,
      "totalAmount": 4500.00,
      "cards": [ { "..." } ]
    },
    { "stage": "PROPOSAL_SENT", "count": 0, "totalAmount": 0, "cards": [] },
    { "stage": "WON",           "count": 3, "totalAmount": 12000.00, "cards": [ "..." ] },
    { "stage": "LOST",          "count": 1, "totalAmount": 0, "cards": [ "..." ] }
  ],
  "totalLeads": 7,
  "totalPipelineValue": 19500.00
}
```

---

## 8. Performance

- Chaque colonne déclenche **1 requête filtrée** sur `crm_lead`
- Le `activityCount` est calculé via une **requête bulk** par colonne (`countByLeadIds`) — aucun problème N+1
- Total maximum : **12 requêtes** pour un board complet (6 colonnes × 2 queries)

---

## 9. Permissions requises

| Action | Permission |
|---|---|
| Voir le board | `CRM:READ` ou `CRM_READ` |
| Déplacer une carte | `CRM:WRITE` ou `CRM_WRITE` |
| Convertir en client | `CRM:CONVERT` ou `CRM_CONVERT` |