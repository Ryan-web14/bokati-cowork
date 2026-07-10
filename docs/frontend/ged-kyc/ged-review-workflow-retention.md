# GED — File de revue, workflows d'approbation & rétention

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Back-office**, réservé aux réviseurs / compliance.

Trois blocs pour les gros volumes et les circuits d'approbation :

- **File de revue en masse** (`/review`) — traiter plusieurs documents d'un coup.
- **Workflows personnalisés** (`/document-workflows`) — circuits multi-étapes.
- **Politiques de rétention** (`/document-retention-policies`) — archivage/purge automatique.

---

## Partie A — File de revue en masse · `/review`

🔒 File globale : permission **`DOCUMENT:REVIEW`**. Files et actions par espace : contrôle **par espace** (`DOCUMENT_REVIEW_<SPACE>`).

### A.1 Consulter la file

```http
GET /review/queue?page=0&size=20            🔒 DOCUMENT:REVIEW   (tous espaces confondus)
GET /review/{space}?page=0&size=20          🔒 par espace         (space = valeur de DocumentSpace)
```
**Réponse** → `PaginatedResponse<DocumentResponse>` (documents `PENDING_REVIEW`, enveloppe `data`/`pageable`).

### A.2 Approuver en masse

```http
POST /review/{space}/bulk-approve
Content-Type: application/json
```
```json
{ "codes": ["DOC-...41", "DOC-...42", "DOC-...43"], "reviewedBy": 42, "comment": "Lot conforme" }
```
`codes` (non vide) et `reviewedBy` obligatoires ; `comment` optionnel.

### A.3 Rejeter en masse

```http
POST /review/{space}/bulk-reject
```
```json
{
  "codes": ["DOC-...44", "DOC-...45"],
  "reviewedBy": 42,
  "rejectionReasonCode": "ILLEGIBLE",
  "rejectionReasonDetail": "Documents flous",
  "comment": ""
}
```

### A.4 Demander correction en masse

```http
POST /review/{space}/bulk-request-correction
```
```json
{ "codes": ["DOC-...46"], "reviewedBy": 42, "correctionNote": "Renvoyer une version nette", "deadlineDays": 7 }
```
`correctionNote` obligatoire ; `deadlineDays` défaut `7` (min 1).

### A.5 Réponse commune — `DocumentBulkActionResponse`

Les trois actions renvoient le même objet, avec le détail par document :

```json
{
  "total": 3,
  "succeeded": 2,
  "failed": 1,
  "results": [
    { "code": "DOC-...41", "status": "SUCCESS", "reason": null },
    { "code": "DOC-...42", "status": "SUCCESS", "reason": null },
    { "code": "DOC-...43", "status": "FAILED", "reason": "Document verrouillé" }
  ]
}
```

> ⚠️ **Un seul motif/commentaire pour tout le lot** — les actions en masse GED n'acceptent pas un motif différent par document (contrairement au KYC, voir [kyc-document-review-admin.md](./kyc-document-review-admin.md)). En revanche `results[]` détaille le succès/échec de chaque élément.

---

## Partie B — Workflows d'approbation personnalisés · `/document-workflows`

Pour les documents nécessitant plusieurs étapes de validation successives (au-delà du simple approve/reject). Aucune permission spécifique déclarée (auth admin standard).

### B.1 Créer une définition de workflow

```http
POST /document-workflows
Content-Type: application/json
```
```json
{
  "name": "Validation contrat en 2 étapes",
  "description": "Juridique puis Direction",
  "workflowType": "SEQUENTIAL",
  "space": "CONTRACT_SPACE",
  "documentTypeCode": "CONTRAT",
  "steps": [
    { "stepName": "Revue juridique", "approverType": "ROLE", "approverValue": "LEGAL", "required": true, "autoApproveDays": null },
    { "stepName": "Validation direction", "approverType": "USER", "approverValue": "42", "required": true, "autoApproveDays": 5 }
  ]
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `name` | ✅ | Nom du workflow. |
| `description` | — | Texte libre. |
| `workflowType` | — | Type (ex `SEQUENTIAL`). |
| `space` / `documentTypeCode` | — | Portée d'application. |
| `steps` | ✅ (non vide) | Étapes ordonnées. |
| `steps[].stepName` | ✅ | Nom de l'étape. |
| `steps[].approverType` | ✅ | Type d'approbateur (ex `ROLE`, `USER`). |
| `steps[].approverValue` | ✅ | Valeur associée (rôle ou id utilisateur). |
| `steps[].required` | — | Étape obligatoire. |
| `steps[].autoApproveDays` | — | Auto-approbation de l'étape après N jours. |

**Réponse** `201 Created` → `WorkflowResponse` :

```json
{
  "code": "WKF-202607-00000001",
  "name": "Validation contrat en 2 étapes",
  "description": "Juridique puis Direction",
  "workflowType": "SEQUENTIAL",
  "space": "CONTRACT_SPACE",
  "documentTypeCode": "CONTRAT",
  "active": true,
  "createdAt": "2026-07-09T10:00:00Z",
  "steps": [
    { "id": 1, "stepOrder": 1, "stepName": "Revue juridique", "approverType": "ROLE", "approverValue": "LEGAL", "required": true, "autoApproveDays": null },
    { "id": 2, "stepOrder": 2, "stepName": "Validation direction", "approverType": "USER", "approverValue": "42", "required": true, "autoApproveDays": 5 }
  ]
}
```

### B.2 Lire / supprimer les définitions

```http
GET    /document-workflows/{code}                  → WorkflowResponse
GET    /document-workflows?activeOnly=true         → List<WorkflowResponse>   (activeOnly optionnel)
DELETE /document-workflows/{code}                  → 204 No Content
```

### B.3 Exécuter un workflow sur un document

```http
POST /documents/{documentCode}/start-workflow?workflowCode=WKF-...   → WorkflowInstanceResponse (201)
POST /documents/{documentCode}/workflow/approve                       → WorkflowInstanceResponse
POST /documents/{documentCode}/workflow/reject                        → WorkflowInstanceResponse
GET  /documents/{documentCode}/workflow/status                        → WorkflowInstanceResponse
GET  /document-workflows/my-pending                                   → List<WorkflowInstanceResponse>
```

- `start-workflow` prend `workflowCode` en **query param**.
- `approve`/`reject` acceptent un **body optionnel** `{ "comment": "..." }` (peut être omis).
- `my-pending` = les instances en attente de l'utilisateur connecté.

`WorkflowInstanceResponse` :
```json
{
  "id": 501,
  "workflowCode": "WKF-202607-00000001",
  "workflowName": "Validation contrat en 2 étapes",
  "documentCode": "DOC-...42",
  "documentTitle": "Contrat de bail",
  "currentStep": 2,
  "currentStepName": "Validation direction",
  "status": "IN_PROGRESS",
  "startedAt": "2026-07-09T10:05:00Z",
  "completedAt": null,
  "actions": [
    { "stepOrder": 1, "stepName": "Revue juridique", "action": "APPROVED", "actorId": 17, "actorEmail": "legal@elleaose.com", "actedAt": "2026-07-09T11:00:00Z", "comment": "OK" }
  ]
}
```

---

## Partie C — Politiques de rétention · `/document-retention-policies`

🔒 **Toutes** les routes exigent **`DOCUMENT:REVIEW`**. Une politique déclenche automatiquement un archivage ou un marquage de purge après une durée.

### C.1 Créer / modifier

```http
POST /document-retention-policies
Content-Type: application/json
```
```json
{
  "code": "KYC-5ANS",
  "name": "Conservation KYC 5 ans",
  "space": "KYC_SPACE",
  "documentTypeCode": null,
  "retentionDays": 1825,
  "retentionReference": "UPLOAD_DATE",
  "action": "ARCHIVE"
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `code` | ✅ | Identifiant unique. |
| `name` | ✅ | Libellé. |
| `space` | — | Portée par espace (**nul = global**). |
| `documentTypeCode` | — | Restreint à un type de document. |
| `retentionDays` | ✅ (≥ 1) | Durée de conservation en jours. |
| `retentionReference` | — | Point de départ : `UPLOAD_DATE` (défaut) ou `EXPIRY_DATE`. |
| `action` | — | `ARCHIVE` (défaut) ou `PURGE_FLAG`. |

**Réponse** `201 Created` (ou `200` en update) → `DocumentRetentionPolicyResponse` :
```json
{
  "id": 3,
  "code": "KYC-5ANS",
  "name": "Conservation KYC 5 ans",
  "space": "KYC_SPACE",
  "documentTypeCode": null,
  "retentionDays": 1825,
  "retentionReference": "UPLOAD_DATE",
  "action": "ARCHIVE",
  "active": true,
  "createdAt": "2026-07-09T10:00:00Z",
  "createdBy": 42
}
```

```http
PUT    /document-retention-policies/{code}    → DocumentRetentionPolicyResponse   (mêmes champs)
```

### C.2 Lire / désactiver

```http
GET    /document-retention-policies           → List<DocumentRetentionPolicyResponse>
GET    /document-retention-policies/{code}     → DocumentRetentionPolicyResponse
DELETE /document-retention-policies/{code}     → 204 No Content   (soft-delete : active = false)
```

### Enums

- **DocumentRetentionAction** : `ARCHIVE, PURGE_FLAG`
- **DocumentRetentionReference** : `UPLOAD_DATE, EXPIRY_DATE`
