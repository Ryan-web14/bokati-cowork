# KYC — Dossiers (back-office)

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Base de ce module** : `/kyc/cases` — **back-office** (JWT admin requis).
> Voir le [README](./README.md) pour le rattachement `ownerType`/`ownerCode`.

Un **dossier KYC** regroupe, pour un propriétaire (client, membre ou entreprise), l'ensemble des documents d'identité/justificatifs à collecter et à valider. Ce document couvre tout le cycle de vie : création, exigences, revue des documents, décision, notes, timeline, risque et export PDF.

---

## Le modèle `KycCaseResponse`

Objet renvoyé par la plupart des routes ci-dessous.

```json
{
  "code": "KYC-202607-00000007",
  "ownerName": "Jean Kabila",
  "ownerCode": "CUS-202606-00000012",
  "ownerType": "CUSTOMER",
  "ownerId": 1287,
  "status": "UNDER_REVIEW",
  "startedAt": "2026-07-01T09:00:00Z",
  "submittedAt": "2026-07-05T14:00:00Z",
  "completedAt": null,
  "reviewedBy": null,
  "reviewedByEmail": null,
  "reviewedAt": null,
  "decisionComment": null,
  "assignedTo": 42,
  "assignedToEmail": "agent@elleaose.com",
  "assignedAt": "2026-07-05T15:00:00Z",
  "slaDeadline": "2026-07-07T14:00:00Z",
  "lastReminderSentAt": null,
  "reminderCount": 0,
  "riskLevel": "MEDIUM",
  "kycLevel": 1,
  "complete": false,
  "approved": false,
  "missingDocumentTypeCodes": ["JUSTIF_DOMICILE"],
  "requirements": [
    { "documentTypeCode": "CNI", "documentTypeName": "Carte nationale d'identité", "required": true, "requiresBackSide": true, "status": "VERIFIED", "documentCode": "KYD-...01", "backDocumentCode": "KYD-...02" }
  ],
  "documents": [ /* KycDocumentResponse[] — voir §5 */ ]
}
```

| Champ | Notes |
|---|---|
| `code` | Numéro public du dossier — identifiant à utiliser dans les routes `/{code}`. |
| `ownerType` / `ownerCode` / `ownerName` | Propriétaire rattaché (voir création ci-dessous). |
| `status` | `KycCaseStatus` (voir cycle de vie plus bas). |
| `complete` / `approved` | Raccourcis booléens pratiques pour l'UI. |
| `missingDocumentTypeCodes` | Types encore manquants. |
| `requirements` | Exigences avec leur statut (`KycRequirementStatus`). |
| `slaDeadline` / `reminderCount` | Suivi SLA et relances (gérés automatiquement). |

---

## 1. Créer un dossier

```http
POST /kyc/cases
Content-Type: application/json
Idempotency-Key: <uuid>
```
```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-202606-00000012",
  "documentTypeCodes": ["CNI", "JUSTIF_DOMICILE"]
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `ownerType` | ✅ | `DocumentOwnerType` : `CUSTOMER, MEMBER, BUSINESS, …`. |
| `ownerCode` | ✅ | **Code public** du propriétaire (ex : le `customerId`/`memberId` affiché dans l'UI, **jamais** l'id interne numérique). |
| `documentTypeCodes` | — | Pré-remplit les exigences du dossier. |

**Réponse** `201 Created` → `KycCaseResponse`.

> **Idempotent par propriétaire** : un second `POST` sur le même `ownerType`+`ownerCode` **réutilise le dossier existant** au lieu d'en créer un doublon. La route porte aussi `@Idempotent` (header `Idempotency-Key` recommandé sur les retries réseau).

---

## 2. Lire / rechercher

```http
GET /kyc/cases/{code}
```
**Réponse** → `KycCaseResponse` complet.

```http
GET /kyc/cases?status=&ownerType=&submittedAfter=&submittedBefore=&reviewedBy=&pendingReviewOnly=&expiringWithinDays=&riskLevel=&page=0&size=20
```
Tous les filtres sont optionnels :

| Paramètre | Type | Description |
|---|---|---|
| `status` | `KycCaseStatus` | Filtre par statut. |
| `ownerType` | `DocumentOwnerType` | Filtre par type de propriétaire. |
| `submittedAfter` / `submittedBefore` | date-heure ISO | Fenêtre de soumission. |
| `reviewedBy` | Long | Id du réviseur. |
| `pendingReviewOnly` | bool | Uniquement en attente de revue. |
| `expiringWithinDays` | number | Documents expirant dans N jours. |
| `riskLevel` | `KycRiskLevel` | Filtre par niveau de risque. |

**Réponse** → `PaginatedResponse<KycCaseResponse>` (enveloppe `data`/`pageable` ; tri par défaut `startedAt`).

---

## 3. Cycle de vie & décisions

```http
POST /kyc/cases/{code}/submit                → KycCaseResponse   (status → SUBMITTED)
```
Sans corps. Idempotent.

```http
POST /kyc/cases/{code}/approve               → KycCaseResponse   (status → APPROVED)
POST /kyc/cases/{code}/reject                → KycCaseResponse   (status → REJECTED)
Content-Type: application/json
```
```json
{ "reviewedBy": 42, "comment": "Dossier complet et conforme" }
```
`reviewedBy` (Long) obligatoire ; `comment` optionnel. Ces routes portent `@Idempotent`.

```http
PATCH /kyc/cases/{code}/assign               → KycCaseResponse
```
```json
{ "assignedTo": 42, "email": null }
```
Assigner par `assignedTo` (id) **ou** `email` (les deux optionnels — fournir l'un des deux).

```http
PATCH /kyc/cases/{code}/risk-level           → KycCaseResponse
```
```json
{ "riskLevel": "HIGH", "reviewedBy": 42, "comment": "Client PEP" }
```
`riskLevel` obligatoire ; `reviewedBy` et `comment` optionnels.

---

## 4. Exigences du dossier

```http
GET  /kyc/cases/{code}/missing-requirements   → List<KycRequirementStatus>
GET  /kyc/cases/{code}/requirements           → List<KycCaseRequirementResponse>
POST /kyc/cases/{code}/requirements           → KycCaseRequirementResponse (201)
DELETE /kyc/cases/{code}/requirements/{documentTypeCode}   → 204 No Content
```

Ajouter une exigence :
```json
{ "documentTypeCode": "PASSEPORT", "documentTypeName": "Passeport", "required": true }
```
`documentTypeCode` obligatoire.

`KycRequirementStatus` (exigences manquantes) :
```json
{ "documentTypeCode": "JUSTIF_DOMICILE", "documentTypeName": "Justificatif de domicile", "required": true, "requiresBackSide": false, "status": "PENDING", "documentCode": null, "backDocumentCode": null }
```

`KycCaseRequirementResponse` (exigences configurées) :
```json
{ "id": 12, "kycCaseCode": "KYC-...07", "documentTypeCode": "CNI", "documentTypeName": "Carte nationale d'identité", "required": true, "requiresBackSide": true, "active": true }
```

---

## 5. Documents du dossier & décisions par document

```http
GET /kyc/cases/{code}/review-queue     → List<KycDocumentResponse>   (documents en attente de revue pour ce dossier)
```

`KycDocumentResponse` :
```json
{
  "id": 501,
  "documentCode": "KYD-202607-00000001",
  "documentType": "CNI",
  "ownerName": "Jean Kabila",
  "documentNumber": "1234567890",
  "fileName": "cni-recto.jpg",
  "fileSize": 245113,
  "mimeType": "image/jpeg",
  "previewUrl": "https://.../preview",
  "downloadUrl": "https://.../download",
  "requiresBackSide": true,
  "backDocumentCode": "KYD-202607-00000002",
  "backFileName": "cni-verso.jpg",
  "backFileSize": 240000,
  "backMimeType": "image/jpeg",
  "backPreviewUrl": "https://.../preview",
  "backDownloadUrl": "https://.../download",
  "issueDate": "2020-01-15",
  "expiryDate": "2030-01-15",
  "status": "PENDING",
  "uploadedBy": 42,
  "uploadedByEmail": "agent@elleaose.com",
  "uploadedAt": "2026-07-05T14:00:00Z"
}
```

### 5.1 Approuver plusieurs documents

```http
POST /kyc/cases/{code}/documents/bulk-approve
```
```json
{ "documentCodes": ["KYD-...01", "KYD-...02"], "reviewedBy": 42 }
```

### 5.2 Rejeter plusieurs documents — **motif par document**

```http
POST /kyc/cases/{code}/documents/bulk-reject
```
```json
{
  "items": [
    { "documentCode": "KYD-...03", "reason": "Document illisible" },
    { "documentCode": "KYD-...04", "reason": "Date d'expiration dépassée" }
  ],
  "reviewedBy": 42
}
```
> Contrairement à la revue en masse GED (un seul motif pour tout le lot), le KYC permet **un motif distinct par document**.

**Réponse** (approve/reject) → `KycBulkActionResponse` :
```json
{
  "processed": 2,
  "succeeded": 2,
  "failed": 0,
  "results": [
    { "documentCode": "KYD-...03", "success": true, "error": null },
    { "documentCode": "KYD-...04", "success": true, "error": null }
  ]
}
```

### 5.3 Demander correction d'un document précis

```http
POST /kyc/cases/{code}/documents/request-correction
```
```json
{ "documentCode": "KYD-...05", "reviewedBy": 42, "correctionNote": "Photo floue, merci de rescanner", "deadlineDays": 7 }
```
**Réponse** → `KycDocumentResponse` mis à jour.

---

## 6. Notes, timeline, expiration, export PDF

```http
GET  /kyc/cases/{code}/notes            → List<KycCaseNoteResponse>
POST /kyc/cases/{code}/notes            → KycCaseNoteResponse (201)
```
```json
{ "content": "Client contacté par téléphone", "authorId": 42, "internal": true }
```
`content` et `authorId` obligatoires ; `internal` défaut `true` (note interne non visible du client).

`KycCaseNoteResponse` :
```json
{ "id": 88, "kycCaseCode": "KYC-...07", "content": "Client contacté par téléphone", "authorId": 42, "authorEmail": "agent@elleaose.com", "createdAt": "2026-07-06T10:00:00Z", "internal": true }
```

```http
GET /kyc/cases/{code}/timeline          → List<KycTimelineEntryResponse>
```
```json
[
  { "timestamp": "2026-07-01T09:00:00Z", "action": "CASE_CREATED", "actor": "agent@elleaose.com", "description": "Dossier créé" },
  { "timestamp": "2026-07-05T14:00:00Z", "action": "SUBMITTED", "actor": "jean.kabila", "description": "Dossier soumis" }
]
```

```http
GET /kyc/cases/{code}/expiry-status     → List<KycExpiryDocumentStatus>
```
```json
[
  { "documentCode": "KYD-...01", "documentType": "CNI", "expiryDate": "2030-01-15", "daysUntilExpiry": 1286, "expired": false, "status": "VERIFIED" }
]
```

```http
GET /kyc/cases/{code}/export/pdf?includeInternalNotes=true    → fichier PDF
```
`includeInternalNotes` défaut `true`. Renvoie des **octets bruts** (`application/pdf`, `Content-Disposition: attachment`), pas du JSON.

---

## 7. Tableau de bord & files

```http
GET /kyc/cases/dashboard                             → KycDashboardResponse
GET /kyc/cases/expiring-soon?days=30&page=0&size=20   → PaginatedResponse<KycCaseResponse>
GET /kyc/cases/my-queue?userId=42&page=0&size=20      → PaginatedResponse<KycCaseResponse>
```
`days` défaut `30` ; `userId` optionnel (défaut = utilisateur connecté).

`KycDashboardResponse` :
```json
{
  "generatedAt": "2026-07-09T12:00:00Z",
  "summary": { "TOTAL": 320, "SUBMITTED": 40, "UNDER_REVIEW": 25, "APPROVED": 240, "REJECTED": 15 },
  "sla": { "casesReviewedWithin24h": 180, "casesExceeding48h": 6, "avgReviewTimeHours": 8.5 },
  "expiringSoon": { "within7Days": 3, "within30Days": 12, "within60Days": 25 },
  "byOwnerType": [ { "ownerType": "CUSTOMER", "count": 200, "pendingReview": 15 }, { "ownerType": "MEMBER", "count": 120, "pendingReview": 10 } ],
  "recentActivity": [ { "action": "APPROVED", "count": 12, "period": "2026-07-09" } ]
}
```

---

## Enums

- **KycCaseStatus** : `NOT_STARTED` (créé, aucune activité) → `IN_PROGRESS` (upload en cours) → `SUBMITTED` (soumis) → `UNDER_REVIEW` (pris en charge) → `APPROVED` / `REJECTED` / `PENDING_CORRECTION` (correction demandée) / `RENEWAL_REQUIRED` (document expiré sur dossier déjà approuvé) / `EXPIRED`
- **KycRiskLevel** : `LOW, MEDIUM, HIGH, VERY_HIGH`
- **KycDocumentVerificationStatus** : `PENDING, VERIFIED, REJECTED, EXPIRED`

## Automatisations (aucune action frontend requise)

- OCR par document (résultat consultable via [kyc-document-review-admin.md](./kyc-document-review-admin.md)).
- Rappels d'expiration hebdomadaires + expiration automatique après la période de grâce.
- Relances des dossiers incomplets (job quotidien) — reflétées dans `reminderCount`/`lastReminderSentAt`.
- Auto-approbation de certains types de documents après un délai configuré (`autoApproveAfterDays` sur le type de document).
