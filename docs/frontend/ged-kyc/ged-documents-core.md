# GED — Documents (cœur du module)

> Base : `/sni/api/v1/documents` — back-office uniquement. Voir [README](./README.md) pour le rattachement `ownerType`/`ownerCode` et les endpoints de recherche client/membre/entreprise.

## Upload &amp; cycle de vie

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /documents/upload` (multipart) | Upload d'un nouveau document | `ownerType*`, `ownerCode*`, `documentTypeCode*`, `title*`, `description`, `documentNumber`, `issueDate`, `expiryDate` + part `file` | `DocumentResponse` (201) |
| `POST /documents/{code}/replace` (multipart) | Remplace le fichier (nouvelle version) | part `file` | `DocumentResponse` |
| `GET /documents/{code}` | Détail d'un document | — | `DocumentResponse` (incl. `versions[]`, `tags[]`, `metadata`) |
| `GET /documents` | Liste paginée | query `ownerType`, `ownerCode` (optionnels), `Pageable` | `PaginatedResponse<DocumentResponse>` |
| `GET /documents/{code}/versions` | Historique des versions | — | `List<DocumentVersionResponse>` (versionNumber, previewUrl, downloadUrl, fileSizeBytes, uploadStatus, antivirusStatus, current) |
| `GET /documents/{code}/download` | Télécharge le fichier (log DOWNLOAD) | — | fichier binaire |
| `GET /documents/{code}/preview` | Aperçu inline (log PREVIEW) | — | fichier binaire inline |
| `POST /documents/{code}/versions/{versionNumber}/restore` | Restaure une ancienne version comme version courante | — | `DocumentResponse` — 🔒 `DOCUMENT:REVIEW` |
| `POST /documents/{code}/archive` | Archive le document | query `reason` (optionnel) | `DocumentResponse` (status → `ARCHIVED`) |
| `POST /documents/{code}/lock` / `unlock` | Verrouille/déverrouille (évite les modifications concurrentes) | — | 200 sans corps |

## Décisions de revue

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /documents/{code}/approve` | Approuve un document | `reviewedBy*` (Long), `comment` | `DocumentResponse` (status → `APPROVED`) — 🔒 `DOCUMENT:REVIEW` |
| `POST /documents/{code}/reject` | Rejette un document | `reviewedBy*`, `comment`, `rejectionReasonCode`, `rejectionReasonDetail` | `DocumentResponse` (status → `REJECTED`) — 🔒 `DOCUMENT:REVIEW` |
| `POST /documents/{code}/request-correction` | Demande une correction/re-soumission | `reviewedBy*`, `correctionNote*`, `comment`, `deadlineDays` (défaut 7) | `DocumentResponse` (status → `NEEDS_CORRECTION`) — 🔒 `DOCUMENT:REVIEW` |

Pour la revue en masse (plusieurs documents à la fois), voir [ged-review-workflow-retention.md](./ged-review-workflow-retention.md).

## Upload en masse

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /documents/upload-batch` (multipart) | Upload de plusieurs fichiers avec métadonnées partagées | parts `files[]` ; `ownerType*`, `ownerCode*`, `documentTypeCode*`, `space`, `folderCode`, `referenceCode`, `tagCodes[]` | `DocumentBulkActionResponse` (total/succeeded/failed + résultats par fichier) |
| `POST /documents/import-zip` (multipart) | Import d'un ZIP (recrée l'arborescence de dossiers) | part `file` (zip) + mêmes métadonnées que ci-dessus | `ZipImportResponse` (totalFiles, succeeded, failed, foldersCreated, items[]) |

## Recherche, dashboard &amp; analytics

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `GET /documents/search` | Recherche avancée / plein texte | `q`, `space`, `category`, `tags[]`, `ownerType`, `ownerCode`, `statuses[]`, `expiresInDays`, `isExpired`, `uploadedAfter`/`uploadedBefore`, `metaKey`/`metaValue`, `Pageable` | `PaginatedResponse<DocumentResponse>` |
| `GET /documents/dashboard` | KPI d'un espace (ou global) | query `space` (optionnel) | `DocumentDashboardResponse` (total, byStatus, byCategory, pendingReviewOlderThan48h, needsCorrectionCount, expiringIn30Days, rejectionRate30d, avgReviewTimeHours, topTags) |
| `GET /documents/folder` | Dossier documentaire complet d'un propriétaire (tous espaces + exigences manquantes + expirations proches) | query `ownerType*`, `ownerCode*` | `DocumentFolderResponse` (spaces[], missingRequirements[], expiringSoon[]) |
| `GET /documents/analytics` | Analytique étendue / séries temporelles | query `space`, `months` (défaut 12) | `DocumentAnalyticsResponse` — 🔒 `DOCUMENT:REVIEW` |
| `GET /documents/export/csv` | Export CSV filtré | mêmes filtres que `/search` | fichier CSV — 🔒 `DOCUMENT:REVIEW` |
| `GET /documents/{code}/access-logs` | Journal d'accès d'un document | `Pageable` | `PaginatedResponse<DocumentAccessLogResponse>` — 🔒 `DOCUMENT:REVIEW` |
| `GET /documents/access-logs/by-user` | Journal d'accès filtré par utilisateur | query `userId*` | `PaginatedResponse<DocumentAccessLogResponse>` — 🔒 `DOCUMENT:REVIEW` |

`GET /documents/folder` est l'endpoint clé pour un écran "dossier client/membre" : il donne en un appel tout ce qu'il faut afficher (documents par espace, ce qui manque, ce qui expire bientôt).

### Filtrage par statut

Il n'existe **pas** de route dédiée par statut (`/documents/approved`, `/documents/rejected`, etc.) — tout passe par `GET /documents/search` avec le paramètre répétable `statuses[]`, qui accepte une ou plusieurs valeurs de `DocumentStatus` :

| Besoin frontend | Appel |
|---|---|
| Tous les documents, sans filtrer par statut | `GET /documents/search` (omettre `statuses[]`) |
| Documents pas encore soumis à revue ("sans statut définitif") | `GET /documents/search?statuses[]=DRAFT&statuses[]=UPLOADED` |
| En attente de revue | `GET /documents/search?statuses[]=PENDING_REVIEW` |
| À corriger | `GET /documents/search?statuses[]=NEEDS_CORRECTION` |
| Approuvés | `GET /documents/search?statuses[]=APPROVED` |
| Rejetés | `GET /documents/search?statuses[]=REJECTED` |
| Signés | `GET /documents/search?statuses[]=SIGNED` |
| Expirés / archivés / remplacés | `GET /documents/search?statuses[]=EXPIRED`, `...=ARCHIVED`, `...=SUPERSEDED` |

Combinable avec `ownerType`/`ownerCode` pour restreindre à un client/membre/entreprise précis. Pour la file d'attente de revue dédiée (documents `PENDING_REVIEW` triés par ancienneté), voir [ged-review-workflow-retention.md](./ged-review-workflow-retention.md).

## Enums

- **DocumentStatus** : `DRAFT, UPLOADED, PENDING_REVIEW, NEEDS_CORRECTION, APPROVED, REJECTED, SIGNED, EXPIRED, ARCHIVED, SUPERSEDED`
- **DocumentVersionUploadStatus** : `PENDING_SCAN, READY, QUARANTINED, REJECTED, DELETED`
- **DocumentAntivirusStatus** : `PENDING, CLEAN, INFECTED, ERROR`

## Notes

- `download`/`preview` renvoient des octets bruts, pas du JSON.
- Upload/replace et les décisions d'approbation portent `@Idempotent` — envoyer un `Idempotency-Key` pour sécuriser les retries.
