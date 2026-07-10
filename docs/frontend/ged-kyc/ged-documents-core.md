# GED — Documents (cœur du module)

> **Audience** : développeur frontend
> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Base de ce module** : `/documents` — **back-office uniquement** (JWT admin requis).
> Voir le [README](./README.md) pour le rattachement `ownerType`/`ownerCode` et les enums transverses.

Ce document couvre le cycle de vie d'un document : upload, versions, décisions de revue, recherche, dashboard, dossier client, analytics, export et journaux d'accès. Chaque section donne la requête exacte, un exemple de payload et un exemple de réponse.

---

## Le modèle `DocumentResponse`

C'est l'objet renvoyé par la quasi-totalité des routes ci-dessous. Le connaître une fois évite de le répéter.

```json
{
  "code": "DOC-202607-00000042",
  "ownerId": 1287,
  "ownerType": "CUSTOMER",
  "category": "KYC",
  "space": "KYC_SPACE",
  "spaceReferenceCode": null,
  "folderCode": "FOL-202607-00000005",
  "folderName": "Pièces d'identité",
  "documentTypeCode": "CNI",
  "documentTypeName": "Carte nationale d'identité",
  "title": "CNI recto",
  "description": "Carte d'identité du client",
  "fileName": "cni-recto.jpg",
  "fileUrl": "https://.../documents/DOC-202607-00000042/download",
  "previewUrl": "https://.../documents/DOC-202607-00000042/preview",
  "downloadUrl": "https://.../documents/DOC-202607-00000042/download",
  "fileSize": 245113,
  "mimeType": "image/jpeg",
  "checksumSha256": "9f86d0818...",
  "currentVersionNumber": 1,
  "status": "PENDING_REVIEW",
  "issueDate": "2020-01-15",
  "expiryDate": "2030-01-15",
  "uploadedBy": 42,
  "uploadedAt": "2026-07-09T10:15:30Z",
  "updatedAt": "2026-07-09T10:15:30Z",
  "versions": [ /* DocumentVersionResponse[] — voir §3 */ ],
  "tags": [
    { "id": 7, "code": "urgent", "label": "Urgent", "color": "#FF0000", "space": null, "createdBy": 42, "createdAt": "2026-07-01T09:00:00Z" }
  ],
  "metadata": { "source": "guichet", "agence": "Brazzaville-Centre" }
}
```

| Champ | Type | Notes |
|---|---|---|
| `code` | string | Numéro public du document — **c'est l'identifiant à utiliser partout** dans les routes `/{code}`. |
| `ownerId` | number | Id **interne** du propriétaire (renvoyé en lecture seule ; en écriture on envoie toujours `ownerCode`). |
| `ownerType` | enum `DocumentOwnerType` | `CUSTOMER, MEMBER, BUSINESS, CONTRACT, INVOICE, PAYMENT, PROPOSAL, ASSET`. |
| `category` | enum `DocumentCategory` | Héritée du type de document. `KYC, LEGAL, SYSTEM, FINANCIAL, ASSET, OTHER`. |
| `space` | enum `DocumentSpace` | `KYC_SPACE, CONTRACT_SPACE, FINANCIAL_SPACE, ASSET_SPACE, ADMINISTRATIVE, GENERIC`. |
| `status` | enum `DocumentStatus` | Voir la liste en fin de document. |
| `currentVersionNumber` | number | Version actuellement active. |
| `fileUrl` / `previewUrl` / `downloadUrl` | string | URLs prêtes à l'emploi ; `preview` et `download` déclenchent un log d'accès. |
| `versions` / `tags` / `metadata` | listes/map | Peuplés sur le détail (`GET /documents/{code}`). |

---

## 1. Uploader un document

```http
POST /documents/upload
Content-Type: multipart/form-data
Idempotency-Key: <uuid>   (recommandé)
```

Requête **multipart** — deux parties :

- une partie `file` : le fichier binaire ;
- les autres champs (métadonnées) envoyés comme champs de formulaire (`DocumentUploadMetadataRequest`).

| Champ (form-data) | Obligatoire | Description |
|---|---|---|
| `ownerType` | ✅ | Type de propriétaire (`DocumentOwnerType`). |
| `ownerCode` | ✅ | **Code public** du propriétaire (jamais l'id interne). |
| `documentTypeCode` | ✅ | Code du type de document (voir [ged-catalog-config.md](./ged-catalog-config.md)). |
| `title` | ✅ | Titre affiché. |
| `description` | — | Texte libre. |
| `documentNumber` | — | Numéro figurant sur la pièce (ex: n° de CNI). |
| `issueDate` | — | Date d'émission (`yyyy-MM-dd`). |
| `expiryDate` | — | Date d'expiration (`yyyy-MM-dd`). |
| `file` | ✅ | La partie binaire. |

Exemple (pseudo-`FormData`) :

```
ownerType=CUSTOMER
ownerCode=CUS-202606-00000012
documentTypeCode=CNI
title=CNI recto
documentNumber=1234567890
issueDate=2020-01-15
expiryDate=2030-01-15
file=<binary>
```

**Réponse** `201 Created` → un `DocumentResponse` (voir ci-dessus), avec `status` initial (`UPLOADED` ou `PENDING_REVIEW` selon la config du type de document) et `currentVersionNumber = 1`.

> Cette route porte `@Idempotent` : envoyez un header `Idempotency-Key` pour éviter les doublons en cas de retry réseau sur un formulaire d'upload.

---

## 2. Remplacer le fichier (nouvelle version)

```http
POST /documents/{code}/replace
Content-Type: multipart/form-data
Idempotency-Key: <uuid>
```

Une seule partie : `file`. Le document garde son `code` ; une nouvelle entrée est ajoutée dans `versions[]` et `currentVersionNumber` est incrémenté.

**Réponse** `200 OK` → `DocumentResponse` mis à jour.

---

## 3. Versions

```http
GET /documents/{code}/versions
```

**Réponse** `200 OK` → `DocumentVersionResponse[]` :

```json
[
  {
    "versionNumber": 2,
    "storageProvider": "S3",
    "storagePath": "documents/2026/07/....",
    "previewUrl": "https://.../preview",
    "downloadUrl": "https://.../download",
    "originalFileName": "cni-recto-v2.jpg",
    "storedFileName": "a1b2c3.jpg",
    "mimeTypeDeclared": "image/jpeg",
    "mimeTypeDetected": "image/jpeg",
    "fileExtension": "jpg",
    "checksumSha256": "9f86d0818...",
    "fileSizeBytes": 251004,
    "uploadStatus": "READY",
    "antivirusStatus": "CLEAN",
    "uploadedBy": 42,
    "uploadedAt": "2026-07-09T11:00:00Z",
    "current": true
  }
]
```

- `uploadStatus` (`DocumentVersionUploadStatus`) : `PENDING_SCAN, READY, QUARANTINED, REJECTED, DELETED`.
- `antivirusStatus` (`DocumentAntivirusStatus`) : `PENDING, CLEAN, INFECTED, ERROR`. Tant que ce n'est pas `CLEAN`, ne pas proposer le téléchargement.

Restaurer une ancienne version comme version courante :

```http
POST /documents/{code}/versions/{versionNumber}/restore     🔒 DOCUMENT:REVIEW
```

**Réponse** `200 OK` → `DocumentResponse`.

---

## 4. Télécharger / prévisualiser

```http
GET /documents/{code}/download    → pièce jointe (Content-Disposition: attachment), log d'accès DOWNLOAD
GET /documents/{code}/preview     → inline (Content-Disposition: inline), log d'accès PREVIEW
```

⚠️ Ces deux routes renvoient des **octets bruts** (le fichier), pas du JSON — à traiter comme un blob côté frontend. Le type MIME et le nom de fichier sont dans les headers de la réponse.

---

## 5. Détail & liste

```http
GET /documents/{code}
```
**Réponse** `200 OK` → `DocumentResponse` complet (avec `versions[]`, `tags[]`, `metadata`).

```http
GET /documents?ownerType=CUSTOMER&ownerCode=CUS-...&page=0&size=20
```
Les deux filtres sont optionnels. **Réponse** `200 OK` → `PaginatedResponse<DocumentResponse>`. **Toutes** les listes paginées de ce module utilisent la même enveloppe :

```json
{
  "data": [ /* DocumentResponse[] */ ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 134,
    "totalPages": 7,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false,
    "sort": { "sorted": true, "unsorted": false, "direction": "DESC", "properties": ["uploadedAt"] }
  }
}
```

⚠️ Le tableau d'éléments est dans **`data`** (pas `content`) et les métadonnées de pagination dans **`pageable`**. Pagination par défaut : `size=20`, tri par `uploadedAt` décroissant.

---

## 6. Décisions de revue

Toutes ces routes exigent la permission **`DOCUMENT:REVIEW`** et renvoient le `DocumentResponse` avec le nouveau `status`.

### 6.1 Approuver

```http
POST /documents/{code}/approve     🔒 DOCUMENT:REVIEW
Content-Type: application/json
```
```json
{ "reviewedBy": 42, "comment": "Pièce conforme" }
```
`reviewedBy` (Long) est **obligatoire** ; `comment` optionnel. → `status = APPROVED`.

### 6.2 Rejeter

```http
POST /documents/{code}/reject      🔒 DOCUMENT:REVIEW
```
```json
{
  "reviewedBy": 42,
  "comment": "Document illisible",
  "rejectionReasonCode": "ILLEGIBLE",
  "rejectionReasonDetail": "Le recto est flou, merci de rescanner"
}
```
`reviewedBy` obligatoire ; `comment`, `rejectionReasonCode`, `rejectionReasonDetail` optionnels. → `status = REJECTED`.

### 6.3 Demander une correction

```http
POST /documents/{code}/request-correction     🔒 DOCUMENT:REVIEW
```
```json
{
  "reviewedBy": 42,
  "correctionNote": "Merci de renvoyer une photo nette du recto",
  "comment": "",
  "deadlineDays": 7
}
```
`reviewedBy` et `correctionNote` obligatoires ; `deadlineDays` par défaut `7` (min 1). → `status = NEEDS_CORRECTION`.

> Pour traiter **plusieurs documents en un appel**, voir [ged-review-workflow-retention.md](./ged-review-workflow-retention.md).

---

## 7. Archiver / verrouiller

```http
POST /documents/{code}/archive?reason=Fin+de+conservation   → DocumentResponse (status = ARCHIVED)
POST /documents/{code}/lock                                  → 200, sans corps
POST /documents/{code}/unlock                                → 200, sans corps
```
`reason` est optionnel. Le verrou empêche les modifications concurrentes (upload/replace) tant qu'il est actif.

---

## 8. Upload en masse

### 8.1 Plusieurs fichiers, métadonnées partagées

```http
POST /documents/upload-batch
Content-Type: multipart/form-data
```
Parties : `files` (répété, un par fichier) + les champs `BatchUploadMetadataRequest` :

| Champ | Obligatoire | Description |
|---|---|---|
| `ownerType` | ✅ | `DocumentOwnerType`. |
| `ownerCode` | ✅ | Code public du propriétaire. |
| `documentTypeCode` | ✅ | Type appliqué à tous les fichiers du lot. |
| `space` | — | `DocumentSpace`. |
| `folderCode` | — | Dossier de destination. |
| `referenceCode` | — | Référence métier libre. |
| `tagCodes` | — | Liste de codes de tags à appliquer. |

**Réponse** `201 Created` → `DocumentBulkActionResponse` :

```json
{
  "total": 3,
  "succeeded": 2,
  "failed": 1,
  "results": [
    { "code": "DOC-...41", "status": "SUCCESS", "reason": null },
    { "code": "DOC-...42", "status": "SUCCESS", "reason": null },
    { "code": null, "status": "FAILED", "reason": "Type MIME non autorisé" }
  ]
}
```

### 8.2 Import d'un ZIP (recrée l'arborescence de dossiers)

```http
POST /documents/import-zip
Content-Type: multipart/form-data
```
Partie `file` (le .zip) + mêmes champs `BatchUploadMetadataRequest`.

**Réponse** `201 Created` → `ZipImportResponse` :

```json
{
  "totalFiles": 12,
  "succeeded": 11,
  "failed": 1,
  "foldersCreated": 3,
  "items": [
    { "originalPath": "identite/cni.jpg", "documentCode": "DOC-...50", "folderCode": "FOL-...05", "success": true, "error": null },
    { "originalPath": "corrompu.xyz", "documentCode": null, "folderCode": null, "success": false, "error": "Extension non supportée" }
  ]
}
```

---

## 9. Recherche avancée

```http
GET /documents/search
```
Tous les paramètres sont optionnels et combinables :

| Paramètre | Type | Description |
|---|---|---|
| `q` | string | Recherche plein texte (titre, description, nom de fichier…). |
| `space` | `DocumentSpace` | Filtre par espace. |
| `category` | `DocumentCategory` | Filtre par catégorie. |
| `tags` | liste | Codes de tags (répétable : `tags=urgent&tags=legal`). |
| `ownerType` / `ownerCode` | enum / string | Restreint à un propriétaire précis. |
| `statuses` | liste `DocumentStatus` | Répétable (voir filtrage par statut ci-dessous). |
| `expiresInDays` | number | Documents expirant dans N jours. |
| `isExpired` | bool | `true` = uniquement les expirés. |
| `uploadedAfter` / `uploadedBefore` | date `yyyy-MM-dd` | Fenêtre d'upload. |
| `metaKey` / `metaValue` | string | Filtre sur une métadonnée libre. |
| `page` / `size` / `sort` | pagination | Défaut `size=20`, tri `uploadedAt` desc. |

**Réponse** `200 OK` → `PaginatedResponse<DocumentResponse>`.

### Filtrage par statut

Il n'existe **pas** de route par statut (`/documents/approved`, etc.). Tout passe par `statuses` (répétable) :

| Besoin frontend | Appel |
|---|---|
| Tous, sans filtre | `GET /documents/search` (omettre `statuses`) |
| Pas encore soumis à revue | `GET /documents/search?statuses=DRAFT&statuses=UPLOADED` |
| En attente de revue | `GET /documents/search?statuses=PENDING_REVIEW` |
| À corriger | `GET /documents/search?statuses=NEEDS_CORRECTION` |
| Approuvés | `GET /documents/search?statuses=APPROVED` |
| Rejetés | `GET /documents/search?statuses=REJECTED` |
| Signés | `GET /documents/search?statuses=SIGNED` |
| Expirés / archivés / remplacés | `...=EXPIRED` · `...=ARCHIVED` · `...=SUPERSEDED` |

---

## 10. Dashboard d'un espace

```http
GET /documents/dashboard?space=KYC_SPACE
```
`space` optionnel (absent = global). **Réponse** `200 OK` → `DocumentDashboardResponse` :

```json
{
  "space": "KYC_SPACE",
  "total": 320,
  "byStatus": { "PENDING_REVIEW": 25, "APPROVED": 260, "REJECTED": 12 },
  "byCategory": { "KYC": 300, "LEGAL": 20 },
  "pendingReviewOlderThan48h": 4,
  "needsCorrectionCount": 8,
  "expiringIn30Days": 15,
  "rejectionRate30d": 3.75,
  "avgReviewTimeHours": 6.4,
  "topTags": [ { "tagCode": "urgent", "tagLabel": "Urgent", "count": 12 } ]
}
```

---

## 11. Dossier documentaire d'un propriétaire

```http
GET /documents/folder?ownerType=CUSTOMER&ownerCode=CUS-202606-00000012
```
Les deux paramètres sont **obligatoires**. C'est l'endpoint clé pour un écran « dossier client/membre » : en un seul appel il renvoie les documents groupés par espace, les exigences manquantes et les documents qui expirent bientôt.

**Réponse** `200 OK` → `DocumentFolderResponse` :

```json
{
  "owner": { "type": "CUSTOMER", "code": "CUS-202606-00000012", "name": "Jean Kabila" },
  "spaces": [
    {
      "space": "KYC_SPACE",
      "totalDocuments": 4,
      "approvedDocuments": 3,
      "completionRate": 0.75,
      "documents": [ /* DocumentResponse[] */ ]
    }
  ],
  "missingRequirements": [
    {
      "documentTypeCode": "JUSTIF_DOMICILE",
      "documentTypeName": "Justificatif de domicile",
      "required": true,
      "requiresBackSide": false,
      "status": "PENDING",
      "documentCode": null,
      "backDocumentCode": null
    }
  ],
  "expiringSoon": [ /* DocumentResponse[] */ ]
}
```

---

## 12. Analytics, export & journaux d'accès

Ces routes exigent **`DOCUMENT:REVIEW`**.

```http
GET /documents/analytics?space=KYC_SPACE&months=12     🔒
```
**Réponse** → `DocumentAnalyticsResponse` (état courant + métriques qualité 30 j + séries temporelles mensuelles) :

```json
{
  "space": "KYC_SPACE",
  "periodMonths": 12,
  "totalDocuments": 320,
  "pendingReview": 25,
  "needsCorrection": 8,
  "expiredDocuments": 5,
  "byStatus": { "APPROVED": 260, "REJECTED": 12 },
  "bySpace": { "KYC_SPACE": 320 },
  "byCategory": { "KYC": 300 },
  "correctionRate30d": 2.5,
  "rejectionRate30d": 3.75,
  "avgReviewTimeHours": 6.4,
  "uploadsByMonth": [ { "period": "2026-06", "count": 40 }, { "period": "2026-07", "count": 12 } ],
  "approvalsByMonth": [ { "period": "2026-07", "count": 30 } ],
  "rejectionsByMonth": [ { "period": "2026-07", "count": 2 } ],
  "correctionsByMonth": [ { "period": "2026-07", "count": 1 } ]
}
```

```http
GET /documents/export/csv?space=&category=&tags=&ownerType=&ownerCode=&statuses=&uploadedAfter=&uploadedBefore=    🔒
```
Mêmes filtres que `/search` (sous-ensemble). **Réponse** → fichier **CSV** (`Content-Disposition: attachment; filename="documents-export.csv"`), pas du JSON.

```http
GET /documents/{code}/access-logs?page=0&size=20                 🔒
GET /documents/access-logs/by-user?userId=42&page=0&size=20      🔒
```
**Réponse** → `PaginatedResponse<DocumentAccessLogResponse>` (même enveloppe `data`/`pageable` qu'au §5) :

```json
{
  "data": [
    { "id": 901, "documentCode": "DOC-...42", "userId": 42, "action": "DOWNLOAD", "accessedAt": "2026-07-09T12:00:00Z", "ipAddress": "41.x.x.x", "userAgent": "Mozilla/5.0 ..." }
  ],
  "pageable": { "page": 0, "size": 20, "totalElements": 3, "totalPages": 1, "first": true, "last": true, "hasNext": false, "hasPrevious": false }
}
```

---

## Enums

- **DocumentStatus** : `DRAFT, UPLOADED, PENDING_REVIEW, NEEDS_CORRECTION, APPROVED, REJECTED, SIGNED, EXPIRED, ARCHIVED, SUPERSEDED`
- **DocumentVersionUploadStatus** : `PENDING_SCAN, READY, QUARANTINED, REJECTED, DELETED`
- **DocumentAntivirusStatus** : `PENDING, CLEAN, INFECTED, ERROR`

## À retenir

- `download`/`preview`/`export/csv` renvoient des **octets bruts**, pas du JSON.
- `upload`, `replace`, `approve`, `reject` portent `@Idempotent` — envoyer un `Idempotency-Key` sur les formulaires d'upload et les décisions.
- En écriture on envoie **`ownerCode`** (code public) ; en lecture la réponse expose `ownerId` (id interne).