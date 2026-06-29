# GED & KYC — API Frontend Admin

> Base URL: `/sni/api/v1`
> Auth: Bearer JWT requis sur tous les endpoints.
> Content-Type: `application/json` sauf upload → `multipart/form-data`.

---

## Table des matières

### PARTIE 1 — GED (Gestion Electronique de Documents)

1. [Patterns communs](#1-patterns-communs)
2. [Enums de référence](#2-enums-de-référence)
3. [Types de documents](#3-types-de-documents)
4. [Exigences documentaires (globales)](#4-exigences-documentaires-globales)
5. [Documents — CRUD & Lifecycle](#5-documents--crud--lifecycle)
6. [Tags & Métadonnées](#6-tags--métadonnées)
7. [Signatures](#7-signatures)
8. [File de revue (Review Queue)](#8-file-de-revue-review-queue)
9. [Dashboard, Analytics & Export](#9-dashboard-analytics--export)
10. [Journaux d'accès](#10-journaux-daccès)
11. [Politiques de rétention](#11-politiques-de-rétention)
12. [Espaces documentaires — Upload & Gestion par espace](#12-espaces-documentaires--upload--gestion-par-espace)
13. [Dossiers & Arborescence](#13-dossiers--arborescence)
14. [Upload Multiple & Import ZIP](#14-upload-multiple--import-zip)
15. [Workflow documentaire](#15-workflow-documentaire)
16. [Partage documentaire](#16-partage-documentaire)
17. [Permissions granulaires](#17-permissions-granulaires)
18. [Verrouillage (Lock/Unlock)](#18-verrouillage-lockunlock)

### PARTIE 2 — KYC (Know Your Customer)

12. [Dossiers KYC — Cycle de vie](#12-dossiers-kyc--cycle-de-vie)
13. [Requirements par dossier](#13-requirements-par-dossier)
14. [Documents recto/verso (Front/Back)](#14-documents-rectoverso-frontback)
15. [Revue de documents dans un dossier](#15-revue-de-documents-dans-un-dossier)
16. [Revue globale de documents KYC](#16-revue-globale-de-documents-kyc)
17. [Règles de validation croisée](#17-règles-de-validation-croisée)
18. [Notes & Timeline](#18-notes--timeline)
19. [Dashboard KYC](#19-dashboard-kyc)
20. [OCR & Extraction automatique](#20-ocr--extraction-automatique)
21. [Export PDF](#21-export-pdf)

### PARTIE 3 — Intégration

22. [Flows d'utilisation recommandés](#22-flows-dutilisation-recommandés)
23. [Recommandations d'écrans](#23-recommandations-décrans)
24. [Permissions RBAC](#24-permissions-rbac)
25. [Codes d'erreur](#25-codes-derreur)

---

# PARTIE 1 — GED (Gestion Electronique de Documents)

---

## 1. Patterns communs

### Réponse paginée

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

| Param    | Type    | Défaut    | Description                                 |
|----------|---------|-----------|---------------------------------------------|
| `page`   | int     | `0`       | Numéro de page (0-indexed)                  |
| `size`   | int     | `20`      | Éléments par page                           |
| `sort`   | string  | variable  | Champ de tri, ex: `uploadedAt,desc`         |

### Résolution automatique (backend → frontend)

Le backend résout automatiquement les IDs en informations lisibles dans toutes les réponses :

| Champ envoyé (ID) | Champ résolu (retourné) | Source |
|---|---|---|
| `reviewedBy` (Long) | `reviewedByEmail` (String) | Table `users` |
| `assignedTo` (Long) | `assignedToEmail` (String) | Table `users` |
| `authorId` (Long) | `authorEmail` (String) | Table `users` |
| `createdBy` (Long) | `createdByEmail` (String) | Table `users` |
| `ownerId` (Long) | `ownerName` (String) | Member/Customer/Business |
| `granteeId` (Long) | `granteeEmail` (String) | Table `users` |
| `grantedBy` (Long) | `grantedByEmail` (String) | Table `users` |

**Le frontend ne devrait jamais afficher un ID numérique brut** — utiliser les champs résolus.

### Saisie assistée — NE PAS saisir les codes manuellement

Pour tous les champs qui demandent un **code** (`ownerCode`, `documentTypeCode`, `folderCode`, `workflowCode`, etc.), le frontend doit utiliser les endpoints de lookup pour proposer un sélecteur / autocomplete :

| Champ à remplir | Endpoint de lookup | Afficher | Envoyer |
|---|---|---|---|
| `ownerCode` (MEMBER) | `GET /v1/members?search=...` | Nom du membre | `memberId` |
| `ownerCode` (CUSTOMER) | `GET /v1/customers?search=...` | Nom du client | `customerId` |
| `ownerCode` (BUSINESS) | `GET /v1/businesses?search=...` | Nom de l'entreprise | `code` |
| `documentTypeCode` | `GET /v1/document-types?ownerType=...` | Nom du type | `code` |
| `folderCode` | `GET /v1/document-folders/roots` ou `/{code}/children` | Nom du dossier | `code` |
| `workflowCode` | `GET /v1/document-workflows?activeOnly=true` | Nom du workflow | `code` |
| `tagCodes` | `GET /v1/document-tags?space=...` | Label du tag | `code` |
| `assignedTo` / `email` | `GET /v1/admin/users` (recherche par email) | Email | `email` (le backend résout l'ID) |
| `ownerType` | Dropdown statique | Label enum | Valeur enum |
| `space` | Dropdown statique | Label enum | Valeur enum |

**Règle générale** : si un champ se termine par `Code`, `Type`, ou est un ID utilisateur → proposer un sélecteur, pas un champ texte libre.

### Idempotence

Les endpoints marqués **Idempotent** acceptent le header `X-Idempotency-Key` (UUID). Le serveur garantit qu'un même appel avec la même clé ne produira pas de doublon.

### Audit

Les endpoints marqués **Audité** génèrent une entrée dans le journal d'audit avec module, action et ressource.

---

## 2. Enums de référence

### DocumentStatus

| Valeur             | Description                                      |
|--------------------|--------------------------------------------------|
| `DRAFT`            | Brouillon, pas encore uploadé                    |
| `UPLOADED`         | Fichier uploadé, en attente                      |
| `PENDING_REVIEW`   | Soumis pour revue                                |
| `NEEDS_CORRECTION` | Correction demandée par le reviewer              |
| `APPROVED`         | Approuvé                                         |
| `REJECTED`         | Rejeté définitivement                            |
| `SIGNED`           | Signé électroniquement                           |
| `EXPIRED`          | Date d'expiration dépassée                       |
| `ARCHIVED`         | Archivé (rétention)                              |
| `SUPERSEDED`       | Remplacé par une version plus récente            |

### DocumentOwnerType

| Valeur      | Description                   |
|-------------|-------------------------------|
| `MEMBER`    | Membre coworking              |
| `CUSTOMER`  | Client                        |
| `BUSINESS`  | Entreprise                    |
| `CONTRACT`  | Contrat                       |
| `INVOICE`   | Facture                       |
| `PAYMENT`   | Paiement                      |
| `PROPOSAL`  | Devis                         |
| `ASSET`     | Actif / ressource             |

### DocumentSpace

| Valeur             | Description                          |
|--------------------|--------------------------------------|
| `KYC_SPACE`        | Documents KYC                        |
| `CONTRACT_SPACE`   | Documents contractuels               |
| `FINANCIAL_SPACE`  | Documents financiers                 |
| `ASSET_SPACE`      | Documents d'actifs                   |
| `ADMINISTRATIVE`   | Documents administratifs             |
| `GENERIC`          | Espace par défaut                    |

### DocumentCategory

| Valeur      | Description             |
|-------------|-------------------------|
| `KYC`       | Know Your Customer      |
| `LEGAL`     | Juridique               |
| `SYSTEM`    | Système                 |
| `FINANCIAL` | Financier               |
| `ASSET`     | Actif                   |
| `OTHER`     | Autre                   |

### DocumentSignatureStatus

`PENDING` · `SIGNED` · `DECLINED` · `EXPIRED`

### DocumentVersionUploadStatus

`PENDING_SCAN` · `READY` · `QUARANTINED` · `REJECTED` · `DELETED`

### DocumentAntivirusStatus

`PENDING` · `CLEAN` · `INFECTED` · `ERROR`

### DocumentRetentionAction

`ARCHIVE` · `PURGE_FLAG`

### DocumentRetentionReference

`UPLOAD_DATE` · `EXPIRY_DATE`

---

## 3. Types de documents

> Base: `/v1/document-types`

### Lister les types

```
GET /v1/document-types
```

| Param       | Type              | Requis | Description                        |
|-------------|-------------------|--------|------------------------------------|
| `ownerType` | DocumentOwnerType | Non    | Filtrer par type de propriétaire   |
| `active`    | Boolean           | Non    | Filtrer par statut actif/inactif   |

**Réponse** : `List<DocumentTypeResponse>`

```json
{
  "code": "MEMBER_ID_CARD",
  "name": "Carte d'identité membre",
  "category": "KYC",
  "ownerType": "MEMBER",
  "description": "Carte nationale d'identité",
  "helpText": "Veuillez fournir une copie lisible",
  "documentDetails": null,
  "required": true,
  "requiresExpiryDate": true,
  "requiresReview": true,
  "requiresSignature": false,
  "multipleAllowed": false,
  "requiresBackSide": true,
  "allowedMimeTypes": "application/pdf,image/jpeg,image/png,image/webp",
  "maxFileSizeBytes": 10485760,
  "active": true
}
```

### Obtenir un type par code

```
GET /v1/document-types/{code}
```

### Créer un type — Audité, Idempotent

```
POST /v1/document-types
```

**Body** : `DocumentTypeRequest`

```json
{
  "code": "MEMBER_PASSPORT",
  "name": "Passeport membre",
  "category": "KYC",
  "ownerType": "MEMBER",
  "description": "Passeport en cours de validité",
  "helpText": "Page d'identité uniquement",
  "required": true,
  "requiresExpiryDate": true,
  "requiresReview": true,
  "requiresSignature": false,
  "multipleAllowed": false,
  "requiresBackSide": false,
  "allowedMimeTypes": "application/pdf,image/jpeg,image/png",
  "maxFileSizeBytes": 10485760,
  "active": true
}
```

### Modifier un type — Audité, Idempotent

```
PATCH /v1/document-types/{code}
```

Body identique à la création (champs partiels acceptés).

### Activer / Désactiver — Audité

```
PATCH /v1/document-types/{code}/activate
PATCH /v1/document-types/{code}/deactivate
```

### Supprimer — Audité

```
DELETE /v1/document-types/{code}
```

---

## 4. Exigences documentaires (globales)

> Base: `/v1/document-requirements`

Les exigences globales définissent quels types de documents sont requis par défaut pour un `ownerType` donné. Elles s'appliquent à tous les dossiers KYC qui n'ont pas de requirements spécifiques.

### Lister

```
GET /v1/document-requirements
```

| Param       | Type              | Requis | Description                  |
|-------------|-------------------|--------|------------------------------|
| `ownerType` | DocumentOwnerType | Non    | Filtrer par propriétaire     |
| `active`    | Boolean           | Non    | Filtrer actif/inactif        |

**Réponse** : `List<DocumentRequirementResponse>`

```json
{
  "id": 1001,
  "ownerType": "MEMBER",
  "documentTypeCode": "MEMBER_ID_CARD",
  "documentTypeName": "Carte d'identité membre",
  "customerType": null,
  "businessLegalForm": null,
  "required": true,
  "active": true
}
```

### Créer — Audité, Idempotent

```
POST /v1/document-requirements
```

**Body** : `DocumentRequirementRequest`

```json
{
  "ownerType": "MEMBER",
  "documentTypeCode": "MEMBER_ID_CARD",
  "documentTypeName": "Carte d'identité membre",
  "customerType": null,
  "businessLegalForm": null,
  "required": true,
  "active": true
}
```

### Modifier — Audité, Idempotent

```
PATCH /v1/document-requirements/{id}
```

### Obtenir par ID

```
GET /v1/document-requirements/{id}
```

### Activer / Désactiver — Audité

```
PATCH /v1/document-requirements/{id}/activate
PATCH /v1/document-requirements/{id}/deactivate
```

### Supprimer — Audité

```
DELETE /v1/document-requirements/{id}
```

---

## 5. Documents — CRUD & Lifecycle

> Base: `/v1/documents`

### Upload — Audité, Idempotent

```
POST /v1/documents/upload
Content-Type: multipart/form-data
```

| Part       | Type                          | Description                  |
|------------|-------------------------------|------------------------------|
| `file`     | fichier binaire               | Le document à uploader       |
| `metadata` | JSON (form field)             | Métadonnées du document      |

**Metadata** (`DocumentUploadMetadataRequest`) :

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-202604-000012",
  "documentTypeCode": "MEMBER_ID_CARD",
  "title": "CNI — MBR-202604-000012",
  "description": "Carte nationale d'identité",
  "documentNumber": "AB1234567",
  "issueDate": "2023-01-15",
  "expiryDate": "2033-01-15"
}
```

> **Saisie assistée :**
> - `ownerType` → dropdown (`MEMBER`, `CUSTOMER`, `BUSINESS`, ...)
> - `ownerCode` → autocomplete via `GET /v1/members?search=` ou `/v1/customers?search=` selon ownerType sélectionné
> - `documentTypeCode` → select via `GET /v1/document-types?ownerType=MEMBER` — afficher `name`, envoyer `code`
>
> Le `title` peut être auto-généré : `{documentTypeName} — {ownerName}`

```
```

**Réponse** : `DocumentResponse`

```json
{
  "code": "DOC-202606-000042",
  "ownerId": 15,
  "ownerType": "MEMBER",
  "category": "KYC",
  "space": "KYC_SPACE",
  "documentTypeCode": "MEMBER_ID_CARD",
  "documentTypeName": "Carte d'identité membre",
  "title": "CNI — MBR-202604-000012",
  "fileName": "cni_recto.jpg",
  "fileSize": 2048576,
  "mimeType": "image/jpeg",
  "checksumSha256": "a1b2c3...",
  "currentVersionNumber": 1,
  "status": "PENDING_REVIEW",
  "issueDate": "2023-01-15",
  "expiryDate": "2033-01-15",
  "uploadedBy": 5,
  "uploadedAt": "2026-06-20T10:30:00Z",
  "updatedAt": "2026-06-20T10:30:00Z",
  "previewUrl": "/sni/api/v1/documents/DOC-202606-000042/preview",
  "downloadUrl": "/sni/api/v1/documents/DOC-202606-000042/download",
  "versions": [...],
  "tags": [],
  "metadata": {}
}
```

### Remplacer (nouvelle version) — Audité, Idempotent

```
POST /v1/documents/{code}/replace
Content-Type: multipart/form-data
```

| Part   | Type    | Description         |
|--------|---------|---------------------|
| `file` | fichier | Nouvelle version    |

### Obtenir un document

```
GET /v1/documents/{code}
```

### Lister les documents

```
GET /v1/documents
```

| Param       | Type              | Requis | Défaut             |
|-------------|-------------------|--------|--------------------|
| `ownerType` | DocumentOwnerType | Non    | —                  |
| `ownerCode` | String            | Non    | —                  |
| `page`      | int               | Non    | 0                  |
| `size`      | int               | Non    | 20                 |
| `sort`      | String            | Non    | `uploadedAt,desc`  |

### Lister les versions

```
GET /v1/documents/{code}/versions
```

**Réponse** : `List<DocumentVersionResponse>`

```json
{
  "versionNumber": 2,
  "originalFileName": "cni_v2.jpg",
  "mimeTypeDetected": "image/jpeg",
  "fileSizeBytes": 1048576,
  "uploadStatus": "READY",
  "antivirusStatus": "CLEAN",
  "uploadedBy": 5,
  "uploadedAt": "2026-06-20T14:00:00Z",
  "current": true
}
```

### Télécharger / Prévisualiser

```
GET /v1/documents/{code}/download    → fichier binaire (attachment)
GET /v1/documents/{code}/preview     → fichier binaire (inline)
```

Ces endpoints génèrent un log d'accès (action: `DOWNLOAD` / `PREVIEW`).

### Approuver — Audité, Idempotent — Permission: `DOCUMENT:REVIEW`

```
POST /v1/documents/{code}/approve
```

**Body** : `DocumentReviewDecisionRequest`

```json
{
  "reviewedBy": 5,
  "comment": "Document conforme"
}
```

### Rejeter — Audité, Idempotent — Permission: `DOCUMENT:REVIEW`

```
POST /v1/documents/{code}/reject
```

```json
{
  "reviewedBy": 5,
  "comment": "Photo floue, illisible",
  "rejectionReasonCode": "POOR_QUALITY",
  "rejectionReasonDetail": "Texte illisible sur le document"
}
```

### Demander une correction — Audité — Permission: `DOCUMENT:REVIEW`

```
POST /v1/documents/{code}/request-correction
```

**Body** : `DocumentCorrectionRequest`

```json
{
  "reviewedBy": 5,
  "correctionNote": "Le numéro du document est manquant, veuillez resoumettre",
  "comment": "Correction requise",
  "deadlineDays": 7
}
```

### Restaurer une version — Audité — Permission: `DOCUMENT:REVIEW`

```
POST /v1/documents/{code}/versions/{versionNumber}/restore
```

### Archiver — Audité

```
POST /v1/documents/{code}/archive?reason=expiration
```

### Recherche avancée

```
GET /v1/documents/search
```

| Param           | Type                   | Description                         |
|-----------------|------------------------|-------------------------------------|
| `q`             | String                 | Texte libre                         |
| `space`         | DocumentSpace          | Espace documentaire                 |
| `category`      | DocumentCategory       | Catégorie                           |
| `tags`          | List\<String\>         | Codes de tags                       |
| `ownerType`     | DocumentOwnerType      | Type propriétaire                   |
| `ownerCode`     | String                 | Code propriétaire                   |
| `statuses`      | List\<DocumentStatus\> | Statuts                             |
| `expiresInDays` | Integer                | Expire dans N jours                 |
| `isExpired`     | Boolean                | Documents expirés                   |
| `uploadedAfter` | LocalDate              | Uploadé après (ISO)                 |
| `uploadedBefore`| LocalDate              | Uploadé avant (ISO)                 |
| `metaKey`       | String                 | Clé métadonnée                      |
| `metaValue`     | String                 | Valeur métadonnée                   |

### Vue dossier propriétaire

```
GET /v1/documents/folder?ownerType=MEMBER&ownerCode=MBR-202604-000012
```

**Réponse** : `DocumentFolderResponse` — documents groupés par espace avec les requirements manquants.

---

## 6. Tags & Métadonnées

### Tags

**Créer un tag**
```
POST /v1/document-tags
```
```json
{
  "code": "urgent",
  "label": "Urgent",
  "color": "#FF0000",
  "space": "KYC_SPACE"
}
```

**Modifier un tag**
```
PUT /v1/document-tags/{code}
```
```json
{ "label": "Très urgent", "color": "#CC0000" }
```

**Obtenir / Lister / Supprimer**
```
GET /v1/document-tags/{code}
GET /v1/document-tags?space=KYC_SPACE
DELETE /v1/document-tags/{code}
```

**Assigner des tags à un document**
```
POST /v1/documents/{code}/tags
```
```json
{ "tagCodes": ["urgent", "kyc-priority"] }
```

**Retirer un tag d'un document**
```
DELETE /v1/documents/{code}/tags/{tagCode}
```

**Lister les tags d'un document**
```
GET /v1/documents/{code}/tags
```

### Métadonnées clé-valeur

**Ajouter / Modifier des métadonnées**
```
PATCH /v1/documents/{code}/metadata
```
```json
{ "source": "scanner-bureau", "operator": "agent-12" }
```

**Lister les métadonnées**
```
GET /v1/documents/{code}/metadata
```

**Supprimer une clé**
```
DELETE /v1/documents/{code}/metadata/{key}
```

---

## 7. Signatures

> Base: `/v1/documents/{documentCode}/signatures`

### Créer une demande de signature

```
POST /v1/documents/{documentCode}/signatures
```

```json
{
  "signerType": "USER",
  "signerId": 15,
  "signerName": "Jean Bokati",
  "signerEmail": "jean@bokati.com"
}
```

### Signer un document

```
PATCH /v1/documents/{documentCode}/signatures/{signatureId}/sign
```

```json
{
  "accepted": true,
  "signatureData": "base64-encoded-signature-image..."
}
```

### Lister les signatures

```
GET /v1/documents/{documentCode}/signatures
```

**Réponse** : `List<DocumentSignatureResponse>`

```json
{
  "id": 100,
  "documentCode": "DOC-202606-000042",
  "signerType": "USER",
  "signerId": 15,
  "signerName": "Jean Bokati",
  "signerEmail": "jean@bokati.com",
  "signatureStatus": "SIGNED",
  "signedAt": "2026-06-20T15:00:00Z",
  "ipAddress": "192.168.1.10",
  "userAgent": "Mozilla/5.0..."
}
```

---

## 8. File de revue (Review Queue)

> Base: `/v1/review`
> Permission: `DOCUMENT:REVIEW`

### File de revue globale

```
GET /v1/review/queue
```

Paginé. Retourne tous les documents en `PENDING_REVIEW`.

### File de revue par espace

```
GET /v1/review/{space}
```

Ex: `GET /v1/review/KYC_SPACE`

### Approbation en masse par espace — Audité

```
POST /v1/review/{space}/bulk-approve
```

```json
{
  "codes": ["DOC-202606-000042", "DOC-202606-000043"],
  "reviewedBy": 5,
  "comment": "Lot approuvé"
}
```

**Réponse** : `DocumentBulkActionResponse`

```json
{
  "total": 2,
  "succeeded": 2,
  "failed": 0,
  "results": [
    { "code": "DOC-202606-000042", "status": "APPROVED", "reason": null },
    { "code": "DOC-202606-000043", "status": "APPROVED", "reason": null }
  ]
}
```

### Rejet en masse par espace — Audité

```
POST /v1/review/{space}/bulk-reject
```

```json
{
  "codes": ["DOC-202606-000044"],
  "reviewedBy": 5,
  "rejectionReasonCode": "POOR_QUALITY",
  "rejectionReasonDetail": "Documents illisibles",
  "comment": "Rejet qualité"
}
```

### Correction en masse par espace — Audité

```
POST /v1/review/{space}/bulk-request-correction
```

```json
{
  "codes": ["DOC-202606-000045"],
  "reviewedBy": 5,
  "correctionNote": "Veuillez fournir la page 2",
  "deadlineDays": 5
}
```

---

## 9. Dashboard, Analytics & Export

### Dashboard documents

```
GET /v1/documents/dashboard?space=KYC_SPACE
```

**Réponse** : `DocumentDashboardResponse`

```json
{
  "space": "KYC_SPACE",
  "total": 150,
  "byStatus": { "APPROVED": 80, "PENDING_REVIEW": 30, "NEEDS_CORRECTION": 10, ... },
  "byCategory": { "KYC": 120, "LEGAL": 30 },
  "pendingReviewOlderThan48h": 5,
  "needsCorrectionCount": 10,
  "expiringIn30Days": 8,
  "rejectionRate30d": 0.12,
  "avgReviewTimeHours": 4.5,
  "topTags": [{ "code": "urgent", "label": "Urgent", "count": 15 }]
}
```

### Analytics — Permission: `DOCUMENT:REVIEW`

```
GET /v1/documents/analytics?space=KYC_SPACE&months=12
```

**Réponse** : `DocumentAnalyticsResponse` — tendances mensuelles, taux de correction/rejet, temps moyen de revue.

### Export CSV — Permission: `DOCUMENT:REVIEW`

```
GET /v1/documents/export/csv?space=KYC_SPACE&statuses=APPROVED,REJECTED
```

Retourne un fichier CSV avec les filtres appliqués.

---

## 10. Journaux d'accès

> Permission: `DOCUMENT:REVIEW`

### Par document

```
GET /v1/documents/{code}/access-logs
```

### Par utilisateur

```
GET /v1/documents/access-logs/by-user?userId=5
```

**Réponse paginée** : `DocumentAccessLogResponse`

```json
{
  "id": 500,
  "documentCode": "DOC-202606-000042",
  "userId": 5,
  "action": "DOWNLOAD",
  "accessedAt": "2026-06-20T14:30:00Z",
  "ipAddress": "192.168.1.10",
  "userAgent": "Mozilla/5.0..."
}
```

---

## 11. Politiques de rétention

> Base: `/v1/document-retention-policies`
> Permission: `DOCUMENT:REVIEW` (tous les endpoints)

### Lister

```
GET /v1/document-retention-policies
```

### Obtenir par code

```
GET /v1/document-retention-policies/{code}
```

### Créer — Audité

```
POST /v1/document-retention-policies
```

```json
{
  "code": "KYC_ARCHIVE_1Y",
  "name": "Archivage KYC 1 an",
  "space": "KYC_SPACE",
  "documentTypeCode": null,
  "retentionDays": 365,
  "retentionReference": "EXPIRY_DATE",
  "action": "ARCHIVE"
}
```

**Réponse** : `DocumentRetentionPolicyResponse`

```json
{
  "id": 10,
  "code": "KYC_ARCHIVE_1Y",
  "name": "Archivage KYC 1 an",
  "space": "KYC_SPACE",
  "documentTypeCode": null,
  "retentionDays": 365,
  "retentionReference": "EXPIRY_DATE",
  "action": "ARCHIVE",
  "active": true,
  "createdAt": "2026-06-20T10:00:00Z",
  "createdBy": 1
}
```

### Modifier — Audité

```
PUT /v1/document-retention-policies/{code}
```

### Désactiver — Audité

```
DELETE /v1/document-retention-policies/{code}
```

---

## 12. Espaces documentaires — Upload & Gestion par espace

> Base: `/v1/spaces/{espace}/documents`

Chaque espace documentaire dispose de ses propres endpoints d'upload avec des **validations spécifiques** (types de documents autorisés, formats de fichiers acceptés, métadonnées contextuelles).

### Principe

L'upload via `/v1/spaces/{espace}/documents/upload` :
1. **Force l'espace** — le document est automatiquement classé dans l'espace cible
2. **Valide la catégorie** — le `documentTypeCode` doit correspondre à une catégorie compatible avec l'espace
3. **Valide le format de fichier** — chaque espace accepte des formats spécifiques
4. **Applique les tags et métadonnées** en une seule opération (optionnel)
5. **Assigne un code de référence** optionnel (`referenceCode`) pour lier le document à une entité métier

### Espaces et validations

| Espace | Path | Catégories autorisées | Formats acceptés |
|--------|------|----------------------|------------------|
| **Contrats** | `/v1/spaces/contracts/...` | `LEGAL` | PDF, DOCX, JPEG, PNG |
| **Financier** | `/v1/spaces/financial/...` | `FINANCIAL` | PDF, JPEG, PNG, XLSX, CSV |
| **Actifs** | `/v1/spaces/assets/...` | `ASSET` | PDF, JPEG, PNG, WEBP, TIFF |
| **Administratif** | `/v1/spaces/administrative/...` | `SYSTEM`, `OTHER` | Tous formats |
| **Général** | `/v1/spaces/general/...` | Toutes | Tous formats |

> L'espace **KYC** est géré par le module KYC dédié (`/v1/kyc/cases/...`).

### Request Body — `SpaceDocumentUploadRequest`

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-202604-000012",
  "documentTypeCode": "CONTRACT_SIGNED_COPY",
  "title": "Contrat location bureau B12",
  "description": "Contrat signé pour la période 2026-2027",
  "documentNumber": "CTR-2026-0042",
  "issueDate": "2026-06-15",
  "expiryDate": "2027-06-15",
  "referenceCode": "CTR-202606-000042",
  "tagCodes": ["contrat-actif", "bureau-b12"],
  "metadata": {
    "contractType": "LOCATION",
    "montantMensuel": "150000"
  }
}
```

| Champ            | Type               | Requis | Description |
|------------------|--------------------|--------|-------------|
| Champ | Type | Requis | Saisie frontend |
|---|---|---|---|
| `ownerType` | DocumentOwnerType | Oui | **Dropdown** statique |
| `ownerCode` | String | Oui | **Autocomplete** via `GET /v1/members?search=` ou `/v1/customers?search=` selon ownerType |
| `documentTypeCode` | String | Oui | **Select** via `GET /v1/document-types?ownerType=X` — afficher `name`, envoyer `code` |
| `title` | String | Oui | Auto-générable : `{typeName} — {ownerName}` |
| `description` | String | Non | Champ texte libre |
| `documentNumber` | String | Non | Champ texte libre |
| `issueDate` | LocalDate | Non | Date picker |
| `expiryDate` | LocalDate | Non | Date picker |
| `referenceCode` | String | Non | **Autocomplete** selon espace (contrats → search contrats, factures → search factures) |
| `folderCode` | String | Non | **Tree picker** via `GET /v1/document-folders/roots?space=X` puis `/{code}/children` |
| `tagCodes` | List\<String\> | Non | **Multi-select** via `GET /v1/document-tags?space=X` |
| `metadata` | Map\<String,String\> | Non | Formulaire clé-valeur dynamique |

### Endpoints par espace

#### Espace Contrats — Audité, Idempotent

```
POST /v1/spaces/contracts/documents/upload     Upload d'un document contractuel
GET  /v1/spaces/contracts/documents            Lister les documents contractuels
GET  /v1/spaces/contracts/dashboard            Dashboard espace contrats
```

**Upload :**
```
POST /v1/spaces/contracts/documents/upload
Content-Type: multipart/form-data
Parts: file + metadata (SpaceDocumentUploadRequest)
```

**Lister :**
```
GET /v1/spaces/contracts/documents?ownerType=MEMBER&ownerCode=MBR-...&statuses=APPROVED&q=bureau
```

| Param       | Type               | Requis | Description |
|-------------|--------------------|--------|-------------|
| `ownerType` | DocumentOwnerType  | Non    | Filtrer par propriétaire |
| `ownerCode` | String             | Non    | Code propriétaire |
| `statuses`  | List\<DocumentStatus\> | Non | Filtrer par statut |
| `q`         | String             | Non    | Recherche texte |

#### Espace Financier — Audité, Idempotent

```
POST /v1/spaces/financial/documents/upload     Upload d'un document financier
GET  /v1/spaces/financial/documents            Lister (filtres + dates)
GET  /v1/spaces/financial/dashboard            Dashboard espace financier
```

L'espace financier accepte en plus les filtres par dates :

| Param           | Type      | Description |
|-----------------|-----------|-------------|
| `uploadedAfter` | LocalDate | Documents uploadés après cette date |
| `uploadedBefore`| LocalDate | Documents uploadés avant cette date |

#### Espace Actifs — Audité, Idempotent

```
POST /v1/spaces/assets/documents/upload        Upload d'un document d'actif
GET  /v1/spaces/assets/documents               Lister les documents d'actifs
GET  /v1/spaces/assets/dashboard               Dashboard espace actifs
```

#### Espace Administratif — Audité, Idempotent

```
POST /v1/spaces/administrative/documents/upload  Upload document administratif
GET  /v1/spaces/administrative/documents          Lister
GET  /v1/spaces/administrative/dashboard          Dashboard
```

#### Espace Général — Audité, Idempotent

```
POST /v1/spaces/general/documents/upload       Upload document général
GET  /v1/spaces/general/documents              Lister
GET  /v1/spaces/general/dashboard              Dashboard
```

### Réponse

Tous les endpoints d'upload retournent un `DocumentResponse` complet avec l'espace, les tags et les métadonnées déjà appliqués :

```json
{
  "code": "DOC-202606-000055",
  "ownerType": "MEMBER",
  "category": "LEGAL",
  "space": "CONTRACT_SPACE",
  "spaceReferenceCode": "CTR-202606-000042",
  "documentTypeCode": "CONTRACT_SIGNED_COPY",
  "title": "Contrat location bureau B12",
  "status": "PENDING_REVIEW",
  "tags": [
    { "code": "contrat-actif", "label": "Contrat actif", "color": "#00AA00" }
  ],
  "metadata": {
    "contractType": "LOCATION",
    "montantMensuel": "150000"
  },
  "...": "..."
}
```

### Flow recommandé par espace

#### Contrats

```
1. POST /v1/spaces/contracts/documents/upload
   → Upload du contrat signé avec referenceCode = code du contrat

2. POST /v1/documents/{code}/approve
   → Validation du document par un reviewer

3. GET /v1/spaces/contracts/documents?ownerCode=MBR-...
   → Vue des contrats d'un membre

4. GET /v1/spaces/contracts/dashboard
   → Vue globale : contrats en attente, expirés, etc.
```

#### Financier (factures, reçus)

```
1. POST /v1/spaces/financial/documents/upload
   → Upload facture avec referenceCode = code facture, metadata = {montant, devise}

2. GET /v1/spaces/financial/documents?uploadedAfter=2026-01-01&uploadedBefore=2026-06-30
   → Documents financiers du semestre

3. GET /v1/spaces/financial/dashboard
   → Compteurs par statut, documents expirant
```

#### Actifs (photos, fiches techniques)

```
1. POST /v1/spaces/assets/documents/upload
   → Photo ou fiche d'un actif avec referenceCode = code de la ressource

2. GET /v1/spaces/assets/documents?ownerType=ASSET&ownerCode=RSC-...
   → Documents liés à une ressource spécifique
```

---

## 13. Dossiers & Arborescence

> Base: `/v1/document-folders`

Le système de dossiers permet d'organiser les documents en arborescence hiérarchique (jusqu'à 10 niveaux). Chaque dossier appartient à un espace documentaire et peut optionnellement être lié à un propriétaire.

### Créer un dossier — Audité

```
POST /v1/document-folders
```

```json
{
  "name": "Contrats 2026",
  "description": "Tous les contrats de l'année 2026",
  "parentCode": null,
  "space": "CONTRACT_SPACE",
  "ownerType": "BUSINESS",
  "ownerCode": "BUS-001",
  "color": "#3366FF",
  "icon": "folder-contract"
}
```

| Champ | Type | Requis | Saisie frontend |
|---|---|---|---|
| `name` | String | Oui | Champ texte (unique dans le parent) |
| `description` | String | Non | Champ texte libre |
| `parentCode` | String | Non | **Tree picker** via `GET /v1/document-folders/roots` puis navigation enfants. Null = racine |
| `space` | DocumentSpace | Non | **Dropdown** statique. Hérité du parent si sous-dossier |
| `ownerType` | DocumentOwnerType | Non | **Dropdown** statique |
| `ownerCode` | String | Non | **Autocomplete** via search members/customers/businesses selon ownerType |
| `color` | String | Non | Color picker hex |
| `icon` | String | Non | Icon picker |

**Réponse** : `FolderDetailResponse`

```json
{
  "code": "FLD-202606-000001",
  "name": "Contrats 2026",
  "description": "Tous les contrats de l'année 2026",
  "parentCode": null,
  "space": "CONTRACT_SPACE",
  "ownerType": "BUSINESS",
  "ownerId": 5,
  "ownerName": "Bokati Cowork SAS",
  "path": "/42/",
  "depth": 0,
  "sortOrder": 0,
  "color": "#3366FF",
  "icon": "folder-contract",
  "childrenCount": 0,
  "documentsCount": 0,
  "createdBy": 1,
  "createdByEmail": "admin@bokati.com",
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:00:00Z"
}
```

### Obtenir un dossier

```
GET /v1/document-folders/{code}
```

### Modifier un dossier — Audité

```
PATCH /v1/document-folders/{code}
```

```json
{
  "name": "Contrats 2026 (archivés)",
  "color": "#999999",
  "sortOrder": 10
}
```

### Supprimer un dossier — Audité

```
DELETE /v1/document-folders/{code}
```

Le dossier doit être **vide** (ni sous-dossiers ni documents). Sinon erreur 400.

### Déplacer un dossier — Audité

```
POST /v1/document-folders/{code}/move
```

```json
{ "targetParentCode": "FLD-202606-000005" }
```

Passer `null` pour déplacer à la racine. Interdit de déplacer un dossier dans un de ses descendants.

### Dossiers racine

```
GET /v1/document-folders/roots?space=CONTRACT_SPACE
```

### Enfants directs

```
GET /v1/document-folders/{code}/children
```

### Arborescence complète

```
GET /v1/document-folders/{code}/tree
```

**Réponse** : `DocumentFolderTreeNode` (récursif)

```json
{
  "code": "FLD-202606-000001",
  "name": "Contrats 2026",
  "icon": "folder-contract",
  "color": "#3366FF",
  "depth": 0,
  "documentsCount": 5,
  "children": [
    {
      "code": "FLD-202606-000002",
      "name": "Baux commerciaux",
      "icon": null,
      "color": null,
      "depth": 1,
      "documentsCount": 3,
      "children": []
    },
    {
      "code": "FLD-202606-000003",
      "name": "Contrats fournisseurs",
      "icon": null,
      "color": null,
      "depth": 1,
      "documentsCount": 2,
      "children": []
    }
  ]
}
```

### Documents dans un dossier (paginé)

```
GET /v1/document-folders/{code}/documents
```

### Fil d'Ariane (Breadcrumb)

```
GET /v1/document-folders/{code}/breadcrumb
```

**Réponse** : `List<BreadcrumbItem>`

```json
[
  { "code": "FLD-202606-000001", "name": "Contrats 2026", "depth": 0 },
  { "code": "FLD-202606-000002", "name": "Baux commerciaux", "depth": 1 }
]
```

### Déplacer un document dans un dossier — Audité

```
POST /v1/document-folders/{documentCode}/move-document
```

```json
{ "folderCode": "FLD-202606-000002" }
```

Passer `null` pour retirer le document du dossier.

### Upload dans un dossier (via espaces)

Les endpoints d'upload par espace (`/v1/spaces/{espace}/documents/upload`) acceptent le champ optionnel `folderCode` pour classer le document directement dans un dossier à l'upload.

### Réponse Document enrichie

Le `DocumentResponse` inclut maintenant le dossier :

```json
{
  "code": "DOC-202606-000042",
  "folderCode": "FLD-202606-000002",
  "folderName": "Baux commerciaux",
  "...": "..."
}
```

### Flow recommandé : créer une arborescence

```
1. POST /v1/document-folders
   Body: { name: "RH", space: "ADMINISTRATIVE" }
   → Crée le dossier racine

2. POST /v1/document-folders
   Body: { name: "Contrats employés", parentCode: "FLD-202606-000001" }
   → Crée un sous-dossier

3. POST /v1/document-folders
   Body: { name: "Fiches de paie", parentCode: "FLD-202606-000001" }
   → Crée un second sous-dossier

4. POST /v1/spaces/administrative/documents/upload
   Body: { ..., folderCode: "FLD-202606-000002" }
   → Upload un document directement dans le sous-dossier

5. GET /v1/document-folders/FLD-202606-000001/tree
   → Voir toute l'arborescence avec les compteurs
```

---

## 14. Upload Multiple & Import ZIP

> Base: `/v1/documents`

### Upload batch (plusieurs fichiers) — Audité

```
POST /v1/documents/upload-batch
Content-Type: multipart/form-data
```

| Part       | Type                          | Description                       |
|------------|-------------------------------|-----------------------------------|
| `files`    | fichiers binaires (multiple)  | 1 à 20 fichiers                   |
| `metadata` | form fields                   | Métadonnées communes (voir ci-dessous) |

**Metadata** (`BatchUploadMetadataRequest`) :

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-202604-000012",
  "documentTypeCode": "GENERAL_DOCUMENT",
  "space": "ADMINISTRATIVE",
  "folderCode": "FLD-202606-000001",
  "referenceCode": "IMPORT-2026-06",
  "tagCodes": ["import-batch"]
}
```

| Champ | Type | Requis | Saisie frontend |
|---|---|---|---|
| `ownerType` | DocumentOwnerType | Oui | **Dropdown** statique |
| `ownerCode` | String | Oui | **Autocomplete** via search members/customers/businesses |
| `documentTypeCode` | String | Oui | **Select** via `GET /v1/document-types?ownerType=X` |
| `space` | DocumentSpace | Non | **Dropdown** statique |
| `folderCode` | String | Non | **Tree picker** via dossiers |
| `referenceCode` | String | Non | Champ texte libre |
| `tagCodes` | List\<String\> | Non | **Multi-select** via `GET /v1/document-tags` |

**Réponse** : `DocumentBulkActionResponse`

```json
{
  "total": 3,
  "succeeded": 3,
  "failed": 0,
  "results": [
    { "code": "DOC-202606-000050", "status": "UPLOADED", "reason": "contrat_a.pdf" },
    { "code": "DOC-202606-000051", "status": "UPLOADED", "reason": "contrat_b.pdf" },
    { "code": "DOC-202606-000052", "status": "UPLOADED", "reason": "annexe.pdf" }
  ]
}
```

**Limites** : 20 fichiers max par batch. Le titre de chaque document est dérivé du nom de fichier.

### Import ZIP — Audité

```
POST /v1/documents/import-zip
Content-Type: multipart/form-data
```

| Part       | Type     | Description                         |
|------------|----------|-------------------------------------|
| `file`     | fichier  | Archive ZIP (max 100 Mo)            |
| `metadata` | form     | Métadonnées communes (même DTO)     |

Le ZIP est extrait et chaque fichier crée un document. Si le ZIP contient des **sous-dossiers**, les dossiers correspondants sont automatiquement créés dans l'arborescence GED.

**Réponse** : `ZipImportResponse`

```json
{
  "totalFiles": 5,
  "succeeded": 5,
  "failed": 0,
  "foldersCreated": 2,
  "items": [
    {
      "originalPath": "Contrats/bail_bureau_A.pdf",
      "documentCode": "DOC-202606-000053",
      "folderCode": "FLD-202606-000010",
      "success": true,
      "error": null
    },
    {
      "originalPath": "Contrats/bail_bureau_B.pdf",
      "documentCode": "DOC-202606-000054",
      "folderCode": "FLD-202606-000010",
      "success": true,
      "error": null
    },
    {
      "originalPath": "Factures/facture_juin.pdf",
      "documentCode": "DOC-202606-000055",
      "folderCode": "FLD-202606-000011",
      "success": true,
      "error": null
    }
  ]
}
```

**Limites** : 50 fichiers max, 100 Mo max pour le ZIP.

### Flow recommandé : import en masse

```
1. Préparer un ZIP avec la structure de dossiers souhaitée :
   archives/
   ├── Contrats/
   │   ├── bail_bureau_A.pdf
   │   └── bail_bureau_B.pdf
   └── Factures/
       └── facture_juin.pdf

2. POST /v1/documents/import-zip
   Parts: file=archives.zip, metadata={ownerType, ownerCode, documentTypeCode, space}

3. Le backend crée automatiquement :
   - Dossier "Contrats" → FLD-xxx-000010
   - Dossier "Factures" → FLD-xxx-000011
   - 3 documents classés dans leurs dossiers respectifs

4. GET /v1/document-folders/roots?space=ADMINISTRATIVE
   → Voir les dossiers créés

5. GET /v1/document-folders/FLD-xxx-000010/tree
   → Voir l'arborescence avec les documents
```

---

## 15. Workflow documentaire

> Base: `/v1/document-workflows` et `/v1/documents/{code}/workflow`

Circuit de validation multi-étapes pour les documents. Chaque workflow définit une séquence d'approbateurs (par email ou rôle).

### Types de workflow

| Type | Description |
|------|-------------|
| `SIMPLE` | Validation unique |
| `SEQUENTIAL` | Étapes séquentielles, une après l'autre |
| `PARALLEL` | Toutes les étapes en parallèle |
| `HIERARCHICAL` | Validation hiérarchique par niveau |

### Créer un workflow — Audité

```
POST /v1/document-workflows
```

```json
{
  "name": "Validation contrat",
  "description": "Circuit de validation des contrats",
  "workflowType": "SEQUENTIAL",
  "space": "CONTRACT_SPACE",
  "documentTypeCode": "CONTRACT_SIGNED_COPY",
  "steps": [
    { "stepName": "Vérification juridique", "approverType": "ROLE", "approverValue": "ROLE_LEGAL", "required": true },
    { "stepName": "Approbation direction", "approverType": "USER", "approverValue": "directeur@bokati.com", "required": true, "autoApproveDays": 5 }
  ]
}
```

> **Saisie assistée :**
> - `workflowType` → **Dropdown** : SIMPLE, SEQUENTIAL, PARALLEL, HIERARCHICAL
> - `space` → **Dropdown** DocumentSpace
> - `documentTypeCode` → **Select** via `GET /v1/document-types` — afficher `name`, envoyer `code`
> - `steps[].approverType` → **Dropdown** : USER, ROLE
> - `steps[].approverValue` → si USER : **Autocomplete email** via recherche users. Si ROLE : **Dropdown** des rôles disponibles

### Lister / Obtenir / Supprimer

```
GET  /v1/document-workflows?activeOnly=true
GET  /v1/document-workflows/{code}
DELETE /v1/document-workflows/{code}     (désactivation)
```

### Démarrer un workflow sur un document — Audité

```
POST /v1/documents/{documentCode}/start-workflow?workflowCode=WFL-202606-000001
```

> **Saisie assistée :** `workflowCode` → **Select** via `GET /v1/document-workflows?activeOnly=true`. Afficher `name`, envoyer `code`.

### Approuver / Rejeter l'étape courante — Audité

```
POST /v1/documents/{documentCode}/workflow/approve
POST /v1/documents/{documentCode}/workflow/reject
```

```json
{ "comment": "Document conforme, validé" }
```

### Statut du workflow

```
GET /v1/documents/{documentCode}/workflow/status
```

**Réponse** : `WorkflowInstanceResponse`

```json
{
  "id": 1,
  "workflowCode": "WFL-202606-000001",
  "workflowName": "Validation contrat",
  "documentCode": "DOC-202606-000042",
  "documentTitle": "Contrat location bureau B12",
  "currentStep": 2,
  "currentStepName": "Approbation direction",
  "status": "IN_PROGRESS",
  "startedAt": "2026-06-20T10:00:00Z",
  "completedAt": null,
  "actions": [
    {
      "stepOrder": 1,
      "stepName": "Vérification juridique",
      "action": "APPROVE",
      "actorId": 3,
      "actorEmail": "legal@bokati.com",
      "actedAt": "2026-06-20T11:00:00Z",
      "comment": "Conforme"
    }
  ]
}
```

### Mes approbations en attente

```
GET /v1/document-workflows/my-pending
```

---

## 16. Partage documentaire

> Endpoints authentifiés: `/v1/documents/{code}/share`, `/v1/document-folders/{code}/share`
> Endpoint public: `/v1/shares/{token}`

### Créer un lien de partage — Audité

```
POST /v1/documents/{documentCode}/share
POST /v1/document-folders/{folderCode}/share
```

```json
{
  "expiresInHours": 48,
  "password": "monMotDePasse123",
  "allowDownload": true,
  "maxAccessCount": 10
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `expiresInHours` | Integer | Oui | Durée de validité en heures |
| `password` | String | Non | Protection par mot de passe |
| `allowDownload` | Boolean | Non | Autoriser le téléchargement (défaut: true) |
| `maxAccessCount` | Integer | Non | Nombre max de consultations (null = illimité) |

**Réponse** : `ShareLinkResponse`

```json
{
  "token": "a1b2c3d4e5f6...",
  "shareUrl": "https://api.bokati.com/sni/api/v1/shares/a1b2c3d4e5f6...",
  "documentCode": "DOC-202606-000042",
  "folderCode": null,
  "allowDownload": true,
  "passwordProtected": true,
  "maxAccessCount": 10,
  "accessCount": 0,
  "expiresAt": "2026-06-22T10:00:00Z",
  "createdBy": 5,
  "createdByEmail": "admin@bokati.com",
  "active": true
}
```

### Accéder au lien (public, sans auth)

```
GET /v1/shares/{token}
```

### Révoquer un lien — Audité

```
DELETE /v1/shares/{token}
```

### Lister les liens d'un document

```
GET /v1/documents/{documentCode}/shares
```

---

## 17. Permissions granulaires

> Base: `/v1/document-permissions`

Contrôle d'accès fin par document ou dossier, par utilisateur ou rôle.

### Permissions disponibles

`READ` · `DOWNLOAD` · `EDIT` · `REVIEW` · `DELETE` · `ADMIN`

### Types de cibles

`DOCUMENT` · `FOLDER`

### Types de bénéficiaires

`USER` · `ROLE`

### Accorder une permission — Audité

```
POST /v1/document-permissions
```

```json
{
  "targetType": "FOLDER",
  "targetCode": "FLD-202606-000001",
  "granteeType": "USER",
  "granteeId": 8,
  "permission": "READ"
}
```

> **Saisie assistée :**
> - `targetType` → **Dropdown** : DOCUMENT, FOLDER
> - `targetCode` → Si DOCUMENT : **Autocomplete** via `GET /v1/documents/search?q=...`. Si FOLDER : **Tree picker** dossiers
> - `granteeType` → **Dropdown** : USER, ROLE
> - `granteeId` → Si USER : **Autocomplete email** via recherche users (le backend retourne l'ID dans la réponse). Si ROLE : **Dropdown** des rôles
> - `permission` → **Dropdown** : READ, DOWNLOAD, EDIT, REVIEW, DELETE, ADMIN

### Révoquer une permission — Audité

```
DELETE /v1/document-permissions?targetType=FOLDER&targetCode=FLD-202606-000001&granteeType=USER&granteeId=8&permission=READ
```

### Lister les permissions d'une cible

```
GET /v1/document-permissions?targetType=DOCUMENT&targetCode=DOC-202606-000042
```

### Permissions effectives d'un utilisateur sur un document

```
GET /v1/document-permissions/effective?userId=8&documentCode=DOC-202606-000042
```

Retourne les permissions héritées du dossier parent incluses.

**Réponse** : `["READ", "DOWNLOAD"]`

### Héritage

Les permissions sur un dossier s'appliquent automatiquement à tous ses documents et sous-dossiers. Une permission explicite sur un document s'ajoute aux permissions héritées.

---

## 18. Verrouillage (Lock/Unlock)

> Endpoints sur `/v1/documents/{code}/lock` et `/unlock`

Empêche les modifications concurrentes sur un document.

### Verrouiller — Audité

```
POST /v1/documents/{code}/lock
```

Le verrou expire automatiquement après **1 heure**. Un admin peut forcer le déverrouillage d'un document verrouillé par un autre utilisateur.

### Déverrouiller — Audité

```
POST /v1/documents/{code}/unlock
```

### Comportement

- Un document verrouillé ne peut être modifié que par l'utilisateur qui l'a verrouillé
- Si le verrou a expiré, un autre utilisateur peut re-verrouiller
- Les admins (`ROLE_ADMIN`, `ROLE_SUPER_ADMIN`) peuvent déverrouiller tout document
- Erreur 400 si le document est verrouillé par un autre utilisateur

---

# PARTIE 2 — KYC (Know Your Customer)

---

## Enums KYC

### KycCaseStatus

| Valeur               | Description                                          |
|----------------------|------------------------------------------------------|
| `NOT_STARTED`        | Dossier créé mais aucune action                      |
| `IN_PROGRESS`        | Client en train de fournir ses documents              |
| `SUBMITTED`          | Dossier soumis pour revue                            |
| `UNDER_REVIEW`       | En cours de revue par un agent                       |
| `APPROVED`           | Dossier approuvé                                     |
| `REJECTED`           | Dossier rejeté définitivement                        |
| `PENDING_CORRECTION` | Correction demandée, en attente du client            |
| `RENEWAL_REQUIRED`   | Documents expirés, renouvellement nécessaire         |
| `EXPIRED`            | Dossier expiré                                       |

### KycDocumentVerificationStatus

| Valeur     | Description               |
|------------|---------------------------|
| `PENDING`  | En attente de vérification |
| `VERIFIED` | Vérifié et validé          |
| `REJECTED` | Rejeté                     |
| `EXPIRED`  | Expiré                     |

### KycRiskLevel

`LOW` · `MEDIUM` · `HIGH` · `VERY_HIGH`

---

## 12. Dossiers KYC — Cycle de vie

> Base: `/v1/kyc/cases`

### Créer un dossier — Audité, Idempotent

```
POST /v1/kyc/cases
```

**Body** : `CreateKycCaseRequest`

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-202604-000012",
  "documentTypeCodes": ["MEMBER_ID_CARD", "MEMBER_PASSPORT"]
}
```

| Champ | Type | Requis | Saisie frontend |
|---|---|---|---|
| `ownerType` | DocumentOwnerType | Oui | **Dropdown** : `MEMBER`, `CUSTOMER`, `BUSINESS` |
| `ownerCode` | String | Oui | **Autocomplete** via `GET /v1/members?search=` ou `/v1/customers?search=` selon ownerType. Afficher nom, envoyer code |
| `documentTypeCodes` | List\<String\> | Non | **Multi-select checkbox** via `GET /v1/document-types?ownerType=MEMBER`. Afficher `name`, envoyer `code`. Si omis → requirements globaux |

**Réponse** : `KycCaseResponse`

```json
{
  "code": "KCS-202606-000001",
  "ownerName": "Jean Bokati",
  "ownerCode": "MBR-202604-000012",
  "ownerType": "MEMBER",
  "ownerId": 15,
  "status": "IN_PROGRESS",
  "startedAt": "2026-06-20T10:00:00Z",
  "submittedAt": null,
  "completedAt": null,
  "reviewedBy": null,
  "reviewedByEmail": null,
  "reviewedAt": null,
  "decisionComment": null,
  "assignedTo": null,
  "assignedToEmail": null,
  "assignedAt": null,
  "slaDeadline": null,
  "lastReminderSentAt": null,
  "reminderCount": 0,
  "riskLevel": "LOW",
  "kycLevel": 1,
  "complete": false,
  "approved": false,
  "missingDocumentTypeCodes": ["MEMBER_ID_CARD", "MEMBER_PASSPORT"],
  "requirements": [
    {
      "documentTypeCode": "MEMBER_ID_CARD",
      "documentTypeName": "Carte d'identité membre",
      "required": true,
      "requiresBackSide": true,
      "status": null,
      "documentCode": null,
      "backDocumentCode": null
    },
    {
      "documentTypeCode": "MEMBER_PASSPORT",
      "documentTypeName": "Passeport membre",
      "required": true,
      "requiresBackSide": false,
      "status": null,
      "documentCode": null,
      "backDocumentCode": null
    }
  ],
  "documents": []
}
```

> **Note `missingDocumentTypeCodes`** : Quand un document nécessite un recto/verso et que seul le recto est fourni, le code apparaît avec le suffixe `_BACK` (ex: `MEMBER_ID_CARD_BACK`).

### Obtenir un dossier

```
GET /v1/kyc/cases/{code}
```

### Rechercher des dossiers (paginé)

```
GET /v1/kyc/cases
```

| Param               | Type              | Requis | Description                          |
|----------------------|-------------------|--------|--------------------------------------|
| `status`             | KycCaseStatus     | Non    | Filtrer par statut                   |
| `ownerType`          | DocumentOwnerType | Non    | Filtrer par type propriétaire        |
| `submittedAfter`     | Instant (ISO)     | Non    | Soumis après                         |
| `submittedBefore`    | Instant (ISO)     | Non    | Soumis avant                         |
| `reviewedBy`         | Long              | Non    | Filtrer par reviewer                 |
| `pendingReviewOnly`  | Boolean           | Non    | Uniquement en attente de revue       |
| `expiringWithinDays` | Integer           | Non    | Documents expirant dans N jours      |
| `riskLevel`          | KycRiskLevel      | Non    | Filtrer par niveau de risque         |

### Soumettre un dossier — Audité, Idempotent

```
POST /v1/kyc/cases/{code}/submit
```

Valide que tous les documents requis (y compris les versos) sont fournis. Calcule le niveau de risque et le SLA.

### Approuver un dossier — Audité, Idempotent

```
POST /v1/kyc/cases/{code}/approve
```

```json
{
  "reviewedBy": 5,
  "comment": "Dossier conforme, KYC validé"
}
```

La réponse inclut `reviewedByEmail` résolu automatiquement à partir de l'ID.

Pré-conditions :
- Tous les documents requis sont fournis
- Tous les documents requis sont en statut `VERIFIED`
- Les règles de validation croisée passent

Effets secondaires :
- `MEMBER` → statut passe à `ACTIVE`, wallet par défaut créé
- `CUSTOMER` → statut passe à `ACTIVE`

### Rejeter (demander correction) — Audité, Idempotent

```
POST /v1/kyc/cases/{code}/reject
```

```json
{
  "reviewedBy": 5,
  "comment": "La photo de la CNI est floue. Veuillez resoumettre."
}
```

Le commentaire est **obligatoire**. Le dossier passe en `PENDING_CORRECTION`.

### Assigner un reviewer — Audité

```
PATCH /v1/kyc/cases/{code}/assign
```

**Recommandé — par email :**
```json
{ "email": "reviewer@bokati.com" }
```

> **Saisie assistée :** `email` → **Autocomplete** via recherche users par email. Le backend résout l'email en ID utilisateur.

Alternative par ID (usage interne uniquement) :
```json
{ "assignedTo": 8 }
```

Si le dossier est en `SUBMITTED`, il passe automatiquement en `UNDER_REVIEW`.

### Modifier le niveau de risque — Audité

```
PATCH /v1/kyc/cases/{code}/risk-level
```

```json
{
  "riskLevel": "HIGH",
  "reviewedBy": 5,
  "comment": "Suspicion de fraude documentaire"
}
```

### Ma file d'attente (dossiers assignés)

```
GET /v1/kyc/cases/my-queue?userId=8
```

Paginé. Si `userId` omis, utilise l'utilisateur connecté.

### Dossiers avec documents expirant bientôt

```
GET /v1/kyc/cases/expiring-soon?days=30
```

### Requirements manquants

```
GET /v1/kyc/cases/{code}/missing-requirements
```

**Réponse** : `List<KycRequirementStatus>` — uniquement les exigences non satisfaites.

### Statut d'expiration des documents

```
GET /v1/kyc/cases/{code}/expiry-status
```

**Réponse** : `List<KycExpiryDocumentStatus>`

```json
{
  "documentCode": "DOC-202606-000042",
  "documentType": "MEMBER_ID_CARD",
  "expiryDate": "2027-01-15",
  "daysUntilExpiry": 209,
  "expired": false,
  "status": "VERIFIED"
}
```

---

## 13. Requirements par dossier

> Base: `/v1/kyc/cases/{code}/requirements`

Permet de personnaliser les pièces requises par dossier. Si un dossier a des requirements spécifiques, ils **remplacent** les requirements globaux.

### Lister les requirements du dossier

```
GET /v1/kyc/cases/{code}/requirements
```

**Réponse** : `List<KycCaseRequirementResponse>`

```json
{
  "id": 500,
  "kycCaseCode": "KCS-202606-000001",
  "documentTypeCode": "MEMBER_ID_CARD",
  "documentTypeName": "Carte d'identité membre",
  "required": true,
  "requiresBackSide": true,
  "active": true
}
```

### Ajouter un requirement — Audité

```
POST /v1/kyc/cases/{code}/requirements
```

```json
{
  "documentTypeCode": "MEMBER_DRIVERS_LICENSE",
  "documentTypeName": "Permis de conduire",
  "required": true
}
```

| Champ | Type | Requis | Saisie frontend |
|---|---|---|---|
| `documentTypeCode` | String | Oui | **Select** via `GET /v1/document-types?ownerType=X`. Afficher `name`, envoyer `code` |
| `documentTypeName` | String | Non | **Auto-résolu** par le backend depuis DocumentType. Ne pas demander à l'utilisateur |
| `required` | Boolean | Non | Checkbox (défaut: `true`) |

### Retirer un requirement — Audité

```
DELETE /v1/kyc/cases/{code}/requirements/{documentTypeCode}
```

Le requirement est désactivé (soft delete), pas supprimé physiquement.

---

## 14. Documents recto/verso (Front/Back)

Certains types de documents (CNI, permis de conduire) nécessitent le recto **et** le verso. Le champ `requiresBackSide` sur le `DocumentType` contrôle cette exigence.

### Fonctionnement

1. Le frontend vérifie `requiresBackSide` dans les requirements du dossier
2. L'upload du **recto** crée un `KycDocument` classique (via portail client ou upload admin)
3. L'upload du **verso** s'attache au même `KycDocument` via le paramètre `side=BACK`
4. L'assessment considère le document **incomplet** tant que le verso manque (apparaît comme `{TYPE}_BACK` dans `missingDocumentTypeCodes`)

### Réponse document avec verso

Quand un document a un verso, les champs suivants sont remplis dans `KycDocumentResponse` :

```json
{
  "id": 200,
  "documentCode": "DOC-202606-000042",
  "documentType": "MEMBER_ID_CARD",
  "ownerName": "Jean Bokati",
  "documentNumber": "AB1234567",
  "fileName": "cni_recto.jpg",
  "fileSize": 2048576,
  "mimeType": "image/jpeg",
  "previewUrl": "/sni/api/v1/documents/DOC-202606-000042/preview",
  "downloadUrl": "/sni/api/v1/documents/DOC-202606-000042/download",
  "requiresBackSide": true,
  "backDocumentCode": "DOC-202606-000043",
  "backFileName": "cni_verso.jpg",
  "backFileSize": 1948576,
  "backMimeType": "image/jpeg",
  "backPreviewUrl": "/sni/api/v1/documents/DOC-202606-000043/preview",
  "backDownloadUrl": "/sni/api/v1/documents/DOC-202606-000043/download",
  "issueDate": "2023-01-15",
  "expiryDate": "2033-01-15",
  "status": "PENDING"
}
```

### Types de documents avec recto/verso (seed)

| Code                      | Nom                  | requiresBackSide |
|---------------------------|----------------------|------------------|
| `MEMBER_ID_CARD`          | Carte d'identité     | `true`           |
| `MEMBER_DRIVERS_LICENSE`  | Permis de conduire   | `true`           |
| `MEMBER_PASSPORT`         | Passeport            | `false`          |
| `MEMBER_NIU`              | NIU                  | `false`          |

---

## 15. Revue de documents dans un dossier

> Base: `/v1/kyc/cases/{code}/...`

Ces endpoints opèrent **uniquement** sur les documents appartenant au dossier spécifié. Si un code de document n'appartient pas au dossier, l'opération échoue pour ce document.

### File de revue du dossier

```
GET /v1/kyc/cases/{code}/review-queue
```

**Réponse** : `List<KycDocumentResponse>` — documents en `PENDING` dans ce dossier.

### Approbation en masse dans le dossier — Audité, Idempotent

```
POST /v1/kyc/cases/{code}/documents/bulk-approve
```

```json
{
  "documentCodes": ["DOC-202606-000042", "DOC-202606-000043"],
  "reviewedBy": 5
}
```

**Réponse** : `KycBulkActionResponse`

```json
{
  "processed": 2,
  "succeeded": 2,
  "failed": 0,
  "results": [
    { "documentCode": "DOC-202606-000042", "success": true, "error": null },
    { "documentCode": "DOC-202606-000043", "success": true, "error": null }
  ]
}
```

### Rejet en masse dans le dossier — Audité, Idempotent

```
POST /v1/kyc/cases/{code}/documents/bulk-reject
```

```json
{
  "items": [
    { "documentCode": "DOC-202606-000044", "reason": "Document expiré" },
    { "documentCode": "DOC-202606-000045", "reason": "Photo trop sombre" }
  ],
  "reviewedBy": 5
}
```

### Demande de correction immédiate — Audité

```
POST /v1/kyc/cases/{code}/documents/request-correction
```

```json
{
  "documentCode": "DOC-202606-000042",
  "reviewedBy": 5,
  "correctionNote": "Le numéro de document est coupé. Veuillez resoumettre une photo complète.",
  "deadlineDays": 5
}
```

**Réponse** : `KycDocumentResponse` — le document mis à jour avec statut `REJECTED`.

---

## 16. Revue globale de documents KYC

> Base: `/v1/kyc/documents`

Ces endpoints opèrent sur **tous** les documents KYC, sans restriction de dossier.

### File de revue globale

```
GET /v1/kyc/documents/review-queue
```

### Documents révisés (filtré par statut)

```
GET /v1/kyc/documents/reviewed?status=APPROVED
GET /v1/kyc/documents/approved
GET /v1/kyc/documents/rejected
```

Valeurs acceptées pour `status` : `APPROVED`, `VERIFIED`, `REJECTED`, `REJECT`, `DISAPPROVED`

### Approbation en masse globale — Audité, Idempotent

```
POST /v1/kyc/documents/bulk-approve
```

```json
{
  "documentIds": ["DOC-202606-000042", "DOC-202606-000043"],
  "reviewedBy": 5
}
```

### Rejet en masse global — Audité, Idempotent

```
POST /v1/kyc/documents/bulk-reject
POST /v1/kyc/documents/bulk-disapprove
POST /v1/kyc/documents/bulk-desapprove
```

```json
{
  "items": [
    { "documentCode": "DOC-202606-000044", "reason": "Non conforme" }
  ],
  "reviewedBy": 5
}
```

---

## 17. Règles de validation croisée

> Base: `/v1/kyc/cross-validation-rules`

Permettent de comparer automatiquement un champ extrait par OCR entre deux types de documents (ex: vérifier que le nom sur la CNI correspond au nom sur le passeport).

### Champs comparables

`first_name` · `last_name` · `date_of_birth` · `expiry_date` · `document_number` · `nationality`

### Lister les règles

```
GET /v1/kyc/cross-validation-rules?activeOnly=true
```

### Créer une règle — Audité

```
POST /v1/kyc/cross-validation-rules
```

```json
{
  "documentTypeCode1": "MEMBER_ID_CARD",
  "documentTypeCode2": "MEMBER_PASSPORT",
  "fieldToCompare": "last_name",
  "blocking": true,
  "active": true
}
```

| Champ              | Type    | Requis | Description                                               |
|--------------------|---------|--------|-----------------------------------------------------------|
| `documentTypeCode1`| String  | Oui    | Premier type de document                                  |
| `documentTypeCode2`| String  | Oui    | Second type de document                                   |
| `fieldToCompare`   | String  | Oui    | Champ OCR à comparer                                      |
| `blocking`         | Boolean | Non    | Si `true`, bloque l'approbation en cas de mismatch        |
| `active`           | Boolean | Non    | Règle active                                              |

### Modifier — Audité

```
PUT /v1/kyc/cross-validation-rules/{id}
```

### Obtenir / Supprimer

```
GET /v1/kyc/cross-validation-rules/{id}
DELETE /v1/kyc/cross-validation-rules/{id}
```

---

## 18. Notes & Timeline

### Ajouter une note — Audité

```
POST /v1/kyc/cases/{code}/notes
```

```json
{
  "content": "Client contacté par téléphone, va resoumettre demain",
  "authorId": 5,
  "internal": true
}
```

| Champ      | Type    | Requis | Description                                        |
|------------|---------|--------|----------------------------------------------------|
| `content`  | String  | Oui    | Contenu de la note                                 |
| `authorId` | Long    | Oui    | ID de l'auteur                                     |
| `internal` | Boolean | Non    | `true` = note interne (cachée du client), défaut   |

### Lister les notes

```
GET /v1/kyc/cases/{code}/notes
```

**Réponse** : `List<KycCaseNoteResponse>`

```json
{
  "id": 50,
  "kycCaseCode": "KCS-202606-000001",
  "content": "Client contacté par téléphone",
  "authorId": 5,
  "authorEmail": "agent@bokati.com",
  "createdAt": "2026-06-20T14:30:00Z",
  "internal": true
}
```

### Timeline complète

```
GET /v1/kyc/cases/{code}/timeline
```

**Réponse** : `List<KycTimelineEntryResponse>` — événements triés chronologiquement.

```json
[
  {
    "timestamp": "2026-06-20T10:00:00Z",
    "action": "CASE_CREATED",
    "actor": "SYSTEM",
    "description": "KYC case created"
  },
  {
    "timestamp": "2026-06-20T10:15:00Z",
    "action": "DOCUMENT_UPLOADED",
    "actor": "MBR-202604-000012",
    "description": "Document MEMBER_ID_CARD submitted"
  },
  {
    "timestamp": "2026-06-20T11:00:00Z",
    "action": "CASE_SUBMITTED",
    "actor": "MBR-202604-000012",
    "description": "KYC case submitted"
  },
  {
    "timestamp": "2026-06-20T14:30:00Z",
    "action": "NOTE_ADDED",
    "actor": "USR-5",
    "description": "Client contacté par téléphone"
  },
  {
    "timestamp": "2026-06-20T15:00:00Z",
    "action": "DOCUMENT_VERIFIED",
    "actor": "USR-5",
    "description": "Document review recorded"
  }
]
```

---

## 19. Dashboard KYC

```
GET /v1/kyc/cases/dashboard
```

**Réponse** : `KycDashboardResponse`

```json
{
  "generatedAt": "2026-06-20T16:00:00Z",
  "summary": {
    "totalCases": 150,
    "NOT_STARTED": 5,
    "IN_PROGRESS": 20,
    "SUBMITTED": 10,
    "UNDER_REVIEW": 8,
    "APPROVED": 90,
    "REJECTED": 2,
    "PENDING_CORRECTION": 12,
    "RENEWAL_REQUIRED": 3,
    "EXPIRED": 0
  },
  "sla": {
    "casesReviewedWithin24h": 85,
    "casesExceeding48h": 3,
    "avgReviewTimeHours": 6.50
  },
  "expiringSoon": {
    "within7Days": 2,
    "within30Days": 8,
    "within60Days": 15
  },
  "byOwnerType": [
    { "ownerType": "MEMBER", "count": 120, "pendingReview": 15 },
    { "ownerType": "CUSTOMER", "count": 25, "pendingReview": 3 },
    { "ownerType": "BUSINESS", "count": 5, "pendingReview": 0 }
  ],
  "recentActivity": [
    { "action": "APPROVED", "period": "LAST_7_DAYS", "count": 12 },
    { "action": "REJECTED", "period": "LAST_7_DAYS", "count": 3 }
  ]
}
```

---

## 20. OCR & Extraction automatique

```
GET /v1/kyc/documents/{documentCode}/ocr-result
```

**Réponse** : `KycDocumentOcrResultResponse`

```json
{
  "documentCode": "DOC-202606-000042",
  "extractedFirstName": "Jean",
  "extractedLastName": "Bokati",
  "extractedDateOfBirth": "1990-05-15",
  "extractedExpiryDate": "2033-01-15",
  "extractedDocumentNumber": "AB1234567",
  "extractedNationality": "CG",
  "confidenceScore": 0.9250,
  "rawOcrJson": "{...}",
  "processedAt": "2026-06-20T10:31:00Z"
}
```

L'OCR est exécuté automatiquement après l'upload via Tesseract. Le `confidenceScore` va de `0.0` à `1.0`.

---

## 21. Export PDF

```
GET /v1/kyc/cases/{code}/export/pdf?includeInternalNotes=true
```

| Param                | Type    | Défaut | Description                      |
|----------------------|---------|--------|----------------------------------|
| `includeInternalNotes`| boolean| `true` | Inclure les notes internes       |

Retourne un PDF contenant le rapport complet du dossier KYC (timeline, documents, expiry, notes).

---

# PARTIE 3 — Intégration

---

## 22. Flows d'utilisation recommandés

### Flow 1 : Création d'un dossier KYC avec pièces personnalisées

```
1. GET /v1/document-types?ownerType=MEMBER&active=true
   → Obtenir la liste des types de documents disponibles

2. POST /v1/kyc/cases
   Body: { ownerType: "MEMBER", ownerCode: "MBR-...", documentTypeCodes: ["MEMBER_ID_CARD", "MEMBER_NIU"] }
   → Crée le dossier avec requirements spécifiques

3. GET /v1/kyc/cases/{code}
   → Vérifie les requirements et documents manquants
```

### Flow 2 : Ajout de pièces à un dossier existant

```
1. GET /v1/kyc/cases/{code}/requirements
   → Liste actuelle des requirements du dossier

2. POST /v1/kyc/cases/{code}/requirements
   Body: { documentTypeCode: "MEMBER_DRIVERS_LICENSE" }
   → Ajoute le permis de conduire aux pièces requises

3. GET /v1/kyc/cases/{code}
   → Le permis apparaît maintenant dans missingDocumentTypeCodes
```

### Flow 3 : Upload recto/verso d'une pièce d'identité (portail client)

```
1. GET /v1/client/documents/kyc/requirements
   → Voir les pièces requises avec requiresBackSide

2. POST /v1/client/documents/kyc/documents
   Form: file=cni_recto.jpg, documentType=MEMBER_ID_CARD, side=FRONT
   → Upload le recto

3. POST /v1/client/documents/kyc/documents
   Form: file=cni_verso.jpg, documentType=MEMBER_ID_CARD, side=BACK
   → Upload le verso, attaché au même KycDocument

4. GET /v1/client/documents/kyc/documents
   → Le document montre backDocumentCode rempli
```

### Flow 4 : Revue d'un dossier KYC complet

```
1. GET /v1/kyc/cases/my-queue
   → Liste des dossiers assignés au reviewer

2. GET /v1/kyc/cases/{code}
   → Détail du dossier avec requirements et documents

3. GET /v1/kyc/cases/{code}/review-queue
   → Documents en PENDING dans ce dossier

4a. POST /v1/kyc/cases/{code}/documents/bulk-approve
    → Approuver tous les documents conformes

4b. POST /v1/kyc/cases/{code}/documents/request-correction
    → Demander une correction sur un document spécifique

5. GET /v1/kyc/documents/{documentCode}/ocr-result
   → Vérifier les données extraites par OCR

6. POST /v1/kyc/cases/{code}/approve  (ou /reject)
   → Valider ou rejeter le dossier complet
```

### Flow 5 : Gestion des types de documents et requirements globaux

```
1. POST /v1/document-types
   → Créer un nouveau type (ex: MEMBER_FISCAL_CERTIFICATE)

2. POST /v1/document-requirements
   → Rendre ce type obligatoire pour tous les MEMBER

3. PATCH /v1/document-types/{code}
   → Modifier les propriétés (requiresBackSide, requiresExpiryDate, etc.)
```

### Flow 6 : Monitoring et suivi

```
1. GET /v1/kyc/cases/dashboard
   → Vue globale KYC (compteurs, SLA, expirations)

2. GET /v1/kyc/cases?pendingReviewOnly=true
   → Dossiers en attente de revue

3. GET /v1/kyc/cases/expiring-soon?days=30
   → Dossiers avec documents expirant

4. GET /v1/documents/dashboard?space=KYC_SPACE
   → Dashboard documentaire espace KYC

5. GET /v1/documents/analytics?space=KYC_SPACE&months=6
   → Tendances et métriques
```

---

## 23. Recommandations d'écrans

### Module GED — Ecrans recommandés

#### 1. Dashboard Documents
- **Widgets** : total par statut, par catégorie, taux de rejet, temps moyen de revue, top tags
- **Source** : `GET /v1/documents/dashboard`
- **Filtres** : par espace (`DocumentSpace`)

#### 2. Explorateur de documents
- **Vue** : tableau paginé avec recherche et filtres avancés
- **Source** : `GET /v1/documents/search`
- **Colonnes** : code, titre, type, propriétaire, statut, date upload, taille, tags
- **Actions ligne** : preview, download, approve, reject, request-correction, archive
- **Filtres sidebar** : espace, catégorie, statut, tags, propriétaire, dates, expiration

#### 3. Vue dossier propriétaire
- **Vue** : documents groupés par espace pour un propriétaire donné
- **Source** : `GET /v1/documents/folder`
- **Sections** : KYC, Contrats, Financier, Administratif
- **Indicateurs** : requirements manquants, documents expirant bientôt

#### 4. File de revue
- **Vue** : liste des documents en attente par espace
- **Source** : `GET /v1/review/queue` ou `GET /v1/review/{space}`
- **Actions** : approve, reject, request-correction (unitaire ou en masse)
- **Indicateurs** : documents > 48h en attente

#### 5. Configuration — Types de documents
- **Vue** : CRUD sur les types de documents
- **Source** : `GET/POST/PATCH/DELETE /v1/document-types`
- **Formulaire** : code, nom, catégorie, ownerType, requiresBackSide, requiresExpiryDate, requiresReview, etc.

#### 6. Configuration — Exigences documentaires
- **Vue** : CRUD sur les requirements globaux
- **Source** : `GET/POST/PATCH/DELETE /v1/document-requirements`
- **Groupés par** : ownerType

#### 7. Configuration — Tags
- **Vue** : gestion des tags (code, label, couleur, espace)
- **Source** : `GET/POST/PUT/DELETE /v1/document-tags`

#### 8. Configuration — Politiques de rétention
- **Vue** : gestion des politiques d'archivage
- **Source** : `GET/POST/PUT/DELETE /v1/document-retention-policies`

#### 9. Analytics & Export
- **Vue** : graphiques de tendances, taux, et export CSV
- **Source** : `GET /v1/documents/analytics`, `GET /v1/documents/export/csv`

#### 10. Détail document
- **Vue** : page détail avec preview inline, versions, tags, métadonnées, signatures, logs d'accès, statut workflow, verrou
- **Sources** : `GET /v1/documents/{code}`, `/versions`, `/tags`, `/metadata`, `/signatures`, `/access-logs`, `/workflow/status`, `/shares`
- **Actions** : lock/unlock, start-workflow, share, move to folder

#### 11. Explorateur de dossiers (Arborescence)
- **Vue** : navigation arborescente type explorateur de fichiers
- **Sources** : `GET /v1/document-folders/roots?space=X` → `/{code}/children` → `/{code}/documents`
- **Panneau gauche** : arborescence dossiers avec `GET /{code}/tree`
- **Panneau droit** : contenu du dossier sélectionné (documents paginés)
- **Breadcrumb** : `GET /{code}/breadcrumb`
- **Actions** : créer sous-dossier, renommer, déplacer, supprimer, upload dans le dossier, drag & drop

#### 12. Gestion des workflows
- **Vue** : CRUD sur les workflows et leurs étapes
- **Sources** : `GET/POST/DELETE /v1/document-workflows`
- **Formulaire** : nom, type, espace, type de document, étapes (ordonnées avec drag & drop)
- **Saisie** : approverType = dropdown, approverValue = autocomplete email ou dropdown rôle

#### 13. Mes approbations en attente
- **Vue** : liste des documents en attente d'approbation par l'utilisateur connecté
- **Source** : `GET /v1/document-workflows/my-pending`
- **Actions** : approuver, rejeter (avec commentaire)

#### 14. Gestion des partages
- **Vue** : dans le détail document, section "Liens de partage"
- **Sources** : `GET /v1/documents/{code}/shares`, `POST /v1/documents/{code}/share`
- **Actions** : créer lien, révoquer, copier URL
- **Indicateurs** : expiré, nombre d'accès restants, protégé par mot de passe

#### 15. Gestion des permissions
- **Vue** : dans le détail document ou dossier, onglet "Permissions"
- **Sources** : `GET /v1/document-permissions?targetType=X&targetCode=Y`
- **Actions** : accorder (autocomplete user/role), révoquer
- **Indicateur** : permissions héritées du dossier parent

---

### Module KYC — Ecrans recommandés

#### 1. Dashboard KYC
- **Widgets** : compteurs par statut, SLA (< 24h, > 48h, avg), expirations (7j/30j/60j), activité récente
- **Source** : `GET /v1/kyc/cases/dashboard`
- **Liens rapides** : dossiers en attente, expirant bientôt, ma file

#### 2. Liste des dossiers KYC
- **Vue** : tableau paginé avec filtres multiples
- **Source** : `GET /v1/kyc/cases`
- **Colonnes** : code, propriétaire, type, statut, niveau de risque, date soumission, SLA, assigné à
- **Actions ligne** : voir détail, assigner, changer risque
- **Filtres** : statut, ownerType, riskLevel, pendingReviewOnly, expiringWithinDays

#### 3. Détail d'un dossier KYC
- **Sections** :
  - **En-tête** : code, propriétaire, statut, risque, SLA deadline, assigné à
  - **Requirements** : liste avec statut par type, indicateur recto/verso, documents fournis vs manquants
  - **Documents** : carte par document avec preview recto/verso, statut, actions (approve/reject/correct)
  - **OCR** : résultats d'extraction automatique par document
  - **Timeline** : fil chronologique des événements
  - **Notes** : notes internes avec formulaire d'ajout
- **Actions globales** : assigner, soumettre, approuver dossier, rejeter dossier, export PDF
- **Sources** : `GET /v1/kyc/cases/{code}`, `GET /v1/kyc/cases/{code}/requirements`, `GET /v1/kyc/cases/{code}/review-queue`, `GET /v1/kyc/cases/{code}/timeline`, `GET /v1/kyc/cases/{code}/notes`, `GET /v1/kyc/cases/{code}/expiry-status`, `GET /v1/kyc/documents/{documentCode}/ocr-result`

#### 4. File de revue KYC
- **Vue** : documents KYC en attente de vérification
- **Source** : `GET /v1/kyc/documents/review-queue`
- **Actions** : bulk-approve, bulk-reject, voir OCR
- **Onglets** : En attente | Approuvés | Rejetés

#### 5. Revue dans un dossier (vue scopée)
- **Vue** : documents d'un dossier spécifique en attente
- **Source** : `GET /v1/kyc/cases/{code}/review-queue`
- **Actions** :
  - Approuver tout : `POST /v1/kyc/cases/{code}/documents/bulk-approve`
  - Rejeter sélection : `POST /v1/kyc/cases/{code}/documents/bulk-reject`
  - Correction unitaire : `POST /v1/kyc/cases/{code}/documents/request-correction`

#### 6. Gestion des requirements par dossier
- **Vue** : dans le détail du dossier, section "Pièces requises"
- **Sources** : `GET/POST/DELETE /v1/kyc/cases/{code}/requirements`
- **Actions** : ajouter un type, retirer un type, voir quels sont satisfaits

#### 7. Configuration — Règles de validation croisée
- **Vue** : CRUD sur les règles de comparaison inter-documents
- **Source** : `GET/POST/PUT/DELETE /v1/kyc/cross-validation-rules`
- **Formulaire** : document type 1, document type 2, champ à comparer, bloquant oui/non

#### 8. Création de dossier KYC
- **Vue** : formulaire de création
- **Étapes** :
  1. Sélection du type de propriétaire et du propriétaire
  2. Choix des pièces requises (multi-select depuis les types disponibles) — optionnel, sinon requirements globaux
  3. Confirmation et création
- **Source** : `GET /v1/document-types?ownerType=MEMBER`, `POST /v1/kyc/cases`

---

## 24. Permissions RBAC

| Permission          | Endpoints protégés                                           |
|---------------------|--------------------------------------------------------------|
| `DOCUMENT:REVIEW`   | approve, reject, request-correction, restore-version (documents) |
| `DOCUMENT:REVIEW`   | review queue, bulk operations, analytics, export CSV, access logs |
| `DOCUMENT:REVIEW`   | politiques de rétention (tous les endpoints)                 |

Les endpoints KYC ne sont pas protégés par des permissions spécifiques au niveau contrôleur — la sécurité est gérée par les filtres d'authentification JWT. Les rôles admin ont accès à toutes les opérations KYC.

---

## 25. Codes d'erreur

| Code HTTP | Situation                                                    |
|-----------|--------------------------------------------------------------|
| `400`     | Validation échouée, document manquant, dossier non soumissible, rejet sans commentaire |
| `401`     | Token JWT manquant ou expiré                                 |
| `403`     | Permission insuffisante (ex: `DOCUMENT:REVIEW` manquant)     |
| `404`     | Document, dossier KYC, type de document ou requirement non trouvé |
| `409`     | Doublon d'idempotence, requirement déjà existant sur le dossier |
| `422`     | Cross-validation échouée lors de l'approbation               |

### Format d'erreur standard

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "KYC case is incomplete. Missing required documents: MEMBER_ID_CARD, MEMBER_ID_CARD_BACK",
  "timestamp": "2026-06-20T10:30:00Z",
  "path": "/sni/api/v1/kyc/cases/KCS-202606-000001/submit"
}
```
